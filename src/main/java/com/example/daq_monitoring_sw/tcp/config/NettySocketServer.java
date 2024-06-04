package com.example.daq_monitoring_sw.tcp.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@RequiredArgsConstructor
@Component
public class NettySocketServer {
    private final ServerBootstrap serverBootstrap;
    private final InetSocketAddress port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start(){
        try{
            log.info("==================================================  Netty 서버를 시작중... ================================================== ");
            // 서버 소켓을 특정 포트에 바인딩하고, 비동기 작업 결과를 기다림
            ChannelFuture serverChannelFuture = serverBootstrap.bind(port).sync();

            // 바인딩 결과로 생성된 서버 채널을 변수에 할당
            serverChannel = serverChannelFuture.channel();
            log.info("==================================================  Netty 서버 [ 포트: {} ] 에서 시작 ================================================== ",port.getPort());
        } catch (InterruptedException e){
            log.error("Netty 서버 시작 중 오류 발생: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    /*
    * 바인딩 된 NettySocketServer 종료
    */
    @PreDestroy
    public void stop() throws InterruptedException{
        log.info("================================================== Netty 서버를 종료합니다... ================================================== ");
        if (serverChannel != null){
            serverChannel.close().sync();
            log.info("서버 채널이 성공적으로 닫혔습니다.");
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("==================================================  Netty 서버가 성공적으로 종료되었습니다. ==================================================");
    }

}
