package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.codec.ErrorResEncdoer;
import com.example.daq_monitoring_sw.tcp.codec.MessageDecoder;
import com.example.daq_monitoring_sw.tcp.codec.SensorDataResEncoder;
import com.example.daq_monitoring_sw.tcp.common.ChannelManager;
import com.example.daq_monitoring_sw.tcp.codec.DcPayloadEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ChannelDataHandler nettyChannelDataHandler;
    private final ConnectionHandler connectionHandler;
    private final ChannelManager channelManager;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // logging - 원시 바이트 패킷 로깅을 위해 주석 해제 (INFO 레벨로 변경하여 운영 환경에서도 노출)
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        // channel 관리 - 활성화/비활성화 및 예외 처리
        pipeline.addLast("channelManagerHandler", connectionHandler);
        // codec
        pipeline.addLast(new MessageDecoder(channelManager));
        pipeline.addLast(new SensorDataResEncoder());
        pipeline.addLast(new DcPayloadEncoder());
        pipeline.addLast(new ErrorResEncdoer());
        // dataHandler
        pipeline.addLast("nettyChannelDataHandler", nettyChannelDataHandler);
    }
}
