package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;

public class AlertServiceTest {
    public static void main(String[] args) {
        System.out.println("Testing AlertService rate limiting and event processing...");
        AlertService service = new AlertService();

        // Check 1: Initial beep should succeed
        boolean beep1 = service.triggerAudibleAlert();
        assertCondition(beep1, "First beep should be allowed immediately");
        System.out.println("PASS: Initial audio alert triggered.");

        // Check 2: Immediate follow-up beep should be throttled (< 2000ms)
        boolean beep2 = service.triggerAudibleAlert();
        assertCondition(!beep2, "Second immediate beep should be throttled by rate limiter");
        System.out.println("PASS: Audible rate limiter prevented audio spamming.");

        // Check 3: Violation handling integration
        ViolationRecord sampleViolation = new ViolationRecord(
                DetectionStatus.MULTIPLE_FACES_DETECTED,
                2,
                "Unauthorized person entered frame"
        );
        service.onViolation(sampleViolation);
        System.out.println("PASS: onViolation handled record cleanly.");

        // Check 4: Mute functionality
        service.setAudioEnabled(false);
        boolean beepMuted = service.triggerAudibleAlert();
        assertCondition(!beepMuted, "Muted service must not trigger beeps");
        System.out.println("PASS: Audio mute verified.");

        System.out.println("\nALL ALERT SERVICE UNIT TESTS PASSED!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("TEST FAILURE: " + message);
            System.exit(1);
        }
    }
}
