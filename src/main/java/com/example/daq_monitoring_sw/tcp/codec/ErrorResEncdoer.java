package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.ErrorResponse;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class ErrorResEncdoer extends MessageToByteEncoder<ErrorResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ErrorResponse res, ByteBuf out) throws Exception {
        log.info("Starting encode method for ErrorResponse: {}", res);

        // 본문 데이터 생성
        ByteBuf body = ctx.alloc().buffer();
        try{

            String errorMsg = res.getMsg();
            body.writeBytes(errorMsg.getBytes(StandardCharsets.UTF_8));

            // 헤더
            int bodyLength = body.readableBytes();
            String totalLength = String.format("%03d", bodyLength+7); // stx(1)+length(3)+status(2)+etx(1)

            // stx
            out.writeByte(ProtocolState.STX.getValue());
            // 전체 패킷 길이
            out.writeBytes(totalLength.getBytes(StandardCharsets.UTF_8));
            // command
            String status = String.valueOf(res.getStatus());
            out.writeBytes(status.getBytes(StandardCharsets.UTF_8));
            //body
            out.writeBytes(body);
            //etx
            out.writeByte(ProtocolState.ETX.getValue());

            log.debug("Encoded Error Data: {}", out.toString(StandardCharsets.UTF_8));
        } finally {
            body.release();
        }

    }
}
