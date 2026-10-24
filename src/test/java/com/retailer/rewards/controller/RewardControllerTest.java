package com.retailer.rewards.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyRewardResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.service.RewardService;

@WebMvcTest(RewardController.class)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardService rewardService;

    @Test
    void getRewardsForCustomerReturnsMonthlyBreakdownAndTotal() throws Exception {
        when(rewardService.getRewardsForCustomer(1L)).thenReturn(sampleRewards(1L, "Emma Wilson"));

        mockMvc.perform(get("/api/rewards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Emma Wilson"))
                .andExpect(jsonPath("$.periodStartDate").value("2026-08-01"))
                .andExpect(jsonPath("$.periodEndDate").value("2026-10-24"))
                .andExpect(jsonPath("$.monthlyRewards", hasSize(3)))
                .andExpect(jsonPath("$.monthlyRewards[0].month").value("2026-08"))
                .andExpect(jsonPath("$.monthlyRewards[0].points").value(115))
                .andExpect(jsonPath("$.monthlyRewards[2].month").value("2026-10"))
                .andExpect(jsonPath("$.monthlyRewards[2].points").value(270))
                .andExpect(jsonPath("$.totalPoints").value(435));
    }

    @Test
    void getRewardsForAllCustomersReturnsOneEntryPerCustomer() throws Exception {
        when(rewardService.getRewardsForAllCustomers()).thenReturn(List.of(
                sampleRewards(1L, "Emma Wilson"),
                sampleRewards(2L, "Liam Carter")));

        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerId").value(1))
                .andExpect(jsonPath("$[1].customerId").value(2));
    }

    @Test
    void unknownCustomerReturnsNotFound() throws Exception {
        when(rewardService.getRewardsForCustomer(99L)).thenThrow(new CustomerNotFoundException(99L));

        mockMvc.perform(get("/api/rewards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.detail").value("Customer not found with id 99"));
    }

    @Test
    void nonNumericCustomerIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rewards/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'abc' for parameter 'customerId'"));
    }

    private static CustomerRewardsResponse sampleRewards(Long customerId, String customerName) {
        return new CustomerRewardsResponse(
                customerId,
                customerName,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 10, 24),
                List.of(
                        new MonthlyRewardResponse(YearMonth.of(2026, 8), 115),
                        new MonthlyRewardResponse(YearMonth.of(2026, 9), 50),
                        new MonthlyRewardResponse(YearMonth.of(2026, 10), 270)),
                435);
    }
}
