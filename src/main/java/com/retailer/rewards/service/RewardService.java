package com.retailer.rewards.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyRewardResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

/**
 * Calculates reward points per customer for the reward period, which is the
 * current month plus the previous months up to {@code rewards.period-months}
 * (three by default). Transactions outside the period are ignored, no matter
 * what the repository holds.
 */
@Service
public class RewardService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RewardCalculator rewardCalculator;
    private final Clock clock;
    private final int periodMonths;

    public RewardService(CustomerRepository customerRepository,
                         TransactionRepository transactionRepository,
                         RewardCalculator rewardCalculator,
                         Clock clock,
                         @Value("${rewards.period-months:3}") int periodMonths) {
        if (periodMonths < 1) {
            throw new IllegalArgumentException("rewards.period-months must be at least 1");
        }
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.rewardCalculator = rewardCalculator;
        this.clock = clock;
        this.periodMonths = periodMonths;
    }

    public List<CustomerRewardsResponse> getRewardsForAllCustomers() {
        RewardPeriod rewardPeriod = currentRewardPeriod();

        Map<Long, List<Transaction>> transactionsByCustomerId = transactionRepository
                .findByTransactionDateBetween(rewardPeriod.startDate(), rewardPeriod.endDate())
                .stream()
                .collect(groupingBy(Transaction::customerId));

        return customerRepository.findAll().stream()
                .map(customer -> buildRewardsResponse(customer,
                        transactionsByCustomerId.getOrDefault(customer.id(), List.of()),
                        rewardPeriod))
                .toList();
    }

    public CustomerRewardsResponse getRewardsForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        RewardPeriod rewardPeriod = currentRewardPeriod();

        List<Transaction> customerTransactions = transactionRepository.findByCustomerIdAndTransactionDateBetween(
                customerId, rewardPeriod.startDate(), rewardPeriod.endDate());

        return buildRewardsResponse(customer, customerTransactions, rewardPeriod);
    }

    /**
     * When today is 2026-10-24 and the period is three months, this returns
     * months [2026-08, 2026-09, 2026-10] covering 2026-08-01 to 2026-10-24.
     */
    private RewardPeriod currentRewardPeriod() {
        LocalDate today = LocalDate.now(clock);
        YearMonth oldestMonth = YearMonth.from(today).minusMonths(periodMonths - 1L);

        List<YearMonth> months = Stream.iterate(oldestMonth, month -> month.plusMonths(1))
                .limit(periodMonths)
                .toList();

        return new RewardPeriod(months, oldestMonth.atDay(1), today);
    }

    private CustomerRewardsResponse buildRewardsResponse(Customer customer,
                                                         List<Transaction> transactions,
                                                         RewardPeriod rewardPeriod) {
        Map<YearMonth, Long> pointsByMonth = transactions.stream()
                .collect(groupingBy(transaction -> YearMonth.from(transaction.transactionDate()),
                        summingLong(transaction -> rewardCalculator.calculatePoints(transaction.amount()))));

        // Every month of the period is listed, even the ones without purchases,
        // so consumers always get a predictable shape back.
        List<MonthlyRewardResponse> monthlyRewards = rewardPeriod.months().stream()
                .map(month -> new MonthlyRewardResponse(month, pointsByMonth.getOrDefault(month, 0L)))
                .toList();

        long totalPoints = monthlyRewards.stream()
                .mapToLong(MonthlyRewardResponse::points)
                .sum();

        return new CustomerRewardsResponse(
                customer.id(),
                customer.name(),
                rewardPeriod.startDate(),
                rewardPeriod.endDate(),
                monthlyRewards,
                totalPoints);
    }

    private record RewardPeriod(List<YearMonth> months, LocalDate startDate, LocalDate endDate) {
    }
}
