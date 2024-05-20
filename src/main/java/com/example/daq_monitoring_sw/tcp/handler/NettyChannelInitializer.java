package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.codec.ErrorResEncdoer;
import com.example.daq_monitoring_sw.tcp.codec.ReqDecoder;
import com.example.daq_monitoring_sw.tcp.codec.SensorDataResEncoder;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
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
    private final ChannelManagerHandler channelManagerHandler;
    private final ChannelRepository channelRepository;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // logging
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));

        // channel 관리 - 활성화/비활성화 및 예외 처리
        pipeline.addLast("channelManagerHandler", channelManagerHandler);

        // codec
        pipeline.addLast(new ReqDecoder(channelRepository));
        pipeline.addLast(new SensorDataResEncoder());
        pipeline.addLast(new ErrorResEncdoer());

        // dataHandler
        pipeline.addLast("nettyChannelDataHandler", nettyChannelDataHandler);
    }
}
