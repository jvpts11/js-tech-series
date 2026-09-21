/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A client is never trusted to say which machine it is using, so every payload a client sends reaches its handler
 * only through a gate that checks the player has a screen open on that machine. These read the sources and hold that
 * shape: nothing registers a client payload except the gated registration, every gated registration names one of the
 * gates, and a payload that travels both ways guards its server side.
 */
class ClientPayloadGatesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final String GATED_REGISTRATION = "ComputerAccess.java";
    private static final String ACCEPT_CALL = "ComputerAccess.accept(";
    /** A gated registration: the registrar, the type, the codec, and the gate, which must be one of the named gates. */
    private static final Pattern ACCEPT = Pattern.compile(
            "ComputerAccess\\.accept\\(\\s*registrar\\s*,\\s*[\\w.]+\\s*,\\s*[\\w.]+\\s*,\\s*(\\S+)");

    @Test
    void playToServer_isCalledOnlyByTheGatedRegistration() {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles()) {
            if (!file.endsWith(GATED_REGISTRATION) && read(file).contains("playToServer(")) {
                found.add(SOURCES.relativize(file).toString());
            }
        }
        assertTrue(found.isEmpty(), () -> "client payloads registered without a gate in: " + found);
    }

    @Test
    void everyGatedRegistration_namesAGate() {
        final List<String> found = new ArrayList<>();
        int registrations = 0;
        for (final Path file : javaFiles()) {
            final String text = read(file);
            final int calls = occurrences(text, ACCEPT_CALL);
            int recognised = 0;
            final Matcher matcher = ACCEPT.matcher(text);
            while (matcher.find()) {
                recognised++;
                if (!matcher.group(1).startsWith("ComputerAccess.")) {
                    found.add(SOURCES.relativize(file) + ": gate " + matcher.group(1));
                }
            }
            if (recognised != calls) {
                found.add(SOURCES.relativize(file) + ": " + (calls - recognised) + " registration(s) in a form this test cannot read");
            }
            registrations += calls;
        }
        final int total = registrations;
        assertTrue(total > 0, "the gated registrations are found in the sources");
        assertTrue(found.isEmpty(), () -> "registrations whose gate is not one of ComputerAccess's (" + total + " read):\n"
                + String.join("\n", found));
    }

    @Test
    void bothWayPayloads_guardTheirServerSide() {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles()) {
            final String text = read(file);
            int at = text.indexOf("playBidirectional(");
            while (at >= 0) {
                final int end = text.indexOf(';', at);
                final String statement = text.substring(at, end < 0 ? text.length() : end);
                if (!statement.contains("ComputerAccess.guarded(")) {
                    found.add(SOURCES.relativize(file).toString());
                }
                at = text.indexOf("playBidirectional(", at + 1);
            }
        }
        assertTrue(found.isEmpty(), () -> "payloads travelling both ways with an unguarded server side in: " + found);
    }

    private static int occurrences(final String text, final String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private static List<Path> javaFiles() {
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
