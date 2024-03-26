package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;
import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.findAllChannel;

@Slf4j
@Component
public class ReqDecoder extends ReplayingDecoder<ProtocolState> {
    Map<String, String> parsedSensorData = new LinkedHashMap<>();

    public ReqDecoder(ChannelRepository channelRepository) {
        super(ProtocolState.STX);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        switch (state()) {
            case STX:
                String stx = readLength(in, 1);
                checkpoint(ProtocolState.TOTAL_LENGHT);
                break;
            case TOTAL_LENGHT:
                String totalLength = readLength(in, 2);
                checkpoint(ProtocolState.COMMAND);
                break;
            case COMMAND:
                String command = readLength(in, 2);
                switchCommandState(in, command, ctx);
                break;
            case ETX:
                String etx = readLength(in, 1);

                DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

                if (daqCenter != null) {
                    UserRequest userRequest = UserRequest.builder()
                            .status(daqCenter.getStatus())
                            .daqId(daqCenter.getDaqId())

                            .channelId(daqCenter.getChannelId())
                            .readTo(daqCenter.getReadTo())

                            .sensorCnt(daqCenter.getSensorCnt())
                            .sensorIdsOrder(daqCenter.getSensorIdsOrder())
                            .parsedSensorData(daqCenter.getParsedSensorData())
                            .build();

                    log.info("[DECODER] channelRepository 저장 확인: {}", findAllChannel());
                    out.add(userRequest);
                }
                checkpoint(ProtocolState.STX);
        }

    }

    private void switchCommandState(ByteBuf in, String command, ChannelHandlerContext ctx) {
        DaqCenter currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        switch (command) {
            case "IN":
                String daqId = readLength(in, 5);
                int sensorCnt = Integer.parseInt(readLength(in, 2));

                List<String> sensorIdsOrder = new ArrayList<>();
                for (int i = 0; i < sensorCnt; i++) {
                    String sensorId = readLength(in, 4);
                    sensorIdsOrder.add(sensorId);
                }

                // channel에 저장할 daqcenter 객체 생성
                DaqCenter daqCenter = DaqCenter.builder()
                        .daqId(daqId)
                        .status(Status.IN)
                        .sensorCnt(sensorCnt)
                        .sensorIdsOrder(sensorIdsOrder)
                        .build();

                // channel에 저장
                ctx.channel().attr(DAQ_CENTER_KEY).set(daqCenter);
                // channel repository에 저장, key: daqId
                ChannelRepository.putChannel(daqId, daqCenter);

                checkpoint(ProtocolState.ETX);
                break;

            case "WD":
                currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

                if (currentDaqCenter != null) {
                    List<String> wdSensorIds = currentDaqCenter.getSensorIdsOrder();

                    sensorCnt = Integer.parseInt(readLength(in, 2));
                    for (int i = 0, sensorIdIndex = 0; i < sensorCnt; i++) {
                        String parsedData = processRawData(in, 5); // 데이터 파싱
                        String sensorId = wdSensorIds.get(i);

                        if (sensorId.startsWith("DU")) {
                            String additionalData = processRawData(in, 5);
                            parsedData += additionalData;
                            i++;
                        }
                        parsedSensorData.put(sensorId, parsedData); // 파싱된 데이터 저장
                        sensorIdIndex++;
                    }

                    currentDaqCenter.setStatus(Status.WD);
                    currentDaqCenter.setParsedSensorData(parsedSensorData);

                    // 변경된 객체를 다시 채널 속성에 설정
                    ctx.channel().attr(DAQ_CENTER_KEY).set(currentDaqCenter);

                }

                checkpoint(ProtocolState.ETX);
                break;

            case "RQ":
                String readToDaqId = readLength(in, 5);

                String channelId = ctx.channel().id().asShortText();

                // channel에 저장할 daqcenter 객체 생성
                DaqCenter daqCenter_rQ = DaqCenter.builder()
                        .daqId(channelId)
                        .channelId(channelId)
                        .readTo(readToDaqId)
                        .status(Status.RQ)
                        .build();

                // channel에 저장
                ctx.channel().attr(DAQ_CENTER_KEY).set(daqCenter_rQ);
                // channel repository에 저장, key: channelId
                ChannelRepository.putChannel(channelId, daqCenter_rQ);

                checkpoint(ProtocolState.ETX);
                break;

            case "ST":
                currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

                if (currentDaqCenter != null) {
                    currentDaqCenter.setStatus(Status.ST);
                    ctx.channel().attr(DAQ_CENTER_KEY).set(currentDaqCenter);
                }
                checkpoint(ProtocolState.ETX);
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
        return rawData.substring(0, rawData.length() - 1) + "." + rawData.substring(rawData.length() - 1);
    }

}
