package com.shadowledger.shadow_ledger_service.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ledger_events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}

