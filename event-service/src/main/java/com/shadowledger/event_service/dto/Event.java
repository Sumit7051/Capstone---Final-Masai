package com.shadowledger.event_service.dto;

import java.math.BigDecimal;

public record Event(String eventId, String accountId, String type, BigDecimal amount, long timestamp)
{
    public Event(String eventId, String accountId, String type, BigDecimal amount, long timestamp) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public String accountId() {
        return accountId;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public BigDecimal amount() {
        return amount;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }
}