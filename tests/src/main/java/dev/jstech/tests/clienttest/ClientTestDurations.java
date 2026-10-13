/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;

/**
 * How many ticks each client test took the last time it passed, kept in {@code durations.txt} beside the shards'
 * game directories, so the next run can share the suite out by it and its shards end together. A run updates only
 * the tests it ran: one test run alone leaves the others as they were. The file is the machine's own, never part of
 * the repository; a run without it (a fresh checkout, the CI) shares the suite out round-robin.
 */
final class ClientTestDurations {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE = "durations.txt";
    /** Several shards finish at about the same time; each holds this while it reads and rewrites the file. */
    private static final String LOCK = "durations.lock";

    private ClientTestDurations() {
    }

    /** The ticks each test took when it last passed, by its name; empty when nothing was ever measured. */
    static Map<String, Integer> read(final Path runDirectory) {
        final Path file = runDirectory.resolve(FILE);
        final Map<String, Integer> ticks = new TreeMap<>();
        if (!Files.isRegularFile(file)) {
            return ticks;
        }
        try {
            for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                final int space = line.indexOf(' ');
                if (space > 0) {
                    ticks.put(line.substring(space + 1).trim(), Integer.parseInt(line.substring(0, space)));
                }
            }
        } catch (final IOException | NumberFormatException e) {
            // A file that cannot be read only costs the balance: the suite is shared out round-robin instead.
            LOGGER.warn("[JSC-CT] the client test durations could not be read from {}", file, e);
            return new TreeMap<>();
        }
        return ticks;
    }

    /** Writes how long each test that passed in this run took, over what the file said of it. */
    static void record(final Path runDirectory, final List<ClientTestReport.Result> results) {
        try {
            Files.createDirectories(runDirectory);
            try (FileChannel channel = FileChannel.open(runDirectory.resolve(LOCK), StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                final Map<String, Integer> ticks = read(runDirectory);
                for (final ClientTestReport.Result result : results) {
                    if (result.passed()) {
                        ticks.put(result.name(), result.ticks());
                    }
                }
                final List<String> lines = new ArrayList<>(ticks.size());
                ticks.forEach((name, taken) -> lines.add(taken + " " + name));
                Files.write(runDirectory.resolve(FILE), lines, StandardCharsets.UTF_8);
            }
        } catch (final IOException e) {
            LOGGER.warn("[JSC-CT] the client test durations could not be written to {}", runDirectory, e);
        }
    }
}
