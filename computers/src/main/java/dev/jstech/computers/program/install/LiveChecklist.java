/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * What stands between an installation by hand and a system that comes up.
 *
 * <p>Two lists. The first is what no system boots without, and is always asked for: something to boot, a
 * table of what to mount, and a bootloader that is installed and has been told what to start. The second is
 * the rest of what the handbooks have you do, the clock, the name, the bind mounts, the profile, which a
 * system comes up without and is the worse for. Whether a world asks for the second as well is the world's
 * own setting, one for each distribution, because how much of the handbook an installation should be is a
 * matter of taste.
 *
 * <p>Neither list has a root password, a time zone or a locale on it. Nothing in this world logs in with a
 * password yet, the game has no time zones, and the language a player reads in is the one they chose in the
 * game's own options, so a step that set any of them would be setting nothing.
 */
@TextHolder
final class LiveChecklist {

    /* Each item names what is missing, and in brackets the step that provides it, typed as it is typed. */
    private static final TextKey NO_BASE_GENTOO = TextKey.of("jsc.install.live_checklist.no_base_gentoo",
            "no base system installed (wget the stage 3, then tar)");
    private static final TextKey NO_BASE_ARCH = TextKey.of("jsc.install.live_checklist.no_base_arch",
            "no base system installed (pacstrap)");
    private static final TextKey NO_ROOT_IN_FSTAB = TextKey.of("jsc.install.live_checklist.no_root_in_fstab",
            "no root filesystem in /etc/fstab (blkid, then write the line)");
    private static final TextKey NO_FSTAB = TextKey.of("jsc.install.live_checklist.no_fstab",
            "no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)");
    private static final TextKey NO_KERNEL = TextKey.of("jsc.install.live_checklist.no_kernel",
            "no kernel (genkernel all, or make in /usr/src/linux)");
    private static final TextKey NO_BOOTLOADER = TextKey.of("jsc.install.live_checklist.no_bootloader",
            "no bootloader (grub-install)");
    private static final TextKey NOTHING_TO_START = TextKey.of("jsc.install.live_checklist.nothing_to_start",
            "nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)");
    private static final TextKey NOT_BOUND = TextKey.of("jsc.install.live_checklist.not_bound",
            "/proc, /sys, /dev and /run were not bound into the new system (mount --rbind, --types proc)");
    private static final TextKey NO_PROFILE = TextKey.of("jsc.install.live_checklist.no_profile",
            "no profile chosen (eselect profile list, then set)");
    private static final TextKey NOT_UP_TO_DATE = TextKey.of("jsc.install.live_checklist.not_up_to_date",
            "the system was not brought up to date (emerge --ask --verbose --update --deep --newuse @world)");
    private static final TextKey NO_KERNEL_LINK = TextKey.of("jsc.install.live_checklist.no_kernel_link",
            "/usr/src/linux points nowhere (eselect kernel set 1)");
    private static final TextKey NO_CLOCK = TextKey.of("jsc.install.live_checklist.no_clock",
            "the hardware clock was not set (hwclock --systohc)");
    private static final TextKey NO_NAME = TextKey.of("jsc.install.live_checklist.no_name",
            "the machine has no name (echo <name> > /etc/hostname)");

    private LiveChecklist() {
    }

    /**
     * Everything still missing, each by the step that provides it.
     *
     * @param tableByHand whether a table written by hand names a root, for the distribution that writes its own
     * @param chosenName  the name the machine was given, empty when it was given none
     * @param everyStep   whether this world asks for the whole handbook
     */
    static List<Text> missing(final LiveInstallState.Distro distro, final LiveProgress progress,
                              final boolean tableByHand, final LiveDisks disks, final String chosenName,
                              final boolean everyStep) {
        final boolean gentoo = distro == LiveInstallState.Distro.GENTOO;
        final List<Text> out = new ArrayList<>();
        if (!progress.base) {
            out.add((gentoo ? NO_BASE_GENTOO : NO_BASE_ARCH).text());
        }
        if (gentoo ? !tableByHand : !progress.fstab) {
            out.add((gentoo ? NO_ROOT_IN_FSTAB : NO_FSTAB).text());
        }
        if (!progress.kernelBuilt) {
            out.add(NO_KERNEL.text());
        }
        if (!progress.bootloader) {
            out.add(NO_BOOTLOADER.text());
        }
        /* A bootloader with nothing to start starts nothing, so its list counts as much as it does. */
        if (!progress.grubConfig) {
            out.add(NOTHING_TO_START.text());
        }
        if (everyStep) {
            wholeHandbook(out, gentoo, progress, disks, chosenName);
        }
        return out;
    }

    /** What the handbook asks for beyond what a system boots without. */
    private static void wholeHandbook(final List<Text> out, final boolean gentoo, final LiveProgress progress,
                                      final LiveDisks disks, final String chosenName) {
        if (gentoo && !disks.boundIn()) {
            out.add(NOT_BOUND.text());
        }
        if (gentoo && progress.profile == 0) {
            out.add(NO_PROFILE.text());
        }
        if (gentoo && !progress.worldUpdated) {
            out.add(NOT_UP_TO_DATE.text());
        }
        if (gentoo && !progress.kernelChosen) {
            out.add(NO_KERNEL_LINK.text());
        }
        if (!gentoo && !progress.clockSet) {
            out.add(NO_CLOCK.text());
        }
        if (chosenName.isEmpty()) {
            out.add(NO_NAME.text());
        }
    }
}
