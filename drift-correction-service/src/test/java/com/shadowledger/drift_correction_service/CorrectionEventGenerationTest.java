package com.shadowledger.drift_correction_service;

import com.shadowledger.drift_correction_service.dto.CorrectionEvent;
import com.shadowledger.drift_correction_service.service.DriftCorrectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(topics = {"transactions.corrections"}, partitions = 1)
class CorrectionEventGenerationTest {

    @Autowired
    private DriftCorrectionService driftCorrectionService;

    @Test
    void testCorrectionEventGeneration() {
        CorrectionEvent correction = driftCorrectionService.ManualCorrection(
                "Sumit_010551000", "credit", 1000.0
        );

        assertNotNull(correction);
        assertEquals("Sumit_010551000", correction.getAccountId());
        assertEquals("credit", correction.getType());
        assertEquals(1000.0, correction.getAmount());
        assertNotNull(correction.getEventId());
        assertTrue(correction.getEventId().contains("A_TEST"), "Event ID should contain account ID");
    }

    @Test
    void testCorrectionEventIdUniqueness() {
        CorrectionEvent correction1 = driftCorrectionService.ManualCorrection(
                "Sumit_uni", "credit", 89.0
        );

        // Sleep briefly to ensure different timestamps in event ID
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        CorrectionEvent correction2 = driftCorrectionService.ManualCorrection(
                "Sumit_uni", "credit", 89.0
        );

        assertNotEquals(correction1.getEventId(), correction2.getEventId(),
                "Each correction should have unique event ID");
    }

    @Test
    void testCorrectionEventTypesValid() {
        CorrectionEvent debitCorrection = driftCorrectionService.ManualCorrection(
                "51000", "debit", 50.0
        );
        CorrectionEvent creditCorrection = driftCorrectionService.ManualCorrection(
                "51000", "credit", 100.0
        );

        assertEquals("debit", debitCorrection.getType());
        assertEquals("credit", creditCorrection.getType());
    }
}


