package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.batch.BatchScheduler;
import com.example.daq_monitoring_sw.tcp.common.JsonConverter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import javax.print.DocFlavor;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserReqProcessor {

    private final JsonConverter jsonConverter;
    private final BatchScheduler batchScheduler;

    @Getter
    private final ConcurrentLinkedQueue<UserRequest> userRequestsQueue = new ConcurrentLinkedQueue<>();

    public void process(UserRequest userRequest) {
        processTimestamp(userRequest);
        convertToJson(userRequest);
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
            userRequestsQueue.add(userRequest);

            log.info("userRequestQueue:{}", userRequest);

            // 큐에 데이터가 처음 추가되면 스케줄러 시작
            batchScheduler.startScheduler();


        } catch (JsonProcessingException e) {
            log.error("JSON 변환 오류: {}", e.getMessage(), e);
        }
    }
}
