package com.example.daq_monitoring_sw.tcp.service;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

import com.example.daq_monitoring_sw.tcp.dto.Payloads;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subscriber {
    private String targetDeviceId;
    private String subscriberId;

    // private String deviceId; // 구독자 daq이름
    private Consumer<List<Payloads>> dataHandler; // 데이터를 처리하는 Consumer
    private ChannelHandlerContext channelContext; // 구독자의 채널 상태를 확인

}
