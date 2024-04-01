package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;


@Slf4j
@Component
public class DataManager {

    // DB용 dataMap
    private final Map<String, String> dataMap = new ConcurrentHashMap<>(); // daqId:PR01 = +000.0

    // 메시지 처리 큐
    private final Queue<UserRequest> dataQueue = new ConcurrentLinkedQueue<>();

    private final Map<String, Consumer<List<String>>> subscribers = new ConcurrentHashMap<String, Consumer<List<String>>>(); // channelId,


    // 데이터 저장 및 발행
    public void writeData(UserRequest userRequest) {
        // 큐에 메시지 추가
        dataQueue.add(userRequest);
        log.info("[ 데이터 큐 사이즈 ]: {}", dataQueue.size());

        // 큐에서 데이터 처리
        if (!dataQueue.isEmpty()) {
            UserRequest peek = dataQueue.peek();
            String daqId = peek.getDaqId();
            List<String> sensorIdsOrder = peek.getSensorIdsOrder();
            Map<String, String> parsedSensorData = peek.getParsedSensorData();

            // 데이터 리스트 생성
            List<String> resDataList = new ArrayList<>();

            for (String sensorId : sensorIdsOrder) {
                if (parsedSensorData.containsKey(sensorId)) {
                    String dataValue = parsedSensorData.get(sensorId);
                    resDataList.add(dataValue);
                }
            }
            log.info("[writeData] daqId: {} sensorId: {} dataValue: {}", daqId, sensorIdsOrder, resDataList);

            //데이터 발행
            publishData(daqId, resDataList);

        }


      /*  while ((userRequest = dataQueue.peek()) != null) { // 큐에서 데이터를 제거하지 않음
//        while ((userRequest = dataQueue.poll()) != null) {

            String daqId = userRequest.getDaqId();
            List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
            Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

            // log.info("[ writeData daqcenter info ] daqId: {}, sensorIdsOrder: {}, parsedSensorData: {}", daqId, sensorIdsOrder, parsedSensorData);

            // 데이터 리스트 생성
            List<String> resDataList = new ArrayList<>();

            for (String sensorId : sensorIdsOrder) {
                if (parsedSensorData.containsKey(sensorId)) {
                    String dataValue = parsedSensorData.get(sensorId);
                    resDataList.add(dataValue);
                }
            }
            log.info("[writeData] daqId: {} sensorId: {} dataValue: {}", daqId, sensorIdsOrder, resDataList);

            //데이터 발행
            publishData(daqId, resDataList);
        }*/
    }

    // 데이터 발행
    private void publishData(String key, List<String> resDataList) {
        if (subscribers.containsKey(key)) {
            log.info("데이터 발행: {} 채널에 {} 데이터 발행", key, resDataList);
            subscribers.get(key).accept(resDataList);

            // 발행한 데이터 삭제       }
        }
    }

    public void subscribe(String subscribeKey, Consumer<List<String>> consumer) {
        subscribers.put(subscribeKey, consumer);
        log.info("{} 채널 구독자 등록, 현재 구독자 수: {}", subscribeKey, subscribers.size());

    }
}
