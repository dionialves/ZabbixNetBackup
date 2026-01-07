package com.dionialves.core.util;

public class ProgressBar {
    private final String taskName;
    private final int total;
    private int current;
    private long startTime;
    private String extraMessage = "";

    public ProgressBar(String taskName, int total) {
        this.taskName = taskName;
        this.total = total;
        this.current = 0;
        this.startTime = System.currentTimeMillis();
    }

    public void step() {
        current++;
        update();
    }

    public void setExtraMessage(String message) {
        this.extraMessage = message;
    }

    private void update() {
        int percentage = (current * 100) / total;
        int barLength = 40;
        int filled = (current * barLength) / total;

        StringBuilder bar = new StringBuilder();
        bar.append("\r"); // Retorna ao início da linha
        bar.append(taskName).append(": ");
        bar.append(percentage).append("% ");
        bar.append("[");

        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }

        bar.append("] ");
        bar.append(current).append("/").append(total);

        if (!extraMessage.isEmpty()) {
            bar.append(" - ").append(extraMessage);
        }

        // Adicionar tempo estimado
        if (current > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            long estimatedTotal = (elapsed * total) / current;
            long remaining = estimatedTotal - elapsed;

            if (remaining > 0) {
                bar.append(" (ETA: ").append(formatTime(remaining)).append(")");
            }
        }

        // Limpa o resto da linha
        bar.append("   ");

        System.out.print(bar.toString());
        System.out.flush();

        // Nova linha ao terminar
        if (current >= total) {
            System.out.println();
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    public void close() {
        if (current < total) {
            current = total;
            update();
        }
    }
}