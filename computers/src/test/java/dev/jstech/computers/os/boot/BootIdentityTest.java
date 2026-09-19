/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BootIdentityTest {

    @Test
    void constructor_readsWhatIsMissingAsNothing() {
        assertEquals(BootIdentity.NONE, new BootIdentity(null, null, null));
    }

    @Test
    void goingDown_keepsTheSystemAndTheHostAndNamesNoDesktop() {
        final BootIdentity down = new BootIdentity("cde", "UNIX System V", "desk").goingDown();
        assertEquals("", down.desktopId());
        assertEquals("UNIX System V", down.systemName());
        assertEquals("desk", down.hostName());
    }

    @Test
    void goingDown_ofAMachineWithNoDesktopIsTheSameMachine() {
        final BootIdentity prompt = new BootIdentity("", "FreeBSD", "desk");
        assertEquals(prompt, prompt.goingDown());
    }
}
