package com.proctoring.service;

import com.proctoring.model.DetectionStatus;
import com.proctoring.model.ViolationRecord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ReportExportServiceTest {
    public static void main(String[] args) throws IOException {
        System.out.println("Testing ReportExportService (CSV, JSON, Compliance Report)...");
        ReportExportService exportService = new ReportExportService();

        List<ViolationRecord> sampleHistory = new ArrayList<>();
        sampleHistory.add(new ViolationRecord(
                DetectionStatus.NO_FACE_DETECTED,
                0,
                "Candidate absent for > 5 frames"
        ));
        sampleHistory.add(new ViolationRecord(
                DetectionStatus.MULTIPLE_FACES_DETECTED,
                2,
                "Second person detected in frame"
        ));

        String examId = "EXAM-CS101-USER13";

        // 1. Test CSV Export
        File csvFile = exportService.exportToCsv(sampleHistory, examId);
        assertCondition(csvFile.exists(), "CSV file must exist on disk");
        assertCondition(csvFile.length() > 0, "CSV file must not be empty");
        System.out.println("PASS: CSV file successfully written (" + csvFile.length() + " bytes).");

        // 2. Test JSON Export
        File jsonFile = exportService.exportToJson(sampleHistory, examId);
        assertCondition(jsonFile.exists(), "JSON file must exist on disk");
        String jsonContent = Files.readString(jsonFile.toPath());
        assertCondition(jsonContent.contains(examId), "JSON must contain the examId");
        assertCondition(jsonContent.contains("MULTIPLE_FACES_DETECTED"), "JSON must record violation type");
        System.out.println("PASS: JSON file successfully written (" + jsonFile.length() + " bytes).");

        // 3. Test Summary Report Generation
        String summary = exportService.generateSummaryReport(sampleHistory, examId);
        assertCondition(summary.contains("Candidate Absence (0 faces) : 1"), "Summary must reflect category count");
        assertCondition(summary.contains("Collusion (2+ faces)        : 1"), "Summary must reflect category count");
        System.out.println("PASS: Summary report generated correctly:\n" + summary);

        System.out.println("ALL REPORT EXPORT TESTS PASSED!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("TEST FAILURE: " + message);
            System.exit(1);
        }
    }
}
