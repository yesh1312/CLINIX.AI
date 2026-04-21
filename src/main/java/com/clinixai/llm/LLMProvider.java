package com.clinixai.llm;

import com.medai.braintumor.model.TumorFinding;

public interface LLMProvider {
    /**
     * Analyze MRI and return a TumorFinding object.
     * 
     * @param imageBytes The raw MRI image bytes for vision processing
     * @param context    Clinical symptoms/metadata
     * @param onnxResult Result from the local ONNX model (fallback/supplement)
     * @return Fully populated TumorFinding
     */
    TumorFinding analyzeMRI(byte[] imageBytes, String context, String onnxResult) throws Exception;

    /**
     * Basic follow-up chat
     */
    String chat(String message, String history) throws Exception;

    /**
     * Provider identification
     */
    String getName();
}
