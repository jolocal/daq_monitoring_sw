package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingDataService {
    private final UserReqProcessor userRequestProcessor;
    private final SubscriberNotifier subscriberNotifier;
    //private final BatchScheduler batchScheduler;


    public void writeData(UserRequest userRequest) {
        userRequestProcessor.process(userRequest);
        subscriberNotifier.notifySubscribers(userRequest);
    }

    public void subscribe(String subscribeKey, String daqName, ChannelHandlerContext ctx, Consumer<List<String>> dataHandler) {
        subscriberNotifier.subscribe(subscribeKey, daqName, ctx, dataHandler);
    }

    public void unSubscribe(String subscribeKey, String daqName) {
        subscriberNotifier.unSubscribe(subscribeKey, daqName);
    }


    public void stopAndCleanup(String daqId) {
        try {
            log.info("[WD-ST] WD사용자 프로세스 종료 및 리소스 정리 작업 시작");
            // db 저장 종료
            //batchScheduler.stopScheduler();

        } catch (Exception e) {
            log.error("[WD-stopAndCleanup] 예외 발생 - 원인: {}", e.getMessage(), e);
        }

    }


   /* private final DaqCenterRepository daqCenterRepository;
    private final JsonConverter jsonConverter;


    @Getter
    private final Map<String, List<Subscriber>> subscriberMap = new ConcurrentHashMap<>(); // 구독자 관리(RD 사용자 관리)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<String>>> daqSensorData = new ConcurrentHashMap<>();  // 각 DAQID 별로 센서 데이터 패킷을 저장하는 map


    public void writeData(UserRequest userRequest) {
        String daqId = userRequest.getDaqName();
        Map<String, String> sensorDataMap = userRequest.getSensorDataMap();
        String timeStamp = String.valueOf(userRequest.getCliSentTime());

        ConcurrentLinkedQueue<String> packet = createPacket(timeStamp, sensorDataMap);
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

        // 클라이언트가 데이터 보낸 시간
        String timeStr = userRequest.getCliSentTime();
        LocalTime cliSentTime = formatLocalTime(timeStr);

        // 서버가 현재 시간을 가져옴
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul")); // 현재 시간 구하기(서울 시간 설정)
        LocalTime servRecvTime  = formatLocalTime(now.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));

        // 지연율 구하기
        Duration delay = Duration.between(servRecvTime, cliSentTime);
        String delayFormatted  = formatDuration(delay);

        userRequest.setServRecvTime(servRecvTime.toString());
        userRequest.setTransDelay(delayFormatted);

        try {
            String dataListJson = jsonConverter.toJson(userRequest.getSensorDataMap());

            DaqEntity entity = DaqEntity.builder()
                    .daqName(userRequest.getDaqName())
                    .status(userRequest.getStatus())
                    .sensorCnt(Integer.parseInt(userRequest.getSensorCnt()))
                    .dataList(dataListJson)
                    .cliSentTime(userRequest.getCliSentTime())
                    .servRecvTime(userRequest.getServRecvTime())
                    .transDelay(userRequest.getTransDelay())
                    .build();

            log.debug("entity : {}", entity);

            daqCenterRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("json convert 오류, DB 저장 실패: {}", e.getMessage(),e.getCause());
        }



    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    public static LocalTime formatLocalTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSS");
        return LocalTime.parse(time);
    }

    private ConcurrentLinkedQueue<String> createPacket(String timeStamp, Map<String, String> parsedSensorData) {
        ConcurrentLinkedQueue<String> packet = new ConcurrentLinkedQueue<>();
        packet.add(timeStamp);
        packet.addAll(parsedSensorData.values());
        return packet;
    }



    // 리스너에게 데이터 발행
    private void publishData(String daqName, ConcurrentLinkedQueue<String> packet) {
        List<Subscriber> subscriberList = subscriberMap.get(daqName);

        if (subscriberList != null) {
            List<Subscriber> activeSubscribers = new ArrayList<>();

            for (Subscriber subscriber : subscriberList) {
                // 채널이 활성 상태인지 확인
                if (subscriber.getChannelContext().channel().isActive()) {
                    activeSubscribers.add(subscriber);
                    subscriber.getDataHandler().accept(new ArrayList<>(packet)); // 큐를 리스트로 변환하여 데이터 전달
                    log.debug("[PublishData] 데이터 발행 - 구독자: {}", subscriber.getDaqName());
                } else {
                    log.warn("[PublishData] 비활성화된 구독자 제거: {}", subscriber.getDaqName());
                }
            }
            // 비활성 구독자 제거
            subscriberMap.put(daqName, activeSubscribers);
        }
        // 발행 후 큐 클리어 및 참조 해제
        packet.clear();
    }

    // kafka로 데이터를 전송하는 메서드
    private void sendToKafka(String daqId, Map<String, List<String>> sensorDataMap) {
        log.debug("[Kafka] 데이터 전송 - DAQ ID: {}", daqId);
        // Kafka 전송 로직 구현 (예시)
        // KafkaProducer.send(daqId, sensorData);
        log.info("[Kafka] 데이터 전송 성공 - DAQ ID: {}", daqId);
    }


    // 리스너 구독 등록

    public void subscribe(String subscribeKey, String daqName, ChannelHandlerContext ctx, Consumer<List<String>> dataHandler) {
        Subscriber newSubscriber = new Subscriber(daqName, dataHandler, ctx); // Subscriber 객체 생성
        subscriberMap.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);
        // computeIfAbsent : subscriberMap에 subscribekey가 존재하지 않으면 새로운 CopyOnWriteArrayList<>를 생성하고 해당 키에 매핑
        // 이미 subscribekey가 존재하면 기존의 값을 반환,
        // -> 이 메서드는 키에 해당하는 값이 없는 경우에만 새로운 값을 계산하고 삽입
        log.info("[구독] 채널: {}, 구독자 등록 - 현재 구독자 수: {}", subscribeKey, subscriberMap.get(subscribeKey).size());
    }


    // 리스너 구독 해제
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
    }*/

}
