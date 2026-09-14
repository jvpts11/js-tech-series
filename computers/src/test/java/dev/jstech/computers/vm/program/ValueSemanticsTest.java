/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValueSemanticsTest {

    private static final String SOURCE = "using System.*; namespace Tests; "
            + "struct Point { public int X; public int Y; }\n"
            + "struct Line { public Point A; public Point B; }\n"
            + "class Box { public int X; }\n"
            + "class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n}\n";

    private static final ProgramImage PROGRAM = load();

    private static ProgramImage load() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs", SOURCE)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    /** The name a type goes by in the loaded program, whatever its namespace makes of it. */
    private static String typeNamed(final String simple) {
        for (final TypeImage type : PROGRAM.types()) {
            if (type.name().equals(simple) || type.name().endsWith("." + simple)) {
                return type.name();
            }
        }
        throw new AssertionError("no type " + simple + " in the program");
    }

    private static Values.Obj point(final int x, final int y) {
        final Values.Obj made = new Values.Obj(typeNamed("Point"));
        made.set("X", x);
        made.set("Y", y);
        return made;
    }

    private static Values.Obj line(final Values.Obj a, final Values.Obj b) {
        final Values.Obj made = new Values.Obj(typeNamed("Line"));
        made.set("A", a);
        made.set("B", b);
        return made;
    }

    private static Values.Obj box(final int x) {
        final Values.Obj made = new Values.Obj(typeNamed("Box"));
        made.set("X", x);
        return made;
    }

    @Test
    void truth_takesAOneForTrueAndAZeroForFalse() {
        assertTrue(ValueSemantics.truth(true));
        assertTrue(ValueSemantics.truth(1));
        assertTrue(ValueSemantics.truth("iron"), "anything that is there counts as true");
        assertFalse(ValueSemantics.truth(false));
        assertFalse(ValueSemantics.truth(0));
        assertFalse(ValueSemantics.truth(0L));
        assertFalse(ValueSemantics.truth(null));
    }

    @Test
    void same_holdsNothingTheSameOnlyAsNothing() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertTrue(values.same(null, null));
        assertFalse(values.same(null, 0));
        assertFalse(values.same(box(1), null));
    }

    @Test
    void same_readsABoolAndTheNumberStandingForIt() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertTrue(values.same(true, 1));
        assertTrue(values.same(0, false));
        assertFalse(values.same(true, 0));
    }

    @Test
    void same_comparesTextByWhatItSaysAndNumbersByValueWhateverTheirKind() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertTrue(values.same("iron", new String("iron")));
        assertFalse(values.same("iron", "copper"));
        assertTrue(values.same(1, 1L));
        assertTrue(values.same(2.0, 2));
        assertFalse(values.same(1, 2));
    }

    @Test
    void same_comparesTwoStructsFieldByField() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertTrue(values.same(point(1, 2), point(1, 2)));
        assertFalse(values.same(point(1, 2), point(1, 3)));
    }

    @Test
    void same_comparesStructsHoldingStructsAllTheWayDown() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertTrue(values.same(line(point(0, 0), point(4, 5)), line(point(0, 0), point(4, 5))));
        assertFalse(values.same(line(point(0, 0), point(4, 5)), line(point(0, 0), point(4, 6))));
    }

    @Test
    void same_holdsTwoClassObjectsTheSameOnlyWhenTheyAreOne() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);
        final Values.Obj one = box(7);

        assertTrue(values.same(one, one));
        assertFalse(values.same(one, box(7)), "a class is compared by which object it is, not by what it holds");
    }

    @Test
    void same_neverHoldsValuesOfDifferentStructTypesTheSame() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);

        assertFalse(values.same(point(0, 0), line(null, null)));
    }

    @Test
    void same_leavesTheFieldsItComparesAsTheyWere() {
        final ValueSemantics values = new ValueSemantics(PROGRAM);
        final Values.Obj left = point(1, 2);
        final Values.Obj right = point(1, 2);

        values.same(left, right);

        assertTrue(left.all().equals(point(1, 2).all()) && right.all().equals(point(1, 2).all()));
    }
}
