/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.testkit.check.ApiUse;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The example mod's build rule: every class, field and method of the Core it reaches is one the Core's API photo
 * promises, and it reaches nothing of the other mods of the series at all. A mod built on the Core runs the same
 * check against the photo of the Core it is built on.
 */
class ExampleApiTest {

    @Test
    void example_usesOnlyWhatTheCorePromises() {
        final ApiUse use = ApiUse.builder()
                .own("dev.jstech.example")
                .photoOf("core")
                .forbid("dev.jstech.computers", "dev.jstech.industrial", "dev.jstech.tests", "dev.jstech.testkit")
                .build();
        assertEquals(List.of(), use.problems(Path.of("build", "classes", "java", "main")));
    }
}
