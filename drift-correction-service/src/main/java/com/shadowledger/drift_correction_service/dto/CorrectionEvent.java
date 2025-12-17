package com.shadowledger.drift_correction_service.dto;



public class CorrectionEvent {
    private String eventId;
    private String accountId;
    private String type; // credit or debit
    private Double amount;

    public CorrectionEvent(String eventId, String accountId, String type, Double amount) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
    }

    public String getEventId() {
        return eventId;
    }



    public String getAccountId() {
        return accountId;
    }



    public String getType() {
        return type;
    }



    public Double getAmount() {
        return amount;
    }


}