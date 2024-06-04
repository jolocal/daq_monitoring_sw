package com.example.daq_monitoring_sw.tcp.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class Listener {
    private  String channelId;
    private  String readTo;
    private  List<String> sensorList;
}
