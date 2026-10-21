package com.fraud.gatehouse.model;

public class RuleHit {
    private String code;
    private String description;
    private boolean hardBlock;
    private double weight;

    public RuleHit() {}

    public RuleHit(String code, String description, boolean hardBlock, double weight) {
        this.code = code;
        this.description = description;
        this.hardBlock = hardBlock;
        this.weight = weight;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHardBlock() { return hardBlock; }
    public void setHardBlock(boolean hardBlock) { this.hardBlock = hardBlock; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
