package com.example.daq_monitoring_sw.tcp.batch;

import com.example.daq_monitoring_sw.tcp.dto.ProtocolMessage;
import com.example.daq_monitoring_sw.tcp.entity.DaqEntity;
import com.example.daq_monitoring_sw.tcp.repository.DaqCenterRepository;
import com.example.daq_monitoring_sw.tcp.service.MessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchJobConfig {
    /*
     *  배치 Job 정의 (Job, Step, Reader, Processor, Writer)
     *  실시간 MessageQueue -> DB 저장 흐름 처리
     *  Spring Batch 메인 Job 구성 클래스
     */
    @Lazy
    private final MessageProcessor messageProcessor;
    private final DaqCenterRepository daqCenterRepository;

    // DB 저장용 타임스탬프 포맷: yyyy-MM-dd HH:mm:ss.SSS
    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Seoul"));

    
    // -------------------------
    // 1) Batch Job 설정
    // -------------------------
    @Bean
    public Job messageJob(JobRepository repo, PlatformTransactionManager txManager) {
        
        // log.info("  ▷ [BatchConfig] messageJob 초기화 완료 ");
        
        return new JobBuilder("messageJob", repo)
                .start(step1(repo, txManager))
                .build();
    }

    // -------------------------
    // 2) Step 설정
    // -------------------------
    @Bean
    public Step step1(JobRepository repo, PlatformTransactionManager txManager) {
        
        // log.info("  ▷ [BatchConfig] step1 초기화 완료 ");

        return new StepBuilder("step1", repo)
                .<ProtocolMessage, DaqEntity>chunk(400, txManager)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();

    }

    @Bean
    public ItemReader<? extends ProtocolMessage> itemReader() {
        return new ItemReader<ProtocolMessage>() {
            @Override
            public ProtocolMessage read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                ProtocolMessage msg = messageProcessor.getMessageQueue().poll();

                // NPE 방어
                if (msg == null) {
                    log.info("  ▷ [Reader] 큐가 비어있음");
                    return null;
                }

                // log.info("  ▷ [Reader] 메시지 읽음 (deviceId: {}, latency: {}, sensorCnt: {}) | 남은 큐: {}",
                // msg.getDeviceId(), msg.getLatency(), msg.getSensorCnt(),
                // messageProcessor.getMessageQueue().size());
                return msg;
            }
        };

    }


    @Bean
    public ItemProcessor<ProtocolMessage, DaqEntity> itemProcessor() {
        return new ItemProcessor<ProtocolMessage, DaqEntity>() {
            @Override
            public DaqEntity process(ProtocolMessage msg) throws Exception {      
                // dto -> entity 변환
                DaqEntity entity =  DaqEntity.builder()
                        .deviceID(msg.getDeviceId())
                        .status(msg.getStatus().name())
                        .sensorCnt(msg.getSensorCnt())
                        .dataList(msg.getDataListJson())

                        .cliSentTime(formatEpochMs(msg.getCli_ts_ms()))
                        .srvRecvTime(formatEpochMs(msg.getSrv_ts_ms()))
                        .latency(msg.getLatency())
                        .latencyStr(formatLatency(msg.getLatency()))

                        .createdAt(LocalDateTime.now())
                        .build();
                
                // log.info("  ▷ [Processor] DaqEntity 변환 완료: {}", entity);
                
                return entity;
            }
        };
    }

    /**
     * epoch ms(long)을 yyyyMMddHHmmssSSS 형태의 문자열로 변환.
     */
    private String formatEpochMs(long epochMs) {
        return TS_FORMATTER.format(Instant.ofEpochMilli(epochMs));
    }

    /**
     * 지연시간(ms)을 HH:mm:ss.SSS 문자열로 변환.
     */
    private String formatLatency(long latencyMs) {
        long hours = latencyMs / 3_600_000;
        long minutes = (latencyMs % 3_600_000) / 60_000;
        long seconds = (latencyMs % 60_000) / 1_000;
        long millis = latencyMs % 1_000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    // -------------------------
    // 5) ItemWriter — DB 저장
    // -------------------------
    @Bean
    public ItemWriter<DaqEntity> itemWriter() {
        return new ItemWriter<DaqEntity>() {
            @Override
            public void write(Chunk<? extends DaqEntity> items) throws Exception {
                daqCenterRepository.saveAll(items.getItems());
                log.info("  ▷ [Writer] DB 저장 완료 - 저장 건수 {}건", items.getItems().size());
            }
        };
    }

}
