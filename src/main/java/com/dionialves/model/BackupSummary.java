package com.dionialves.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BackupSummary {
    private int total = 0;
    private int successful = 0;
    private int failed = 0;
    private final List<BackupResult> failures = new ArrayList<>();
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    public BackupSummary() {
        this.startTime = LocalDateTime.now();
    }

    public void add(BackupResult result) {
        total++;
        if (result.isSuccess()) {
            successful++;
        } else {
            failed++;
            failures.add(result);
        }
    }

    public void finish() {
        this.endTime = LocalDateTime.now();
    }

    public int getTotal() {
        return total;
    }

    public int getSuccessful() {
        return successful;
    }

    public int getFailed() {
        return failed;
    }

    public List<BackupResult> getFailures() {
        return new ArrayList<>(failures);
    }

    public Duration getDuration() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }

    public void print() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           BACKUP SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println("Total devices: " + total);
        System.out.println("Successful: " + successful + " (" + getSuccessPercentage() + "%)");
        System.out.println("Failed: " + failed);

        Duration duration = getDuration();
        long seconds = duration.getSeconds();
        System.out.println("Total time: " + formatDuration(seconds));

        if (!failures.isEmpty()) {
            System.out.println("\n" + "-".repeat(50));
            System.out.println("Failed devices:");
            System.out.println("-".repeat(50));
            for (BackupResult result : failures) {
                System.out.println("  • " + result.getIp() + "- Reason: " + result.getMessage());
            }
        }

        System.out.println("=".repeat(50));
    }

    private int getSuccessPercentage() {
        if (total == 0) return 0;
        return (successful * 100) / total;
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}