package com.example.daq_monitoring_sw.tcp.batch;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.entity.DaqEntity;
import com.example.daq_monitoring_sw.tcp.repository.DaqCenterRepository;
import com.example.daq_monitoring_sw.tcp.service.UserReqProcessor;
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

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchConfigTest {

    @Lazy
    private final UserReqProcessor userReqProcessor;
    private final DaqCenterRepository daqCenterRepository;

    @Bean
    public Job userReqJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("userReqJob", jobRepository)
                .start(step1(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step1", jobRepository)
                .<UserRequest, DaqEntity>chunk(200, transactionManager)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();

    }

    @Bean
    public ItemReader<? extends UserRequest> itemReader() {
        return new ItemReader<UserRequest>() {
            @Override
            public UserRequest read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                UserRequest request = userReqProcessor.getUserRequestsQueue().poll();
                // log.info("ItemReader: UserRequest 읽음: {}", request);
                return request;
            }
        };

    }


    @Bean
    public ItemProcessor<UserRequest, DaqEntity> itemProcessor() {
        return new ItemProcessor<UserRequest, DaqEntity>() {
            @Override
            public DaqEntity process(UserRequest item) throws Exception {
                // log.info("ItemProcessor: UserRequest 처리 중: {}", item);
                // dto -> entity
                return DaqEntity.builder()
                        .daqName(item.getDaqName())
                        .status(item.getStatus())
                        .sensorCnt(Integer.parseInt(item.getSensorCnt()))
                        .dataList(item.getDataListJson())
                        .cliSentTime(item.getCliSentTime())
                        .servRecvTime(item.getServRecvTime())
                        .transDelay(item.getTransDelay())
                        .dbSaveTime(LocalDateTime.now())
                        .build();
            }
        };
    }

    @Bean
    public ItemWriter<DaqEntity> itemWriter() {
        return new ItemWriter<DaqEntity>() {
            @Override
            public void write(Chunk<? extends DaqEntity> items) throws Exception {
                // 데이터베이스에 쓰기 로직을 여기서 작성
                // log.info("ItemWriter: DaqEntity 쓰기 중: {}", items);
                daqCenterRepository.saveAll(items.getItems());
            }
        };
    }

}
