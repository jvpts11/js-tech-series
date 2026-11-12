/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiagnosticBagTest {

    @Test
    void add_keepsAnErrorThatComesAfterTheCapFilledWithWarnings() {
        final DiagnosticBag bag = new DiagnosticBag("Main.sg");
        for (int i = 0; i < DiagnosticBag.MAX_DIAGNOSTICS; i++) {
            bag.warning(i + 1, 1, SigmaError.DIRECTIVE_SAYS, "still to do");
        }
        bag.error(DiagnosticBag.MAX_DIAGNOSTICS + 1, 1, SigmaError.MALFORMED_NUMBER, "12x");
        assertTrue(bag.wasCapped(), "the cap was reached");
        assertTrue(bag.hasErrors(), "and the error is kept, so the program does not build");
        assertFalse(DiagnosticBag.noErrors(bag.sorted()), "what is reported holds the error too");
        assertEquals(DiagnosticBag.MAX_DIAGNOSTICS, bag.size(), "in a warning's place, the cap kept");
    }
}
