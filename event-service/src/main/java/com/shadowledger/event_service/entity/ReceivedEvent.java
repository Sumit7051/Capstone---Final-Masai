package com.shadowledger.event_service.entity;


import com.shadowledger.event_service.dto.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "received_events")
public class ReceivedEvent {

    @Id
    private String eventId;

    private String accountId;
    private String type;
    private BigDecimal amount;
    private long timestamp;

    protected ReceivedEvent() {}

    public ReceivedEvent(Event dto) {
        this.eventId = dto.eventId();
        this.accountId = dto.accountId();
        this.type = dto.type();
        this.amount = dto.amount();
        this.timestamp = dto.timestamp();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}