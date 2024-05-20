package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class DaqCenter {

    // write
    private String daqId;
    private Status status;

    // read
    private String channelId;
    private String readTo;

    // common
    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String, String> parsedSensorData;
    private String timeStamp;

    // 이전 상태
    private Status previousStatus;
    // 클린업 여부
    private boolean cleanupDone = false;

}
