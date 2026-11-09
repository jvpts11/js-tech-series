/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.hardware.Isas;
import java.util.List;
import org.junit.jupiter.api.Test;

class KernelNamesTest {

    @Test
    void architecture_namesTheMakersArchitectureOnFreeBsd() {
        assertEquals("vel64", KernelNames.architecture(Platform.FREEBSD, 64));
        assertEquals("IA-32", KernelNames.architecture(Platform.FREEBSD, 32));
    }

    @Test
    void architecture_keepsTheLinuxWordsOnLinux() {
        assertEquals("x86_64", KernelNames.architecture(Platform.LINUX, 64));
        assertEquals("i686", KernelNames.architecture(Platform.LINUX, 32));
    }

    @Test
    void architecture_treatsASixteenBitProcessorAsTheNarrowerOfTheTwo() {
        assertEquals("IA-32", KernelNames.architecture(Platform.FREEBSD, 16));
        assertEquals("i686", KernelNames.architecture(Platform.LINUX, 16));
    }

    @Test
    void kernel_givesTheNameTheReleaseAndTheArchitecture() {
        assertEquals("FreeBSD 14.1-RELEASE vel64", KernelNames.kernel(Platform.FREEBSD, 64, ""));
        assertEquals("Linux 6.8-jsc x86_64", KernelNames.kernel(Platform.LINUX, 64, ""));
        assertEquals("Wallace 8.0.0 ppc", KernelNames.kernel(Platform.QUINCE, 32, Isas.POWERPC.id()));
    }

    @Test
    void everything_followsEachFamilysOwnOrder() {
        assertEquals("FreeBSD desk 14.1-RELEASE FreeBSD 14.1-RELEASE GENERIC IA-32",
                KernelNames.everything(Platform.FREEBSD, "desk", 32, ""));
        assertEquals("Linux desk 6.8-jsc #1 SMP x86_64 GNU/Linux",
                KernelNames.everything(Platform.LINUX, "desk", 64, ""));
        assertEquals("Wallace desk 8.0.0 Wallace Kernel Version 8.0.0: arm64",
                KernelNames.everything(Platform.QUINCE, "desk", 64, Isas.ARM64.id()));
    }

    @Test
    void architecture_namesQuincesAfterTheInstructionSetAndAnyIntegraOneI386() {
        assertEquals("m68k", KernelNames.architecture(Platform.QUINCE, 16, Isas.M68K.id()));
        assertEquals("ppc", KernelNames.architecture(Platform.QUINCE, 64, Isas.POWERPC.id()));
        assertEquals("arm64", KernelNames.architecture(Platform.QUINCE, 64, Isas.ARM64.id()));
        assertEquals("i386", KernelNames.architecture(Platform.QUINCE, 64, "x86_64"));
        assertEquals("x86_64", KernelNames.architecture(Platform.LINUX, 64, Isas.ARM64.id()),
                "the other families go by the width alone");
    }

    @Test
    void kernelLine_namesFreeBsdsBuildAndJustTheVersionElsewhere() {
        assertEquals("14.1-RELEASE GENERIC", KernelNames.kernelLine(Platform.FREEBSD));
        assertEquals("6.8-jsc", KernelNames.kernelLine(Platform.LINUX));
    }

    @Test
    void terminal_isTheFirstOneOnTheMachinesOwnScreenAsEachFamilyCallsIt() {
        assertEquals("ttyv0", KernelNames.terminal(Platform.FREEBSD));
        assertEquals("tty1", KernelNames.terminal(Platform.LINUX));
    }

    @Test
    void name_isEachFamilysOwn() {
        assertEquals("Linux", KernelNames.name(Platform.LINUX));
        assertEquals("FreeBSD", KernelNames.name(Platform.FREEBSD));
        assertEquals("UNIX", KernelNames.name(Platform.UNIX));
    }

    @Test
    void architecture_onSystemVGoesDownToTheSixteenBitWord() {
        assertEquals("IA-16", KernelNames.architecture(Platform.UNIX, 16));
        assertEquals("IA-32", KernelNames.architecture(Platform.UNIX, 32));
        assertEquals("vel64", KernelNames.architecture(Platform.UNIX, 64));
    }

    @Test
    void everything_onSystemVIsTheSystemTheNodeTheReleaseTheVersionAndTheMachine() {
        assertEquals("UNIX desk 3.2 2 IA-16", KernelNames.everything(Platform.UNIX, "desk", 16, ""));
        assertEquals("console", KernelNames.terminal(Platform.UNIX));
    }

    @Test
    void bootFiles_areNamedTheWayEachFamilyNamesThem() {
        assertEquals(List.of("boot/vmlinuz", "boot/initrd.img"), KernelNames.bootFiles(Platform.LINUX, 0));
        assertEquals(List.of("boot/loader", "boot/kernel/kernel"), KernelNames.bootFiles(Platform.FREEBSD, 0));
        assertEquals(List.of("unix"), KernelNames.bootFiles(Platform.UNIX, 0));
        assertEquals(List.of("System Folder/System", "System Folder/Seeker"),
                KernelNames.bootFiles(Platform.QUINCE, QuinceFamily.OS_9));
        assertEquals(List.of("System/Library/CoreServices/boot.efi"),
                KernelNames.bootFiles(Platform.QUINCE, QuinceFamily.OS_X));
    }

    @Test
    void name_andTerminal_areWallaceAndTheFirstPseudoTerminalOnQuince() {
        assertEquals("Wallace", KernelNames.name(Platform.QUINCE));
        assertEquals("ttys000", KernelNames.terminal(Platform.QUINCE));
    }
}
