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

class FieldAccessTest {

    private static final String SOURCE = "using System.*; namespace Tests; "
            + "enum Tint { Red, Green }\n"
            + "class Counter { public int Count; }\n"
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

    private static FieldAccess access() {
        final Process process = new Process(PROGRAM, 64L * 1024, IHost.still());
        return new FieldAccess(process, process.heap0(), process.library(), PROGRAM);
    }

    private static Frame frame() {
        return new Frame(PROGRAM.method(PROGRAM.entryPoint(), "OnTick", List.of()), null);
    }

    @Test
    void storeThenLoad_writesAndReadsAnObjectsField() {
        final FieldAccess access = access();
        final Frame frame = frame();
        final Values.Obj counter = new Values.Obj(typeNamed("Counter"));
        final IOperand.Field count = new IOperand.Field(typeNamed("Counter"), "Count");

        frame.push(counter);
        frame.push(5);
        access.store(frame, count, 1);
        frame.push(counter);
        access.load(frame, count, 2);

        assertEquals(5, frame.pop());
        assertEquals(5, counter.get("Count"));
    }

    @Test
    void store_haltsOnSomethingThatIsNoObject() {
        final FieldAccess access = access();
        final Frame frame = frame();
        final IOperand.Field count = new IOperand.Field(typeNamed("Counter"), "Count");
        frame.push("iron");
        frame.push(1);

        final Halt halt = assertThrows(Halt.class, () -> access.store(frame, count, 3));

        assertEquals(Halt.Reason.NO_OBJECT, halt.reason());
        assertEquals("there is no object to write Count on", halt.getMessage());
    }

    @Test
    void storeStaticThenLoadStatic_keepsOneHolderPerType() {
        final FieldAccess access = access();
        final Frame frame = frame();
        final IOperand.Field made = new IOperand.Field(typeNamed("Counter"), "Made");

        frame.push(3);
        access.storeStatic(frame, made, 1);
        access.loadStatic(frame, made, 2);

        assertEquals(3, frame.pop());
        assertEquals(List.of(typeNamed("Counter")), List.copyOf(access.statics().keySet()));
        assertSame(access.statics(typeNamed("Counter")), access.statics(typeNamed("Counter")));
    }

    @Test
    void loadStatic_readsOneOfAnEnumsValues() {
        final FieldAccess access = access();
        final Frame frame = frame();

        access.loadStatic(frame, new IOperand.Field(typeNamed("Tint"), "Red"), 1);

        assertEquals(PROGRAM.type(typeNamed("Tint")).values().get("Red"), frame.pop());
    }

    @Test
    void loadStatic_asksTheProcessForTheThreadAsking() {
        final FieldAccess access = access();
        final Frame frame = frame();

        access.loadStatic(frame, new IOperand.Field("Thread", "Current"), 1);

        final Object token = frame.pop();
        assertTrue(token instanceof Values.Obj thread && "Thread".equals(thread.type())
                && Integer.valueOf(1).equals(thread.get("Id")), () -> String.valueOf(token));
    }

    @Test
    void statics_cannotBeChangedFromOutside() {
        final FieldAccess access = access();
        access.statics(typeNamed("Counter"));

        assertThrows(UnsupportedOperationException.class, () -> access.statics().clear());
    }
}
