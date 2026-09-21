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
import dev.jstech.computers.vm.system.SystemApi;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProcessCallsTest {

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
