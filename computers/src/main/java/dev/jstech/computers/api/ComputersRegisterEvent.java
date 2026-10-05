/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OperatingSpaceDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

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

    /** Adds an instruction set architecture, a kind of machine that programs can then be built for. */
    @ApiStatus.Experimental
    public void isa(final IsaSpec isa) {
        JsComputersApi.registerIsa(isa);
    }

    /**
     * Adds a kind of machine under the ISA's former name.
     *
     * @deprecated use {@link #isa(IsaSpec)}; this goes in the next cycle of the series
     */
    @Deprecated(since = "0.5.0a", forRemoval = true)
    @SuppressWarnings("removal")
    public void architecture(final ArchitectureSpec architecture) {
        isa(architecture.toIsa());
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

    /**
     * Adds a Network Operations Engine, which a Mainframe can then install and run to plan its network's work.
     *
     * <p>Register its package as a program too, the way any software is; this says what that package installs.
     */
    @ApiStatus.Experimental
    public void engine(final EngineDef engine) {
        JsComputersApi.registerEngine(engine);
    }

    /** Adds a rule to the planner of the engines that take extensions. */
    @ApiStatus.Experimental
    public void plannerRule(final IPlannerRule rule) {
        JsComputersApi.registerPlannerRule(rule);
    }

    /** Adds a hint to the dialect of the engines that take extensions. */
    @ApiStatus.Experimental
    public void plannerOperator(final IPlannerOperator operator) {
        JsComputersApi.registerPlannerOperator(operator);
    }

    /** Adds a statistic to the planner of the engines that take extensions. */
    @ApiStatus.Experimental
    public void plannerStatistic(final IPlannerStatistic statistic) {
        JsComputersApi.registerPlannerStatistic(statistic);
    }

    /** Adds notes the planners of the engines that take extensions show under a plan's steps. */
    @ApiStatus.Experimental
    public void explainNode(final IExplainNode node) {
        JsComputersApi.registerExplainNode(node);
    }

    /**
     * Adds a kind of component for Σ# programs to put in their windows, named in your mod's namespace.
     *
     * <p>Register what draws it from your client setup, through {@code ComponentRenderers}; a player without your
     * mod sees a placeholder in its place.
     */
    @ApiStatus.Experimental
    public void componentKind(final ComponentKind kind) {
        JsComputersApi.registerComponentKind(kind);
    }
}
