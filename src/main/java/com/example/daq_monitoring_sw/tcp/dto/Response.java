package com.example.daq_monitoring_sw.tcp.dto;

import java.util.List;

import com.example.daq_monitoring_sw.tcp.common.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Response {
    // common
    private String result;
    private Status rsType;
    private String targetDeviceId;
    private String deviceId;

    //private String targetDeviceId;
    private String message;

    private List<String> sensorList;

}
