/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationPriorityTest {

    @Test
    void fromKeyword_resolvesEnumNamesCaseInsensitively() {
        assertEquals(Optional.of(OperationPriority.HIGH), OperationPriority.fromKeyword("high"));
        assertEquals(Optional.of(OperationPriority.MEDIUM_LOW), OperationPriority.fromKeyword("Medium_Low"));
    }

    @Test
    void fromKeyword_acceptsHyphenatedSpellingAndNormalAlias() {
        assertEquals(Optional.of(OperationPriority.MEDIUM_HIGH), OperationPriority.fromKeyword("medium-high"));
        assertEquals(Optional.of(OperationPriority.MEDIUM), OperationPriority.fromKeyword("normal"));
    }

    @Test
    void fromKeyword_rejectsUnknownAndNull() {
        assertTrue(OperationPriority.fromKeyword("urgent").isEmpty());
        assertTrue(OperationPriority.fromKeyword(null).isEmpty());
        assertTrue(OperationPriority.fromKeyword("  ").isEmpty());
    }

    @Test
    void byId_readsBackEveryLevel() {
        for (final OperationPriority level : OperationPriority.values()) {
            assertEquals(level, OperationPriority.byId(level.id()));
        }
    }

    @Test
    void byId_fallsBackToTheDefaultForAnIdNoLevelDeclares() {
        assertEquals(OperationPriority.DEFAULT, OperationPriority.byId(-3));
        assertEquals(OperationPriority.DEFAULT, OperationPriority.byId(99));
    }

    @Test
    void fromKeyword_readsBackEveryLevelsOwnName() {
        for (final OperationPriority level : OperationPriority.values()) {
            assertEquals(Optional.of(level), OperationPriority.fromKeyword(level.serializedName()));
        }
    }

    @Test
    void raiseAndLower_saturateAtTheEnds() {
        assertEquals(OperationPriority.HIGH, OperationPriority.HIGH.raise());
        assertEquals(OperationPriority.LOW, OperationPriority.LOW.lower());
        assertEquals(OperationPriority.MEDIUM_HIGH, OperationPriority.MEDIUM.raise());
        assertEquals(OperationPriority.MEDIUM_LOW, OperationPriority.MEDIUM.lower());
    }

    @Test
    void labels_areFourCharactersAtMost() {
        for (final OperationPriority level : OperationPriority.values()) {
            assertTrue(level.label().length() <= 4, level + " label too long: " + level.label());
        }
    }

    @Test
    void default_isMedium() {
        assertEquals(OperationPriority.MEDIUM, OperationPriority.DEFAULT);
    }
}
