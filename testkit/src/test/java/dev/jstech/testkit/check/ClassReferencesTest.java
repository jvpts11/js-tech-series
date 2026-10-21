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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassReferencesTest {

    @TempDir
    Path folder;

    private ClassReferences kiln;

    @BeforeEach
    void compileTheKiln() throws IOException {
        final Path source = folder.resolve("Kiln.java");
        Files.writeString(source, """
                package com.demo;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.function.Supplier;

                public class Kiln extends ArrayList<String> implements Supplier<List<Integer>> {
                    public static long made;
                    protected int heat;
                    private double[] history;

                    @Override
                    public List<Integer> get() {
                        made = Math.max(made, heat);
                        return List.of(heat);
                    }

                    static void cool() {
                    }
                }
                """, StandardCharsets.UTF_8);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", folder.toString(),
                "--release", "21", source.toString()), "the kiln compiles");
        kiln = ClassReferences.read(Files.readAllBytes(folder.resolve("com").resolve("demo").resolve("Kiln.class")));
    }

    @Test
    void read_namesTheClassWhatItExtendsAndWhatItImplements() {
        assertEquals("com/demo/Kiln", kiln.name());
        assertEquals("java/util/ArrayList", kiln.superName());
        assertEquals(List.of("java/util/function/Supplier"), kiln.interfaces());
    }

    @Test
    void read_findsWhatItDeclaresWithWhoCanReachIt() {
        assertTrue(kiln.declared().contains(new ClassReferences.Declared("made", "J", true, true, true)));
        assertTrue(kiln.declared().contains(new ClassReferences.Declared("heat", "I", true, true, false)));
        assertTrue(kiln.declared().contains(new ClassReferences.Declared("history", "[D", true, false, false)));
        assertTrue(kiln.declared().contains(new ClassReferences.Declared("cool", "()V", false, false, true)));
        assertTrue(kiln.declares("get", "()Ljava/util/List;", false));
        assertTrue(kiln.declares("heat", "", true));
        assertFalse(kiln.declares("get", "()Ljava/lang/Object;", true));
    }

    @Test
    void read_findsTheTypesItNamesInSignaturesToo() {
        assertTrue(kiln.types().containsAll(List.of("java/util/ArrayList", "java/util/List", "java/lang/Math",
                "java/lang/String", "java/lang/Integer")));
    }

    @Test
    void read_findsTheMembersItUsesWithTheirOwners() {
        assertTrue(kiln.members().contains(new ClassReferences.Member("java/lang/Math", "max", "(JJ)J", false)));
        assertTrue(kiln.members().contains(new ClassReferences.Member("com/demo/Kiln", "made", "J", true)));
        assertTrue(kiln.members().contains(new ClassReferences.Member("java/util/List", "of",
                "(Ljava/lang/Object;)Ljava/util/List;", false)));
    }

    @Test
    void read_refusesWhatIsNoClassFile() {
        assertThrows(IOException.class, () -> ClassReferences.read(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}));
    }
}
