package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.service.DataService;
import com.example.daq_monitoring_sw.tcp.util.ChannelRepository;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@RequiredArgsConstructor
@Sharable
public class ChannelHandler extends SimpleChannelInboundHandler<UserRequest> {

    private final DataService dataService;
    private final ChannelRepository channelRepository;
//    private final ChannelRepository channelRepository;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("==================================== Client connected: {} ====================================", ctx.channel().remoteAddress());
        channelRepository.channelActive(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("==================================== Client disConnected: {} ====================================", ctx.channel().remoteAddress());
        channelRepository.channelInactive(ctx);
    }


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
                    dataService.writeData(daqCenter);
                }

                case RQ -> {
                    /* 데이터 SUB */
                    log.info(" Reqeust [{}] start", daqCenter.getStatus());

                    UserRequest checkUser = dataService.checkUser(ctx, userReq);
                    if (checkUser != null) {
                        daqCenter.setStatus(Status.RS);
                        log.info(" RQ Reqeust -> [{}] Change", daqCenter.getStatus());

                        ctx.writeAndFlush(userReq);

                        log.info("시작됨?");
                        // 비동기 실행
                        dataService.startDataTransmission(ctx, checkUser);

                    }
                }

                case ST -> {
                    log.info(" Reqeust [{}] start", daqCenter.getStatus());
                    dataService.handleStopRequest(ctx);
                }

                default -> throw new IllegalStateException("Unexpected value: " + daqCenter.getStatus());
            }

            ctx.writeAndFlush(userReq);

        } else {
            throw new IllegalStateException("저장된 채널 정보가 없습니다.");
        }

    }


}
