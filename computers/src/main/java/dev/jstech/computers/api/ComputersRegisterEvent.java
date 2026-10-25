/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import dev.jstech.computers.api.language.LanguageRegistry;
import dev.jstech.computers.api.planner.IExplainNode;
import dev.jstech.computers.api.planner.IPlannerOperator;
import dev.jstech.computers.api.planner.IPlannerRule;
import dev.jstech.computers.api.planner.IPlannerStatistic;
import dev.jstech.computers.block.SmallComputerBlock;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OperatingSpaceDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.media.EraMedia;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The one moment anything may be added to what the computers know.
 *
 * <p>Fired once on the mod bus while the game loads, after every mod has been built and before anything
 * has run. Listening for it is how an addon brings a language, a kind of machine, a kernel, an operating
 * system, a program or a desktop; there is no other way in, because after the loading is done every one of
 * these is closed and refuses to change.
 *
 * <p>What a program can call is not here, and that is deliberate. A call is part of what the language
 * means, and a listing built on one machine runs on every machine of its line only because the same
 * listing means the same thing everywhere. A mod brings a language of its own, or a machine of its own,
 * and both keep that true.
 */
public final class ComputersRegisterEvent extends Event implements IModBusEvent {

    /**
     * The languages the computers of a world can be programmed in: a player writes a program in one and a machine
     * runs it.
     *
     * <p>A language claims the file extensions it writes and the ones it runs. One already claimed is refused, and
     * so is one the machines keep for themselves, such as that of the listings they run (the assembly a compiler of
     * the series turns a program into). A language under an id already taken replaces the one before it, so an
     * addon can improve a language in place, and the log says so.
     */
    @ApiStatus.Experimental
    public LanguageRegistry languages() {
        return JsComputersApi.languages();
    }

    /** Adds an instruction set architecture, a kind of machine that programs can then be built for. */
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
        if (architecture == null) {
            return;
        }
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

    /**
     * Says the windows of a program draw through a surface, which holds video memory on the machine while they are
     * open. The server counts that memory, so this is said here, on both sides, and not only in client setup.
     *
     * @param programId the program's id, as it was registered
     */
    public void graphicsProgram(final ResourceLocation programId) {
        JsComputersApi.registerGraphicsProgram(programId);
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
    public void engine(final EngineDef engine) {
        JsComputersApi.registerEngine(engine);
    }

    /** Adds a rule to the planner of the engines that take extensions. */
    public void plannerRule(final IPlannerRule rule) {
        JsComputersApi.registerPlannerRule(rule);
    }

    /** Adds a hint to the dialect of the engines that take extensions. */
    public void plannerOperator(final IPlannerOperator operator) {
        JsComputersApi.registerPlannerOperator(operator);
    }

    /** Adds a statistic to the planner of the engines that take extensions. */
    public void plannerStatistic(final IPlannerStatistic statistic) {
        JsComputersApi.registerPlannerStatistic(statistic);
    }

    /** Adds notes the planners of the engines that take extensions show under a plan's steps. */
    public void explainNode(final IExplainNode node) {
        JsComputersApi.registerExplainNode(node);
    }

    /**
     * Adds a kind of component for Σ# programs to put in their windows, named in your mod's namespace.
     *
     * <p>Register what draws it from your client setup, through {@code ComponentRenderers}; a player without your
     * mod sees a placeholder in its place.
     */
    public void componentKind(final ComponentKind kind) {
        JsComputersApi.registerComponentKind(kind);
    }

    /**
     * Makes {@code item} a processor of that sheet: an item of your mod's is a part because this says so, and then
     * seats in the boards whose socket and era it shares, as this mod's own processors do.
     */
    @ApiStatus.Experimental
    public void processor(final ItemLike item, final CpuSpec spec) {
        JsComputersApi.registerProcessor(item, spec);
    }

    /** Makes {@code item} a graphics card of that sheet, which seats in a slot of its bus's family. */
    @ApiStatus.Experimental
    public void graphicsCard(final ItemLike item, final GpuSpec spec) {
        JsComputersApi.registerGraphicsCard(item, spec);
    }

    /**
     * Makes {@code item} a memory module of that sheet, which seats in the boards of its era that take its
     * generation.
     */
    @ApiStatus.Experimental
    public void memory(final ItemLike item, final RamSpec spec) {
        JsComputersApi.registerMemory(item, spec);
    }

    /** Makes {@code item} a disk of that sheet. */
    @ApiStatus.Experimental
    public void disk(final ItemLike item, final DiskSpec spec) {
        JsComputersApi.registerDisk(item, spec);
    }

    /** Makes {@code item} a motherboard of that sheet, which seats in the cases of its era that take its form. */
    @ApiStatus.Experimental
    public void motherboard(final ItemLike item, final MotherboardSpec spec) {
        JsComputersApi.registerMotherboard(item, spec);
    }

    /**
     * Makes {@code item} a power supply of that sheet. A supply goes in any computer of any era; {@code soldIn} is
     * the era it is shown with.
     */
    @ApiStatus.Experimental
    public void powerSupply(final ItemLike item, final PsuSpec spec, final Era soldIn) {
        JsComputersApi.registerPowerSupply(item, spec, soldIn);
    }

    /**
     * Makes {@code item} an expansion card of that sheet: one of this mod's kinds, or one of your own
     * ({@link #cardKind}), which your mod finds in a build by its kind. {@code era} is the era it is shown with, or
     * null for a card every era shares.
     */
    @ApiStatus.Experimental
    public void expansionCard(final ItemLike item, final IExpansionCardSpec spec, final @Nullable Era era) {
        JsComputersApi.registerExpansionCard(item, spec, era);
    }

    /** Adds a generation of memory, which your modules are made in and your boards take. */
    @ApiStatus.Experimental
    public void memoryGeneration(final RamGeneration generation) {
        JsComputersApi.registerMemoryGeneration(generation);
    }

    /** Adds a generation of expansion slot, which your cards are made for and your boards offer. */
    @ApiStatus.Experimental
    public void slotGeneration(final PcieGeneration generation) {
        JsComputersApi.registerSlotGeneration(generation);
    }

    /** Adds a form factor of motherboard, which your boards are made in and your computers' cases take. */
    @ApiStatus.Experimental
    public void formFactor(final FormFactor formFactor) {
        JsComputersApi.registerFormFactor(formFactor);
    }

    /** Adds a kind of expansion card, which your cards are and your mod finds them by. */
    @ApiStatus.Experimental
    public void cardKind(final ExpansionCardKind kind) {
        JsComputersApi.registerCardKind(kind);
    }

    /**
     * Says what the software of your era ships on: its systems, its programs and its services. An era that says
     * nothing ships on what the era before it does.
     */
    @ApiStatus.Experimental
    public void eraMedia(final Era era, final EraMedia media) {
        JsComputersApi.registerEraMedia(era, media);
    }

    /**
     * Makes {@code block} a computer of that entry: a block of your mod's is a computer because this says so, and
     * reads from the entry the boards its case takes, the machine it is and the parts it counts. A small computer of
     * yours is a {@link SmallComputerBlock}, in one of the cases this mod ships, of the entry's era.
     */
    @ApiStatus.Experimental
    public void computer(final Block block, final ComputerDef computer) {
        JsComputersApi.registerComputer(block, computer);
    }

    /** Makes {@code item}, a {@link ServerItem} of your mod's, a server built in that chassis. */
    @ApiStatus.Experimental
    public void server(final ItemLike item, final RackChassis chassis) {
        JsComputersApi.registerServer(item, chassis);
    }

    /** Adds a machine, which your computers are and your programs are made for. */
    @ApiStatus.Experimental
    public void machine(final HostScope machine) {
        JsComputersApi.registerMachine(machine);
    }
}
