/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ScanFileTest {

    private static final byte[] KEY = "a world's own key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER = "another world's".getBytes(StandardCharsets.UTF_8);

    @Test
    void open_givesBackWhatWasSealedUnderTheSameKey() {
        final String body = "Sharpness V\n\n#ench minecraft:sharpness 5\n";
        assertEquals(body, ScanFile.open(ScanFile.seal(body, KEY), KEY));
    }

    @Test
    void open_refusesAnotherWorldsSeal() {
        assertNull(ScanFile.open(ScanFile.seal("#map 3\n", KEY), OTHER));
    }

    @Test
    void open_refusesAFileWhoseWordsWereChanged() {
        final String sealed = ScanFile.seal("Sharpness V\n\n#ench minecraft:sharpness 5\n", KEY);
        assertNull(ScanFile.open(sealed.replace("sharpness 5", "sharpness 9"), KEY));
    }

    @Test
    void open_refusesAFileWithNoSealOrABrokenOne() {
        assertNull(ScanFile.open("#map 3\n", KEY));
        assertNull(ScanFile.open("#map 3\n#seal not-hex\n", KEY));
        assertNull(ScanFile.open(null, KEY));
    }

    @Test
    void enchantmentsOf_readsBackWhatEnchantmentsWrote() {
        final String body = ScanFile.enchantments(List.of(new ScanFile.Enchanted("Sharpness V", "minecraft:sharpness",
                5), new ScanFile.Enchanted("Mending", "minecraft:mending", 1)));
        assertEquals(List.of(new ScanFile.Enchanted("", "minecraft:sharpness", 5),
                new ScanFile.Enchanted("", "minecraft:mending", 1)), ScanFile.enchantmentsOf(body));
        assertTrue(body.startsWith("Sharpness V\nMending\n"), body);
    }

    @Test
    void enchantmentsOf_skipsLinesWithNoLevel() {
        assertEquals(List.of(), ScanFile.enchantmentsOf("#ench minecraft:sharpness five\n#ench lonely\n"));
    }

    @Test
    void mapOf_readsTheMapsNumber() {
        assertEquals(OptionalInt.of(42), ScanFile.mapOf(ScanFile.map(42, "pix")));
        assertEquals(OptionalInt.empty(), ScanFile.mapOf("not a map"));
        assertEquals(OptionalInt.empty(), ScanFile.mapOf("#map x\npix"));
    }

    @Test
    void pictureOf_isThePictureUnderTheMapLineWithoutTheSeal() {
        final String sealed = ScanFile.seal(ScanFile.map(7, "P 2 2\nabcd"), KEY);
        assertEquals("P 2 2\nabcd", ScanFile.pictureOf(sealed));
        assertEquals("plain", ScanFile.pictureOf("plain"));
    }
}
