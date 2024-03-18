//package com.example.daq_monitoring_sw.tcp.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.ApplicationListener;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class TcpStartupTask implements ApplicationListener<ApplicationReadyEvent> {
//    // NettyServerSocket 의존성 주입
//    private final NettyServerSocket nettyServerSocket;
//
//    @Override
//    public void onApplicationEvent(ApplicationReadyEvent event) {
//        try {
//            nettyServerSocket.start();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
