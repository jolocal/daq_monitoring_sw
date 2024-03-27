package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.pub_sub.DataEventListener;
import com.example.daq_monitoring_sw.tcp.pub_sub.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ListenerService {
    private final Map<String, Listener> listenerGroup = new ConcurrentHashMap<>();

    // 리스너 등록
    public void addListener(String channelId, Listener listener) {
        listenerGroup.putIfAbsent(channelId, listener);
    }


}
