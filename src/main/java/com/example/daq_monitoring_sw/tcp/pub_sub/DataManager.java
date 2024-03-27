package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Slf4j
@Component
public class DataManager {

    // DB용 dataMap
    private final Map<String, String> dataMap = new ConcurrentHashMap<>(); // daqId:PR01 = +000.0

    // 실시간 데이터용
    private final Map<String, String> realTimeData = new ConcurrentHashMap<>();

    private final Map<String, Consumer<String>> subscribers = new ConcurrentHashMap<String, Consumer<String>>(); // channelId,

    // 각 구독자별 마지막 처리 인덱스 관리
    private final Map<String, Integer> lastProcessedIndex = new ConcurrentHashMap<>();


    // 데이터 저장 및 발행
    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        for (String sensorId : sensorIdsOrder) {
            if (parsedSensorData.containsKey(sensorId)){
                String data = parsedSensorData.get(sensorId);
                String key = daqId + ":" + sensorId;
                //dataMap.put(key, data); // DB 처리용 Map에 저장

                realTimeData.put(key, data);

                log.info(">>>>>>>>>>>>>>>>>> 현재 realTimeData 데이터 저장: Key = {}, Value = {}", daqId, data);

                publishData(daqId, realTimeData.get(key));
            }
        }

        // log.info("현재 dataMap 크기: {}", dataMap.values().size());

        realTimeData.forEach((key, dataList) -> {
            log.info("[realTimeData] Key: {}, 현재 데이터: {}", key, dataList);
        });
    }

    // 데이터 발행
    private void publishData(String key, String data) {
        if (subscribers.containsKey(key)) {
//            Integer lastRdIndex = lastProcessedIndex.getOrDefault(key,0);
//            List<String> newDatas = datas.subList(lastRdIndex, datas.size());
//            lastProcessedIndex.put(key, datas.size());

            log.info("데이터 발행: {} 채널에 {} 데이터 발행", key, data.toString());
            subscribers.get(key).accept(data);
        }
    }

    // 구독자 등록 및 데이터 수신
    public void subscribe(String key, Consumer<String> consumer) {
        subscribers.put(key, consumer);
        log.info("{} 채널 구독자 등록, 현재 구독자 수: {}", key, subscribers.size());
    }

}
