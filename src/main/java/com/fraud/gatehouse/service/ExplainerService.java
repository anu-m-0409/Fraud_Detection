package com.fraud.gatehouse.service;

import com.fraud.gatehouse.model.*;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;

@Service
public class ExplainerService {

    public List<String> buildHumanReadableReasons(Map<String, Double> features, List<RuleHit> rules, double riskScore) {
        List<String> reasons = new ArrayList<>();

        for (RuleHit r : rules) {
            reasons.add("[Rule Fired] " + r.getDescription());
        }

        double amtVs = features.getOrDefault("amount_vs_usual", 1.0);
        if (amtVs >= 4.0) {
            reasons.add(String.format("Spike in transaction size: %.1fx above customer's normal baseline.", amtVs));
        }

        if (features.getOrDefault("unseen_device", 0.0) > 0.5) {
            reasons.add("Access from an unrecognized device hardware identifier.");
        }

        if (features.getOrDefault("foreign_country", 0.0) > 0.5) {
            reasons.add("Payment routing initiated outside user's primary operating domestic region.");
        }

        if (features.getOrDefault("txns_last_hour", 0.0) >= 3.0) {
            reasons.add("Card velocity spike: Multiple consecutive authorisations within 60 minutes.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Transaction aligns with historical behavioral patterns and device fingerprint.");
        }
        return reasons;
    }

    public AuditReportResponse generateGenAiAuditReport(EvaluationResponse res) {
        AuditReportResponse r = new AuditReportResponse();
        r.setTransactionId(res.getTransactionId());
        r.setRiskLevel(res.getDecision());

        List<String> drivers = new ArrayList<>();
        if (res.getShapContributions() != null) {
            res.getShapContributions().entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .forEach(e -> drivers.add(String.format("%s (impact weight: +%.1f%%)", formatFeature(e.getKey()), e.getValue() * 100)));
        }
        r.setPrimaryDrivers(drivers);

        List<String> violations = new ArrayList<>();
        for (RuleHit h : res.getRuleHits()) {
            violations.add(h.getCode() + " — " + h.getDescription());
        }
        r.setComplianceViolations(violations);

        // Synthesize Executive Auditor Narrative
        StringBuilder narrative = new StringBuilder();
        narrative.append("Transaction ").append(res.getTransactionId())
                .append(" was evaluated by Gatehouse Hybrid Ensemble at ")
                .append(String.format("%.1f", res.getLatencyMs())).append(" ms. ");

        if ("DECLINE".equalsIgnoreCase(res.getDecision())) {
            narrative.append("CRITICAL RISK DETECTED (Aggregate Risk: ")
                    .append(Math.round(res.getRiskScore() * 100)).append("%). ")
                    .append("The evaluation triggered high-conviction anti-fraud rules and behavioral anomalies. ")
                    .append("Immediate suspension is recommended to avoid settlement chargebacks.");
            r.setRecommendedAction("Block authorization immediately. Dispatch urgent 2FA verification to cardholder and freeze automated debit token.");
        } else if ("REVIEW".equalsIgnoreCase(res.getDecision())) {
            narrative.append("ELEVATED ANOMALY FLAGGED (Aggregate Risk: ")
                    .append(Math.round(res.getRiskScore() * 100)).append("%). ")
                    .append("While no hard sanctions were triggered, the ML model observed significant variance from user baseline spend and location patterns.");
            r.setRecommendedAction("Hold transaction in escrow. Trigger out-of-band mobile biometric challenge or automated voice confirmation.");
        } else {
            narrative.append("NORMAL BEHAVIOR (Confidence: ")
                    .append(Math.round((1.0 - res.getRiskScore()) * 100)).append("%). ")
                    .append("Cardholder fingerprint, merchant reputation, and spending volume fall within safe statistical boundaries.");
            r.setRecommendedAction("Authorize transaction with frictionless zero-friction processing.");
        }

        r.setExecutiveSummary(narrative.toString());
        r.setAuditTrailHash(generateAuditHash(res.getTransactionId() + res.getRiskScore() + res.getDecision()));

        return r;
    }

    private String formatFeature(String key) {
        return switch (key) {
            case "log_amount" -> "Transaction Value";
            case "amount_vs_usual" -> "Deviation From Spend Baseline";
            case "txns_last_hour" -> "Velocity Frequency";
            case "night_time" -> "Nocturnal Activity";
            case "foreign_country" -> "Cross-border Origin";
            case "unseen_device" -> "Hardware Anonymity";
            case "merchant_risk" -> "Merchant Category Hazard";
            case "log_distance_km" -> "Distance From Known Geolocation";
            default -> key;
        };
    }

    private String generateAuditHash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder("GH-AUDIT-");
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            return "GH-AUDIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
