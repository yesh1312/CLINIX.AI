package com.medai.braintumor.model;

public class AnalysisRequest {
    private String message;
    private String sessionId;

    public AnalysisRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
