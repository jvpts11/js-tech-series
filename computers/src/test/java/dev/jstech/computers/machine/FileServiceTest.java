/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileServiceTest {

    @Test
    void bytesOf_countsATextTheWayTheDiskStoresIt() {
        // A Gothic letter lies past the first 65,536 characters, so Java holds it as a pair and UTF-8 as four bytes.
        final List<String> texts = List.of("", "plain words", "Σ# program", "€100", "𐍈 letter",
                "half \uD800 of a pair", "other half \uDF48 alone", "\uD800");
        for (final String text : texts) {
            assertEquals(text.getBytes(StandardCharsets.UTF_8).length, FileService.bytesOf(text), text);
        }
    }
}
