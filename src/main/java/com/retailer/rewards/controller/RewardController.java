package com.retailer.rewards.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.service.RewardService;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public List<CustomerRewardsResponse> getRewardsForAllCustomers() {
        return rewardService.getRewardsForAllCustomers();
    }

    @GetMapping("/{customerId}")
    public CustomerRewardsResponse getRewardsForCustomer(@PathVariable Long customerId) {
        return rewardService.getRewardsForCustomer(customerId);
    }
}
