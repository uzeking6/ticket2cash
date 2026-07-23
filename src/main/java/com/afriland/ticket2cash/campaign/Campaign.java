package com.afriland.ticket2cash.campaign;

import com.afriland.ticket2cash.merchant.Merchant;
import com.afriland.ticket2cash.product.CashbackType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private CashbackType cashbackType;

    private BigDecimal cashbackValue;
    private BigDecimal dailyLimitPerClient;
    private BigDecimal monthlyLimitPerClient;
    private BigDecimal totalBudget;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    public Campaign() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = CampaignStatus.DRAFT;
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public CashbackType getCashbackType() {
        return cashbackType;
    }

    public BigDecimal getCashbackValue() {
        return cashbackValue;
    }

    public BigDecimal getDailyLimitPerClient() {
        return dailyLimitPerClient;
    }

    public BigDecimal getMonthlyLimitPerClient() {
        return monthlyLimitPerClient;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setCashbackType(CashbackType cashbackType) {
        this.cashbackType = cashbackType;
    }

    public void setCashbackValue(BigDecimal cashbackValue) {
        this.cashbackValue = cashbackValue;
    }

    public void setDailyLimitPerClient(BigDecimal dailyLimitPerClient) {
        this.dailyLimitPerClient = dailyLimitPerClient;
    }

    public void setMonthlyLimitPerClient(BigDecimal monthlyLimitPerClient) {
        this.monthlyLimitPerClient = monthlyLimitPerClient;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }
}