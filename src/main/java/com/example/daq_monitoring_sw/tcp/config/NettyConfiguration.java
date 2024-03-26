package com.example.daq_monitoring_sw.tcp.config;

import com.example.daq_monitoring_sw.tcp.handler.NettyChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NettyProperties.class)
public class NettyConfiguration {

    private final NettyProperties nettyProperties;

    @Bean(name = "serverBootstrap")
    public ServerBootstrap bootstrap(NettyChannelInitializer channelInitializer) {
        ServerBootstrap b = new ServerBootstrap(); // 네티에서 서버 채널을 설정하고 초기화하는 데 사용
        b.group(bossGroup(), workerGroup())
                .channel(NioServerSocketChannel.class) // 서버 소켓 채널의 타입 (비동기 I/O (NIO)를 사용하는 서버 소켓 채널)
                .childHandler(channelInitializer); // 각 클라이언트 채널이 생성될 때 호출, 채널 파이프라인에 다양한 핸들러(예: 데이터 처리, 인코딩/디코딩, SSL/TLS 설정 등)를 추가.
        b.option(ChannelOption.SO_BACKLOG, nettyProperties.getBacklog()); // 백로그는 동시에 처리할 수 있는 대기 중인 연결 요청의 최대 수
        b.option(ChannelOption.AUTO_CLOSE, true); // 연결이 끝나면 자동으로 채널을 닫을 것인지를 설정
        b.option(ChannelOption.SO_REUSEADDR, true); // 소켓 주소를 재사용할 수 있게 하는 옵션 서버 재시작 시 주소가 이미 사용 중 오류를 방지하는데 도움
        b.childOption(ChannelOption.SO_KEEPALIVE, true); // client 소켓이 활성 상태인지 정기적으로 체크
        b.childOption(ChannelOption.TCP_NODELAY, true); // Nagle 알고리즘 비활성화, 데이터가 즉시 전송되도록 네트워크 지연을 최소화 (실시간 데이터 전송 시 중요)
        return b;
    }

    // 클라이언트의 연결 요청을 수신하고 수락합니다. 주로 연결 수립에 관련된 작업을 처리
    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup bossGroup() {
        return new NioEventLoopGroup(nettyProperties.getBossCount());
    }

    // 연결된 채널의 I/O 작업을 처리합니다. 데이터 읽기, 쓰기 및 연결 유지 관리가 이 그룹의 주요 작업
    @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup workerGroup() {
        return new NioEventLoopGroup(nettyProperties.getWorkerCount());
    }

    // 서버가 바인딩 될 tcp 소켓 주소 정의
    @Bean
    public InetSocketAddress tcpSocketAddress() {
        return new InetSocketAddress(nettyProperties.getPort());
    }

    // 연결된 채널들을 관리하기 위한 레포지토리
//    @Bean
//    public ChannelRepository channelRepository() {
//        return new ChannelRepository();
//    }


}
