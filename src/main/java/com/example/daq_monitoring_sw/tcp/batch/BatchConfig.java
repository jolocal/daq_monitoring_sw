//package com.example.daq_monitoring_sw.tcp.batch;
//
//import com.example.daq_monitoring_sw.tcp.dto.UserRequest;
//import com.example.daq_monitoring_sw.tcp.entity.DaqEntity;
//import com.example.daq_monitoring_sw.tcp.repository.DaqCenterRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//@Slf4j
//@Configuration
//public class BatchConfig {
//
//    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<UserRequest>> userRequestMap = new ConcurrentHashMap<>();
//    @Bean
//    public Job job(JobRepository jobRepository, Step step1, Step step2) {
//        return new JobBuilder("job", jobRepository)
//                .start(step1)
//                .next(step2)
//                .build();
//    }
//
//    @Bean
//    public Step step1(JobRepository jobRepository, Tasklet collectDataTasklet, PlatformTransactionManager platformTransactionManager){
//        return new StepBuilder("step1", jobRepository)
//                .tasklet(collectDataTasklet, platformTransactionManager)
//                .build();
//    }
//    @Bean
//    public Step step2(JobRepository jobRepository, Tasklet saveDataTasklet, PlatformTransactionManager platformTransactionManager){
//        return new StepBuilder("step2", jobRepository)
//                .tasklet(saveDataTasklet, platformTransactionManager)
//                .build();
//    }
//
//    @Bean
//    public Tasklet collectDataTasklet(){
//        return ((contribution, chunkContext) -> {
//            log.info("==================== Collecting data... ====================");
//            // 1분 동안 데이터를 모으는 로직
//            return RepeatStatus.FINISHED;
//        });
//    }
//
//    @Bean
//    public Tasklet saveDataTasklet(ObjectMapper objectMapper, DaqCenterRepository daqCenterRepository) {
//        return (contribution, chunkContext) -> {
//            log.info("==================== Saving data to DB... ====================");
//            userRequestMap.forEach((daqId, reqQueue)-> {
//                while (!reqQueue.isEmpty()){
//                    UserRequest userRequest = reqQueue.poll();
//                    if (userRequest != null) {
//                        DaqEntity entity = DaqEntity.builder()
//                                .daqName(userRequest.getDaqName())
//                                .status(userRequest.getStatus())
//                                .sensorCnt(Integer.parseInt(userRequest.getSensorCnt()))
//                                .dataList(dataListJson)
//                                .cliSentTime(userRequest.getCliSentTime())
//                                .servRecvTime(userRequest.getServRecvTime())
//                                .transDelay(userRequest.getTransDelay())
//                                .build();
//                    }
//                }
//            });
//        }
//    }
//
//}
