package com.example.daq_monitoring_sw.web.controller;

import com.example.daq_monitoring_sw.tcp.util.DaqCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class WebDaqCenterController {

    @PostMapping("/daqcenter/update")
    public Mono<ResponseEntity<Void>> updateDaqcenter(@RequestBody DaqCenter daqCenter){
        // 받은 daqcenter 정보 처리
        // 데이터베이스 저장, 로그 작성, 모니터링 시스템 업데이트 등
        return Mono.just(ResponseEntity.ok().build());
    }
}
