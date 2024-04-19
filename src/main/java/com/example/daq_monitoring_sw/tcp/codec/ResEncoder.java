package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.dto.UserResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;


@Slf4j
@Component
public class ResEncoder extends MessageToByteEncoder<UserResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, UserResponse res, ByteBuf out) throws Exception {
        log.info("Starting encode method for UserResponse: {}", res);
        Status currentStatus = res.getStatus();

        // 본문 데이터 생성
        ByteBuf body = ctx.alloc().buffer();

        try {
            switch (currentStatus) {
/*                case WD:
                    String daqId_wd = res.getDaqId();
                    body.writeBytes(daqId_wd.getBytes(StandardCharsets.UTF_8));

                    for (String sensorId : res.getSensorIdsOrder()) {
                        body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));

                        String sensorData = res.getParsedSensorData().get(sensorId);
                        if (sensorData != null) {
                            body.writeBytes(sensorData.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    break;
                case RQ:
                    String daqId_rq = res.getDaqId();
                    body.writeBytes(daqId_rq.getBytes(StandardCharsets.UTF_8));

                    for (String sensorId : res.getSensorIdsOrder()) {
                        body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));
                    }
                    break;*/
                case RS:
                    String daqId_rs = res.getDaqId();
                    body.writeBytes(daqId_rs.getBytes(StandardCharsets.UTF_8));

                    int sensorCnt_rs = res.getSensorCnt();
                    String sensorCnt_rs_str = String.format("%02d", sensorCnt_rs);
                    body.writeBytes(sensorCnt_rs_str.getBytes(StandardCharsets.UTF_8));

                    for (String sensorId : res.getSensorIdsOrder()) {
                        body.writeBytes(sensorId.getBytes(StandardCharsets.UTF_8));
                    }
                    break;

                case RD:
                    // 센서갯수
                    int sensorCnt_rd = res.getSensorCnt();
                    String sensorCnt_rd_str = String.format("%02d", sensorCnt_rd);
                    body.writeBytes(sensorCnt_rd_str.getBytes(StandardCharsets.UTF_8));

                    String timeStamp = res.getTimeStamp();
                    body.writeBytes(timeStamp.getBytes(StandardCharsets.UTF_8));

                    // 센서 데이터를 바이트로 변환하여 body에 쓰기
                    Queue<String> resDataList = res.getResDataList();
                    for (String resData : resDataList) {
                        byte[] dataBytes = resData.getBytes(StandardCharsets.UTF_8);
                        log.info("resData: {}", resData);
                        body.writeBytes(dataBytes);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("지원되지 않는 명령어 유형");
            }

            // 헤더 작성
            int packetLength = body.readableBytes() + 6; // STX(1) + length(2) + status(2) + ETX(1)
            String packetLengthStr = String.format("%03d", packetLength);
            log.info("packetLength: {} / packetLengthStr: {}", packetLength, packetLengthStr);

            // stx
            out.writeByte(ProtocolState.STX.getValue());

            // 전체 패킷 길이
            out.writeBytes(packetLengthStr.getBytes(StandardCharsets.UTF_8));
            // command
            out.writeBytes(currentStatus.toString().getBytes(StandardCharsets.UTF_8));
            // body
            out.writeBytes(body);
            // etx
            out.writeByte(ProtocolState.ETX.getValue());
            log.info("==================================================================================================== \n");
            log.info("Encoded Data: {}  \n ", out.toString(StandardCharsets.UTF_8));
            log.info("====================================================================================================");
            ctx.writeAndFlush(out);

        } finally {
            out.release();
            body.release();
        }
    }


}
