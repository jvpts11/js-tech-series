/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a FreeBSD machine reads out on its way up and on its way down.
 *
 * <p>It reads nothing like a Linux, and that is the point of giving it lines of its own. Its kernel stamps no
 * times and marks nothing: it names the machine plainly, a line to a part, and its disks are {@code ada0} and
 * not {@code sda}. Then the startup scripts say "Starting" and the name of each thing, with a full stop and no
 * word on how it went, because on this system a service that did not start says so and one that did says no
 * more. It stops the same way in reverse, and ends on the sentence that system has always ended on.
 *
 * <p>Every line is about this machine: the processor it has, the memory, the disks that are in it, a network
 * only when a cable reaches one, the Mirror only when one answers, a display manager only when a desktop is
 * installed.
 */
final class BsdBootLines {

    private BsdBootLines() {
    }

    /** The kernel naming the machine, then the startup scripts bringing the system up. */
    static BootSequence up(final IOsHost machine, final OsDef system, @Nullable final ServerLevel level) {
        final HardwareEra era = machine.installedEra() != null ? machine.installedEra() : HardwareEra.STANDARD;
        final String arch = KernelNames.architecture(Platform.FREEBSD, machine.processorBits());
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Loading kernel...")
                .subtitle("Booting...");
        out.line(Branding.systemCopyright(system.displayName(), era));
        out.line("FreeBSD " + KernelNames.FREEBSD_RELEASE + " GENERIC " + arch);
        out.line("CPU: " + BootLines.cpuName(machine));
        out.line("real memory  = " + machine.ramTotalMb() + " MB");
        int drive = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (disk.getItem() instanceof DiskItem) {
                out.line("ada" + drive++ + ": <" + disk.getHoverName().getString() + ">");
            }
        }
        out.line("Trying to mount root from ufs:/dev/ada0p2 [rw]...");
        out.line("Setting hostname: " + Installers.hostName(machine) + ".");
        if (machine.networkAttached()) {
            out.line("Starting Network: lo0 em0.");
        }
        final String mirror = level == null ? "" : Installers.mirrorHost(machine, level);
        if (!mirror.isEmpty()) {
            out.line("pkg: repository Mirror found on " + mirror + ".");
        }
        out.line("Starting devd.");
        out.line("Starting syslogd.");
        final String desktop = desktopOf(machine);
        if (!desktop.isEmpty()) {
            out.line("Starting " + desktop + " display manager.");
        }
        return out.build();
    }

    /**
     * The same machine stopping: the desktop, the network, the scripts, then the disks, and the last sentence.
     *
     * @param restarting the machine is coming straight back up, which this system says in one word
     */
    static BootSequence down(final IOsHost machine, final boolean restarting) {
        final BootSequence.Builder out = new BootSequence.Builder().title("").subtitle("");
        final String desktop = desktopOf(machine);
        if (!desktop.isEmpty()) {
            out.line("Stopping " + desktop + " display manager.");
        }
        out.line("Stopping syslogd.");
        out.line("Stopping devd.");
        if (machine.networkAttached()) {
            out.line("Stopping Network: em0.");
        }
        out.line("Writing entropy file: .");
        out.line("Terminated");
        out.line("Syncing disks, vnodes remaining... 0 0 done");
        out.line("All buffers synced.");
        out.line(restarting ? "Rebooting..." : "The operating system has halted.");
        return out.build();
    }

    /** The desktop installed on the machine as its startup names it, or nothing for a machine at a terminal. */
    private static String desktopOf(final IOsHost machine) {
        return machine.installedDesktopId() == null ? ""
                : machine.installedDesktopId().getPath().replace('_', ' ');
    }
}
