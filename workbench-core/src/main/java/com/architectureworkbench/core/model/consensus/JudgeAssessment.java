package com.architectureworkbench.core.model.consensus;

public class JudgeAssessment {
    private String judgeId;
    private String provider;
    private String model;
    private String verdict;
    private double confidence;
    private String rationaleRef;
    private String activityId;

    public String getJudgeId() { return judgeId; }
    public void setJudgeId(String judgeId) { this.judgeId = judgeId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getRationaleRef() { return rationaleRef; }
    public void setRationaleRef(String rationaleRef) { this.rationaleRef = rationaleRef; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
}
