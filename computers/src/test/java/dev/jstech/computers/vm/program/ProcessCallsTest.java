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
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProcessCallsTest {

    @Test
    void bindings_answerEveryCallTheSystemLeavesToTheProcess() {
        final List<String> unbound = new ArrayList<>();
        for (final String owner : List.of("Console", "Random", "Thread", "Program", "Process")) {
            for (final IMemberSpec member : SystemApi.type(owner).members()) {
                // A value the process keeps, such as the running thread, is read rather than called.
                final boolean called = !(member instanceof PropertySpec);
                if (called && member.kind() == MemberKind.PROCESS && ProcessCalls.find(member.id()) == null) {
                    unbound.add(member.id().describe());
                }
            }
        }
        assertTrue(unbound.isEmpty(), () -> "nothing answers " + unbound);
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
    void bindings_takeTheObjectOnlyForTheCallsMadeOnOne() {
        final Set<String> onTarget = new HashSet<>();
        for (final ProcessCalls.Binding binding : ProcessCalls.all()) {
            if (binding.onTarget()) {
                onTarget.add(binding.id().describe());
            }
        }
        assertEquals(Set.of("Thread.Join()", "Thread.Join(long)", "Thread.Stop()", "Process.Wait()",
                "Process.Wait(long)"), onTarget);
    }

    @Test
    void find_answersNothingForACallTheProcessDoesNotTake() {
        assertNull(ProcessCalls.find(new MemberId("Computer", "Disks", List.of())));
        assertNull(ProcessCalls.find(new MemberId("Console", "PrintLine", List.of("int"))));
    }
}
