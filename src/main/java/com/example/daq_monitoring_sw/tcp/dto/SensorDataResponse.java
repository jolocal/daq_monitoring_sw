package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class SensorDataResponse {

    private String daqId;
    private Status status;
    private String channelId;
    private String readTo;
    private List<String> resDataList;
    private String timeStamp;

    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String,String> parsedSensorData;

    String msg;
}
