package com.example.daq_monitoring_sw.tcp.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobEventListener {
    private final SchedulerConfig schedulerConfig;

    @EventListener
    public void handleTriggerBatchJobEvent(TriggerBatchJobEvent event) {
        schedulerConfig.triggerBatchJob();
    }

}
