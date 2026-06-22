package com.example.daq_monitoring_sw.tcp.handler;

// import com.example.daq_monitoring_sw.tcp.batch.SchedulerConfig;
import com.example.daq_monitoring_sw.tcp.codec.ProtocolState;
import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.common.Client;
import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.*;
import com.example.daq_monitoring_sw.tcp.service.MessageProcessorService;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    private final MessageProcessorService dataManager;
    private final ChannelManager channelManager;
    // private final SchedulerConfig schedulerConfig;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage message) throws Exception {
        log.debug("=============================== 받은 메시지 ===============================");
        log.debug(String.valueOf(message));
        log.debug("=========================================================================");

        // DaqEntity currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();
        Client client = channelManager.getClientInfo(ctx.channel());
        Status status = client.getStatus();

        switch (status) {
            case IN -> handleINCommand(ctx,message);
            case WD -> handleWDCommand(message);
            case RD -> handleRDCommand(ctx, message);
            case ST -> handleSTCommand(ctx, client, message);
            default -> throw new IllegalStateException("예상치 못한 상태: " + client.getStatus());
        }
    }

    private void handleINCommand(ChannelHandlerContext ctx, ProtocolMessage message) {
        log.info("'IN' 명령 처리 - DAQ ID: {} - {}", message.getDeviceType(), message.getDeviceId());
        String deviceType = message.getDeviceType();

        if (deviceType.equals("C")){

            String targetDeviceId = message.getTargetDeviceId();
            Optional<Client> activeWDdevice = channelManager.findActiveCliByDaqName(targetDeviceId);
            
            activeWDdevice.ifPresentOrElse(device  -> {
                message.setStatus(Status.IS);
                Response response = Response.builder()
                                            .result("S")
                                            .rsType(message.getStatus())
                                            .targetDeviceId(message.getTargetDeviceId())
                                            .sensorList(activeWDdevice.get().getSensorList())
                                            .build();

                sendResponse(ctx, response);
                // ctx.writeAndFlush(response);
            }, 
            () -> {
                message.setStatus(Status.IE);
                Response response = Response.builder()
                                            .result("E")
                                            .rsType(message.getStatus())
                                            .message("Not found active WD channel")
                                            .build();
                                            
                sendResponse(ctx, response);
                //ctx.writeAndFlush(response);
            });
        // 
        }
        
        if (deviceType.equals("D") || deviceType.equals("T")){
            // message.setStatus(Status.IN);
            Response response = Response.builder()
                                .result("S")
                                .rsType(message.getStatus())
                                .message("Successfully connected to Cloud-Server!")
                                .build();
            

            ctx.writeAndFlush(response);
        }
    }

    // 'WD' 쓰기
    private void handleWDCommand(ProtocolMessage userReq) {
        log.info("'WD' 명령 처리 - DAQ ID: {}", userReq.getDeviceId());
        //* 1) 실시간 구독자에게 뿌리기 (최소한의 일만)
        dataManager.writeData(userReq);
        //* 2) DB 저장은 별도 스레드에게 맡김
        
    }

    // 'RD' 읽기 응답
    private void handleRDCommand(ChannelHandlerContext ctx, ProtocolMessage message) {
        Client client = channelManager.getClientInfo(ctx.channel());
        log.info("'RD' 명령 처리 - daqName: {} for DAQID: {}", message.getDeviceId(), message.getTargetDeviceId());

        // 활성화된 'WD'클라이언트를 찾기
        Optional<Client> activeWDdevice = channelManager.findActiveCliByDaqName(message.getTargetDeviceId());
        activeWDdevice.ifPresentOrElse(device -> {
            
            //*  1. RD 구독 등록
            regListenerAndDataPub(ctx, message);

        }, () -> {
            log.debug("활성화된 WD 채널이 없음.");
            Response response = Response.builder()
                                .result("E")
                                .rsType(message.getStatus())
                                .message("No active WD channel found for Device ID: " + message.getTargetDeviceId())
                                .build();

            ctx.writeAndFlush(response);

            // ErrorResponse noWdResponse = createNoWdResponse(client);
            // ctx.writeAndFlush(noWdResponse).addListener(ChannelFutureListener.CLOSE); // 응답 전송 후 채널 닫기
        });

    }

    private void regListenerAndDataPub(ChannelHandlerContext ctx, ProtocolMessage message) {
        
        String deviceId = message.getDeviceId(); // subscriber
        String targetDeviceId = message.getTargetDeviceId(); // 읽을 daqcenter -> subscriberkey

        //*  2. RD 구독 등록 
        //*     - 람다(payloads -> {}): 나중에 SubscriberNotifier 데이터를 줄 때 어떻게 보낼지 정의한 콜백
        dataManager.subscribe(targetDeviceId, deviceId, ctx, payloads -> {
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 데이터 발행 (dataHandler) : {} - {}", deviceId, payloads.toString());

            if (payloads == null || payloads.isEmpty()){
                return;
            }

            //* 수신한 Payloads를 DcPayload 변환
            for (Payloads sample : payloads) {
                DcPayload dcPayload = DcPayload.builder()
                        .targetDeviceId(targetDeviceId)
                        .sensorCnt(sample.getSensorCnt())
                        .ts_ms(sample.getTimeStamp())
                        .values(sample.getValues())
                        .build();
            
                sendDcPayload(ctx, dcPayload);
            }
        });
    }


    // 'ST' 종료
    private void handleSTCommand(ChannelHandlerContext ctx, Client client, ProtocolMessage message) {
        log.info("'ST' 명령 처리 - DAQ ID: {}", client.getDeviceId());

        if (client.getPreviousStatus() == Status.WD) {
            dataManager.stopAndCleanup(message.getDeviceId());
        }

        if (client.getPreviousStatus() == Status.RD) {
            dataManager.unSubscribe(message.getTargetDeviceId(), message.getDeviceId());
        }
        client.setCleanupDone(true);
        // 채널 비활성화 이벤트를 다음 핸들러로 전달하여 ChannelManagerHandler의 channelInactive가 호출되도록 함
        ctx.fireChannelInactive();

    }

    // // RQ 첫번째 응답
    // private RqInfoRes createFirstRes(Client curWDcli) {
    //     RqInfoRes resRQ = RqInfoRes.builder()
    //             .status(Status.RS)
    //             .deviceId(curWDcli.getDeviceId())
    //             .sensorCnt(curWDcli.getSensorCnt())
    //             .sensorList(curWDcli.getSensorList())
    //             .build();
    //     return resRQ;
    // }




    // private RqInfoRes createResponse(Client client, List<String> remainingPacketList) {

    //     RqInfoRes rqInfoRes = RqInfoRes.builder()
    //             .status(Status.RD)
    //             .daqName(client.deviceId())
    //             .readTo(client.targetDeviceId())
    //             .sensorCnt(client.getSensorCnt())
    //             .cliSentTime(client.getCliSentTime())
    //             .packetList(remainingPacketList)
    //             .build();

    //     return rqInfoRes;
    // }

    private void sendResponse(ChannelHandlerContext ctx, Response response) {
        log.info("(2) sendResponse: {}", response);

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(response).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("클라이언트로 응답 전송 실패", future.cause());
                }
            });
        } else {
            log.warn("채널이 비활성화 상태입니다. 클라이언트로 응답을 전송할 수 없습니다.");
        }
    }

    private void sendDcPayload (ChannelHandlerContext ctx, DcPayload dcPayload) {
        if(ctx.channel().isActive()) {
            ctx.writeAndFlush(dcPayload).addListener(future -> {
                if(!future.isSuccess()) {
                    log.error("클라이언트로 DC 페이로드 전송 실패", future.cause());
                }
            });
        } else {
            log.warn("채널이 비활성화 상태입니다. 클라이언트로 DC 페이로드 전송할 수 없습니다.");
        }
    }
}