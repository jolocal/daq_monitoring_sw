package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.pub_sub.ProcessingDataManager;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import com.example.daq_monitoring_sw.web.service.WebChannelEventService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelManagerHandler extends ChannelInboundHandlerAdapter {
    private final ChannelRepository channelRepository;
    private final ProcessingDataManager dataManager;

    // 채널 활성화 시 호출
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText(); // 채널ID 가져오기
        log.info("==================================== Client connected: {} ====================================", channelId);
        ctx.fireChannelActive();
    }

    // 채널 비활성화 시 호출
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DaqCenter currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        try {
            if (currentDaqCenter != null) {
                String daqId = currentDaqCenter.getDaqId(); //rd: channelId (3d12b95a)
                String channelId = currentDaqCenter.getChannelId(); // rd: channelId (3d12b95a)

                log.info("==================================== Client DisConnected: WD:{}, RD:{} ====================================", daqId, channelId);
                log.info("channel info: {}", currentDaqCenter);
                if (!currentDaqCenter.isCleanupDone()) {
                    performCleanup(currentDaqCenter, daqId, ctx);
                }

                channelRepository.removeChannel(daqId);

                ctx.close(); // 소켓닫기

            }
        } catch (Exception e) {
            log.error("[channelInactive] 채널 비활성화 중 예외 발생: {}", e.getMessage(),e);
        }

    }

    // 예외 발생 시 호출
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof java.net.SocketException && "Connection reset".equals(cause.getMessage())) {
            log.warn("Connection reset by client: {}", ctx.channel().remoteAddress());
        } else {
            log.error("Exception in channel {}: {}", ctx.channel().id().asShortText(), cause.getMessage(), cause);
        }

        DaqCenter currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        if (currentDaqCenter != null) {
            String daqId = currentDaqCenter.getDaqId();

            log.info(currentDaqCenter.toString());
            performCleanup(currentDaqCenter, daqId, ctx);

            channelRepository.removeChannel(daqId);
            log.info("[exceptionCaught] 채널 저장소에서 제거 - DAQ ID: {}", daqId);
        }

        ctx.close();
    }

    private void performCleanup(DaqCenter currentChannel, String daqId, ChannelHandlerContext ctx) {

        // WD 사용자일 경우 데이터 발행 중지
        if (currentChannel.getPreviousStatus() == Status.WD) {
            log.info("[performCleanup - WD] 데이터 발행 중지 및 클린업 시작 - DAQ ID: {}", daqId);
            dataManager.stopAndCleanup(daqId);
            log.info("[performCleanup - WD] 데이터 발행 중지 및 클린업 완료 - DAQ ID: {}", daqId);
        }

        // RD 사용자일 경우 리스너그룹에서 구독 해제
        if (currentChannel.getPreviousStatus() == Status.RQ) {
            String subscribeKey = currentChannel.getReadTo();
            String channelId = currentChannel.getChannelId();
            log.info("[performCleanup - RD] 리스너 그룹에서 구독 해제 시작 - Subscribe Key: {}, Channel ID: {}", subscribeKey, channelId);
            dataManager.unSubscribe(subscribeKey, channelId);
            log.info("[performCleanup - RD] 리스너 그룹에서 구독 해제 완료 - Subscribe Key: {}, Channel ID: {}", subscribeKey, channelId);
        }

        currentChannel.setCleanupDone(true); // 클린업 완료 기록
    }
}
