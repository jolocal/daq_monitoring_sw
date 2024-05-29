package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.*;
import com.example.daq_monitoring_sw.tcp.pub_sub.ProcessingDataService;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final ChannelRepository channelRepository;
    private final ProcessingDataService dataManager;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserRequest userReq) throws Exception {
        log.debug("메시지 수신: {}", userReq);
        DaqCenter currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();

        if (currentChannel != null) {
            switch (currentChannel.getStatus()) {
                case IN -> {
                    return;
                }
                case WD -> handleWDCommand(userReq);
                case RQ -> handleRQCommand(ctx, currentChannel, userReq);
                case ST -> handleSTCommand(ctx, currentChannel, userReq);
                default -> throw new IllegalStateException("예상치 못한 상태: " + currentChannel.getStatus());
            }
        } else {
            throw new IllegalStateException("NETTY 채널에 저장된 사용자 정보가 없습니다.");
        }
    }

    // 'WD' 쓰기
    private void handleWDCommand(UserRequest userReq) {
        log.info("'WD' 명령 처리 - DAQ ID: {}", userReq.getDaqId());
        dataManager.writeData(userReq);
    }

    // 'RQ' 읽기 응답
    private void handleRQCommand(ChannelHandlerContext ctx, DaqCenter currentChannel, UserRequest userReq) {
        log.info("'RQ' 명령 처리 - ChannelId: {} for DAQID: {}", currentChannel.getChannelId(), currentChannel.getReadTo());
        Optional<DaqCenter> wdActiveChannel = channelRepository.findChannel(userReq.getReadTo());

        wdActiveChannel.ifPresentOrElse(currentWdDaqcenter -> {
            log.info("활성화된 WD 채널 정보: {}", currentWdDaqcenter);
            SensorDataResponse firstRes = createFirstRes(currentWdDaqcenter);
            ctx.writeAndFlush(firstRes).addListener(future -> {
                if (future.isSuccess()) {
                    // 첫번째 응답 전송 후 리스너 등록
                    // 리스너 등록 및 데이터 발행
                    regListenerAndDataPub(ctx, currentChannel);
                } else {
                    log.error("첫 번째 응답 전송 실패: {}", future.cause());
                }
            });
        }, () -> {
            log.info("활성화된 WD 채널이 없음.");
            ErrorResponse noWdResponse = createNoWdResponse(currentChannel);
            // 응답 전송 후 채널 닫기
            ctx.writeAndFlush(noWdResponse).addListener(ChannelFutureListener.CLOSE);
        });

    }

    private ErrorResponse createNoWdResponse(DaqCenter currentChannel) {
        return ErrorResponse.builder()
                .status(Status.ER)
                .msg("No active WD channel found for DAQ ID: " + currentChannel.getReadTo())
                .build();
    }


    // 'ST' 종료
    private void handleSTCommand(ChannelHandlerContext ctx, DaqCenter currentChannel, UserRequest userReq) {
        log.info("'ST' 명령 처리 - DAQ ID: {}", currentChannel.getDaqId());

        if (currentChannel.getPreviousStatus() == Status.WD) {
            dataManager.stopAndCleanup(userReq.getDaqId());
        }

        if (currentChannel.getPreviousStatus() == Status.RQ) {
            dataManager.unSubscribe(currentChannel.getReadTo(), currentChannel.getChannelId());
        }
        currentChannel.setCleanupDone(true);
        // 채널 비활성화 이벤트를 다음 핸들러로 전달하여 ChannelManagerHandler의 channelInactive가 호출되도록 함
        ctx.fireChannelInactive();

    }

    // RQ 첫번째 응답
    private SensorDataResponse createFirstRes(DaqCenter currentDaqcenter) {
        // 1차 응답 생성 로직
        return SensorDataResponse.builder()
                .status(Status.RS)
                .daqId(currentDaqcenter.getDaqId())
                .sensorCnt(currentDaqcenter.getSensorCnt())
                .sensorIdsOrder(currentDaqcenter.getSensorIdsOrder())
                .build()
                ;
    }

    // 리스너 등록 및 데이터 발행
    private void regListenerAndDataPub(ChannelHandlerContext ctx, DaqCenter currentChannel) {
        String channelId = currentChannel.getChannelId();
        String subscribeKey = currentChannel.getReadTo();

        dataManager.subscribe(subscribeKey, channelId, ctx, packetList -> {
            log.info("데이터 발행: {} - {}", channelId, packetList.toString());

            if (packetList.isEmpty()) {
                log.warn("빈 패킷 리스트 수신 - DAQ ID: {}", currentChannel.getDaqId());
                return;
            }

            // 응답 생성
            SensorDataResponse response = createResponse(subscribeKey, packetList);
            log.info("response: {}", response);

            // 응답 전송
            sendResponse(ctx, response);

            // 리소스 정리
            packetList.clear();
        });
    }

    private SensorDataResponse createResponse(String subscribeKey, List<String> packetList) {
        String timeStamp = packetList.get(0);
        List<String> resDataList = new ArrayList<>(packetList.subList(1, packetList.size()));

        return SensorDataResponse.builder()
                .status(Status.RD)
                .readTo(subscribeKey)
                .sensorCnt(resDataList.size())
                .timeStamp(timeStamp)
                .resDataList(resDataList)
                .build();
    }

    private void sendResponse(ChannelHandlerContext ctx, SensorDataResponse response) {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(response).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("클라이언트로 응답 전송 실패", future.cause());
                }
            });
        } else {
            log.warn("채널이 비활성화 상태입니다. 클라이언트로 응답을 전송할 수 없습니다.");
        }
    }


/*    private void cleanupChannel(ChannelHandlerContext ctx) {
        DaqCenter currentDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        if (currentDaqCenter != null) {
            performCleanup(currentDaqCenter, currentDaqCenter.getDaqId(), ctx);
            channelRepository.removeChannel(currentDaqCenter.getDaqId());
            log.info("[exceptionCaught] 채널 저장소에서 제거 - DAQ ID: {}", currentDaqCenter.getDaqId());
        }
        ctx.close();
    }

    private void performCleanup(DaqCenter currentDaqCenter, String daqId, ChannelHandlerContext ctx) {
        if (!currentDaqCenter.isCleanupDone()) {
            if (currentDaqCenter.getPreviousStatus() == Status.WD) {
                log.info("[performCleanup] 데이터 발행 중지 및 클린업 시작 - DAQ ID: {}", daqId);
                dataManager.stopAndCleanup(daqId);
                log.info("[performCleanup] 데이터 발행 중지 및 클린업 완료 - DAQ ID: {}", daqId);
            }

            if (currentDaqCenter.getPreviousStatus() == Status.RQ) {
                String subscribeKey = currentDaqCenter.getReadTo();
                String channelId = currentDaqCenter.getChannelId();
                log.info("[performCleanup] 리스너 그룹에서 구독 해제 시작 - Subscribe Key: {}, Channel ID: {}", subscribeKey, channelId);
                dataManager.unSubscribe(subscribeKey, channelId);
                log.info("[performCleanup] 리스너 그룹에서 구독 해제 완료 - Subscribe Key: {}, Channel ID: {}", subscribeKey, channelId);
            }

            currentDaqCenter.setCleanupDone(true);
        }
    }*/

}

 /*       log.debug("Received message: {}", userReq);
        DaqCenter currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();

        if (currentChannel != null) {
            switch (currentChannel.getStatus()) {
                case IN -> {
                    return;
                }
                case WD -> {
                    log.info("Processing 'wd' command for DAQ ID: {}", currentChannel.getDaqId());
                    dataManager.writeData(userReq);
                }

                // 리스너 생성, 데이터 발행 클래스에 등록
                case RQ -> {
                    log.info("Processing 'RQ' command for DAQ ID: {}", currentChannel.getDaqId());
                    // wd-channel 확인
                    Optional<DaqCenter> wdActiveChannel = channelRepository.findChannel(userReq.getReadTo());

                    wdActiveChannel.ifPresentOrElse(currentWdDaqcenter -> {
                        log.info("활성화 중인 'WD' 채널 정보: {}",  currentWdDaqcenter);
                        SensorDataResponse firstRes = createFirstRes(currentWdDaqcenter);
                        ctx.writeAndFlush(firstRes);
                    }, () -> log.info("활성화된 'WD' 채널이 없습니다. 리스너 등록을 진행합니다."));

                    registerListener(ctx, currentChannel);


                    // WD 사용자 확인 (존재하지 않더라도 리스너 등록)
//                    if (wdActiveChannel.isEmpty()) {
//                        log.info("활성화된 WD 채널이 없습니다. 리스너 등록을 진행합니다.");
//                    } else {
//                        // WD 사용자가 존재할 경우의 추가 로직
//                        // RS: 1차응답
//                        DaqCenter currentWdDaqcenter = wdActiveChannel.get();
//                        log.info("활성화 중인 WD 채널 정보: {}", currentWdDaqcenter);
//                        SensorDataResponse firstRes = createFirstRes(currentWdDaqcenter);
//                        ctx.writeAndFlush(firstRes);
//                    }

                    //////////////////////////////////////////////////////////////////////

                    // RD: 2차응답 준비 및 리스너 등록
                    log.info("Registering listener for DAQ ID: {}", currentChannel.getDaqId());
                    String channelId = currentChannel.getChannelId();
                    String subscribeKey = currentChannel.getReadTo();


                    dataManager.subscribe(subscribeKey, channelId, ctx, packetList -> {
                        log.info("Data published to {}: {} packets", channelId, packetList.size());

                        if (packetList.isEmpty()) {
                            log.warn("Received empty packet list for DAQ ID: {}", currentChannel.getDaqId());
                            return;
                        }

                        String timeStamp = packetList.get(0);
                        List<String> resDataList = new ArrayList<>(packetList.subList(1, packetList.size()));

                        SensorDataResponse response = SensorDataResponse.builder()
                                .status(Status.RD)
                                .readTo(subscribeKey)
                                .sensorCnt(resDataList.size())
                                .timeStamp(timeStamp)
                                .resDataList(resDataList)
                                .build();

                        if (ctx.channel().isActive()) { // channel이 활성화 되엇을때
                            ctx.writeAndFlush(response).addListener(future -> {
                                if (!future.isSuccess()){
                                    log.error("Failed to send response to client", future.cause());
                                }
                            });
                        } else {
                            log.warn("Channel is inactive, cannot send response to client");
                        }

                        // 패킷 리스트 클리어 및 참조 해제
                        packetList.clear();
                    });

                }
                case ST -> {
                    log.info("Processing 'ST' command: initiating cleanup for DAQ ID: {}", currentChannel.getDaqId());
                    log.info("current channel info: {}", currentChannel);
                    String channelId = currentChannel.getChannelId();
                    String subscribeKey = currentChannel.getReadTo();

                    // WD 사용자일 경우 데이터 발행 중지
                    if (currentChannel.getPreviousStatus() == Status.WD)
                        dataManager.stopAndCleanup(userReq.getDaqId());

                    // RD 사용자일 경우 리스너그룹에서 구독 해제
                    if (currentChannel.getPreviousStatus() == Status.RQ)
                        dataManager.unSubscribe(subscribeKey, channelId);

                    currentChannel.setCleanupDone(true); // 클린업 완료 기록

                }

                default -> throw new IllegalStateException("Unexpected value: " + currentChannel.getStatus());
            }

        } else {
            throw new IllegalStateException("NETTY 채널에 저장된 사용자 정보가 없습니다.");
        }*/