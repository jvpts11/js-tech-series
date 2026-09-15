/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessValuesTest {

    @Test
    void bindings_readEveryValueTheSystemLeavesToTheProcessOnAType() {
        final List<String> unbound = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                if (member instanceof PropertySpec && member.kind() == MemberKind.PROCESS && member.isStatic()
                        && ProcessValues.find(type.name(), member.id().name(), true) == null) {
                    unbound.add(type.name() + "." + member.id().name());
                }
            }
        }
        assertTrue(unbound.isEmpty(), () -> "nothing reads " + unbound);
    }

    @Test
    void bindings_readAndWriteEverythingAWindowOrAWidgetHolds() {
        final List<String> missing = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            if (!"System.UI".equals(type.namespace())) {
                continue;
            }
            for (final IMemberSpec member : type.members()) {
                final boolean held = member instanceof PropertySpec || member instanceof EventSpec;
                final ProcessValues.Binding binding = ProcessValues.find(type.name(), member.id().name(), false);
                if (held && (binding == null || binding.write() == null)) {
                    missing.add(type.name() + "." + member.id().name());
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> "nothing reads and writes " + missing);
    }

    @Test
    void bindings_readEveryValueOfTheLanguagesCore() {
        for (final String core : ProgramImage.CORE_VALUES) {
            final int dot = core.indexOf('.');
            assertNotNull(ProcessValues.find(core.substring(0, dot), core.substring(dot + 1), false), core);
        }
    }

    @Test
    void find_keepsTheTypeApartFromItsObjectsAndLeavesTheMachinesValuesAlone() {
        assertNull(ProcessValues.find("Program", "Name", false));
        assertNull(ProcessValues.find("List", "Count", true));
        assertNull(ProcessValues.find("Computer", "Name", true));
    }
}
