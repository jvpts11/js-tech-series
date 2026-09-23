/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.tier.HardwareEra;
import java.util.Locale;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What stands between a machine and a program it is asked to install from the Mirror: its generation, the kind of
 * machine it is, and whether its hardware and its disk have room for the program. The same gates for a package and
 * for a port; only what they say differs, and that is the caller's to word.
 */
final class InstallGates {

    private final IComputerTerminalHost terminal;

    InstallGates(final IComputerTerminalHost terminal) {
        this.terminal = terminal;
    }

    /**
     * Refuses a program the machine is too old to run, or {@code null} when the era is fine. Software cannot predate
     * its hardware generation: a desktop of the 2010s does not install on a machine of the 1990s, however much disk
     * it has free.
     *
     * <p>This is the package manager's gate. The install medium does not come through here: it is refused by
     * {@code SetupGate}, which tests the same rule among the others it checks and names the machine's era in its
     * message. Two doors, one rule.
     */
    @Nullable
    ICliComputer.OpResult era(final ProgramSpec spec) {
        if (spec.minEra() == HardwareEra.VINTAGE) {
            return null; // no requirement
        }
        /*
         * displayEra, not installedEra: a Vintage or Legacy chassis IS that generation whatever board
         * sits in it, and that chassis is the only way a machine of an older era exists right now.
         */
        final BlockEntity machine = (BlockEntity) this.terminal;
        final HardwareEra era = machine instanceof IOsHost computer ? computer.displayEra() : null;
        if (era != null && OsGating.canInstall(spec.minEra(), era)) {
            return null;
        }
        final String needed = spec.minEra().name();
        return ICliComputer.OpResult.fail(spec.commandName() + " needs "
                + (needed.charAt(0) + needed.substring(1).toLowerCase(Locale.ROOT))
                + " hardware or later");
    }

    /** Why a program made for another kind of machine does not install on this one, or null when it does. */
    @Nullable
    ICliComputer.OpResult machine(final ProgramSpec spec) {
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (spec.hostScope() == HostScope.MAINFRAME && !(machine instanceof MainframeBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName() + " only installs on the Mainframe");
        }
        if (spec.hostScope() == HostScope.SERVER && !(machine instanceof ServerRackBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName() + " only installs on a server in a rack");
        }
        if (spec.hostScope() == HostScope.CLUSTER_MANAGEMENT_COMPUTER
                && !(machine instanceof ClusterManagementComputerBlockEntity)) {
            return ICliComputer.OpResult.fail(spec.commandName()
                    + " only installs on a Cluster Management Computer");
        }
        return null;
    }

    /**
     * Whether that machine meets what the program asks of its hardware and has the room for it.
     *
     * @param fromSource whether it is about to be built there from source, which asks a little less of both
     */
    static boolean fits(final IOsHost host, final ProgramSpec spec, final boolean fromSource) {
        return OsRegistry.canInstallProgram(host.installedOsId(), spec.id(), host.maxCpuMhz(), host.totalVramMb(),
                host.systemDiskFreeMb(), fromSource);
    }
}
