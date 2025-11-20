package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.Response;
import com.example.daq_monitoring_sw.tcp.dto.RqInfoRes;
import com.example.daq_monitoring_sw.tcp.common.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Slf4j
@Component
public class SensorDataResEncoder extends MessageToByteEncoder<Response> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Response res, ByteBuf out) throws Exception {

        String rsType = res.getRsType().toString();
        ByteBuf body = ctx.alloc().buffer();
        try {
            
            if (rsType.equals("IS")){
                body.writeBytes(res.getResult().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getRsType().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getTargetDeviceId().toString().getBytes(StandardCharsets.UTF_8));
                List<String> sensorList = res.getSensorList(); // ["PF", "PB", ...]
                for (String sid : sensorList) {
                    String s2 = sid == null ? "" : sid;
                    // 2바이트 고정: 부족하면 공백 패딩, 넘치면 자르기
                    s2 = String.format("%-2s", s2).substring(0, 2);
                    body.writeBytes(s2.getBytes(StandardCharsets.UTF_8));
                }
            } else if (rsType.equals("IE")){
                body.writeBytes(res.getResult().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getRsType().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getMessage().toString().getBytes(StandardCharsets.UTF_8));
            } else if (rsType.equals("IN")) {
                body.writeBytes(res.getResult().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getRsType().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getMessage().toString().getBytes(StandardCharsets.UTF_8));
            }
            else {
                body.writeBytes(res.getResult().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getRsType().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getDeviceId().toString().getBytes(StandardCharsets.UTF_8));
                body.writeBytes(res.getMessage().toString().getBytes(StandardCharsets.UTF_8));
            }

            // 헤더
            int fixLength = 7; 
            int totalLength = body.readableBytes() + fixLength;
            String totalLengthStr = String.format("%03d", totalLength);
            // stx
            out.writeByte(ProtocolState.STX.getValue());
            // 전체 패킷 길이
            out.writeBytes(totalLengthStr.getBytes(StandardCharsets.UTF_8));
            // command
            out.writeBytes(rsType.toString().getBytes(StandardCharsets.UTF_8));
            // body
            out.writeBytes(body);
            // etx
            out.writeByte(ProtocolState.ETX.getValue());

            log.debug("  ▷ [SensorDataResEncoder] Encoded Data: {}", out.toString(StandardCharsets.UTF_8));

        } finally {
            body.release();
        }
    }


}
