# CLINIX.AI: Neural MRI Diagnostic Engine

[![Status: Research Use Only](https://img.shields.io/badge/Status-Research_Use_Only-orange.svg)](https://clinixai.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Stack: Spring Boot + ONNX](https://img.shields.io/badge/Stack-Java_17_+_ONNX-red.svg)](https://spring.io/projects/spring-boot)

## Overview

CLINIX.AI is a high-fidelity medical intelligence platform designed to bridge the gap between advanced neural architectures and clinical neuroradiology. The system provides real-time brain MRI analysis, tumor classification, and automated clinical report generation, optimized for both server-side execution and edge-based research environments.

## Technical Abstract

The core diagnostic engine leverages a hybrid inference model:

1. **Computer Vision Layer**: Utilizes specialized **EfficientNet-B0** architectures exported via **ONNX Runtime** for high-performance ROI detection and segmentation.
2. **Linguistic Synthesis**: Employs a zero-hallucination prompting protocol to merge model insights into professional clinical reports, mimicking standard diagnostic laboratory outputs.
3. **Informatics Layer**: A robust **Spring Boot** backend ensures safe handling of medical metadata and history tracking via an H2/SQL relational architecture.

## Key Features

- **Neural Radiomics**: Real-time detection of Meningioma, Glioma, and Pituitary pathologies with confidence scoring.
- **Automated Clinical Reporting**: Generation of physician-ready PDF reports including letterheads, digital stamps, and systematic impressions.
- **Standalone Explorer**: A zero-dependency static deployment for instant research visualization (GitHub Pages ready).
- **Medical Intelligence Chat**: Context-aware AI assistant for radiological follow-up and protocol guidance.

## Architecture & Reproducibility

The project is structured to ensure ease of deployment and scientific verification:

- **`src/main/resources/models`**: Pre-trained ONNX models optimized for medical imaging.
- **`standalone/`**: Independent, browser-based demonstration suite.
- **`Dockerfile`**: Containerized deployment for consistent environment replication.

## Getting Started

### Standard Deployment (Server Mode)

Requires **Java 17** and **Maven**.

```bash
mvn spring-boot:run
```

### Research Mode (Standalone)

Open `standalone/index.html` in any modern browser for immediate interactive visualization.

---
**Disclaimer**: This platform is intended for **research and educational purposes only**. It should not be used as a primary diagnostic tool in clinical settings without proper medical oversight and regulatory approval.
