/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AsmRoundTripTest {

    private AsmReader reader;

    private AsmProgram read(final String text) {
        this.reader = new AsmReader(text);
        return this.reader.read();
    }

    private List<String> codes() {
        return this.reader.problems().stream().map(ListingProblem::code).toList();
    }

    /** A program using every directive and a spread of operand shapes. */
    private static AsmProgram sample() {
        final AsmProgram program = new AsmProgram();
        program.setEntryPoint("Monitor", Shape.SCRIPT);

        final AsmType script = new AsmType(AsmType.Kind.INTERFACE, "IScript");
        script.addMethod(new AsmMethod("OnInit", "void", List.of(), false, 0, null));
        script.addMethod(new AsmMethod("OnTick", "void", List.of(), false, 0, null));
        program.addType(script);

        final AsmType level = new AsmType(AsmType.Kind.ENUM, "LogLevel");
        level.addValue(new AsmType.Value("INFO", 0));
        level.addValue(new AsmType.Value("WARN", 1));
        program.addType(level);

        final AsmType finder = new AsmType(AsmType.Kind.DELEGATE, "Finder");
        finder.setInvoke(new AsmMethod("Invoke", "bool", List.of("string", "out int"), false, 0, null));
        program.addType(finder);

        final AsmType monitor = new AsmType(AsmType.Kind.CLASS, "Monitor");
        monitor.addBase("IScript");
        monitor.addField(new AsmType.Field("threshold", "int", false));
        monitor.addField(new AsmType.Field("counts", "Map<string, int>", false));
        monitor.addField(new AsmType.Field("started", "bool", true));
        monitor.addEvent(new AsmType.Event("Changed", "Finder"));
        monitor.addMethod(new AsmMethod("OnInit", "void", List.of(), false, 0, List.of(
                Instruction.of(Opcode.RET))));
        monitor.addMethod(new AsmMethod("OnTick", "void", List.of(), false, 3, List.of(
                Instruction.of(Opcode.LDTHIS),
                Instruction.of(Opcode.LDFLD, new IOperand.Field(null, "counts")),
                Instruction.of(Opcode.LDSTR, new IOperand.Text("minecraft:diamond")),
                Instruction.of(Opcode.CALL, new IOperand.Method("Map", "TryGet",
                        List.of("string", "out int"), "bool")),
                Instruction.of(Opcode.STLOC, new IOperand.Slot(1)),
                Instruction.of(Opcode.BRFALSE, new IOperand.Label("L1")),
                Instruction.of(Opcode.LDLOC, new IOperand.Slot(1)),
                Instruction.of(Opcode.LDTHIS),
                Instruction.of(Opcode.LDFLD, new IOperand.Field(null, "threshold")),
                Instruction.of(Opcode.BGE, new IOperand.Label("L1")),
                Instruction.of(Opcode.LDC_I4, new IOperand.I4(1)).saying("LogLevel.WARN"),
                Instruction.of(Opcode.LDSTR, new IOperand.Text("low; and \"quoted\"")),
                Instruction.of(Opcode.CALL, new IOperand.Method("Mainframe", "Log",
                        List.of("int", "string"), "void")),
                Instruction.of(Opcode.LDC_I8, new IOperand.I8(9000000000L)),
                Instruction.of(Opcode.LDC_R4, new IOperand.R4(1.5f)),
                Instruction.of(Opcode.LDC_R8, new IOperand.R8(2.25)),
                Instruction.of(Opcode.CONV_R8),
                Instruction.of(Opcode.NEWOBJ, new IOperand.Constructor("Monitor", List.of("int"))),
                Instruction.of(Opcode.NEWARR, new IOperand.Type("int")),
                Instruction.of(Opcode.CASTCLASS, new IOperand.Type("List<string>")),
                Instruction.of(Opcode.LDSFLD, new IOperand.Field("LogLevel", "WARN")),
                Instruction.of(Opcode.SYS, new IOperand.Text("Network.Current")),
                Instruction.of(Opcode.POP),
                Instruction.of(Opcode.RET).labelled("L1"))));
        program.addType(monitor);
        return program;
    }

    /** A small program whose listing is short enough to write out line by line. */
    private static AsmProgram golden() {
        final AsmProgram program = new AsmProgram();
        program.setEntryPoint("Monitor", Shape.SCRIPT);
        final AsmType monitor = new AsmType(AsmType.Kind.CLASS, "Monitor");
        monitor.addBase("IScript");
        monitor.addField(new AsmType.Field("threshold", "int", false));
        monitor.addMethod(new AsmMethod("OnTick", "void", List.of(), false, 1, List.of(
                Instruction.of(Opcode.LDTHIS),
                Instruction.of(Opcode.LDFLD, new IOperand.Field(null, "threshold")),
                Instruction.of(Opcode.LDC_I4, new IOperand.I4(100)),
                Instruction.of(Opcode.BLT, new IOperand.Label("L1")),
                Instruction.of(Opcode.RET),
                Instruction.of(Opcode.LDC_I4, new IOperand.I4(1)).labelled("L1").saying("LogLevel.WARN"),
                Instruction.of(Opcode.LDSTR, new IOperand.Text("stock is low")),
                Instruction.of(Opcode.CALL, new IOperand.Method("Mainframe", "Log",
                        List.of("int", "string"), "void")),
                Instruction.of(Opcode.RET))));
        program.addType(monitor);
        return program;
    }

    @Test
    void write_laysTheListingOutInColumnsAPlayerCanFollow() {
        final String[] lines = AsmWriter.write(golden()).split("\n", -1);
        assertEquals(".asm 3", lines[0]);
        assertEquals(".arch jsc:x86", lines[1]);
        assertEquals(".start Monitor script", lines[2]);
        assertEquals("", lines[3]);
        assertEquals(".class Monitor : IScript", lines[4]);
        assertEquals(".field int threshold", lines[5]);
        assertEquals("", lines[6]);
        assertEquals(".method void OnTick() slots 1", lines[7]);
        assertEquals("    ldthis", lines[8]);
        assertEquals("    ldfld   threshold", lines[9]);
        assertEquals("    ldc.i4  100", lines[10]);
        assertEquals("    blt     L1", lines[11]);
        assertEquals("    ret", lines[12]);
        assertEquals("L1: ldc.i4  1" + " ".repeat(32) + "; LogLevel.WARN", lines[13]);
        assertEquals("    ldstr   \"stock is low\"", lines[14]);
        assertEquals("    call    Mainframe.Log(int, string) -> void", lines[15]);
        assertEquals("    ret", lines[16]);
    }

    @Test
    void write_thenRead_thenWrite_givesTheSameText() {
        final String once = AsmWriter.write(sample());
        final AsmProgram read = this.read(once);
        assertFalse(this.reader.hasProblems(), () -> String.join("\n", this.reader.problems().stream()
                .map(ListingProblem::format).toList()));
        assertEquals(once, AsmWriter.write(read));
    }

    @Test
    void read_keepsEveryPartOfTheProgram() {
        final AsmProgram read = this.read(AsmWriter.write(sample()));
        assertEquals("Monitor", read.entryPoint());
        assertEquals(4, read.types().size());

        final AsmType monitor = read.type("Monitor");
        assertEquals(List.of("IScript"), monitor.bases());
        assertEquals("Map<string, int>", monitor.fields().get(1).type());
        assertTrue(monitor.fields().get(2).isStatic());
        assertEquals("Finder", monitor.events().getFirst().type());

        final AsmMethod tick = monitor.methods().get(1);
        assertEquals(3, tick.slots());
        assertTrue(tick.hasBody());
        assertEquals("L1", tick.body().getLast().label());
        assertEquals("LogLevel.WARN", tick.body().get(10).comment());
    }

    @Test
    void read_keepsWhatEachOperandHeld() {
        final AsmType monitor = this.read(AsmWriter.write(sample())).type("Monitor");
        final List<Instruction> body = monitor.methods().get(1).body();
        assertEquals(new IOperand.Text("minecraft:diamond"), body.get(2).operand());
        assertEquals(new IOperand.Method("Map", "TryGet", List.of("string", "out int"), "bool"),
                body.get(3).operand());
        assertEquals(new IOperand.Text("low; and \"quoted\""), body.get(11).operand());
        assertEquals(new IOperand.I8(9000000000L), body.get(13).operand());
        assertEquals(new IOperand.R4(1.5f), body.get(14).operand());
        assertEquals(new IOperand.R8(2.25), body.get(15).operand());
        assertEquals(new IOperand.Constructor("Monitor", List.of("int")), body.get(17).operand());
        assertEquals(new IOperand.Type("List<string>"), body.get(19).operand());
        assertEquals(new IOperand.Field("LogLevel", "WARN"), body.get(20).operand());
    }

    @Test
    void read_keepsASignatureWithNoBodyApartFromOneWithABody() {
        final AsmProgram read = this.read(AsmWriter.write(sample()));
        assertFalse(read.type("IScript").methods().getFirst().hasBody());
        assertNull(read.type("IScript").methods().getFirst().body());
        assertTrue(read.type("Monitor").methods().getFirst().hasBody());
    }

    @Test
    void read_keepsADelegateShapeAndAnEnumsNumbers() {
        final AsmProgram read = this.read(AsmWriter.write(sample()));
        final AsmMethod invoke = read.type("Finder").invoke();
        assertNotNull(invoke);
        assertEquals("bool", invoke.returns());
        assertEquals(List.of("string", "out int"), invoke.parameters());
        assertEquals(1, read.type("LogLevel").values().get(1).number());
    }

    @Test
    void write_thenRead_copesWithAProgramThatHoldsNothing() {
        final String text = AsmWriter.write(new AsmProgram());
        assertEquals(".asm 3\n.arch jsc:x86\n", text);
        final AsmProgram read = this.read(text);
        assertFalse(this.reader.hasProblems());
        assertTrue(read.types().isEmpty());
        assertNull(read.entryPoint());
    }

    @Test
    void read_refusesAListingWithNoVersionLine() {
        this.read(".class C\n");
        assertEquals(List.of("A4001"), this.codes());
    }

    @Test
    void read_refusesAListingFromALaterVersion() {
        this.read(".asm 99\n.class C\n");
        assertEquals(List.of("A4002"), this.codes());
    }

    @Test
    void read_refusesAListingFromTooEarlyAVersion() {
        this.read(".asm " + (AsmProgram.OLDEST_VERSION - 1) + "\n.class C\n");
        assertEquals(List.of("A4012"), this.codes());
    }

    @Test
    void read_takesAListingFromAnEarlierVersionThatStillReads() {
        final AsmProgram read = this.read(".asm " + AsmProgram.OLDEST_VERSION + "\n.class C\n");
        assertFalse(this.reader.hasProblems());
        assertEquals(AsmProgram.OLDEST_VERSION, read.version());
        assertNotNull(read.type("C"));
    }

    @Test
    void read_aListingThatNamesNoArchitecture_isTheOneTheFormatFallsBackTo() {
        final AsmProgram read = this.read(".asm " + AsmProgram.OLDEST_VERSION + "\n.class C\n");
        assertEquals(AsmProgram.DEFAULT_ARCHITECTURE, read.architecture());
    }

    @Test
    void read_keepsTheArchitectureAListingNames() {
        final AsmProgram read = this.read(".asm 3\n.arch jsc:x86_64\n.class C\n");
        assertFalse(this.reader.hasProblems());
        assertEquals("jsc:x86_64", read.architecture());
    }

    @Test
    void read_remembersWhereTheArchitectureWasNamed() {
        assertEquals(2, this.read(".asm 3\n.arch jsc:x86_64\n.class C\n").architectureLine());
    }

    @Test
    void read_aListingThatNamesNoArchitecture_pointsAtItsHead() {
        assertEquals(1, this.read(".asm 2\n.class C\n").architectureLine());
    }

    @Test
    void read_refusesAnArchitectureThatIsNotANamespacedName() {
        this.read(".asm 3\n.arch x86\n.class C\n");
        assertEquals(List.of("A4006"), this.codes());
    }

    @Test
    void write_thenRead_keepsTheArchitecture() {
        final AsmProgram program = new AsmProgram();
        program.setArchitecture("other:risc64", 1);
        assertEquals("other:risc64", this.read(AsmWriter.write(program)).architecture());
    }

    @Test
    void read_findsTheOwnerOfAConstructorCallBeforeTheDoubleDot() {
        final String text = ".asm " + AsmProgram.VERSION + "\n.arch " + AsmProgram.DEFAULT_ARCHITECTURE
                + "\n\n.class Tests.Below\n\n.method void .ctor() slots 0\n"
                + "    call    Tests.Base..ctor(int) -> void\n    ret\n";
        final AsmProgram read = this.read(text);
        assertFalse(this.reader.hasProblems(), () -> String.join("\n", this.reader.problems().stream()
                .map(ListingProblem::format).toList()));
        final AsmMethod constructor = read.type("Tests.Below").methods().getFirst();
        assertEquals(AsmMethod.CONSTRUCTOR, constructor.name());
        assertEquals(new IOperand.Method("Tests.Base", AsmMethod.CONSTRUCTOR, List.of("int"), "void"),
                constructor.body().getFirst().operand());
        assertEquals(text, AsmWriter.write(read));
    }

    @Test
    void read_reportsALineItDoesNotKnow() {
        this.read(".asm 2\n.class C\n.method void M() slots 0\n    nonsense\n");
        assertEquals(List.of("A4003"), this.codes());
    }

    @Test
    void read_reportsAnInstructionGivenTheWrongThing() {
        this.read(".asm 2\n.class C\n.method void M() slots 0\n    ldc.i4\n    ret 7\n    br\n");
        assertEquals(List.of("A4004", "A4005", "A4004"), this.codes());
    }

    @Test
    void read_reportsANumberThatIsNotOne() {
        this.read(".asm 2\n.class C\n.method void M() slots 0\n    ldc.i4  nine\n");
        assertEquals(List.of("A4006"), this.codes());
    }

    @Test
    void read_reportsAnInstructionThatIsNotInAMethod() {
        this.read(".asm 2\n.class C\n    ret\n");
        assertEquals(List.of("A4008"), this.codes());
    }

    @Test
    void read_reportsADirectiveThatIsNotInAType() {
        this.read(".asm 2\n.field int x\n");
        assertEquals(List.of("A4009"), this.codes());
    }

    @Test
    void read_reportsABranchToALabelNothingCarries() {
        this.read(".asm 2\n.class C\n.method void M() slots 0\n    br      L9\n    ret\n");
        assertEquals(List.of("A4010"), this.codes());
        assertEquals(4, this.reader.problems().getFirst().line());
    }

    @Test
    void read_ignoresBlankLinesAndLinesThatAreOnlyANote() {
        final AsmProgram read = this.read(".asm 2\n\n; a note of its own\n.class C\n"
                + ".method void M() slots 0\n\n    ret\n");
        assertFalse(this.reader.hasProblems());
        assertEquals(1, read.type("C").methods().getFirst().body().size());
    }
}
