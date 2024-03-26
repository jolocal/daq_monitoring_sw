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
    private final List<Listener> listenerGroup = new CopyOnWriteArrayList<>();


    // 데이터 구독
    public void subscribe(Listener listener) {
        listenerGroup.add(listener);
        log.info("리스너 등록됨: {}", listener);
    }

    // 데이터 구독 해제
    public void unsubscribe(DataEventListener eventListener) {
        listenerGroup.removeIf(listener -> listener.getListener().equals(eventListener));

    }

    // 리스너그룹에서 해당 하는 리스너에게 데이터 발행
    public void publishData( Map<String, String> collectedData) {
        for (Listener listener : listenerGroup) {
            listener.getListener().onDataReceived(collectedData);
        }
    }

    // 전체 리스너 조회 -> 로그
    public void PublisherListenersList(){
        for (Listener listener : listenerGroup){
            log.info("listener: {}", listener.toString());
        }
    }

//    public boolean hasListenersFor(UserRequest userRequest) {
//        for (Listener listener : listenerGroup) {
//            if (listener.getChannelId().equals(userRequest.getDaqId())) {
//                log.info("haslistenersFor true");
//                log.info("Listenr: {}, userRequest: {}",listener.toString(),userRequest.toString());
//                return true;
//            }
//        }
//        return false;
//    }

}

