package com.example.daq_monitoring_sw.tcp.dto;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DcPayload {

    private String targetDeviceId;
    private int sensorCnt;
    private long ts_ms;
    private List<Double> values;
    //private Map<String, Double> sensorDataMap;

    // private List<String> 

}
