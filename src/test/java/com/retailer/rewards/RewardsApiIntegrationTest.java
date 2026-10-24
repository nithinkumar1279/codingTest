package com.retailer.rewards;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end check of the running application against the sample data set
 * loaded by {@link com.retailer.rewards.data.SampleDataLoader}. The sample
 * dates are relative to the current month, so the expected points below
 * hold no matter which day the tests run.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rewardsForAllCustomersMatchSampleData() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].customerName").value("Emma Wilson"))
                .andExpect(jsonPath("$[0].totalPoints").value(435))
                .andExpect(jsonPath("$[1].customerName").value("Liam Carter"))
                .andExpect(jsonPath("$[1].totalPoints").value(517))
                .andExpect(jsonPath("$[2].customerName").value("Sophia Patel"))
                .andExpect(jsonPath("$[2].totalPoints").value(199))
                .andExpect(jsonPath("$[3].customerName").value("Noah Brooks"))
                .andExpect(jsonPath("$[3].totalPoints").value(0));
    }

    @Test
    void rewardsForSingleCustomerAreBrokenDownByMonth() throws Exception {
        YearMonth currentMonth = YearMonth.now();

        mockMvc.perform(get("/api/rewards/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.periodStartDate").value(currentMonth.minusMonths(2).atDay(1).toString()))
                .andExpect(jsonPath("$.monthlyRewards", hasSize(3)))
                .andExpect(jsonPath("$.monthlyRewards[0].month").value(currentMonth.minusMonths(2).toString()))
                .andExpect(jsonPath("$.monthlyRewards[0].points").value(52))
                .andExpect(jsonPath("$.monthlyRewards[1].month").value(currentMonth.minusMonths(1).toString()))
                .andExpect(jsonPath("$.monthlyRewards[1].points").value(450))
                .andExpect(jsonPath("$.monthlyRewards[2].month").value(currentMonth.toString()))
                .andExpect(jsonPath("$.monthlyRewards[2].points").value(15))
                .andExpect(jsonPath("$.totalPoints").value(517));
    }

    @Test
    void customerWithOnlyOldPurchasesHasZeroPointsInEveryMonth() throws Exception {
        mockMvc.perform(get("/api/rewards/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRewards[*].points").value(everyItem(is(0))))
                .andExpect(jsonPath("$.totalPoints").value(0));
    }

    @Test
    void unknownCustomerReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/rewards/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void transactionsIncludeEveryRecordWithItsPoints() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));
    }

    @Test
    void transactionsCanBeFilteredByCustomer() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].customerId").value(everyItem(is(3))))
                .andExpect(jsonPath("$[0].amount").value(99.99))
                .andExpect(jsonPath("$[0].rewardPoints").value(49));
    }
}
