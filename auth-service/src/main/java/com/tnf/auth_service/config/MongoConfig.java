package com.tnf.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing so {@code @CreatedDate} / {@code @LastModifiedDate} fields on entities are
 * populated automatically on insert and update.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
