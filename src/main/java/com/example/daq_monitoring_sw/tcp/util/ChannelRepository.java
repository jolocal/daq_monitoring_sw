package com.example.daq_monitoring_sw.tcp.util;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChannelRepository {
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    // 채널 추가
    public void addChannel(String channelId, Channel channel) {
        channelMap.put(channelId, channel);
    }

    // 채널 제거
    public void removeChannel(String channelId) {
        channelMap.remove(channelId);
    }

    // 채널 가져오기
    public Channel getChannel(String channelId) {
        return channelMap.get(channelId);
    }

    // 특정 채널에 메시지 보내기
    public void sendMessageToChannel(String channelId, Object message) {
        Channel channel = getChannel(channelId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

}
