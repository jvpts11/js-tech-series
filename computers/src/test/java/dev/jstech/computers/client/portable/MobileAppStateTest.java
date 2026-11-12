/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobileAppStateTest {

    @Test
    void room_startsAFreshDraftInAnotherRoom() {
        final MobileAppState apps = new MobileAppState();
        apps.room("alice");
        apps.type('h');
        apps.type('i');
        assertEquals("hi", apps.draft());

        apps.room("bob");

        assertEquals("", apps.draft(), "what was written to Alice is never sent on to Bob");
    }

    @Test
    void room_keepsTheDraftOfTheSameRoom() {
        final MobileAppState apps = new MobileAppState();
        apps.room("alice");
        apps.type('h');

        apps.room("alice");

        assertEquals("h", apps.draft());
    }
}
