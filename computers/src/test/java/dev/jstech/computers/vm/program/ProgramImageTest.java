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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.computers.vm.system.MemberKind;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramImageTest {

    /**
     * An interface, a class implementing it and a class standing on that one, written out the way a listing reads,
     * with a method whose lines branch, make an object and call a method it inherits.
     */
    private static ProgramImage shapes() {
        final AsmProgram program = new AsmProgram();
        final AsmType face = new AsmType(AsmType.Kind.INTERFACE, "Tests.IShape");
        face.addMethod(new AsmMethod("Area", "int", List.of(), false, 0, null));
        program.addType(face);

        final AsmType shape = new AsmType(AsmType.Kind.CLASS, "Tests.Shape");
        shape.addBase("Tests.IShape");
        shape.addField(new AsmType.Field("Side", "int", false));
        shape.addField(new AsmType.Field("Made", "int", true));
        shape.addMethod(body("Area", List.of(), false));
        shape.addMethod(body("Name", List.of(), false));
        program.addType(shape);

        final AsmType square = new AsmType(AsmType.Kind.CLASS, "Tests.Square");
        square.addBase("Tests.Shape");
        square.addField(new AsmType.Field("Extra", "long", false));
        square.addMethod(body(AsmMethod.CONSTRUCTOR, List.of("int"), false));
        square.addMethod(body(AsmMethod.CONSTRUCTOR, List.of("int", "int"), false));
        square.addMethod(new AsmMethod("Area", "int", List.of(), false, 0, List.of(
                Instruction.of(Opcode.BR, new IOperand.Label("L1")),
                Instruction.of(Opcode.NEWOBJ, new IOperand.Constructor("Tests.Square", List.of("int"))),
                Instruction.of(Opcode.CALL, new IOperand.Method("Tests.Square", "Name", List.of(), "string")),
                Instruction.of(Opcode.CALL, new IOperand.Method("Math", "Abs", List.of("double"), "double")),
                Instruction.of(Opcode.RET).labelled("L1"))));
        square.addMethod(body(AsmMethod.TYPE_SET_UP, List.of(), true));
        program.addType(square);
        return ProgramImage.of(program);
    }

    private static AsmMethod body(final String name, final List<String> parameters, final boolean isStatic) {
        return new AsmMethod(name, "void", parameters, isStatic, parameters.size(), List.of(Instruction.of(Opcode.RET)));
    }

    private static MethodImage squareArea(final ProgramImage image) {
        return image.type("Tests.Square").methods().get("Area()");
    }

    @Test
    void method_findsWhatATypeInheritsAsWellAsWhatItDeclares() {
        final ProgramImage image = shapes();
        assertEquals("Tests.Shape", image.method("Tests.Square", "Name", List.of()).owner());
        assertEquals("Tests.Square", image.method("Tests.Square", "Area", List.of()).owner());
        assertFalse(image.method("Tests.IShape", "Area", List.of()).hasCode());
        assertNull(image.method("Tests.Square", "Missing", List.of()));
        assertNull(image.method("Tests.Nowhere", "Area", List.of()));
    }

    @Test
    void resolve_pointsEveryBranchAtTheLineItLandsOn() {
        assertEquals(4, squareArea(shapes()).jump(0));
    }

    @Test
    void resolve_givesACallTheMethodItReachesOrTheFunctionTheSystemAnswersItWith() {
        final MethodImage area = squareArea(shapes());
        final ProgramImage.CallSite inherited = area.call(2);
        assertEquals("Tests.Shape", inherited.direct().owner());
        assertNull(inherited.intrinsic());
        assertFalse(inherited.constructs());
        assertTrue(inherited.gives());
        final ProgramImage.CallSite pure = area.call(3);
        assertNull(pure.direct());
        assertNull(pure.signature());
        assertEquals("Math.Abs(double)", pure.intrinsic().describe());
        assertEquals(1, pure.outs().length);
    }

    @Test
    void resolve_givesACallAndANewTheSystemAnswersTheirDeclarations() {
        final AsmProgram program = new AsmProgram();
        final AsmType desk = new AsmType(AsmType.Kind.CLASS, "Tests.Desk");
        desk.addMethod(new AsmMethod("Open", "void", List.of(), true, 0, List.of(
                Instruction.of(Opcode.CALL, new IOperand.Method("Computer", "Disks", List.of(), "List<DiskInfo>")),
                Instruction.of(Opcode.NEWOBJ, new IOperand.Constructor("Window", List.of("string", "int", "int"))),
                Instruction.of(Opcode.CALL, new IOperand.Method("Computer", "Nowhere", List.of(), "void")),
                Instruction.of(Opcode.RET))));
        program.addType(desk);
        final MethodImage open = ProgramImage.of(program).type("Tests.Desk").methods().get("Open()");
        assertEquals("Computer.Disks()", open.call(0).declared().id().describe());
        assertEquals(MemberKind.WORLD, open.call(0).declared().kind());
        assertEquals("Window.new(string, int, int)", open.creation(1).declared().id().describe());
        assertNull(open.call(2).declared(), "a call the system does not declare is matched to nothing");

        final MethodImage area = squareArea(shapes());
        assertNull(area.call(2).declared(), "the program's own method is not the system's");
        assertNull(area.call(3).declared(), "nor is a call a pure function answers");
        assertNull(area.creation(1).declared(), "nor is a new of the program's own type");
    }

    @Test
    void resolve_givesANewItsConstructorAndWhatTheObjectWeighs() {
        final ProgramImage.Creation creation = squareArea(shapes()).creation(1);
        assertEquals(List.of("int"), creation.constructor().parameters());
        // A header, the int it inherits and its own long; the static field weighs nothing on the object.
        assertEquals(Heap.HEADER + 4 + 8, creation.type().instanceSize());
    }

    @Test
    void constructor_isFoundByHowManyArgumentsItTakes() {
        final ProgramImage image = shapes();
        assertEquals(List.of("int", "int"), image.type("Tests.Square").constructor(2).parameters());
        assertNull(image.type("Tests.Square").constructor(3));
        assertNull(image.type("Tests.Shape").constructor(0));
    }

    @Test
    void setUp_isKeptApartFromTheMethodsCalledByName() {
        final ProgramImage image = shapes();
        assertNotNull(image.type("Tests.Square").setUp());
        assertNull(image.method("Tests.Square", AsmMethod.TYPE_SET_UP, List.of()));
    }

    @Test
    void isA_followsClassesAndInterfacesAllTheWayDown() {
        final ProgramImage image = shapes();
        assertTrue(image.isA("Tests.Square", "Tests.IShape"));
        assertTrue(image.isA("Tests.Square", "Tests.Square"));
        assertTrue(image.isA("List<int>", "List<int>"));
        assertFalse(image.isA("Tests.Shape", "Tests.Square"));
        assertFalse(image.isA(null, "Tests.Shape"));
    }

    @Test
    void of_loadsATypeThatStandsOnItselfWithoutGoingRoundForever() {
        final AsmProgram program = new AsmProgram();
        final AsmType first = new AsmType(AsmType.Kind.CLASS, "Tests.First");
        first.addBase("Tests.Second");
        first.addMethod(body("Only", List.of(), false));
        program.addType(first);
        final AsmType second = new AsmType(AsmType.Kind.CLASS, "Tests.Second");
        second.addBase("Tests.First");
        program.addType(second);

        final ProgramImage image = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> ProgramImage.of(program));
        assertEquals("Tests.First", image.method("Tests.Second", "Only", List.of()).owner());
        assertTrue(image.isA("Tests.First", "Tests.Second"));
        assertTrue(image.isA("Tests.Second", "Tests.First"));
    }

    @Test
    void checksum_staysWithTheListingAndChangesWhenTheListingDoes() {
        final AsmProgram other = new AsmProgram();
        final AsmType lone = new AsmType(AsmType.Kind.CLASS, "Tests.Lone");
        lone.addMethod(body("Only", List.of(), false));
        other.addType(lone);

        assertEquals(shapes().checksum(), shapes().checksum());
        assertNotEquals(shapes().checksum(), ProgramImage.of(other).checksum());
        assertEquals(64, shapes().checksum().length(), "a SHA-256 written as hex");
    }
}
