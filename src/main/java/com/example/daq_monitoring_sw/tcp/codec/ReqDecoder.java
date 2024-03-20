package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
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
    public static final AttributeKey<DaqCenter> DAQ_CENTER_KEY = AttributeKey.valueOf("DAQ_CENTER");
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

                DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
                UserRequest userRequest = UserRequest.builder()
                        .status(daqCenter.getStatus())
                        .daqId(daqCenter.getDaqId())
                        .sensorCnt(daqCenter.getSensorCnt())
                        .sensorIdsOrder(daqCenter.getSensorIdsOrder())
                        .parsedSensorData(daqCenter.getParsedSensorData())
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
                DaqCenter daqCenter = DaqCenter.builder()
                        .daqId(daqId)
                        .status(Status.IN)
                        .sensorCnt(sensorCnt)
                        .sensorIdsOrder(sensorIdsOrder)
                        .build();
                ctx.channel().attr(DAQ_CENTER_KEY).set(daqCenter);

                checkpoint(ProtocolState.ETX);
                break;

            case "WD":
                DaqCenter wdDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
                List<String> wdSensorIds = wdDaqCenter.getSensorIdsOrder();

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

                // 채널의 DaqCenter 객체 업데이트
                DaqCenter updatedDaqCenter = DaqCenter.builder()
                        .daqId(wdDaqCenter.getDaqId()) // 기존 데이터 유지
                        .status(Status.WD)
                        .sensorCnt(wdDaqCenter.getSensorCnt()) // 기존 데이터 유지
                        .sensorIdsOrder(wdDaqCenter.getSensorIdsOrder()) // 기존 데이터 유지
                        .parsedSensorData(parsedSensorData) // 새로 파싱된 데이터 추가
                        .build();

                ctx.channel().attr(DAQ_CENTER_KEY).set(updatedDaqCenter); // 채널에 저장

                checkpoint(ProtocolState.ETX);
                break;

            case "RD":
            case "ST":
                DaqCenter st_daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
                DaqCenter st_update_daqCenter = DaqCenter.builder().status(Status.ST).build();
                ctx.channel().attr(DAQ_CENTER_KEY).set(st_update_daqCenter); // 채널에 저장

            break;

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
