package com.example.daq_monitoring_sw.tcp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Payloads {
    private int sensorCnt;
    private Long timeStamp;
    private List<String> sensorList;
    private List<Double> values;
}
