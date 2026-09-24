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

/**
 * The walkthrough a live medium carries, which is the one thing on it that tells a player what to do.
 *
 * <p>The real media ship exactly this: a plain file in the root user's home naming the steps in order. It is
 * written from the steps the sequence really accepts, so following it works, and it says which steps a system
 * cannot boot without and which are the rest of the handbook.
 *
 * <p>It is read at a terminal, so it is written well inside a monitor's columns: a step on a line of its own
 * and what it is for underneath, because a line that runs past the edge is broken wherever the edge happens to
 * be.
 *
 * <p>The walkthrough is a file on the medium, so it is data in the machine's own language like any other file, and
 * reads the same whatever language the player has; what {@code help} says at the terminal is the tool talking, and
 * is read in the player's.
 */
@TextHolder
final class LiveGuide {

    private static final Text ARCH = Text.literal(String.join("\n",
            "Installing Arch Linux by hand.",
            "",
            "Steps marked * are the ones a system cannot boot without.",
            "On older firmware the root is the whole disk, /dev/sda, and the",
            "steps that say so below are left out.",
            "",
            "   lsblk",
            "       see the disks",
            " * fdisk /dev/sda",
            "       modern firmware only: g, n (+512M), t (1), n, w",
            " * mkfs.fat -F 32 /dev/sda1",
            "       modern firmware only: the partition the firmware reads",
            " * mkfs.ext4 /dev/sda2",
            " * mount /dev/sda2 /mnt",
            " * mount --mkdir /dev/sda1 /mnt/boot",
            "       modern firmware only",
            " * pacstrap -K /mnt base linux linux-firmware",
            "       the base system, from the Mirror",
            " * genfstab -U /mnt >> /mnt/etc/fstab",
            "       the table of what to mount",
            " * arch-chroot /mnt",
            "       step into the new system",
            "   hwclock --systohc",
            "   echo <name> > /etc/hostname",
            "       name the machine",
            "   mkinitcpio -P",
            "       only after changing /etc/mkinitcpio.conf",
            "   pacman -S <package>",
            "       anything the Mirror has, to be there when the system comes up",
            " * pacman -S grub efibootmgr",
            "       the bootloader is a package",
            " * grub-install --target=x86_64-efi --efi-directory=/boot",
            "       older firmware: grub-install /dev/sda",
            " * grub-mkconfig -o /boot/grub/grub.cfg",
            "       what the bootloader should start",
            " * exit",
            " * umount -R /mnt",
            " * reboot",
            "",
            "A Mainframe on this network must be running the Mirror.",
            "CTRL+C stops a tool that is running."));

    private static final Text GENTOO = Text.literal(String.join("\n",
            "Installing Gentoo by hand.",
            "",
            "Steps marked * are the ones a system cannot boot without.",
            "On older firmware the root is the whole disk, /dev/sda, and the",
            "steps that say so below are left out.",
            "",
            "   lsblk",
            "       see the disks",
            " * fdisk /dev/sda",
            "       modern firmware only: g, n (+512M), t (1), n, w",
            " * mkfs.fat -F 32 /dev/sda1",
            "       modern firmware only: the partition the firmware reads",
            " * mkfs.ext4 /dev/sda2",
            " * mount /dev/sda2 /mnt/gentoo",
            " * cd /mnt/gentoo",
            " * wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz",
            " * tar xpvf stage3-*.tar.xz --xattrs-include='*.*'",
            "   nano -w /mnt/gentoo/etc/portage/make.conf",
            "       MAKEOPTS=\"-j4\" makes every compile four jobs wide",
            "   mount --types proc /proc /mnt/gentoo/proc",
            "       and --rbind /sys, /dev and /run the same way",
            " * mount /dev/sda1 /mnt/gentoo/efi",
            "       modern firmware only",
            " * chroot /mnt/gentoo /bin/bash",
            "       then: source /etc/profile",
            " * emerge-webrsync",
            "       the package tree",
            "   eselect profile list",
            "       then: eselect profile set <n>",
            "   emerge --ask --verbose --update --deep --newuse @world",
            " * emerge sys-kernel/gentoo-sources",
            "       the kernel sources",
            "   eselect kernel set 1",
            " * genkernel all",
            "       or, in /usr/src/linux:",
            "       make -j4 && make modules_install && make install",
            " * blkid >> /etc/fstab",
            "       then: nano /etc/fstab, and cut the root's line down to",
            "       UUID=<its uuid>  /  ext4  noatime  0 1",
            "   echo <name> > /etc/hostname",
            "       name the machine",
            "   emerge --ask kde-plasma/plasma-meta",
            "       a desktop, or any program the Mirror has; a profile only",
            "       says how things are built, it installs nothing",
            " * emerge --ask sys-boot/grub",
            "       the bootloader is a package, and it compiles",
            " * grub-install --efi-directory=/efi",
            "       older firmware: grub-install /dev/sda",
            " * grub-mkconfig -o /boot/grub/grub.cfg",
            "       what the bootloader should start",
            " * exit",
            " * cd",
            " * umount -R /mnt/gentoo",
            " * reboot",
            "",
            "A Mainframe on this network must be running the Mirror.",
            "CTRL+C stops a tool that is running."));

    /*
     * What help says, a line each, kept as short as the lines were so it still fits the monitor's columns. The one
     * sentence that runs over two lines is two keys, so the break stays where it reads well.
     */
    private static final TextKey HELP_MEDIUM = TextKey.of("jsc.install.live_guide.help_medium",
            "This is the %s installation medium.");
    private static final TextKey HELP_BY_HAND = TextKey.of("jsc.install.live_guide.help_by_hand",
            "Nothing here installs itself: the steps are typed by hand, in");
    private static final TextKey HELP_REAL_TOOLS = TextKey.of("jsc.install.live_guide.help_real_tools",
            "order, and every tool is the real one.");
    private static final TextKey HELP_WALKTHROUGH = TextKey.of("jsc.install.live_guide.help_walkthrough",
            "The whole walkthrough is in %s");
    private static final TextKey HELP_STOP = TextKey.of("jsc.install.live_guide.help_stop",
            "A tool that is running can be stopped with CTRL+C.");

    private LiveGuide() {
    }

    /** The walkthrough file, in the English every file on a machine is kept in. */
    static String of(final LiveInstallState.Distro distro) {
        return (distro == LiveInstallState.Distro.ARCH ? ARCH : GENTOO).english();
    }

    /** What {@code help} says, which is where the walkthrough is. */
    static Text[] help(final LiveInstallState.Distro distro) {
        return new Text[]{
            HELP_MEDIUM.with(Text.literal(distro == LiveInstallState.Distro.ARCH ? "Arch Linux" : "Gentoo")),
            HELP_BY_HAND.text(),
            HELP_REAL_TOOLS.text(),
            Text.EMPTY,
            HELP_WALKTHROUGH.with("/root/install.txt"),
            Text.literal("    less /root/install.txt"),
            HELP_STOP.text()};
    }
}
