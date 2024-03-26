package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.pub_sub.DataEventListener;
import com.example.daq_monitoring_sw.tcp.service.DataService;
import com.example.daq_monitoring_sw.tcp.service.ScheduledDataService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final ScheduledDataService scheduledDataService;
    private final DataService dataService;

    // 각 채널과 그에 대응하는 리스너 관리
    private final ConcurrentHashMap<String, DataEventListener> listenerGroup = new ConcurrentHashMap<>();


    /* 예외 발생시 클라이언트와의 연결을 닫고 예외정보 출력 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close(); // 예외 발생 시 채널 닫기
        cause.printStackTrace(); // 네트워크 또는 처리 중 예외 발생 시 호출
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserRequest userReq) throws Exception {

        log.info("channelRead called recevied data:{}", userReq.toString());

        DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        if (daqCenter != null) {
            // DaqCenter 객체 사용
            log.info("저장된 채널 정보:{}", daqCenter.toString());


            switch (daqCenter.getStatus()) {
                case IN -> {
                    return;
                }

                case WD -> {
                    /* 데이터 PUB */
                    // daqId:sensorId
                    log.info(" Reqeust [{}] start", daqCenter.getStatus());
                    dataService.writeData(userReq);
                }

                case RQ -> {
                    /* 데이터 SUB */
                    log.info("Reqeust [{}] start", daqCenter.getStatus());

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
                }



                case ST -> {
                    log.info(" Reqeust [{}] start", daqCenter.getStatus());
                    scheduledDataService.handleStopRequest(ctx);
                }
                default -> throw new IllegalStateException("Unexpected value: " + daqCenter.getStatus());
            }

        } else {
            throw new IllegalStateException("저장된 채널 정보가 없습니다.");
        }

    }


}
