package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.entity.DaqCenter;
import com.example.daq_monitoring_sw.tcp.repository.DaqCenterRepository;
import com.example.daq_monitoring_sw.tcp.repository.SensorRepository;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingDataService {

    private final DaqCenterRepository daqCenterRepository;
    private final SensorRepository sensorRepository;


    @Getter
    private final Map<String, List<Subscriber>> subscriberMap = new ConcurrentHashMap<>(); // 구독자 관리(RD 사용자 관리)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<String>>> daqSensorData = new ConcurrentHashMap<>();  // 각 DAQID 별로 센서 데이터 패킷을 저장하는 map


    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqId();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();
        String timeStamp = String.valueOf(userRequest.getCliSentTime());

        ConcurrentLinkedQueue<String> packet = createPacket(timeStamp, parsedSensorData);
        log.debug("[WD] 패킷 생성 - 크기: {}: {}", packet.size(), packet);

        // 데이터 발행
        List<Subscriber> subscriberList = subscriberMap.get(daqId);
        if (subscriberList != null) { // 구독자가 있을 경우만 데이터 발행
            publishData(daqId, packet);
            log.info("구독자 존재 데이터 발행 완료");
        } else {
            packet.clear();
            log.info("구독자가 존재하지 않아 데이터를 발행하지 않음. packet clear()");
        }

        // DB 저장
        storeSensorData(userRequest);

        packet.clear();
        log.debug("[WD] 패킷 전송 후 클리어: {}", packet);
    }

    private void storeSensorData(UserRequest userRequest) {
        log.info("DB 데이터 저장 시작");
        // 지연율 구하기
        String cliSentStr = userRequest.getCliSentTime();
        LocalTime cliSentTime = convertToLocalDateTime(cliSentStr);

        // 서버가 현재 시간을 가져옴
        LocalTime serverReceivedTime = LocalTime.now();

        // 지연 시간 계산
        Duration delay = Duration.between(cliSentTime, serverReceivedTime);
        long delayInMillis = delay.toMillis();

        // 지연 시간 출력 (또는 로그 기록)
        System.out.println("Sent time: " + formatLocalTime(cliSentTime));
        System.out.println("Received time: " + formatLocalTime(serverReceivedTime));
        System.out.println("Delay in milliseconds: " + delayInMillis + " ms");

    }

    public static LocalTime convertToLocalDateTime(String cliSentStr) {
        long millis = Long.parseLong(cliSentStr);

        long hours = millis / 3600000;
        millis %= 3600000;
        long minutes = millis / 60000;
        millis %= 60000;
        long seconds = millis / 1000;
        long milliseconds = millis % 1000;

        return LocalTime.of((int) hours, (int) minutes, (int) seconds, (int) milliseconds * 1000000);
    }

    public static String formatLocalTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmssSSS");
        return time.format(formatter);
    }

    private ConcurrentLinkedQueue<String> createPacket(String timeStamp, Map<String, String> parsedSensorData) {
        ConcurrentLinkedQueue<String> packet = new ConcurrentLinkedQueue<>();
        packet.add(timeStamp);
        packet.addAll(parsedSensorData.values());
        return packet;
    }

   /* private void storeSensorData(String daqId, Map<String, String> parsedSensorData) {
        ConcurrentHashMap<String, List<String>> sensorDataMap = daqSensorData.computeIfAbsent(daqId, k -> new ConcurrentHashMap<>());
        parsedSensorData.forEach((sensorId, sensorValue) -> {
            List<String> dataList = sensorDataMap.computeIfAbsent(sensorId, k -> new ArrayList<>());
            dataList.add(sensorValue);

            // 데이터 정리
            if (dataList.size() > 1000) {
                dataList.remove(0);
            }
        });
    }*/


    // 리스너에게 데이터 발행
    private void publishData(String daqId, ConcurrentLinkedQueue<String> packet) {
        List<Subscriber> subscriberList = subscriberMap.get(daqId);

        if (subscriberList != null) {
            List<Subscriber> activeSubscribers = new ArrayList<>();

            for (Subscriber subscriber : subscriberList) {
                // 채널이 활성 상태인지 확인
                if (subscriber.getChannelContext().channel().isActive()) {
                    activeSubscribers.add(subscriber);
                    subscriber.getDataHandler().accept(new ArrayList<>(packet)); // 큐를 리스트로 변환하여 데이터 전달
                    log.debug("[PublishData] 데이터 발행 - 구독자: {}", subscriber.getChannelId());
                } else {
                    log.warn("[PublishData] 비활성화된 구독자 제거: {}", subscriber.getChannelId());
                }
            }
            // 비활성 구독자 제거
            subscriberMap.put(daqId, activeSubscribers);
        }
        // 발행 후 큐 클리어 및 참조 해제
        packet.clear();
    }

    public void stopAndCleanup(String daqId) {
        try {
            log.info("[WD-ST] WD사용자 프로세스 종료 및 리소스 정리 작업 시작");
            ConcurrentHashMap<String, List<String>> sensorDataMap = daqSensorData.remove(daqId);
            if (sensorDataMap != null) {
                sendToKafka(daqId, sensorDataMap);
                log.info("[WD-stopAndCleanup] 데이터 제거 완료 - DAQ ID: {}", daqId);
            }

        } catch (Exception e) {
            log.error("[WD-stopAndCleanup] 예외 발생 - 원인: {}", e.getMessage(), e);
        }

    }

    // kafka로 데이터를 전송하는 메서드
    private void sendToKafka(String daqId, Map<String, List<String>> sensorDataMap) {
        log.debug("[Kafka] 데이터 전송 - DAQ ID: {}", daqId);
        // Kafka 전송 로직 구현 (예시)
        // KafkaProducer.send(daqId, sensorData);
        log.info("[Kafka] 데이터 전송 성공 - DAQ ID: {}", daqId);
    }


    // 리스너 구독 등록

    public void subscribe(String subscribeKey, String channelId, ChannelHandlerContext ctx, Consumer<List<String>> dataHandler) {
        Subscriber newSubscriber = new Subscriber(dataHandler, channelId, ctx); // Subscriber 객체 생성
        subscriberMap.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);
        // computeIfAbsent : subscriberMap에 subscribekey가 존재하지 않으면 새로운 CopyOnWriteArrayList<>를 생성하고 해당 키에 매핑
        // 이미 subscribekey가 존재하면 기존의 값을 반환,
        // -> 이 메서드는 키에 해당하는 값이 없는 경우에만 새로운 값을 계산하고 삽입
        log.info("[구독] 채널: {}, 구독자 등록 - 현재 구독자 수: {}", subscribeKey, subscriberMap.get(subscribeKey).size());
    }


    // 리스너 구독 해제
    public void unSubscribe(String subscribeKey, String channelId) {
      subscriberMap.compute(subscribeKey, (key, subscriberList) -> {
            if (subscriberList == null) {
                log.warn("[RD-구독 해제] 구독자 리스트 없음 - 구독 키: {}", subscribeKey);
                return null;
            }
            boolean removed = subscriberList.removeIf(subscriber -> subscriber.getChannelId().equals(channelId));
            if (removed) {
                log.info("[RD-구독 해제] 완료 - 구독 키: {}, 채널 ID: {}, 남은 구독자 수: {}", subscribeKey, channelId, subscriberList.size());
            } else {
                log.info("[RD-구독 해제] 해당 구독자 없음 - 구독 키: {}, 채널 ID: {}", subscribeKey, channelId);
            }
            return subscriberList.isEmpty() ? null : subscriberList;
        });
    }

}
