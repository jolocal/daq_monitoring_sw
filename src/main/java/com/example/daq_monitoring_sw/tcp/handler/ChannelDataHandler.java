package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataEventListener;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataManager;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataPublisher;
import com.example.daq_monitoring_sw.tcp.pub_sub.Listener;
import com.example.daq_monitoring_sw.tcp.service.DataService;
import com.example.daq_monitoring_sw.tcp.service.ScheduledDataService;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final ScheduledDataService scheduledDataService;
    private final DataService dataService;
    private final ChannelRepository channelRepository;
    private final DataPublisher dataPublisher;
    private final DataManager dataManager;

    // 각 채널과 그에 대응하는 리스너 관리
//    private final ConcurrentHashMap<String, DataEventListener> listenerGroup = new ConcurrentHashMap<>();


    /* 예외 발생시 클라이언트와의 연결을 닫고 예외정보 출력 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close(); // 예외 발생 시 채널 닫기
        cause.printStackTrace(); // 네트워크 또는 처리 중 예외 발생 시 호출
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserRequest userReq) throws Exception {

        DaqCenter currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();

        if (currentChannel != null) {
            switch (currentChannel.getStatus()) {
                case IN -> {
                    return;
                }

                case WD -> {
                    /* 데이터 PUB */
                    // daqId:sensorId
                    dataManager.writeData(userReq);
//                    dataService.writeData(userReq);
                }

                // 리스너 생성, 데이터 발행 클래스에 등록
                case RQ -> {
                    // wd-channel 확인
                    Optional<DaqCenter> wdActiveChannel = channelRepository.findChannel(userReq.getReadTo());

                    if (!wdActiveChannel.isPresent()) {
                        log.info("활성화된 WD 채널이 없습니다.");
                        return;
                    }

                    // RS: 1차응답
                    DaqCenter currentDaqcenter = wdActiveChannel.get();
                    log.info("활성화 중인 WD 채널 정보: {}", currentDaqcenter);

                    UserRequest firstRes = createFirstRes(currentDaqcenter);
                    ctx.writeAndFlush(firstRes);

                    //////////////////////////////////////////////////////////////////////

                    // RD: 2차응답
                    String channelId = currentChannel.getChannelId();
                    dataManager.subscribe(channelId, data -> {
                        // 여기서 실시간으로 발행된 데이터를 클라이언트에게 전송하는 로직 작성
                        UserRequest ResponseData = UserRequest.builder()
                                .status(Status.RD)
                                .sensorCnt(firstRes.getSensorCnt())
                                .sensorIdsOrder(firstRes.getSensorIdsOrder())
                                .parsedSensorData(data)
                                .build();

                        ctx.writeAndFlush(ResponseData);

                    });
                }

                case ST -> {
                    log.info(" Reqeust [{}] start", currentChannel.getStatus());
                }
                default -> throw new IllegalStateException("Unexpected value: " + currentChannel.getStatus());
            }

        } else {
            throw new IllegalStateException("저장된 채널 정보가 없습니다.");
        }

    }

    private UserRequest createFirstRes(DaqCenter currentDaqcenter) {
        // 1차 응답 생성 로직
        return UserRequest.builder()
                .status(Status.RS)
                .daqId(currentDaqcenter.getDaqId())
                .sensorCnt(currentDaqcenter.getSensorCnt())
                .sensorIdsOrder(currentDaqcenter.getSensorIdsOrder())
                .build();
    }

    private void handleRequest(ChannelHandlerContext ctx, UserRequest userReq) {
        // RQ 처리 로직
        Optional<DaqCenter> wdActiveChannel = channelRepository.findChannel(userReq.getReadTo());

        if (!wdActiveChannel.isPresent()) {
            log.info("활성화된 WD 채널이 없습니다.");
            return;
        }

        // 구독 설정 로직
        String channelId = userReq.getChannelId();

    }


//    // RQ요청에 대응하는 리스너 생성, ( WD 데이터가 업데이트될 때마다 호출되며, 변경된 데이터를 클라이언트에게 전송 )
//    private DataEventListener createDataEventListener(ChannelHandlerContext ctx, UserRequest lastRes) {
//        return collectedData -> {
//            lastRes.setStatus(Status.RD);
//            lastRes.setParsedSensorData(collectedData);
//
///*            UserRequest res = UserRequest.builder()
//                    .status(Status.RD)
//                    .sensorCnt(firstRes.getSensorCnt())
//                    .sensorIdsOrder(firstRes.getSensorIdsOrder())
//                    .parsedSensorData(collectedData)
//                    .build();*/
//
//            log.info("onDataRecevied res: {}", lastRes);
//
//            ctx.writeAndFlush(lastRes);
//        };
//    }

}

//                        listenerGroup.put(userReq.getChannelId(), dataEventListener);
//                        String rq_daqId = daqCenter.getDaqId();
//                        listenerGroup.put(rq_daqId, dataEventListener);
//                        dataService.subscribeToData(rq_daqId, dataEventListener);

//                    // daqcenter 확인
//                    Optional<DaqCenter> optionalDaqCenter = channelRepository.getChannelDaqCenter(userReq.getDaqId());
//                    if (optionalDaqCenter.isPresent()) {
//                        DaqCenter foundDaqCenter = optionalDaqCenter.get();
//                        log.info("현재 접속된 사용자 정보: {}", foundDaqCenter);
//
//                        // 리스너 생성 및 등록
//                        DataEventListener dataEventListener = new DataEventListener() {
//                            // 2차 응답
//                            @Override
//                            public void onDataReceived(Map<String, String> collectedData) {
//                                UserRequest res = UserRequest.builder()
//                                        .status(Status.RD)
//                                        .parsedSensorData(collectedData)
//                                        .build();
//                                log.info("Response for data received");
//                                ctx.writeAndFlush(res);
//                            }
//                        };
//
//                        listenerGroup.put(userReq.getDaqId(), dataEventListener);
//                        // 데이터 구독
//                        dataService.subscribeToData(userReq, dataEventListener);
//
//                        // 1차 응답
//                        log.info("First response: {}", userReq);
//                        UserRequest firstRes = UserRequest.builder()
//                                .status(Status.RS)
//                                .daqId(foundDaqCenter.getDaqId())
//                                .sensorCnt(foundDaqCenter.getSensorCnt())
//                                .sensorIdsOrder(foundDaqCenter.getSensorIdsOrder())
//                                .build();
//
//                        ctx.writeAndFlush(firstRes);
//
//                    } else {
//                        log.info("DaqCenter not found for ID: {}", userReq.getDaqId());
//                    }

/*
                    // TEMP 데이터 보내기 //
                    UserRequest checkUser = scheduledDataService.checkUser(ctx, userReq);
                    if (checkUser != null) {
                        daqCenter.setStatus(Status.RS);
                        log.info(" RQ Reqeust -> [{}] Change", daqCenter.getStatus());

                        ctx.writeAndFlush(checkUser);

                        daqCenter.setStatus(Status.RD);

                        // 비동기 실행
                        CompletableFuture<Void> future = scheduledDataService.startDataTransmission(ctx, userReq);

                        future.thenRun(() ->
                                        log.info("Data transmission completed successfully."))
                                .exceptionally(ex -> {
                                    log.error("An error occurred during data transmission: ", ex);
                                    return null;
                                });
                    }
                    */

//                    ST 요청시
//                    scheduledDataService.handleStopRequest(ctx);
