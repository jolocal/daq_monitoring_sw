package com.example.daq_monitoring_sw.tcp.batch;

import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
import com.example.daq_monitoring_sw.tcp.entity.DaqEntity;
import com.example.daq_monitoring_sw.tcp.repository.DaqCenterRepository;
import com.example.daq_monitoring_sw.tcp.service.UserReqProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final UserReqProcessor userReqProcessor;

    @Bean
    public Job job(JobRepository jobRepository, Step step1) {
        return new JobBuilder("job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, Tasklet saveDataTasklet, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("step1", jobRepository)
                .tasklet(saveDataTasklet, platformTransactionManager)
                .build();
    }


    @Bean
    public Tasklet saveDataTasklet(DaqCenterRepository daqCenterRepository) {
        return (contribution, chunkContext) -> {
            log.info("==================== Saving data to DB... ====================");
            ConcurrentLinkedQueue<UserRequest> queue = userReqProcessor.getUserRequestsQueue();

            while (!queue.isEmpty()) {
                UserRequest userRequest = queue.poll();
                if (userRequest != null) {
                    try{
                        DaqEntity entity = DaqEntity.builder()
                                .daqName(userRequest.getDaqName())
                                .status(userRequest.getStatus())
                                .sensorCnt(Integer.parseInt(userRequest.getSensorCnt()))
                                .dataList(userRequest.getDataListJson())
                                .cliSentTime(userRequest.getCliSentTime())
                                .servRecvTime(userRequest.getServRecvTime())
                                .transDelay(userRequest.getTransDelay())
                                .dbSaveTime(LocalDateTime.now())
                                .build();

                        daqCenterRepository.save(entity);
                    } catch (Exception e) {
                        log.error("Error saving to DB: {}", e.getMessage(), e);
                    }
                }
            }
            return RepeatStatus.FINISHED;
        };
    }


}
