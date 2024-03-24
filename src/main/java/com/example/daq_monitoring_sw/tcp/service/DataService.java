package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

/* https://musma.github.io/2023/08/30/netty-socket.html  */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private RedisProperties.Jedis jedis;


    public void handleStopRequest(ChannelHandlerContext ctx){

    }

    public void writeData(DaqCenter daqCenter) {
    }

    public UserRequest checkUser(ChannelHandlerContext ctx, UserRequest userReq) {
        DaqCenter wdDaqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        String userReqDaqId = userReq.getDaqId();
        String channelDaqId = wdDaqCenter.getDaqId();
        if (channelDaqId.equals(userReqDaqId)){
            /*
            return userRequest = UserRequest.builder()
                    .daqId(wdDaqCenter.getDaqId())
                    .sensorCnt(wdDaqCenter.getSensorCnt())
                    .sensorIdsOrder(wdDaqCenter.getSensorIdsOrder())
                    .build();
            */
            return UserRequest.builder()
                    .daqId("DAQ01")
                    .sensorCnt(2)
                    .sensorIdsOrder(Arrays.asList("FL00","TP00"))
                    .build();
        } else {
            throw new IllegalArgumentException("요청한 DAQ ID가 채널의 DAQ ID와 일치하지 않습니다.");
        }

    }

    public void  startDataTransmission(ChannelHandlerContext ctx, UserRequest checkUser) {
        DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        new Thread(() -> {
            while (daqCenter.getStatus() == Status.RD) {
                Map<String, String> sensorData = new HashMap<>();
                sensorData.put("FL00", "+000.1"); // FL00 센서의 데이터
                sensorData.put("TP00", "+000.2"); // TP00 센서의 데이터

                UserRequest testData = UserRequest.builder()
                        .daqId("DAQ01")
                        .sensorCnt(2)
                        .parsedSensorData(sensorData)
                        .build();

                ctx.writeAndFlush(testData);
                try {
                    Thread.sleep(5); // 예시:  5밀리초마다 데이터 전송
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 중단 조건 충족 시 루프 탈출
                }
            }
        }).start();
    }

/*    private final ClientMappingService clientMappingService;
    private final Map<ChannelHandlerContext, Integer> clientsOrderMap = new ConcurrentHashMap<>();
    private final Map<ChannelHandlerContext, ClientState> clientStates = new ConcurrentHashMap<>();
    private ChannelHandlerContext currentChattingClient;
    private int orderCounter = 0;
    private enum ClientState{
        NORMAL, WRITE, READ
    }

    // 특정 클라이언트에게 메시지 전송
    public void sendRequestToClient(ChannelHandlerContext ctx, String receivedMsg){
        log.info("sendRequest: {}", receivedMsg);

        ctx.writeAndFlush(receivedMsg);
    }

    // 30s 클라이언트의 연결 상태 로깅
    @Scheduled(fixedDelay = 30000)
    public void logClientStatus(){
        for (Map.Entry<ChannelHandlerContext, Integer>entry : clientOrderMap.entrySet()){
            ChannelHandlerContext ctx = entry.getKey();
            int port = ((InetSocketAddress)ctx.channel().remoteAddress().getPort());
            log.info("I`m live. Port: {}, Order: {}", port, entry.getValue() );
        }
    }

    // 10s 모든 클라이언트에게 현재 시각과 포트 뿌리기
    @Scheduled(fixedRate = 10000)
    public void sendTimeToClient(){
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        for (ChannelHandlerContext ctx : clientsOrderMap.keySet()) {
            int port = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
            String message = "Current Time: " + currentTime + ", Your Port: " + port + "\n";
            ByteBuf buf = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
            ctx.writeAndFlush(buf);
        }
    }*/


}
