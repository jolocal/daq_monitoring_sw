package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.SensorData;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class ReqDecoder extends ReplayingDecoder<ProtocolState> {

    private static final AttributeKey<SensorData> SENSOR_DATA_KEY = AttributeKey.valueOf("USER");
    Map<String, String> parsedSensorData = new LinkedHashMap<>();
    private String stx;
    private String totalLength;
    private String command;
    private String daqId;
    private int sensorCnt;
    private List<String> sensorIdsOrder;
    private String etx;

    public ReqDecoder() {
        super(ProtocolState.STX);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        switch (state()) {
            case STX:
                stx = readLength(in, 1);
                checkpoint(ProtocolState.TOTAL_LENGHT);
                break;
            case TOTAL_LENGHT:
                totalLength = readLength(in, 2);
                checkpoint(ProtocolState.COMMAND);
                break;
            case COMMAND:
                command = readLength(in, 2);
                switchCommandState(in, command, ctx);
                break;
            case ETX:
                etx = readLength(in, 1);

                SensorData sensorData = ctx.channel().attr(SENSOR_DATA_KEY).get();

                UserRequest userRequest = UserRequest.builder()
                        .daqId(sensorData.getDaqId())
                        .sensorCnt(sensorData.getSensorCnt())
                        .sensorIdsOrder(sensorData.getSensorIdsOrder())
                        .parsedSensorData(parsedSensorData)
                        .build();

                out.add(userRequest);
                checkpoint(ProtocolState.STX);
        }
    }

    private void switchCommandState(ByteBuf in, String command, ChannelHandlerContext ctx) {
        switch (command) {
            case "IN":
                daqId = readLength(in, 5);
                sensorCnt = Integer.parseInt(readLength(in, 2));

                sensorIdsOrder = new ArrayList<>();
                for (int i = 0; i < sensorCnt; i++) {
                    String sensorId = readLength(in, 4);
                    sensorIdsOrder.add(sensorId);
                }

                // channel에 저장
                SensorData sensorData = SensorData.builder()
                        .daqId(daqId)
                        .sensorCnt(sensorCnt)
                        .sensorIdsOrder(sensorIdsOrder)
                        .build();
                ctx.channel().attr(SENSOR_DATA_KEY).set(sensorData);

                checkpoint(ProtocolState.ETX);
                break;

            case "WD":
                SensorData wdSensorData = ctx.channel().attr(SENSOR_DATA_KEY).get();
                List<String> wdSensorIds = wdSensorData.getSensorIdsOrder();

                sensorCnt = Integer.parseInt(readLength(in, 2));
                for (int i = 0, sensorIdIndex = 0; i < sensorCnt; i++) {
                    String parsedData = processRawData(in,5); // 데이터 파싱
                    String sensorId = wdSensorIds.get(i);

                    if (sensorId.startsWith("DU")){
                        String additionalData = processRawData(in,5);
                        parsedData += additionalData;
                        i++;
                    }
                    parsedSensorData.put(sensorId,parsedData); // 파싱된 데이터 저장
                    sensorIdIndex++;
                }

                // 채널의 SensorData 객체 업데이트
                SensorData updatedSensorData = SensorData.builder()
                        .daqId(wdSensorData.getDaqId()) // 기존 데이터 유지
                        .sensorCnt(wdSensorData.getSensorCnt()) // 기존 데이터 유지
                        .sensorIdsOrder(wdSensorData.getSensorIdsOrder()) // 기존 데이터 유지
                        .parsedSensorData(parsedSensorData) // 새로 파싱된 데이터 추가
                        .build();

                ctx.channel().attr(SENSOR_DATA_KEY).set(updatedSensorData); // 채널에 저장

                checkpoint(ProtocolState.ETX);
                break;

               /* Map<String, String> parsedSensorData = new LinkedHashMap<>(); // 파싱된 데이터 저장

                for (int i = 0; i < sensorCnt; i++) {
                    String rawData = processRawData(in, 5);
                    // Sequence사용하여 현재 인덱스에 해당하는 센서 ID를 찾습니다.
                    String sensorId = channelSensorIds.get(i); // 인덱스로 센서 Id 찾기

                    // 'DU'
                    if (sensorId.startsWith("DU")) {
                        // DU 센서는 데이터가 두 개 필요하므로 한 번 더 파싱합니다.
                        String additionalData = processRawData(in, 5);
                        rawData += additionalData; // 두 번째 데이터 추가
                    }

                    parsedSensorData.put(sensorId, rawData); // 파싱된 데이터 매핑
                }

                UserRequest.builder()
                        .daqId(wdDaqId)
                        .parsedSensorData(parsedSensorData)
                        .build();

                checkpoint(ProtocolState.ETX);
                break;*/

            case "RD":
            case "ST":
        }
    }

    private String readLength(ByteBuf in, int length) {
        ByteBuf buf = in.readBytes(length);
        String res = buf.toString(StandardCharsets.UTF_8);
        buf.release();
        return res;
    }

    private String processRawData(ByteBuf in, int length) {
        String rawData = readLength(in, 5);
        // 첫 번째 문자가 '0'인 경우 '+'로 변경
        if (rawData.startsWith("0")) {
            rawData = "+" + rawData.substring(1);
        }
        // 마지막에서 두 번째 위치에 소수점 추가
        String processedData = rawData.substring(0, rawData.length() - 1) + "." + rawData.substring(rawData.length() - 1);
        return processedData;
    }

}
