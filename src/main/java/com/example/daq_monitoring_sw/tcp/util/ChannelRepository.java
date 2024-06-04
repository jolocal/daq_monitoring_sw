//package com.example.daq_monitoring_sw.tcp.util;
//
//import com.example.daq_monitoring_sw.web.service.WebChannelEventService;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.util.AttributeKey;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Collection;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.ConcurrentHashMap;
//
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ChannelRepository extends ChannelInboundHandlerAdapter {
//
//    private final WebChannelEventService channelEventService;
//
//    public static final AttributeKey<DaqEntity> DAQ_CENTER_KEY = AttributeKey.valueOf("DAQ_CENTER");
//    private static final Map<String, DaqEntity> channelGroup = new ConcurrentHashMap<>();
//
//
//    // 특정 채널의 현재 상태 조회
//    public DaqEntity currentDaqStatus(String daqId){
//        return channelGroup.get(daqId);
//    }
//
//    // 특정 채널 조회
//    public Optional<DaqEntity> findChannel(String daqId) {
//        return Optional.ofNullable((DaqEntity) channelGroup.get(daqId));
//    }
//
//    // 모든 채널 정보 조회
//    public static Collection<DaqEntity> findAllChannel(){
//        return channelGroup.values();
//    }
//
//    public static void putChannel(String daqId, DaqEntity daqEntity) {
//        channelGroup.put(daqId,daqEntity);
//        log.info("[channelRepository] channelGroup put : {}",channelGroup);
//    }
//
//    public void removeChannel(String channelId) {
//        channelGroup.remove(channelId);
//    }
//
//}
///*
//    // 모든 연결된 채널에 메시지를 전송
//    public static void sendMessageToAll(String message){
//
//        for (channelGroup channel : )
//
//
//        for (ChannelHandlerContext ctx: channelGroup.values()){
//            ctx.writeAndFlush(message);
//        }
//    }
//
//    public DaqEntity findChannelAttr(String daqId){
//        DaqEntity daqEntity = channelGroup.get(daqId);
//        return daqEntity;
//    }
//*/
