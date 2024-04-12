package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.dto.UserResponse;
import com.example.daq_monitoring_sw.tcp.pub_sub.ProcessingDataManager;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelDataHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final ChannelRepository channelRepository;
    private final ProcessingDataManager dataManager;

    /* 예외 발생시 클라이언트와의 연결을 닫고 예외정보 출력 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // 네트워크 또는 처리 중 예외 발생 시 호출
        log.error("error: {}", cause.getMessage());
        ctx.close(); // 예외 발생 시 채널 닫기
    }

    /* 서버는 들어오는 데이터를 하나의 패킷으로 처리하고 있으며, 각 파이프라인은 독립적으로 수행되고 있는 것으로 보입니다.*/
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserRequest userReq) throws Exception {

        DaqCenter currentChannel = ctx.channel().attr(DAQ_CENTER_KEY).get();

        if (currentChannel != null) {
            switch (currentChannel.getStatus()) {
                case IN -> {
                    return;
                }

                case WD -> {
//                    dataManager.writeData(userReq);
                    dataManager.writeData(userReq);
                }

                // 리스너 생성, 데이터 발행 클래스에 등록
                case RQ -> {

                    // wd-channel 확인
                    Optional<DaqCenter> wdActiveChannel = channelRepository.findChannel(userReq.getReadTo());

                    // WD 사용자 확인 (존재하지 않더라도 리스너 등록)
                    if (wdActiveChannel.isEmpty()) {
                        log.info("활성화된 WD 채널이 없습니다. 리스너 등록을 진행합니다.");
                    } else {
                        // WD 사용자가 존재할 경우의 추가 로직
                        // RS: 1차응답
                        DaqCenter currentWdDaqcenter = wdActiveChannel.get();
                        log.info("활성화 중인 WD 채널 정보: {}", currentWdDaqcenter);

                        UserResponse firstRes = createFirstRes(currentWdDaqcenter);

                        ctx.writeAndFlush(firstRes);
                    }

                    //////////////////////////////////////////////////////////////////////

                    // RD: 2차응답 준비 및 리스너 등록
                    log.info("currentChannel info: {}", currentChannel.toString());

                    String channelId = currentChannel.getChannelId();
                    String subscribeKey = currentChannel.getReadTo();


                    dataManager.subscribe(subscribeKey, channelId, queue -> {
                        // buffer.retain();

                        log.info("[응답 전] {} 에게 발행된 복사된 데이터: {} 데이터사이즈: {}", channelId, queue, queue.size());

                        UserResponse response = UserResponse.builder()
                                .status(Status.RD)
                                .readTo(subscribeKey) //daqId
                                .sensorCnt(queue.size())
                                .resDataList(queue)
                                .build();

                        ctx.writeAndFlush(response);

                        log.info("[응답 후] {} 에게 발행된 복사된 데이터: {} 데이터사이즈: {}", channelId, queue, queue.size());
                    });
                }
                case ST -> {
                    log.info("==================================== Reqeust [{}] start ====================================", currentChannel.getStatus());
                    String daqId = currentChannel.getDaqId();
                }
                default -> throw new IllegalStateException("Unexpected value: " + currentChannel.getStatus());
            }

        } else {
            throw new IllegalStateException("NETTY 채널에 저장된 사용자 정보가 없습니다.");
        }

    }

    private UserResponse createFirstRes(DaqCenter currentDaqcenter) {
        // 1차 응답 생성 로직
        return UserResponse.builder()
                .status(Status.RS)
                .daqId(currentDaqcenter.getDaqId())
                .sensorCnt(currentDaqcenter.getSensorCnt())
                .sensorIdsOrder(currentDaqcenter.getSensorIdsOrder())
                .build()
                ;
    }
}
