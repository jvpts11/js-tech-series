/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureSpecTest {

    @Test
    void runs_ownProgramsAlways() {
        final ArchitectureSpec arch = new ArchitectureSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertTrue(arch.runs("other:risc"));
    }

    @Test
    void runs_anArchitectureItDoesNotList_isFalse() {
        final ArchitectureSpec arch = new ArchitectureSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertFalse(arch.runs("jsc:x86"));
    }

    @Test
    void runs_null_isFalse() {
        final ArchitectureSpec arch = new ArchitectureSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertFalse(arch.runs((ArchitectureSpec) null));
    }

    @Test
    void runs_theSpecItself_readsTheSameAsItsId() {
        final ArchitectureSpec older = new ArchitectureSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        final ArchitectureSpec newer =
                new ArchitectureSpec("other:risc64", "RISC-64", 64, Set.of("other:risc64", "other:risc"));
        assertTrue(newer.runs(older));
        assertFalse(older.runs(newer));
    }

    @Test
    void construct_keepsItsOwnCopyOfTheList() {
        final Set<String> mutable = new HashSet<>(Set.of("other:risc"));
        final ArchitectureSpec arch = new ArchitectureSpec("other:risc", "RISC", 32, mutable);
        mutable.add("jsc:x86");
        assertFalse(arch.runs("jsc:x86"));
    }

    @Test
    void construct_idWithoutANamespace_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArchitectureSpec("risc", "RISC", 32, Set.of("risc")));
    }

    @Test
    void construct_idWithNothingAfterTheColon_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArchitectureSpec("other:", "RISC", 32, Set.of("other:")));
    }

    @Test
    void construct_noWordSize_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArchitectureSpec("other:risc", "RISC", 0, Set.of("other:risc")));
    }

    @Test
    void construct_anArchitectureThatDoesNotRunItsOwnPrograms_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArchitectureSpec("other:risc", "RISC", 32, Set.of("jsc:x86")));
    }

    @Test
    void bits_areTheArchitecturesOwn() {
        assertEquals(64, new ArchitectureSpec("other:risc64", "RISC-64", 64, Set.of("other:risc64")).bits());
    }
}
