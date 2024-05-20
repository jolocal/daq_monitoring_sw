package com.example.daq_monitoring_sw.tcp.pub_sub;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class Subscriber {
    private Consumer<List<String>> dataHandler; // 데이터를 처리하는 Consumer
    private String channelId; // 채널 식별자
    private ChannelHandlerContext channelContext; // 구독자의 채널 상태를 확인

}
