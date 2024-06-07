package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.common.Client;
import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.*;
import com.example.daq_monitoring_sw.tcp.service.ProcessingDataService;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final ProcessingDataService dataManager;
    private final ChannelManager channelManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserRequest userReq) throws Exception {
        log.debug("=============================== 받은 메시지 ===============================");
        log.debug(String.valueOf(userReq));
        log.debug("=========================================================================");

        // DaqEntity currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();
        Client client = channelManager.getClientInfo(ctx.channel());
        Status status = client.getStatus();

        switch (status) {
            case IN -> {
                return;
            }
            case WD -> handleWDCommand(userReq);
            case RQ -> handleRQCommand(ctx, userReq);
            case ST -> handleSTCommand(ctx, client, userReq);
            default -> throw new IllegalStateException("예상치 못한 상태: " + client.getStatus());
        }
    }

    // 'WD' 쓰기
    private void handleWDCommand(UserRequest userReq) {
        log.info("'WD' 명령 처리 - DAQ ID: {}", userReq.getDaqName());
        dataManager.writeData(userReq);
    }

    // 'RQ' 읽기 응답
    private void handleRQCommand(ChannelHandlerContext ctx, UserRequest userReq) {
        Client client = channelManager.getClientInfo(ctx.channel());

        log.info("'RQ' 명령 처리 - daqName: {} for DAQID: {}", userReq.getDaqName(), userReq.getReadTo());

        Optional<Client> activeCliByDaqName = channelManager.findActiveCliByDaqName(userReq.getReadTo());

        activeCliByDaqName.ifPresentOrElse(curWDcli -> {
            // 활성화된 'WD'클라이언트를 'RQ' Channel에 저장
            client.setSensorCnt(curWDcli.getSensorCnt());
            client.setSensorList(curWDcli.getSensorList());

            log.info("현재 rq 채널의 정보: {}", client);
            RqInfoRes firstRes = createFirstRes(curWDcli);

            ctx.writeAndFlush(firstRes).addListener(future -> {
                if (future.isSuccess()) {
                    log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 첫번째 응답 'RS' 전송");

                    // 리스너 등록 & 데이터 구독
                    regListenerAndDataPub(ctx, userReq);

                } else {
                    log.error("첫 번째 응답 전송 실패: ", future.cause());
                }
            });
        }, () -> {
            log.debug("활성화된 WD 채널이 없음.");
            ErrorResponse noWdResponse = createNoWdResponse(client);
            // 응답 전송 후 채널 닫기
            ctx.writeAndFlush(noWdResponse).addListener(ChannelFutureListener.CLOSE);
        });

    }

    private ErrorResponse createNoWdResponse(Client client) {
        return ErrorResponse.builder()
                .status(Status.ER)
                .msg("No active WD channel found for DAQ ID: " + client.getReadTo())
                .build();
    }


    // 'ST' 종료
    private void handleSTCommand(ChannelHandlerContext ctx, Client client, UserRequest userReq) {
        log.info("'ST' 명령 처리 - DAQ ID: {}", client.getDaqName());

        if (client.getPreviousStatus() == Status.WD) {
            dataManager.stopAndCleanup(userReq.getDaqName());
        }

        if (client.getPreviousStatus() == Status.RQ) {
            dataManager.unSubscribe(userReq.getReadTo(), userReq.getDaqName());
        }
        client.setCleanupDone(true);
        // 채널 비활성화 이벤트를 다음 핸들러로 전달하여 ChannelManagerHandler의 channelInactive가 호출되도록 함
        ctx.fireChannelInactive();

    }

    // RQ 첫번째 응답
    private RqInfoRes createFirstRes(Client curWDcli) {
        RqInfoRes resRQ = RqInfoRes.builder()
                .status(Status.RS)
                .daqName(curWDcli.getDaqName())
                .sensorCnt(curWDcli.getSensorCnt())
                .sensorList(curWDcli.getSensorList())
                .build();
        return resRQ;
    }

    // 리스너 등록 및 데이터 발행
    private void regListenerAndDataPub(ChannelHandlerContext ctx, UserRequest userReq) {
        String readTo = userReq.getReadTo(); // 읽을 daqcenter -> subscriberkey
        String daqName = userReq.getDaqName(); // subscriber

        dataManager.subscribe(readTo, daqName, ctx, dataToSend -> {
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 데이터 발행 (dataHandler) : {} - {}", daqName, dataToSend.toString());

            Client client = channelManager.getClientInfo(ctx.channel());
            if (dataToSend != null && !dataToSend.isEmpty()) {
                String cliSentTime = dataToSend.get(0);
                List<String> remainingPacketList = new ArrayList<>(dataToSend.subList(1, dataToSend.size()));

                client.setCliSentTime(cliSentTime);

                // 응답 생성
                RqInfoRes response = createResponse(client, remainingPacketList);
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 데이터 발행 후 응답: {}", response);

                // 응답 전송
                sendResponse(ctx, response);

                // 리소스 정리
                dataToSend.clear();
            }
        });
    }

    private RqInfoRes createResponse(Client client, List<String> remainingPacketList) {

        RqInfoRes rqInfoRes = RqInfoRes.builder()
                .status(Status.RD)
                .daqName(client.getDaqName())
                .readTo(client.getReadTo())
                .sensorCnt(client.getSensorCnt())
                .cliSentTime(client.getCliSentTime())
                .packetList(remainingPacketList)
                .build();

        return rqInfoRes;
    }

    private void sendResponse(ChannelHandlerContext ctx, RqInfoRes response) {
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
}