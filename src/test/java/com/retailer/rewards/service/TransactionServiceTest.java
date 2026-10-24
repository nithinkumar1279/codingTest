package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        CustomerRepository customerRepository = new CustomerRepository();
        transactionRepository = new TransactionRepository();
        transactionService = new TransactionService(transactionRepository, customerRepository, new RewardCalculator());

        customerRepository.save(new Customer(1L, "Emma Wilson"));
        customerRepository.save(new Customer(2L, "Liam Carter"));
    }

    @Test
    void returnsAllTransactionsOrderedByDateWithPoints() {
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 9, 10));
        saveTransaction(2L, 2L, "75.00", LocalDate.of(2026, 8, 2));

        List<TransactionResponse> transactions = transactionService.getAllTransactions();

        assertThat(transactions).containsExactly(
                new TransactionResponse(2L, 2L, new BigDecimal("75.00"), LocalDate.of(2026, 8, 2), 25),
                new TransactionResponse(1L, 1L, new BigDecimal("120.00"), LocalDate.of(2026, 9, 10), 90));
    }

    @Test
    void returnsOnlyTheRequestedCustomersTransactions() {
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 9, 10));
        saveTransaction(2L, 2L, "75.00", LocalDate.of(2026, 8, 2));

        List<TransactionResponse> transactions = transactionService.getTransactionsForCustomer(1L);

        assertThat(transactions)
                .extracting(TransactionResponse::transactionId)
                .containsExactly(1L);
    }

    @Test
    void returnsEmptyListForCustomerWithoutTransactions() {
        assertThat(transactionService.getTransactionsForCustomer(2L)).isEmpty();
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> transactionService.getTransactionsForCustomer(42L))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    private void saveTransaction(Long transactionId, Long customerId, String amount, LocalDate transactionDate) {
        transactionRepository.save(new Transaction(transactionId, customerId, new BigDecimal(amount), transactionDate));
    }
}
