package com.example.daq_monitoring_sw.tcp.repository;

import com.example.daq_monitoring_sw.tcp.entity.DaqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaqCenterRepository extends JpaRepository<DaqEntity,Long> {
}
