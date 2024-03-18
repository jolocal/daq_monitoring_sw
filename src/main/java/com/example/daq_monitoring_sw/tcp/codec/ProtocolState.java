package com.example.daq_monitoring_sw.tcp.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum ProtocolState {
    STX((byte)0x02),

    TOTAL_LENGHT(null),
    COMMAND(null),
    DAQ_ID(null),
    SENSOR_CNT(null),
    SENSOR_ID(null),

    ETX((byte)0x03);

    private final Byte value;

}
