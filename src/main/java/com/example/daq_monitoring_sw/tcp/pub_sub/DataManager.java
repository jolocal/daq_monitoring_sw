package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Slf4j
@Component
public class DataManager {

    private final Map<String, String> dataMap = new ConcurrentHashMap<>(); // daqId:PR01 = +000.0
    private final Map<String, Consumer<String>> subscribers = new ConcurrentHashMap<>(); // channelId,


    // 데이터 저장 및 발행
    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        for (String sensorId : sensorIdsOrder) {
            if (parsedSensorData.containsKey(sensorId)){
                String data = parsedSensorData.get(sensorId);
                dataMap.put(daqId + ":" + sensorId, data);
                publishData(daqId + ":" + sensorId, data);
            }
        }
    }

    // 데이터 발행
    private void publishData(String key, String data) {
        if (subscribers.containsKey(key)) {
            subscribers.get(key).accept(data);
        }
    }

    // 구독자 등록 및 데이터 수신
    public void subscribe(String key, Consumer<String> consumer) {
        subscribers.put(key, consumer);
    }


    // 특정 채널에 대한 데이터를 해당 채널의 구독자들에게 전달하는 기능
//    private void publishData(String channelId, String data) {
//        if (subscribers.containsKey(channelId)){ // 구독자 존재여부 확인
//            subscribers.get(channelId).accept(data);
//        }
//    }
}
