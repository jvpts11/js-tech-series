/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.machine.MachinePrograms;
import org.junit.jupiter.api.Test;

class SystemProcessesTest {

    @Test
    void initName_isSystemdOnTheDistributionsThatStartItAndInitElsewhere() {
        assertEquals("systemd", SystemProcesses.initName(Platform.LINUX, PackageManagerKind.APT));
        assertEquals("init", SystemProcesses.initName(Platform.LINUX, PackageManagerKind.EMERGE));
        assertEquals("init", SystemProcesses.initName(Platform.FREEBSD, PackageManagerKind.PKG));
        assertEquals("init", SystemProcesses.initName(Platform.UNIX, PackageManagerKind.NONE));
    }

    @Test
    void servicePid_andServiceIndex_readEachOtherBack() {
        for (int index = 0; index < 40; index++) {
            assertEquals(index, SystemProcesses.serviceIndex(SystemProcesses.servicePid(index)));
        }
        assertEquals(-1, SystemProcesses.serviceIndex(SystemProcesses.INIT_PID));
        assertEquals(-1, SystemProcesses.serviceIndex(SystemProcesses.servicePid(2) + 1));
    }

    @Test
    void servicePid_staysBelowTheProgramsForEveryServiceAMachineCouldHave() {
        assertTrue(SystemProcesses.servicePid(60) < MachinePrograms.FIRST_PID);
        assertTrue(SystemProcesses.servicePid(0) > SystemProcesses.INIT_PID);
    }
}
