package com.retailer.rewards.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.retailer.rewards.model.Customer;

/**
 * In-memory customer store. Swapping this for a JPA repository later
 * would not require any change in the service layer.
 */
@Repository
public class CustomerRepository {

    private final Map<Long, Customer> customersById = new ConcurrentHashMap<>();

    public Customer save(Customer customer) {
        customersById.put(customer.id(), customer);
        return customer;
    }

    public Optional<Customer> findById(Long customerId) {
        return Optional.ofNullable(customersById.get(customerId));
    }

    public List<Customer> findAll() {
        return customersById.values().stream()
                .sorted(Comparator.comparing(Customer::id))
                .toList();
    }
}
