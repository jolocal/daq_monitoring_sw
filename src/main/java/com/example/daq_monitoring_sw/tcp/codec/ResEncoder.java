package com.example.daq_monitoring_sw.tcp.codec;

import com.example.daq_monitoring_sw.tcp.dto.DaqCenter;
import com.example.daq_monitoring_sw.tcp.dto.Status;
import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.example.daq_monitoring_sw.tcp.util.ChannelRepository.DAQ_CENTER_KEY;


@Slf4j
@Component
public class ResEncoder extends MessageToByteEncoder<UserRequest> {
/*
    STX	0	0	R	S	D	A	Q	0	0	0	0	F/P/T/D	L/R/E/U	0	0	ETX
    STX	0	0	R	D	0	0	0 or -	0	0	0	0	ETX
*/

    @Override
    protected void encode(ChannelHandlerContext ctx, UserRequest res, ByteBuf out) throws Exception {

        log.info("encode start res: {}", res);
        DaqCenter daqcenter = ctx.channel().attr(DAQ_CENTER_KEY).get();
        Status currentStatus = res.getStatus();

        // 본문 데이터 생성
        ByteBuf body = Unpooled.buffer();

        switch (currentStatus) {
            case WD:
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
                break;

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
                int sensorCnt_rd = res.getSensorCnt();
                String sensorCnt_rd_str = String.format("%02d", sensorCnt_rd);
                body.writeBytes(sensorCnt_rd_str.getBytes(StandardCharsets.UTF_8));

                for (String sensorId : res.getSensorIdsOrder()) {
                    String sensorData = res.getParsedSensorData().get(sensorId);

                    if (sensorData != null) {
                        body.writeBytes(sensorData.getBytes(StandardCharsets.UTF_8));
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("지원되지 않는 명령어 유형");
        }

        // 헤더 작성
        int packetLength = body.readableBytes() + 6; // STX(1) + length(2) + status(2) + ETX(1)
        String packetLengthStr = String.format("%02d", packetLength);

        // stx
        out.writeByte(ProtocolState.STX.getValue());
        out.writeBytes(packetLengthStr.getBytes(StandardCharsets.UTF_8));

        // command
        String command = currentStatus.toString();
        out.writeBytes(command.getBytes(StandardCharsets.UTF_8));
     /*   if (command.equals("RQ")){
            out.writeBytes("RS".getBytes(StandardCharsets.UTF_8));
        } else{
            out.writeBytes(command.getBytes(StandardCharsets.UTF_8));
        }*/

        // body
        out.writeBytes(body);

        // etx
        out.writeByte(ProtocolState.ETX.getValue());

        log.info("Encoded Data: {}", out.toString(StandardCharsets.UTF_8));

        ctx.writeAndFlush(out);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // 예외출력
        ctx.close(); // 채널 닫기 및 네트워크 리소스 정리
    }

}
