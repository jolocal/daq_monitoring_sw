package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Data
@Component
public class DataPublisher {

    // 전체 데이터 발행자(DataPublisher)와 관련된 리스너들의 정보를 관리
    private final List<Listener> publisherListenerGroup = new CopyOnWriteArrayList<>();


    // 데이터 구독
    public void subscribe(Listener listener) {
        publisherListenerGroup.add(listener);
    }

    // 데이터 구독 해제
    public void unsubscribe(DataEventListener eventListener) {
        publisherListenerGroup.removeIf(listener -> listener.getListener().equals(eventListener));

    }

    // listenerDetail 객체를 순회하면서, 조건에 맞는 리스너에게만 데이터를 전송
    public void publishData(UserRequest userRequest, Map<String, String> collectedData) {
        for (Listener listener : publisherListenerGroup) {
            if (listener.getDaqId().equals(userRequest.getDaqId()) && listener.getSensorList().equals(userRequest.getSensorIdsOrder())){
                listener.getListener().onDataReceived(collectedData);
            }
        }

    }


    public boolean hasListenersFor(UserRequest userRequest) {
        for (Listener listener : publisherListenerGroup) {
            if (listener.getDaqId().equals(userRequest.getDaqId()) && listener.getSensorList().equals(userRequest.getSensorIdsOrder())) {
                log.info("haslistenersFor true");
                log.info("Listenr: {}, userRequest: {}",listener.toString(),userRequest.toString());
                return true;
            }
        }
        log.info("haslistenersFor false");
        return false;
    }


}

