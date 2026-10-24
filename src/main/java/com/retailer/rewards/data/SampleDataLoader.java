package com.retailer.rewards.data;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

/**
 * Loads a demo data set on startup. Dates are relative to the current month
 * so the API always has data inside the three-month reward period, plus a
 * few older purchases that should be left out of the calculation.
 *
 * <p>Purchases for the current month are dated on the 1st, so they are never
 * in the future regardless of the day the app is started.
 */
@Component
public class SampleDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final YearMonth currentMonth;

    private long nextTransactionId = 1;

    public SampleDataLoader(CustomerRepository customerRepository,
                            TransactionRepository transactionRepository,
                            Clock clock) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.currentMonth = YearMonth.now(clock);
    }

    @Override
    public void run(String... args) {
        customerRepository.save(new Customer(1L, "Emma Wilson"));
        customerRepository.save(new Customer(2L, "Liam Carter"));
        customerRepository.save(new Customer(3L, "Sophia Patel"));
        customerRepository.save(new Customer(4L, "Noah Brooks"));

        // Emma: covers the $100 boundary, a sub-$50 purchase and an old purchase outside the period
        addTransaction(1L, "500.00", 3, 15);
        addTransaction(1L, "120.00", 2, 5);
        addTransaction(1L, "75.50", 2, 18);
        addTransaction(1L, "100.00", 1, 3);
        addTransaction(1L, "49.99", 1, 22);
        addTransaction(1L, "210.00", 0, 1);

        // Liam: exactly $50, just over $100 and a large purchase with cents
        addTransaction(2L, "50.00", 2, 10);
        addTransaction(2L, "101.00", 2, 25);
        addTransaction(2L, "300.25", 1, 14);
        addTransaction(2L, "65.00", 0, 1);

        // Sophia: nothing in the oldest month of the period
        addTransaction(3L, "99.99", 1, 8);
        addTransaction(3L, "150.00", 1, 20);
        addTransaction(3L, "35.00", 0, 1);

        // Noah: only purchases older than the reward period, so zero points
        addTransaction(4L, "250.00", 4, 12);
        addTransaction(4L, "80.00", 3, 2);

        log.info("Loaded {} customers and {} transactions",
                customerRepository.findAll().size(), transactionRepository.findAll().size());
    }

    private void addTransaction(Long customerId, String amount, int monthsAgo, int dayOfMonth) {
        LocalDate transactionDate = currentMonth.minusMonths(monthsAgo).atDay(dayOfMonth);
        transactionRepository.save(
                new Transaction(nextTransactionId++, customerId, new BigDecimal(amount), transactionDate));
    }
}
