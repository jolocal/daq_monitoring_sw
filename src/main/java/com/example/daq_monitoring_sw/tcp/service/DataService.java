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
//                log.info("collectedData: {}", collectedData);
            }
        }

        // 등록된 리스너에게 데이터 발행
        if (!collectedData.isEmpty()) {
            dataPublisher.publishData(collectedData);
        }
    }

    public void subscribeToData(UserRequest userReq, DataEventListener dataEventListener) {
        // 리스너 객체 초기화
        Listener listener = Listener.builder()
                .channelId(userReq.getChannelId())
                .readTo(userReq.getReadTo())
                .sensorList(new ArrayList<>())
                .listener(dataEventListener)
                .build();

//        log.info("리스너 객체 생성: {}, 데이터 구독", listener.toString());
        dataPublisher.subscribe(listener);
    }

    // 리스너 구독 해제
    public void unsubscribeToData(DataEventListener dataEventListener) {
        dataPublisher.unsubscribe(dataEventListener);
    }

}
