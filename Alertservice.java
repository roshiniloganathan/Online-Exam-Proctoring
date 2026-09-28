

package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;

import java.awt.Toolkit;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service handling audible alerts and rate-limited warning dispatch.
 * 
 * Architectural Concepts:
 * - Rate limiting / Throttling (avoids continuous speaker buzzing)
 * - Thread-safe atomic timestamps
 * - Decoupled audio trigger with headless fallback
 */
public class AlertService {

    // Maximum 1 audio beep every 2000 milliseconds to avoid deafening the user
    private static final long BEEP_COOLDOWN_MS = 2000;
    private final AtomicLong lastBeepTimestamp = new AtomicLong(0);
    private final AtomicBoolean audioEnabled = new AtomicBoolean(true);

    public AlertService() {
    }

    /**
     * Triggers an audible warning beep if cooldown has elapsed and audio is enabled.
     */
    public boolean triggerAudibleAlert() {
        if (!audioEnabled.get()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = lastBeepTimestamp.get();

        if (now - last >= BEEP_COOLDOWN_MS) {
            if (lastBeepTimestamp.compareAndSet(last, now)) {
                // Background execution so audio subsystem doesn't hitch video frames
                new Thread(() -> {
                    try {
                        Toolkit.getDefaultToolkit().beep();
                        System.out.println("[AUDIBLE ALERT] Beep triggered at " + Instant.now());
                    } catch (Throwable t) {
                        // In headless Linux environments without sound card, log smoothly
                        System.out.println("[AUDIBLE ALERT SIMULATED] Beep triggered at " + Instant.now());
                    }
                }, "Audio-Beep-Thread").start();
                return true;
            }
        }
        return false;
    }

    /**
     * Handles violation event dispatched from rule engine.
     */
    public void onViolation(ViolationRecord record) {
        if (record != null && record.getStatus().isViolation()) {
            triggerAudibleAlert();
        }
    }

    public boolean isAudioEnabled() {
        return audioEnabled.get();
    }

    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled.set(enabled);
    }
}
