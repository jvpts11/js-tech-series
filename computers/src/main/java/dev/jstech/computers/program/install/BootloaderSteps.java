/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.install.voice.BootVoices;

/**
 * The bootloader's two steps, which both distributions take in the same words: putting it where this
 * machine's firmware looks for one, and writing the list of what it can start.
 *
 * <p>Neither tool is on the live medium. They belong to the system being built, and only once the package
 * that carries them has been installed into it, so asked for before that they are as absent as the real ones.
 */
final class BootloaderSteps {

    private final LiveInstallState.Distro distro;
    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress;

    /** How long each of the two takes, which is a moment whatever the machine. */
    private static final int INSTALL_TICKS = 34;
    private static final int CONFIG_TICKS = 50;

    private static final String EFI_FLAG = "--efi-directory=";

    BootloaderSteps(final LiveInstallState.Distro distro, final LiveFiles files, final LiveDisks disks,
                    final LiveProgress progress) {
        this.distro = distro;
        this.files = files;
        this.disks = disks;
        this.progress = progress;
    }

    /**
     * Installs the bootloader, for the firmware this machine really has.
     *
     * <p>A machine of the older firmware takes it on the disk itself and is named a disk. A machine of the
     * modern one takes it in the partition the firmware reads, and has to be told where that is mounted unless
     * it is where the tool looks by default.
     */
    LiveTurn install(final String[] parts, final LiveInstallState.Env env) {
        final boolean arch = this.distro == LiveInstallState.Distro.ARCH;
        if (!this.files.inside()) {
            return LiveTurn.refused("grub-install: command not found (it is the new system's: step into it first)");
        }
        if (!this.progress.grubPackage) {
            return LiveTurn.refused(arch ? "zsh: command not found: grub-install"
                    : "bash: grub-install: command not found", "      (the bootloader is a package: "
                    + (arch ? "pacman -S grub efibootmgr" : "emerge --ask sys-boot/grub") + ")");
        }
        if (!this.progress.kernelBuilt) {
            return LiveTurn.refused("grub-install: warning: no kernel image found in /boot.",
                    "              (build one first: genkernel all, or make in /usr/src/linux)");
        }
        final Runnable installed = () -> this.progress.bootloader = true;
        if (env.uefi()) {
            String asked = "/boot/efi";
            for (final String part : parts) {
                if (part.startsWith(EFI_FLAG)) {
                    asked = part.substring(EFI_FLAG.length());
                }
            }
            if (this.disks.espMount().isEmpty() || !this.disks.espMount().equals(this.files.root() + asked)) {
                return LiveTurn.refused("grub-install: error: " + asked + " doesn't look like an EFI partition.",
                        "              (mount the EFI partition inside the new system, then name where:",
                        "               grub-install --efi-directory=/boot or /efi)");
            }
            return LiveTurn.running(BootVoices.grubInstall("x86_64-efi", INSTALL_TICKS, installed));
        }
        final String dev = LiveDisks.deviceName(parts[parts.length - 1]);
        if (parts.length < 2 || dev.startsWith("-")) {
            return LiveTurn.refused("grub-install: error: install device isn't specified.");
        }
        final String disk = LiveDisks.diskOf(this.disks.rootDevice());
        if (!dev.equals(disk)) {
            return LiveTurn.refused("grub-install: error: cannot find a device for /dev/" + dev + ".",
                    "              (it goes on the disk, not in a partition: grub-install /dev/" + disk + ")");
        }
        return LiveTurn.running(BootVoices.grubInstall("i386-pc", INSTALL_TICKS, installed));
    }

    /** Writes the bootloader's list of what it can start, from the system that is really installed. */
    LiveTurn config(final String line, final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("grub-mkconfig: command not found (it is the new system's: step into it first)");
        }
        if (!this.progress.bootloader) {
            return LiveTurn.refused("/usr/bin/grub-mkconfig: /boot/grub: No such file or directory",
                    "      (install the bootloader first: grub-install)");
        }
        if (!line.contains("-o")) {
            return LiveTurn.refused("Usage: grub-mkconfig -o /boot/grub/grub.cfg");
        }
        final boolean arch = this.distro == LiveInstallState.Distro.ARCH;
        final String kernel = arch ? "/boot/vmlinuz-linux" : "/boot/vmlinuz-" + GentooSteps.KERNEL + "-gentoo-x86_64";
        final String initrd = arch ? "/boot/initramfs-linux.img"
                : "/boot/initramfs-" + GentooSteps.KERNEL + "-gentoo-x86_64.img";
        final String uuid = this.disks.rootUuid(env);
        return LiveTurn.running(BootVoices.grubMkconfig(kernel, initrd, env.uefi(), CONFIG_TICKS, () -> {
            this.progress.grubConfig = true;
            this.files.write(this.files.inNewSystem("/boot/grub/grub.cfg"), String.join("\n",
                    "# generated by grub-mkconfig",
                    "menuentry '" + (arch ? "Arch Linux" : "Gentoo Linux") + "' {",
                    "        set root='hd0'",
                    "        linux " + kernel + " root=UUID=" + uuid + " rw",
                    "        initrd " + initrd,
                    "}"));
        }));
    }
}
