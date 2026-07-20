package com.tnf.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tnf.account.model.BankAccount;

/**
 * Persistence for all account subtypes. Typed to the {@link BankAccount} base so it reads and
 * writes the polymorphic "accounts" collection; concrete subtypes are resolved via the stored
 * discriminator.
 */
@Repository
public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findByCustomerId(String customerId);

    boolean existsByAccountNumber(String accountNumber);
}
