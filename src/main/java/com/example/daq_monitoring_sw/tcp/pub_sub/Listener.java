package com.example.daq_monitoring_sw.tcp.pub_sub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class Listener {
    private final String daqId;
    private final List<String> sensorList;
    private final DataEventListener listener;
}
