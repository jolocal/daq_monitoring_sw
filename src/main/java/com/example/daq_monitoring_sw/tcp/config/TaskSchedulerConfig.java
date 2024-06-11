package com.example.daq_monitoring_sw.tcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);  // 스레드 풀의 크기 설정
        taskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler-");
        taskScheduler.initialize();
        return taskScheduler;
    }
}
