//package com.example.daq_monitoring_sw.tcp.config;
//
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.EventLoopGroup;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class NettyServerSocket {
//    private final ServerBootstrap serverBootstrap;
//    private final InetSocketAddress port;
//    private final EventLoopGroup bossGroup;
//    private final EventLoopGroup workerGroup;
//    private Channel serverChannel;
//
//    @PostConstruct
//    public void start() throws InterruptedException {
//        try{
//            log.info("Starting Netty server...");
//            ChannelFuture serverChannelFuture = serverBootstrap.bind(port).sync();
//            serverChannel = (Channel) serverChannelFuture.channel().closeFuture().sync().channel();
//            log.info("Server started on port: {}", port.getPort());
//        } catch (InterruptedException e){
//            log.error("Error starting Netty server: {}", e.getMessage(), e);
//            throw new RuntimeException(e);
//        }
//    }
//    @PreDestroy
//    public void stop() throws IOException, InterruptedException {
//        log.info("Stopping Netty server...");
//        if (serverChannel != null) {
//            serverChannel.close().sync();
//        }
//        bossGroup.shutdownGracefully();
//        workerGroup.shutdownGracefully();
//        log.info("Netty server stopped successfully.");
//    }
//}
