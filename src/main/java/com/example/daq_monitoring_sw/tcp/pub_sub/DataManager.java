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
        while ((userRequest = dataQueue.poll()) != null) {

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
        }
    }

    // 데이터 발행
    private void publishData(String key, List<String> resDataList) {
        if (subscribers.containsKey(key)) {
            log.info("데이터 발행: {} 채널에 {} 데이터 발행", key, resDataList);
            subscribers.get(key).accept(resDataList);
        }
    }

    // 구독자 등록 및 데이터 수신
    public void subscribe(String key, Consumer<List<String>> consumer) {
        subscribers.put(key, consumer);
        log.info("{} 채널 구독자 등록, 현재 구독자 수: {}", key, subscribers.size());
    }


    // 데이터 저장 및 발행
//    public void writeData(UserRequest userRequest) {
//        String daqId = userRequest.getDaqId();
//        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
//        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();
//
//        for (String sensorId : sensorIdsOrder) {
//            if (parsedSensorData.containsKey(sensorId)){
//                String dataValue = parsedSensorData.get(sensorId);
//                String key = daqId + ":" + sensorId;
//                //dataMap.put(key, data); // DB 처리용 Map에 저장
////                realTimeData.put(daqId, data);
//
//                publishData(daqId, dataValue);
//            }
//        }
//    }

    // log.info("현재 dataMap 크기: {}", dataMap.values().size());

//        realTimeData.forEach((key, dataList) -> {
//            log.info("[realTimeData] Key: {}, 현재 데이터: {}", key, dataList);
//        });


}


//3. Queue를 사용하여 데이터가 들어온 순서를 유지하고, publishDataInOrder 메소드를 통해 순차적으로 데이터를 발행합니다.

//public class DataPublisher {
//    private final Queue<String> publishQueue = new LinkedList<>();
//
//    public void writeData(UserRequest userRequest) {
//        String daqId = userRequest.getDaqId();
//        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
//        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();
//
//        for (String sensorId : sensorIdsOrder) {
//            if (parsedSensorData.containsKey(sensorId)) {
//                String data = parsedSensorData.get(sensorId);
//                String key = daqId + ":" + sensorId;
//
//                realTimeData.put(key, data);
//                publishQueue.add(key); // 들어온 순서대로 큐에 추가
//                log.info("Data 저장: Key = {}, Value = {}", key, data);
//            }
//        }
//
//        publishDataInOrder(); // 순서대로 데이터 발행
//    }
//
//    private void publishDataInOrder() {
//        while (!publishQueue.isEmpty()) {
//            String key = publishQueue.poll(); // 큐에서 하나씩 꺼내서
//            String data = realTimeData.get(key);
//            if (data != null) {
//                publishData(key, data); // 데이터 발행
//                log.info("Data 발행: Key = {}, Value = {}", key, data);
//            }
//        }
//    }
//
//    private void publishData(String key, String data) {
//        // 발행 로직
//    }
//}