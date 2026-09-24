/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.install.voice.BootVoices;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The bootloader's two steps, which both distributions take in the same words: putting it where this
 * machine's firmware looks for one, and writing the list of what it can start.
 *
 * <p>Neither tool is on the live medium. They belong to the system being built, and only once the package
 * that carries them has been installed into it, so asked for before that they are as absent as the real ones.
 */
@TextHolder
final class BootloaderSteps {

    private final LiveInstallState.Distro distro;
    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress;

    /** How long each of the two takes, which is a moment whatever the machine. */
    private static final int INSTALL_TICKS = 34;
    private static final int CONFIG_TICKS = 50;

    private static final String EFI_FLAG = "--efi-directory=";

    private static final TextKey NOT_YET_THERE = TextKey.of("jsc.install.bootloader_steps.not_yet_there",
            "%s: command not found (it is the new system's: step into it first)");
    private static final TextKey IS_A_PACKAGE = TextKey.of("jsc.install.bootloader_steps.is_a_package",
            "      (the bootloader is a package: %s)");
    private static final TextKey NO_KERNEL = TextKey.of("jsc.install.bootloader_steps.no_kernel",
            "grub-install: warning: no kernel image found in /boot.");
    private static final TextKey NO_KERNEL_HINT = TextKey.of("jsc.install.bootloader_steps.no_kernel_hint",
            "              (build one first: genkernel all, or make in /usr/src/linux)");
    private static final TextKey NOT_EFI = TextKey.of("jsc.install.bootloader_steps.not_efi",
            "grub-install: error: %s doesn't look like an EFI partition.");
    private static final TextKey NOT_EFI_HINT = TextKey.of("jsc.install.bootloader_steps.not_efi_hint",
            "              (mount the EFI partition inside the new system, then name where:");
    private static final TextKey NOT_EFI_WHERE = TextKey.of("jsc.install.bootloader_steps.not_efi_where",
            "               grub-install --efi-directory=/boot or /efi)");
    private static final TextKey NO_DEVICE = TextKey.of("jsc.install.bootloader_steps.no_device",
            "grub-install: error: install device isn't specified.");
    private static final TextKey NOT_A_DISK = TextKey.of("jsc.install.bootloader_steps.not_a_disk",
            "grub-install: error: cannot find a device for /dev/%s.");
    private static final TextKey NOT_A_DISK_HINT = TextKey.of("jsc.install.bootloader_steps.not_a_disk_hint",
            "              (it goes on the disk, not in a partition: grub-install /dev/%s)");
    private static final TextKey NO_GRUB_DIR = TextKey.of("jsc.install.bootloader_steps.no_grub_dir",
            "/usr/bin/grub-mkconfig: /boot/grub: No such file or directory");
    private static final TextKey INSTALL_FIRST = TextKey.of("jsc.install.bootloader_steps.install_first",
            "      (install the bootloader first: grub-install)");
    private static final TextKey MKCONFIG_USAGE = TextKey.of("jsc.install.bootloader_steps.mkconfig_usage",
            "Usage: grub-mkconfig -o /boot/grub/grub.cfg");

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
            return LiveTurn.refused(NOT_YET_THERE.with("grub-install"));
        }
        if (!this.progress.grubPackage) {
            return LiveTurn.refused(
                    (arch ? LiveInstallState.ZSH_NOT_FOUND : LiveInstallState.BASH_NOT_FOUND).with("grub-install"),
                    IS_A_PACKAGE.with(Text.literal(arch ? "pacman -S grub efibootmgr" : "emerge --ask sys-boot/grub")));
        }
        if (!this.progress.kernelBuilt) {
            return LiveTurn.refused(NO_KERNEL.text(), NO_KERNEL_HINT.text());
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
                return LiveTurn.refused(NOT_EFI.with(asked), NOT_EFI_HINT.text(), NOT_EFI_WHERE.text());
            }
            return LiveTurn.running(BootVoices.grubInstall("x86_64-efi", INSTALL_TICKS, installed));
        }
        final String dev = LiveDisks.deviceName(parts[parts.length - 1]);
        if (parts.length < 2 || dev.startsWith("-")) {
            return LiveTurn.refused(NO_DEVICE.text());
        }
        final String disk = LiveDisks.diskOf(this.disks.rootDevice());
        if (!dev.equals(disk)) {
            return LiveTurn.refused(NOT_A_DISK.with(dev), NOT_A_DISK_HINT.with(disk));
        }
        return LiveTurn.running(BootVoices.grubInstall("i386-pc", INSTALL_TICKS, installed));
    }

    /** Writes the bootloader's list of what it can start, from the system that is really installed. */
    LiveTurn config(final String line, final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused(NOT_YET_THERE.with("grub-mkconfig"));
        }
        if (!this.progress.bootloader) {
            return LiveTurn.refused(NO_GRUB_DIR.text(), INSTALL_FIRST.text());
        }
        if (!line.contains("-o")) {
            return LiveTurn.refused(MKCONFIG_USAGE.text());
        }
        final boolean arch = this.distro == LiveInstallState.Distro.ARCH;
        final String kernel = arch ? "/boot/vmlinuz-linux" : "/boot/vmlinuz-" + GentooSteps.KERNEL + "-gentoo-x86_64";
        final String initrd = arch ? "/boot/initramfs-linux.img"
                : "/boot/initramfs-" + GentooSteps.KERNEL + "-gentoo-x86_64.img";
        final String uuid = this.disks.rootUuid(env);
        return LiveTurn.running(BootVoices.grubMkconfig(kernel, initrd, env.uefi(), CONFIG_TICKS, () -> {
            this.progress.grubConfig = true;
            // A configuration file, so data in the machine's own words whatever language the player reads.
            this.files.write(this.files.inNewSystem("/boot/grub/grub.cfg"), Text.literal(String.join("\n",
                    "# generated by grub-mkconfig",
                    "menuentry '" + (arch ? "Arch Linux" : "Gentoo Linux") + "' {",
                    "        set root='hd0'",
                    "        linux " + kernel + " root=UUID=" + uuid + " rw",
                    "        initrd " + initrd,
                    "}")).english());
        }));
    }
}
