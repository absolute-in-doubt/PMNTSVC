package com.innowise.paymentservice.infrastructure.outbox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SchedulingProperties.class)
@RequiredArgsConstructor
public class SchedulingConfig {

    private final SchedulingProperties properties;

    @Bean
    public TaskScheduler taskScheduler(){
        return new ThreadPoolTaskSchedulerBuilder()
                .poolSize(properties.poolSize())
                .awaitTermination(true)
                .awaitTerminationPeriod(Duration.ofSeconds(properties.shutdown().awaitTerminationPeriodSeconds()))
                .build();
    }
}
