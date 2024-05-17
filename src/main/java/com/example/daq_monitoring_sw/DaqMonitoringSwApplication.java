package com.example.daq_monitoring_sw;

import com.example.daq_monitoring_sw.tcp.config.NettyServerSocket;
import com.example.daq_monitoring_sw.tcp.config.TcpServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DaqMonitoringSwApplication {

	public static void main(String[] args) {
		SpringApplication.run(DaqMonitoringSwApplication.class, args);
	}



}
