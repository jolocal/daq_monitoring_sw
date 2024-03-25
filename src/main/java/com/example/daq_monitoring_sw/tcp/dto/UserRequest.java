package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class UserRequest {

    private Status status;
    private String daqId;
    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String, String> parsedSensorData; // TP01:+000.0
}

