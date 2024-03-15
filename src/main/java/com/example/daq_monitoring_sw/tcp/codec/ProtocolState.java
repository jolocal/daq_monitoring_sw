package com.example.daq_monitoring_sw.tcp.codec;

import lombok.Getter;

@Getter
public enum ProtocolState {
    STX,
    TOTAL_LENGHT,
    COMMAND,
    DAQ_ID,
    SENSOR_CNT,
    SENSOR_ID,
    ETX
}
