/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.system.ConstructorSpec;
import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.MethodSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

/**
 * What the compiler lets a program write is answered when the program runs, each thing by what its declaration says
 * answers it: a pure call by the language's functions; a call, a value or an event of the program's own by its process;
 * a window or a widget by what makes them. A call to the world is the machine's to answer, and a real computer is held
 * to that by a GameTest.
 *
 * <p>The other way round is checked where the bindings are made: a binding that names no declaration of its own kind
 * keeps its class from loading.
 */
class SystemApiConformanceTest {

    /** The namespace a program's windows and widgets are declared in. */
    private static final String UI = "System.UI";

    @Test
    void pureCalls_areAnsweredByTheLanguagesFunctions() {
        final List<String> missing = unanswered(
                (type, member) -> member instanceof MethodSpec && member.kind() == MemberKind.PURE,
                (type, member) -> PureFunctions.REGISTRY.find(member.id().owner(), member.id().name(),
                        member.id().parameters()) != null);
        assertTrue(missing.isEmpty(), () -> "no function of the language answers " + missing);
    }

    @Test
    void processCalls_areAnsweredByTheProgramsOwnProcess() {
        final List<String> missing = unanswered(
                (type, member) -> member instanceof MethodSpec && member.kind() == MemberKind.PROCESS,
                (type, member) -> ProcessCalls.find(member.id()) != null);
        assertTrue(missing.isEmpty(), () -> "the process answers none of " + missing);
    }

    @Test
    void processValuesOnAType_areReadByTheProgramsOwnProcess() {
        final List<String> missing = unanswered(
                (type, member) -> member instanceof PropertySpec && member.kind() == MemberKind.PROCESS
                        && member.isStatic(),
                (type, member) -> ProcessValues.find(type.name(), member.id().name(), true) != null);
        assertTrue(missing.isEmpty(), () -> "the process reads none of " + missing);
    }

    /*
     * A value the process keeps on an object of any other type is one of a record the program was handed, which the
     * object carries itself, so nothing is bound to read it.
     */
    @Test
    void widgetValuesAndEvents_areReadAndWrittenByTheProgramsOwnProcess() {
        final List<String> missing = unanswered(
                (type, member) -> UI.equals(type.namespace())
                        && (member instanceof PropertySpec || member instanceof EventSpec),
                (type, member) -> {
                    final ProcessValues.Binding binding = ProcessValues.find(type.name(), member.id().name(), false);
                    return binding != null && binding.write() != null;
                });
        assertTrue(missing.isEmpty(), () -> "the process reads and writes none of " + missing);
    }

    @Test
    void constructors_makeWhatTheyDeclare() {
        final List<String> missing = unanswered(
                (type, member) -> member instanceof ConstructorSpec,
                (type, member) -> WidgetObjects.find(type.name()) != null || CoreObjects.find(type.name()) != null);
        assertTrue(missing.isEmpty(), () -> "nothing makes " + missing);
    }

    /** Every member the test asks about that nothing answers, written the way a listing names it. */
    private static List<String> unanswered(final BiPredicate<TypeSpec, IMemberSpec> asked,
                                           final BiPredicate<TypeSpec, IMemberSpec> answered) {
        final List<String> missing = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                if (asked.test(type, member) && !answered.test(type, member)) {
                    missing.add(member.id().describe());
                }
            }
        }
        return missing;
    }
}
