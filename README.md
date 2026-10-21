# Gatehouse: Real-Time Payment Fraud Screening & Explainable AI Audit Engine

[![Java 17](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-1.17.3-005CED?logo=onnx&logoColor=white)](https://onnxruntime.ai/)
[![Docker Ready](https://img.shields.io/badge/Docker-Multi--Stage-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

> **Hackathon Pitch Statement:**
> Gatehouse is an enterprise-grade, sub-5 millisecond hybrid fraud screening engine that bridges high-throughput machine learning inference with deterministic AML/FATF compliance rules, SHAP feature attributions, and instant GenAI regulatory audit reports.

---

## 🌟 Hackathon Winning Features

1. **Dual-Mode ML Serving Architecture:**
   - **Zero-Friction Startup:** Ships with an embedded, calibrated ensemble tabular engine in pure Java that runs immediately (`mvn spring-boot:run`) without needing external runtime dependencies or crashing if native C++ libraries are absent.
   - **Full ONNX Integration:** Supports hot-swapping any `.onnx` deep learning or gradient-boosted model (`fraud_detector.onnx`) directly into Microsoft ONNX Runtime with zero code changes.
2. **Explainable AI (XAI) with SHAP Attributions:**
   - Instead of a black-box percentage, every score breaks down the top mathematical drivers (Spend baseline deviation, velocity burst, hardware anonymity, nocturnal flags).
3. **Automated GenAI Compliance Auditor (`/api/audit/generate`):**
   - Synthesizes complex ML telemetry into human-readable regulatory dossiers with cryptographic SHA-256 audit trail hashes for FinTech fraud analysts.
4. **Interactive Simulation Studio:**
   - Test adversarial attacks live in the UI: rapid bot velocity bursts, international SIM swaps, nocturnal card drains, and OFAC sanctions enforcement.

---

## ⚡ Quick Start

### Prerequisites
- JDK 17 or higher
- Maven 3.8+ (or Docker)

### Run Locally (One Command)
```bash
mvn clean spring-boot:run
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

---

## 📡 API Endpoints

### 1. Evaluate Transaction
`POST /api/evaluate`
```json
{
  "userId": "rohit_verma",
  "amount": 28000,
  "merchantCategory": "gift_cards",
  "country": "NG",
  "deviceId": "unknown-device-77",
  "distanceFromHomeKm": 5200,
  "timestamp": "2026-10-21T03:10:00+05:30"
}
```

### 2. Generate GenAI Audit Dossier
`POST /api/audit/generate`
Passes the evaluation object directly and outputs an executive summary, mandated regulatory actions, and risk driver weights.

### 3. Telemetry & Stats
`GET /api/stats`
Returns total transactions screened, approval rates, and moving average latency (typically 1–4 ms).

---

## 🧠 Retraining the Model (`tools/build_model.py`)
To train a fresh classifier on your own dataset or export to ONNX:
```bash
pip install numpy scikit-learn skl2onnx onnx
python tools/build_model.py
```
This generates and places `fraud_detector.onnx` directly into `src/main/resources/`.

---

## 🚢 Cloud Deployment (Render / Docker)
Deploy with the included multi-stage `Dockerfile` and `render.yaml`:
```bash
docker build -t gatehouse-fraud-engine .
docker run -p 8080:8080 gatehouse-fraud-engine
```
