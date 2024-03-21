package com.example.daq_monitoring_sw.tcp.util;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

@Slf4j
@Component
public class ChannelRepository extends ChannelInboundHandlerAdapter {
    private static final Map<String, ChannelHandlerContext> channelGroup = new ConcurrentHashMap<>();

    // 채널이 활성화 되었을 때 실행
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String daqId = retrieveDaqId(ctx);
        channelGroup.put(daqId,ctx);
        log.info("Channel active: {}" , ctx.channel().id().asLongText());

        super.channelActive(ctx);
    }

    // 채널이 비활성화되었을 때 실행
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String daqId = retrieveDaqId(ctx);
        channelGroup.remove(daqId,ctx);
        log.info("Channel inactive: " + ctx.channel().id().asLongText());

        // TODO: 리소스 정리
        ctx.close(); // 채널 닫기

        super.channelInactive(ctx);
    }

    // 채널의 상태에 접근하는 메서드
    public DaqCenter getChannelDaqCenter(ChannelHandlerContext ctx){
        return ctx.channel().attr(DAQ_CENTER_KEY).get();
    }

    // 모든 연결된 채널에 메시지를 전송
    public static void sendMessageToAll(String message){
        for (ChannelHandlerContext ctx: channelGroup.values()){
            ctx.writeAndFlush(message);
        }
    }

    // daqId 추출
    private String retrieveDaqId(ChannelHandlerContext ctx){
        DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        return daqCenter.getDaqId();
    }
}
