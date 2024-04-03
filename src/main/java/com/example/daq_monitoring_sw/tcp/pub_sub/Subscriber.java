package com.example.daq_monitoring_sw.tcp.pub_sub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class Subscriber {
    private Consumer<Queue<String>> consumer;
    private String channelId;
}
