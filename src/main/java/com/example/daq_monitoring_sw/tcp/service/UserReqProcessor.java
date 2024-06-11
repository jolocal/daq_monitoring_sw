package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.batch.SchedulerConfig;
import com.example.daq_monitoring_sw.tcp.batch.TriggerBatchJobEvent;
import com.example.daq_monitoring_sw.tcp.common.JsonConverter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReqProcessor {

    private final JsonConverter jsonConverter;
    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private final ConcurrentLinkedQueue<UserRequest> userRequestsQueue = new ConcurrentLinkedQueue<>();

    public void process(UserRequest userRequest) {
        processTimestamp(userRequest);
        convertToJson(userRequest);
        userRequestsQueue.add(userRequest);

        log.info("UserRequest가 큐에 추가됨. 현재 큐 크기: {}", userRequestsQueue.size());

        // 큐에 데이터가 처음 쌓일 때 이벤트 발생
        if (shouldTriggerBatchJob(userRequest)) {
            log.info("큐에 첫 번째 UserRequest 추가됨, 배치 작업 이벤트 트리거");
            eventPublisher.publishEvent(new TriggerBatchJobEvent(this));
        }
    }

    private boolean shouldTriggerBatchJob(UserRequest userRequest) {
        // 특정 조건 평가 로직
        return userRequestsQueue.size() == 1;
    }


    // 시간 구하기
    private void processTimestamp(UserRequest userRequest){
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> Process Timestamp... ");
        // 서버가 데이터를 받은 시간
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime servRecvTime =  now.toLocalTime(); //  08:39:32.885666
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String format = formatter.format(servRecvTime);


        String timeStr = userRequest.getCliSentTime();
        LocalTime cliSentTime = formatLocalTime(timeStr);

        Duration delay = Duration.between(cliSentTime, servRecvTime);
        String delayFormatted  = formatDuration(delay);

        userRequest.setServRecvTime(format);
        userRequest.setTransDelay(delayFormatted );

    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    private LocalTime formatLocalTime(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        return LocalTime.parse(timeStr, formatter);

    }


    // Json 변환
    private void convertToJson(UserRequest userRequest) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> Convert To Json... ");
        try{
            String dataListJson = jsonConverter.toJson(userRequest.getSensorDataMap());
            userRequest.setDataListJson(dataListJson);
            log.info("JSON 변환 완료: {}", userRequest);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 오류: {}", e.getMessage(), e);
        }
    }
}
