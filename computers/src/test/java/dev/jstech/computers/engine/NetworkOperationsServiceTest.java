/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The network's work comes in by one door.
 *
 * <p>The Mainframe's submit methods are the Operations core: the engines hand their plans to it, and an Operation
 * already running asks it for the work it is made of. Anything else that wants the network to do work goes through
 * the Network Operations Service, so that the engine running decides how, and a network with no engine refuses what
 * only an engine plans. A call to the core from anywhere else is a second door, and this test finds it.
 */
class NetworkOperationsServiceTest {

    /** The mods built on the computers, whose code could reach a Mainframe. */
    private static final Set<String> MODULES = Set.of("computers", "industrial");
    /** The core's own calls: what asks it for work by its own name. */
    private static final List<String> CORE_CALLS = List.of(".submitNetworkSelect(", ".submitNetworkMove(",
            ".submitNetworkDelete(", ".submitNetworkInsert(", ".submitNetworkCraft(", ".submitPlannedCraft(",
            ".submitNetworkProcessing(", ".submitNetworkMultiStage(", ".submitCraftRequest(");
    /** Where calling the core is the point: the engines, the core itself, and Operations made of other work. */
    private static final Set<String> CORE_SIDE = Set.of(
            "computers/src/main/java/dev/jstech/computers/engine/",
            "computers/src/main/java/dev/jstech/computers/blockentity/MainframeBlockEntity.java",
            "computers/src/main/java/dev/jstech/computers/blockentity/MainframeCrafts.java",
            "computers/src/main/java/dev/jstech/computers/crafting/NetworkCraftOperation.java",
            "computers/src/main/java/dev/jstech/computers/crafting/NetworkMultiStageOperation.java",
            "computers/src/main/java/dev/jstech/computers/crafting/PendingCraftOperation.java");

    @Test
    void coreCalls_comeOnlyFromTheEnginesAndTheCore() throws IOException {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<String> doors = new ArrayList<>();
        for (final String module : MODULES) {
            final Path sources = root.resolve(module).resolve("src").resolve("main").resolve("java");
            assertTrue(Files.isDirectory(sources), "the sources of " + module + " are where the test reads them");
            try (Stream<Path> files = Files.walk(sources)) {
                files.filter(file -> file.toString().endsWith(".java")).forEach(file -> {
                    final String name = root.relativize(file).toString().replace('\\', '/');
                    if (CORE_SIDE.stream().noneMatch(name::startsWith)) {
                        doors.addAll(coreCallsIn(file, name));
                    }
                });
            }
        }
        assertTrue(doors.isEmpty(), () -> "these ask the Operations core for work directly; ask the network's"
                + " NetworkOperationsService instead:\n" + String.join("\n", doors));
    }

    private static List<String> coreCallsIn(final Path file, final String name) {
        final List<String> found = new ArrayList<>();
        try {
            final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                if (CORE_CALLS.stream().anyMatch(line::contains)) {
                    found.add(name + ":" + (i + 1) + ": " + line.strip());
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }
}
