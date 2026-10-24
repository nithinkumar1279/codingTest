package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RewardCalculatorTest {

    private final RewardCalculator rewardCalculator = new RewardCalculator();

    @ParameterizedTest(name = "${0} earns {1} points")
    @CsvSource({
            "0.00,    0",
            "10.00,   0",
            "49.99,   0",
            "50.00,   0",
            "50.99,   0",
            "51.00,   1",
            "75.50,   25",
            "99.99,   49",
            "100.00,  50",
            "100.99,  50",
            "101.00,  52",
            "120.00,  90",
            "120.75,  90",
            "200.00,  250",
            "300.25,  450"
    })
    void calculatesPointsForAmount(BigDecimal amount, long expectedPoints) {
        assertThat(rewardCalculator.calculatePoints(amount)).isEqualTo(expectedPoints);
    }

    @Test
    void exampleFromRequirementsIsNinetyPoints() {
        // $120 purchase = 2 x $20 + 1 x $50
        assertThat(rewardCalculator.calculatePoints(new BigDecimal("120"))).isEqualTo(90);
    }

    @Test
    void oneHundredDollarsEarnsFiftyPointsNotZero() {
        assertThat(rewardCalculator.calculatePoints(new BigDecimal("100"))).isEqualTo(50);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> rewardCalculator.calculatePoints(new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> rewardCalculator.calculatePoints(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
