package com.example.daq_monitoring_sw.tcp.pub_sub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ScheduleData {
//
//    private final ProcessingDataManager dataManager;
//
//    public void scheduleDataSending(){
//        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//        executorService.scheduleAtFixedRate(() -> {
//            dataManager.sensorData
//        })
//    }
//}
