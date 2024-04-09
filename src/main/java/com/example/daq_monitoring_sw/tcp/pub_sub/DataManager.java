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

    /*
    큐 리소스 관리

    실시간 데이터 처리 시스템에서는 메모리 용량과 처리 속도를 기반으로 한 유연한 크기 조절 전략을 사용하는 것이 좋습니다.
    시작점으로는 시스템의 메모리 한계의 일부분을 큐 크기로 설정하고, 성능 테스트를 통해 조정하는 것을 권장합니다.
    예를 들어, 시스템 메모리의 10-20%를 초기 큐 크기로 설정한 후, 테스트와 모니터링을 통해 이 값을 조정
    */
    private static final int MAX_QUEUE_SIZE = 6500;
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();
    // N:N 데이터 저장 및 발행
    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>(); // 각 DAQID별로 데이터를 처리하기 위한 스레드 풀

    private final Map<String, ConcurrentLinkedQueue<ConcurrentLinkedQueue<String>>> dataQueue = new ConcurrentHashMap<>();

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

                    log.info("[2] 데이터 발행");
                    // 데이터 발행
                    publishData(daqId,processData);


                }).exceptionally(e -> {
                    log.error("[ 비동기 데이터 처리 중 예외 발생 ] : {}", e.getMessage());
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

        // kafka 전송을 위한 독립적인 비동기 작업
        CompletableFuture.runAsync(() -> {
                    // kafka 전송 로직(userRequest 기반으로 kafka데이터 구성 및 전송)
                    // sendToKafka(kafkaDatas);
                }, executorService)
                .exceptionally(kafkaException -> {
                    log.error(" [ Kafka 전송 중 예외 발생 ] : {}", kafkaException.getMessage());
                    return null;
                });
    }

    private List<String> processData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        List<String> newSensorData = new ArrayList<>();

//        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

        for (String sensorId : sensorIdsOrder) {
            if (parsedSensorData.containsKey(sensorId)) {
                String dataValue = parsedSensorData.get(sensorId);
                 newSensorData.add(dataValue);
//                queue.add(dataValue);
            }
        }
//        dataQueue.computeIfAbsent(daqId, k -> new ConcurrentLinkedQueue<>()).add(queue);
//        log.info("processData queue: {}", queue.toString());
        return newSensorData;
//        return queue;
    }


    // 리스너에게 데이터 발행
    private void publishData(String daqId,List<String> resDataList) {
        if (subscribers.containsKey(daqId)) {
            for (Subscriber subscriber : subscribers.get(daqId)) {
                subscriber.getConsumer().accept(resDataList);
            }
        }
        log.info("[publishData] resDataList Queue size: {}", resDataList.size());
//        ConcurrentLinkedQueue<ConcurrentLinkedQueue<String>> queue = dataQueue.get(daqId);
//        log.info("publishData queue: {}",queue.toString());
//
//        if (queue != null) {
//            subscribers.get(daqId).forEach(subscriber -> subscriber.getConsumer().accept(queue));
//            log.info("[publishData] 발행된 데이터: {}", queue);
//
//        }
        log.info("[3] 발행 완료");

    }

   /* private void publishData(String key, List<String> resDataList) {
        if (subscribers.containsKey(key)) {
            for (Subscriber subscriber : subscribers.get(key)) {
                subscriber.getConsumer().accept(resDataList);
            }
        }
        log.info("[publishData] resDataList Queue size: {}", resDataList.size());
    }*/


    public void subscribe(String subscribeKey, String channelId, Consumer<List<String>> consumer) {
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
