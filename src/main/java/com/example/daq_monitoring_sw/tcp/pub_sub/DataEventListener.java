package com.example.daq_monitoring_sw.tcp.pub_sub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public interface DataEventListener {
    void onDataReceived(Map<String,String> collectedData);
}
