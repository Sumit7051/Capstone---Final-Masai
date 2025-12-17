package com.shadowledger.shadow_ledger_service.repository;

import com.shadowledger.shadow_ledger_service.dto.LedgerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LedgerEventRepository extends JpaRepository<LedgerEvent, Long> {
    Optional<LedgerEvent> findByEventId(String eventId);

    @Query(value = """
            SELECT account_id AS accountId,
                   SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END) OVER (PARTITION BY account_id ORDER BY timestamp, event_id) AS Balance,
                   event_id AS LastEvent
            FROM ledger_events
            WHERE account_id = :accountId
            ORDER BY timestamp, event_id
            """, nativeQuery = true)
    List<Map<String, Object>> computeShadowBalance(@Param("accountId") String accountId);
}
