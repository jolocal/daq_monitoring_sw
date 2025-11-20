package com.example.daq_monitoring_sw.tcp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {

    IN("INIT"),
    IS("INIT_SUCCESS"),
    IE("INIT_ERROR"),
    WD("WRITE"),
    RD("READ"),
    ST("STOP"),
    DC("DATA_TO_CLIENT"),
    

    RQ("REQUEST_TO_SERVER"),
    RS("RESPONSE_TO_CLIENT"),

    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED"),
    RECONNECTING("RECONNECTING"),

    E("ERROR")
    ;

    private String desc;
}
