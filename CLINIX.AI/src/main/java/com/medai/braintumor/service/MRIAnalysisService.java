package com.medai.braintumor.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.braintumor.model.AnalysisResponse;
import com.medai.braintumor.model.TumorFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MRIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MRIAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrtEnvironment env;
    private OrtSession session;

    // The order of classes usually imported by PyTorch ImageFolder
    // Assuming: 0=Glioma, 1=Meningioma, 2=no_tumor, 3=Pituitary
    // The exact names depend on the dataset folder structure, but usually
    // alphabetical.
    private static final String[] CLASSES = { "Glioma", "Meningioma", "No Tumor", "Pituitary" };

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            
            // 1. Try to find the model as a regular file (works in IDE/Local)
            File modelFile = new File("src/main/resources/clinix_mri_model_standalone.onnx");
            String modelPath;

            if (modelFile.exists()) {
                modelPath = modelFile.getAbsolutePath();
                log.info("Loading ONNX model from filesystem: {}", modelPath);
            } else {
                // 2. Load from Classpath and copy to temp file (works in JAR/Production)
                log.info("Model not found as file, attempting classpath load...");
                ClassPathResource resource = new ClassPathResource("clinix_mri_model_standalone.onnx");
                
                File tempFile = File.createTempFile("clinix_mri_model", ".onnx");
                tempFile.deleteOnExit();
                
                try (InputStream is = resource.getInputStream()) {
                    java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                modelPath = tempFile.getAbsolutePath();
                log.info("Loading ONNX model from temp file: {}", modelPath);
            }

            session = env.createSession(modelPath);
            log.info("ONNX Model loaded successfully.");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to load ONNX model", e);
            // Re-throw to prevent the service from being used in a broken state
            throw new RuntimeException("Could not initialize MRI Analysis Service", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (session != null)
                session.close();
            if (env != null)
                env.close();
        } catch (OrtException e) {
            log.error("Error closing ONNX runtime", e);
        }
    }

    public AnalysisResponse analyzeMRIImage(MultipartFile imageFile, String conversationContext, String patientId)
            throws Exception {
        System.err.println("DEBUG: Received analysis request for file: " + imageFile.getOriginalFilename());
        if (session == null) {
            throw new IllegalStateException("ONNX Session is not initialized. Check server logs for startup errors.");
        }
        long startTime = System.currentTimeMillis();

        validateImageFile(imageFile);

        BufferedImage img = ImageIO.read(imageFile.getInputStream());
        if (img == null) {
            throw new IllegalArgumentException("Invalid image file format");
        }

        // 1. Preprocess: Resize to 224x224 and normalize
        FloatBuffer tensorBuffer = preprocessImage(img);

        // 2. Prepare Input Tensor
        long[] shape = new long[] { 1, 3, 224, 224 }; // batch, channels, height, width
        TumorFinding finding = new TumorFinding();

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, tensorBuffer, shape)) {
            Map<String, OnnxTensor> inputs = Collections.singletonMap("input", inputTensor);

            // 3. Run Inference
            try (OrtSession.Result result = session.run(inputs)) {
                float[][] output = (float[][]) result.get(0).getValue();
                float[] probabilities = softmax(output[0]);

                int predictedClass = argmax(probabilities);
                float confidence = probabilities[predictedClass];

                String className = CLASSES[predictedClass];

                finding.setImageQuality("Good");
                finding.setConfidenceScore((int) (confidence * 100));

                if ("No Tumor".equalsIgnoreCase(className) || "no_tumor".equalsIgnoreCase(className)) {
                    finding.setTumorDetected(false);
                    finding.setLikelyDiagnosis("No visible tumor detected.");
                    finding.setAdditionalNotes(
                            "The MRI scan appears clear. No significant masses or lesions observed.");
                } else {
                    finding.setTumorDetected(true);
                    finding.setLikelyDiagnosis(className + " Tumor");

                    TumorFinding.Details details = new TumorFinding.Details();
                    details.setEstimatedSize("Requires manual radiologist measurement");
                    details.setLocation("Brain parenchyma");
                    details.setLobe("Unspecified");
                    details.setHemisphere("Unspecified");
                    finding.setTumorFindings(details);

                    finding.setClinicalRecommendation(
                            "Immediate review by a board-certified neuroradiologist is recommended for confirmation and treatment planning.");
                    finding.setUrgencyLevel("Urgent");
                    finding.setKeyMRIFeatures(List.of("Mass lesion detected matching " + className + " profile."));
                }
                finding.setDisclaimer(
                        "⚠️ This AI analysis is powered by a local EfficientNet model for academic purposes. Do not use for clinical diagnosis.");
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // Convert finding to raw JSON for the frontend to digest
        String rawContent = objectMapper.writeValueAsString(finding);

        return buildAnalysisResponse(finding, rawContent, UUID.randomUUID().toString(), patientId, processingTime);
    }

    public String handleFollowUp(String userMessage, String sessionId) throws Exception {
        return "Chat support is currently offline in local ONNX mode. The model focuses purely on image classification.";
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image file provided");
        }
    }

    private FloatBuffer preprocessImage(BufferedImage img) {
        // Resize
        BufferedImage resized = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, 224, 224, null);
        g.dispose();

        FloatBuffer buffer = FloatBuffer.allocate(3 * 224 * 224);

        // PyTorch normalization: mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]
        float[] mean = { 0.485f, 0.456f, 0.406f };
        float[] std = { 0.229f, 0.224f, 0.225f };

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    int rgb = resized.getRGB(x, y);
                    float val = 0;
                    if (c == 0)
                        val = ((rgb >> 16) & 0xFF) / 255.0f; // R
                    else if (c == 1)
                        val = ((rgb >> 8) & 0xFF) / 255.0f; // G
                    else if (c == 2)
                        val = (rgb & 0xFF) / 255.0f; // B

                    val = (val - mean[c]) / std[c];
                    buffer.put(val);
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    private float[] softmax(float[] input) {
        float[] output = new float[input.length];
        float max = input[0];
        for (float v : input)
            if (v > max)
                max = v;

        float sum = 0;
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) Math.exp(input[i] - max);
            sum += output[i];
        }
        for (int i = 0; i < input.length; i++) {
            output[i] /= sum;
        }
        return output;
    }

    private int argmax(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx])
                maxIdx = i;
        }
        return maxIdx;
    }

    private AnalysisResponse buildAnalysisResponse(TumorFinding finding, String rawContent, String sessionId,
            String patientId, long processingTimeMs) {
        AnalysisResponse resp = new AnalysisResponse();
        resp.setSessionId(sessionId);
        resp.setPatientId(patientId != null ? patientId : "ANON-" + UUID.randomUUID().toString().substring(0, 8));
        resp.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        resp.setProcessingTimeMs(processingTimeMs);
        resp.setRawAnalysis(rawContent);
        resp.setFinding(finding);
        resp.setSuccess(true);
        resp.setDisclaimer(finding.getDisclaimer());
        return resp;
    }
}
