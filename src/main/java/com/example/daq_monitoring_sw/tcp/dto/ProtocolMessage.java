package com.example.daq_monitoring_sw.tcp.dto;

import com.example.daq_monitoring_sw.tcp.common.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProtocolMessage {


    private Status status;
    private Status previousStatus;

    private String deviceType;
    private String deviceId;

    // read
    private String targetDeviceId;

    // common
    private int sensorCnt;
    private List<String> sensorList;
    private Map<String, Double> sensorDataMap; // TP01:±0000.00
    private String dataListJson;

    // timestamp
    private long timeStamp;
    private long cli_ts_ms;
    private long srv_ts_ms;
    private long latency;

}

