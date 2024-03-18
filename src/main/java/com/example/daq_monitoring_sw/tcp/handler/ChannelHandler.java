package com.example.daq_monitoring_sw.tcp.handler;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/* Netty Server의 핵심부분 */
/* 클라이언트와의 연결 수립, 데이터 읽기 및 쓰기, 예외처리 등의 로직 */
@Slf4j
@Component
@RequiredArgsConstructor
@io.netty.channel.ChannelHandler.Sharable
public class ChannelHandler extends ChannelInboundHandlerAdapter {

//    private final DataService dataService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disConnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channelRead called");
        UserRequest user = (UserRequest) ctx.channel().attr(AttributeKey.valueOf("user")).get();

        try{
            UserRequest userRequest = (UserRequest) msg;
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
                log.warn("Message received is not of type UserRequest: {}", msg.getClass().getSimpleName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* 예외 발생시 클라이언트와의 연결을 닫고 예외정보 출력 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
