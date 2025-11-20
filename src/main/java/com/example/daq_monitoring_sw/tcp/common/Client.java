package com.example.daq_monitoring_sw.tcp.common;

import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Client {

    private String id;  // uuid

    private String deviceType;
    private String deviceId;
    private String targetDeviceId;

    private Status status;
    private Status previousStatus;

    private List<String> sensorList;
    private int sensorCnt;

    private Long cli_ts_ms;
    private long srv_ts_ms;
    
    private LocalDateTime connectTime;
    private LocalDateTime lastActiveTime;

    // private long servRecvTime;

    // 클린업 여부
    private boolean cleanupDone = false;

    // // 특정 필드만 setter 메서드가 필요하다면 아래와 같이 개별적으로 커스텀할 수 있습니다.
    // public void setReceiveTime(String formattedReceiveTime) {
    //     this.servRecvTime = formattedReceiveTime;
    // }

}
