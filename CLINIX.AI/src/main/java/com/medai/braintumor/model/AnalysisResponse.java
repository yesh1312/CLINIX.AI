package com.medai.braintumor.model;

import java.util.List;

public class AnalysisResponse {
    private String sessionId;
    private String patientId;
    private String timestamp;
    private long processingTimeMs;
    private String rawAnalysis;
    private TumorFinding finding;
    private boolean success;
    private String disclaimer;
    private String error;

    public AnalysisResponse() {
    }

    public static AnalysisResponse error(String message) {
        AnalysisResponse resp = new AnalysisResponse();
        resp.setSuccess(false);
        resp.setError(message);
        return resp;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getRawAnalysis() {
        return rawAnalysis;
    }

    public void setRawAnalysis(String rawAnalysis) {
        this.rawAnalysis = rawAnalysis;
    }

    public TumorFinding getFinding() {
        return finding;
    }

    public void setFinding(TumorFinding finding) {
        this.finding = finding;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
