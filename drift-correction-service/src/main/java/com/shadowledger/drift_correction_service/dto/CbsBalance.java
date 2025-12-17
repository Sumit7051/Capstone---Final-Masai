package com.shadowledger.drift_correction_service.dto;

public class CbsBalance {
    private String accountId;
    private Double reportedBalance;

    public CbsBalance(){};

    public CbsBalance(String accountId, Double reportedBalance) {
        this.accountId = accountId;
        this.reportedBalance = reportedBalance;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Double getReportedBalance() {
        return reportedBalance;
    }

    public void setReportedBalance(Double reportedBalance) {
        this.reportedBalance = reportedBalance;
    }
}
