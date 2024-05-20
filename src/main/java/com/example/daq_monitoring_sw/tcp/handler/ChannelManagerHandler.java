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
    private final WebChannelEventService webChannelEventService;
    private final ChannelRepository channelRepository;
    private final ProcessingDataManager dataManager;

    // 채널 활성화 시 호출
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText(); // 채널ID 가져오기
        log.info("==================================== Client connected: {} ====================================", channelId);
//        webChannelEventService.sendDaqCenterInfo(daqCenter); // 웹 서버에 정보 전송
        ctx.fireChannelActive();
    }

    // 채널 비활성화 시 호출
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DaqCenter currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        if (currentDaqCenter != null) {
            String daqId = currentDaqCenter.getDaqId();
            String channelId = currentDaqCenter.getChannelId();
            String subscribeKey = currentDaqCenter.getReadTo();

            log.info("==================================== Client DisConnected: {} ====================================", channelId);

            // WD 사용자일 경우 데이터 발행 중지 및 클린업
            if (currentDaqCenter.getStatus() == Status.WD){
                dataManager.stopAndCleanup(daqId);
            }
            // RD 사용자일 경우 리스너그룹에서 구독 해제
            if (currentDaqCenter.getStatus() == Status.RD){
                dataManager.unSubscribe(subscribeKey,channelId);
            }
            // 채널 저장소에서 제거
            channelRepository.removeChannel(currentDaqCenter.getDaqId());

            // 채널 비활성화 정보 웹 서버에 전송
            // webChannelEventService.sendDaqCenterInfo(daqCenter);

            super.channelInactive(ctx);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in channel {}: {}", ctx.channel().id().asShortText(), cause.getMessage());
        ctx.close(); // 예외 발생 시 채널 닫기
    }
}
