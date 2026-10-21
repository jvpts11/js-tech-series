/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedDataTest {

    @TempDir
    Path folder;

    @Test
    void differences_areNoneForTheSameFiles() throws IOException {
        final Path before = files("before", "assets/mymod/lang/en_us.json", "{}");
        final Path after = files("after", "assets/mymod/lang/en_us.json", "{}");

        assertEquals(List.of(), GeneratedData.differences(before, after));
    }

    @Test
    void differences_nameEachFileNewGoneAndChanged() throws IOException {
        final Path before = files("before", "a.json", "1");
        write(before.resolve("b.json"), "same");
        write(before.resolve("gone.json"), "x");
        final Path after = files("after", "a.json", "2");
        write(after.resolve("b.json"), "same");
        write(after.resolve("data/new.json"), "y");

        assertEquals(List.of("changed: a.json", "new: data/new.json", "gone: gone.json"),
                GeneratedData.differences(before, after));
    }

    @Test
    void differences_leaveTheDataGenerationsCacheOut() throws IOException {
        final Path before = files("before", ".cache/one", "old");
        final Path after = files("after", ".cache/one", "new");
        write(after.resolve(".cache/two"), "more");

        assertEquals(List.of(), GeneratedData.differences(before, after));
    }

    @Test
    void differences_ofAFolderThatIsNotThereAreAllItsFilesNew() throws IOException {
        final Path after = files("after", "a.json", "1");

        assertEquals(List.of("new: a.json"), GeneratedData.differences(folder.resolve("none"), after));
    }

    private Path files(final String name, final String path, final String text) throws IOException {
        final Path root = folder.resolve(name);
        write(root.resolve(path), text);
        return root;
    }

    private static void write(final Path file, final String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
