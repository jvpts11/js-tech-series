/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeChecksTest {

    private static final String SOURCE = "using System.*; namespace Tests; "
            + "class Shape { }\n"
            + "class Square : Shape { }\n"
            + "class Stone { }\n"
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

    @Test
    void cast_givesANumberBackAsTheKindAskedFor() {
        final TypeChecks types = new TypeChecks(PROGRAM);

        assertEquals(7L, types.cast(7, "long", 1));
        assertEquals(7, types.cast(7.0, "int", 1));
        assertEquals('A', types.cast(65, "char", 1));
        assertEquals(true, types.cast(true, "bool", 1));
    }

    @Test
    void cast_haltsOnWhatIsNoNumberAndOnNothing() {
        final TypeChecks types = new TypeChecks(PROGRAM);

        final Halt wrong = assertThrows(Halt.class, () -> types.cast("iron", "int", 4));
        assertEquals(Halt.Reason.BAD_CAST, wrong.reason());
        assertEquals("this is not a int", wrong.getMessage());
        final Halt nothing = assertThrows(Halt.class, () -> types.cast(null, "int", 4));
        assertEquals("there is nothing here to make a int", nothing.getMessage());
    }

    @Test
    void cast_letsAnObjectThroughAsWhatItIsOrStandsOnAndNothingThroughAsAnything() {
        final TypeChecks types = new TypeChecks(PROGRAM);
        final Values.Obj square = new Values.Obj(typeNamed("Square"));

        assertSame(square, types.cast(square, typeNamed("Shape"), 1));
        assertSame(square, types.cast(square, typeNamed("Square"), 1));
        assertNull(types.cast(null, typeNamed("Shape"), 1));
        final Halt unrelated = assertThrows(Halt.class, () -> types.cast(square, typeNamed("Stone"), 9));
        assertEquals(Halt.Reason.BAD_CAST, unrelated.reason());
    }

    @Test
    void isInstance_answersForEveryKindOfValue() {
        final TypeChecks types = new TypeChecks(PROGRAM);

        assertTrue(types.isInstance("iron", "string"));
        assertTrue(types.isInstance(3, "int"));
        assertFalse(types.isInstance(3, "long"));
        assertTrue(types.isInstance(4.5, "object"));
        assertFalse(types.isInstance(null, "object"), "nothing is of no type");
        assertTrue(types.isInstance(new Values.Obj(typeNamed("Square")), typeNamed("Shape")));
        assertFalse(types.isInstance(new Values.Obj(typeNamed("Shape")), typeNamed("Square")));
        assertTrue(types.isInstance(new Values.Arr("int", 2), "int[]"));
        assertFalse(types.isInstance(new Values.Arr("int", 2), "long[]"));
        assertTrue(types.isInstance(new Values.ListValue(), "List<int>"));
    }
}
