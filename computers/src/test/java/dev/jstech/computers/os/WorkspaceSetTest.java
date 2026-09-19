/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkspaceSetTest {

    @Test
    void only_holdsThatWorkspaceAndNoOther() {
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            for (int other = 0; other < WorkspaceSet.COUNT; other++) {
                assertEquals(i == other, WorkspaceSet.holds(WorkspaceSet.only(i), other));
            }
        }
    }

    @Test
    void only_bringsANumberNoDesktopHasInsideWhatItHas() {
        assertEquals(WorkspaceSet.only(0), WorkspaceSet.only(-3));
        assertEquals(WorkspaceSet.only(WorkspaceSet.COUNT - 1), WorkspaceSet.only(9));
    }

    @Test
    void every_holdsEachWorkspace() {
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            assertTrue(WorkspaceSet.holds(WorkspaceSet.EVERY, i));
        }
    }

    @Test
    void holds_isFalseForAWorkspaceThatDoesNotExist() {
        assertFalse(WorkspaceSet.holds(WorkspaceSet.EVERY, -1));
        assertFalse(WorkspaceSet.holds(WorkspaceSet.EVERY, WorkspaceSet.COUNT));
        assertFalse(WorkspaceSet.holds(-1, 31));
    }

    @Test
    void normalised_dropsWorkspacesThatDoNotExist() {
        assertEquals(WorkspaceSet.only(1), WorkspaceSet.normalised(WorkspaceSet.only(1) | 1 << 7));
        assertEquals(WorkspaceSet.EVERY, WorkspaceSet.normalised(-1));
    }

    @Test
    void normalised_putsAnEmptySetOnTheFirstWorkspace() {
        assertEquals(WorkspaceSet.only(0), WorkspaceSet.normalised(0));
        assertEquals(WorkspaceSet.only(0), WorkspaceSet.normalised(1 << 9));
    }

    @Test
    void first_isTheLowestWorkspaceOfTheSet() {
        assertEquals(2, WorkspaceSet.first(WorkspaceSet.only(2) | WorkspaceSet.only(3)));
        assertEquals(0, WorkspaceSet.first(WorkspaceSet.EVERY));
        assertEquals(0, WorkspaceSet.first(0));
    }

    @Test
    void toggled_ticksAWorkspaceOnAndOff() {
        final int oneAndThree = WorkspaceSet.toggled(WorkspaceSet.only(0), 2);
        assertTrue(WorkspaceSet.holds(oneAndThree, 0) && WorkspaceSet.holds(oneAndThree, 2));
        assertEquals(WorkspaceSet.only(2), WorkspaceSet.toggled(oneAndThree, 0));
    }

    @Test
    void toggled_refusesToEmptyTheSet() {
        assertEquals(WorkspaceSet.only(1), WorkspaceSet.toggled(WorkspaceSet.only(1), 1));
    }

    @Test
    void toggled_leavesTheSetAloneForAWorkspaceThatDoesNotExist() {
        assertEquals(WorkspaceSet.only(1), WorkspaceSet.toggled(WorkspaceSet.only(1), WorkspaceSet.COUNT));
        assertEquals(WorkspaceSet.only(1), WorkspaceSet.toggled(WorkspaceSet.only(1), -1));
    }
}
