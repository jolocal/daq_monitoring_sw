package com.example.daq_monitoring_sw.tcp.repository;

import com.example.daq_monitoring_sw.tcp.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorRepository extends JpaRepository<Sensor,Long> {
}
