/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionalDependencyCheckTest {

    private static final OptionalDependencyCheck CHECK = OptionalDependencyCheck.of(List.of("mezz.jei."));

    @TempDir
    private Path root;

    @Test
    void violations_noneWhenTheOptionalModIsNamedOnlyBehindItsGuard() throws IOException {
        write("mymod/Main.java", "import mymod.integration.jei.JeiGuard;");
        write("mymod/integration/jei/JeiGuard.java", "import mymod.integration.jei.JeiAdapter;");
        write("mymod/integration/jei/JeiAdapter.java", "import mezz.jei.api.IModPlugin;");

        assertEquals(List.of(), CHECK.violations(List.of(root)));
    }

    @Test
    void violations_anOptionalClassNamedOutsideAnIntegrationPackage() throws IOException {
        write("mymod/Main.java", "import mezz.jei.api.IModPlugin;");

        final List<String> found = CHECK.violations(List.of(root));

        assertEquals(1, found.size());
        assertTrue(found.getFirst().contains("/mymod/Main.java"), found.toString());
    }

    @Test
    void violations_aGuardThatNamesTheOptionalModItself() throws IOException {
        write("mymod/Main.java", "import mymod.integration.jei.JeiGuard;");
        write("mymod/integration/jei/JeiGuard.java", "import mezz.jei.api.IModPlugin;");

        final List<String> found = CHECK.violations(List.of(root));

        assertEquals(1, found.size());
        assertTrue(found.getFirst().startsWith("mymod.integration.jei.JeiGuard"), found.toString());
    }

    private void write(final String path, final String imports) throws IOException {
        final Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "package x;\n\n" + imports + "\n\nfinal class X {\n}\n");
    }
}
