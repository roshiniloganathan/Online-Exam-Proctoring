package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service handling persistent audit logging and report generation.
 * 
 * Concepts Applied:
 * - Data serialization (CSV & JSON format exports)
 * - Safe file I/O with try-with-resources (AutoCloseable streams)
 * - Statistical aggregation & compliance summary reporting
 */
public class ReportExportService {

    private static final String DEFAULT_LOG_DIR = "reports";
    private static final DateTimeFormatter FILE_TS_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ISO_INSTANT;

    public ReportExportService() {
        ensureDirectoryExists(DEFAULT_LOG_DIR);
    }

    private void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Exports violation history to a standard CSV file for spreadsheet analysis.
     *
     * @param history List of violation records
     * @param examId Unique exam or candidate identifier
     * @return File object of the written CSV
     */
    public File exportToCsv(List<ViolationRecord> history, String examId) throws IOException {
        String timestamp = FILE_TS_FORMATTER.format(Instant.now());
        String filename = String.format("%s/audit_%s_%s.csv", DEFAULT_LOG_DIR, examId, timestamp);
        File csvFile = new File(filename);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            // Write standard CSV header
            writer.write("Timestamp,ExamId,ViolationType,IsViolation,DetectedFaceCount,Details");
            writer.newLine();

            for (ViolationRecord record : history) {
                // Escape commas or quotes in details
                String cleanDetails = record.getDetails().replace("\"", "\"\"");
                writer.write(String.format("\"%s\",\"%s\",\"%s\",%b,%d,\"%s\"",
                        ISO_FORMATTER.format(record.getTimestamp()),
                        examId,
                        record.getStatus().name(),
                        record.getStatus().isViolation(),
                        record.getDetectedFaceCount(),
                        cleanDetails));
                writer.newLine();
            }
        }
        System.out.println("[EXPORT] CSV Audit log generated at: " + csvFile.getAbsolutePath());
        return csvFile;
    }

    /**
     * Exports violation history to structured JSON format.
     */
    public File exportToJson(List<ViolationRecord> history, String examId) throws IOException {
        String timestamp = FILE_TS_FORMATTER.format(Instant.now());
        String filename = String.format("%s/audit_%s_%s.json", DEFAULT_LOG_DIR, examId, timestamp);
        File jsonFile = new File(filename);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"examId\": \"").append(examId).append("\",\n");
        sb.append("  \"generatedAt\": \"").append(ISO_FORMATTER.format(Instant.now())).append("\",\n");
        sb.append("  \"totalViolations\": ").append(history.size()).append(",\n");
        sb.append("  \"violations\": [\n");

        for (int i = 0; i < history.size(); i++) {
            ViolationRecord r = history.get(i);
            sb.append("    {\n");
            sb.append("      \"timestamp\": \"").append(ISO_FORMATTER.format(r.getTimestamp())).append("\",\n");
            sb.append("      \"status\": \"").append(r.getStatus().name()).append("\",\n");
            sb.append("      \"detectedFaces\": ").append(r.getDetectedFaceCount()).append(",\n");
            sb.append("      \"description\": \"").append(r.getStatus().getDescription()).append("\",\n");
            sb.append("      \"details\": \"").append(r.getDetails().replace("\"", "\\\"")).append("\"\n");
            sb.append("    }").append(i < history.size() - 1 ? "," : "").append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile))) {
            writer.write(sb.toString());
        }
        System.out.println("[EXPORT] JSON Audit log generated at: " + jsonFile.getAbsolutePath());
        return jsonFile;
    }

    /**
     * Generates a readable compliance summary report with incident statistics.
     */
    public String generateSummaryReport(List<ViolationRecord> history, String examId) {
        Map<DetectionStatus, Integer> counts = new EnumMap<>(DetectionStatus.class);
        for (DetectionStatus s : DetectionStatus.values()) {
            counts.put(s, 0);
        }

        for (ViolationRecord r : history) {
            counts.put(r.getStatus(), counts.get(r.getStatus()) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("===================================================\n");
        sb.append("      EXAMINATION PROCTORING COMPLIANCE REPORT      \n");
        sb.append("===================================================\n");
        sb.append("Exam ID            : ").append(examId).append("\n");
        sb.append("Report Timestamp   : ").append(ISO_FORMATTER.format(Instant.now())).append("\n");
        sb.append("Total Infractions  : ").append(history.size()).append("\n");
        sb.append("---------------------------------------------------\n");
        sb.append("BREAKDOWN BY VIOLATION CATEGORY:\n");
        sb.append(String.format("  - Candidate Absence (0 faces) : %d\n", counts.get(DetectionStatus.NO_FACE_DETECTED)));
        sb.append(String.format("  - Collusion (2+ faces)        : %d\n", counts.get(DetectionStatus.MULTIPLE_FACES_DETECTED)));
        sb.append(String.format("  - Suspicious Posture / Scale  : %d\n", counts.get(DetectionStatus.SUSPICIOUS_POSTURE)));
        sb.append("---------------------------------------------------\n");

        if (history.size() == 0) {
            sb.append("COMPLIANCE RULING: PASSED (Clean Session - No Infractions)\n");
        } else if (history.size() <= 4) {
            sb.append("COMPLIANCE RULING: FLAGGED (Minor Infractions - Manual Review Recommended)\n");
        } else {
            sb.append("COMPLIANCE RULING: REJECTED (Disqualified - Exceeded Infraction Quota)\n");
        }
        sb.append("===================================================\n");

        return sb.toString();
    }
}
