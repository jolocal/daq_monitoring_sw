package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;
import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.findAllChannel;

@Slf4j
@Component
public class ReqDecoder extends ReplayingDecoder<ProtocolState> {
    Map<String, String> parsedSensorData = new LinkedHashMap<>();
    private final StringBuilder stringBuilder = new StringBuilder();

    public ReqDecoder(ChannelRepository channelRepository) {
        super(ProtocolState.STX);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        try{

            switch (state()) {
                case STX:
                    String stx = readLength(in, 1);
                    checkpoint(ProtocolState.TOTAL_LENGHT);
                    break;

                case TOTAL_LENGHT:
                    String totalLength = readLength(in, 3);
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
                                .previousStatus(daqCenter.getPreviousStatus())
                                .daqId(daqCenter.getDaqId())

                                .channelId(daqCenter.getChannelId())
                                .readTo(daqCenter.getReadTo())

                                .sensorCnt(daqCenter.getSensorCnt())
                                .sensorIdsOrder(daqCenter.getSensorIdsOrder())
                                .parsedSensorData(daqCenter.getParsedSensorData())
                                .timeStamp(daqCenter.getTimeStamp())

                                .build();

                        log.debug("[ReqDecoder] Decoded UserRequest: {}", userRequest);
                        out.add(userRequest);
                    }

                    checkpoint(ProtocolState.STX);
                    break;
            }
        } catch (Exception e) {
            log.error("decode 메서드 예외 발생: {}",e.getMessage());
            throw e;

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
                if (currentDaqCenter != null) {
                    List<String> wdSensorIds = currentDaqCenter.getSensorIdsOrder();

                    // 센서갯수
                    sensorCnt = Integer.parseInt(readLength(in, 2));

                    String timeStamp = readLength(in, 9); // hh

                    /*String rawTime = readLength(in, 8);
                    String timeStamp = rawTime.substring(0,5) + "." + rawTime.substring(5);*/

                    for (int i = 0, sensorIdIndex = 0; i < sensorCnt; i++) {
                        String parsedData = processRawData(in, 5); // 데이터 파싱
                        String sensorId = wdSensorIds.get(i);

                        parsedSensorData.put(sensorId, parsedData); // 파싱된 데이터 저장
                        sensorIdIndex++;
                    }

                    currentDaqCenter.setStatus(Status.WD);
                    currentDaqCenter.setTimeStamp(timeStamp);
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
                if (currentDaqCenter != null) {
                    currentDaqCenter.setPreviousStatus(currentDaqCenter.getStatus());
                    currentDaqCenter.setStatus(Status.ST);
                    ctx.channel().attr(DAQ_CENTER_KEY).set(currentDaqCenter);
                }

                checkpoint(ProtocolState.ETX);
                break;

        }
    }

    private String readLength(ByteBuf in, int length) {
        // TODO: ByteBuf에서 직접 바이트를 읽어 StringBuilder에 추가하는 방식으로 변경
        // ByteBuf의 데이터를 불필요하게 ByteBuf 객체로 변환하고 해제하는 과정을 줄일
        // StringBuilder를 사용하여 문자열을 결합하는 방식으로 변경, String 불필요한 객체 생성 줄임
        stringBuilder.setLength(0);
        for (int i=0; i < length; i++){
            stringBuilder.append((char)in.readByte());
        }
        return stringBuilder.toString();
    }
    private String processRawData(ByteBuf in, int length) {
        String rawData = readLength(in, length);
        // 첫 번째 문자가 '0'인 경우 '+'로 변경
        if (rawData.startsWith("0")) {
            rawData = "+" + rawData.substring(1);
        }
        // 마지막에서 두 번째 위치에 소수점 추가
        return rawData.substring(0, rawData.length() - 1) + "." + rawData.substring(rawData.length() - 1);
    }

}
