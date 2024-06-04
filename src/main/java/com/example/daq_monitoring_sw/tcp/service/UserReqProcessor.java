package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.common.JsonConverter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.DocFlavor;
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
    private final ConcurrentLinkedQueue<UserRequest> userRequestsQueue = new ConcurrentLinkedQueue<>();

    public void process(UserRequest userRequest) {
        processTimestamp(userRequest);
        convertToJson(userRequest);
    }


    // 시간 구하기
    private void processTimestamp(UserRequest userRequest){

        // 서버가 데이터를 받은 시간
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime servRecvTime =  now.toLocalTime();
        log.info("servRecvTime: {}", servRecvTime);

        String timeStr = userRequest.getCliSentTime();
        LocalTime cliSentTime = formatLocalTime(timeStr);

        Duration delay = Duration.between(cliSentTime, servRecvTime);
        String delayFormatted  = formatDuration(delay);

        userRequest.setServRecvTime(String.valueOf(servRecvTime));
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
        try{
            String dataListJson = jsonConverter.toJson(userRequest.getSensorDataMap());
            userRequest.setDataListJson(dataListJson);
            userRequestsQueue.add(userRequest);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 오류: {}", e.getMessage(), e);
        }
    }
}
