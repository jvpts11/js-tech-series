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

import java.util.ArrayList;
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

    /*
     * The machine still charges from its own table. Until it reads what these declarations say, the two have to agree,
     * or an editor would quote one price and the machine charge another. The Gateway and the windows are priced where
     * the machine answers them, from the same numbers the declarations use, so the table names neither.
     */
    @Test
    void members_costWhatTheMachineCharges() {
        final List<String> wrong = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            if (!namedByTheTable(type.name())) {
                continue;
            }
            for (final IMemberSpec member : type.members()) {
                final SigmaCosts.Cost charged = SigmaCosts.of(type.name(), member.id().name());
                final CallCost expected = new CallCost(charged.fixed(), charged.perRow() ? 1 : 0, 0);
                if (!expected.equals(member.cost())) {
                    wrong.add(member.id().describe() + " declares " + member.cost().describe()
                            + " but the machine charges " + expected.describe());
                }
            }
        }
        assertTrue(wrong.isEmpty(), () -> String.join("\n", wrong));
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

    private static boolean namedByTheTable(final String owner) {
        for (final String call : SigmaCosts.all()) {
            if (call.startsWith(owner + ".")) {
                return true;
            }
        }
        return false;
    }
}
