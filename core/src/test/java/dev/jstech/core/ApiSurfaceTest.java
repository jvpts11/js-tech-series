/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.testkit.check.ApiSurface;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * What the series promises a mod built on it, and nothing more, checked with the test of the API's surface the Core's
 * test kit offers every mod ({@link ApiSurface}), over the sources of every mod of the series.
 *
 * <p>The promise is what sits in an {@code api} package, in a package its mod's API class lists as promised, or in
 * one of the few promised types that live where they belong; everything else in the series is its own business and
 * changes without a word. Every name a promised face says is followed to where it comes from, and it may only be the
 * promise's own, the language's or the game's; a promised package has to be its mod's own and explain itself in its
 * {@code package-info}. The promise is kept as a photo, one file per mod in {@code src/test/resources/api}, and a
 * failing run writes what the photo would have to say to {@code build/api-photo}, to be read and copied over.
 *
 * <p>The list below is the same one {@code docs/API.md} names, and the two are meant to be read together.
 */
class ApiSurfaceTest {

    /**
     * Ours that are part of the promise though they do not live in an api package.
     *
     * <p>Each is something an addon has to hold to do what the API is for: a registry it adds to, or a description of
     * a thing it is adding.
     */
    private static final Set<String> PROMISED = Set.of(
            // the registries an addon adds to
            "LanguageRegistry", "OperationTypeRegistry",
            // what it adds to them
            "IProgrammingLanguage", "OperationType", "IOperationArgs",
            // the computers: what a machine is, and what can be installed on one
            "IsaSpec", "KernelDef", "OsDef", "ProgramSpec", "DesktopEnvironmentDef", "OperatingSpaceDef",
            // the engines a Mainframe can run to plan its network's work, and what they may offer
            "EngineDef", "EngineCapability",
            // the former name of IsaSpec, deprecated for one cycle before it goes
            "ArchitectureSpec");

    /**
     * What the promise reached before it was followed to its end, each line a promised type and a type of the series
     * it names without promising: the debts of the promise as it stood, written down so no new one joins them.
     *
     * <p>Most of them are types whose place or shape is about to change: the programming languages move to J's
     * Computers' API, the kinds of Operation and the eras to the series' package, the eras becoming a registry. A
     * line goes when its type is promised or leaves the signature, and the test fails while a line outlives its leak.
     */
    private static final Set<String> KNOWN_LEAKS = Set.of(
            "dev.jstech.computers.engine.EngineCapability > dev.jstech.core.id.IStableName",
            "dev.jstech.computers.engine.EngineDef > dev.jstech.core.tier.HardwareEra",
            "dev.jstech.computers.os.DesktopEnvironmentDef > dev.jstech.computers.os.PanelStyle",
            "dev.jstech.computers.os.DesktopEnvironmentDef > dev.jstech.core.text.TextKey",
            "dev.jstech.computers.os.DesktopEnvironmentDef > dev.jstech.computers.os.SoftwareHouse",
            "dev.jstech.computers.os.KernelDef > dev.jstech.computers.os.SchedulerKind",
            "dev.jstech.computers.os.KernelDef > dev.jstech.computers.os.FilesystemKind",
            "dev.jstech.computers.os.KernelDef > dev.jstech.computers.os.ShellFamily",
            "dev.jstech.computers.os.OperatingSpaceDef > dev.jstech.computers.os.SoftwareHouse",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.OsCapability",
            "dev.jstech.computers.os.OsDef > dev.jstech.core.tier.HardwareEra",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.Platform",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.ShellKind",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.PackageManagerKind",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.InstallMode",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.SoftwareHouse",
            "dev.jstech.computers.os.OsDef > dev.jstech.computers.os.install.InstallerStyle",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.computers.os.Platform",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.computers.os.ProgramKind",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.computers.os.HostScope",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.core.tier.HardwareEra",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.computers.os.SoftwareHouse",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.computers.os.ProgramRequirement",
            "dev.jstech.computers.os.ProgramSpec > dev.jstech.core.text.TextKey",
            "dev.jstech.core.language.IProgrammingLanguage.Complaint > dev.jstech.core.text.Text",
            "dev.jstech.core.language.IProgrammingLanguage > dev.jstech.core.language.ILanguageProcess",
            "dev.jstech.core.language.IProgrammingLanguage > dev.jstech.core.language.IMachineView",
            "dev.jstech.core.operation.OperationType > dev.jstech.core.operation.OperationCategory",
            "dev.jstech.core.operation.OperationType > dev.jstech.core.tier.IndustrialTier",
            "dev.jstech.core.operation.OperationType > dev.jstech.core.network.NetworkCategory",
            "dev.jstech.core.operation.OperationType > dev.jstech.core.operation.IOperationHandler");

    /* Every mod of the series, its photos beside the Core's tests; run from the Core's folder. */
    private static final ApiSurface SURFACE = ApiSurface.builder()
            .modsUnder(Path.of("").toAbsolutePath().getParent())
            .apiClass("Js\\w+Api\\.java")
            .promised(PROMISED)
            .knownLeaks(KNOWN_LEAKS)
            .photos(Path.of("src", "test", "resources", "api"))
            .proposals(Path.of("build", "api-photo"))
            .build();

    @Test
    void api_reachesNothingThatIsNotPromised() {
        final List<String> wrong = SURFACE.leaks();

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void api_isWhereItIsSaidToBe() {
        final List<Path> found = SURFACE.apiSources();

        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsCoreApi.java")), () -> "found " + found);
        assertTrue(found.stream().anyMatch(path -> path.endsWith("CoreRegisterEvent.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsComputersApi.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("ComputersRegisterEvent.java")));
    }

    @Test
    void api_promisedPackagesAreTheModsOwnAndExplained() {
        final List<String> wrong = SURFACE.packageProblems();

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void api_isWhatItsPhotoSays() {
        final List<String> wrong = SURFACE.photoProblems();

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void api_versionIsTheNewestNumberInItsPhoto() {
        final List<String> wrong = SURFACE.versionProblems();

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void api_marksExperimentalExactlyWhatIsNewerThanTheStableVersion() {
        final List<String> wrong = SURFACE.experimentalProblems();

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }
}
