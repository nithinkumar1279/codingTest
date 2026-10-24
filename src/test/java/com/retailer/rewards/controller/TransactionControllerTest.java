package com.retailer.rewards.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.service.TransactionService;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void getTransactionsReturnsEveryTransaction() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(
                new TransactionResponse(1L, 1L, new BigDecimal("120.00"), LocalDate.of(2026, 8, 5), 90),
                new TransactionResponse(2L, 2L, new BigDecimal("50.00"), LocalDate.of(2026, 8, 10), 0)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId").value(1))
                .andExpect(jsonPath("$[0].amount").value(120.00))
                .andExpect(jsonPath("$[0].transactionDate").value("2026-08-05"))
                .andExpect(jsonPath("$[0].rewardPoints").value(90));
    }

    @Test
    void getTransactionsFiltersByCustomerId() throws Exception {
        when(transactionService.getTransactionsForCustomer(2L)).thenReturn(List.of(
                new TransactionResponse(2L, 2L, new BigDecimal("50.00"), LocalDate.of(2026, 8, 10), 0)));

        mockMvc.perform(get("/api/transactions").param("customerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId").value(2));

        verify(transactionService).getTransactionsForCustomer(2L);
    }

    @Test
    void unknownCustomerReturnsNotFound() throws Exception {
        when(transactionService.getTransactionsForCustomer(7L)).thenThrow(new CustomerNotFoundException(7L));

        mockMvc.perform(get("/api/transactions").param("customerId", "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCustomerIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "seven"))
                .andExpect(status().isBadRequest());
    }
}
