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

    private String clientId;

    private String daqName;
    private String readTo;

    private Status status;
    private Status previousStatus;

    private List<String> sensorList;
    private String sensorCnt;
    private String cliSentTime;

    private LocalDateTime connectTime;
    private LocalDateTime lastActiveTime;

    private String servRecvTime;

    // 클린업 여부
    private boolean cleanupDone = false;

    // 특정 필드만 setter 메서드가 필요하다면 아래와 같이 개별적으로 커스텀할 수 있습니다.
    public void setReceiveTime(String formattedReceiveTime) {
        this.servRecvTime = formattedReceiveTime;
    }

}
