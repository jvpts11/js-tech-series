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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObjectMakingTest {

    private static final String SOURCE = "using System.*; namespace Tests; "
            + "struct Point { public int X; public int Y; }\n"
            + "struct Line { public Point A; }\n"
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

    private static ObjectMaking objects(final Process process) {
        final CallDispatch calls = new CallDispatch(process, process.heap0(), process.library(), PROGRAM);
        return new ObjectMaking(process.heap0(), process.library(), PROGRAM, calls);
    }

    private static Process process() {
        return new Process(PROGRAM, 64L * 1024, IHost.still());
    }

    private static Frame frame() {
        return new Frame(PROGRAM.method(PROGRAM.entryPoint(), "OnTick", List.of()), null);
    }

    @Test
    void instance_makesAnObjectOfTheProgramsTypeOnItsHeap() {
        final Process process = process();

        final Object made = objects(process).instance(typeNamed("Box"), List.of(), 1);

        assertTrue(made instanceof Values.Obj box && box.type().equals(typeNamed("Box")), () -> String.valueOf(made));
        assertTrue(process.heap0().bytesOf(made) > 0, "it weighs on the program's heap");
    }

    @Test
    void copyOf_makesANewStructHoldingTheSameAllTheWayDown() {
        final ObjectMaking objects = objects(process());
        final Values.Obj point = new Values.Obj(typeNamed("Point"));
        point.set("X", 1);
        point.set("Y", 2);
        final Values.Obj line = new Values.Obj(typeNamed("Line"));
        line.set("A", point);

        final Values.Obj copy = (Values.Obj) objects.copyOf(line, 1);

        assertNotSame(line, copy);
        assertNotSame(point, copy.get("A"), "a struct inside a struct is copied too");
        assertEquals(1, ((Values.Obj) copy.get("A")).get("X"));
        assertEquals(2, ((Values.Obj) copy.get("A")).get("Y"));
    }

    @Test
    void copyOf_handsBackAClassObjectANumberOrNothingAsItIs() {
        final ObjectMaking objects = objects(process());
        final Values.Obj box = new Values.Obj(typeNamed("Box"));

        assertSame(box, objects.copyOf(box, 1));
        assertEquals(5, objects.copyOf(5, 1));
        assertNull(objects.copyOf(null, 1));
    }

    @Test
    void newArray_makesAnArrayOfTheLengthAskedAndRefusesANegativeOne() {
        final ObjectMaking objects = objects(process());
        final Frame frame = frame();

        frame.push(3);
        objects.newArray(frame, new IOperand.Type("int"), 1);
        assertEquals(3, ((Values.Arr) frame.pop()).length());

        frame.push(-1);
        final Halt halt = assertThrows(Halt.class, () -> objects.newArray(frame, new IOperand.Type("int"), 2));
        assertEquals(Halt.Reason.OUT_OF_RANGE, halt.reason());
        assertEquals("an array cannot have -1 places", halt.getMessage());
    }

    @Test
    void storeThenLoadElement_writesAndReadsAPlace() {
        final ObjectMaking objects = objects(process());
        final Frame frame = frame();
        final Values.Arr numbers = new Values.Arr("int", 2);

        frame.push(numbers);
        frame.push(1);
        frame.push(9);
        objects.storeElement(frame, 1);
        frame.push(numbers);
        frame.push(1);
        objects.loadElement(frame, 2);

        assertEquals(9, frame.pop());
    }

    @Test
    void array_haltsOnWhatIsNoArray() {
        final ObjectMaking objects = objects(process());

        final Halt halt = assertThrows(Halt.class, () -> objects.array("iron", 4));

        assertEquals(Halt.Reason.NO_OBJECT, halt.reason());
        assertEquals("there is no array here", halt.getMessage());
    }
}
