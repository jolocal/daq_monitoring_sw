package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataEventListener;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataPublisher;
import com.example.daq_monitoring_sw.tcp.pub_sub.Listener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        log.info("writeData userRequest: {}", userRequest);
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
        if (!collectedData.isEmpty()) {
            dataPublisher.publishData(collectedData);
        }
    }

//        log.info("------------------------------------------- 읽기 요청 리스너 검색 중 -------------------------------------------");
//        if (dataPublisher.hasListenersFor(userRequest)) {
//            dataPublisher.PublisherListenersList();
//            log.info("읽기 요청 리스너 검색 완료");
//            dataPublisher.publishData(userRequest,collectedData);
//            log.info("데이터 발행 완료.");
//        } else {
//            dataPublisher.PublisherListenersList();
//            log.info("현재 등록된 리스너가 없습니다.");
//        }


    public void subscribeToData(UserRequest userReq, DataEventListener dataEventListener) {
        // 리스너 객체 초기화
        Listener listener = Listener.builder()
                .channelId(userReq.getChannelId())
                .readTo(userReq.getReadTo())
                .sensorList(new ArrayList<>())
                .listener(dataEventListener)
                .build();

        log.info("리스너 객체 생성: {}, 데이터 구독", listener.toString());
        dataPublisher.subscribe(listener);
    }

    // 리스너 구독 해제
    public void unsubscribeToData(DataEventListener dataEventListener) {
        dataPublisher.unsubscribe(dataEventListener);
    }

    // 해당 유저가 이미 리스너에 등록되었는지 확인하는 로직
//    public boolean isSubscribed(UserRequest userReq) {
//        String channelId = userReq.getChannelId(); // UserRequest와 연관된 식별자
//        return dataPublisher.getListeners().stream()
//                .anyMatch(listener -> listener.getChannelId().equals(channelId));
//    }
}
