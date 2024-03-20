package com.example.daq_monitoring_sw.tcp.config;

import com.example.daq_monitoring_sw.tcp.handler.NettyChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Data
@ConfigurationProperties(prefix = "netty")
public class NettyProperties {

    private int port; // 서버 포트번호
    private int bossCount; // 보스 스레드 그룹의 크기
    private int workerCount; // 워커 스레드 그룹의 크기
    private boolean keepAlive;
    private int backlog;
}
