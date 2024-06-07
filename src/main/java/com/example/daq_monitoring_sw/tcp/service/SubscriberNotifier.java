package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
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
    private final Map<String, List<Subscriber>> subscriberMap = new ConcurrentHashMap<>();

    // 특정 데이터 구독한 구독자에게 데이터 전달
    public void notifySubscribers(UserRequest userRequest) {
        String daqName = userRequest.getDaqName();
        List<Subscriber> subscriberList = subscriberMap.get(daqName);

        if (subscriberList != null) {
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> 구독한 구독자( {}명 )에게 데이터 전달중... ", subscriberList.size());
            for (Subscriber subscriber : subscriberList) {

                if (subscriber.getChannelContext().channel().isActive()) { // 활성화중인 구독자
                    String cliSentTime = userRequest.getCliSentTime();
                    Collection<String> values = userRequest.getSensorDataMap().values();

                    List<String> dataToSend = new ArrayList<>();
                    dataToSend.add(cliSentTime);
                    dataToSend.addAll(values);

                    subscriber.getDataHandler().accept(dataToSend);
                    log.debug("[PublishData] 데이터 발행 - 구독자: {}", subscriber.getDaqName());

                } else {
                    log.warn("[PublishData] 비활성화된 구독자 제거: {}", subscriber.getDaqName());
                    subscriberList.remove(subscriber);
                }
            }
            subscriberMap.put(daqName, subscriberList);

        } else {
            log.info("구독자가 존재하지 않아 데이터를 발행하지 않음.");
        }
    }

    // 구독
    public void subscribe(String subscribeKey, String daqName, ChannelHandlerContext ctx, Consumer<List<String>> dataHandler) {
        Subscriber newSubscriber  = new Subscriber(subscribeKey, dataHandler, ctx);
        subscriberMap.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>> ['{}' 구독] 구독자 ({}) 등록 - 현재 구독자 수: {}", subscribeKey, daqName, subscriberMap.get(subscribeKey).size());
    }

    // 구독해지
    public void unSubscribe(String subscribeKey, String daqName) {
        subscriberMap.compute(subscribeKey, (key, subscriberList) -> {
            if (subscriberList == null) {
                log.warn("[RD-구독 해제] 구독자 리스트 없음 - 구독 키: {}", subscribeKey);
                return null;
            }
            boolean removed = subscriberList.removeIf(subscriber -> subscriber.getDaqName().equals(daqName));
            if (removed) {
                log.info("[RD-구독 해제] 완료 - 구독 키: {}, 채널 ID: {}, 남은 구독자 수: {}", subscribeKey, daqName, subscriberList.size());
            } else {
                log.info("[RD-구독 해제] 해당 구독자 없음 - 구독 키: {}, 채널 ID: {}", subscribeKey, daqName);
            }
            return subscriberList.isEmpty() ? null : subscriberList;
        });
    }


}
