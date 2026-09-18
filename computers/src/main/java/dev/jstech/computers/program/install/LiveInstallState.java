/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An installation done by hand from a live medium, as the shell of that medium drives it.
 *
 * <p>The player runs the real steps in the real order and is refused, in the real tool's words, whatever a real
 * machine would refuse at that point. The steps that take time are left running in front of the terminal and
 * do what they are for when they end, so what is recorded here is always what is really on the disk.
 *
 * <p>This is where a line is sent to whatever answers it. The files the session touches, the disks, how far
 * the installation has got, and each distribution's own steps are things of their own beside it. Pure, no
 * Minecraft types, so every sequence is tested without the game; what it needs of the world comes in through
 * {@link Env}.
 */
public final class LiveInstallState {

    private final Distro distro;
    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress = new LiveProgress();
    private final GentooSteps gentoo;
    private final ArchSteps arch;
    private final BootloaderSteps bootloader;
    private final SettingsSteps settings;

    /**
     * Every verb {@link #run} answers to, which is also every verb the shell of a live medium accepts.
     *
     * <p>It lives here, beside the switch that answers them, because a second list in the shell drifted from
     * this one once, and a verb answered here and refused there is a verb nobody can type.
     */
    public static final List<String> VERBS = List.of(
            "ls", "cat", "less", "more", "cd", "echo", "nano", "mkdir", "lsblk", "blkid", "fdisk", "mkfs.ext4", "mkfs",
            "mkfs.fat", "mkfs.vfat", "mount", "umount", "pacstrap", "pacman", "wget", "tar", "genfstab",
            "arch-chroot", "chroot", "source", "export", "env-update", "emerge-webrsync", "emerge", "eselect",
            "genkernel", "make", "locale-gen", "ln", "hwclock", "hostname", "mkinitcpio", "grub-install",
            "grub-mkconfig", "passwd", "exit", "reboot", "help");

    /**
     * What stands in front of the name of a file of the session wherever a file of the machine is named.
     *
     * <p>An editor asks the machine for a file by name, and these are not on any disk: they are the medium's
     * and the half-built system's, and they go when the session does. The mark is what sends the asking here.
     */
    public static final String FILE_SCHEME = "live:";

    public LiveInstallState(final Distro distro) {
        this.distro = distro;
        this.files = new LiveFiles(distro == Distro.GENTOO ? "/mnt/gentoo" : "/mnt", LiveGuide.of(distro));
        this.disks = new LiveDisks(this.files.root());
        this.gentoo = new GentooSteps(this.files, this.disks, this.progress);
        this.arch = new ArchSteps(this.files, this.disks, this.progress);
        this.bootloader = new BootloaderSteps(distro, this.files, this.disks, this.progress);
        this.settings = new SettingsSteps(distro, this.files, this.progress);
    }

    /** The two distributions that are installed by hand. */
    public enum Distro implements IStableName {
        ARCH("arch"),
        GENTOO("gentoo");

        private static final StableNames<Distro> NAMES = StableNames.of(Distro.class);

        private final String serializedName;

        Distro(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The distribution a saved state names, or null for a name none declares. */
        static Distro find(final String name) {
            return NAMES.find(name);
        }
    }

    /**
     * A disk the machine really has, as the live shell sees it.
     *
     * @param name   the device name the shell gives it, {@code sda} for the first
     * @param sizeMb how big it is, so the tools print the disk the player actually put in
     * @param speed  how many times faster than a mechanical disk it is, which is how long writing to it takes
     */
    public record Device(String name, int sizeMb, int speed) {

        /** A mechanical disk of that size. */
        public Device(final String name, final int sizeMb) {
            this(name, sizeMb, 1);
        }
    }

    /**
     * What the shell needs from the world for one command.
     *
     * @param cores     how many the processor has, which caps how much of a compile can happen at once
     * @param mhz       how fast it runs, which is the other half of how long a compile takes
     * @param eraFactor how much faster than the earliest machines this one's connection is
     * @param uefi      whether this machine's firmware boots the modern way, which decides whether the disk
     *                  needs a partition of its own for the bootloader and which target it is installed for
     * @param dayTime   the world's count of ticks across all its days, which is what a tool stamps a date from
     * @param everyStep whether this world asks for every step of the handbook rather than only the ones a
     *                  system cannot boot without
     */
    public record Env(List<Device> devices, boolean mirror, long now, int cores, int mhz, int eraFactor,
                      boolean uefi, long dayTime, boolean everyStep) {

        /** Whether a device of that name is in the machine. */
        public boolean has(final String name) {
            return this.find(name) != null;
        }

        /** The device of that name, or null when the machine has none. */
        public Device find(final String name) {
            for (final Device device : this.devices) {
                if (device.name().equals(name)) {
                    return device;
                }
            }
            return null;
        }
    }

    public Distro distro() {
        return this.distro;
    }

    public boolean inChroot() {
        return this.files.inside();
    }

    /** The index of the target disk among the machine's ({@code sda} is 0), or -1 when none is chosen. */
    public int targetIndex() {
        return this.disks.targetIndex();
    }

    /**
     * The live shell's prompt, in the distribution's own style.
     *
     * <p>It names where the session is standing, as those prompts do, with the root user's home written as a
     * tilde. Inside the new system it is that system's own root that is shown, not the mount point it hangs
     * off outside, because from in there that is where you are.
     */
    public String prompt() {
        final String where = this.files.cwdForPrompt();
        if (this.distro == Distro.ARCH) {
            return this.files.inside() ? "[root@archiso " + where + "]#" : "root@archiso " + where + " #";
        }
        return this.files.inside() ? "(chroot) livecd " + where + " #" : "livecd " + where + " #";
    }

    /**
     * The name the player gave the machine while installing it, empty when they gave none.
     *
     * <p>Given with the tool for it or written into the file that holds it, which is the same thing done two
     * ways and both are how people do it.
     */
    public String chosenName() {
        return this.settings.chosenName();
    }

    /** The packages the player asked for inside the new system, in the order they asked. */
    public List<String> askedFor() {
        return this.progress.askedFor();
    }

    /** The filesystem table the installation wrote, empty when it never wrote one. */
    public String filesystemTable() {
        final String table = this.files.read(this.files.inNewSystem("/etc/fstab"));
        return table == null ? "" : table;
    }

    /** The host name the live medium reports. */
    public String hostname() {
        return this.settings.mediumName();
    }

    /** How many jobs the build options ask for, which is one until somebody writes otherwise. */
    public int makeJobs() {
        return this.gentoo.makeJobs();
    }

    /**
     * The build options the installation wrote, empty for a distribution that has none or a file never written.
     *
     * <p>They belong to the system and not to the install: every package it builds from then on is built by them.
     */
    public String buildOptions() {
        final String written = this.distro == Distro.GENTOO ? this.gentoo.buildOptions() : null;
        return written == null ? "" : written;
    }

    /** How long compiling the kernel takes on this machine, with what the build options ask of it. */
    public long compileTicks(final Env env) {
        return LiveTimes.compile(LiveTimes.KERNEL_WORK, LiveTimes.jobs(this.makeJobs(), env), env);
    }

    /**
     * The file an editor started with those words would open, as it was typed, or null when they name nothing
     * an editor can open: no file at all, or a directory.
     *
     * <p>The words are everything after the editor's name, so its options are among them and are passed over.
     */
    public String editable(final List<String> words) {
        String file = "";
        for (final String word : words) {
            if (!word.startsWith("-") && !word.startsWith("+")) {
                file = word;
            }
        }
        return file.isEmpty() || this.files.isDir(this.files.resolve(file)) ? null : file;
    }

    /** A file of the session as somebody standing in it names it, for an editor; null when there is none. */
    public String fileAt(final String typed) {
        return this.files.read(this.files.resolve(typed));
    }

    /** Writes a file of the session, which is what an editor closed on it does. */
    public void writeFileAt(final String typed, final String content) {
        this.files.write(this.files.resolve(typed), content);
    }

    /** Runs one command line against the installation. */
    public LiveTurn run(final String line, final Env env) {
        final String[] parts = line.trim().split("\\s+");
        final String verb = parts.length == 0 ? "" : parts[0].toLowerCase(Locale.ROOT);
        final String first = parts.length > 1 ? parts[1] : "";
        return switch (verb) {
            case "ls" -> this.files.ls(first);
            case "cat" -> this.files.cat(first, false);
            case "less", "more" -> this.files.cat(first, true);
            case "cd" -> this.files.cd(first);
            case "echo" -> this.files.echo(line.trim());
            case "nano" -> this.nano(parts);
            case "mkdir" -> this.mkdir(parts);
            case "lsblk" -> this.disks.lsblk(env, this.distro == Distro.ARCH ? 1_126 : 749,
                    this.distro == Distro.ARCH ? "/run/archiso/airootfs" : "/run/initramfs/live");
            case "blkid" -> this.disks.blkid(env);
            case "fdisk" -> this.fdisk(first, env);
            case "mkfs.fat", "mkfs.vfat" -> this.disks.mkfsFat(parts, env);
            case "mkfs.ext4" -> this.disks.mkfsExt4(first, env);
            case "mkfs" -> this.disks.mkfsExt4(parts.length > 2 ? parts[parts.length - 1] : "", env);
            case "mount" -> this.disks.mount(parts, this.files);
            case "umount", "source", "export", "env-update" -> LiveTurn.silent();
            case "ln" -> this.settings.linkZone(line);
            case "hwclock" -> this.settings.setClock();
            case "arch-chroot", "chroot" -> this.enter(verb, first);
            case "hostname" -> this.settings.hostname(parts);
            case "passwd" -> this.settings.passwd();
            case "grub-install" -> this.bootloader.install(parts, env);
            case "grub-mkconfig" -> this.bootloader.config(line, env);
            case "exit" -> this.exit();
            case "reboot" -> this.reboot(env);
            case "help" -> LiveTurn.said(LiveGuide.help(this.distro));
            default -> this.distro == Distro.GENTOO ? this.gentooVerb(verb, parts, line, env)
                    : this.archVerb(verb, parts, line, env);
        };
    }

    public String serialize() {
        final LiveSaved out = new LiveSaved();
        out.put("distro", this.distro.serializedName());
        this.files.save(out);
        this.disks.save(out);
        this.progress.save(out);
        return out.write();
    }

    public static LiveInstallState deserialize(final String written) {
        final LiveSaved saved = LiveSaved.read(written);
        final Distro distro = Distro.find(saved.text("distro", ""));
        if (distro == null) {
            return null;
        }
        final LiveInstallState state = new LiveInstallState(distro);
        state.files.load(saved);
        state.disks.load(saved);
        state.progress.load(saved);
        return state;
    }

    private LiveTurn gentooVerb(final String verb, final String[] parts, final String line, final Env env) {
        return switch (verb) {
            case "wget" -> this.gentoo.wget(parts, env);
            case "tar" -> this.gentoo.tar(parts, line, env);
            case "emerge", "emerge-webrsync" -> this.gentoo.emerge(parts, line, env);
            case "eselect" -> this.gentoo.eselect(parts);
            case "genkernel" -> this.gentoo.genkernel(env);
            case "make" -> this.gentoo.make(line.trim(), env);
            case "locale-gen" -> this.gentoo.localeGen(env);
            default -> LiveTurn.refused("bash: " + verb + ": command not found");
        };
    }

    private LiveTurn archVerb(final String verb, final String[] parts, final String line, final Env env) {
        return switch (verb) {
            case "pacstrap" -> this.arch.pacstrap(parts, env);
            case "genfstab" -> this.arch.genfstab(line, env);
            case "pacman" -> this.arch.pacman(parts, env);
            case "mkinitcpio" -> this.arch.mkinitcpio(env);
            case "locale-gen" -> this.arch.localeGen(env);
            default -> LiveTurn.refused("zsh: command not found: " + verb);
        };
    }

    /** Opens the partition editor on a disk, which takes the terminal over until it is written or left. */
    private LiveTurn fdisk(final String arg, final Env env) {
        final String dev = LiveDisks.deviceName(arg);
        if (dev.isEmpty()) {
            return LiveTurn.refused("fdisk: bad usage", "Try 'fdisk --help' for more information.");
        }
        final Device disk = env.find(dev);
        if (disk == null) {
            return LiveTurn.refused("fdisk: cannot open /dev/" + dev + ": No such file or directory");
        }
        return LiveTurn.running(new FdiskProcess(this.disks, dev, disk.sizeMb()));
    }

    /**
     * The editor, which says nothing when it opens: the shell gives it the terminal, and whatever it has to
     * say from then on it says on its own glass. What is answered here is only why it would not open.
     */
    private LiveTurn nano(final String[] parts) {
        final List<String> words = List.of(parts).subList(1, parts.length);
        if (this.editable(words) != null) {
            return LiveTurn.silent();
        }
        for (final String word : words) {
            if (!word.startsWith("-") && !word.startsWith("+")) {
                return LiveTurn.refused("nano: " + word + " is a directory");
            }
        }
        return LiveTurn.refused("Usage: nano [OPTIONS] [[+LINE[,COLUMN]] FILE]...");
    }

    private LiveTurn mkdir(final String[] parts) {
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("-")) {
                this.files.makeDir(this.files.resolve(parts[i]));
            }
        }
        return LiveTurn.silent();
    }

    /** Steps into the new system, by whichever of the two tools this distribution uses for it. */
    private LiveTurn enter(final String verb, final String point) {
        if (verb.equals("arch-chroot") && this.distro != Distro.ARCH) {
            return LiveTurn.refused("bash: arch-chroot: command not found");
        }
        if (!point.equals(this.files.root())) {
            return LiveTurn.refused("chroot: cannot change root directory to '" + point
                    + "': No such file or directory");
        }
        if (!this.progress.base) {
            return LiveTurn.refused("chroot: failed to run command '/bin/bash': No such file or directory");
        }
        this.files.enter();
        return LiveTurn.silent();
    }

    private LiveTurn exit() {
        if (!this.files.inside()) {
            return LiveTurn.said("logout");
        }
        this.files.leave();
        return LiveTurn.said("exit");
    }

    /** Restarts into the new system, or says everything that would stop it coming up. */
    private LiveTurn reboot(final Env env) {
        if (this.files.inside()) {
            return LiveTurn.refused("reboot: you are inside the new system. exit first.");
        }
        final List<String> missing = new ArrayList<>(LiveChecklist.missing(this.distro, this.progress,
                this.distro == Distro.GENTOO && this.gentoo.fstabNamesARoot(), this.disks, this.chosenName(),
                env.everyStep()));
        if (!missing.isEmpty()) {
            final List<String> out = new ArrayList<>();
            out.add("The new system will not boot:");
            for (final String each : missing) {
                out.add("  - " + each);
            }
            return LiveTurn.refused(out.toArray(String[]::new));
        }
        return LiveTurn.finished("Rebooting into the new system ...");
    }
}
