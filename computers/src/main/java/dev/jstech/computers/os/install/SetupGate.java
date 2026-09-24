/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.IMainframeService;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.RackUnitHost;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.Locale;
import java.util.Optional;

/**
 * Whether a machine can take a program, and if not, why, in words the Setup window can show.
 *
 * <p>Every way of installing a program (the disc's setup, This PC's button, the prompt, the package
 * manager) used to keep its own copy of these checks, and each said no in its own words, or in none.
 * They are asked here, once, in the order a real setup asks them, and the first thing wrong is the
 * answer.
 *
 * <p>The words are the setup's own, read in the player's language; the names of programs, systems and eras are
 * data and go into them as they are.
 */
@TextHolder
public final class SetupGate {

    /** A machine with nowhere to keep programs, which the runner says too when it is asked to start one. */
    static final TextKey CANNOT_HOLD = TextKey.of("jsc.install.setup_gate.cannot_hold",
            "This computer cannot hold installed programs.");

    private static final TextKey NOT_INSTALLED = TextKey.of("jsc.install.setup_gate.not_installed",
            "%s is not installed.");
    private static final TextKey COMES_WITH_EVERY_SYSTEM = TextKey.of("jsc.install.setup_gate.comes_with_every_system",
            "%s comes with every system. There is nothing to install.");
    private static final TextKey ALREADY_INSTALLED = TextKey.of("jsc.install.setup_gate.already_installed",
            "%s is already installed.");
    private static final TextKey NEEDS_ERA = TextKey.of("jsc.install.setup_gate.needs_era",
            "%s needs %s hardware or newer. This computer is %s.");
    private static final TextKey NEEDS_SYSTEM = TextKey.of("jsc.install.setup_gate.needs_system",
            "%s needs an operating system to install on.");
    private static final TextKey WRONG_PLATFORM = TextKey.of("jsc.install.setup_gate.wrong_platform",
            "%s runs on %s, not on %s.");
    private static final TextKey NEEDS_NEWER_SYSTEM = TextKey.of("jsc.install.setup_gate.needs_newer_system",
            "%s needs %s or newer. This computer runs %s.");
    private static final TextKey NEEDS_PROCESSOR = TextKey.of("jsc.install.setup_gate.needs_processor",
            "%s needs a %s MHz processor. This computer has %s MHz.");
    private static final TextKey NEEDS_VIDEO_MEMORY = TextKey.of("jsc.install.setup_gate.needs_video_memory",
            "%s needs %s MB of video memory. This computer has %s MB.");
    private static final TextKey NEEDS_DISK = TextKey.of("jsc.install.setup_gate.needs_disk",
            "%s needs %s MB free on the disk. There are %s MB.");
    private static final TextKey PUT_DISC_IN = TextKey.of("jsc.install.setup_gate.put_disc_in",
            "Put the %s disc in a drive linked to this computer.");
    private static final TextKey CANNOT_INSTALL_HERE = TextKey.of("jsc.install.setup_gate.cannot_install_here",
            "%s cannot install on this computer's system or hardware.");
    private static final TextKey ONLY_INSTALLS_ON = TextKey.of("jsc.install.setup_gate.only_installs_on",
            "%s only installs on %s.");
    private static final TextKey ON_A_MAINFRAME = TextKey.of("jsc.install.setup_gate.on_a_mainframe", "a Mainframe");
    private static final TextKey ON_A_CRAFTING_COMPUTER = TextKey.of("jsc.install.setup_gate.on_a_crafting_computer",
            "a Crafting Computer");
    private static final TextKey ON_A_SERVER = TextKey.of("jsc.install.setup_gate.on_a_server", "a server in a rack");
    private static final TextKey ON_A_CLUSTER_MANAGER = TextKey.of("jsc.install.setup_gate.on_a_cluster_manager",
            "a Cluster Management Computer");
    private static final TextKey ON_THIS_COMPUTER = TextKey.of("jsc.install.setup_gate.on_this_computer",
            "this computer");
    private static final TextKey AND = TextKey.of("jsc.install.setup_gate.and", "%s and %s");
    private static final TextKey NOTHING = TextKey.of("jsc.install.setup_gate.nothing", "nothing");

    private SetupGate() {
    }

    /**
     * Why the machine refuses to install (or remove) {@code spec}, or empty when it may go ahead.
     *
     * @param hasMedium whether the program's disc is in a linked drive, or it comes over the network
     */
    public static Optional<Text> refusal(final IOsHost host, final ProgramSpec spec, final boolean removing,
                                         final boolean hasMedium) {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return Optional.of(CANNOT_HOLD.text());
        }
        final String name = spec.displayName();
        if (removing) {
            return installed(host, console, spec) ? Optional.empty()
                    : Optional.of(NOT_INSTALLED.with(name));
        }
        if (spec.preinstalled()) {
            return Optional.of(COMES_WITH_EVERY_SYSTEM.with(name));
        }
        if (installed(host, console, spec)) {
            return Optional.of(ALREADY_INSTALLED.with(name));
        }
        final HardwareEra era = host.displayEra();
        if (spec.minEra() != HardwareEra.VINTAGE && era != null && !OsGating.canInstall(spec.minEra(), era)) {
            return Optional.of(NEEDS_ERA.with(name, eraName(spec.minEra()), eraName(era)));
        }
        final OsDef os = host.installedOs();
        if (os == null) {
            return Optional.of(NEEDS_SYSTEM.with(name));
        }
        if (!spec.platforms().contains(os.platform())) {
            return Optional.of(WRONG_PLATFORM.with(name, platforms(spec), os.platform().label()));
        }
        final int rank = OsRegistry.osVersionRank(os.id());
        if (rank != 0 && rank < spec.minOsRank()) {
            return Optional.of(NEEDS_NEWER_SYSTEM.with(name, OsRegistry.systemOfRank(spec.minOsRank()),
                    os.displayName()));
        }
        final Optional<Text> scope = hostScope(host, spec);
        if (scope.isPresent()) {
            return scope;
        }
        if (host.maxCpuMhz() < spec.minCpuMhz()) {
            return Optional.of(NEEDS_PROCESSOR.with(name, spec.minCpuMhz(), host.maxCpuMhz()));
        }
        if (host.totalVramMb() < spec.minVramMb()) {
            return Optional.of(NEEDS_VIDEO_MEMORY.with(name, spec.minVramMb(), host.totalVramMb()));
        }
        if (host.systemDiskFreeMb() < spec.minDiskMb()) {
            return Optional.of(NEEDS_DISK.with(name, spec.minDiskMb(), host.systemDiskFreeMb()));
        }
        if (!hasMedium) {
            return Optional.of(PUT_DISC_IN.with(name));
        }
        // The registry's own word is the last one, in case a rule lives there and nowhere above.
        if (!OsRegistry.canInstallProgram(os.id(), spec.id(), host.maxCpuMhz(), host.totalVramMb(),
                host.systemDiskFreeMb())) {
            return Optional.of(CANNOT_INSTALL_HERE.with(name));
        }
        return Optional.empty();
    }

    /**
     * Whether {@code spec} is on the machine already.
     *
     * <p>A Mainframe's services live on the Mainframe: one switched on with its own verb, as the
     * Mirror is with {@code mirror install}, is installed whether or not the console lists it, and
     * removing it has to be allowed on the strength of the service.
     */
    private static boolean installed(final IOsHost host, final ComputerConsoleState console, final ProgramSpec spec) {
        if (console.isInstalled(spec.id().toString())) {
            return true;
        }
        if (host instanceof MainframeBlockEntity mainframe) {
            final IMainframeService service = mainframe.service(spec.id());
            return service != null && service.installed();
        }
        return false;
    }

    /** A program bound to one kind of machine refuses every other kind by name. */
    private static Optional<Text> hostScope(final IOsHost host, final ProgramSpec spec) {
        final boolean allowed = switch (spec.hostScope()) {
            case ANY -> true;
            case MAINFRAME -> host instanceof MainframeBlockEntity;
            case CRAFTING_COMPUTER -> host instanceof CraftingComputerBlockEntity;
            case SERVER -> host instanceof ServerRackBlockEntity || host instanceof RackUnitHost;
            case CLUSTER_MANAGEMENT_COMPUTER -> host instanceof ClusterManagementComputerBlockEntity;
        };
        if (allowed) {
            return Optional.empty();
        }
        final TextKey where = switch (spec.hostScope()) {
            case MAINFRAME -> ON_A_MAINFRAME;
            case CRAFTING_COMPUTER -> ON_A_CRAFTING_COMPUTER;
            case SERVER -> ON_A_SERVER;
            case CLUSTER_MANAGEMENT_COMPUTER -> ON_A_CLUSTER_MANAGER;
            default -> ON_THIS_COMPUTER;
        };
        return Optional.of(ONLY_INSTALLS_ON.with(spec.displayName(), where));
    }

    /** The families a program runs on, joined the way a sentence lists them; the families' names are data. */
    private static Text platforms(final ProgramSpec spec) {
        Text out = null;
        for (final Platform platform : spec.platforms()) {
            out = out == null ? Text.literal(platform.label()) : AND.with(out, platform.label());
        }
        return out == null ? NOTHING.text() : out;
    }

    /** An era's name as a word: "Legacy", not "LEGACY". */
    public static String eraName(final HardwareEra era) {
        final String raw = era.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
