/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GentooProfilesTest {

    @Test
    void profileOf_readsANumberOrAWholeName() {
        assertEquals(2, GentooProfiles.profileOf("2"));
        assertEquals(3, GentooProfiles.profileOf(GentooProfiles.names().get(2)));
    }

    @Test
    void profileOf_answersZeroForAnythingElse() {
        assertEquals(0, GentooProfiles.profileOf("0"));
        assertEquals(0, GentooProfiles.profileOf(String.valueOf(GentooProfiles.names().size() + 1)));
        assertEquals(0, GentooProfiles.profileOf("desktop"));
    }
}
