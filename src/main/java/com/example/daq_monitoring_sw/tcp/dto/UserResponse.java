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
public class UserResponse {

    private String daqId;
    private Status status;
    private String channelId;
    private String readTo;
    private Queue<String> resDataList;
    private String timeStamp;

    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String,String> parsedSensorData;
}
