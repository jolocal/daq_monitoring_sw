package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.common.Client;
import com.example.daq_monitoring_sw.tcp.util.DaqCenter;
import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.service.ProcessingDataService;
//import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

//import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {


    private final ProcessingDataService dataManager;
    private final ChannelManager channelManager;


    // 채널 활성화 시 호출
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String clientId = UUID.randomUUID().toString();

        Client client = Client.builder()
                .clientId(clientId)
                .connectTime(LocalDateTime.now())
                .status(Status.CONNECTED)
                .build();

        channelManager.addChannel(ctx.channel());
        channelManager.addClientInfo(ctx.channel(), client);

        log.info(">>>>>>>>>>>>>>>>>>>> New Client connected: {}", client.getClientId());
    }

    // 채널 비활성화 시 호출
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Client client = channelManager.getClientInfo(ctx.channel());

        if (client != null) {
            client.setStatus(Status.DISCONNECTED);
            String daqName = client.getDaqName();

            // 클린업 진행
            performCleanup(daqName, ctx);
            // 채널 삭제
            channelManager.removeChannel(ctx.channel());
            log.info(">>>>>>>>>>>>>>>>>>>> Client disconnected: {}-{}", client.getClientId(), client.getDaqName());
        }
        ctx.close();
    }


    private void performCleanup(String daqName, ChannelHandlerContext ctx) {
        Client client = channelManager.getClientInfo(ctx.channel());
        Status status = client.getStatus();
        Status previousStatus = client.getPreviousStatus();

        // WD 사용자일 경우 데이터 발행 중지
        if (previousStatus == Status.WD) {
            log.info("[performCleanup - WD] 데이터 발행 중지 및 클린업 시작 - DAQ ID: {}", daqName);
            dataManager.stopAndCleanup(daqName);
            log.info("[performCleanup - WD] 데이터 발행 중지 및 클린업 완료 - DAQ ID: {}", daqName);
        }

        // RD 사용자일 경우 리스너그룹에서 구독 해제
        if (previousStatus == Status.RQ) {
            String subscribeKey = client.getReadTo();

            log.info("[performCleanup - RD] 리스너 그룹에서 구독 해제 시작 - Subscribe Key: {}, Channel ID: {}", subscribeKey, daqName);
            dataManager.unSubscribe(subscribeKey, daqName);
            log.info("[performCleanup - RD] 리스너 그룹에서 구독 해제 완료 - Subscribe Key: {}, Channel ID: {}", subscribeKey, daqName);
        }

        client.setCleanupDone(true);
    }


    // 예외 발생 시 호출
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Client client = channelManager.getClientInfo(ctx.channel());
        String clientId = client.getClientId();
        String daqName = client.getDaqName();


        if (cause instanceof java.net.SocketException && "Connection reset".equals(cause.getMessage())) {
            log.warn("Connection reset by client: {}", clientId);
            performCleanup(daqName, ctx);
            channelManager.removeChannel(ctx.channel());
        } else {
            log.error("Exception in channel {}: {}", clientId, cause.getMessage(), cause);
            performCleanup(daqName, ctx);
            channelManager.removeChannel(ctx.channel());
        }

        ctx.close();
    }

}
