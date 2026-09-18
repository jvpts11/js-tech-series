/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

/**
 * The walkthrough a live medium carries, which is the one thing on it that tells a player what to do.
 *
 * <p>The real media ship exactly this: a plain file in the root user's home naming the steps in order. It is
 * written from the steps the sequence really accepts, so following it works, and it says which steps a system
 * cannot boot without and which are the rest of the handbook.
 */
final class LiveGuide {

    private static final String ARCH = String.join("\n",
            "Installing Arch Linux by hand.",
            "",
            "Steps marked * are the ones a system cannot boot without.",
            "",
            "   lsblk                                       see the disks",
            " * fdisk /dev/sda                              modern firmware only: g, n (+512M), t (1), n, w",
            " * mkfs.fat -F 32 /dev/sda1                    modern firmware only: the partition it reads",
            " * mkfs.ext4 /dev/sda2                         the root (the whole disk, /dev/sda, on older firmware)",
            " * mount /dev/sda2 /mnt                        mount it",
            " * mount --mkdir /dev/sda1 /mnt/boot           modern firmware only",
            " * pacstrap -K /mnt base linux linux-firmware  the base system, from the Mirror",
            " * genfstab -U /mnt >> /mnt/etc/fstab          the table of what to mount",
            " * arch-chroot /mnt                            step into the new system",
            "   ln -sf /usr/share/zoneinfo/Europe/Lisbon /etc/localtime",
            "   hwclock --systohc",
            "   nano /etc/locale.gen, then locale-gen       uncomment a locale, then generate it",
            "   echo LANG=en_US.UTF-8 > /etc/locale.conf",
            "   echo <name> > /etc/hostname                 name the machine",
            "   mkinitcpio -P                               only after changing /etc/mkinitcpio.conf",
            " * passwd                                      a root password",
            " * pacman -S grub efibootmgr                   the bootloader is a package",
            " * grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=GRUB",
            "                                               (older firmware: grub-install /dev/sda)",
            " * grub-mkconfig -o /boot/grub/grub.cfg        what it should start",
            " * exit, umount -R /mnt, reboot",
            "",
            "A Mainframe on this network must be running the Mirror. CTRL+C stops a tool that is running.");

    private static final String GENTOO = String.join("\n",
            "Installing Gentoo by hand.",
            "",
            "Steps marked * are the ones a system cannot boot without.",
            "",
            "   lsblk                                       see the disks",
            " * fdisk /dev/sda                              modern firmware only: g, n (+512M), t (1), n, w",
            " * mkfs.fat -F 32 /dev/sda1                    modern firmware only: the partition it reads",
            " * mkfs.ext4 /dev/sda2                         the root (the whole disk, /dev/sda, on older firmware)",
            " * mount /dev/sda2 /mnt/gentoo                 mount it",
            " * cd /mnt/gentoo",
            " * wget mirror://mainframe/gentoo/stage3-amd64-openrc.tar.xz",
            " * tar xpvf stage3-*.tar.xz --xattrs-include='*.*' --numeric-owner",
            "   nano -w /mnt/gentoo/etc/portage/make.conf   MAKEOPTS=\"-j4\" makes every compile four jobs wide",
            "   mount --types proc /proc /mnt/gentoo/proc   and --rbind /sys, /dev and /run the same way",
            " * mount /dev/sda1 /mnt/gentoo/efi             modern firmware only",
            " * chroot /mnt/gentoo /bin/bash                then: source /etc/profile",
            " * emerge-webrsync                             the package tree",
            "   eselect profile list                        and eselect profile set <n>",
            "   emerge --ask --verbose --update --deep --newuse @world",
            "   echo Europe/Lisbon > /etc/timezone, then emerge --config sys-libs/timezone-data",
            "   nano /etc/locale.gen, then locale-gen",
            " * emerge sys-kernel/gentoo-sources            the kernel sources",
            "   eselect kernel set 1",
            " * genkernel all                               or: cd /usr/src/linux, then",
            "                                               make -j4 && make modules_install && make install",
            " * blkid, then nano /etc/fstab                 this table is written by hand",
            "   echo <name> > /etc/hostname                 name the machine",
            " * passwd                                      a root password",
            " * emerge --ask sys-boot/grub                  the bootloader is a package, and it compiles",
            " * grub-install --efi-directory=/efi           (older firmware: grub-install /dev/sda)",
            " * grub-mkconfig -o /boot/grub/grub.cfg        what it should start",
            " * exit, cd, umount -R /mnt/gentoo, reboot",
            "",
            "A Mainframe on this network must be running the Mirror. CTRL+C stops a tool that is running.");

    private LiveGuide() {
    }

    static String of(final LiveInstallState.Distro distro) {
        return distro == LiveInstallState.Distro.ARCH ? ARCH : GENTOO;
    }

    /** What {@code help} says, which is where the walkthrough is. */
    static String[] help(final LiveInstallState.Distro distro) {
        return new String[]{
            "This is the " + (distro == LiveInstallState.Distro.ARCH ? "Arch Linux" : "Gentoo")
                    + " installation medium. Nothing here installs itself:",
            "the steps are typed by hand, in order, and every tool is the real one.",
            "",
            "The whole walkthrough is in /root/install.txt   (less /root/install.txt)",
            "A tool that is running can be stopped with CTRL+C."};
    }
}
