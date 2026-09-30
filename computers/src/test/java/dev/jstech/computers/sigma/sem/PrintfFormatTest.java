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

import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A format as printf reads one: the text between the holes, and what each hole takes. */
class PrintfFormatTest {

    private static PrintfFormat.Hole hole(final char letter, final PrintfFormat.Wants wants) {
        return new PrintfFormat.Hole(letter, wants, "%" + letter, "%" + letter, 1);
    }

    private static PrintfFormat.Hole only(final String format) {
        final PrintfFormat.Read read = PrintfFormat.read(format);
        assertNull(read.problem(), format);
        return (PrintfFormat.Hole) read.pieces().getFirst();
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
        assertEquals(new PrintfFormat.Hole('d', PrintfFormat.Wants.WHOLE_NUMBER, "%ld", "%d", 1), only("%ld"));
        assertEquals(new PrintfFormat.Hole('f', PrintfFormat.Wants.FRACTION, "%lf", "%f", 1), only("%lf"));
        assertTrue(only("%ld").plain());
    }

    /** The second version reads the rest of what C wrote in a hole, and says so by the version of each hole. */
    @Test
    void read_takesWidthsPrecisionsFlagsAndTheOtherLettersFromTheSecondVersion() {
        final PrintfFormat.Read read = PrintfFormat.read("%-12s %5d %05.1f %#x %X %o %u %e %E %+i %hd %lld");
        assertNull(read.problem());
        final List<String> specs = read.pieces().stream().filter(PrintfFormat.Hole.class::isInstance)
                .map(piece -> ((PrintfFormat.Hole) piece).spec()).toList();
        assertEquals(List.of("%-12s", "%5d", "%05.1f", "%#x", "%X", "%o", "%u", "%e", "%E", "%+i", "%d", "%d"),
                specs);
        for (final Object piece : read.pieces()) {
            if (piece instanceof PrintfFormat.Hole hole) {
                assertEquals(2, hole.since(), hole.written());
            }
        }
        assertEquals(PrintfFormat.Wants.WHOLE_NUMBER, only("%x").wants());
        assertEquals(PrintfFormat.Wants.FRACTION, only("%e").wants());
        assertTrue(only("%hd").plain(), "a length letter asks for nothing, though the first version did not read it");
        assertEquals("%hd", only("%hd").written());
    }

    @Test
    void read_aWidthFromAValueOrOneTooWideIsAProblem() {
        assertTrue(PrintfFormat.read("%*d").problem().english().contains("cannot come from a value"));
        assertTrue(PrintfFormat.read("%1000d").problem().english().contains("up to 999"));
        assertTrue(PrintfFormat.read("%.1000f").problem().english().contains("up to 999"));
        assertNull(PrintfFormat.read("%999d").problem());
    }

    @Test
    void read_twoPercentSignsAreOneAndNoHole() {
        final PrintfFormat.Read read = PrintfFormat.read("100%% of %d");
        assertEquals(List.of("100% of ", hole('d', PrintfFormat.Wants.WHOLE_NUMBER)), read.pieces());
        assertEquals(1, read.holes());
    }

    @Test
    void read_aLetterItDoesNotHaveIsNamedWithTheOnesItDoes() {
        final Text read = PrintfFormat.read("%q").problem();
        assertNotNull(read);
        final String problem = read.english();
        assertTrue(problem.contains("'%q'") && problem.contains("%d, %i, %u, %x"), problem);
        assertNotNull(PrintfFormat.read("%5q").problem(), "a width does not make a letter one");
        assertNotNull(PrintfFormat.read("%lhd").problem(), "nor two lengths that are no length");
    }

    @Test
    void read_aPercentSignAtTheVeryEndIsAProblem() {
        assertNotNull(PrintfFormat.read("done %").problem());
    }
}
