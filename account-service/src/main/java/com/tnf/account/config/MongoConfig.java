package com.tnf.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Enables Spring-managed multi-document transactions so a {@code @Transactional} fund transfer
 * commits or rolls back atomically across the two account documents.
 *
 * <p><strong>Note:</strong> MongoDB multi-document transactions require the server to run as a
 * replica set (even a single-node one via {@code rs.initiate()}). Against a standalone
 * {@code mongod} the transfer still executes but without atomic rollback guarantees.
 *
 * <p>This bean lives in account-service (not the shared library) on purpose, so services that
 * don't need transactions never pull in a transaction manager.
 */
@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
