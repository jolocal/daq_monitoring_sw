package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@Data
@AllArgsConstructor
@Builder
public class UserRequest {

    // write
    private Status status;
    private String daqId;

    // read
    private String channelId;
    private String readTo;
    private List<String> resDataList;

    // common
    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String, String> parsedSensorData; // TP01:+000.0
    private String timeStamp;
}

