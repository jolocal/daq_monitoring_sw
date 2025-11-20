package com.example.daq_monitoring_sw.tcp.entity;

import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.ProtocolMessage;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daqcenter")
public class DaqEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @Column(name = "device_id")
    private String deviceID;

    @Column(name = "status")
    private String status;

    /////////// sensor ///////////

    @Column(name = "sensor_cnt")
    private int sensorCnt;

    @Type(JsonType.class)
    @Column(name = "sensor_data_list", columnDefinition = "TEXT")
    private String dataList; // JSON

    @Column(name = "cli_sent_time")
    private String cliSentTime; // 클라이언트가 데이터를 보낸시간 (yyyyMMddHHmmssSSS)

    @Column(name = "srv_recv_time")
    private String srvRecvTime; // 서버가 데이터를 받은 시간 (yyyyMMddHHmmssSSS)

    @Column(name = "latency")
    private long latency; // 지연시간

    @Column(name = "latency_str")
    private String latencyStr; // 지연시간 포맷 (HH:mm:ss.SSS)

    @Column(name = "created_at")
    private LocalDateTime createdAt; // db 저장시간


}
