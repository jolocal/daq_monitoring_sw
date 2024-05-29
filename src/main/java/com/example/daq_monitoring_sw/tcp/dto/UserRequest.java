package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequest {

    // write
    private Status status;
    private String daqId;

    // read
    private String channelId;
    private String readTo;
    private List<String> resDataList;

    // common
    private Integer sensorCnt;
    private List<String> sensorIdsOrder;
    private Map<String, String> parsedSensorData; // TP01:+000.0

    // timestamp
    private String cliSentTime; // 클라이언트가 데이터를 보낸 시간
    private String servRecvTime; // 서버가 데이터를 받은 시간
    private String transDelay; // 지연시간

    // 이전 상태
    private Status previousStatus;

}

