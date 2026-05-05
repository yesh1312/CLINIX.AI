package com.clinixai.llm.impl;

import com.clinixai.llm.LLMProvider;
import com.medai.braintumor.model.TumorFinding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements LLMProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${clinixai.llm.gemini.api-key:}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = """
        You are NEURO-AI, an advanced Brain MRI Analysis Engine. 
        STRICT ZERO-HALLUCINATION POLICY.
        ANALYSIS PROTOCOL:
        1. QUALITY ASSESSMENT
        2. ANOMALY DETECTION
        3. TUMOR CHARACTERIZATION
        4. CLINICAL RECOMMENDATION
        
        Respond ONLY with valid JSON:
        {
          "imageQuality": "Good|Fair|Poor",
          "tumorDetected": true|false,
          "confidenceScore": 0-100,
          "tumorFindings": { "location": "string", "size": "string", "morphology": "string", "enhancementPattern": "string" },
          "likelyDiagnosis": "string",
          "differentialDiagnoses": ["string"],
          "regionHighlight": { "approximateX": 0-100, "approximateY": 0-100, "description": "string", "quadrant": "string", "radiusPercent": 10-20 },
          "keyMRIFeatures": ["string"],
          "clinicalRecommendation": "string",
          "urgencyLevel": "Routine|Urgent|Emergency",
          "additionalNotes": "string",
          "disclaimer": "string"
        }
        STRICT SPATIAL ACCURACY: Ensure regionHighlight coordinates (approximateX, approximateY) correspond precisely to the lesion's center point (0-100 scale).""";

    public GeminiProvider(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
    }

    @Override
    public TumorFinding analyzeMRI(byte[] imageBytes, String context, String onnxResult) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API Key is not configured.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String prompt = SYSTEM_PROMPT + "\n\nContext: " + (context != null ? context : "Analyze this scan.") + "\n\nONNX Classifier Result: " + onnxResult;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt),
                    Map.of("inlineData", Map.of(
                        "mimeType", "image/jpeg",
                        "data", base64Image
                    ))
                ))
            )
        );

        String result = webClient.post()
            .uri(uriBuilder -> uriBuilder.path("/v1/models/gemini-1.5-flash:generateContent")
                .queryParam("key", apiKey)
                .build())
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        // Parse structured response from Gemini
        Map<String, Object> responseMap = objectMapper.readValue(result, Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates returned from Gemini API");
        }
        
        String rawText = (String) ((Map<String, Object>) ((List<Map<String, Object>>) ((Map<String, Object>) candidates.get(0).get("content")).get("parts")).get(0)).get("text");
        log.info("Gemini Raw Response: {}", rawText);
        
        // Strip markdown if present
        String jsonText = rawText.replaceAll("```json|```", "").trim();
        try {
            return objectMapper.readValue(jsonText, TumorFinding.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON: {}. Raw text: {}", e.getMessage(), jsonText);
            throw e;
        }
    }

    @Override
    public String chat(String message, String history) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) return "System: AI Assistant API Key not configured.";
        
        String prompt = "You are NEURO-AI Medical Assistant. Answer concisely based on this scan history and medical knowledge.\\n\\nScan Context: " + history + "\\n\\nUser: " + message;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        String result = webClient.post()
            .uri(uriBuilder -> uriBuilder.path("/v1/models/gemini-1.5-flash:generateContent")
                .queryParam("key", apiKey)
                .build())
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        Map<String, Object> responseMap = objectMapper.readValue(result, Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        return (String) ((Map<String, Object>) ((List<Map<String, Object>>) ((Map<String, Object>) candidates.get(0).get("content")).get("parts")).get(0)).get("text");
    }

    @Override
    public String getName() {
        return "gemini";
    }
}
