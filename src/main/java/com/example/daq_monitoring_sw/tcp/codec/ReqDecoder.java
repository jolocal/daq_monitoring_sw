package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.common.Client;
import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Slf4j
@Component
public class ReqDecoder extends ReplayingDecoder<ProtocolState> {
    private final StringBuilder stringBuilder = new StringBuilder();
    private final ChannelManager channelManager;

    private String daqName;
    private String readTo;
    private String sensorCnt;
    private Map<String, String> sensorDataMap = new LinkedHashMap<>();
    private List<String> sensorList = new ArrayList<>();
    private String cliSentTime;

    @Autowired
    public ReqDecoder(ChannelManager channelManager) {
        super(ProtocolState.STX);
        this.channelManager = channelManager;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Client client = channelManager.getClientInfo(ctx.channel());

        try {
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

                    UserRequest userRequest = UserRequest.builder()
                            .daqName(daqName)
                            .status(client.getStatus())
                            .previousStatus(client.getPreviousStatus())
                            .readTo(readTo)
                            .sensorCnt(sensorCnt)
                            .sensorList(sensorList)
                            .sensorDataMap(sensorDataMap)
                            .cliSentTime(cliSentTime)
                            .build();

                    out.add(userRequest);

                    checkpoint(ProtocolState.STX);
                    break;
            }

        } catch (Exception e) {
            log.error("decode 예외 발생: {}", e.getMessage());
            throw e;
        }

    }

    private void switchCommandState(ByteBuf in, String command, ChannelHandlerContext ctx) {
        //DaqEntity currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        Client client = channelManager.getClientInfo(ctx.channel());

        switch (command) {
            case "IN": // channel에 저장
                daqName = readLength(in, 5);
                sensorCnt = readLength(in, 2);

                int cnt = Integer.parseInt(sensorCnt);
                for (int i = 0; i < cnt; i++) {
                    String sensorId = readLength(in, 4);
                    sensorList.add(sensorId);
                }

                // 채널 - client 객체 저장
                client.setStatus(Status.IN);
                client.setDaqName(daqName);
                client.setSensorCnt(sensorCnt);
                client.setSensorList(sensorList);
                // 채널에 daqName 업데이트.
                channelManager.updateDaqName(ctx.channel(),daqName);

                checkpoint(ProtocolState.ETX);
                break;



            case "WD":
                List<String> sensorList1 = client.getSensorList();

                sensorCnt = readLength(in, 2);
                cliSentTime = readLength(in, 12); // HH:MM:SS.mmm

                int cnt1 = Integer.parseInt(sensorCnt);
                for (int i = 0; i < cnt1; i++) {
                    String parsedData = processRawData(in, 5); // 데이터 파싱 000.0
                    String sensorName = sensorList1.get(i);

                    // 유효한 데이터만 저장
                    if (isValidData(parsedData)){
                        sensorDataMap.put(sensorName, parsedData);
                    } else {
                        sensorDataMap.put(sensorName, "+000.0");
                    }
                }

                client.setStatus(Status.WD);
                checkpoint(ProtocolState.ETX);
                break;

            case "RQ":
                daqName = readLength(in, 5);
                readTo = readLength(in, 5);

                // 채널 - client 객체 저장
                client.setStatus(Status.RQ);
                client.setReadTo(readTo);
                client.setDaqName(daqName);

                checkpoint(ProtocolState.ETX);
                break;

            case "ST":

                client.setPreviousStatus(client.getStatus());
                client.setStatus(Status.ST);

                checkpoint(ProtocolState.ETX);
                break;

        }
    }

    // +-000.0
    private boolean isValidData(String data) {
        return data.matches("^[+-]\\d{3}\\.\\d$");
    }

    private String readLength(ByteBuf in, int length) {
        // TODO: ByteBuf에서 직접 바이트를 읽어 StringBuilder에 추가하는 방식으로 변경
        // ByteBuf의 데이터를 불필요하게 ByteBuf 객체로 변환하고 해제하는 과정을 줄일
        // StringBuilder를 사용하여 문자열을 결합하는 방식으로 변경, String 불필요한 객체 생성 줄임
        stringBuilder.setLength(0);
        for (int i = 0; i < length; i++) {
            stringBuilder.append((char) in.readByte());
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
