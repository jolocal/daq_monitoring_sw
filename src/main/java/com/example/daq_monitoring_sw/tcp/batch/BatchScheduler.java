package com.example.daq_monitoring_sw.tcp.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.example.daq_monitoring_sw.tcp.service.MessageProcessor;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {


    private static final long BATCH_INTERVAL_MS = 2000L;// 2초 주기 (400msg/s * 2초 = 최대 800개)
    private static final int MAX_IDLE_TICKS = 15;      // 2초 * 15 = 30초 동안 큐가 비어 있으면 배치 중단

    private final JobLauncher jobLauncher; // 배치Job 실행기
    private final Job messageJob; // 메시지 적재용 배치 Job
    private final ThreadPoolTaskScheduler taskScheduler;
    private final MessageProcessor messageProcessor; // 큐 상태 확인용

    // 현재 배치가 주기적으로 돌고 있는지 여부
    private ScheduledFuture<?> batchFuture;
    private boolean batchActive = false;
    private int idleCount = 0; // 연속 idle 횟수

    /**
     * 큐에 첫 메시지가 들어왔을 때(TriggerBatchJobEvent 수신 시)
     * 배치 작업을 1초 후부터 BATCH_INTERVAL_MS 간격으로 주기 실행 시작.
     * 이미 동작 중이면 재시작하지 않는다.
     */
    public void triggerBatchJob() {
        synchronized (this) {
            if (batchActive) {
                log.info("  ▷ [Scheduler] 배치가 이미 동작 중이므로 재시작하지 않음");
                return;
            }

            log.info("  ▷ [Scheduler] 배치 작업 주기 실행 시작 예약 (1초 후, 간격: {} ms)", BATCH_INTERVAL_MS);
            batchActive = true;

            batchFuture = taskScheduler.scheduleAtFixedRate(
                    this::runBatchJob,
                    new Date(System.currentTimeMillis() + 1000),
                    BATCH_INTERVAL_MS
            );
        }
    }

    /**
     * 주기적으로 실행되는 배치 Job 런처.
     * 큐가 비어 있으면 Job 실행 자체를 생략하고 idle 횟수를 카운트한다.
     */
    private void runBatchJob() {
        int queueSize = messageProcessor.getMessageQueue().size();

        // 큐가 비어 있을 경우: idle 카운트 증가 후, 일정 시간 이상 비어 있으면 배치 중단
        if (queueSize == 0) {
            idleCount++;
            log.info("  ▷ [Scheduler] 큐 비어 있음 - idleCount: {}", idleCount);

            if (idleCount >= MAX_IDLE_TICKS) {
                stopBatchDueToIdle();
            }
            return;
        }

        // 큐에 데이터가 있으면 idle 카운트 리셋
        idleCount = 0;

        var params = new JobParametersBuilder()
                .addLong("startAt", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info("  ▷ [Scheduler] 배치 작업 시작 - params: {}", params);
            jobLauncher.run(messageJob, params);
            log.info("  ▷ [Scheduler] 배치 작업 완료");
        } catch (Exception e) {
            log.error("  ▷ [Scheduler] 배치 작업 실행 실패 - params: {}", params, e);
        }
    }

    /**
     * 큐가 장시간 비어 있는 경우 배치 주기 실행을 중단.
     */
    private void stopBatchDueToIdle() {
        synchronized (this) {
            if (batchFuture != null) {
                batchFuture.cancel(false);
                batchFuture = null;
            }
            batchActive = false;
            idleCount = 0;
            log.info("  ▷ [Scheduler] 큐가 장시간 비어 있어 배치 주기 실행 중단");
        }
    }

}

