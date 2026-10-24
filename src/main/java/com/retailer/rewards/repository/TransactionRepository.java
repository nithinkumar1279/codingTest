package com.retailer.rewards.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.stereotype.Repository;

import com.retailer.rewards.model.Transaction;

/**
 * In-memory transaction store. The date range queries mirror what a real
 * database query would look like, so the reward period is always decided
 * by the caller and not by whatever data happens to be stored.
 */
@Repository
public class TransactionRepository {

    private static final Comparator<Transaction> BY_DATE_THEN_ID =
            Comparator.comparing(Transaction::transactionDate).thenComparing(Transaction::id);

    private final Map<Long, Transaction> transactionsById = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        transactionsById.put(transaction.id(), transaction);
        return transaction;
    }

    public List<Transaction> findAll() {
        return findMatching(transaction -> true);
    }

    public List<Transaction> findByCustomerId(Long customerId) {
        return findMatching(transaction -> transaction.customerId().equals(customerId));
    }

    public List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate) {
        return findMatching(transaction -> isWithin(transaction, startDate, endDate));
    }

    public List<Transaction> findByCustomerIdAndTransactionDateBetween(Long customerId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        return findMatching(transaction -> transaction.customerId().equals(customerId)
                && isWithin(transaction, startDate, endDate));
    }

    private List<Transaction> findMatching(Predicate<Transaction> filter) {
        return transactionsById.values().stream()
                .filter(filter)
                .sorted(BY_DATE_THEN_ID)
                .toList();
    }

    private static boolean isWithin(Transaction transaction, LocalDate startDate, LocalDate endDate) {
        LocalDate transactionDate = transaction.transactionDate();
        return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
    }
}
