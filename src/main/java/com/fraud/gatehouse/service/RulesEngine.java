package com.fraud.gatehouse.service;

import com.fraud.gatehouse.model.RuleHit;
import com.fraud.gatehouse.model.TransactionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RulesEngine {

    @Value("${fraud.max-amount-ceiling:500000.0}")
    private double maxAmountCeiling;

    @Value("${fraud.hourly-velocity-limit:5}")
    private int hourlyVelocityLimit;

    private static final Set<String> BLOCKED_COUNTRIES = Set.of("KP", "IR", "SY");

    public List<RuleHit> evaluateRules(TransactionRequest req, Map<String, Double> features) {
        List<RuleHit> hits = new ArrayList<>();

        // 1. Hard Sanctions / Blocked Jurisdictions (OFAC / FATF Compliance)
        if (BLOCKED_COUNTRIES.contains(req.getCountry().toUpperCase())) {
            hits.add(new RuleHit(
                    "COMPLIANCE_SANCTIONED_JURISDICTION",
                    "Transaction originated from a sanctioned territory (" + req.getCountry() + "). Mandatory immediate decline.",
                    true,
                    1.0
            ));
        }

        // 2. Ceiling Limit Exceeded
        if (req.getAmount() > maxAmountCeiling) {
            hits.add(new RuleHit(
                    "POLICY_AMOUNT_CEILING_EXCEEDED",
                    "Amount exceeds absolute single-transaction threshold (₹" + maxAmountCeiling + ").",
                    true,
                    0.90
            ));
        }

        // 3. Velocity Burst
        double txLastHour = features.getOrDefault("txns_last_hour", 0.0);
        if (txLastHour >= hourlyVelocityLimit) {
            hits.add(new RuleHit(
                    "VELOCITY_BURST_ATTACK",
                    "High frequency burst: " + ((int)txLastHour) + " attempts detected within 60 minutes.",
                    false,
                    0.35
            ));
        }

        // 4. Triad Suspicion: Foreign Country + New Device + High Value Spike
        double foreign = features.getOrDefault("foreign_country", 0.0);
        double unseen = features.getOrDefault("unseen_device", 0.0);
        double spendRatio = features.getOrDefault("amount_vs_usual", 1.0);

        if (foreign > 0.5 && unseen > 0.5 && spendRatio >= 4.0) {
            hits.add(new RuleHit(
                    "ACCOUNT_TAKEOVER_TRIAD",
                    "Simultaneous foreign jurisdiction, unseen hardware fingerprint, and spend spike (" + String.format("%.1f", spendRatio) + "x usual).",
                    false,
                    0.40
            ));
        }

        // 5. High Risk Category at Night
        double night = features.getOrDefault("night_time", 0.0);
        double mRisk = features.getOrDefault("merchant_risk", 0.0);
        if (night > 0.5 && mRisk >= 0.8) {
            hits.add(new RuleHit(
                    "NOCTURNAL_HIGH_LIQUIDITY_EXCHANGE",
                    "Late night transaction through high liquidity / cash-out merchant (" + req.getMerchantCategory() + ").",
                    false,
                    0.28
            ));
        }

        return hits;
    }

    public double combineRisk(double modelScore, List<RuleHit> ruleHits) {
        for (RuleHit hit : ruleHits) {
            if (hit.isHardBlock()) return 1.0;
        }

        double score = modelScore;
        for (RuleHit hit : ruleHits) {
            // Asymptotic risk booster: increases risk without exceeding 1.0
            score = score + (1.0 - score) * hit.getWeight();
        }
        return Math.min(1.0, Math.max(0.0, score));
    }
}
