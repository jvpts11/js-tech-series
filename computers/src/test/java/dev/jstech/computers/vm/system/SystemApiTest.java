/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SystemApiTest {

    @Test
    void types_areEachDeclaredOnce() {
        final Set<String> names = new HashSet<>();
        for (final TypeSpec type : SystemApi.types()) {
            assertTrue(names.add(type.name()), () -> type.name() + " is declared twice");
        }
    }

    @Test
    void members_areEachDeclaredOnce() {
        final Set<MemberId> ids = new HashSet<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                assertTrue(ids.add(member.id()), () -> member.id().describe() + " is declared twice");
            }
        }
    }

    @Test
    void types_liveInTheSystemsNamespaces() {
        for (final TypeSpec type : SystemApi.types()) {
            assertTrue(type.namespace().startsWith("System."), () -> type.name() + " is in " + type.namespace());
        }
    }

    @Test
    void types_buildOnlyOnTypesTheSystemDeclares() {
        for (final TypeSpec type : SystemApi.types()) {
            if (!type.base().isEmpty()) {
                assertNotNull(SystemApi.type(type.base()), () -> type.name() + " builds on " + type.base());
            }
        }
    }

    @Test
    void type_findsATypeByItsName() {
        assertEquals("System.Utils", SystemApi.type("Math").namespace());
        assertEquals("System.Execution", SystemApi.type("Process").namespace());
        assertEquals("Widget", SystemApi.type("Button").base());
        assertNull(SystemApi.type("Nothing"));
    }

    @Test
    void members_findEveryWayACallIsWritten() {
        assertEquals(4, SystemApi.members("Gateway", "Call").size());
        assertEquals(CallCost.of(105),
                SystemApi.member("Gateway", "Call", List.of("string", "string", "object")).cost());
        assertNull(SystemApi.member("Gateway", "Call", List.of("int")));
        assertTrue(SystemApi.members("Gateway", "Nothing").isEmpty());
    }

    /*
     * A call the compiler lets a program write has to be one a computer answers, or the program stops at the line
     * instead of being told when it loads. Running source text handed over as a string was only ever answered by the
     * translation for ComputerCraft computers, which is gone.
     */
    @Test
    void members_declareNoRunningOfSourceTextNoComputerAnswers() {
        assertTrue(SystemApi.members("Program", "RunSource").isEmpty());
    }

    @Test
    void pureMembers_costNothing() {
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                if (member.kind() == MemberKind.PURE) {
                    assertEquals(CallCost.FREE, member.cost(), () -> member.id().describe() + " needs no machine");
                }
            }
        }
    }

    /*
     * Asking to be told costs nothing on purpose: a program that says once that it wants to know when the iron runs
     * low is doing the cheap thing, and one that asks every tick is not.
     */
    @Test
    void watches_costNothingToArm() {
        for (final String name : List.of("Watch", "WatchBelow", "WatchAbove")) {
            final IMemberSpec watch = SystemApi.members("Network", name).getFirst();
            assertEquals(CallCost.FREE, watch.cost(), () -> name + " should cost nothing to arm");
            assertEquals(MemberKind.PROCESS, watch.kind(), () -> name + " is kept by the program's own process");
        }
    }

    @Test
    void members_makeAskingForWorkDearerThanReadingIt() {
        assertTrue(price("Operations", "Craft") > price("Operations", "Get"));
        assertTrue(price("File", "Write") > price("File", "Read"));
        assertTrue(price("Computer", "Disks") > price("Computer", "Name"));
    }

    @Test
    void new_refusesAMemberOfAnotherType() {
        final MethodSpec abs = new MethodSpec(new MemberId("Math", "Abs", List.of("int")), "int", true,
                MemberKind.PURE, CallCost.FREE);
        assertThrows(IllegalArgumentException.class, () -> new TypeSpec("System.Utils", "Convert", List.of(abs)));
    }

    @Test
    void property_refusesToTakeAnything() {
        final MemberId taking = new MemberId("Time", "Tick", List.of("int"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertySpec(taking, "long", true, false, MemberKind.WORLD, CallCost.FREE, CallCost.FREE));
    }

    @Test
    void property_refusesAWritePriceForAValueNobodyCanWrite() {
        final MemberId open = new MemberId("Window", "Open", List.of());
        assertThrows(IllegalArgumentException.class,
                () -> new PropertySpec(open, "bool", false, false, MemberKind.PROCESS, CallCost.FREE, CallCost.of(50)));
    }

    @Test
    void constructor_refusesAnyOtherName() {
        final MemberId named = new MemberId("Label", "Make", List.of("string"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConstructorSpec(named, MemberKind.PROCESS, CallCost.FREE));
    }

    @Test
    void event_refusesAHandlerWithTypeArguments() {
        final MemberId clicked = new MemberId("Button", "OnClick", List.of());
        assertThrows(IllegalArgumentException.class,
                () -> new EventSpec(clicked, "Action<int>", false, MemberKind.PROCESS, CallCost.FREE));
    }

    /** What the first way of writing {@code owner.name} costs when it brings nothing back. */
    private static int price(final String owner, final String name) {
        return SystemApi.members(owner, name).getFirst().cost().at(0, 0);
    }
}
