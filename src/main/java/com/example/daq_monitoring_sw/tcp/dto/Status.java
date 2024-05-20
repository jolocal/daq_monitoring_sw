package com.example.daq_monitoring_sw.tcp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {

    IN("INIT"),
    WD("WRITE"),
    RD("READ"),
    ST("STOP"),

    RQ("REQUEST_TO_SERVER"),
    RS("RESPONSE_TO_CLIENT"),

    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED"),
    RECONNECTING("RECONNECTING"),

    ER("ERROR")
    ;

    private String desc;
}
