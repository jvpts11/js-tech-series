/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MediaVolumeTest {

    @Test
    void key_ofAMediumIsTheReaderAlone() {
        assertEquals("media:42", MediaVolume.key(42L, MediaVolume.MEDIUM));
    }

    @Test
    void key_ofADockedDiskNamesItsTray() {
        assertEquals("media:42.2", MediaVolume.key(42L, 2));
    }

    @Test
    void parse_readsTheKeyBackWithOrWithoutAPath() {
        assertEquals(new MediaVolume(-7L, MediaVolume.MEDIUM), MediaVolume.parse("media:-7"));
        assertEquals(new MediaVolume(42L, 1), MediaVolume.parse("media:42.1/docs/notes.txt"));
    }

    @Test
    void parse_answersNullForWhatIsNoVolume() {
        assertNull(MediaVolume.parse("C:/notes.txt"));
        assertNull(MediaVolume.parse("media:abc"));
        assertNull(MediaVolume.parse("media:42.x"));
        assertNull(MediaVolume.parse(null));
    }

    @Test
    void subPath_isWhatFollowsTheVolume() {
        assertEquals("docs/notes.txt", MediaVolume.subPath("media:42.1/docs/notes.txt"));
        assertEquals("", MediaVolume.subPath("media:42"));
    }

    @Test
    void docked_onlyForATray() {
        assertTrue(new MediaVolume(1L, 0).docked());
        assertFalse(new MediaVolume(1L, MediaVolume.MEDIUM).docked());
    }
}
