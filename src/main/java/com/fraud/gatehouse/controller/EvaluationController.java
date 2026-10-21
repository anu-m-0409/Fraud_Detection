package com.fraud.gatehouse.controller;

import com.fraud.gatehouse.model.*;
import com.fraud.gatehouse.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class EvaluationController {

    private final FeatureStoreService featureStore;
    private final InferenceService inferenceService;
    private final RulesEngine rulesEngine;
    private final ExplainerService explainerService;

    @Value("${fraud.review-threshold:0.40}")
    private double reviewThreshold;

    @Value("${fraud.decline-threshold:0.75}")
    private double declineThreshold;

    private final Deque<EvaluationResponse> ledger = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalCount = new AtomicLong();
    private final AtomicLong approvedCount = new AtomicLong();
    private final AtomicLong reviewCount = new AtomicLong();
    private final AtomicLong declinedCount = new AtomicLong();
    private final List<Double> latencies = Collections.synchronizedList(new ArrayList<>());

    public EvaluationController(FeatureStoreService featureStore,
                                InferenceService inferenceService,
                                RulesEngine rulesEngine,
                                ExplainerService explainerService) {
        this.featureStore = featureStore;
        this.inferenceService = inferenceService;
        this.rulesEngine = rulesEngine;
        this.explainerService = explainerService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluateTransaction(@Valid @RequestBody TransactionRequest req) {
        long start = System.nanoTime();

        // 1. Feature Engineering
        Map<String, Double> features = featureStore.extractFeatures(req);

        // 2. Machine Learning Inference (ONNX / Tabular Ensemble)
        double modelScore = inferenceService.predictRisk(features);

        // 3. Deterministic Compliance & Behavioral Rules
        List<RuleHit> ruleHits = rulesEngine.evaluateRules(req, features);

        // 4. Score Aggregation
        double finalRisk = rulesEngine.combineRisk(modelScore, ruleHits);

        // 5. Decision Triaging
        String decision;
        if (finalRisk >= declineThreshold) {
            decision = "DECLINE";
            declinedCount.incrementAndGet();
        } else if (finalRisk >= reviewThreshold) {
            decision = "REVIEW";
            reviewCount.incrementAndGet();
        } else {
            decision = "APPROVE";
            approvedCount.incrementAndGet();
            featureStore.recordApproved(req);
        }

        totalCount.incrementAndGet();
        long elapsedNanos = System.nanoTime() - start;
        double latencyMs = elapsedNanos / 1_000_000.0;
        latencies.add(latencyMs);

        // 6. Explainable AI (SHAP attributions & narrative)
        Map<String, Double> shap = inferenceService.computeShapAttributions(features, finalRisk);
        List<String> reasons = explainerService.buildHumanReadableReasons(features, ruleHits, finalRisk);

        EvaluationResponse res = new EvaluationResponse();
        res.setTransactionId("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        res.setEvaluatedAt(req.getTimestamp() != null ? req.getTimestamp() : OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        res.setUserId(req.getUserId());
        res.setAmount(req.getAmount());
        res.setMerchantCategory(req.getMerchantCategory());
        res.setCountry(req.getCountry());
        res.setDecision(decision);
        res.setRiskScore(Math.round(finalRisk * 1000.0) / 1000.0);
        res.setModelScore(Math.round(modelScore * 1000.0) / 1000.0);
        res.setLatencyMs(Math.round(latencyMs * 10.0) / 10.0);
        res.setRuleHits(ruleHits);
        res.setReasons(reasons);
        res.setFeatures(features);
        res.setShapContributions(shap);

        // Append to tape (keep max 100 in memory)
        ledger.addFirst(res);
        while (ledger.size() > 100) {
            ledger.removeLast();
        }

        return ResponseEntity.ok(res);
    }

    @GetMapping("/recent")
    public List<EvaluationResponse> getRecentTransactions(@RequestParam(defaultValue = "15") int limit) {
        return ledger.stream().limit(limit).toList();
    }

    @GetMapping("/stats")
    public StatsResponse getStats() {
        StatsResponse s = new StatsResponse();
        s.setTotal(totalCount.get());
        s.setApproved(approvedCount.get());
        s.setReview(reviewCount.get());
        s.setDeclined(declinedCount.get());
        s.setReviewThreshold(reviewThreshold);
        s.setDeclineThreshold(declineThreshold);

        double avgLat = 0.0;
        synchronized (latencies) {
            if (!latencies.isEmpty()) {
                avgLat = latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }
        }
        s.setAvgLatencyMs(Math.round(avgLat * 10.0) / 10.0);
        return s;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> errResponse = new HashMap<>();
        errResponse.put("error", "Validation failed");
        errResponse.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(errResponse);
    }
}
