/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.MethodSpec;
import dev.jstech.computers.vm.system.SystemApi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProcessCallsTest {

    /*
     * The types whose calls the process answers through bindings so far. A Gateway's listener is the process's too,
     * but it is still answered on the machine's side until the Gateway's service takes it.
     */
    private static final List<String> BOUND_OWNERS = List.of("Console", "Random", "Thread", "Program", "Process",
            "Network", "Window", "Row", "Column", "ListBox", "Canvas", "MessageBox");

    @Test
    void bindings_answerEveryCallTheSystemLeavesToTheProcess() {
        final List<String> unbound = new ArrayList<>();
        for (final String owner : BOUND_OWNERS) {
            for (final IMemberSpec member : SystemApi.type(owner).members()) {
                // A value, a constructor or an event is read, made or joined rather than called.
                if (member instanceof MethodSpec && member.kind() == MemberKind.PROCESS
                        && ProcessCalls.find(member.id()) == null) {
                    unbound.add(member.id().describe());
                }
            }
        }
        assertTrue(unbound.isEmpty(), () -> "nothing answers " + unbound);
    }

    @Test
    void bindings_takeTheObjectExactlyWhenTheSystemDeclaresTheCallOnOne() {
        for (final ProcessCalls.Binding binding : ProcessCalls.all()) {
            final IMemberSpec declared =
                    SystemApi.member(binding.id().owner(), binding.id().name(), binding.id().parameters());
            assertEquals(!declared.isStatic(), binding.onTarget(), () -> binding.id().describe());
        }
    }

    @Test
    void bindings_waitOnlyInTheCallsThatReadALineOrWaitForAThreadOrAProgram() {
        final Set<String> waiting = new HashSet<>();
        for (final ProcessCalls.Binding binding : ProcessCalls.all()) {
            if (binding.waiting() != null) {
                waiting.add(binding.id().describe());
            }
        }
        assertEquals(Set.of("Console.ReadLine()", "Console.ReadInt()", "Console.ReadLong()", "Console.ReadDouble()",
                "Console.ReadBool()", "Thread.Join()", "Thread.Join(long)", "Process.Wait()", "Process.Wait(long)"),
                waiting);
    }

    @Test
    void bindings_takeTheObjectForAThreadOrAWidgetButNotForAMessageBox() {
        assertTrue(ProcessCalls.find(new MemberId("Thread", "Stop", List.of())).onTarget());
        assertTrue(ProcessCalls.find(new MemberId("Canvas", "SetPixel", List.of("int", "int", "int"))).onTarget());
        assertTrue(!ProcessCalls.find(new MemberId("MessageBox", "Show", List.of("string", "string"))).onTarget());
    }

    @Test
    void find_answersNothingForACallTheProcessDoesNotTake() {
        assertNull(ProcessCalls.find(new MemberId("Computer", "Disks", List.of())));
        assertNull(ProcessCalls.find(new MemberId("Console", "PrintLine", List.of("int"))));
    }
}
