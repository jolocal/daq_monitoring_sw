package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.service.DataService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@io.netty.channel.ChannelHandler.Sharable
@RequiredArgsConstructor
public class ChannelHandler extends ChannelInboundHandlerAdapter {

    private final DataService dataService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disConnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channelRead called");
        try{
            String receivedMsg = (String) msg;
            // 데이터 처리 로직
            // dataService.(ctx,receivedMessage)
            log.info("received message: {}", receivedMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* 예외 발생시 클라이언트와의 연결을 닫고 예외정보 출력 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
