/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.install.voice.FetchVoice;
import dev.jstech.computers.program.install.voice.PacmanVoices;
import java.util.ArrayList;
import java.util.List;

/**
 * The steps that are Arch's own: the base system over the network, the table written for you, the package
 * manager inside the new system, and the boot images.
 *
 * <p>Each refuses what a real machine would refuse at that point, in the words the real tool uses, and each
 * that takes time is left running in front of the terminal rather than answered: what it is for is done when
 * it ends.
 */
final class ArchSteps {

    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress;

    /** The kernel release this medium installs, which its boot images are named after. */
    static final String RELEASE = "6.11.5-arch1-1";

    /** How long building both boot images takes on the slowest processor there is, in megahertz-seconds. */
    private static final long IMAGES_WORK = 18_000L;

    ArchSteps(final LiveFiles files, final LiveDisks disks, final LiveProgress progress) {
        this.files = files;
        this.disks = disks;
        this.progress = progress;
    }

    /** Installs the base system into the mounted root, over the network. */
    LiveTurn pacstrap(final String[] parts, final LiveInstallState.Env env) {
        String point = "";
        for (int i = 1; i < parts.length && point.isEmpty(); i++) {
            if (parts[i].startsWith("/")) {
                point = parts[i];
            }
        }
        if (!point.equals(this.files.root()) || !this.disks.mounted()) {
            return LiveTurn.refused("==> ERROR: " + (point.isEmpty() ? this.files.root() : point)
                    + " is not a mountpoint!");
        }
        if (!env.mirror()) {
            return LiveTurn.refused("error: failed retrieving file 'core.db' from " + FetchVoice.MIRROR
                    + " : Could not resolve host: mainframe",
                    "error: failed to synchronize all databases (unexpected error)");
        }
        final LiveInstallState.Device disk = env.find(LiveDisks.diskOf(this.disks.rootDevice()));
        final double size = PacmanVoices.baseMib();
        return LiveTurn.running(PacmanVoices.pacstrap(this.files.root(), LiveTimes.fetch(size, env),
                LiveTimes.write(size * 4, disk == null ? 1 : disk.speed()), RELEASE, this::laidOut));
    }

    /**
     * Writes the table of what to mount at boot.
     *
     * <p>Sent into the file, as the guide has it, it says nothing, because what it had to say went into the
     * file; run without the redirection it prints the table instead and writes nothing, which is how anybody
     * checks what it is about to write.
     */
    LiveTurn genfstab(final String line, final LiveInstallState.Env env) {
        if (!this.progress.base) {
            return LiveTurn.refused("==> ERROR: " + this.files.root() + "/etc does not exist",
                    "    (install the base system first: pacstrap -K " + this.files.root() + " base linux)");
        }
        if (!line.contains(this.files.root())) {
            return LiveTurn.refused("usage: genfstab [options] root",
                    "       genfstab -U " + this.files.root() + " >> " + this.files.root() + "/etc/fstab");
        }
        final List<String> table = new ArrayList<>(List.of("# Static information about the filesystems.",
                "# See fstab(5) for details.", "", "# <file system> <dir> <type> <options> <dump> <pass>",
                "# /dev/" + this.disks.rootDevice(),
                "UUID=" + this.disks.rootUuid(env) + "\t/         \text4      \trw,relatime\t0 1"));
        if (!this.disks.espMount().isEmpty()) {
            table.add("");
            table.add("# /dev/" + this.disks.espDevice());
            table.add("UUID=" + this.disks.espSerial(env) + "      \t"
                    + this.disks.espMount().substring(this.files.root().length()) + "     \tvfat      \t"
                    + "rw,relatime,fmask=0022,dmask=0022,codepage=437,iocharset=ascii,shortname=mixed,utf8,"
                    + "errors=remount-ro\t0 2");
        }
        if (!line.contains(">")) {
            return LiveTurn.said(table.toArray(String[]::new));
        }
        this.files.write(this.files.inNewSystem("/etc/fstab"), String.join("\n", table));
        this.progress.fstab = true;
        return LiveTurn.silent();
    }

    /** Installs named packages inside the new system, where the question is really asked. */
    LiveTurn pacman(final String[] parts, final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("error: you cannot perform this operation unless you are root in the new",
                    "       system (arch-chroot " + this.files.root() + " first)");
        }
        if (parts.length < 3 || !parts[1].startsWith("-S")) {
            return LiveTurn.refused("error: no operation specified (use -h for help)");
        }
        if (!env.mirror()) {
            return LiveTurn.refused("error: failed retrieving file 'core.db' from " + FetchVoice.MIRROR
                    + " : Could not resolve host: mainframe",
                    "error: failed to synchronize all databases (unexpected error)");
        }
        final List<String> named = new ArrayList<>();
        final List<PacmanVoices.Package> packages = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            if (parts[i].startsWith("-")) {
                continue;
            }
            named.add(parts[i]);
            packages.add(switch (parts[i]) {
                case "grub" -> new PacmanVoices.Package("grub-2:2.12-3", 7.8);
                case "efibootmgr" -> new PacmanVoices.Package("efibootmgr-18-3", 0.1);
                default -> new PacmanVoices.Package(parts[i] + "-1.0-1", 1.8);
            });
        }
        if (packages.isEmpty()) {
            return LiveTurn.refused("error: no targets specified (use -h for help)");
        }
        double size = 0;
        for (final PacmanVoices.Package one : packages) {
            size += one.mib();
        }
        return LiveTurn.running(PacmanVoices.install(packages, LiveTimes.fetch(size, env),
                LiveTimes.write(size * 4, 4), () -> {
                    for (final String each : named) {
                        if (each.equals("grub")) {
                            this.progress.grubPackage = true;
                        } else if (!each.equals("efibootmgr")) {
                            this.progress.ask(each);
                        }
                    }
                }));
    }

    /** Builds both boot images again, for whoever changed what goes into them. */
    LiveTurn mkinitcpio(final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("mkinitcpio: command not found (it is the new system's: arch-chroot "
                    + this.files.root() + " first)");
        }
        return LiveTurn.running(PacmanVoices.mkinitcpio(RELEASE, LiveTimes.compile(IMAGES_WORK, 1, env), () -> {
            this.progress.initramfs = true;
            this.files.write(this.files.inNewSystem("/boot/initramfs-linux.img"), "");
            this.files.write(this.files.inNewSystem("/boot/initramfs-linux-fallback.img"), "");
        }));
    }

    /** Generates the locales the file names, in this distribution's plainer words. */
    LiveTurn localeGen(final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("locale-gen: command not found");
        }
        final List<String> locales = new ArrayList<>();
        final String file = this.files.read(this.files.inNewSystem("/etc/locale.gen"));
        for (final String each : (file == null ? "" : file).split("\n")) {
            if (!each.isBlank() && !each.startsWith("#")) {
                locales.add(each.trim().split("\\s+")[0]);
            }
        }
        return LiveTurn.running(PacmanVoices.localeGen(locales, LiveTimes.compile(12_000L, 1, env),
                () -> this.progress.localesGenerated = true));
    }

    /** The directories and files a base system brings with it, and everything installing it built. */
    private void laidOut() {
        this.progress.startOver();
        this.progress.base = true;
        /* The kernel came with it, and the hook that runs when it is installed built the boot images. */
        this.progress.kernelBuilt = true;
        this.progress.initramfs = true;
        for (final String dir : new String[]{"/etc", "/boot", "/root", "/usr", "/var", "/proc", "/sys", "/dev",
            "/run"}) {
            this.files.makeDir(this.files.inNewSystem(dir));
        }
        this.files.write(this.files.inNewSystem("/boot/vmlinuz-linux"), "");
        this.files.write(this.files.inNewSystem("/boot/initramfs-linux.img"), "");
        this.files.write(this.files.inNewSystem("/boot/initramfs-linux-fallback.img"), "");
        this.files.write(this.files.inNewSystem("/etc/locale.gen"), String.join("\n",
                "# Configuration file for locale-gen",
                "#",
                "# lists of locales that are to be generated by the locale-gen command.",
                "#",
                "#en_GB.UTF-8 UTF-8",
                "#en_US.UTF-8 UTF-8",
                "#en_US ISO-8859-1"));
    }
}
