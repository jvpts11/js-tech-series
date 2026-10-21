/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The plugin applied in small builds of its own: what it sets up, with ModDevGradle and without it, and its licence
 * headers checked and written.
 */
class JsTechPluginTest {

    private static final String BARE = """
            package com.demo;

            public final class Kiln {
            }
            """;

    @TempDir
    Path project;

    @BeforeEach
    void writeTheDemoMod() throws IOException {
        write("settings.gradle", "rootProject.name = 'demo'\n");
        write("gradle.properties", """
                mod_id=demo
                mod_name=Demo
                mod_license=MIT
                mod_authors=Jane Doe
                """);
        write("src/main/java/com/demo/Kiln.java", BARE);
    }

    @Test
    void info_saysTheModAndTheCoreItIsBuiltOn() throws IOException {
        write("build.gradle", "plugins {\n    id 'dev.jstech.core'\n}\n");

        final BuildResult result = runner("jstechInfo").build();

        assertTrue(result.getOutput().contains("mod: demo"), result.getOutput());
        assertTrue(result.getOutput().contains("core: dev.jstech:core:" + System.getProperty("jstech.gradle.version")),
                result.getOutput());
        assertTrue(result.getOutput().contains("test kit: dev.jstech:testkit:"), result.getOutput());
    }

    @Test
    void modDev_isGivenTheModsRunsAndItsTestsSourceSet() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'net.neoforged.moddev'
                    id 'dev.jstech.core'
                }
                tasks.register('sourceSetsSaid') {
                    def names = sourceSets.names.toList()
                    doLast {
                        println "source sets: " + names.join(', ')
                    }
                }
                """);

        final BuildResult result = runner("jstechInfo", "sourceSetsSaid").build();

        assertTrue(result.getOutput().contains("runs: client, clientTests, data, gameTestServer, server"),
                result.getOutput());
        assertTrue(result.getOutput().contains("gametest"), result.getOutput());
    }

    @Test
    void licenceHeaders_areCheckedAndWrittenWhereMissing() throws IOException {
        write("build.gradle", "plugins {\n    id 'dev.jstech.core'\n}\n");

        final BuildResult missing = runner("checkLicenceHeaders").buildAndFail();
        assertTrue(missing.getOutput().contains("Kiln.java has no licence header"), missing.getOutput());

        final BuildResult written = runner("addLicenceHeaders").build();
        assertEquals(TaskOutcome.SUCCESS, written.task(":addLicenceHeaders").getOutcome());
        final String source = Files.readString(project.resolve("src/main/java/com/demo/Kiln.java"));
        assertTrue(source.startsWith("/*\n * SPDX-License-Identifier: MIT\n *\n * Copyright (C) "), source);
        assertTrue(source.contains(" Jane Doe\n *\n * This file is part of Demo.\n */\npackage com.demo;"), source);

        final BuildResult checked = runner("checkLicenceHeaders").build();
        assertEquals(TaskOutcome.SUCCESS, checked.task(":checkLicenceHeaders").getOutcome());
    }

    @Test
    void licenceHeaders_namingAnotherLicenceFailTheCheck() throws IOException {
        write("build.gradle", "plugins {\n    id 'dev.jstech.core'\n}\njstech {\n    licence = 'Apache-2.0'\n}\n");
        write("src/main/java/com/demo/Kiln.java", LicenceHeaders.header("MIT", 2026, "Jane Doe", "Demo") + BARE);

        final BuildResult result = runner("checkLicenceHeaders").buildAndFail();

        assertTrue(result.getOutput().contains("names MIT, not Apache-2.0"), result.getOutput());
    }

    @Test
    void apiSurface_keepsAModWithNoApiAndFailsOneThatReachesPastItsPromise() throws IOException {
        write("build.gradle", "plugins {\n    id 'dev.jstech.core'\n}\n");

        final BuildResult kept = runner("checkApiSurface").build();
        assertTrue(kept.getOutput().contains("The API of demo keeps its promise."), kept.getOutput());

        write("src/main/java/com/demo/api/JsDemoApi.java", """
                package com.demo.api;

                public final class JsDemoApi {
                    public static final int VERSION = 1;
                }
                """);
        write("src/main/java/com/demo/api/Widget.java", """
                package com.demo.api;

                import com.demo.Kiln;

                public final class Widget {
                    public Kiln kiln() {
                        return null;
                    }
                }
                """);
        final BuildResult broken = runner("checkApiSurface").buildAndFail();

        assertTrue(broken.getOutput().contains("com.demo.api.Widget > com.demo.Kiln: not promised"),
                broken.getOutput());
        assertTrue(Files.isRegularFile(project.resolve("build/api-photo/demo.txt")), "a photo is proposed");
    }

    private GradleRunner runner(final String... tasks) {
        final String[] arguments = new String[tasks.length + 2];
        System.arraycopy(tasks, 0, arguments, 0, tasks.length);
        arguments[tasks.length] = "--stacktrace";
        // The test kit the plugin's checks run, as this build just published it.
        arguments[tasks.length + 1] = "-Dmaven.repo.local=" + System.getProperty("jstech.gradle.repository");
        return GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath().withArguments(arguments)
                .forwardOutput();
    }

    private void write(final String path, final String text) throws IOException {
        final Path file = project.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
