package com.retailer.rewards.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final RewardCalculator rewardCalculator;

    public TransactionService(TransactionRepository transactionRepository,
                              CustomerRepository customerRepository,
                              RewardCalculator rewardCalculator) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.rewardCalculator = rewardCalculator;
    }

    public List<TransactionResponse> getAllTransactions() {
        return toResponses(transactionRepository.findAll());
    }

    public List<TransactionResponse> getTransactionsForCustomer(Long customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }
        return toResponses(transactionRepository.findByCustomerId(customerId));
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> new TransactionResponse(
                        transaction.id(),
                        transaction.customerId(),
                        transaction.amount(),
                        transaction.transactionDate(),
                        rewardCalculator.calculatePoints(transaction.amount())))
                .toList();
    }
}
