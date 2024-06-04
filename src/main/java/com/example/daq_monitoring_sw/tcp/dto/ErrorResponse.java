package com.example.daq_monitoring_sw.tcp.dto;

import com.example.daq_monitoring_sw.tcp.common.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private Status status;
    private String msg;
}
