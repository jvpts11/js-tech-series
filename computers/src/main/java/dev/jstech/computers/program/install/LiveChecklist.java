/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.ArrayList;
import java.util.List;

/**
 * What stands between an installation by hand and a system that comes up.
 *
 * <p>Two lists. The first is what no system boots without, and is always asked for: something to boot, a
 * table of what to mount, a bootloader that is installed and has been told what to start, and a way in. The
 * second is the rest of what the handbooks have you do, the clock, the locale, the name, the bind mounts, the
 * profile, which a system comes up without and is the worse for. Whether a world asks for the second as well
 * is the world's own setting, one for each distribution, because how much of the handbook an installation
 * should be is a matter of taste.
 */
final class LiveChecklist {

    private LiveChecklist() {
    }

    /**
     * Everything still missing, each by the step that provides it.
     *
     * @param tableByHand whether a table written by hand names a root, for the distribution that writes its own
     * @param chosenName  the name the machine was given, empty when it was given none
     * @param everyStep   whether this world asks for the whole handbook
     */
    static List<String> missing(final LiveInstallState.Distro distro, final LiveProgress progress,
                                final boolean tableByHand, final LiveDisks disks, final String chosenName,
                                final boolean everyStep) {
        final boolean gentoo = distro == LiveInstallState.Distro.GENTOO;
        final List<String> out = new ArrayList<>();
        if (!progress.base) {
            out.add("no base system installed (" + (gentoo ? "wget the stage 3, then tar" : "pacstrap") + ")");
        }
        if (gentoo ? !tableByHand : !progress.fstab) {
            out.add(gentoo ? "no root filesystem in /etc/fstab (blkid, then write the line)"
                    : "no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)");
        }
        if (!progress.kernelBuilt) {
            out.add("no kernel (genkernel all, or make in /usr/src/linux)");
        }
        if (!progress.bootloader) {
            out.add("no bootloader (grub-install)");
        }
        /* A bootloader with nothing to start starts nothing, so its list counts as much as it does. */
        if (!progress.grubConfig) {
            out.add("nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)");
        }
        if (!progress.password) {
            out.add("no root password (passwd)");
        }
        if (everyStep) {
            wholeHandbook(out, gentoo, progress, disks, chosenName);
        }
        return out;
    }

    /** What the handbook asks for beyond what a system boots without. */
    private static void wholeHandbook(final List<String> out, final boolean gentoo, final LiveProgress progress,
                                      final LiveDisks disks, final String chosenName) {
        if (gentoo && !disks.boundIn()) {
            out.add("/proc, /sys, /dev and /run were not bound into the new system (mount --rbind, --types proc)");
        }
        if (gentoo && !progress.profileChosen) {
            out.add("no profile chosen (eselect profile list, then set)");
        }
        if (gentoo && !progress.worldUpdated) {
            out.add("the system was not brought up to date (emerge --ask --verbose --update --deep --newuse @world)");
        }
        if (gentoo && !progress.kernelChosen) {
            out.add("/usr/src/linux points nowhere (eselect kernel set 1)");
        }
        if (!progress.timezone) {
            out.add(gentoo ? "no time zone (echo a zone into /etc/timezone, emerge --config sys-libs/timezone-data)"
                    : "no time zone (ln -sf /usr/share/zoneinfo/<Region>/<City> /etc/localtime)");
        }
        if (!gentoo && !progress.clockSet) {
            out.add("the hardware clock was not set (hwclock --systohc)");
        }
        if (!progress.localesGenerated) {
            out.add("no locale generated (uncomment one in /etc/locale.gen, then locale-gen)");
        }
        if (chosenName.isEmpty()) {
            out.add("the machine has no name (echo <name> > /etc/hostname)");
        }
    }
}
