/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.language.LanguageRegistry;
import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.nextgre.NextgreExtensions;
import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.GraphicsPrograms;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OperatingSpaceDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.ComponentKinds;
import dev.jstech.computers.os.media.EraMedia;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.registry.ComputerRegistry;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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
    public static final int VERSION = 8;

    /* The languages the computers of a world can be programmed in. */
    private static final LanguageRegistry LANGUAGES = new LanguageRegistry();

    private JsComputersApi() {
    }

    /**
     * The languages the computers of a world can be programmed in: one is added while the game loads, through
     * {@link ComputersRegisterEvent#languages()}, and the registry closes once the game has loaded.
     */
    @ApiStatus.Experimental
    public static LanguageRegistry languages() {
        return LANGUAGES;
    }

    /**
     * Adds an instruction set architecture, a kind of machine, with what it runs and how wide its words are.
     *
     * <p>A program built for an older machine of the same line runs on it; nothing runs what was built for
     * a machine that came after it.
     */
    public static void registerIsa(final IsaSpec isa) {
        if (isa == null) {
            return;
        }
        if (OsRegistry.isFrozen()) {
            JsComputers.LOGGER.warn("The ISA {} was not registered: they are only added while the game loads",
                    isa.id());
            return;
        }
        Isas.add(isa);
    }

    /**
     * Adds a kind of machine under the ISA's former name.
     *
     * @deprecated use {@link #registerIsa(IsaSpec)}; this goes in the next cycle of the series
     */
    @Deprecated(since = "0.5.0a", forRemoval = true)
    @SuppressWarnings("removal")
    public static void registerArchitecture(final ArchitectureSpec architecture) {
        if (architecture == null) {
            return;
        }
        registerIsa(architecture.toIsa());
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
        if (OsRegistry.isFrozen()) {
            JsComputers.LOGGER.warn("The file opener {} was not registered: they are only added while the game loads",
                    programId);
            return;
        }
        FileOpeners.registerAnyFileOpener(programId);
    }

    /**
     * Says the windows of a program draw through a surface, which holds video memory on the machine while they are
     * open.
     *
     * <p>This is the common half of that fact: the server counts video memory too, and it never runs the client setup
     * that says what opens the window. Call it for every program whose window answers a renderer.
     *
     * @param programId the program's id, as it was registered
     */
    public static void registerGraphicsProgram(final ResourceLocation programId) {
        if (programId != null) {
            GraphicsPrograms.register(programId);
        }
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

    /**
     * Adds a Network Operations Engine, which a Mainframe can then install, choose and run to plan its network's
     * work. Its package is a program registered like any other; this says what the engine it installs is.
     *
     * <p>Until the engine brings a planner of its own, it plans the way the Midsoft IQL Server does, so a network
     * never runs on an engine that cannot do the work.
     */
    public static void registerEngine(final EngineDef engine) {
        NetworkEngines.register(engine);
    }

    /**
     * Adds a rule to the planner of the engines that take extensions (NextgreIQL among them): it weighs every plan
     * they consider for a craft, and a player switches it on or off for their own Mainframe.
     */
    public static void registerPlannerRule(final IPlannerRule rule) {
        NextgreExtensions.addRule(rule);
    }

    /** Adds a hint to the dialect of the engines that take extensions, which changes the plans of the statement. */
    public static void registerPlannerOperator(final IPlannerOperator operator) {
        NextgreExtensions.addOperator(operator);
    }

    /** Adds a statistic those planners show, and may reckon a step's time with. */
    public static void registerPlannerStatistic(final IPlannerStatistic statistic) {
        NextgreExtensions.addStatistic(statistic);
    }

    /** Adds notes those planners show under the steps of a plan. */
    public static void registerExplainNode(final IExplainNode node) {
        NextgreExtensions.addExplainNode(node);
    }

    /**
     * Adds a kind of component for Σ# programs to put in their windows: {@code new GenericComponent("yourmod:dial")}.
     *
     * <p>The kind is named in the namespace of the mod adding it, and a name already taken stops the load. What draws
     * it is registered on the client, through {@code ComponentRenderers}.
     */
    public static void registerComponentKind(final ComponentKind kind) {
        ComponentKinds.register(kind);
    }

    /** Makes {@code item} a processor of that sheet. */
    @ApiStatus.Experimental
    public static void registerProcessor(final ItemLike item, final CpuSpec spec) {
        HardwareRegistry.processor(item, spec);
    }

    /** Makes {@code item} a graphics card of that sheet. */
    @ApiStatus.Experimental
    public static void registerGraphicsCard(final ItemLike item, final GpuSpec spec) {
        HardwareRegistry.graphicsCard(item, spec);
    }

    /** Makes {@code item} a memory module of that sheet. */
    @ApiStatus.Experimental
    public static void registerMemory(final ItemLike item, final RamSpec spec) {
        HardwareRegistry.memory(item, spec);
    }

    /** Makes {@code item} a disk of that sheet. */
    @ApiStatus.Experimental
    public static void registerDisk(final ItemLike item, final DiskSpec spec) {
        HardwareRegistry.disk(item, spec);
    }

    /** Makes {@code item} a motherboard of that sheet. */
    @ApiStatus.Experimental
    public static void registerMotherboard(final ItemLike item, final MotherboardSpec spec) {
        HardwareRegistry.motherboard(item, spec);
    }

    /** Makes {@code item} a power supply of that sheet, shown with the machines of {@code soldIn}. */
    @ApiStatus.Experimental
    public static void registerPowerSupply(final ItemLike item, final PsuSpec spec, final Era soldIn) {
        HardwareRegistry.powerSupply(item, spec, soldIn);
    }

    /** Makes {@code item} an expansion card of that sheet, shown with {@code era}, or with every era for null. */
    @ApiStatus.Experimental
    public static void registerExpansionCard(final ItemLike item, final IExpansionCardSpec spec,
                                             final @Nullable Era era) {
        HardwareRegistry.expansionCard(item, spec, era);
    }

    /** Adds a generation of memory, which modules are made in and boards take. */
    @ApiStatus.Experimental
    public static void registerMemoryGeneration(final RamGeneration generation) {
        RamGeneration.declare(generation);
    }

    /** Adds a generation of expansion slot, which cards are made for and boards offer. */
    @ApiStatus.Experimental
    public static void registerSlotGeneration(final PcieGeneration generation) {
        PcieGeneration.declare(generation);
    }

    /** Adds a form factor of motherboard, which a computer's case takes. */
    @ApiStatus.Experimental
    public static void registerFormFactor(final FormFactor formFactor) {
        FormFactor.declare(formFactor);
    }

    /** Adds a kind of expansion card, which a build finds its cards by. */
    @ApiStatus.Experimental
    public static void registerCardKind(final ExpansionCardKind kind) {
        ExpansionCardKind.declare(kind);
    }

    /** Says what the software of {@code era} ships on; an era that says nothing ships on the era before it's. */
    @ApiStatus.Experimental
    public static void registerEraMedia(final Era era, final EraMedia media) {
        InstallMedia.declare(era, media);
    }

    /** Makes {@code block} a computer of that entry, while the game loads. */
    @ApiStatus.Experimental
    public static void registerComputer(final Block block, final ComputerDef computer) {
        ComputerRegistry.computer(block, computer);
    }

    /** Makes {@code item} a server built in that chassis, while the game loads. */
    @ApiStatus.Experimental
    public static void registerServer(final ItemLike item, final RackChassis chassis) {
        ComputerRegistry.server(item, chassis);
    }

    /** Adds a machine, which computers are and programs are made for. */
    @ApiStatus.Experimental
    public static void registerMachine(final HostScope machine) {
        HostScope.declare(machine);
    }
}
