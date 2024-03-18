//package com.example.daq_monitoring_sw.tcp.config;
//
//import com.example.daq_monitoring_sw.tcp.handler.NettyChannelInitializer;
//import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.util.concurrent.DefaultEventExecutorGroup;
//import io.netty.util.concurrent.EventExecutorGroup;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.net.InetSocketAddress;
//
//@Configuration
//@RequiredArgsConstructor
//@EnableConfigurationProperties(NettyProperties.class)
//public class NettyConfiguration {
//
//    private final NettyProperties nettyProperties;
//    @Bean
//    public EventExecutorGroup eventExecutorGroup() {
//        return new DefaultEventExecutorGroup(10);
//    }
//
//    // group(), channel(), handler(), childHandler() 메소드를 사용하여 각각 이벤트 루프 그룹, 채널타입, 로깅핸들러, 채널 초기화 핸들러 등 설정
//    @Bean(name = "serverBootstrap")
//    public ServerBootstrap bootstrap(NettyChannelInitializer channelInitializer) {
//        ServerBootstrap b = new ServerBootstrap(); // 네티에서 서버 채널을 설정하고 초기화하는 데 사용
//        b.group(bossGroup(), workerGroup())
//                .channel(NioServerSocketChannel.class) // 서버 소켓 채널의 타입 (비동기 I/O (NIO)를 사용하는 서버 소켓 채널)
////                .handler(new LoggingHandler(LogLevel.DEBUG))
//                .childHandler(channelInitializer); // 각 클라이언트 채널이 생성될 때 호출, 채널 파이프라인에 다양한 핸들러(예: 데이터 처리, 인코딩/디코딩, SSL/TLS 설정 등)를 추가.
//        b.option(ChannelOption.SO_BACKLOG, nettyProperties.getBacklog()); // 서버 소켓 채널의 옵션 설정
//        b.childOption(ChannelOption.SO_KEEPALIVE,true);
//        // SO_BACKLOG: 동시에 처리할 수 있는 연결 요청의 최대 큐 크기 / nettyProperties.getBacklog()는 이 값을 구성 파일로부터 가져옴
//        return b;
//    }
//
//    // 클라이언트의 연결 요청을 수신하고 수락합니다. 주로 연결 수립에 관련된 작업을 처리
//    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
//    public NioEventLoopGroup bossGroup() {
//        return new NioEventLoopGroup(nettyProperties.getBossCount());
//    }
//
//    // 연결된 채널의 I/O 작업을 처리합니다. 데이터 읽기, 쓰기 및 연결 유지 관리가 이 그룹의 주요 작업
//    @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
//    public NioEventLoopGroup workerGroup() {
//        return new NioEventLoopGroup(nettyProperties.getWorkerCount());
//    }
//
//    // 서버가 바인딩 될 tcp 소켓 주소 정의
//    @Bean
//    public InetSocketAddress tcpSocketAddress() {
//        return new InetSocketAddress(nettyProperties.getTcpPort());
//    }
//
//    // 연결된 채널들을 관리하기 위한 레포지토리
//    @Bean
//    public ChannelRepository channelRepository() {
//        return new ChannelRepository();
//    }
//
//
//}
