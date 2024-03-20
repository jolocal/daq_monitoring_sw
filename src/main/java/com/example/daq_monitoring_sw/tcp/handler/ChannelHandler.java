package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.service.DataService;
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


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("==================================== Client connected: {} ====================================", ctx.channel().remoteAddress() );
        String daqId = String.valueOf(ctx.channel().attr(DAQ_CENTER_KEY).get());
        channelRepository.addClient(daqId, ctx.channel());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("==================================== Client disConnected: {} ====================================", ctx.channel().remoteAddress());
        String daqId = String.valueOf(ctx.channel().attr(DAQ_CENTER_KEY).get());
        channelRepository.removeClient(daqId);
        ctx.close(); // 채널 닫기
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
        }

        ctx.writeAndFlush(userReq);

        switch (daqCenter.getStatus()){
            case WD ->  log.info(" Reqeust [{}] start", daqCenter.getStatus());

            /* 리스너 등록 */
            case RQ ->  log.info(" Reqeust [{}] start", daqCenter.getStatus());
            /* 데이터 구독, 이벤트 발생 */
            case RD ->  log.info(" Reqeust [{}] start", daqCenter.getStatus());

            case ST -> {
                log.info(" Reqeust [{}] start", daqCenter.getStatus());
                dataService.handleStopRequest(ctx);
            }

            default -> throw new IllegalStateException("Unexpected value: "+ daqCenter.getStatus());
        }


      /*  try{
            UserRequest userRequest = (UserRequest) userReq;
            // 데이터 처리 로직
            // dataService.(ctx,receivedMessage)
            // UserRequest 객체의 내용을 로깅합니다.
            log.info("UserRequest: {}", userRequest.toString());
            // 추가적으로 UserRequest의 세부 내용을 로그로 출력할 수 있습니다.
            log.info("DAQ ID: {}", userRequest.getDaqId());
            log.info("Sensor Count: {}", userRequest.getSensorCnt());
            log.info("Sensor IDs Order: {}", userRequest.getSensorIdsOrder());
            if (userRequest.getParsedSensorData() != null) {
                userRequest.getParsedSensorData().forEach((sensorId, data) ->
                        log.info("Sensor ID: {}, Data: {}", sensorId, data));
            }else {
                // msg가 UserRequest 타입이 아닌 경우의 처리 (필요에 따라)
                log.warn("Message received is not of type UserRequest: {}", userReq.getClass().getSimpleName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/

        log.info("encode complete");
    }


}
