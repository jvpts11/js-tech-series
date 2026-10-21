/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagFilesTest {

    @Test
    void members_joinEveryFileOfTheTag() {
        final Map<String, List<String>> files = Map.of("c:stones", List.of(
                "{\"values\": [\"minecraft:stone\", \"minecraft:granite\"]}",
                "{\"replace\": false, \"values\": [\"othermod:limestone\"]}"));

        assertEquals(Set.of("minecraft:stone", "minecraft:granite", "othermod:limestone"),
                TagFiles.members("c:stones", tag -> files.getOrDefault(tag, List.of())));
    }

    @Test
    void members_followTheTagsATagNames() {
        final Map<String, List<String>> files = Map.of(
                "minecraft:base_stone_overworld", List.of("{\"values\": [\"minecraft:stone\", \"#othermod:rocks\"]}"),
                "othermod:rocks", List.of("{\"values\": [\"othermod:marble\", \"#minecraft:base_stone_overworld\"]}"));

        assertEquals(Set.of("minecraft:stone", "othermod:marble"),
                TagFiles.members("minecraft:base_stone_overworld", tag -> files.getOrDefault(tag, List.of())));
    }

    @Test
    void members_readTheEntriesWrittenAsObjects() {
        final Map<String, List<String>> files = Map.of("c:stones", List.of(
                "{\"values\": [{\"id\": \"absentmod:slate\", \"required\": false}, {\"id\": \"#othermod:rocks\"}]}"),
                "othermod:rocks", List.of("{\"values\": [\"othermod:marble\"]}"));

        assertEquals(Set.of("absentmod:slate", "othermod:marble"),
                TagFiles.members("c:stones", tag -> files.getOrDefault(tag, List.of())));
    }

    @Test
    void members_passOverAFileThatCannotBeRead() {
        final Map<String, List<String>> files = Map.of("c:stones", List.of("{not json", "{\"values\": 3}",
                "{\"values\": [\"othermod:marble\"]}"));

        assertEquals(Set.of("othermod:marble"), TagFiles.members("c:stones", tag -> files.getOrDefault(tag,
                List.of())));
    }

    @Test
    void tagsIn_givesOnlyTheTagsATagNamesItself() {
        final Map<String, List<String>> files = Map.of("c:storage_blocks", List.of(
                "{\"values\": [\"#c:storage_blocks/iron\", {\"id\": \"#c:storage_blocks/tin\"}, \"othermod:odd\"]}"),
                "c:storage_blocks/iron", List.of("{\"values\": [\"#c:storage_blocks/raw_iron\"]}"));

        assertEquals(Set.of("c:storage_blocks/iron", "c:storage_blocks/tin"),
                TagFiles.tagsIn("c:storage_blocks", tag -> files.getOrDefault(tag, List.of())));
    }
}
