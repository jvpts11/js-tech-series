/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaIdTest {

    /** The SHA-256 of "abc", as the standard gives it. */
    private static final String ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void of_isNamedByTheHashOfItsBytes() {
        final MediaId id = MediaId.of("abc".getBytes(StandardCharsets.US_ASCII), "OGG");
        assertEquals(ABC, id.hash());
        assertEquals("ogg", id.format(), "the kind is kept in lower case");
        assertEquals(3, id.bytes());
        assertEquals(ABC + ".ogg", id.fileName());
    }

    @Test
    void constructor_refusesANameThatIsNoHash() {
        assertThrows(IllegalArgumentException.class, () -> new MediaId("abc", "ogg", 3));
        assertThrows(IllegalArgumentException.class, () -> new MediaId(ABC.toUpperCase(), "ogg", 3));
    }

    @Test
    void constructor_refusesAKindThatIsNoExtension() {
        assertThrows(IllegalArgumentException.class, () -> new MediaId(ABC, "../ogg", 3));
        assertThrows(IllegalArgumentException.class, () -> new MediaId(ABC, "", 3));
    }

    @Test
    void constructor_refusesFewerThanNoBytes() {
        assertThrows(IllegalArgumentException.class, () -> new MediaId(ABC, "ogg", -1));
    }
}
