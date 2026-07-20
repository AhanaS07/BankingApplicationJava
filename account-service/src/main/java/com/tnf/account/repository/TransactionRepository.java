package com.tnf.account.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tnf.account.model.Transaction;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    /** Transaction history for an account, most recent first. */
    List<Transaction> findByAccountIdOrderByTimestampDesc(String accountId);
}
