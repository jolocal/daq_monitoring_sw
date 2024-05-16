package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class ProcessingDataManager {

    @Getter
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    // 각 DAQID 별로 센서 데이터 패킷을 저장하는 map
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<String>>> daqSensorData = new ConcurrentHashMap<>();


    public void writeData(UserRequest userRequest) {

        String daqId = userRequest.getDaqId();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();
        String timeStamp = userRequest.getTimeStamp();

        // 실시간 데이터 전송 ( 1 packet )
        ConcurrentLinkedQueue<String> packet = new ConcurrentLinkedQueue<>();
        packet.add(timeStamp);
        packet.addAll(parsedSensorData.values());
        log.info("[ wd ] packet: {}:{}", packet.size(),packet);

        // 데이터 발행
        if (subscribers.containsKey(daqId)) {
            publishData(daqId, packet);
        }

        // TODO: 메모리 누수 의심 부분
        // 데이터 축적
        // 1. daqId에 해당하는 센서 데이터 맵을 가져오거나 새로 생성
        ConcurrentHashMap<String, List<String>> sensorDataMap = daqSensorData.computeIfAbsent(daqId, k -> new ConcurrentHashMap<>());

        // 2. parsedSensorData의 각 센서 ID에 대해 처리
        parsedSensorData.forEach((sensorId, sensorValue) -> {
            List<String> dataList = sensorDataMap.computeIfAbsent(sensorId, k -> new ArrayList<>());
            dataList.add(sensorValue);
            if (dataList.size() > 1000) { // 데이터 리스트 크기 제한
                dataList.remove(0); // 오래된 데이터 제거
            }
        });

        // 패킷 큐 재사용을 위해 클리어
        packet.clear();
        log.info("[ wd ] packet clear() -> {}" , packet);
    }


    // 리스너에게 데이터 발행
    private void publishData(String daqId, ConcurrentLinkedQueue<String> packet) {
        for (Subscriber subscriber : subscribers.get(daqId)) {
            // 복사된 데이터로 이벤트 발생
            log.info("[ Copid PublishData ] 복사된 데이터 발행");
            List<String> packetCopy = new ArrayList<>(packet); // 큐를 리스트로 변환하여 데이터 전달
            subscriber.getConsumer().accept(packetCopy);
        }

        // 발행 후 큐 클리어 및 참조 해제
        packet.clear();
        log.info("[ publishData ] packet clear() -> {}" , packet);
    }

    public void stopAndCleanup(String daqId){
        ConcurrentHashMap<String, List<String>> sensorDataMap = daqSensorData.get(daqId);
        if (sensorDataMap != null) {
            sendToKafka(daqId, sensorDataMap);
            daqSensorData.remove(daqId);
            // 데이터 삭제 확인
            if (daqSensorData.containsKey(daqId)) {
                log.error("Failed to remove data for DAQ ID: {}", daqId);
            } else {
                log.info("Data successfully removed for DAQ ID: {}", daqId);
            }
        }else {
            log.warn("No sensor data found for DAQ ID: {}",daqId);
        }
    }

    // kafka로 데이터를 전송하는 메서드
    private void sendToKafka(String daqId, Map<String, List<String>> sensorDataMap) {
        log.debug("Sending data to Kafka for DAQ ID: {}", daqId);
        // Kafka 전송 로직 구현 (예시)
        // KafkaProducer.send(daqId, sensorData);
        log.info("Data successfully sent to Kafka for DAQ ID: {}", daqId);
    }



    // 리스너 구독 등록

    public void subscribe(String subscribeKey, String channelId, Consumer<List<String>> consumer) {
        Subscriber newSubscriber = new Subscriber(consumer, channelId);
        subscribers.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);

        log.info("새로운 구독자: {}", newSubscriber.toString());
        log.info("[ {} ] 채널에 [{}] 구독자 등록, 현재 구독자 수: {}", subscribeKey, channelId, subscribers.get(subscribeKey).size());
    }


    // 리스너 구독 해제
    public void unSubscribe(String subscribeKey, String channelId) {
        try {

            log.info("subscribeKey: {}, channelId: {} ", subscribeKey, channelId);

            subscribers.compute(subscribeKey,(key,subscriberList) -> {
                if (subscriberList == null) {
                    log.info("[{}] 키에 대한 구독자 리스트가 존재하지 않습니다.", subscribeKey);
                    return null;
                }
                log.info("{} 구독자 리스트: {}", subscribeKey,subscriberList);

                boolean removed = subscriberList.removeIf(subscriber -> subscriber.getChannelId().equals(channelId));
                if (removed) {
                    log.info("[{}] 채널의 [{}] 구독자 해제 완료, 현재 구독자 수: {}", subscribeKey, channelId, subscriberList.size());
                } else {
                    log.info("[{}] 채널에 [{}] 구독자가 존재하지 않습니다.", subscribeKey, channelId);
                }

                return subscriberList.isEmpty() ? null : subscriberList;
            });

        } catch (Exception e) {
            log.info("구독자 해제 오류: {}", e.getMessage());
        }
    }

}
