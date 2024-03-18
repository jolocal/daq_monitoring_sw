package com.example.daq_monitoring_sw.tcp.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@RequiredArgsConstructor
@Component
public class TcpServer {
    private final ServerBootstrap serverBootstrap;
    private final InetSocketAddress port;
    private Channel serverChannel;

    public void start(){
        try{
            ChannelFuture serverChannelFuture = serverBootstrap.bind(port).sync();
            log.info("Server is started : port {}", port.getPort());
        } catch (InterruptedException e){
            log.info("InterruptedException : {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /*
    * 바인딩 된 NettyServer 종료
    */
    @PreDestroy
    public void stop() throws InterruptedException{
        if (serverChannel != null){
            serverChannel.close();
            serverChannel.parent().close();
        }
    }

}
