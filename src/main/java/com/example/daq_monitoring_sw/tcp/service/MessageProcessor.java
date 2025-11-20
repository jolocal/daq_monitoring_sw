package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.batch.TriggerBatchJobEvent;
import com.example.daq_monitoring_sw.tcp.common.JsonConverter;
import com.example.daq_monitoring_sw.tcp.dto.ProtocolMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final JsonConverter jsonConverter;
    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private final ConcurrentLinkedQueue<ProtocolMessage> messageQueue = new ConcurrentLinkedQueue<>();

    // ST 종료 이후에는 더 이상 큐에 쓰지 않기 위한 플래그 (전역 기준)
    private volatile boolean writingEnabled = true;

    /**
     * WD 측정 시작(ST 시작 등) 시 호출하여 큐 적재를 허용.
     */
    public void enableWriting() {
        writingEnabled = true;
        log.info("  ▷ [MessageProcessor] writingEnabled = true");
    }

    /**
     * WD 측정 종료(ST 종료, 채널 종료 등) 시 호출하여
     * 더 이상 큐에 데이터를 적재하지 않도록 제어.
     */
    public void disableWriting() {
        writingEnabled = false;
        log.info("  ▷ [MessageProcessor] writingEnabled = false");
    }

    // 오차율 계산, 센서데이터 json 변환 -> 큐 추가
    public void process(ProtocolMessage message) {
        // ST 종료 이후에는 큐에 적재하지 않음
        if (!writingEnabled) {
            log.info("  ▷ [MessageProcessor] writing disabled, 큐에 저장하지 않음");
            return;
        }

        processTimestamp(message); // 오차율 계산
        convertToJson(message); // sensorDataMap을 Json 변환
        messageQueue.add(message);
        log.info("  ▷ [MessageProcessor] ProtocolMessage: {}", message);
        log.info("  ▷ [MessageProcessor] 큐 크기: {}", messageQueue.size());
        
        // 큐가 처음 채워질 때, TriggerBatchJobEvent 퍼블리시
        // 이벤트는 BatchJobEventListener가 받고 SchedulerConfig.triggerBatchJob()을 호출하여 1분 후 배치 실행을 예약
        if (messageQueue.size() == 1) {
            log.info("  ▷ [MessageProcessor] 큐에 첫 번째 ProtocolMessage 추가됨, 배치 작업 이벤트 트리거");
            eventPublisher.publishEvent(new TriggerBatchJobEvent(this));
        }
        
    }

    // 시간 구하기 (클라이언트 전송 시간 vs 서버 수신 시간 -> 지연 시간 계산)
    private void processTimestamp(ProtocolMessage message){

        Long cli_ts_ms = message.getCli_ts_ms();
        Long srv_ts_ms = message.getSrv_ts_ms();
        Long latency = srv_ts_ms - cli_ts_ms;
        
        message.setLatency(latency);
        
        log.info("  ▷ [MessageProcessor] 오차율 계산 완료: {}", latency);
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        // // String 형식의 시간을 LocalTime으로 변환
        // LocalTime cliSentTime = LocalTime .parse(userRequest.getCliSentTime(), formatter);
        // LocalTime servRecvTime = LocalTime.parse(userRequest.getServRecvTime(), formatter);

        // Duration delay = Duration.between(cliSentTime, servRecvTime);
        // String delayFormatted = formatDuration(delay);

        // userRequest.setTransDelay(delayFormatted);
        // log.info("지연 시간: {}", delayFormatted);
    }

    // private String formatDuration(Duration duration) {
    //     long hours = duration.toHours();
    //     long minutes = duration.toMinutes() % 60;
    //     long seconds = duration.getSeconds() % 60;
    //     long millis = duration.toMillis() % 1000;
    //     return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    // }
    // private LocalTime formatLocalTime(String timeStr) {
    //     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    //     return LocalTime.parse(timeStr, formatter);
    // }


    // sensorDataMap을 Json 변환
    private void convertToJson(ProtocolMessage message) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> Convert To Json... ");
        try{
            String dataListJson = jsonConverter.toJson(message.getSensorDataMap());
            message.setDataListJson(dataListJson);
            log.info("  ▷ [MessageProcessor] JSON 변환 완료: {}", message);
        
        } catch (JsonProcessingException e) {
            log.error("  ▷ [MessageProcessor] JSON 변환 오류: {}", e.getMessage(), e);
        }
    }
}
