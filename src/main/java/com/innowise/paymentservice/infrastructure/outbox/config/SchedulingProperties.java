package com.innowise.paymentservice.infrastructure.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.task.scheduling")
public record SchedulingProperties(
        int poolSize,
        String threadNamePrefix,
        Shutdown shutdown
) {

    public record Shutdown(
            long awaitTerminationPeriodSeconds
    ){}
}
