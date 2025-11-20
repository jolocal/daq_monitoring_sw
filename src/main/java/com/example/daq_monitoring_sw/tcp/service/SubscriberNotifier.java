package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.Payloads;
import com.example.daq_monitoring_sw.tcp.dto.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberNotifier {

    // 구독자 관리(RD 사용자 관리)
    private final Map<String, List<Subscriber>> subscriberMap = new ConcurrentHashMap<>();     // key: targetDeviceId, deviceId: 해당 daq를 구독하는 구독자리스트

    //* WD가 데이터를 보냈을 때 "해당 WD를 구독 중인 RD 들에게 즉시 전달"
    public void notifyWDSubscribers(ProtocolMessage message) {
        
        String deviceId = message.getDeviceId();
        List<Subscriber> subscriberList = subscriberMap.get(deviceId); // 구독자리스트
        
        // 구독자가 없는 경우 NPE 방지 및 조기 반환
        if (subscriberList == null) {
            log.info("  ▷ 구독자가 존재하지 않아 데이터를 발행하지 않음.");
            return;
        }

        // 비활성화 제거 후 더이상 구독자 없으면 종료
        subscriberList.removeIf(subscriber -> {
            boolean inActive = !subscriber.getChannelContext().channel().isActive();
            if (inActive) {
                log.warn("  ▷ [PublishData] 비활성화된 구독자 제거: {}", subscriber.getSubscriberId());
                return true;
            }
            return inActive;
        });
        if (subscriberList.isEmpty()) {
            log.info("  ▷ 비활성화 구독자 제거 후 더이상 구독자가 없음. targetDeviceId={}", deviceId);
            subscriberMap.remove(deviceId);
            return;
        }

        //log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 구독한 구독자( {}명 )에게 데이터 전달중... ", subscriberList.size());
        // ProtocolMessage의 원시 데이터(타임스탬프, 센서 값 등)를 Payloads DTO로 정규화
        for (Subscriber subscriber : subscriberList) {
            List<Payloads> payloads = List.of(
                Payloads.builder()
                    .sensorCnt(message.getSensorCnt())
                    .timeStamp(message.getCli_ts_ms())
                    .values(new ArrayList<>(message.getSensorDataMap().values()))
                    .build()
            );
            
            // 여기서 생성한 payloads 리스트가 구독자 콜백의 입력
            subscriber.getDataHandler().accept(payloads);
            log.debug("  ▶▶▶ [PublishData] 데이터 발행 - 구독자: {} ({}명)", subscriber.getSubscriberId(), subscriberList.size());
        }

        subscriberMap.put(deviceId, subscriberList);
    }

    // 새 구독자 등록
    public void subscribe(String targetDeviceId, String subscriberId, ChannelHandlerContext ctx, Consumer<List<Payloads>> dataHandler) {
        Subscriber newSubscriber  = Subscriber.builder()
                                        .targetDeviceId(targetDeviceId)
                                        .subscriberId(subscriberId)
                                        .dataHandler(dataHandler)
                                        .channelContext(ctx)
                                        .build();

        subscriberMap.computeIfAbsent(targetDeviceId, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);
        
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> ['{}' 구독] 구독자 ({}) 등록 - 현재 구독자 수: {}", targetDeviceId, subscriberId, subscriberMap.get(targetDeviceId).size());
    }


    // 구독자 제거
    public void unSubscribe(String targetDeviceId, String deviceId) {
        subscriberMap.compute(targetDeviceId, (key, subscriberList) -> {
            if (subscriberList == null) {
                log.warn("[RD-구독 해제] 구독자 리스트 없음 - 구독 키: {}", targetDeviceId);
                return null;
            }
            
            boolean removed = subscriberList.removeIf(subscriber -> subscriber.getSubscriberId().equals(deviceId));
            if (removed) {
                log.info("[RD-구독 해제] 완료 - 구독 키: {}, 채널 ID: {}, 남은 구독자 수: {}", targetDeviceId, deviceId, subscriberList.size());
            } else {
                log.info("[RD-구독 해제] 해당 구독자 없음 - 구독 키: {}, 채널 ID: {}", targetDeviceId, deviceId);
            }
            return subscriberList.isEmpty() ? null : subscriberList;
        });
    }


}
