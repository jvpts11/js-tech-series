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

class IsaSpecTest {

    @Test
    void runs_ownProgramsAlways() {
        final IsaSpec isa = new IsaSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertTrue(isa.runs("other:risc"));
    }

    @Test
    void runs_anIsaItDoesNotList_isFalse() {
        final IsaSpec isa = new IsaSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertFalse(isa.runs("jsc:x86"));
    }

    @Test
    void runs_null_isFalse() {
        final IsaSpec isa = new IsaSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertFalse(isa.runs((IsaSpec) null));
    }

    @Test
    void runs_theSpecItself_readsTheSameAsItsId() {
        final IsaSpec older = new IsaSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        final IsaSpec newer = new IsaSpec("other:risc64", "RISC-64", 64, Set.of("other:risc64", "other:risc"));
        assertTrue(newer.runs(older));
        assertFalse(older.runs(newer));
    }

    @Test
    void construct_keepsItsOwnCopyOfTheList() {
        final Set<String> mutable = new HashSet<>(Set.of("other:risc"));
        final IsaSpec isa = new IsaSpec("other:risc", "RISC", 32, mutable);
        mutable.add("jsc:x86");
        assertFalse(isa.runs("jsc:x86"));
    }

    @Test
    void construct_idWithoutANamespace_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new IsaSpec("risc", "RISC", 32, Set.of("risc")));
    }

    @Test
    void construct_idWithNothingAfterTheColon_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new IsaSpec("other:", "RISC", 32, Set.of("other:")));
    }

    @Test
    void construct_noWordSize_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new IsaSpec("other:risc", "RISC", 0, Set.of("other:risc")));
    }

    @Test
    void construct_anIsaThatDoesNotRunItsOwnPrograms_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new IsaSpec("other:risc", "RISC", 32, Set.of("jsc:x86")));
    }

    @Test
    void bits_areTheIsasOwn() {
        assertEquals(64, new IsaSpec("other:risc64", "RISC-64", 64, Set.of("other:risc64")).bits());
    }

    @Test
    @SuppressWarnings("removal")
    void formerName_carriesTheSameValuesAcross() {
        final ArchitectureSpec former = new ArchitectureSpec("other:risc", "RISC", 32, Set.of("other:risc"));
        assertEquals(new IsaSpec("other:risc", "RISC", 32, Set.of("other:risc")), former.toIsa());
    }
}
