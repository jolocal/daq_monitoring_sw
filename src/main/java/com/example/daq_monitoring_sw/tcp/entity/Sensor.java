//package com.example.daq_monitoring_sw.tcp.entity;
//
//import io.hypersistence.utils.hibernate.type.json.JsonType;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.Type;
//
//import java.time.LocalDateTime;
//
//@Getter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Table(name = "sensor")
//public class Sensor {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "sensor_id")
//    private Long sensorId;
//
//    @ManyToOne
//    @JoinColumn(name = "daq_id")
//    private DaqEntity daqEntity;
//
//    @Column(name = "sensor_name")
//    private String sensorName;
//
//    @Column(name = "sensor_type")
//    private String sensorType;
//
//    @Column(name = "sensor_cnt")
//    private int sensorCnt;
//
//    @Type(JsonType.class)
//    @Column(name = "data_list", columnDefinition = "json")
//    private String dataList; // JSON
//
//    @Column(name = "cli_sent_time")
//    private LocalDateTime cliSentTime; // 클라이언트가 데이터를 보낸시간
//
//    @Column(name = "serv_recv_time")
//    private LocalDateTime servRecvTime; // 서버가 데이터를 받은 시간
//
//    @Column(name = "trans_delay")
//    private LocalDateTime transDelay; // 지연시간
//
//
//}
