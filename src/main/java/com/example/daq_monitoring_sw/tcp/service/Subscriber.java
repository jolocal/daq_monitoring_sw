package com.example.daq_monitoring_sw.tcp.service;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class Subscriber {
    private String daqName;
    private Consumer<List<String>> dataHandler; // 데이터를 처리하는 Consumer
    private ChannelHandlerContext channelContext; // 구독자의 채널 상태를 확인

}
