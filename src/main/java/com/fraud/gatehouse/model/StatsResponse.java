package com.fraud.gatehouse.model;

public class StatsResponse {
    private long total;
    private long approved;
    private long review;
    private long declined;
    private double avgLatencyMs;
    private double reviewThreshold;
    private double declineThreshold;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getApproved() { return approved; }
    public void setApproved(long approved) { this.approved = approved; }

    public long getReview() { return review; }
    public void setReview(long review) { this.review = review; }

    public long getDeclined() { return declined; }
    public void setDeclined(long declined) { this.declined = declined; }

    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }

    public double getReviewThreshold() { return reviewThreshold; }
    public void setReviewThreshold(double reviewThreshold) { this.reviewThreshold = reviewThreshold; }

    public double getDeclineThreshold() { return declineThreshold; }
    public void setDeclineThreshold(double declineThreshold) { this.declineThreshold = declineThreshold; }
}
