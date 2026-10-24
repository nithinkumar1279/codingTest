package com.retailer.rewards.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(@RequestParam(required = false) Long customerId) {
        if (customerId == null) {
            return transactionService.getAllTransactions();
        }
        return transactionService.getTransactionsForCustomer(customerId);
    }
}
