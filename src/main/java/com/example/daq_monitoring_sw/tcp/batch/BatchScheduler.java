package com.example.daq_monitoring_sw.tcp.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job job;
    private final AtomicBoolean isSchedulerRunning = new AtomicBoolean(false);

    public void startScheduler() {
        if (!isSchedulerRunning.getAndSet(true)) {
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> Starting scheduler...");
            scheduleBatchJob();
        }
    }

    @Scheduled(cron = "0 * * * * *") // 1분
    public void scheduleBatchJob() {
        try {
            jobLauncher.run(job, new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            log.error("Error running job", e);
        }
    }
}
