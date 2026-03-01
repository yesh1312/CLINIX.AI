# CLINIX.AI - Java Website

This is a modern, premium medical AI platform website built with Java (Spring Boot), Thymeleaf, and CSS.

## Features

- **Modern UI**: Glassmorphism design with smooth animations.
- **Spring Boot Backend**: Robust Java backend for serving content and handling logic.
- **Responsive Design**: Works perfectly on desktop and mobile.
- **AI Lab Simulation**: Interactive laboratory interface for medical image analysis.

## Prerequisites

- **Java 8 or higher** (Detected: Java 1.8)
- **Apache Maven** (If not installed, you can use your IDE's built-in Maven support)

## How to Run

### Option 1: Using an IDE (Recommended)

1. Open this folder (`CLINIX.AI`) in **IntelliJ IDEA**, **Eclipse**, or **VS Code**.
2. Wait for the IDE to sync the Maven dependencies defined in `pom.xml`.
3. Locate `src/main/java/com/clinixai/ClinixAiApplication.java`.
4. Right-click the file and select **Run 'ClinixAiApplication'**.

### Option 2: Using Command Line (Maven required)

1. Open a terminal in the project root.
2. Run the following command:

   ```bash
   mvn spring-boot:run
   ```

### Accessing the Site

Once the application starts, open your browser and go to:
[http://localhost:8080](http://localhost:8080)

## Project Structure

- `src/main/java`: Java backend code.
- `src/main/resources/templates`: HTML pages (index, research, lab).
- `src/main/resources/static`: Static assets (CSS, JS).
- `pom.xml`: Project dependencies and configuration.
