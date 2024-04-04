package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


@Slf4j
@Component
public class DataManager {

    // DB용 dataMap
    private final Map<String, String> dataMap = new ConcurrentHashMap<>(); // daqId:PR01 = +000.0


    // 메시지 처리 큐
    // private final Queue<UserRequest> dataQueue = new ConcurrentLinkedQueue<>();
//    private final Map<String, List<Consumer<List<String>>>> subscribers = new ConcurrentHashMap<String, Consumer<List<String>>>(); // channelId,
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    // N:N 데이터 저장 및 발행
    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>(); // 각 DAQID별로 데이터를 처리하기 위한 스레드 풀
    private final Map<String, ConcurrentLinkedQueue<String>> dataStorage = new ConcurrentHashMap<>(); // 각 DAQID별 데이터 저장소


    // 비동기 처리시 패킷 순서가 보장되지 않기 때문에
    private final AtomicLong sequence = new AtomicLong(0);


    // 1:N + 동시성관리
    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();

        // 고정된 크기의 스레드 풀 설정
        // 스레드풀 크기: 애플리케이션의 성능 요구사항 + 하드웨어 자원에 따라 다름
        // 일반적인 규칙으로 CPU 코어 수의 2배 정도를 초과하지 않는 범위 내에서 설정 (내컴퓨터 코어:10)
        // DAQID 별로 별도의 ExecutorService 생성 및 관리
        executorServices.computeIfAbsent(daqId, k -> Executors.newFixedThreadPool(8));

        ExecutorService executorService = executorServices.get(daqId);
        log.info("스레드 생성 - {}", executorService);

        CompletableFuture.supplyAsync(() -> processData(userRequest), executorService)
                .thenAccept(processData -> {
                    log.info("[ 비동기 데이터 처리 ] - 센서 타입별 데이터 파싱 완료: {}", processData);
                    // 데이터 발행
                    publishData(daqId, processData);
                }).exceptionally(e -> {
                    log.info("[ 비동기 데이터 처리 중 예외 발생 ] : {}", e.getMessage());
                    return null;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        // 예외가 발생한 경우의 추가 처리
                        log.error("[ 비동기 작업 완료 후 예외 처리 ]", throwable);
                    } else {
                        // 성공적으로 완료된 경우의 추가 처리
                        log.info("[ 비동기 작업 성공적으로 완료 ]");
                    }
                });
/*
        // 데이터 처리를 비동기적으로 수행
        executorService.submit(() -> {
            Queue<String> processedData = processData(userRequest);
            log.info("[ 비동기 데이터 처리 ] - 센서 타입별 데이터 파싱 완료: {}", processedData);
            // 데이터 발행
            publishData(daqId, processedData);
        });
*/
    }


    private Queue<String> processData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        Queue<String> newSensorData = new ConcurrentLinkedQueue<>();

        for (String sensorId : sensorIdsOrder) {
            if (parsedSensorData.containsKey(sensorId)) {
                String dataValue = parsedSensorData.get(sensorId);
                newSensorData.add(dataValue);
            }
        }
        return newSensorData;
        // 데이터 처리 로직 추가 (예: 데이터 저장, 로깅, 기타)

    }


    // 데이터 발행
    private void publishData(String key, Queue<String> resDataList) {
        if (subscribers.containsKey(key)) {
            for (Subscriber subscriber : subscribers.get(key)) {
                subscriber.getConsumer().accept(resDataList);
            }
        }
        log.info("[{}] 채널 구독자에게 데이터 발행 완료 - 구독자 리스트: {}", key, subscribers.get(key));
    }

    public void subscribe(String subscribeKey, String channelId, Consumer<Queue<String>> consumer) {
        Subscriber newSubscriber = new Subscriber(consumer, channelId);
        subscribers.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);

        log.info("새로운 구독자: {}", newSubscriber.toString());

        log.info("[ {} ] 채널에 [{}] 구독자 등록, 현재 구독자 수: {}", subscribeKey, channelId, subscribers.get(subscribeKey).size());

    }

    public void unSubscribe(String subscribeKey, String channelId) {
        try {

            log.info("subscribeKey: {}, channelId: {} ", subscribeKey, channelId);

            if (subscribers.containsKey(subscribeKey)) {
                List<Subscriber> subscriberList = subscribers.get(subscribeKey);
                log.info("{} 구독자 리스트: {}", subscribeKey, subscriberList);

                // 동기화 블록을 사용하여 리스트 수정 시 동기화 문제를 방지
                synchronized (subscriberList) {
                    boolean removed = subscriberList.removeIf(subscriber -> subscriber.getChannelId().equals(channelId));

                    if (removed) {
                        log.info("[{}] 채널의 [{}] 구독자 해제 완료, 현재 구독자 수: {}", subscribeKey, channelId, subscriberList.size());
                    } else {
                        log.info("[{}] 채널에 [{}] 구독자가 존재하지 않습니다.", subscribeKey, channelId);
                    }
                }
            } else {
                log.info("[{}] 채널에 대한 구독자가 존재하지 않습니다.", subscribeKey);
            }
        } catch (Exception e) {
            log.info("구독자 해제 오류: {}", e.getMessage());
        }
    }

    public void handleSTRequest(String daqId) {
        // 해당 daqId와 연결된 스레드 풀을 찾아 종료
        ExecutorService executorService = executorServices.get(daqId);
        if (executorService != null) {
            log.info("[ 스레드 종료-{} ] 대한 실행자 서비스 종료 시작", daqId);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) { // 시간 초과 시 강제 종료
                    log.warn("[ 스레드 종료-{} ] 대한 실행자 서비스가 지정된 시간 내에 종료되지 않음", daqId);
                    List<Runnable> droppedTasks = executorService.shutdownNow(); // 강제 종료
                    log.info("[ 스레드 종료-{} ] 대한 실행자 서비스가 강제로 종료됨. 중단된 작업: {}", daqId, droppedTasks);
                } else {
                    log.info("[ 스레드 종료-{} ] 대한 실행자 서비스가 성공적으로 종료됨", daqId);
                }
            } catch (InterruptedException e) {
                log.error("[ 스레드 종료-{} ] 종료를 기다리는 동안 중단됨", daqId);
                executorService.shutdownNow(); //인터럽트 발생 시 강제 종료
                Thread.currentThread().interrupt();
            }
            executorServices.remove(daqId); // 맵에서 제거
        } else {
            log.warn("[{}] 대한 실행자 서비스를 찾을 수 없음", daqId);
        }
    }

}
