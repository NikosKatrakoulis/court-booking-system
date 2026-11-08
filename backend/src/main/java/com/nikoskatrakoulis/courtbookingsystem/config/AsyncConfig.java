package com.nikoskatrakoulis.courtbookingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async (without it the annotation is silently ignored and code runs synchronously). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
