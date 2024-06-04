package com.example.daq_monitoring_sw.tcp.dto;

import com.example.daq_monitoring_sw.tcp.common.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class RqInfoRes {

    private String daqName;
    private Status status;
    private String readTo;
    private List<String> packetList;
    private String cliSentTime;

    private String sensorCnt;
    private List<String> sensorList;
    private Map<String,String> parsedSensorData;

    String msg;
}
