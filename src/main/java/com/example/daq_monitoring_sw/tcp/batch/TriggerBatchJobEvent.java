package com.example.daq_monitoring_sw.tcp.batch;

import org.springframework.context.ApplicationEvent;

public class TriggerBatchJobEvent extends ApplicationEvent {
    public TriggerBatchJobEvent(Object source) {
        super(source);
    }
}
