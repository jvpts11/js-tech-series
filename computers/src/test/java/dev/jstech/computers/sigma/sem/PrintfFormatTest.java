/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** A format as printf reads one: the text between the holes, and what each hole takes. */
class PrintfFormatTest {

    private static PrintfFormat.Hole hole(final char letter, final PrintfFormat.Wants wants) {
        return new PrintfFormat.Hole(letter, wants);
    }

    @Test
    void read_aFormatWithNoHolesIsItsOwnText() {
        final PrintfFormat.Read read = PrintfFormat.read("hello\n");
        assertNull(read.problem());
        assertEquals(List.of("hello\n"), read.pieces());
        assertEquals(0, read.holes());
    }

    @Test
    void read_keepsTheTextAndTheHolesInTheOrderTheyCome() {
        final PrintfFormat.Read read = PrintfFormat.read("%s has %d items, %f full\n");
        assertNull(read.problem());
        assertEquals(List.of(hole('s', PrintfFormat.Wants.TEXT), " has ",
                hole('d', PrintfFormat.Wants.WHOLE_NUMBER), " items, ",
                hole('f', PrintfFormat.Wants.FRACTION), " full\n"), read.pieces());
        assertEquals(3, read.holes());
    }

    @Test
    void read_takesTheLettersTheHandWrites() {
        assertEquals(PrintfFormat.Wants.WHOLE_NUMBER, ((PrintfFormat.Hole) PrintfFormat.read("%i").pieces()
                .getFirst()).wants());
        assertEquals(PrintfFormat.Wants.CHARACTER, ((PrintfFormat.Hole) PrintfFormat.read("%c").pieces()
                .getFirst()).wants());
        // The l of a long means nothing here and is taken all the same, since the hand writes it.
        assertEquals(hole('d', PrintfFormat.Wants.WHOLE_NUMBER), PrintfFormat.read("%ld").pieces().getFirst());
        assertEquals(hole('f', PrintfFormat.Wants.FRACTION), PrintfFormat.read("%lf").pieces().getFirst());
    }

    @Test
    void read_twoPercentSignsAreOneAndNoHole() {
        final PrintfFormat.Read read = PrintfFormat.read("100%% of %d");
        assertEquals(List.of("100% of ", hole('d', PrintfFormat.Wants.WHOLE_NUMBER)), read.pieces());
        assertEquals(1, read.holes());
    }

    @Test
    void read_aLetterItDoesNotHaveIsNamedWithTheOnesItDoes() {
        final String problem = PrintfFormat.read("%q").problem();
        assertNotNull(problem);
        assertTrue(problem.contains("'%q'") && problem.contains("%d, %f, %s, %c"), problem);
    }

    @Test
    void read_aWidthOrAPrecisionIsSaidNotToBeHere() {
        for (final String format : List.of("%5d", "%.2f", "%-8s")) {
            final String problem = PrintfFormat.read(format).problem();
            assertNotNull(problem, format);
            assertTrue(problem.contains("no widths or precisions"), problem);
        }
    }

    @Test
    void read_aPercentSignAtTheVeryEndIsAProblem() {
        assertNotNull(PrintfFormat.read("done %").problem());
    }
}
