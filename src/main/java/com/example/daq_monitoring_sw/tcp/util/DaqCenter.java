package com.example.daq_monitoring_sw.tcp.util;

import com.example.daq_monitoring_sw.tcp.common.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
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

    private String cliSentTime;
    private LocalTime servRecvTime;
    private String transDelay;

    // 이전 상태
    private Status previousStatus;
    // 클린업 여부
    private boolean cleanupDone = false;

}
