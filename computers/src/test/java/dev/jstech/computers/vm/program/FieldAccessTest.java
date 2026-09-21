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
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.computers.vm.listing.TypeName;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.SystemApi;
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
        return new FieldAccess(process, process.heap0(), PROGRAM);
    }

    private static Frame frame() {
        return new Frame(PROGRAM.method(PROGRAM.entryPoint(), "OnTick", List.of()), null);
    }

    /**
     * A machine that answers what a program reads of it with functions it binds. The functions say nothing of prices,
     * so whatever a program is charged for a read comes from what the system declares.
     */
    private static final class Machine implements IHost {

        @Override
        public long tick() {
            return 0;
        }

        @Override
        public long dayTime() {
            return 0;
        }

        @Override
        public long day() {
            return 0;
        }

        @Override
        public IWorldFunction bind(final MemberId id) {
            if (!"Computer".equals(id.owner())) {
                return null;
            }
            return switch (id.name()) {
                case "Name" -> (call, target, arguments, line) -> "Workshop";
                case "RamMb" -> (call, target, arguments, line) -> 4096;
                case "Cpu" -> (call, target, arguments, line) -> {
                    final Values.Obj cpu = new Values.Obj("CpuInfo");
                    cpu.set("Mhz", 2400);
                    cpu.set("Cores", 2);
                    cpu.set("Era", "legacy");
                    return cpu;
                };
                default -> null;
            };
        }
    }

    /** Runs a script whose tick is those lines through on that machine, with the machine in reach. */
    private static Process run(final IHost host, final String tick) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs",
                "using System.*; using System.Machine.*; namespace Tests; enum Tint { Red }\n"
                        + "class Monitor : IScript {\n"
                        + "    public void OnInit() { }\n"
                        + "    public void OnTick() {\n" + tick + "\n    }\n"
                        + "    public void OnDestroy() { }\n}\n")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        final ProgramImage program = ProgramImage.of(listing);
        final Process process = new Process(program, 64L * 1024, host);
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(1_000_000);
        return process;
    }

    @Test
    void storeThenLoad_writesAndReadsAnObjectsField() {
        final FieldAccess access = access();
        final Frame frame = frame();
        final Values.Obj counter = new Values.Obj(typeNamed("Counter"));
        final IOperand.Field count = new IOperand.Field(typeNamed("Counter"), "Count");

        frame.push(counter);
        frame.push(5);
        access.store(frame, PROGRAM.valueSite(count, Opcode.STFLD), 1);
        frame.push(counter);
        access.load(frame, PROGRAM.valueSite(count, Opcode.LDFLD), 2);

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

        final ProgramImage.ValueSite site = PROGRAM.valueSite(count, Opcode.STFLD);
        final Halt halt = assertThrows(Halt.class, () -> access.store(frame, site, 3));

        assertEquals(Halt.Reason.NO_OBJECT, halt.reason());
        assertEquals("there is no object to write Count on", halt.getMessage());
    }

    @Test
    void storeStaticThenLoadStatic_keepsOneHolderPerType() {
        final FieldAccess access = access();
        final Frame frame = frame();
        final IOperand.Field made = new IOperand.Field(typeNamed("Counter"), "Made");

        frame.push(3);
        access.storeStatic(frame, PROGRAM.valueSite(made, Opcode.STSFLD), 1);
        access.loadStatic(frame, PROGRAM.valueSite(made, Opcode.LDSFLD), 2);

        assertEquals(3, frame.pop());
        assertEquals(List.of(typeNamed("Counter")), List.copyOf(access.statics().keySet()));
        assertSame(access.statics(typeNamed("Counter")), access.statics(typeNamed("Counter")));
    }

    @Test
    void loadStatic_readsOneOfAnEnumsValues() {
        final FieldAccess access = access();
        final Frame frame = frame();

        access.loadStatic(frame, PROGRAM.valueSite(new IOperand.Field(typeNamed("Tint"), "Red"), Opcode.LDSFLD), 1);

        assertEquals(PROGRAM.type(typeNamed("Tint")).values().get("Red"), frame.pop());
    }

    @Test
    void loadStatic_asksTheProcessForTheThreadAsking() {
        final FieldAccess access = access();
        final Frame frame = frame();

        access.loadStatic(frame, PROGRAM.valueSite(new IOperand.Field("Thread", "Current"), Opcode.LDSFLD), 1);

        final Object token = frame.pop();
        assertTrue(token instanceof Values.Obj thread && "Thread".equals(thread.type())
                && Integer.valueOf(1).equals(thread.get("Id")), () -> String.valueOf(token));
    }

    @Test
    void loadStatic_asksTheProcessForTheProgramsName() {
        final FieldAccess access = access();
        final Frame frame = frame();

        access.loadStatic(frame, PROGRAM.valueSite(new IOperand.Field("Program", "Name"), Opcode.LDSFLD), 1);

        assertEquals("", frame.pop());
    }

    @Test
    void load_readsWhatTheLanguagesCoreKeepsOnAText() {
        final FieldAccess access = access();
        final Frame frame = frame();
        frame.push("iron");

        access.load(frame, PROGRAM.valueSite(new IOperand.Field("string", "Length"), Opcode.LDFLD), 1);

        assertEquals(4, frame.pop());
    }

    @Test
    void load_haltsOnACoreValueAskedOfSomethingThatDoesNotKeepIt() {
        final FieldAccess access = access();
        final Frame frame = frame();
        frame.push(7);
        final ProgramImage.ValueSite length = PROGRAM.valueSite(new IOperand.Field("string", "Length"), Opcode.LDFLD);

        final Halt halt = assertThrows(Halt.class, () -> access.load(frame, length, 2));

        assertEquals(Halt.Reason.NO_SUCH_MEMBER, halt.reason());
        assertEquals("there is no Length to read here", halt.getMessage());
    }

    @Test
    void loadStatic_readsAValueOfTheWorldThroughWhatTheHostBound() {
        final Process process = run(new Machine(), """
                        Console.PrintLine(Computer.Name + " has " + Computer.RamMb + " MB");
                """);

        assertEquals(Process.State.FINISHED, process.state(), String.valueOf(process.message()));
        assertEquals(List.of("Workshop has 4096 MB"), process.console());
    }

    @Test
    void loadStatic_handsTheProgramTheRecordAValueOfTheWorldIs() {
        final Process process = run(new Machine(), """
                        CpuInfo cpu = Computer.Cpu;
                        Console.PrintLine(cpu.Cores + " cores at " + cpu.Mhz + " (" + cpu.Era + ")");
                """);

        assertEquals(Process.State.FINISHED, process.state(), String.valueOf(process.message()));
        assertEquals(List.of("2 cores at 2400 (legacy)"), process.console());
    }

    @Test
    void loadStatic_chargesAValueOfTheWorldWhatTheSystemDeclares() {
        final long own = run(new Machine(), "        Tint tint = Tint.Red;").spent();
        final long world = run(new Machine(), "        int ram = Computer.RamMb;").spent();

        // Both lines read a value from a type and keep it, and a value of the program's own costs nothing beyond that.
        assertEquals(SystemApi.member("Computer", "RamMb", List.of()).cost().at(0, 0), world - own);
    }

    @Test
    void loadStatic_leavesAValueOfTheWorldNoHostBindsToTheHostsOtherDoor() {
        final Process process = run(IHost.still(), "        string name = Computer.Name;");

        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().contains("Computer"), process.message());
    }

    @Test
    void statics_cannotBeChangedFromOutside() {
        final FieldAccess access = access();
        access.statics(typeNamed("Counter"));

        assertThrows(UnsupportedOperationException.class, () -> access.statics().clear());
    }

    /*
     * A field of a type the program itself declares is answered by the program, so nothing is looked for outside
     * and nothing is said about it. Asking the wrong question here reaches for the world instead of the object,
     * which is a program a machine refuses to start and, where it does not refuse, one that reads the wrong thing.
     */
    @Test
    void valueSite_readsAFieldOfTheProgramsOwnTypeAsTheProgramsOwn() {
        final ProgramImage.ValueSite site = PROGRAM.valueSite(
                new IOperand.Field(new TypeName(typeNamed("Counter")), "Count"), Opcode.LDFLD);

        assertTrue(site.own());
        assertNull(site.handled());
        assertEquals(-1, site.world());
    }

    @Test
    void problems_saysNothingAboutAProgramThatUsesItsOwnFields() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Own.sgs",
                "using System.*; namespace Tests;\n"
                        + "class Box { public int Held; public static int Shared; }\n"
                        + "class Own {\n"
                        + "    static void Main() {\n"
                        + "        Box box = new Box();\n"
                        + "        box.Held = 1;\n"
                        + "        Box.Shared = box.Held;\n"
                        + "        Console.PrintLine(\"\" + box.Held + Box.Shared);\n"
                        + "    }\n}\n")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));

        final List<ListingProblem> problems = ProgramImage.of(new AsmReader(built.assembly()).read()).problems();

        assertEquals(List.of(), problems,
                () -> String.join("\n", problems.stream().map(ListingProblem::format).toList()));
    }
}
