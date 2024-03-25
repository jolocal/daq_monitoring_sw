package com.example.daq_monitoring_sw.tcp.service;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

import static com.example.daq_monitoring_sw.tcp.codec.ReqDecoder.DAQ_CENTER_KEY;

/* https://musma.github.io/2023/08/30/netty-socket.html  */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDataService {

    private ScheduledExecutorService executor;


    public void handleStopRequest(ChannelHandlerContext ctx){
        if (executor != null && ! executor.isShutdown()){
            executor.shutdownNow();
            log.info("Data transmission scheduler stopped.");
        }
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
                    .sensorIdsOrder(Arrays.asList("PR01","TE02"))
                    .build();
        } else {
            throw new IllegalArgumentException("요청한 DAQ ID가 채널의 DAQ ID와 일치하지 않습니다.");
        }

    }

    public CompletableFuture<Void> startDataTransmission(ChannelHandlerContext ctx, UserRequest checkUser) {
        log.info("시작?");

        DaqCenter daqCenter = ctx.channel().attr(DAQ_CENTER_KEY).get();

        CompletableFuture<Void> future = new CompletableFuture<>();
        executor = Executors.newSingleThreadScheduledExecutor();

        double[] sensorValue = {0.0}; // 센서 값 초기화

        // 주기적인 데이터 전송 작업
        Runnable task = () -> {
            if (daqCenter.getStatus() != Status.RD || sensorValue[0] > 999.9) {
                executor.shutdownNow();
                future.complete(null); // 모든 데이터 전송 완료
                return;
            }

            Map<String, String> sensorData = new HashMap<>();
            sensorData.put("PR01", String.format("+%05.1f", sensorValue[0]));
            sensorData.put("TE02", String.format("+%05.1f", sensorValue[0]));

            UserRequest testData = checkUser.builder() // 이전 UserRequest를 기반으로 새 객체 생성
                    .daqId("DAQ01")
                    .status(Status.RD)
                    .sensorCnt(2)
                    .sensorIdsOrder(Arrays.asList("PR01","TE02"))
                    .parsedSensorData(sensorData)
                    .build();

            ctx.writeAndFlush(testData);

            sensorValue[0] += 0.1; // 센서 값 증가
        };

        executor.scheduleAtFixedRate(task, 0, 5, TimeUnit.MILLISECONDS);

        return future;
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
