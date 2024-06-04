package com.example.daq_monitoring_sw.tcp.common;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Client {

    private String clientId;

    private String daqName;
    private String readTo;

    private Status status;
    private Status previousStatus;

    private List<String> sensorList;
    private String sensorCnt;

    private LocalDateTime connectTime;
    private LocalDateTime lastActiveTime;

    // 클린업 여부
    private boolean cleanupDone = false;




}
