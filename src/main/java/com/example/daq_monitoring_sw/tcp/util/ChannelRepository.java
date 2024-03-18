package com.example.daq_monitoring_sw.tcp.util;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChannelRepository {
    private ConcurrentMap<String, Channel> channelCache = new ConcurrentHashMap<>();
    private AtomicInteger userNameCounter = new AtomicInteger(0); // 사용자 이름 카운터

    public void put(String userName, Channel value) {
        channelCache.put(userName, value);
    }

    public Channel get(String userName){
        return channelCache.get(userName);
    }

    public void remove(String userName){
        this.channelCache.remove(userName);
    }
    public int size(){
        return this.channelCache.size();
    }

    public String createNextUserName(){
        return "DAQ_CENTER_"+userNameCounter.incrementAndGet();
    }
}
