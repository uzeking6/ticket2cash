package com.afriland.ticket2cash.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyClientRepository extends JpaRepository<LoyaltyClient, Long> {
    Optional<LoyaltyClient> findByAccountNumber(String accountNumber);
    List<LoyaltyClient> findByTierIgnoreCase(String tier);
    long countByTierIgnoreCase(String tier);
}
