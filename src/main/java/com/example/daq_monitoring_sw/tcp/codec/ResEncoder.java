package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ResEncoder extends MessageToByteEncoder<UserRequest> {
/*
    STX	0	0	R	S	D	A	Q	0	0	0	0	F/P/T/D	L/R/E/U	0	0	ETX
    STX	0	0	R	D	0	0	0 or -	0	0	0	0	ETX
*/

    @Override
    protected void encode(ChannelHandlerContext ctx, UserRequest res, ByteBuf out) throws Exception {

        // 본문 데이터 생성
        ByteBuf body = Unpooled.buffer();

        switch (res.getCommand()) {
            case RS:
                String daqId_rs = res.getDaqId();
                body.writeBytes(daqId_rs.getBytes(StandardCharsets.UTF_8));

                int sensorCnt = res.getSensorCnt();
                String sensorCntStr = String.format("%02d", sensorCnt);
                body.writeBytes(sensorCntStr.getBytes(StandardCharsets.UTF_8));

                for (String sensorId: res.getSensorIdsOrder()){
                    body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));
                }
                break;

            case RD:
                String daqId_rd = res.getDaqId();
                body.writeBytes(daqId_rd.getBytes(StandardCharsets.UTF_8));

                for (String sensorId: res.getSensorIdsOrder()){
                    body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));

                    String sensorData = res.getParsedSensorData().get(sensorId);
                    if (sensorData != null){
                        body.writeBytes(sensorData.getBytes(StandardCharsets.UTF_8));
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("지원되지 않는 명령어 유형");
        }

        // 헤더 작성
        int packetLength = body.readableBytes() + 5; // STX(1) + length(2) + command(2) + ETX(1)
        String packetLengthStr = String.format("%02d",packetLength);

        out.writeByte(ProtocolState.STX.getValue()); // STX
        out.writeBytes(packetLengthStr.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(body);
        out.writeByte(ProtocolState.ETX.getValue()); // ETX

        ctx.writeAndFlush(out);

    }


}
