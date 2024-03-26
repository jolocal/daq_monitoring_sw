package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataEventListener;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataPublisher;
import com.example.daq_monitoring_sw.tcp.pub_sub.Listener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataService {

    private final DataPublisher dataPublisher;
    Map<String, String> collectedData = new LinkedHashMap<>();


    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        for (String sensorId : sensorIdsOrder) {
            String data = parsedSensorData.get(sensorId);

            if (data != null) {
                /*String key = daqId + ":" + sensorId;*/
                collectedData.put(sensorId, data); // ex: daqId:PR01 = +000.0
                log.info("collectedData: {}", collectedData);
            }
        }

        // 등록된 리스너에게 데이터 발행
        log.info("------------------------------------------- 읽기 요청 리스너 검색 중 -------------------------------------------");
        if (dataPublisher.hasListenersFor(userRequest)) {
            log.info("읽기 요청 리스너 검색 완료");
            dataPublisher.publishData(userRequest,collectedData);
            log.info("데이터 발행 완료.");
        } else {
            log.info("현재 등록된 리스너가 없습니다.");
        }
    }

    // 리스너를 구독자 목록에 추가
    public void subscribeToData(UserRequest userReq, DataEventListener dataEventListener) {
        Listener listener = Listener.builder()
                .daqId(userReq.getDaqId())
                .sensorList(userReq.getSensorIdsOrder())
                .listener(dataEventListener)
                .build();

        dataPublisher.subscribe(listener);
    }

    // 리스너 구독 해제
    public void unsubscribeToData(DataEventListener dataEventListener){
        dataPublisher.unsubscribe(dataEventListener);
    }
}
