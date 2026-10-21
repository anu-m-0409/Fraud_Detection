package com.fraud.gatehouse;

import com.fraud.gatehouse.model.TransactionRequest;
import com.fraud.gatehouse.model.EvaluationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatehouseApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSanctionedCountryDecline() {
        TransactionRequest req = new TransactionRequest();
        req.setUserId("test_user");
        req.setAmount(1500.0);
        req.setCountry("KP");
        req.setDeviceId("test_phone");
        req.setMerchantCategory("grocery");

        ResponseEntity<EvaluationResponse> response = restTemplate.postForEntity("/api/evaluate", req, EvaluationResponse.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("DECLINE", response.getBody().getDecision());
        assertEquals(1.0, response.getBody().getRiskScore());
    }
}
