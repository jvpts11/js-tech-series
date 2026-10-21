/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiSurfaceTest {

    private static final String LEAK = "com.demo.api.Widget > com.demo.inner.Secret";

    @TempDir
    Path folder;

    private Path main;

    @BeforeEach
    void writeTheDemoMod() throws IOException {
        main = folder.resolve("demo").resolve("src").resolve("main").resolve("java");
        write("com/demo/api/JsDemoApi.java", """
                package com.demo.api;

                import java.util.List;

                public final class JsDemoApi {
                    public static final int VERSION = 1;
                    public static final List<String> PROMISED_PACKAGES = List.of("com.demo.kit");

                    private JsDemoApi() {
                    }
                }
                """);
        write("com/demo/api/Widget.java", """
                package com.demo.api;

                import com.demo.inner.Secret;

                public final class Widget {
                    public Secret secret() {
                        return null;
                    }
                }
                """);
        write("com/demo/inner/Secret.java", """
                package com.demo.inner;

                public final class Secret {
                }
                """);
        write("com/demo/kit/Tool.java", """
                package com.demo.kit;

                public interface Tool {
                    int use(String what);
                }
                """);
    }

    @Test
    void leaks_nameATypeOfTheModThePromiseReachesWithoutPromisingIt() {
        final List<String> leaks = surface(Set.of()).leaks();

        assertEquals(1, leaks.size(), () -> String.join("\n", leaks));
        assertTrue(leaks.getFirst().startsWith(LEAK + ": not promised"), leaks.getFirst());
    }

    @Test
    void leaks_writtenDownAsKnownAreNoProblemTillTheyAreNoLongerReached() throws IOException {
        assertEquals(List.of(), surface(Set.of(LEAK)).leaks());

        write("com/demo/api/Widget.java", """
                package com.demo.api;

                public final class Widget {
                }
                """);
        final List<String> leaks = surface(Set.of(LEAK)).leaks();

        assertEquals(List.of(LEAK + ": no longer reached, so it comes off the known leaks"), leaks);
    }

    @Test
    void packageProblems_askAPromisedPackageToExplainItself() throws IOException {
        final List<String> unexplained = surface(Set.of(LEAK)).packageProblems();

        assertEquals(1, unexplained.size());
        assertTrue(unexplained.getFirst().contains("com.demo.kit has no package-info.java"), unexplained.getFirst());

        write("com/demo/kit/package-info.java", """
                /** The tools a mod built on the demo uses. */
                package com.demo.kit;
                """);

        assertEquals(List.of(), surface(Set.of(LEAK)).packageProblems());
    }

    @Test
    void photoProblems_proposeAPhotoThatOnceKeptSaysTheApiAsItIs() throws IOException {
        final ApiSurface surface = surface(Set.of(LEAK));
        final List<String> missing = surface.photoProblems();

        assertEquals(1, missing.size());
        assertTrue(missing.getFirst().contains("1 ") || missing.getFirst().contains("new:"), missing.getFirst());
        final String proposal = Files.readString(folder.resolve("proposals").resolve("demo.txt"));
        assertTrue(proposal.contains("1 com.demo.kit.Tool  abstract int use(String)"), proposal);
        assertTrue(proposal.contains("1 com.demo.api.Widget  com.demo.inner.Secret secret()")
                || proposal.contains("1 com.demo.api.Widget  Secret secret()"), proposal);

        Files.createDirectories(folder.resolve("photos"));
        Files.writeString(folder.resolve("photos").resolve("demo.txt"), proposal.replace("stable 0", "stable 1"));

        assertEquals(List.of(), surface.photoProblems());
        assertEquals(List.of(), surface.versionProblems());
        assertEquals(List.of(), surface.experimentalProblems());
    }

    @Test
    void experimentalProblems_askWhatCameAfterTheStableVersionToSaySo() throws IOException {
        final ApiSurface surface = surface(Set.of(LEAK));
        surface.photoProblems();
        final String proposal = Files.readString(folder.resolve("proposals").resolve("demo.txt"));
        Files.createDirectories(folder.resolve("photos"));
        Files.writeString(folder.resolve("photos").resolve("demo.txt"), proposal);

        final List<String> unmarked = surface.experimentalProblems();

        assertFalse(unmarked.isEmpty());
        assertTrue(unmarked.stream().allMatch(line -> line.contains("has to carry @ApiStatus.Experimental")),
                () -> String.join("\n", unmarked));
    }

    @Test
    void versionProblems_sayAVersionThatIsNotTheNewestInThePhoto() throws IOException {
        final ApiSurface surface = surface(Set.of(LEAK));
        surface.photoProblems();
        final String proposal = Files.readString(folder.resolve("proposals").resolve("demo.txt"));
        Files.createDirectories(folder.resolve("photos"));
        Files.writeString(folder.resolve("photos").resolve("demo.txt"), proposal.replace("\n1 ", "\n2 "));

        final List<String> wrong = surface.versionProblems();

        assertEquals(1, wrong.size());
        assertTrue(wrong.getFirst().contains("the API VERSION is 1 and the newest number in the photo 2"),
                wrong.getFirst());
    }

    private ApiSurface surface(final Set<String> knownLeaks) {
        return ApiSurface.builder()
                .mod("demo", main)
                .apiClass("Js\\w+Api\\.java")
                .knownLeaks(knownLeaks)
                .photos(folder.resolve("photos"))
                .proposals(folder.resolve("proposals"))
                .build();
    }

    private void write(final String path, final String text) throws IOException {
        final Path file = main.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
