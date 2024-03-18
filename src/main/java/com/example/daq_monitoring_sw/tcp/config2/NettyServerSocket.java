//package com.example.daq_monitoring_sw.tcp.config2;
//
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.ChannelFuture;
//import jakarta.annotation.PreDestroy;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.channels.Channel;
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class NettyServerSocket {
//
//    private final ServerBootstrap serverBootstrap;
//    private final InetSocketAddress tcpPort;
//    private Channel serverChannel;
//
//    public void start() throws InterruptedException {
//        try{
//            ChannelFuture serverChannelFuture = serverBootstrap.bind(tcpPort).sync();
//            serverChannel = (Channel) serverChannelFuture.channel().closeFuture().sync().channel();
//        } catch (InterruptedException e){
//            throw new RuntimeException(e);
//        }
//    }
//
//    @PreDestroy
//    public void stop() throws IOException {
//        if (serverChannel != null) {
//            serverChannel.close();
////            serverChannel.parent().closeFuture;
//        }
//    }
//}
