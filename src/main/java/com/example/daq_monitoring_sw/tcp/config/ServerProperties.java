package com.example.daq_monitoring_sw.tcp.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "server")
public class ServerProperties {

    private String host; // 서버 포트번호
    private int port; // 서버 포트번호
}
