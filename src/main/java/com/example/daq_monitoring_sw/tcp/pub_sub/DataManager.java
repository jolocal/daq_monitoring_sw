package com.example.daq_monitoring_sw.tcp.pub_sub;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;


@Slf4j
@Component
public class DataManager {

    // DB용 dataMap
    private final Map<String, String> dataMap = new ConcurrentHashMap<>(); // daqId:PR01 = +000.0


    // 메시지 처리 큐
    // private final Queue<UserRequest> dataQueue = new ConcurrentLinkedQueue<>();
//    private final Map<String, List<Consumer<List<String>>>> subscribers = new ConcurrentHashMap<String, Consumer<List<String>>>(); // channelId,
    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    // 1:N 데이터 저장 및 발행
    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>(); // 각 DAQID별로 데이터를 처리하기 위한 스레드 풀
    private final Map<String, List<String>> dataStorage = new ConcurrentHashMap<>(); // 각 DAQID별 데이터 저장소



    /*
    * 각각의 쓰기 요청은 독립된 스레드(ExecutorService 내)에서 처리
    */
    // 1:N 데이터 저장 및 발행
    public void writeData(UserRequest userRequest){
        String daqId = userRequest.getDaqId();

        // DAQID 별로 별도의 ExecutorService 생성 및 관리
        executorServices.computeIfAbsent(daqId, k -> Executors.newSingleThreadExecutor());

        ExecutorService executorService = executorServices.get(daqId);

        // 데이터 처리를 비동기적으로 수행
        executorService.submit(() -> {
            List<String> processedData  = processData(userRequest);
            log.info("processData: {}", processedData);
            // 데이터 발행
            publishData(daqId, processedData);
        });
    }


    private List<String> processData(UserRequest userRequest){
        String daqId = userRequest.getDaqId();
        List<String> sensorIdsOrder = userRequest.getSensorIdsOrder();
        Map<String, String> parsedSensorData = userRequest.getParsedSensorData();

        List<String> newSensorData = new ArrayList<>();

        for (String sensorId : sensorIdsOrder){
            if (parsedSensorData.containsKey(sensorId)){
                String dataValue = parsedSensorData.get(sensorId);
                newSensorData.add(dataValue);
                // TODO: dataStorage의 사용 유무?
                // dataStorage.computeIfAbsent(daqId, K -> new ArrayList<>()).add(dataValue);
            }
        }
        return newSensorData;
        // 데이터 처리 로직 추가 (예: 데이터 저장, 로깅, 기타)

    }

    // 1:1 데이터 저장 및 발행
/*    public void writeData(UserRequest userRequest) {
        // 큐에 메시지 추가
        dataQueue.add(userRequest);
        log.info("[ 데이터 큐 사이즈 ]: {}", dataQueue.size());

        // 큐에서 데이터 처리
        if (!dataQueue.isEmpty()) {
//            UserRequest peek = dataQueue.peek();
            UserRequest poll = dataQueue.poll();
            String daqId = poll.getDaqId();
            List<String> sensorIdsOrder = poll.getSensorIdsOrder();
            Map<String, String> parsedSensorData = poll.getParsedSensorData();

            // 데이터 리스트 생성
             List<String> resDataList = new ArrayList<>();

            for (String sensorId : sensorIdsOrder) {
                if (parsedSensorData.containsKey(sensorId)) {
                    String dataValue = parsedSensorData.get(sensorId);
                    resDataList.add(dataValue);
                }
            }
            log.info("[writeData] daqId: {} sensorId: {} dataValue: {}", daqId, sensorIdsOrder, resDataList);

            publishData(daqId, resDataList);

        }
    }*/

    // 데이터 발행
    private void publishData(String key, List<String> resDataList) {
        if (subscribers.containsKey(key)) {
            for (Subscriber subscriber : subscribers.get(key)) {
                subscriber.getConsumer().accept(resDataList);
            }
        }
//        if (subscribers.containsKey(key)){
//            log.info("데이터 발행: {} 채널에 {} 데이터 발행", key, resDataList);
//            for (Consumer<List<String>> consumer : subscribers.get(key)){
//                consumer.accept(resDataList);
//            }
//        }

//        if (subscribers.containsKey(key)) {
//            log.info("데이터 발행: {} 채널에 {} 데이터 발행", key, resDataList);
//            subscribers.get(key).accept(resDataList);
//        }
    }

    public void subscribe(String subscribeKey, String channelId, Consumer<List<String>> consumer) {
        Subscriber newSubscriber = new Subscriber(consumer,channelId);

        subscribers.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(newSubscriber);
        log.info("[ {} ] 채널에 [{}] 구독자 등록, 현재 구독자 수: {}", subscribeKey, channelId, subscribers.get(subscribeKey).size());
//        subscribers.computeIfAbsent(subscribeKey, k -> new CopyOnWriteArrayList<>()).add(consumer);
//        log.info("[ {} ] 채널 구독자 등록, 현재 구독자 수: {}", subscribeKey, subscribers.get(subscribeKey).size());
//        subscribers.put(subscribeKey,consumer);
//        log.info("{} 채널 구독자 등록, 현재 구독자 수: {}", subscribeKey, subscribers.size());

    }

    public void unSubscribe(String subscribeKey, String channelId){

        // TODO: 수정후 : 구독자 목록에서 특정 channelId에 해당하는 사용자를 제거
        if (subscribers.containsKey(subscribeKey)) {
            List<Subscriber> subscriberList = subscribers.get(subscribeKey);

            // 지정된 channelId를 가진 구독자를 찾아 제거합니다.
            boolean removed = subscriberList.removeIf(subscriber -> subscriber.getChannelId().equals(channelId));

            if (removed) {
                log.info("[{}] 채널의 [{}] 구독자 해제 완료, 현재 구독자 수: {}", subscribeKey, channelId, subscriberList.size());
            } else {
                log.info("[{}] 채널에 [{}] 구독자가 존재하지 않습니다.", subscribeKey, channelId);
            }
        } else {
            log.info("[{}] 채널에 대한 구독자가 존재하지 않습니다.", subscribeKey);
        }

        // 수정전
/*        if(subscribers.containsKey(subscribeKey)) {
            subscribers.get(subscribeKey).removeIf(subscriber -> subscriber.getChannelId().equals(channelId));
            log.info("[ {} ] 채널의 [{}] 구독자 해제, 현재 구독자 수: {}", subscribeKey, channelId, subscribers.get(subscribeKey).size());
        } else {
            log.info("[ {} ] 채널에 대한 구독자가 존재하지 않습니다. 채널 [{}] 연결이 종료되었습니다.", subscribeKey, channelId);
        }*/
    }


    public void shutdownExecutors() {
        for (ExecutorService executor : executorServices.values()) {
            executor.shutdown(); // ExecutorService를 안전하게 종료
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // 종료가 늦어질 경우 강제 종료
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
