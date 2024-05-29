package com.example.daq_monitoring_sw.tcp.entity;

import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daqcenter")
public class DaqCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long daq_id;
    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(mappedBy = "daqCenter")
    List<Sensor> sensor;

    public DaqCenter daqCenterToEntity(UserRequest userRequest){
        return DaqCenter.builder()
                .name(userRequest.getDaqId())
                .status(userRequest.getStatus())
                .sensor((List<Sensor>) userRequest.getParsedSensorData())
                .build();
    }

}
