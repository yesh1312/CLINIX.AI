package com.medai.braintumor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TumorFinding {
    private String imageQuality;
    private String imageQualityNotes;
    private boolean tumorDetected;
    private int confidenceScore;
    private Details tumorFindings;
    private String likelyDiagnosis;
    private List<String> differentialDiagnoses;
    private List<String> keyMRIFeatures;
    private Highlight regionHighlight;
    private String clinicalRecommendation;
    private String urgencyLevel;
    private String additionalNotes;
    private String disclaimer;

    public TumorFinding() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        private String location;
        private String hemisphere;
        private String lobe;
        private String estimatedSize;
        private String morphology;
        private String intensityPattern;
        private String perilesionalEdema;
        private String massEffect;
        private String midlineShift;
        private String enhancementPattern;

        // Getters and Setters
        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getHemisphere() {
            return hemisphere;
        }

        public void setHemisphere(String hemisphere) {
            this.hemisphere = hemisphere;
        }

        public String getLobe() {
            return lobe;
        }

        public void setLobe(String lobe) {
            this.lobe = lobe;
        }

        public String getEstimatedSize() {
            return estimatedSize;
        }

        public void setEstimatedSize(String estimatedSize) {
            this.estimatedSize = estimatedSize;
        }

        public String getMorphology() {
            return morphology;
        }

        public void setMorphology(String morphology) {
            this.morphology = morphology;
        }

        public String getIntensityPattern() {
            return intensityPattern;
        }

        public void setIntensityPattern(String intensityPattern) {
            this.intensityPattern = intensityPattern;
        }

        public String getPerilesionalEdema() {
            return perilesionalEdema;
        }

        public void setPerilesionalEdema(String perilesionalEdema) {
            this.perilesionalEdema = perilesionalEdema;
        }

        public String getMassEffect() {
            return massEffect;
        }

        public void setMassEffect(String massEffect) {
            this.massEffect = massEffect;
        }

        public String getMidlineShift() {
            return midlineShift;
        }

        public void setMidlineShift(String midlineShift) {
            this.midlineShift = midlineShift;
        }

        public String getEnhancementPattern() {
            return enhancementPattern;
        }

        public void setEnhancementPattern(String enhancementPattern) {
            this.enhancementPattern = enhancementPattern;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Highlight {
        private String description;
        private String quadrant;
        private double approximateX;
        private double approximateY;
        private double radiusPercent;

        // Getters and Setters
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getQuadrant() {
            return quadrant;
        }

        public void setQuadrant(String quadrant) {
            this.quadrant = quadrant;
        }

        public double getApproximateX() {
            return approximateX;
        }

        public void setApproximateX(double approximateX) {
            this.approximateX = approximateX;
        }

        public double getApproximateY() {
            return approximateY;
        }

        public void setApproximateY(double approximateY) {
            this.approximateY = approximateY;
        }

        public double getRadiusPercent() {
            return radiusPercent;
        }

        public void setRadiusPercent(double radiusPercent) {
            this.radiusPercent = radiusPercent;
        }
    }

    // Getters and Setters
    public String getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(String imageQuality) {
        this.imageQuality = imageQuality;
    }

    public String getImageQualityNotes() {
        return imageQualityNotes;
    }

    public void setImageQualityNotes(String imageQualityNotes) {
        this.imageQualityNotes = imageQualityNotes;
    }

    public boolean isTumorDetected() {
        return tumorDetected;
    }

    public void setTumorDetected(boolean tumorDetected) {
        this.tumorDetected = tumorDetected;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Details getTumorFindings() {
        return tumorFindings;
    }

    public void setTumorFindings(Details tumorFindings) {
        this.tumorFindings = tumorFindings;
    }

    public String getLikelyDiagnosis() {
        return likelyDiagnosis;
    }

    public void setLikelyDiagnosis(String likelyDiagnosis) {
        this.likelyDiagnosis = likelyDiagnosis;
    }

    public List<String> getDifferentialDiagnoses() {
        return differentialDiagnoses;
    }

    public void setDifferentialDiagnoses(List<String> differentialDiagnoses) {
        this.differentialDiagnoses = differentialDiagnoses;
    }

    public List<String> getKeyMRIFeatures() {
        return keyMRIFeatures;
    }

    public void setKeyMRIFeatures(List<String> keyMRIFeatures) {
        this.keyMRIFeatures = keyMRIFeatures;
    }

    public Highlight getRegionHighlight() {
        return regionHighlight;
    }

    public void setRegionHighlight(Highlight regionHighlight) {
        this.regionHighlight = regionHighlight;
    }

    public String getClinicalRecommendation() {
        return clinicalRecommendation;
    }

    public void setClinicalRecommendation(String clinicalRecommendation) {
        this.clinicalRecommendation = clinicalRecommendation;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
