/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LiveSavedTest {

    @Test
    void write_aNameWithAnEqualsSignReadsBackWhole() {
        final LiveSaved saved = new LiveSaved();
        saved.put("file:/root/a=b", "x");

        final LiveSaved back = LiveSaved.read(saved.write());

        assertEquals("x", back.value("file:/root/a=b", "missing"));
        assertEquals("missing", back.value("file:/root/a", "missing"));
    }

    @Test
    void write_aNameWithABackslashOrNewlineReadsBackWhole() {
        final LiveSaved saved = new LiveSaved();
        saved.put("file:/root/a\\e", "one");
        saved.put("file:/root/b\nc", "two");

        final LiveSaved back = LiveSaved.read(saved.write());

        assertEquals("one", back.value("file:/root/a\\e", "missing"));
        assertEquals("two", back.value("file:/root/b\nc", "missing"));
    }

    @Test
    void write_aValueWithEqualsSignsStaysIntact() {
        final LiveSaved saved = new LiveSaved();
        saved.put("MAKEOPTS", "a=b=c");

        assertEquals("a=b=c", LiveSaved.read(saved.write()).value("MAKEOPTS", ""));
    }
}
