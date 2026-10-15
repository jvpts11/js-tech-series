/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.optional.OptionalDependencyCheck;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The mods the series works with but does not need (Mekanism, JEI, EMI, FTB, ComputerCraft, Curios, Accessories,
 * Jade, KubeJS) must never be needed by accident, which the Core's own check, the one any mod runs over its sources,
 * holds the series to. The development-only test mod is not held to it.
 */
class SoftDependencyRulesTest {

    private static final List<String> OPTIONAL = List.of("mekanism.", "mezz.jei.", "dev.emi.", "dev.ftb.",
            "dev.architectury.", "dan200.computercraft.", "top.theillusivec4.curios.", "io.wispforest.",
            "snownee.jade.", "dev.latvian.");
    private static final Path ROOT = Path.of("").toAbsolutePath().getParent();

    @Test
    void optionalMods_areReachedOnlyThroughTheirGuards() {
        final List<String> found = OptionalDependencyCheck.of(OPTIONAL).violations(roots());

        assertEquals(List.of(), found, () -> String.join("\n", found));
    }

    @Test
    void sources_includeTheMekanismBridgeAndItsGuard() {
        final Path mekanism = ROOT.resolve(Path.of("core", "src", "main", "java", "dev", "jstech", "core",
                "integration", "mekanism"));

        assertTrue(Files.isRegularFile(mekanism.resolve("MekanismChemicalBridge.java")), "the Mekanism bridge is read");
        assertTrue(Files.isRegularFile(mekanism.resolve("MekanismIntegration.java")), "and its guard");
    }

    private static List<Path> roots() {
        return List.of("core", "computers", "industrial").stream()
                .map(module -> ROOT.resolve(module).resolve("src").resolve("main").resolve("java")).toList();
    }
}
