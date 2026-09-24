/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Reads a machine for the things its installer has to ask about: the disks it has, what each already holds, and
 * the desktops a Mirror could serve along with the system.
 *
 * <p>Everything here is read the moment it is wanted, and nothing of it is saved. What is saved with the machine
 * are the answers, so an installer that comes back after a reload is built again from the machine as it is now:
 * a disk pulled out while nobody was looking is gone from the list, which is the truth.
 */
public final class Installers {

    private Installers() {
    }

    /**
     * The disks the installer can offer, in slot order.
     *
     * <p>What is free on a disk is what would really fit on it, so the free room is counted in whole units of
     * whatever this generation of disk stores a file in, and the system already on it is part of what is taken.
     * Counting it any other way would let a page offer a disk that the write then refuses.
     */
    public static List<InstallerFlow.Disk> disksOf(final IOsHost machine) {
        final List<InstallerFlow.Disk> disks = new ArrayList<>();
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack stack = machine.diskInSlot(slot);
            if (!(stack.getItem() instanceof DiskItem disk)) {
                continue;
            }
            final HardwareEra era = disk.spec().era();
            final long sizeMb = disk.spec().capacityItems() * era.mbPerItem();
            final long freeMb = OsDisks.systemDiskFreeWeight(stack) / StorageKey.MB_EQ_PER_ITEM * era.mbPerItem();
            disks.add(new InstallerFlow.Disk(slot, stack.getHoverName().getString(),
                    (int) Math.min(Integer.MAX_VALUE, sizeMb), (int) Math.min(Integer.MAX_VALUE, freeMb),
                    holderOf(stack), disk.spec().tier().speedMultiplier()));
        }
        return disks;
    }

    /**
     * The desktops a Mirror could serve with that system on a machine of that generation.
     *
     * <p>A desktop cannot predate the hardware it was written for, and it has to be a desktop the system's own
     * platform runs, which is why a machine of the first age is offered none at all.
     */
    public static List<InstallerFlow.Desktop> desktopsFor(final OsDef system, final HardwareEra era,
                                                          final int eraFactor) {
        final List<InstallerFlow.Desktop> desktops = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.kind() != ProgramKind.DESKTOP_ENVIRONMENT || OsRegistry.getDesktop(spec.id()) == null) {
                continue;
            }
            if (!spec.platforms().contains(system.platform()) || era == null || era.level() < spec.minEra().level()) {
                continue;
            }
            desktops.add(new InstallerFlow.Desktop(spec.id().toString(), spec.displayName(), spec.minDiskMb(),
                    SetupTiming.networkTicks(spec.minDiskMb(), false, eraFactor)));
        }
        return desktops;
    }

    /** The Mirror serving this machine, by the name its Mainframe goes by; empty when none answers. */
    public static String mirrorHost(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        return mainframe == null || !mainframe.isMirrorActive() ? "" : nameOf(mainframe);
    }

    /**
     * The Mainframe of the network this machine is on, by the name it goes by; empty when it is on none.
     *
     * <p>Being on a network and being served by a Mirror are two different things, and a machine can be the
     * first without the second: the Mainframe may simply not be running one.
     */
    public static String networkHost(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        return mainframe == null ? "" : nameOf(mainframe);
    }

    /**
     * What the Mainframe of this machine's network is running, named, or nothing when it runs nothing.
     *
     * <p>Beside {@link #networkHost} and {@link #mirrorHost} because it answers the same kind of question about
     * the same Mainframe: what a machine finds when it looks up its own network.
     *
     * <p>The services are named as they are sold, which is data in every language.
     */
    public static Text servicesOn(final IOsHost machine, final ServerLevel level) {
        final MainframeBlockEntity mainframe = mainframeOf(machine, level);
        if (mainframe == null) {
            return Text.EMPTY;
        }
        return Text.literal(String.join(", ", Stream.of(
                        mainframe.isIqlEngineInstalled() ? "IQL Engine" : null,
                        mainframe.isAutomationEngineInstalled() ? "Automation Engine" : null,
                        mainframe.isMirrorInstalled() ? "Mirror" : null)
                .filter(Objects::nonNull).toList()));
    }

    /** The Mainframe orchestrating this machine's network, or null when it belongs to none. */
    @Nullable
    private static MainframeBlockEntity mainframeOf(final IOsHost machine, final ServerLevel level) {
        final NetworkUuid net = machine.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(net)
                .map(pos -> level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** A Mainframe by the name it goes by, or the plain word when it was never given one. */
    private static String nameOf(final MainframeBlockEntity mainframe) {
        final String name = mainframe.console() == null ? "" : mainframe.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /**
     * An installation about to be set up on this machine, with the disk and the name it suggests already filled
     * in and the desktops it could offer gathered.
     *
     * @param baseRate what this machine copies at with the disk left out, in megabytes a second: the medium it
     *                 reads from and the processor that unpacks it. The disk joins it when one is chosen
     */
    public static InstallerFlow beginning(final IOsHost machine, final ServerLevel level, final OsDef system,
                                          final double baseRate) {
        final HardwareEra era = machine.installedEra();
        final String mirror = system.installerStyle().offersDesktop() ? mirrorHost(machine, level) : "";
        final List<InstallerFlow.Desktop> desktops = mirror.isEmpty()
                ? List.of() : desktopsFor(system, era, SetupTiming.eraFactor(era));
        return InstallerFlow.beginning(system.installerStyle(), system.id().toString(), system.displayName(),
                system.footprintMb(), baseRate, disksOf(machine), machineName(machine), desktops, mirror);
    }

    /**
     * An installation put back on a machine as it stands now, with the answers it had been given.
     *
     * <p>The disks and the desktops are read again rather than remembered, so a disk pulled out while the world
     * was away is simply not on the list any more.
     */
    public static InstallerFlow restored(final IOsHost machine, final ServerLevel level, final OsDef system,
                                         final int copyTicks, final int stageIndex, final int targetSlot,
                                         final String computerName, final String desktopId, final int eraseSlot) {
        final HardwareEra era = machine.installedEra();
        final String mirror = system.installerStyle().offersDesktop() ? mirrorHost(machine, level) : "";
        final List<InstallerFlow.Desktop> desktops = mirror.isEmpty()
                ? List.of() : desktopsFor(system, era, SetupTiming.eraFactor(era));
        return InstallerFlow.restored(system.installerStyle(), system.id().toString(), system.displayName(),
                system.footprintMb(), copyTicks, disksOf(machine), desktops, mirror, stageIndex, targetSlot,
                computerName, desktopId, eraseSlot);
    }

    /**
     * What this computer calls itself: the name it was given, else the one on the assembly screen, else the
     * plain word.
     *
     * <p>Not the host name, which is this lowered and hyphenated for the network. This is the name a player
     * typed and should see written back to them.
     */
    public static String machineName(final IOsHost machine) {
        final String console = machine.console() == null ? "" : machine.console().computerName();
        if (console != null && !console.isBlank()) {
            return console;
        }
        final String custom = machine.customName();
        return custom == null || custom.isBlank() ? "computer" : custom;
    }

    /**
     * The name this computer answers to on a network and at a Unix prompt: its own name lowered and hyphenated,
     * or its system's when it was never given one. What a system writes about its host name on its way up has
     * to be this, since it is what the prompt will say a moment later.
     */
    public static String hostName(final IOsHost machine) {
        return machine instanceof IComputerTerminalHost terminal ? terminal.hostname() : machineName(machine);
    }

    /** The system already on that disk, by the name a person reads, or empty when it carries none. */
    private static String holderOf(final ItemStack disk) {
        final ResourceLocation osId = OsDisks.systemOn(disk);
        @Nullable final OsDef held = osId == null ? null : OsRegistry.getOs(osId);
        return held == null ? "" : held.displayName();
    }
}
