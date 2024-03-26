package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.web.service.WebChannelEventService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelManagerHandler extends ChannelInboundHandlerAdapter {

    private final WebChannelEventService webChannelEventService;
    private static final Map<String, DaqCenter> channelGroup = new ConcurrentHashMap<>();

    // 채널 활성화 시 호출
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText(); // 채널ID 가져오기

        // daqcenter 객체 생성 및 초기화
        DaqCenter daqCenter = DaqCenter.builder()
                .daqId(channelId)
                .status(Status.CONNECTED)
                .sensorCnt(0)
                .sensorIdsOrder(new ArrayList<>())
                .parsedSensorData(new HashMap<>())
                .build();

        channelGroup.put(channelId,daqCenter); // 채널 그룹에 저장
        ctx.channel().attr(DAQ_CENTER_KEY).set(daqCenter); // 채널 속성에 저장

        log.info("==================================== Client connected: {} ====================================", channelId);

//        webChannelEventService.sendDaqCenterInfo(daqCenter); // 웹 서버에 정보 전송

        ctx.fireChannelActive();
    }

    // 채널 비활성화 시 호출
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        String channelId = ctx.channel().id().asShortText(); // 채널ID 가져오기

        channelGroup.remove(channelId);

        log.info("==================================== Client connected: {} ====================================", channelId);

        // 채널 비활성화 정보 웹 서버에 전송
//        webChannelEventService.sendDaqCenterInfo(daqCenter);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in channel {}: {}", ctx.channel().id().asShortText(), cause.getMessage());
        ctx.close(); // 예외 발생 시 채널 닫기
    }
}
