package com.afriland.ticket2cash.campaign;

import com.afriland.ticket2cash.product.CashbackType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CampaignRequest {

    private Long merchantId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private CashbackType cashbackType;
    private BigDecimal cashbackValue;
    private BigDecimal dailyLimitPerClient;
    private BigDecimal monthlyLimitPerClient;
    private BigDecimal totalBudget;
    private CampaignStatus status;

    public Long getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public CashbackType getCashbackType() { return cashbackType; }
    public BigDecimal getCashbackValue() { return cashbackValue; }
    public BigDecimal getDailyLimitPerClient() { return dailyLimitPerClient; }
    public BigDecimal getMonthlyLimitPerClient() { return monthlyLimitPerClient; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public CampaignStatus getStatus() { return status; }

    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setCashbackType(CashbackType cashbackType) { this.cashbackType = cashbackType; }
    public void setCashbackValue(BigDecimal cashbackValue) { this.cashbackValue = cashbackValue; }
    public void setDailyLimitPerClient(BigDecimal dailyLimitPerClient) { this.dailyLimitPerClient = dailyLimitPerClient; }
    public void setMonthlyLimitPerClient(BigDecimal monthlyLimitPerClient) { this.monthlyLimitPerClient = monthlyLimitPerClient; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public void setStatus(CampaignStatus status) { this.status = status; }
}