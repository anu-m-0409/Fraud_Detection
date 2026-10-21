package com.fraud.gatehouse.service;

import com.fraud.gatehouse.model.TransactionRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeatureStoreService {

    public static final List<String> FEATURE_NAMES = List.of(
            "log_amount",
            "amount_vs_usual",
            "txns_last_hour",
            "night_time",
            "foreign_country",
            "unseen_device",
            "merchant_risk",
            "log_distance_km"
    );

    private static class UserProfile {
        double avgSpend;
        Set<String> knownDevices = new HashSet<>();
        String homeCountry = "IN";
        List<OffsetDateTime> recentTxTimestamps = new ArrayList<>();

        UserProfile(double avgSpend, Set<String> devices, String homeCountry) {
            this.avgSpend = avgSpend;
            this.knownDevices.addAll(devices);
            this.homeCountry = homeCountry;
        }
    }

    private final Map<String, UserProfile> userProfiles = new ConcurrentHashMap<>();

    public FeatureStoreService() {
        // Seed demo accounts
        userProfiles.put("priya_sharma", new UserProfile(2100.0, Set.of("priya-iphone"), "IN"));
        userProfiles.put("rohit_verma", new UserProfile(1500.0, Set.of("rohit-redmi"), "IN"));
        userProfiles.put("arjun_mehta", new UserProfile(3500.0, Set.of("arjun-pixel"), "IN"));
    }

    public synchronized Map<String, Double> extractFeatures(TransactionRequest req) {
        OffsetDateTime now = req.getTimestamp() != null ? req.getTimestamp() : OffsetDateTime.now(ZoneId.of("Asia/Kolkata"));
        UserProfile profile = userProfiles.computeIfAbsent(req.getUserId(),
                k -> new UserProfile(req.getAmount(), Set.of(req.getDeviceId()), req.getCountry()));

        // 1. log_amount
        double logAmount = Math.log1p(Math.max(0.0, req.getAmount()));

        // 2. amount_vs_usual
        double amountVsUsual = profile.avgSpend > 0 ? (req.getAmount() / profile.avgSpend) : 1.0;

        // 3. txns_last_hour
        OffsetDateTime oneHourAgo = now.minusHours(1);
        profile.recentTxTimestamps.removeIf(t -> t.isBefore(oneHourAgo));
        double txnsLastHour = profile.recentTxTimestamps.size();

        // 4. night_time (between 01:00 and 05:00)
        int hour = now.getHour();
        double nightTime = (hour >= 1 && hour <= 5) ? 1.0 : 0.0;

        // 5. foreign_country
        double foreignCountry = req.getCountry().equalsIgnoreCase(profile.homeCountry) ? 0.0 : 1.0;

        // 6. unseen_device
        double unseenDevice = profile.knownDevices.contains(req.getDeviceId()) ? 0.0 : 1.0;

        // 7. merchant_risk
        double merchantRisk = computeMerchantRisk(req.getMerchantCategory());

        // 8. log_distance_km
        double logDistance = Math.log1p(Math.max(0.0, req.getDistanceFromHomeKm()));

        Map<String, Double> features = new LinkedHashMap<>();
        features.put("log_amount", logAmount);
        features.put("amount_vs_usual", amountVsUsual);
        features.put("txns_last_hour", txnsLastHour);
        features.put("night_time", nightTime);
        features.put("foreign_country", foreignCountry);
        features.put("unseen_device", unseenDevice);
        features.put("merchant_risk", merchantRisk);
        features.put("log_distance_km", logDistance);

        // Record timestamp for velocity tracking
        profile.recentTxTimestamps.add(now);

        return features;
    }

    public synchronized void recordApproved(TransactionRequest req) {
        UserProfile profile = userProfiles.get(req.getUserId());
        if (profile != null) {
            profile.knownDevices.add(req.getDeviceId());
            // Running weighted average spend
            profile.avgSpend = (profile.avgSpend * 0.85) + (req.getAmount() * 0.15);
        }
    }

    private double computeMerchantRisk(String category) {
        if (category == null) return 0.20;
        return switch (category.toLowerCase()) {
            case "crypto", "gambling" -> 0.92;
            case "gift_cards" -> 0.85;
            case "jewellery", "electronics" -> 0.65;
            case "travel" -> 0.45;
            case "restaurant", "pharmacy", "fuel" -> 0.15;
            case "grocery", "utilities" -> 0.05;
            default -> 0.25;
        };
    }
}
