package com.fraud.gatehouse.controller;

import com.fraud.gatehouse.model.AuditReportResponse;
import com.fraud.gatehouse.model.EvaluationResponse;
import com.fraud.gatehouse.service.ExplainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final ExplainerService explainerService;

    public AuditController(ExplainerService explainerService) {
        this.explainerService = explainerService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AuditReportResponse> generateAudit(@RequestBody EvaluationResponse res) {
        AuditReportResponse report = explainerService.generateGenAiAuditReport(res);
        return ResponseEntity.ok(report);
    }
}
