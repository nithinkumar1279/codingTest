package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyRewardResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

/**
 * Uses the real in-memory repositories with a clock fixed at 2026-10-24,
 * which makes the reward period August 1st to October 24th 2026.
 */
class RewardServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 10, 24).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private CustomerRepository customerRepository;
    private TransactionRepository transactionRepository;
    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        customerRepository = new CustomerRepository();
        transactionRepository = new TransactionRepository();
        rewardService = new RewardService(customerRepository, transactionRepository,
                new RewardCalculator(), FIXED_CLOCK, 3);

        customerRepository.save(new Customer(1L, "Emma Wilson"));
        customerRepository.save(new Customer(2L, "Liam Carter"));
    }

    @Test
    void calculatesMonthlyAndTotalPointsForCustomer() {
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 8, 5));    // 90
        saveTransaction(2L, 1L, "75.00", LocalDate.of(2026, 8, 20));    // 25
        saveTransaction(3L, 1L, "100.00", LocalDate.of(2026, 9, 3));    // 50
        saveTransaction(4L, 1L, "210.00", LocalDate.of(2026, 10, 1));   // 270

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L);

        assertThat(rewards.customerId()).isEqualTo(1L);
        assertThat(rewards.customerName()).isEqualTo("Emma Wilson");
        assertThat(rewards.monthlyRewards()).containsExactly(
                new MonthlyRewardResponse(YearMonth.of(2026, 8), 115),
                new MonthlyRewardResponse(YearMonth.of(2026, 9), 50),
                new MonthlyRewardResponse(YearMonth.of(2026, 10), 270));
        assertThat(rewards.totalPoints()).isEqualTo(435);
    }

    @Test
    void reportsThePeriodThatWasUsed() {
        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L);

        assertThat(rewards.periodStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(rewards.periodEndDate()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void ignoresTransactionsOutsideTheThreeMonthPeriod() {
        saveTransaction(1L, 1L, "500.00", LocalDate.of(2026, 7, 31));   // day before the period starts
        saveTransaction(2L, 1L, "120.00", LocalDate.of(2026, 8, 1));    // first day of the period
        saveTransaction(3L, 1L, "300.00", LocalDate.of(2026, 10, 25));  // after today

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L);

        assertThat(rewards.totalPoints()).isEqualTo(90);
    }

    @Test
    void listsMonthsWithoutPurchasesAsZero() {
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 9, 10));

        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(1L);

        assertThat(rewards.monthlyRewards())
                .extracting(MonthlyRewardResponse::points)
                .containsExactly(0L, 90L, 0L);
    }

    @Test
    void customerWithoutTransactionsHasZeroPoints() {
        CustomerRewardsResponse rewards = rewardService.getRewardsForCustomer(2L);

        assertThat(rewards.monthlyRewards()).hasSize(3);
        assertThat(rewards.totalPoints()).isZero();
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> rewardService.getRewardsForCustomer(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id 99");
    }

    @Test
    void returnsRewardsForEveryCustomerWithoutMixingTheirTransactions() {
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 8, 5));    // 90 for Emma
        saveTransaction(2L, 2L, "101.00", LocalDate.of(2026, 9, 15));   // 52 for Liam
        saveTransaction(3L, 2L, "60.00", LocalDate.of(2026, 10, 2));    // 10 for Liam

        List<CustomerRewardsResponse> allRewards = rewardService.getRewardsForAllCustomers();

        assertThat(allRewards)
                .extracting(CustomerRewardsResponse::customerId, CustomerRewardsResponse::totalPoints)
                .containsExactly(
                        tuple(1L, 90L),
                        tuple(2L, 62L));
    }

    @Test
    void periodLengthIsConfigurable() {
        RewardService oneMonthRewardService = new RewardService(customerRepository, transactionRepository,
                new RewardCalculator(), FIXED_CLOCK, 1);
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 9, 30));
        saveTransaction(2L, 1L, "60.00", LocalDate.of(2026, 10, 1));

        CustomerRewardsResponse rewards = oneMonthRewardService.getRewardsForCustomer(1L);

        assertThat(rewards.monthlyRewards())
                .containsExactly(new MonthlyRewardResponse(YearMonth.of(2026, 10), 10));
        assertThat(rewards.totalPoints()).isEqualTo(10);
    }

    @Test
    void periodSpanningYearEndIsHandled() {
        Clock januaryClock = Clock.fixed(
                LocalDate.of(2027, 1, 10).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        RewardService januaryRewardService = new RewardService(customerRepository, transactionRepository,
                new RewardCalculator(), januaryClock, 3);
        saveTransaction(1L, 1L, "120.00", LocalDate.of(2026, 11, 15));
        saveTransaction(2L, 1L, "120.00", LocalDate.of(2026, 12, 15));

        CustomerRewardsResponse rewards = januaryRewardService.getRewardsForCustomer(1L);

        assertThat(rewards.monthlyRewards())
                .extracting(MonthlyRewardResponse::month)
                .containsExactly(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1));
        assertThat(rewards.totalPoints()).isEqualTo(180);
    }

    @Test
    void rejectsPeriodShorterThanOneMonth() {
        assertThatThrownBy(() -> new RewardService(customerRepository, transactionRepository,
                new RewardCalculator(), FIXED_CLOCK, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void saveTransaction(Long transactionId, Long customerId, String amount, LocalDate transactionDate) {
        transactionRepository.save(new Transaction(transactionId, customerId, new BigDecimal(amount), transactionDate));
    }
}
