package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.common.Client;
import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.ProtocolMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@Component
public class MessageDecoder extends ReplayingDecoder<ProtocolState> {
    private final StringBuilder stringBuilder = new StringBuilder();
    private final ChannelManager channelManager;


    private String deviceType;
    private String deviceId;
    private String targetDeviceId;

    private long srv_ts_ms;
    private long cli_ts_ms;
    private long timeStamp;

    private int sensorCnt;
    private List<String> sensorList = new ArrayList<>();
    private Map<String, Double> sensorDataMap = new LinkedHashMap<>();

    private int unknownCommandCount = 0;

    @Autowired
    public MessageDecoder(ChannelManager channelManager) {
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
                    //  바디 길이
                    String totalLength = readLength(in, 3);
                    log.info("  ▷ TOTAL_LEN: {}", totalLength);
                    checkpoint(ProtocolState.COMMAND);
                    break;

                case COMMAND:
                    String command = readLength(in, 2);

                    // 유효한 명령(IN, WD, RD, ST)일 때만 INFO 로그 및 상태 전환
                    if (isValidCommand(command)) {
                        log.info("  ▷ COMMAND  : {}", command);

                        // WD 명령어일 경우에만 서버 수신 시간을 기록
                        if ("WD".equals(command)) {
                            srv_ts_ms = Instant.now().toEpochMilli();
                            client.setSrv_ts_ms(srv_ts_ms);
                            log.info("  ▷ SRV_TS_MS: {}", srv_ts_ms);
                        }
                        switchCommandState(in, command, ctx);
                    
                    } else {
                        unknownCommandCount++;
                        log.debug("UNKNOWN COMMAND BYTES (ignored): {}", command);
                
                        if (unknownCommandCount >= 5) {  // 예: 5번 연속 이상 나오면
                            log.warn("프로토콜 오류: UNKNOWN COMMAND 연속 {}회, 채널 종료", unknownCommandCount);
                            ctx.close();  // 연결 강제 종료
                            unknownCommandCount = 0;
                        }
                    }
                    break;

                case ETX:
                    String etx = readLength(in, 1);
                    
                    // 사용자 요청 객체 생성
                    ProtocolMessage.ProtocolMessageBuilder builder = ProtocolMessage.builder()
                            .deviceType(deviceType)
                            .deviceId(deviceId)
                            .status(client.getStatus())
                            .previousStatus(client.getPreviousStatus())
                            .targetDeviceId(targetDeviceId)
                            .sensorCnt(sensorCnt)
                            .sensorList(sensorList)
                            .sensorDataMap(sensorDataMap)
                            .timeStamp(timeStamp)
                            .cli_ts_ms(timeStamp)
                            .srv_ts_ms(srv_ts_ms);

                    ProtocolMessage message = builder.build();

                    log.info("""
                        ====================================================
                        ▶▶▶ [DECODE COMPLETE] 프로토콜 메시지 생성 ◀◀◀
                        ====================================================
                        ▷ Channel ID    : {}
                        ▷ deviceType    : {}
                        ▷ deviceId      : {}
                        ▷ targetDeviceId: {}
                        ▷ status        : {}
                        ▷ prevStatus    : {}
                        ▷ sensorCnt     : {}
                        ▷ sensorList    : {}
                        ▷ timeStamp(cli): {}
                        ▷ sensorDataMap : {}
                        ▷ cli_ts_ms     : {}
                        ▷ srv_ts_ms     : {}
                        ====================================================
                        """,
                        ctx.channel().id(),
                        deviceType,
                        deviceId,
                        targetDeviceId,
                        client.getStatus(),
                        client.getPreviousStatus(),
                        sensorCnt,
                        sensorList,
                        timeStamp,
                        sensorDataMap,
                        cli_ts_ms,
                        srv_ts_ms
                        );

                    out.add(message);
                    checkpoint(ProtocolState.STX);
                    break;
            }

        } catch (Exception e) {
            log.error("  ▷ decode 예외 발생: {}", e.getMessage());
            throw e;
        }

    }

    private void switchCommandState(ByteBuf in, String command, ChannelHandlerContext ctx) {
        //DaqEntity currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        Client client = channelManager.getClientInfo(ctx.channel());

        switch (command) {
            case "IN": // channel에 저장
                log.info("  ▷ [IN] 장치 등록 요청");
                log.info("  - deviceType: {}", deviceType);
                // 리스트 초기화
                sensorList.clear();
                deviceType = readLength(in, 1);

                if (deviceType.equals("C")) {
                    deviceId = readLength(in, 5);
                    targetDeviceId = readLength(in, 5);
                }

                if (deviceType.equals("D") || deviceType.equals("T")){
                    deviceId = readLength(in, 5);
                    String senCntStr = readLength(in, 2);
                    
                    sensorCnt = Integer.parseInt(senCntStr);
                    for (int i = 0; i < sensorCnt; i++) {
                        String in_sensorId = readLength(in, 2);
                        sensorList.add(in_sensorId);
                    }
                }

                // Client 정보 저장
                client.setDeviceType(deviceType);
                client.setStatus(Status.IN);
                
                client.setDeviceId(deviceId);
                client.setTargetDeviceId(targetDeviceId);
                
                client.setSensorCnt(sensorCnt);
                client.setSensorList(sensorList);

                // 채널에 uuid 대신 deviceId로 저장
                ChannelId chId_1 = ctx.channel().id();
                channelManager.updateDaqId(ctx.channel(),deviceId);
                checkpoint(ProtocolState.ETX);
                break;


            case "WD":
                log.info("▷ [WD] 실시간 데이터 수신");

                ChannelId chId_2 = ctx.channel().id();
                List<String> sensorList = client.getSensorList(); // "IN"에서 저장한 센서리스트

                String senCntStr_1 = readLength(in, 2);
                sensorCnt = Integer.parseInt(senCntStr_1);

                // 클라이언트에서 전송한 타임스탬프(예: epoch ms 문자열)를 읽어서 long으로 변환
                String timeStamp_str = readLength(in, 13);
                try {
                    timeStamp = Long.parseLong(timeStamp_str);
                } catch (NumberFormatException e) {
                    log.warn("  ▷ [WD] timeStamp 파싱 실패: {}, 기본값 0 사용", timeStamp_str);
                    timeStamp = 0L;
                }
                cli_ts_ms = timeStamp;


                for (int i = 0; i < sensorCnt; i++) {
                    String rawData = readLength(in,8); //±0000.00
                    String sensorName = sensorList.get(i);
                    
                    try {

                        double parsedValue = parseSignedSensorValue(rawData); // ← 전용 파서 사용
                        sensorDataMap.put(sensorName, parsedValue);

                    } catch (NumberFormatException e) {
                        log.warn("  ⚠ 센서값 파싱 실패: name={}, raw={}", sensorName, rawData);
                        sensorDataMap.put(sensorName, Double.NaN);
                    }
                    
                }
                // log.info("   ▶ sensorDataMap: {}", sensorDataMap);
                client.setStatus(Status.WD);
                checkpoint(ProtocolState.ETX);
                break;
            
            case "RD":
                log.info("▷ [RD] 읽기 요청");

                targetDeviceId = readLength(in, 5);
                client.setTargetDeviceId(targetDeviceId);
                client.setStatus(Status.RD);
                checkpoint(ProtocolState.ETX);
                break;

            case "ST":
                log.info("▷ [ST] 세션 종료 요청");
                client.setPreviousStatus(client.getStatus());
                client.setStatus(Status.ST);
                checkpoint(ProtocolState.ETX);
                break;

        }
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

    private double parseSignedSensorValue(String rawData) {
        if (rawData == null || rawData.length() != 8) {
            throw new NumberFormatException("Invalid raw sensor data length: " + rawData);
        }

        // 첫 글자는 부호 플래그로 사용 (예: '+', '-', '0' 등)
        char signFlag = rawData.charAt(0);
        String magnitudeStr = rawData.substring(1); // 7글자: "0018.02", "0892.26", "0015.01" 등

        double magnitude = Double.parseDouble(magnitudeStr);

        // 음수인 경우에만 '-'를 사용하고, 그 외(+, 0 등)는 모두 양수로 처리
        return (signFlag == '-') ? -magnitude : magnitude;
    }

    /**
     * 프로토콜에서 사용하는 정상적인 명령어인지 여부를 검사한다.
     * 잘못 파싱된 센서 데이터 조각 등이 COMMAND 단계에 들어오는 경우
     * INFO 로그를 남기지 않기 위해 사용된다.
     */
    private boolean isValidCommand(String command) {
        return "IN".equals(command)
                || "WD".equals(command)
                || "RD".equals(command)
                || "ST".equals(command);
    }

    // private boolean isValidData(String data) {
    //     return data.matches("^[+-]\\d{4}\\.\\d{2}$");
    // }

    // private String processRawData(ByteBuf in, int length) {   
    //     String rawData = readLength(in, length);
    //     return rawData.replace(rawData.substring(1),"+");
    //     // 부호 처리 (±)
    //     // if (rawData.startsWith("0")) {
    //     //     rawData = "+" + rawData.substring(1);
    //     // }
        
    //     // 마지막에서 두 번째 위치에 소수점 추가
    //     // return rawData.substring(0, rawData.length() - 1) + "." + rawData.substring(rawData.length() - 1);
    // }

}
