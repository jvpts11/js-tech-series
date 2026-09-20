/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.Architectures;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OperatingSpaceDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.FileOpeners;

/**
 * What J's Computers promises to anything built on it.
 *
 * <p>Everything a mod is meant to use is reached from this package. Anything else in the mod is the mod's
 * own business and may change in any release without a word.
 *
 * <p>Adding anything happens while the game loads, by listening for {@link ComputersRegisterEvent}; after
 * that every registry is closed. The methods here are what that event calls, and they are public so that
 * something loading earlier than the event can still use them, but the event is the way in.
 */
public final class JsComputersApi {

    /**
     * The number of the shape of this API, raised by one whenever anything is added to it.
     *
     * <p>How settled it is, and how long something lives once it is marked as going, are the series'
     * answers rather than this mod's: see the Core's.
     */
    public static final int VERSION = 1;

    private JsComputersApi() {
    }

    /**
     * Adds a kind of machine, with what it runs and how wide its words are.
     *
     * <p>A program built for an older machine of the same line runs on it; nothing runs what was built for
     * a machine that came after it.
     */
    public static void registerArchitecture(final ArchitectureSpec architecture) {
        Architectures.add(architecture);
    }

    /** Adds a kernel, which an operating system then names as the one it is built on. */
    public static void registerKernel(final KernelDef kernel) {
        OsRegistry.registerKernel(kernel);
    }

    /** Adds an operating system, which can then be installed on a computer that can hold it. */
    public static void registerOperatingSystem(final OsDef os) {
        OsRegistry.registerOs(os);
    }

    /** Adds a program, which can then be installed on an operating system that runs it. */
    public static void registerProgram(final ProgramSpec program) {
        OsRegistry.registerProgram(program);
    }

    /**
     * Says a program can open a file of any kind.
     *
     * @param programId the program's id path, as it was registered
     */
    public static void registerFileOpener(final String programId) {
        FileOpeners.registerAnyFileOpener(programId);
    }

    /** Adds a desktop, which a Linux computer installs as a package or an operating system bundles. */
    public static void registerDesktop(final DesktopEnvironmentDef desktop) {
        OsRegistry.registerDesktop(desktop);
    }

    /**
     * Adds an operating space, which a network system installs as a package.
     *
     * <p>This half names it. The screen that draws it is registered on the client, where there is a screen
     * to register, so an add-on shipping a space calls both: this one from its common setup and the other
     * from its client setup.
     */
    public static void registerSpace(final OperatingSpaceDef space) {
        OsRegistry.registerSpace(space);
    }
}
