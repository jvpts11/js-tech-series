/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StableNamesTest {

    @Test
    void find_returnsTheConstantThatDeclaresTheName() {
        final StableNames<Drive> names = StableNames.of(Drive.class);
        assertEquals(Drive.FLOPPY, names.find("floppy"));
        assertEquals(Drive.OPTICAL, names.find("cd_rom2"));
    }

    @Test
    void find_isExactAboutCaseAndAbsentNames() {
        final StableNames<Drive> names = StableNames.of(Drive.class);
        assertNull(names.find("FLOPPY"));
        assertNull(names.find("OPTICAL"));
        assertNull(names.find(""));
        assertNull(names.find(null));
    }

    @Test
    void byName_fallsBackForANameNoConstantDeclares() {
        final StableNames<Drive> names = StableNames.of(Drive.class);
        assertEquals(Drive.FLOPPY, names.byName("tape", Drive.FLOPPY));
        assertEquals(Drive.OPTICAL, names.byName("cd_rom2", Drive.FLOPPY));
    }

    @Test
    void names_listEveryNameInDeclarationOrder() {
        final StableNames<Drive> names = StableNames.of(Drive.class);
        assertEquals(List.of("floppy", "cd_rom2"), List.copyOf(names.names()));
        assertEquals("Drive", names.typeName());
    }

    @Test
    void of_rejectsANameTwoConstantsDeclare() {
        assertThrows(IllegalStateException.class, () -> StableNames.of(Twice.class));
    }

    @Test
    void of_rejectsANameWithCapitalsOrPunctuation() {
        assertThrows(IllegalStateException.class, () -> StableNames.of(Loud.class));
        assertThrows(IllegalStateException.class, () -> StableNames.of(Hyphenated.class));
        assertThrows(IllegalStateException.class, () -> StableNames.of(Blank.class));
    }

    private enum Drive implements IStableName {
        FLOPPY("floppy"),
        OPTICAL("cd_rom2");

        private final String serializedName;

        Drive(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    private enum Twice implements IStableName {
        ONE,
        OTHER;

        @Override
        public String serializedName() {
            return "same";
        }
    }

    private enum Loud implements IStableName {
        ONLY;

        @Override
        public String serializedName() {
            return "Only";
        }
    }

    private enum Hyphenated implements IStableName {
        ONLY;

        @Override
        public String serializedName() {
            return "one-only";
        }
    }

    private enum Blank implements IStableName {
        ONLY;

        @Override
        public String serializedName() {
            return "";
        }
    }
}
