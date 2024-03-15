package com.example.daq_monitoring_sw.tcp.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* https://musma.github.io/2023/08/30/netty-socket.html  */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {

    private final ClientMappingService clientMappingService;
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
    }


}
