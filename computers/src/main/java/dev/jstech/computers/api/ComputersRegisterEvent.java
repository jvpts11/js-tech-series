/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OperatingSpaceDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * The one moment anything may be added to what the computers know.
 *
 * <p>Fired once on the mod bus while the game loads, after every mod has been built and before anything
 * has run. Listening for it is how an addon brings a kind of machine, a kernel, an operating system, a
 * program or a desktop; there is no other way in, because after the loading is done every one of these is
 * closed and refuses to change.
 *
 * <p>What a program can call is not here, and that is deliberate. A call is part of what the language
 * means, and a listing built on one machine runs on every machine of its line only because the same
 * listing means the same thing everywhere. A mod brings a language of its own, or a machine of its own,
 * and both keep that true.
 */
public final class ComputersRegisterEvent extends Event implements IModBusEvent {

    /** Adds a kind of machine, which programs can then be built for. */
    public void architecture(final ArchitectureSpec architecture) {
        JsComputersApi.registerArchitecture(architecture);
    }

    /** Adds a kernel, which an operating system then names as the one it is built on. */
    public void kernel(final KernelDef kernel) {
        JsComputersApi.registerKernel(kernel);
    }

    /** Adds an operating system, which can then be installed on a computer that can hold it. */
    public void operatingSystem(final OsDef os) {
        JsComputersApi.registerOperatingSystem(os);
    }

    /** Adds a program, which can then be installed on an operating system that runs it. */
    public void program(final ProgramSpec program) {
        JsComputersApi.registerProgram(program);
    }

    /**
     * Says a program can open a file of any kind.
     *
     * <p>Open with then offers it for files the computers have no program for, and among the others for a
     * text file. Its desktop app is handed the file the way the Editor is.
     *
     * @param programId the program's id path, as it was registered
     */
    public void fileOpener(final String programId) {
        JsComputersApi.registerFileOpener(programId);
    }

    /** Adds a desktop, which a Linux computer installs as a package or an operating system bundles. */
    public void desktop(final DesktopEnvironmentDef desktop) {
        JsComputersApi.registerDesktop(desktop);
    }

    /**
     * Adds an operating space, which a network system installs as a package.
     *
     * <p>Name it here and register the screen that draws it from your client setup. A space named with no
     * screen behind it leaves the machine at its prompt, which is what a machine with no space is.
     */
    public void operatingSpace(final OperatingSpaceDef space) {
        JsComputersApi.registerSpace(space);
    }
}
