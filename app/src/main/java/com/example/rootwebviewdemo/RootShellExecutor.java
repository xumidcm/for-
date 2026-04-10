package com.example.rootwebviewdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class RootShellExecutor {

    private RootShellExecutor() {
    }

    public static CommandResult execute(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandResult("", "Command is empty", -1);
        }

        String normalizedCommand = stripWrappingQuotes(command.trim());

        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", normalizedCommand).start();
            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            int statusCode = process.waitFor();
            return new CommandResult(stdout, stderr, statusCode);
        } catch (Exception e) {
            return new CommandResult("", e.getMessage() == null ? "Unknown error" : e.getMessage(), -1);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String stripWrappingQuotes(String command) {
        if (command.length() < 2) {
            return command;
        }
        char first = command.charAt(0);
        char last = command.charAt(command.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return command.substring(1, command.length() - 1);
        }
        return command;
    }

    private static String readAll(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    builder.append('\n');
                }
                builder.append(line);
                first = false;
            }
        }
        return builder.toString();
    }

    public static final class CommandResult {
        public final String stdout;
        public final String stderr;
        public final int statusCode;

        public CommandResult(String stdout, String stderr, int statusCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.statusCode = statusCode;
        }
    }
}
