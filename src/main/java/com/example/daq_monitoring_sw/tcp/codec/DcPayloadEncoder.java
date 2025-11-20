package com.example.daq_monitoring_sw.tcp.codec;


import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.daq_monitoring_sw.tcp.dto.DcPayload;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DcPayloadEncoder extends MessageToByteEncoder<DcPayload>{

    @Override
    protected void encode(ChannelHandlerContext ctx, DcPayload dcPayload, ByteBuf out) throws Exception {
        String status = "DC";
        ByteBuf body = ctx.alloc().buffer();

        try{
            // body
            body.writeBytes(dcPayload.getTargetDeviceId().getBytes(StandardCharsets.UTF_8));
            body.writeBytes(Integer.toString(dcPayload.getSensorCnt()).getBytes(StandardCharsets.UTF_8));
            body.writeBytes(Long.toString(dcPayload.getTs_ms()).getBytes(StandardCharsets.UTF_8));
            
            List<Double> values = dcPayload.getValues();
            for (Double v : values) {
                String v2 = String.format("%+08.2f", v);
                body.writeBytes(v2.getBytes(StandardCharsets.UTF_8));
            }
            
            // 헤더
            int fixLength = 7; 
            int totalLength = body.readableBytes() + fixLength;
            String str_totalLength = String.format("%03d", totalLength);

            out.writeByte(ProtocolState.STX.getValue()); //stx
            out.writeBytes(str_totalLength.getBytes(StandardCharsets.UTF_8)); //totalLength
            out.writeBytes(status.toString().getBytes(StandardCharsets.UTF_8)); // cmd
            out.writeBytes(body);
            out.writeByte(ProtocolState.ETX.getValue()); // etx

            log.debug("  ▷ [Payload] Encoded: {}", out.toString(StandardCharsets.UTF_8));

        } finally {
            body.release();
        }
    }

}
