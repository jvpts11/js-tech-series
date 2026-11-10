/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/**
 * What an OpenBSD machine reads out on its way up and on its way down (computers/docs/design/operating-systems.md).
 *
 * <p>Its kernel names itself and its build, the memory twice, the processor, and each disk by its bus, {@code wd} on
 * the IDE of the older machines and {@code sd} on SATA, with the root on the first; then rc checks the file systems,
 * enables pf, starts the network when a cable reaches one and the daemons the machine has switched on, and prints the
 * date. A display manager only when {@code rcctl enable xenodm} switched one on: a desktop installed is not a desktop
 * started, which is what secure by default means.
 */
final class OpenBsdBootLines {

    /* The label every disk of these machines carries, which the root and the checks name beside the device. */
    private static final String DUID = DiskCheck.OPENBSD_DUID;

    private OpenBsdBootLines() {
    }

    /** What the kernel itself prints as it comes up, up to where it mounts its root. */
    static List<String> kernel(final IOsHost machine, final OsDef system) {
        final Era era = machine.installedEra() != null ? machine.installedEra() : HardwareEra.LEGACY;
        final String arch = KernelNames.architecture(Platform.OPENBSD, machine.processorBits());
        final long ramBytes = machine.ramTotalMb() * 1_048_576L;
        final List<String> out = new ArrayList<>();
        out.add("booting hd0a:/bsd: 9348176+2245636+225296+0+1093632 [692498+107+567200+584283]=0xd6c1a4");
        out.add("entry point at 0x2000d4");
        out.add(Branding.systemCopyright(system.displayName(), era));
        out.add("OpenBSD " + KernelNames.OPENBSD_RELEASE + " (GENERIC) #79: " + arch);
        out.add("real mem  = " + ramBytes + " (" + machine.ramTotalMb() + "MB)");
        final long avail = ramBytes * 92 / 100;
        out.add("avail mem = " + avail + " (" + avail / 1_048_576L + "MB)");
        out.add("cpu0: " + BootLines.cpuName(machine));
        int ide = 0;
        int sata = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk) || !(HardwareRegistry.disk(disk) instanceof DiskSpec spec)) {
                continue;
            }
            final boolean onIde = Installers.ideDisk(spec.era());
            final int index = onIde ? ide++ : sata++;
            final String name = (onIde ? "wd" : "sd") + index;
            out.add(onIde ? name + " at pciide0 channel " + index / 2 + " drive " + index % 2 + ": <"
                    + disk.getHoverName().getString() + ">"
                    : name + " at scsibus1 targ " + index + " lun 0: <" + disk.getHoverName().getString() + ">");
            out.add(name + ": " + spec.capacityMb() + "MB, " + spec.capacityMb() * 2_048L + " sectors");
        }
        out.add("cd0 at scsibus0 targ 0 lun 0: <Internal CD Drive>");
        if (machine.networkAttached()) {
            out.add("em0 at pci0 dev 3 function 0: address 52:54:00:3a:91:0c");
        }
        final String root = rootDisk(machine);
        out.add("root on " + root + "a (" + DUID + ".a) swap on " + root + "b dump on " + root + "b");
        return out;
    }

    /* The first disk the kernel finds, which holds the root: wd0 on IDE, sd0 on SATA. */
    private static String rootDisk(final IOsHost machine) {
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (DriveVolumes.isDisk(disk) && HardwareRegistry.disk(disk) instanceof DiskSpec spec) {
                return Installers.ideDisk(spec.era()) ? "wd0" : "sd0";
            }
        }
        return "wd0";
    }

    /** The kernel naming the machine, then rc bringing the system up. */
    static BootSequence up(final IOsHost machine, final OsDef system) {
        // The kernel's and rc's lines are logs, which are data in every language.
        final BootSequence.Builder out = new BootSequence.Builder().title(Text.EMPTY).subtitle(Text.EMPTY);
        for (final String line : kernel(machine, system)) {
            out.line(Text.literal(line));
        }
        final String root = rootDisk(machine);
        out.line(Text.literal("Automatic boot in progress: starting file system checks."));
        out.line(Text.literal("/dev/" + root + "a (" + DUID + ".a): file system is clean; not checking"));
        out.line(Text.literal("pf enabled"));
        if (machine.networkAttached()) {
            out.line(Text.literal("starting network"));
        }
        out.line(Text.literal("starting early daemons: syslogd pflogd."));
        out.line(Text.literal("savecore: no core dump"));
        out.line(Text.literal("clearing /tmp"));
        out.line(Text.literal("kern.securelevel: 0 -> 1"));
        final boolean sshd = machine.console() != null && machine.console().settings().remoteAllowed();
        out.line(Text.literal("starting network daemons:" + (sshd ? " sshd" : "") + "."));
        out.line(Text.literal("starting package daemons:."));
        final boolean cron = machine.console() != null && machine.console().settings().cronEnabled();
        final boolean xenodm = machine.console() != null && machine.console().settings().xenodmEnabled()
                && !BootLines.desktopOf(machine).isEmpty();
        out.line(Text.literal("starting local daemons:" + (cron ? " cron" : "") + (xenodm ? " xenodm" : "") + "."));
        out.line(Text.literal(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss 'UTC' yyyy",
                Locale.ENGLISH))));
        return out.build();
    }

    /**
     * The machine stopping: the daemons, the disks synced, and the last line.
     *
     * @param restarting the machine is coming straight back up
     */
    static BootSequence down(final boolean restarting) {
        final BootSequence.Builder out = new BootSequence.Builder().title(Text.EMPTY).subtitle(Text.EMPTY);
        out.line(Text.literal("stopping package daemons:."));
        out.line(Text.literal("syncing disks... done"));
        if (restarting) {
            out.line(Text.literal("rebooting..."));
        } else {
            out.line(Text.literal("The operating system has halted."));
            out.line(Text.literal("Please press any key to reboot."));
        }
        return out.build();
    }
}
