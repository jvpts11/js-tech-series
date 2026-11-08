/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefragStateTest {

    @Test
    void write_readsBackTheSameState() {
        final DefragState state = new DefragState(DefragLook.FRAMES_XP,
                List.of(new DefragState.Disk(0, "C:", 0, "NTFS", 40_960L, 13_107L, 680, 120, 30, false, 412L, 900L, 7),
                        new DefragState.Disk(1, "D:", 1, "NTFS", 512L, 512L, 0, 0, 0, true, 0L, -1L, 0)),
                new DefragState.Run(0, 42, 412L, 391L, 21L, 12, true),
                new DefragState.Result(1, 10L, 9L, 1L, 4, 3), true);
        assertEquals(state, DefragState.read(state.write()));
    }

    @Test
    void read_skipsWhatItCannotRead() {
        final DefragState state = DefragState.read("look|linux|0\nsomething|else\ndisk|broken");
        assertEquals(DefragLook.LINUX, state.look());
        assertTrue(state.disks().isEmpty());
        assertNull(state.running());
        assertFalse(state.schedule());
    }

    @Test
    void path_carriesTheActionAndTheDisk() {
        final String path = DefragState.path(DefragState.START, 2);
        assertTrue(DefragState.names(path));
        assertEquals(DefragState.START, DefragState.actionOf(path));
        assertEquals(2, DefragState.diskOf(path));
        assertFalse(DefragState.names("msd:main"));
    }
}
