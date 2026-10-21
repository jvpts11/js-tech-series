/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Collects the outcome of every client test of a shard and writes {@code report.txt}: one {@code PASS} or
 * {@code FAIL} line a test, with the failure's message and the screenshots it took, then a summary line, which reads
 * {@code ALL PASSED} when nothing failed. The same lines go to the log under the {@value #MARK} mark, so a build's log
 * alone tells the story.
 */
public final class ClientTestReport {

    private final List<Result> results = new ArrayList<>();

    /** What every line the client tests write to the log starts with. */
    public static final String MARK = "[CT]";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Records {@code result}, and writes it to the log. */
    public void record(final Result result) {
        results.add(result);
        LOGGER.info("{} {} {}{} ({} ticks)", MARK, result.passed() ? "PASS" : "FAIL", result.name(),
                result.passed() ? "" : ": " + result.message(), result.ticks());
    }

    public int failures() {
        return (int) results.stream().filter(result -> !result.passed()).count();
    }

    public int total() {
        return results.size();
    }

    /** The outcome of every test run so far, in the order they ran. */
    public List<Result> results() {
        return List.copyOf(results);
    }

    /** Writes the report; a failure to write it is logged, never thrown, so the client still shuts down cleanly. */
    public void write(final Path file, final int shard, final int shards) {
        final List<String> lines = new ArrayList<>();
        lines.add("Client tests - shard " + shard + "/" + shards);
        for (final Result result : results) {
            lines.add((result.passed() ? "PASS " : "FAIL ") + result.name()
                    + (result.passed() ? "" : ": " + result.message()) + " [" + result.ticks() + " ticks]");
            for (final String shot : result.screenshots()) {
                lines.add("    screenshot " + shot);
            }
        }
        final String summary = (failures() == 0 ? "ALL PASSED" : failures() + " FAILED") + " - " + total()
                + " tests";
        lines.add(summary);
        LOGGER.info("{} {}", MARK, summary);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOGGER.error("{} could not write the report to {}", MARK, file, e);
        }
    }

    /**
     * The outcome of one test.
     *
     * @param name        its name
     * @param passed      whether it passed
     * @param message     why it failed, empty when it passed
     * @param screenshots the screenshots it took
     * @param ticks       how many ticks it ran
     */
    public record Result(String name, boolean passed, String message, List<String> screenshots, int ticks) {
    }
}
