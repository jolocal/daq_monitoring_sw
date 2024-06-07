package com.example.daq_monitoring_sw.tcp.entity;

import com.example.daq_monitoring_sw.tcp.common.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
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
    private int  daq_id;

    @Column(name = "daq_name")
    private String daqName;

    @Enumerated(EnumType.STRING)
    private Status status;

    /////////// sensor ///////////

    @Column(name = "sensor_cnt")
    private int sensorCnt;

    @Type(JsonType.class)
    @Column(name = "sensor_data_list", columnDefinition = "TEXT")
    private String dataList; // JSON

    @Column(name = "cli_sent_time")
    private String cliSentTime; // 클라이언트가 데이터를 보낸시간

    @Column(name = "serv_recv_time")
    private String servRecvTime; // 서버가 데이터를 받은 시간

    @Column(name = "trans_delay")
    private String transDelay; // 지연시간

    @Column(name = "db_save_time")
    private LocalDateTime dbSaveTime; // db 저장시간


}
