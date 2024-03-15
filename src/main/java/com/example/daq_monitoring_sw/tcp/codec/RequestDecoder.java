package com.example.daq_monitoring_sw.tcp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class RequestDecoder extends ReplayingDecoder<ProtocolState> {

    private String stx;
    private String totalLength;
    private String command;
    private String daqId;
    private int sensorCnt;
    private String sensorId;
    private List<String> dataList;
    private String etx;


    public RequestDecoder(){
        super(ProtocolState.STX);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        switch (state()){
            case STX:
                stx = readLength(in,1);
                checkpoint(ProtocolState.TOTAL_LENGHT);
                break;
            case TOTAL_LENGHT:
                totalLength = readLength(in,2);
                checkpoint(ProtocolState.COMMAND);
                break;
            case COMMAND:
                command = readLength(in, 2);
                switchCommandState(in, command);
                break;
            case ETX:
                etx = readLength(in,1);


        }
    }

    private void switchCommandState(ByteBuf in, String command) {
        switch (command){
            case "IN":
                daqId = readLength(in, 5);
                sensorCnt = Integer.parseInt(readLength(in, 2));
                sensorId = readLength(in, 4);
                checkpoint(ProtocolState.ETX);
                break;
            case "WD":
            case "RD":
            case "ST":
        }
    }

    private String readLength(ByteBuf in, int length){
        ByteBuf buf = in.readBytes(length);
        String res = buf.toString(StandardCharsets.UTF_8);
        buf.release();
        return res;
    }
}
