package com.example.daq_monitoring_sw.tcp.batch;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.DataException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

@EnableScheduling
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final JobLauncher jobLauncher; // 배치 작업을 실행하는 역할
    private final Job userRequestJob; // 실제로 실행될 배치 작업
    private final ThreadPoolTaskScheduler taskScheduler; // 스케줄링을 관리하는 스레드 풀
    private final Object lock = new Object(); // 동기화 블록을 위한 락 객체
    private boolean isScheduled = false; // 배치 작업이 예약되었는지 여부를 추적

    // 배치 작업을 예약하는 메서드
    public void triggerBatchJob() {
        synchronized (lock) {
            if (!isScheduled) {  // 배치 작업이 이미 예약되지 않은 경우에만 예약
                log.info(">>>>>>>>>>>>>>>>>>>> 배치 작업 예약됨 - 시간: {}", new Date(System.currentTimeMillis())); // 배치 작업 예약 로그
                isScheduled = true;  // 배치 작업을 예약 상태로 설정
                taskScheduler.schedule(() -> {
                    try {
                        runBatchJob();
                    } catch (Exception e) {
                        log.error(">>>>>>>>>>>>>>>>>>>> 배치 작업 실행 오류", e);
                    }
                }, new Date(System.currentTimeMillis() + 60000));  // 1분 후에 배치 작업 실행 예약
            }
        }
    }

    // 1분마다 실행되는 배치 작업 메서드
    @Scheduled(fixedRate = 60000)
    public void runBatchJob() throws Exception {
        synchronized (lock) {
            if (!isScheduled) return; // 배치 작업이 예약되지 않았으면 실행하지 않음

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())  // 현재 시간을 매개변수로 추가
                    .toJobParameters();

            log.info(">>>>>>>>>>>>>>>>>>>> 배치 작업 시작 - 시간: {}", new Date(System.currentTimeMillis())); // 배치 작업 시작 로그
            jobLauncher.run(userRequestJob, jobParameters); // 배치 작업 실행
            log.info(">>>>>>>>>>>>>>>>>>>> 배치 작업 실행됨 - 매개변수: {}", jobParameters); // 배치 작업 실행 로그

            isScheduled = true; // 주기적으로 배치 작업을 계속 실행

            log.info("배치 작업 실행됨");
        }
    }

}
