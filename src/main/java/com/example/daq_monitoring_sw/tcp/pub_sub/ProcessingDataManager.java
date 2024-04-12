package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class ProcessingDataManager {

    @Getter
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    // 각 채널은 자신만의 데이터 큐를 가지고 있으므로, 다른 채널의 데이터 처리에 영향을 주지 않습니다.
    // daqid, FL01:+000.0,TP01:+000.1,...
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> sensorData = new ConcurrentHashMap<>();


    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();// PR01=-038.9, FL02=-022.7, TE03=-002.6, PR04=+017.9,...
        ConcurrentLinkedQueue queue = sensorData.computeIfAbsent(daqId, k -> new ConcurrentLinkedQueue());

        // 하나의 패킷 큐
        ConcurrentLinkedQueue<String> valuesQueue = new ConcurrentLinkedQueue<>(parsedSensorData.values());
        log.info("valuesQueue: {}", valuesQueue);
        log.info("valuesQueue size: {}", valuesQueue.size());

        queue.addAll(valuesQueue);
//        log.info("sensorData: {}", sensorData);

        // 데이터 발행 및 메모리 관리
        if (subscribers.containsKey(daqId)) {
            publishData(daqId, valuesQueue);
        }
        cleanUpQueue(queue);
    }

    private void cleanUpQueue(ConcurrentLinkedQueue<String> queue) {
        // 데이터 처리 후 큐 정리
        while (!queue.isEmpty() && conditionToClearQueue(queue)) {
            queue.poll();
        }
    }

    private boolean conditionToClearQueue(ConcurrentLinkedQueue<String> queue) {
        // 큐 정리 조건 정의
        // 예: 특정 시간이 지났거나, 특정 크기 이상이 되었을 때,
        return true;
    }


    // 리스너에게 데이터 발행
    private void publishData(String daqId, ConcurrentLinkedQueue<String> queue) {
        for (Subscriber subscriber : subscribers.get(daqId)) {
            // 복사된 데이터로 이벤트 발생
            subscriber.getConsumer().accept(new ConcurrentLinkedQueue<String>(queue));
        }
    }

    // 리스너 구독 등록

    public void subscribe(String subscribeKey, String channelId, Consumer<ConcurrentLinkedQueue<String>> consumer) {
        Subscriber newSubscriber = new Subscriber(consumer, channelId);
        subscribers.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);

        log.info("새로운 구독자: {}", newSubscriber.toString());

        log.info("[ {} ] 채널에 [{}] 구독자 등록, 현재 구독자 수: {}", subscribeKey, channelId, subscribers.get(subscribeKey).size());

    }

    // 리스너 구독 해제
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

}
