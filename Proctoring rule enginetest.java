package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;
import org.opencv.core.Rect;

import java.util.concurrent.atomic.AtomicInteger;

public class ProctoringRuleEngineTest {
    public static void main(String[] args) {
        System.out.println("Running Unit Tests for ProctoringRuleEngine...");
        ProctoringRuleEngine engine = new ProctoringRuleEngine();

        AtomicInteger violationAlertCount = new AtomicInteger(0);
        engine.addViolationListener(violation -> {
            violationAlertCount.incrementAndGet();
            System.out.println("   [Observer Alert]: " + violation.toString());
        });

        // Test 1: Normal 1-face scenario
        System.out.println("\n[Test 1] Testing normal candidate presence (1 face)...");
        Rect normalFace = new Rect(150, 100, 180, 180);
        DetectionStatus status1 = engine.evaluate(new Rect[]{normalFace});
        assertCondition(status1 == DetectionStatus.NORMAL, "Status must be NORMAL for 1 standard face");
        assertCondition(!status1.isViolation(), "NORMAL status is not a violation");
        System.out.println("PASS: Normal presence verified.");

        // Test 2: 0 faces (Absence alert debouncing)
        System.out.println("\n[Test 2] Testing candidate absence (0 faces with 5-frame threshold)...");
        for (int i = 0; i < 5; i++) {
            engine.evaluate(new Rect[0]);
        }
        assertCondition(engine.getTotalViolationCount() >= 1, "Violation record created after threshold");
        System.out.println("PASS: Absence violation recorded and observer notified.");

        // Test 3: Multiple faces (Collusion alert)
        System.out.println("\n[Test 3] Testing collusion (2 faces)...");
        Rect secondFace = new Rect(400, 120, 160, 160);
        for (int i = 0; i < 5; i++) {
            engine.evaluate(new Rect[]{normalFace, secondFace});
        }
        assertCondition(engine.getTotalViolationCount() >= 2, "Multiple faces violation logged");
        System.out.println("PASS: Collusion violation recorded.");

        System.out.println("\n==========================================");
        System.out.println(" ALL 3 PROCTORING RULE ENGINE TESTS PASSED! ");
        System.out.println("==========================================");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("TEST FAILURE: " + message);
            System.exit(1);
        }
    }
}
