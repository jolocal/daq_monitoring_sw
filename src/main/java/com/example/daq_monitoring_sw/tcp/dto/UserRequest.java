package com.example.daq_monitoring_sw.tcp.dto;

import com.example.daq_monitoring_sw.tcp.common.Status;
import io.netty.channel.ChannelId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String daqName;

    // read
    private String readTo;

    // common
    private String sensorCnt;
    private List<String> sensorList;
    private Map<String, String> sensorDataMap; // TP01:+000.0
    private String dataListJson;

    // timestamp
    private String cliSentTime; // 클라이언트가 데이터를 보낸 시간
    private String servRecvTime; // 서버가 데이터를 받은 시간
    private String transDelay; // 지연시간

    // 이전 상태
    private Status previousStatus;

}

