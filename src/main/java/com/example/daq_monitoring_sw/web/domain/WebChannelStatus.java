package com.example.daq_monitoring_sw.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebChannelStatus {

    private String channelId;
    private String status;
}
