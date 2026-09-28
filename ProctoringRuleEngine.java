package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Pure business logic engine that evaluates face bounding boxes against proctoring policies.
 * 
 * Architectural Concept:
 * Separating domain evaluation from computer vision capture and GUI rendering.
 * Allows effortless unit testing and listener subscription.
 */
public class ProctoringRuleEngine {

    // Thresholds
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;
    private static final int MIN_REASONABLE_FACE_WIDTH = 75; // If too small, candidate is too far
    private static final int MAX_REASONABLE_FACE_WIDTH = 380; // If too large, candidate is crowding lens

    // Concurrency-safe event listeners (Observer Pattern)
    private final List<Consumer<ViolationRecord>> violationListeners = new CopyOnWriteArrayList<>();
    private final List<ViolationRecord> history = new CopyOnWriteArrayList<>();

    // Debouncing / Grace period state to avoid instant false alerts on single-frame blinks
    private int consecutiveNoFaceFrames = 0;
    private int consecutiveMultiFaceFrames = 0;
    private static final int ALERT_FRAME_THRESHOLD = 5; // ~150ms at 30fps

    public ProctoringRuleEngine() {
    }

    /**
     * Registers a listener to be notified when a proctoring violation occurs.
     */
    public void addViolationListener(Consumer<ViolationRecord> listener) {
        if (listener != null) {
            violationListeners.add(listener);
        }
    }

    /**
     * Evaluates detected faces against proctoring compliance rules.
     * 
     * @param detectedFaces Array of Rect bounding boxes found by vision model
     * @return Evaluated DetectionStatus for the current frame
     */
    public DetectionStatus evaluate(Rect[] detectedFaces) {
        if (detectedFaces == null || detectedFaces.length == 0) {
            consecutiveMultiFaceFrames = 0;
            consecutiveNoFaceFrames++;

            if (consecutiveNoFaceFrames >= ALERT_FRAME_THRESHOLD) {
                recordViolation(DetectionStatus.NO_FACE_DETECTED, 0,
                        "Candidate not detected in camera frame for > " + consecutiveNoFaceFrames + " frames.");
                return DetectionStatus.NO_FACE_DETECTED;
            }
            return DetectionStatus.NO_FACE_DETECTED;
        }

        if (detectedFaces.length > 1) {
            consecutiveNoFaceFrames = 0;
            consecutiveMultiFaceFrames++;

            if (consecutiveMultiFaceFrames >= ALERT_FRAME_THRESHOLD) {
                recordViolation(DetectionStatus.MULTIPLE_FACES_DETECTED, detectedFaces.length,
                        "Multiple people (" + detectedFaces.length + ") detected simultaneously in examination view.");
                return DetectionStatus.MULTIPLE_FACES_DETECTED;
            }
            return DetectionStatus.MULTIPLE_FACES_DETECTED;
        }

        // Exactly 1 face detected!
        consecutiveNoFaceFrames = 0;
        consecutiveMultiFaceFrames = 0;

        Rect candidateFace = detectedFaces[0];

        // Check if candidate is too far away or suspicious scale
        if (candidateFace.width < MIN_REASONABLE_FACE_WIDTH) {
            return DetectionStatus.SUSPICIOUS_POSTURE;
        }

        return DetectionStatus.NORMAL;
    }

    private void recordViolation(DetectionStatus status, int faceCount, String details) {
        ViolationRecord record = new ViolationRecord(status, faceCount, details);
        history.add(record);

        // Notify all registered observers
        for (Consumer<ViolationRecord> listener : violationListeners) {
            try {
                listener.accept(record);
            } catch (Exception e) {
                System.err.println("[WARN] Violation listener error: " + e.getMessage());
            }
        }
    }

    public List<ViolationRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public int getTotalViolationCount() {
        return history.size();
    }

    public void clearHistory() {
        history.clear();
        consecutiveNoFaceFrames = 0;
        consecutiveMultiFaceFrames = 0;
    }
}
