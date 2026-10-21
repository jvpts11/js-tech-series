/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class McpFilesTest {

    @TempDir
    Path root;
    private List<McpFiles.Folder> folders;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(this.root.resolve("mod").resolve("assets"));
        this.folders = List.of(new McpFiles.Folder("mod:mymod", this.root.resolve("mod")));
    }

    @Test
    void find_reachesAFileInsideAFolderGiven() {
        final McpFiles.Found found = McpFiles.find(this.folders, "mod:mymod/assets/mymod/lang/en_us.json", true);
        assertEquals(this.root.resolve("mod").resolve("assets").resolve("mymod").resolve("lang")
                .resolve("en_us.json").toAbsolutePath().normalize(), found.path().orElseThrow());
    }

    @Test
    void find_neverStepsOutOfTheFolder() {
        assertTrue(refused("mod:mymod/../outside.json"));
        assertTrue(refused("mod:mymod/assets/../../outside.json"));
        assertTrue(refused("mod:mymod//etc/passwd.txt"));
        assertTrue(refused("mod:mymod/C:/Windows/win.ini"));
    }

    @Test
    void find_refusesAFolderNotGiven() {
        assertTrue(refused("mod:other/assets/x.json"));
        assertTrue(refused("/absolute/x.json"));
    }

    @Test
    void find_refusesCode() {
        assertTrue(refused("mod:mymod/scripts/run.js"));
        assertTrue(refused("mod:mymod/run.bat"));
        assertTrue(refused("mod:mymod/assets/noending"));
    }

    @Test
    void kindOf_isTheEndingInSmallLetters() {
        assertEquals("png", McpFiles.kindOf(Path.of("a", "B.PNG")));
        assertEquals("", McpFiles.kindOf(Path.of("a", "b")));
    }

    private boolean refused(final String path) {
        final McpFiles.Found found = McpFiles.find(this.folders, path, true);
        return found.path().isEmpty() && found.why().isPresent();
    }
}
