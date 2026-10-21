/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LicenceHeadersTest {

    private static final String HEADED = """
            /*
             * SPDX-License-Identifier: MIT
             *
             * Copyright (C) 2026 Jane Doe
             *
             * This file is part of Demo.
             */
            package com.demo;

            final class Kiln {
            }
            """;

    private static final String BARE = """
            package com.demo;

            final class Kiln {
            }
            """;

    @Test
    void licenceOf_readsTheSpdxIdOfTheOpeningBlockComment() {
        assertEquals(Optional.of("MIT"), LicenceHeaders.licenceOf(HEADED));
    }

    @Test
    void licenceOf_findsNoneInASourceWithNoHeaderOrAHeaderLaterOn() {
        assertEquals(Optional.empty(), LicenceHeaders.licenceOf(BARE));
        assertEquals(Optional.empty(), LicenceHeaders.licenceOf("package a;\n/* SPDX-License-Identifier: MIT */\n"));
        assertEquals(Optional.empty(), LicenceHeaders.licenceOf("// SPDX-License-Identifier: MIT\npackage a;\n"));
    }

    @Test
    void problemOf_saysAHeaderMissingOrNamingAnotherLicence() {
        assertEquals(Optional.of("has no licence header"), LicenceHeaders.problemOf(BARE, "MIT"));
        assertEquals(Optional.of("names MIT, not LGPL-3.0-only"), LicenceHeaders.problemOf(HEADED, "LGPL-3.0-only"));
        assertEquals(Optional.empty(), LicenceHeaders.problemOf(HEADED, "MIT"));
        assertEquals(Optional.empty(), LicenceHeaders.problemOf(HEADED, null));
    }

    @Test
    void withHeader_writesTheHeaderAboveASourceWithNone() {
        final String headed = LicenceHeaders.withHeader(BARE, "MIT", 2026, "Jane Doe", "Demo");

        assertEquals(HEADED, headed);
    }

    @Test
    void withHeader_leavesASourceWithAHeaderAsItIs() {
        final String other = HEADED.replace("MIT", "Apache-2.0");

        assertEquals(other, LicenceHeaders.withHeader(other, "MIT", 2026, "Jane Doe", "Demo"));
        assertTrue(LicenceHeaders.header("MIT", 2026, "Jane Doe", "Demo").endsWith(" */\n"));
    }
}
