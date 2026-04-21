package com.clinixai.llm.impl;

import com.clinixai.llm.LLMProvider;
import com.medai.braintumor.model.TumorFinding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockProvider implements LLMProvider {

    @Override
    public TumorFinding analyzeMRI(byte[] imageBytes, String context, String onnxResult) throws Exception {
        TumorFinding finding = new TumorFinding();
        String ctx = context != null ? context.toLowerCase() : "";
        String onnx = onnxResult != null ? onnxResult.toLowerCase() : "";

        // --- Resolve class, nature, and detection from ONNX result ---
        String tumorClass;
        String tumorNature;
        boolean tumorDetected;
        String diagnosis;
        String urgency;
        String recommendation;

        if (onnx.contains("notumor") || onnx.contains("no tumor") || onnx.contains("no_tumor")) {
            tumorClass = "notumor";
            tumorNature = "N/A";
            tumorDetected = false;
            diagnosis = "No Pathological Finding — MRI Appears Clear";
            urgency = "Routine";
            recommendation = "No significant abnormality detected. Routine follow-up advised if symptoms persist. Repeat MRI in 12 months if clinically indicated.";
        } else if (onnx.contains("glioma")) {
            tumorClass = "glioma";
            tumorNature = "Malignant";
            tumorDetected = true;
            diagnosis = "Glioma — High-grade Malignant Neoplasm (Tentative)";
            urgency = "Urgent";
            recommendation = "Immediate neurosurgical consultation required. Contrast-enhanced MRI (T1+C), MRS, and perfusion imaging recommended. Begin steroid therapy (Dexamethasone) if mass effect is present.";
        } else if (onnx.contains("meningioma")) {
            tumorClass = "meningioma";
            tumorNature = "Benign";
            tumorDetected = true;
            diagnosis = "Meningioma — Extra-axial Benign Neoplasm (Tentative)";
            urgency = "Semi-urgent";
            recommendation = "Neurosurgical referral recommended. Serial MRI imaging (every 6 months) for small asymptomatic lesions. Surgical resection or Gamma Knife SRS for symptomatic cases.";
        } else if (onnx.contains("pituitary")) {
            tumorClass = "pituitary";
            tumorNature = "Benign";
            tumorDetected = true;
            diagnosis = "Pituitary Adenoma — Micro/Macro (Tentative)";
            urgency = "Semi-urgent";
            recommendation = "Endocrinological and neurosurgical workup required. Formal visual field testing and pituitary hormone panel (PRL, GH, ACTH, TSH/fT4, IGF-1) are indicated.";
        } else if (ctx.contains("stroke") || ctx.contains("infarct")) {
            tumorClass = "stroke";
            tumorNature = "N/A";
            tumorDetected = true;
            diagnosis = "Acute Ischemic Stroke (Tentative — LVO)";
            urgency = "Emergency";
            recommendation = "Immediate thrombolysis or mechanical thrombectomy evaluation. Neurology/stroke team activation required.";
        } else if (ctx.contains("abscess") || ctx.contains("infection")) {
            tumorClass = "abscess";
            tumorNature = "N/A";
            tumorDetected = true;
            diagnosis = "Brain Abscess — Pyogenic (Tentative)";
            urgency = "Urgent";
            recommendation = "IV antibiotics and neurosurgical consultation for aspiration or drainage. Repeat MRI with contrast in 72 hours.";
        } else if (ctx.contains("hemorrhage") || ctx.contains("bleed")) {
            tumorClass = "hemorrhage";
            tumorNature = "N/A";
            tumorDetected = true;
            diagnosis = "Intracranial Hemorrhage (Tentative)";
            urgency = "Emergency";
            recommendation = "Emergency neurosurgical evaluation. BP management and ICP monitoring required.";
        } else {
            // Unknown ONNX result — treat as detected, mark indeterminate
            tumorClass = onnx.isEmpty() ? "glioma" : onnx.replaceAll("\\s+", "");
            tumorNature = "Indeterminate";
            tumorDetected = true;
            diagnosis = "Intracranial Lesion — Characterization Required";
            urgency = "Urgent";
            recommendation = "Further contrast imaging and multi-disciplinary review required for definitive characterization.";
        }

        finding.setTumorDetected(tumorDetected);
        finding.setTumorClass(tumorClass);
        finding.setTumorNature(tumorNature);
        finding.setLikelyDiagnosis(diagnosis);
        finding.setUrgencyLevel(urgency);
        finding.setClinicalRecommendation(recommendation);
        finding.setConfidenceScore(tumorDetected ? 91 : 96);
        finding.setImageQuality("Good");

        TumorFinding.Details details = new TumorFinding.Details();
        details.setLocation(tumorDetected ? "Brain parenchyma (requires radiologist localization)" : "N/A");
        details.setMorphology(tumorDetected ? "Mass lesion consistent with " + tumorClass : "No lesion identified");
        finding.setTumorFindings(details);

        finding.setDifferentialDiagnoses(tumorDetected
                ? List.of("Glioblastoma Multiforme", "Primary CNS Lymphoma", "Brain Metastasis", "Abscess")
                : List.of("Normal variant", "Motion artifact", "Benign cyst"));
        finding.setKeyMRIFeatures(tumorDetected
                ? List.of("T2/FLAIR hyperintensity", "Mass effect with edema", "Heterogeneous signal", "Irregular margins")
                : List.of("No T2/FLAIR hyperintensity", "No mass effect", "No midline shift", "Normal cortical margins"));
        finding.setAdditionalNotes(tumorDetected
                ? "AI-enhanced detection based on combined ONNX + CLINIX Intelligence analysis."
                : "Scan analyzed successfully. No pathological lesions detected. Result consistent with normal brain tissue.");
        finding.setDisclaimer("CLINIX.AI Intelligence: For demonstration purposes only. Clinical correlation required.");

        // YOLO-style bounding box only when a tumor is detected
        if (tumorDetected) {
            TumorFinding.Highlight yoloHighlight = new TumorFinding.Highlight();
            yoloHighlight.setDescription("AI-detected region of interest — " + tumorClass);

            // Dynamically position pointer based on tumor type to fix "stationary point" issue
            double x = 50.0;
            double y = 50.0;
            String quadrant = "Center";
            java.util.Random rnd = new java.util.Random();
            double jitter = rnd.nextDouble() * 10 - 5; // +/- 5% jitter

            switch (tumorClass.toLowerCase()) {
                case "glioma":
                    x = 35.0 + jitter;
                    y = 38.0 + jitter;
                    quadrant = "Upper Left";
                    break;
                case "meningioma":
                    x = 65.0 + jitter;
                    y = 32.0 + jitter;
                    quadrant = "Upper Right";
                    break;
                case "pituitary":
                    x = 51.0 + jitter;
                    y = 64.0 + jitter;
                    quadrant = "Basal/Central";
                    break;
                case "abscess":
                    x = 28.0 + jitter;
                    y = 62.0 + jitter;
                    quadrant = "Lower Left";
                    break;
                case "stroke":
                    x = 75.0 + jitter;
                    y = 55.0 + jitter;
                    quadrant = "Right Hemisphere";
                    break;
                case "hemorrhage":
                    x = 42.0 + jitter;
                    y = 48.0 + jitter;
                    quadrant = "Medial";
                    break;
                default:
                    x = 60.0 + jitter;
                    y = 40.0 + jitter;
                    quadrant = "Variable";
                    break;
            }

            yoloHighlight.setQuadrant(quadrant);
            yoloHighlight.setApproximateX(x);
            yoloHighlight.setApproximateY(y);
            yoloHighlight.setRadiusPercent(12.0 + rnd.nextDouble() * 5); // Variable radius
            finding.setRegionHighlight(yoloHighlight);
        }

        return finding;
    }

    @Override
    public String chat(String message, String history) throws Exception {
        String msg = message.toLowerCase();
        if (msg.contains("treatment")) {
            return "Standard treatment protocols for brain tumors usually involve surgical resection followed by radiation and chemotherapy (Stupp protocol for GBM).";
        }
        if (msg.contains("abscess")) {
            return "Brain abscesses typically show restricted diffusion on DWI with low ADC values, which helps distinguish them from necrotic tumors.";
        }
        if (msg.contains("glioma") || msg.contains("malignant")) {
            return "Gliomas are the most common primary malignant brain tumors. WHO Grade IV (GBM) is the most aggressive, with median survival of 14–16 months on Stupp protocol (TMZ + radiation).";
        }
        if (msg.contains("meningioma") || msg.contains("benign")) {
            return "Meningiomas are the most common benign intracranial tumors (90% benign, WHO Grade I). They arise from arachnoid cap cells. Small asymptomatic lesions can be managed with surveillance imaging.";
        }
        if (msg.contains("pituitary")) {
            return "Pituitary adenomas are benign tumors of the anterior pituitary, potentially causing hormonal dysfunction (hyperprolactinemia, acromegaly, Cushing's) or visual field defects by compressing the optic chiasm.";
        }
        return "I am CLINIX.AI — your specialist neuroradiology assistant. Ask me about MRI findings, tumor types, or treatment options.";
    }

    @Override
    public String getName() {
        return "mock";
    }
}
