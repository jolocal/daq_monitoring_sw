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
        log.info("packet: {}:{}", packet.size(),packet);

        // 데이터 발행
        if (subscribers.containsKey(daqId)) {
            publishData(daqId, packet);
        }

        // 데이터 축적
        // 1. daqId에 해당하는 센서 데이터 맵을 가져오거나 새로 생성
        ConcurrentHashMap<String, List<String>> sensorDataMap = daqSensorData.computeIfAbsent(daqId, k -> new ConcurrentHashMap<>());
        // 2. parsedSensorData의 각 센서 ID에 대해 처리
        parsedSensorData.forEach((sensorId, sensorValue) -> {
            // 2.1 sensorId에 해당하는 데이터 리스트를 가져오거나 새로 생성
            List<String> dataList = sensorDataMap.computeIfAbsent(sensorId, k -> new ArrayList<>());
            // 데이터리스트에 센서 값 추가
            dataList.add(sensorValue);
            // log.info("Updated data list for sensor {} : {}", sensorId, dataList);
        });
        // log.info("Current state of data for all DAQs: {}", daqSensorData);

    }

    /*
            String daqId = userRequest.getDaqId();
            Map<String, String> sensors = userRequest.getParsedSensorData();
            // parsedSensorData={PR01=+002.6, FL02=+022.7, TE03=+038.9, PR04=+048.3, PR05=+049.4, PR06=+041.9, PR07=+027.2, PR08=+007.8, PR09=-012.9, PR10=-031.5, PR11=-044.6, PR12=-049.9, TE13=-046.7, FL14=-035.4, PR15=-017.9})
            log.info("sensors: ? {}",sensors);
            // sensors: ? {PR01=+002.6, FL02=+022.7, TE03=+038.9, PR04=+048.3, PR05=+049.4, PR06=+041.9, PR07=+027.2, PR08=+007.8, PR09=-012.9, PR10=-031.5, PR11=-044.6, PR12=-049.9, TE13=-046.7, FL14=-035.4, PR15=-017.9}



    //        // 센서 데이터 업데이트
    //        ConcurrentHashMap<String, List<String>> sensorMap = daqSensorData.computeIfAbsent(daqId, k -> new ConcurrentHashMap<>());
    //        sensors.forEach((sensorId, value) -> {
    //            // TODO: KAFKA 전송시 현재 하나씩 보냄 배치처리 필요 // 카프카로 전송: DAQ01:PR06:-041.0 카프카로 전송: DAQ01:PR07:-049.1 ...
    //            // 각 센서 ID별로 데이터 리스트를 업데이트
    ////            sensorMap.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(value);
    ////            sendToKafka(daqId,sensorId,value);
    //        });

            // 패킷 데이터 로깅 및 큐 저장
            List<String> packet = new ArrayList<>(sensors.values());
            log.info("packet: {}" , packet);
            // packet: [+002.6, +022.7, +038.9, +048.3, +049.4, +041.9, +027.2, +007.8, -012.9, -031.5, -044.6, -049.9, -046.7, -035.4, -017.9]

            ConcurrentLinkedQueue<String> packetsQueue = packetData.computeIfAbsent(daqId, key -> new ConcurrentLinkedQueue<>());
            packetsQueue.add(String.join(",", packet)); // 리스트를 문자열로 결합하여 큐에 추가
            log.info("packetQueue: {}", packetsQueue);
            // packetQueue: [+000.9,+021.1,+037.7,+047.8,+049.6,+042.9,+028.7,+009.5,-011.2,-030.1,-043.7,-049.8,-047.3,-036.6,-019.5, +001.7,+021.9,+038.3,+048.1,+049.5,+042.4,+028.0,+008.7,-012.1,-030.8,-044.1,-049.9,-047.0,-036.0,-018.7, +002.6,+022.7,+038.9,+048.3,+049.4,+041.9,+027.2,+007.8,-012.9,-031.5,-044.6,-049.9,-046.7,-035.4,-017.9]

            log.info("Added packet to sensorData for DAQ ID {}: {}", daqId, packet);
            // Added packet to sensorData for DAQ ID DAQ01: [+002.6, +022.7, +038.9, +048.3, +049.4, +041.9, +027.2, +007.8, -012.9, -031.5, -044.6, -049.9, -046.7, -035.4, -017.9]


            // 구독자에게 데이터 발행
            if (subscribers.containsKey(daqId)) {
                publishData(daqId, packetsQueue);
            }*/


    // 리스너에게 데이터 발행
    private void publishData(String daqId, ConcurrentLinkedQueue<String> packet) {
        for (Subscriber subscriber : subscribers.get(daqId)) {
            // 복사된 데이터로 이벤트 발생
            subscriber.getConsumer().accept(new ConcurrentLinkedQueue<>(packet));
        }
    }

    public void stopAndCleanup(String daqId){
        ConcurrentHashMap<String, List<String>> sensorDataMap  = daqSensorData.getOrDefault(daqId, new ConcurrentHashMap<>());
        sendToKafka(daqId, sensorDataMap);
        daqSensorData.remove(daqId);
        // 데이터 삭제 확인
        if (daqSensorData.containsKey(daqId)) {
            log.error("Failed to remove data for DAQ ID: {}", daqId);
        } else {
            log.info("Data successfully removed for DAQ ID: {}", daqId);
        }
    }

    // kafka로 데이터를 전송하는 메서드
    private void sendToKafka(String daqId, Map<String, List<String>> sensorDataMap) {
        log.debug("Sending data to Kafka for DAQ ID: {}", daqId);
        // Kafka 전송 로직 구현 (예시)
        // KafkaProducer.send(daqId, sensorData);
        log.info("Data successfully sent to Kafka for DAQ ID: {}", daqId);
    }


    /*
    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();// {PR01=-034.1, FL02=-046.0, TE03=-050.0, PR04=-045.3, PR05=-032.8, PR06=-014.6, PR07=+006.1,

        ConcurrentLinkedQueue<String> queue = sensorData.computeIfAbsent(daqId, k -> new ConcurrentLinkedQueue<String>());

        // &#xD558;&#xB098;&#xC758; &#xD328;&#xD0B7; &#xD050;
        ConcurrentLinkedQueue<String> valuesQueue = new ConcurrentLinkedQueue<>(parsedSensorData.values());
        log.info("valuesQueue: {}", valuesQueue); //  [-034.1, -046.0, -050.0, -045.3, -032.8, -014.6, +006.1, +025.8, +041.0, +049.1, +048.7, +039.9, +024.2, +004.4, -016.3]
        log.info("valuesQueue size: {}", valuesQueue.size()); // 15

        queue.addAll(valuesQueue); // {DAQ01=[-034.1, -046.0, -050.0, -045.3, -032.8, -014.6, +006.1, +025.8, +041.0, +049.1, +048.7, +039.9, +024.2, +004.4, -016.3]}
        log.info("sensorData: {}", sensorData);

        // &#xB370;&#xC774;&#xD130; &#xBC1C;&#xD589; &#xBC0F; &#xBA54;&#xBAA8;&#xB9AC; &#xAD00;&#xB9AC;
        if (subscribers.containsKey(daqId)) {
            publishData(daqId, valuesQueue);
        }

        cleanUpQueue(queue);
    }

    */


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
