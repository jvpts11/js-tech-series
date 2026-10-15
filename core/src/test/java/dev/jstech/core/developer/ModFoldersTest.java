/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModFoldersTest {

    @Test
    void parse_groupsTheFoldersByMod() {
        final Map<String, List<Path>> folders = ModFolders.parse(
                "jscore%%C:\\work\\core\\build\\classes;jscore%%C:\\work\\core\\build\\resources;jsc%%C:\\work\\jsc",
                ";");

        assertEquals(List.of(Path.of("C:\\work\\core\\build\\classes"), Path.of("C:\\work\\core\\build\\resources")),
                folders.get("jscore"));
        assertEquals(List.of(Path.of("C:\\work\\jsc")), folders.get("jsc"));
    }

    @Test
    void parse_skipsAnEntryWithoutAModOrAFolder() {
        final Map<String, List<Path>> folders = ModFolders.parse("%%/no/mod:jscore%%:jsc%%/work/jsc", ":");

        assertEquals(List.of(Path.of("/work/jsc")), folders.get("jsc"));
        assertEquals(1, folders.size());
    }

    @Test
    void parse_ofNothingIsNothing() {
        assertTrue(ModFolders.parse("", ";").isEmpty());
    }
}
