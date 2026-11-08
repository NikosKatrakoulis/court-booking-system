package com.nikoskatrakoulis.courtbookingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled processing (without this annotation, @Scheduled methods are silently ignored). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
