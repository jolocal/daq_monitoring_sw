package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
public class UserRequest {
    private String daqId;
    private String sensorId;
    private AtomicInteger sequence;
    private List<String> dataList;

}
