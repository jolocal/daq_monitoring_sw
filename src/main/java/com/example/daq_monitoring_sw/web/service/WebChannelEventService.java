package com.example.daq_monitoring_sw.web.service;

import com.example.daq_monitoring_sw.tcp.util.DaqCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebChannelEventService {
    private WebClient webClient;

    public void sendDaqCenterInfo(DaqCenter daqCenter){
        log.info("웹 서버로 HTTP 요청 보내기");
        // 웹 서버로 HTTP 요청 보내기
        webClient.post()
                .uri("/daqcenter/update")
                .bodyValue(daqCenter)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(); // 비동기적으로 요청을 보냅니다.
    }


 /*   public void channelActivated(String channelId) {
        sendChannelStatus(channelId, "active");
    }
    public void channelDeactivated(String channelId) {
        sendChannelStatus(channelId, "inactive");
    }*/
}
