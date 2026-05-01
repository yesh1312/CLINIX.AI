package com.medai.braintumor.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.braintumor.model.AnalysisResponse;
import com.medai.braintumor.model.TumorFinding;
import com.clinixai.model.AnalysisHistory;
import com.clinixai.model.User;
import com.clinixai.repository.HistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MRIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MRIAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private HistoryRepository historyRepository;

    private OrtEnvironment env;
    @Autowired
    private com.clinixai.llm.LLMProviderFactory llmProviderFactory;

    private OrtSession session;
    private final Map<String, TumorFinding> lastFindings = new java.util.concurrent.ConcurrentHashMap<>();

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

    public AnalysisResponse analyzeMRIImage(MultipartFile imageFile, String conversationContext, String patientId,
            String sessionId, User user)
            throws Exception {
        System.err.println("DEBUG: Received analysis request for file: " + imageFile.getOriginalFilename());
        if (session == null) {
            throw new IllegalStateException("ONNX Session is not initialized. Check server logs for startup errors.");
        }
        long startTime = System.currentTimeMillis();

        validateImageFile(imageFile);

        // Read the image file into a byte array once, so it can be used for both ONNX and LLM processing
        byte[] imageBytes = imageFile.getBytes();

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) {
            throw new IllegalArgumentException("Invalid image file format. Ensure the upload is a valid image.");
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
                    finding.setTumorClass("notumor");
                    finding.setTumorNature("N/A");
                    finding.setLikelyDiagnosis("No visible tumor detected.");
                    finding.setUrgencyLevel("Routine");
                    finding.setAdditionalNotes(
                            "The MRI scan appears clear. No significant masses or lesions observed.");
                    finding.setClinicalRecommendation(
                            "No immediate action required. Routine follow-up is recommended if symptoms persist.");
                } else {
                    finding.setTumorDetected(true);
                    // Normalize class name to a lowercase key for the frontend CLASS_INFO map
                    String classKey = className.toLowerCase().replace(" ", "");
                    finding.setTumorClass(classKey);

                    if ("Glioma".equalsIgnoreCase(className)) {
                        finding.setTumorNature("Malignant");
                        finding.setLikelyDiagnosis("Glioma — High-grade (Tentative)");
                        finding.setClinicalRecommendation(
                                "Immediate neurosurgical consultation is required. Contrast-enhanced MRI (T1+C) and MRS are recommended to assess metabolic activity. Consider steroids (Dexamethasone) if mass effect is present.");
                        finding.setUrgencyLevel("Urgent");
                    } else if ("Meningioma".equalsIgnoreCase(className)) {
                        finding.setTumorNature("Benign");
                        finding.setLikelyDiagnosis("Meningioma — Extra-axial (Tentative)");
                        finding.setClinicalRecommendation(
                                "Neurosurgical referral recommended. If asymptomatic and small, serial imaging (6 months) may be considered. If symptomatic or causing mass effect, surgical resection or Gamma Knife SRS should be discussed.");
                        finding.setUrgencyLevel("Semi-urgent");
                    } else if ("Pituitary".equalsIgnoreCase(className)) {
                        finding.setTumorNature("Benign");
                        finding.setLikelyDiagnosis("Pituitary Adenoma — Micro/Macro (Tentative)");
                        finding.setClinicalRecommendation(
                                "Neurosurgical and Endocrinological workup required. Formal visual field testing (perimetry) and pituitary hormone panel (PRL, GH, ACTH, TSH/fT4, IGF-1) are indicated.");
                        finding.setUrgencyLevel("Semi-urgent");
                    } else {
                        finding.setTumorNature("Indeterminate");
                        finding.setLikelyDiagnosis(className + " Tumor");
                        finding.setClinicalRecommendation(
                                "Neurosurgical review recommended for further characterization and management planning.");
                        finding.setUrgencyLevel("Urgent");
                    }

                    // Initialize details to avoid NPE in chat/report
                    TumorFinding.Details details = new TumorFinding.Details();
                    details.setLocation("Brain parenchyma");
                    details.setEstimatedSize("Requires manual radiologist measurement");
                    details.setMorphology("Mass lesion matching " + className + " profile");
                    details.setEnhancementPattern("Requires T1+C for assessment");
                    finding.setTumorFindings(details);

                    finding.setKeyMRIFeatures(List.of("Mass lesion detected matching " + className + " profile."));
                }
                finding.setDisclaimer(
                        "⚠️ This AI analysis is powered by a local EfficientNet model for academic purposes. Do not use for clinical diagnosis.");
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // --- LLM ENHANCEMENT ---
        try {
            com.clinixai.llm.LLMProvider provider = llmProviderFactory.getActiveProvider();
            if (provider != null) {
                log.info("Enhancing analysis with LLM Provider: {}", provider.getName());
                // Pass ONNX class name so MockProvider can use it for more accurate enrichment
                String onnxClassName = finding.getTumorClass() != null ? finding.getTumorClass() : (finding.getLikelyDiagnosis() != null ? finding.getLikelyDiagnosis() : "unknown");
                TumorFinding enhancedFinding = provider.analyzeMRI(imageBytes, conversationContext, onnxClassName);

                // Merge: LLM detection status always wins (it can flip both ways)
                finding.setTumorDetected(enhancedFinding.isTumorDetected());

                if (enhancedFinding.getLikelyDiagnosis() != null) {
                    finding.setLikelyDiagnosis(enhancedFinding.getLikelyDiagnosis());
                }
                // Preserve ONNX class/nature unless LLM explicitly provides them
                if (enhancedFinding.getTumorClass() != null) {
                    finding.setTumorClass(enhancedFinding.getTumorClass());
                }
                if (enhancedFinding.getTumorNature() != null) {
                    finding.setTumorNature(enhancedFinding.getTumorNature());
                }
                if (enhancedFinding.getDifferentialDiagnoses() != null) {
                    finding.setDifferentialDiagnoses(enhancedFinding.getDifferentialDiagnoses());
                }
                if (enhancedFinding.getKeyMRIFeatures() != null) {
                    finding.setKeyMRIFeatures(enhancedFinding.getKeyMRIFeatures());
                }
                if (enhancedFinding.getClinicalRecommendation() != null) {
                    finding.setClinicalRecommendation(enhancedFinding.getClinicalRecommendation());
                }
                if (enhancedFinding.getUrgencyLevel() != null) {
                    finding.setUrgencyLevel(enhancedFinding.getUrgencyLevel());
                }
                if (enhancedFinding.getAdditionalNotes() != null) {
                    finding.setAdditionalNotes(enhancedFinding.getAdditionalNotes());
                }
                if (enhancedFinding.getTumorFindings() != null) {
                    finding.setTumorFindings(enhancedFinding.getTumorFindings());
                }
                if (enhancedFinding.getRegionHighlight() != null) {
                    finding.setRegionHighlight(enhancedFinding.getRegionHighlight());
                }
            }
        } catch (Exception e) {
            log.warn("LLM Enhancement failed, falling back to local ONNX only: {}", e.getMessage());
        } finally {
            // Hint GC to clean up image buffers and tensor data as soon as possible
            System.gc();
        }
        // -------------------------

        // Convert finding to raw JSON for the frontend to digest
        String rawContent = objectMapper.writeValueAsString(finding);

        // Store finding for follow-up context
        String sid = sessionId != null ? sessionId : "default";
        lastFindings.put(sid, finding);

        // Save to Database History if user is provided
        if (user != null) {
            try {
                AnalysisHistory history = new AnalysisHistory();
                history.setUser(user);
                history.setTimestamp(LocalDateTime.now());
                history.setFindingJson(rawContent);
                history.setPatientId(patientId);
                history.setSessionId(sid);
                history.setLikelyDiagnosis(finding.getLikelyDiagnosis());
                history.setUrgencyLevel(finding.getUrgencyLevel());
                historyRepository.save(history);
                log.info("Analysis history saved for user: {}", user.getUsername());
            } catch (Exception e) {
                log.error("Failed to save analysis history to database", e);
            }
        }

        return buildAnalysisResponse(finding, rawContent, sid, patientId, processingTime);
    }

    public String handleFollowUp(String userMessage, String sessionId) {
        String msg = userMessage != null ? userMessage.toLowerCase() : "";
        String sid = sessionId != null ? sessionId : "default";
        TumorFinding lastFinding = lastFindings.get(sid);

        // Try LLM for intelligent follow-up first
        try {
            com.clinixai.llm.LLMProvider provider = llmProviderFactory.getActiveProvider();
            if (!"mock".equalsIgnoreCase(provider.getName())) {
                String context = lastFinding != null ? objectMapper.writeValueAsString(lastFinding) : "No scan data";
                return provider.chat(userMessage, context);
            }
        } catch (Exception e) {
            log.warn("LLM Chat failed, falling back to rule-based: {}", e.getMessage());
        }

        // 1. Handle Contextual Questions (if finding exists)
        if (lastFinding != null) {
            if (msg.contains("urgency") || msg.contains("urgent")) {
                return "The clinical urgency for this finding is categorized as: **" + lastFinding.getUrgencyLevel()
                        + "**. " + lastFinding.getClinicalRecommendation();
            }
            if (msg.contains("diagnosis") || msg.contains("what is it")) {
                return "The most likely diagnosis based on the AI analysis is **" + lastFinding.getLikelyDiagnosis()
                        + "**. The model is seeing "
                        + (lastFinding.getTumorFindings() != null ? lastFinding.getTumorFindings().getMorphology()
                                : "atypical")
                        + " features.";
            }
            if (msg.contains("size") || msg.contains("big")) {
                String size = (lastFinding.getTumorFindings() != null)
                        ? lastFinding.getTumorFindings().getEstimatedSize()
                        : "Requires measurement";
                String loc = (lastFinding.getTumorFindings() != null) ? lastFinding.getTumorFindings().getLocation()
                        : "Brain parenchyma";
                return "The estimated size of the lesion is **" + size + "**. It is located in the " + loc + ".";
            }
            if (msg.contains("malignant") || msg.contains("cancer") || msg.contains("grade")) {
                if (lastFinding.getLikelyDiagnosis().toLowerCase().contains("no tumor")
                        || lastFinding.getLikelyDiagnosis().toLowerCase().contains("no_tumor")) {
                    return "No tumor was detected in the scan, so malignancy markers are not applicable.";
                }
                String enh = (lastFinding.getTumorFindings() != null)
                        ? lastFinding.getTumorFindings().getEnhancementPattern()
                        : "variable";
                return "The AI has classified this as " + lastFinding.getLikelyDiagnosis()
                        + ". For definitive grading, a histopathological biopsy is the clinical gold standard. However, the MRI features suggest "
                        + enh + " enhancement.";
            }
        }

        // 2. Handle Medical Tools / Knowledge Base (Rule-based)
        if (msg.contains("who 2021") || msg.contains("grading")) {
            return "**WHO 2021 CNS Classification Summary:**\n" +
                    "• **Grade 1:** Benign, slow-growing (e.g., Pilocytic Astrocytoma)\n" +
                    "• **Grade 2:** Low-grade, infiltrative (e.g., Diffuse Glioma IDH-mutant)\n" +
                    "• **Grade 3:** Malignant, high mitotic activity\n" +
                    "• **Grade 4:** Highly malignant, necrosis/vascular proliferation (e.g., Glioblastoma, IDH-wildtype)\n\n"
                    +
                    "*Key markers: IDH mutation, 1p/19q codeletion, CDKN2A/B deletion, MGMT methylation.*";
        }

        if (msg.contains("t1") || msg.contains("t2") || msg.contains("flair") || msg.contains("sequence")) {
            return "**Standard Brain MRI Protocol Guide:**\n" +
                    "• **T1:** Best for anatomy. Tumors usually hypointense (dark).\n" +
                    "• **T1 + Contrast:** Shows blood-brain barrier breakdown (enhancement).\n" +
                    "• **T2:** Shows water/fluid. Tumors usually hyperintense (bright).\n" +
                    "• **FLAIR:** Suppresses CSF. Critical for identifying perilesional edema.\n" +
                    "• **DWI/ADC:** Measures water diffusion. Low ADC suggests high cellularity/malignancy.";
        }

        if (msg.contains("stupp") || msg.contains("gbm") || msg.contains("glioblastoma")) {
            return "**Glioblastoma (GBM) Stupp Protocol:**\n" +
                    "1. **Maximum Safe Resection:** Initial surgical debulking.\n" +
                    "2. **Radiotherapy:** 60 Gy in 30 fractions over 6 weeks.\n" +
                    "3. **Concomitant Chemotherapy:** Temozolomide (75 mg/m²) daily during RT.\n" +
                    "4. **Adjuvant Chemotherapy:** Temozolomide (150-200 mg/m²) for 6-12 cycles (5 days every 28 days).";
        }

        if (msg.contains("icd-10") || msg.contains("code")) {
            return "**Key ICD-10-CM Codes for CNS Neoplasms:**\n" +
                    "• **C71.x:** Malignant neoplasm of brain (primary)\n" +
                    "• **D33.x:** Benign neoplasm of brain/CNS\n" +
                    "• **D32.x:** Neoplasm of meninges\n" +
                    "• **D35.2:** Neoplasm of pituitary gland\n" +
                    "• **C79.31:** Secondary malignant neoplasm (Metastasis).";
        }

        if (msg.contains("ring-enhancing") || msg.contains("differential")) {
            return "**Differential Diagnosis for Ring-Enhancing Lesions (MAGIC DR):**\n" +
                    "• **M**etastasis (often multiple, gray-white junction)\n" +
                    "• **A**bscess (restricted diffusion on DWI/ADC)\n" +
                    "• **G**lioblastoma (thick irregular rim, central necrosis)\n" +
                    "• **I**nfarct (subacute stage)\n" +
                    "• **C**ontusion\n" +
                    "• **D**emyelinating disease (Tumefactive MS, incomplete ring)\n" +
                    "• **R**adiation necrosis.";
        }

        if (msg.contains("abscess") || msg.contains("infection")) {
            return "**Brain Abscess — Clinical Summary:**\n" +
                    "• **Etiology:** Bacterial (Streptococcus, Staphylococcus), fungal, parasitic\n" +
                    "• **Key MRI Features:** Ring-enhancing lesion with restricted diffusion (DWI bright, ADC dark) — key differentiator from tumor\n"
                    +
                    "• **Stages:** Cerebritis → Early capsule → Late capsule → Resolution\n" +
                    "• **Treatment:** IV antibiotics (Ceftriaxone + Metronidazole ± Vancomycin) for 4–8 weeks\n" +
                    "• **Surgery:** Stereotactic aspiration if >2.5 cm, posterior fossa, or failing medical therapy\n" +
                    "• **ICD-10:** G06.0 (Intracranial abscess)";
        }

        if (msg.contains("stroke") || msg.contains("ischemic") || msg.contains("infarct")) {
            return "**Cerebrovascular Stroke — Clinical Summary:**\n" +
                    "• **Ischemic Stroke MRI:** DWI bright (restricted diffusion), ADC dark, FLAIR positive after 6 hrs\n"
                    +
                    "• **Hemorrhagic Stroke MRI:** T2*/SWI blooming artifact, GRE hypointense\n" +
                    "• **tPA Window:** IV alteplase within 4.5 hours of onset (NIHSS ≥ 4)\n" +
                    "• **Thrombectomy Window:** Up to 24 hrs (DAWN/DEFUSE-3 criteria) for large vessel occlusion\n" +
                    "• **NIHSS Scoring:** 0 = no deficit, 1-4 = minor, 5-15 = moderate, 16-20 = moderate-severe, 21-42 = severe\n"
                    +
                    "• **ICD-10:** I63.x (Cerebral infarction), I61.x (Intracerebral hemorrhage)";
        }

        if (msg.contains("hemorrhage") || msg.contains("bleed") || msg.contains("epidural") || msg.contains("subdural")
                || msg.contains("subarachnoid")) {
            return "**Intracranial Hemorrhage Types:**\n" +
                    "• **Epidural:** Biconvex/lens-shaped, arterial (middle meningeal a.), does NOT cross sutures\n" +
                    "• **Subdural:** Crescent-shaped, venous (bridging veins), crosses sutures, not dural folds\n" +
                    "• **Subarachnoid (SAH):** Blood in sulci/cisterns. Hunt-Hess grading (I–V). CTA for aneurysm\n" +
                    "• **Intraparenchymal (IPH):** Within brain tissue. Hypertension #1 cause. CT: hyperdense acutely\n"
                    +
                    "• **MRI Signal Evolution:** Hyperacute (T1 iso, T2 bright) → Acute (T1 iso, T2 dark) → Subacute → Chronic\n"
                    +
                    "• **ICD-10:** I60.x (SAH), I61.x (ICH), I62.0 (SDH), S06.4 (EDH)";
        }

        if (msg.contains("biopsy") || msg.contains("histopath") || msg.contains("stereotactic")) {
            return "**Brain Biopsy — Clinical Guide:**\n" +
                    "• **Indications:** Indeterminate lesion on imaging, deep-seated tumors, suspected lymphoma, non-surgical candidates\n"
                    +
                    "• **Techniques:** Stereotactic (frame-based/frameless), Open/craniotomy, Endoscopic\n" +
                    "• **Stereotactic Accuracy:** 91–97% diagnostic yield\n" +
                    "• **Risks:** Hemorrhage (1–5%), infection (<1%), neurological deficit (3–5%)\n" +
                    "• **Histological Markers:** IDH1/2 mutation, ATRX, 1p/19q codeletion, Ki-67 proliferation index\n"
                    +
                    "• **Contraindications:** Coagulopathy, vascular malformation, readily accessible lesion suitable for resection";
        }

        if (msg.contains("autopsy") || msg.contains("post-mortem") || msg.contains("neuropath")) {
            return "**Neuropathology Autopsy — Clinical Guide:**\n" +
                    "• **Brain Removal:** Standard 2-week fixation in 10% formalin\n" +
                    "• **Sampling Protocol:** Standard blocks from cortex, hippocampus, basal ganglia, brainstem, cerebellum, spinal cord\n"
                    +
                    "• **Staining Methods:** H&E (routine), IHC (GFAP, synaptophysin, Ki-67), special stains (Luxol fast blue, Congo red)\n"
                    +
                    "• **Common Findings:** Neurodegeneration (Alzheimer's, Parkinson's), vascular pathology, neoplasia, infection\n"
                    +
                    "• **Reporting:** Includes gross description, microscopic findings, clinicopathological correlation, final diagnosis\n"
                    +
                    "• **Molecular:** Tau immunostaining (Braak staging), α-synuclein, TDP-43";
        }

        if (msg.contains("malignant") || msg.contains("benign") || msg.contains("grade")) {
            if (lastFinding != null && (lastFinding.getLikelyDiagnosis().toLowerCase().contains("no tumor")
                    || lastFinding.getLikelyDiagnosis().toLowerCase().contains("no_tumor"))) {
                return "No tumor was detected in the scan, so malignancy markers are not applicable.";
            }
            return "**Malignant vs Benign Brain Tumors:**\n" +
                    "• **Malignant:** Rapid growth, infiltrative margins, necrosis, high cellularity, DWI restriction, elevated rCBV, high choline peak on MRS\n"
                    +
                    "• **Benign:** Slow growth, well-circumscribed, no necrosis, low cellularity, no DWI restriction, low rCBV\n"
                    +
                    "• **WHO Grading:** Grade 1-2 (low-grade/benign behavior), Grade 3-4 (high-grade/malignant)\n" +
                    "• **Key Molecular Markers:** IDH mutation (favorable), 1p/19q codeletion (favorable), MGMT methylation (TMZ responsive)\n"
                    +
                    "• **Enhancement:** Malignant tends to enhance (BBB breakdown), benign may not enhance\n" +
                    "• **Prognosis:** Malignant (GBM median 15 months), Benign (meningioma >90% 5-year survival)";
        }

        // 3. Fallback
        return "I am the CLINIX.AI local assistant. I can interpret MRI findings (if a scan is uploaded) or provide information on WHO grading, MRI protocols, and treatment guidelines for brain tumors. Please ask a specific clinical question.\n\n⚠️ Always verify with current clinical guidelines and a qualified specialist.";
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
