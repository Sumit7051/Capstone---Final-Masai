package com.shadowledger.drift_correction_service.service;


import com.shadowledger.drift_correction_service.dto.CbsBalance;
import com.shadowledger.drift_correction_service.dto.CorrectionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class DriftCorrectionService {

    private final KafkaTemplate<String, CorrectionEvent> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${shadow-ledger.service.url:http://localhost:8082}")
    private String shadowLedgerServiceUrl;

    public DriftCorrectionService(KafkaTemplate<String, CorrectionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = new RestTemplate();
    }

    public Double getShadowBalance(String accountId) {
        try {
            String url = shadowLedgerServiceUrl + "/accounts/" + accountId + "/shadow-balance";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("balance")) {
                return ((Number) response.get("balance")).doubleValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch shadow balance for " + accountId + ": " + e.getMessage());
        }
        return 0.0;
    }

    public CorrectionEvent generateCorrectionEvent(CbsBalance cbsBalance, Double shadowBalance) {
        double diff = cbsBalance.getReportedBalance() - shadowBalance;
        if (diff == 0) return null;

        String type = diff > 0 ? "credit" : "debit";
        CorrectionEvent correction = new CorrectionEvent(
                "CORR-" + cbsBalance.getAccountId() + "-" + UUID.randomUUID().toString().substring(0, 8),
                cbsBalance.getAccountId(),
                type,
                Math.abs(diff)
        );

        // publish to Kafka
        kafkaTemplate.send("transactions.corrections", correction.getAccountId(), correction);
        return correction;
    }

    public CorrectionEvent ManualCorrection(String accountId, String type, Double amount) {
        CorrectionEvent correction = new CorrectionEvent(
                "MANUAL-" + accountId + "-" + UUID.randomUUID().toString().substring(0, 8),
                accountId,
                type,
                amount
        );

        // publish to Kafka
        kafkaTemplate.send("transactions.corrections", correction.getAccountId(), correction);
        return correction;
    }
}