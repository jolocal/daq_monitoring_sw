package com.example.daq_monitoring_sw.tcp.codec;

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
public class SensorDataResEncoder extends MessageToByteEncoder<RqInfoRes> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RqInfoRes res, ByteBuf out) throws Exception {
        log.info("Starting encode method for RqInfoRes: {}", res);
        Status currentStatus = res.getStatus();

        // 본문 데이터 생성
        ByteBuf body = ctx.alloc().buffer();

        try {
            switch (currentStatus) {
                case RS:
                    String daqName = res.getDaqName();
                    log.info("daqName: {}",daqName);
                    body.writeBytes(daqName.getBytes(StandardCharsets.UTF_8));

                    String sensorCnt = res.getSensorCnt();
                    log.info("getSensorCnt: {}", res.getSensorCnt());
                    body.writeBytes(sensorCnt.getBytes(StandardCharsets.UTF_8));

                    for (String sensorId : res.getSensorList()) {
                        body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));
                    }
                    break;

                case RD:
                    // 센서갯수
                    String cnt = res.getSensorCnt();
                    body.writeBytes(cnt.getBytes(StandardCharsets.UTF_8));
//                    String sensorCnt_rd_str = String.format("%02d", sensorCnt_rd);

                    String cliSentTime = res.getCliSentTime(); // HH:mm:ss.SSS
                    body.writeBytes(cliSentTime.getBytes(StandardCharsets.UTF_8));

                    // 센서 데이터를 바이트로 변환하여 body에 쓰기
                    List<String> resDataList = res.getPacketList();
                    for (String resData : resDataList) {
                        byte[] dataBytes = resData.getBytes(StandardCharsets.UTF_8);
                        body.writeBytes(dataBytes);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("지원되지 않는 명령어 유형");
            }

            // 헤더 작성
            int fixLength = 7; // STX(1) +





            // (3) + status(2) + ETX(1)
            int totalLength = body.readableBytes() + fixLength;
            String totalLengthStr = String.format("%03d", totalLength);

            // stx
            out.writeByte(ProtocolState.STX.getValue());
            // 전체 패킷 길이
            out.writeBytes(totalLengthStr.getBytes(StandardCharsets.UTF_8));
            // command
            out.writeBytes(currentStatus.toString().getBytes(StandardCharsets.UTF_8));
            // body
            out.writeBytes(body);
            // etx
            out.writeByte(ProtocolState.ETX.getValue());

            log.debug("Encoded Data: {}", out.toString(StandardCharsets.UTF_8));

        } finally {
            body.release();
        }
    }


}
