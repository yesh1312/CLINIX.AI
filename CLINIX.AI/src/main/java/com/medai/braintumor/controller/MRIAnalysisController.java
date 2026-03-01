package com.medai.braintumor.controller;

import com.medai.braintumor.model.AnalysisRequest;
import com.medai.braintumor.model.AnalysisResponse;
import com.medai.braintumor.service.MRIAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class MRIAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(MRIAnalysisController.class);

    @Autowired
    private MRIAnalysisService mriAnalysisService;

    /**
     * Analyze an MRI image for brain tumor detection
     * Accepts multipart image upload + optional conversation context
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeMRI(
            @RequestPart("image") MultipartFile imageFile,
            @RequestPart(value = "context", required = false) String conversationContext,
            @RequestPart(value = "patientId", required = false) String patientId) {

        try {
            AnalysisResponse response = mriAnalysisService.analyzeMRIImage(
                    imageFile, conversationContext, patientId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Analysis failed with exception", e);
            return ResponseEntity.internalServerError()
                    .body(AnalysisResponse.error("Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Chat endpoint for follow-up questions about a previous analysis
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody AnalysisRequest request) {
        try {
            String reply = mriAnalysisService.handleFollowUp(
                    request.getMessage(), request.getSessionId());
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Brain Tumor Detection Engine",
                "version", "1.0.0"));
    }
}
