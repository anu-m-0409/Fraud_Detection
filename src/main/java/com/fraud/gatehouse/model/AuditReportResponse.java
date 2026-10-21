package com.fraud.gatehouse.model;

import java.util.List;

public class AuditReportResponse {
    private String transactionId;
    private String executiveSummary;
    private String riskLevel;
    private List<String> primaryDrivers;
    private List<String> complianceViolations;
    private String recommendedAction;
    private String auditTrailHash;

    public AuditReportResponse() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getPrimaryDrivers() { return primaryDrivers; }
    public void setPrimaryDrivers(List<String> primaryDrivers) { this.primaryDrivers = primaryDrivers; }

    public List<String> getComplianceViolations() { return complianceViolations; }
    public void setComplianceViolations(List<String> complianceViolations) { this.complianceViolations = complianceViolations; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public String getAuditTrailHash() { return auditTrailHash; }
    public void setAuditTrailHash(String auditTrailHash) { this.auditTrailHash = auditTrailHash; }
}
