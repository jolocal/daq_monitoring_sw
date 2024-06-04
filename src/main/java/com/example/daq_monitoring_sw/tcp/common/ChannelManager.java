package com.example.daq_monitoring_sw.tcp.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelManager {
    public  final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public  final Map<ChannelId, Client> clientInfoMap = new ConcurrentHashMap<>();
    public  final ConcurrentMap<String, ChannelId> daqToChannelIdMap  = new ConcurrentHashMap<>();

    public  void addChannel(Channel channel) {
        allChannels.add(channel);
    }

    public  void removeChannel(Channel channel) {
        allChannels.remove(channel);
        clientInfoMap.remove(channel.id());
    }

    public  void addClientInfo(Channel channel, Client client) {
        clientInfoMap.put(channel.id(), client);
        log.info("addClientInfo: {}", clientInfoMap.get(channel.id()));
    }

    public  Client getClientInfo(Channel channel) {
        return clientInfoMap.get(channel.id());
    }

    public  Optional<Client> findActiveCliByDaqName(String daqName) {
        ChannelId channelId = daqToChannelIdMap.get(daqName);
        log.info("channelId: {} ", channelId);
        if (channelId != null) {
            Client client = clientInfoMap.get(channelId);
            if (client != null && client.getStatus() == Status.WD){
                return Optional.of(client);
            }
        }
        return Optional.empty();
    }

    public void updateDaqName(Channel channel, String daqName) {
        Client client = clientInfoMap.get(channel.id());
        if (client != null) {            log.info("클라이언트 정보 찾음 - Channel ID: {}, Client: {}", channel.id(), client);
            client.setDaqName(daqName);
            daqToChannelIdMap.put(daqName, channel.id());
            log.debug("클라이언트 DAQ 이름 설정 및 daqToChannelIdMap 값 추가 - DaqName: {}", daqName);
        } else {
            log.warn("클라이언트를 찾을 수 없음 - Channel ID: {}", channel.id());
        }
    }
}
