package com.fraud.gatehouse.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class EvaluationResponse {
    private String transactionId;
    private OffsetDateTime evaluatedAt;
    private String userId;
    private double amount;
    private String merchantCategory;
    private String country;
    private String decision; // APPROVE, REVIEW, DECLINE
    private double riskScore;
    private double modelScore;
    private double latencyMs;
    private List<RuleHit> ruleHits;
    private List<String> reasons;
    private Map<String, Double> features;
    private Map<String, Double> shapContributions;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(OffsetDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public double getModelScore() { return modelScore; }
    public void setModelScore(double modelScore) { this.modelScore = modelScore; }

    public double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }

    public List<RuleHit> getRuleHits() { return ruleHits; }
    public void setRuleHits(List<RuleHit> ruleHits) { this.ruleHits = ruleHits; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public Map<String, Double> getFeatures() { return features; }
    public void setFeatures(Map<String, Double> features) { this.features = features; }

    public Map<String, Double> getShapContributions() { return shapContributions; }
    public void setShapContributions(Map<String, Double> shapContributions) { this.shapContributions = shapContributions; }
}
