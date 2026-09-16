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
 * The manual installation of a source/live distribution, as a state machine the live shell drives: the
 * player runs the real steps in order (partition, mount, bootstrap the base system, fstab, chroot, kernel,
 * bootloader, password, reboot) and gets the genuine error when a step is out of order. Pure (no
 * Minecraft types) so the whole sequence is unit-tested; the shell supplies the world facts it needs
 * through {@link Env} (the disks present, whether the network mirror answers, the current tick).
 *
 * <p>Arch: {@code lsblk, mkfs.ext4, mount, pacstrap, genfstab, arch-chroot, grub-install, passwd, exit,
 * reboot}. Gentoo: {@code lsblk, mkfs.ext4, mount, tar (stage3), chroot, emerge --sync, emerge
 * gentoo-sources (a real compile wait), genkernel, grub-install, passwd, exit, reboot}.
 */
public final class LiveInstallState {

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
            return serializedName;
        }

        /** The distribution a saved state names, or null for a name none declares. */
        static Distro find(final String name) {
            return NAMES.find(name);
        }
    }

    /** What the shell needs from the world for one command. */
    public record Env(List<String> devices, boolean mirror, long now, long kernelBuildTicks) {
    }

    /** The outcome of one command: its output lines, whether it failed, and whether the install completed. */
    public record Result(boolean ok, List<String> lines, boolean complete) {
        static Result pass(final String... lines) {
            return new Result(true, List.of(lines), false);
        }

        static Result fail(final String... lines) {
            return new Result(false, List.of(lines), false);
        }
    }

    private final Distro distro;
    private String device = "";       // the formatted target, e.g. "sda"
    private boolean formatted;
    private boolean mounted;
    private boolean base;             // pacstrap / stage3 done
    private boolean fstab;
    private boolean chroot;
    private boolean synced;           // gentoo: portage tree synced
    private long kernelReadyAt = -1;  // gentoo: kernel sources compile finishes at this tick
    private boolean kernelBuilt;      // gentoo: genkernel done
    private boolean bootloader;
    private boolean password;

    public LiveInstallState(final Distro distro) {
        this.distro = distro;
    }

    public Distro distro() {
        return distro;
    }

    public boolean inChroot() {
        return chroot;
    }

    /** The index of the target disk among the environment's devices ({@code sda}=0, {@code sdb}=1, ...), or -1. */
    public int targetIndex() {
        if (device.length() < 3 || !device.startsWith("sd")) {
            return -1;
        }
        return device.charAt(2) - 'a';
    }

    /** The live shell prompt for the current state, in the distribution's own style. */
    public String prompt() {
        if (distro == Distro.ARCH) {
            return chroot ? "[root@archiso /]#" : "root@archiso ~ #";
        }
        return chroot ? "(chroot) livecd / #" : "livecd ~ #";
    }

    /** The host name the live medium reports. */
    public String hostname() {
        return distro == Distro.ARCH ? "archiso" : "livecd";
    }

    /** The tick the Gentoo kernel sources finish compiling, or -1 when no compile has started. */
    public long kernelReadyAt() {
        return kernelReadyAt;
    }

    /** Whether the Gentoo kernel is currently compiling (sources emerged, genkernel not yet possible). */
    public boolean kernelCompiling(final long nowTick) {
        return distro == Distro.GENTOO && kernelReadyAt >= 0 && !kernelBuilt && nowTick < kernelReadyAt;
    }

    /** Runs one command line against the state. */
    public Result run(final String line, final Env env) {
        final String[] parts = line.trim().split("\\s+");
        final String cmd = parts.length == 0 ? "" : parts[0].toLowerCase(Locale.ROOT);
        final String arg1 = parts.length > 1 ? parts[1] : "";
        return switch (cmd) {
            case "lsblk" -> lsblk(env);
            case "mkfs.ext4", "mkfs" -> mkfs(cmd.equals("mkfs") ? (parts.length > 2 ? parts[2] : "") : arg1, env);
            case "mount" -> mount(arg1, parts.length > 2 ? parts[2] : "");
            case "pacstrap" -> pacstrap(arg1, env);
            case "tar" -> stage3(line, env);
            case "genfstab" -> genfstab(line);
            case "arch-chroot", "chroot" -> enterChroot(arg1);
            case "emerge-webrsync", "emerge" -> emerge(line, env);
            case "genkernel" -> genkernel(env);
            case "grub-install" -> grub(arg1);
            case "passwd" -> passwd();
            case "exit" -> exit();
            case "reboot" -> reboot();
            case "help" -> help();
            default -> Result.fail(cmd + ": command not found");
        };
    }

    private Result lsblk(final Env env) {
        final List<String> out = new ArrayList<>();
        out.add("NAME   TYPE  MOUNTPOINT");
        if (env.devices().isEmpty()) {
            out.add("(no disks detected)");
        }
        for (int i = 0; i < env.devices().size(); i++) {
            final String name = env.devices().get(i);
            final String mp = mounted && name.equals(device) ? "/mnt" : "";
            out.add(String.format(Locale.ROOT, "%-6s disk  %s", name, mp));
        }
        return new Result(true, out, false);
    }

    private static String deviceName(final String arg) {
        return arg.startsWith("/dev/") ? arg.substring(5) : arg;
    }

    private Result mkfs(final String arg, final Env env) {
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return Result.fail("Usage: mkfs.ext4 /dev/<device>");
        }
        if (!env.devices().contains(dev)) {
            return Result.fail("mke2fs: No such file or directory while trying to determine filesystem size");
        }
        device = dev;
        formatted = true;
        mounted = false;
        base = false;
        return Result.pass("mke2fs 1.47 (JSC)", "Creating filesystem on /dev/" + dev, "Writing superblocks and filesystem accounting information: done");
    }

    private Result mount(final String arg, final String point) {
        final String dev = deviceName(arg);
        if (dev.isEmpty() || point.isEmpty()) {
            return Result.fail("mount: bad usage", "Try 'mount /dev/<device> /mnt'.");
        }
        if (!point.equals("/mnt")) {
            return Result.fail("mount: " + point + ": mount point does not exist.");
        }
        if (!formatted || !dev.equals(device)) {
            return Result.fail("mount: /mnt: wrong fs type, bad option, bad superblock on /dev/" + dev + ".",
                    "       (format it first: mkfs.ext4 /dev/" + dev + ")");
        }
        mounted = true;
        return Result.pass();
    }

    private Result pacstrap(final String point, final Env env) {
        if (distro != Distro.ARCH) {
            return Result.fail("pacstrap: command not found");
        }
        if (!point.equals("/mnt") || !mounted) {
            return Result.fail("==> ERROR: '/mnt' is not a mountpoint!");
        }
        if (!env.mirror()) {
            return Result.fail("error: failed retrieving file 'core.db' from mirror://mainframe : Could not resolve host",
                    "error: failed to synchronize all databases (unexpected error)");
        }
        base = true;
        return Result.pass("==> Creating install root at /mnt", ":: Synchronizing package databases (mirror://mainframe)",
                ":: Installing base linux ... done", "pacstrap: installation complete");
    }

    private Result stage3(final String line, final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("tar: stage3: Cannot open: No such file or directory");
        }
        if (!line.contains("stage3")) {
            return Result.fail("tar: Cowardly refusing to create an empty archive");
        }
        if (!mounted) {
            return Result.fail("tar: /mnt: Cannot open: Not a mountpoint");
        }
        if (!env.mirror()) {
            return Result.fail("tar: stage3-amd64.tar.xz: Cannot open: mirror://mainframe could not be resolved");
        }
        base = true;
        return Result.pass("Unpacking stage3 into /mnt ... done");
    }

    private Result genfstab(final String line) {
        if (distro != Distro.ARCH) {
            return Result.fail("genfstab: command not found");
        }
        if (!base) {
            return Result.fail("genfstab: /mnt/etc does not exist (install the base system first)");
        }
        if (!line.contains("/mnt")) {
            return Result.fail("Usage: genfstab -U /mnt >> /mnt/etc/fstab");
        }
        fstab = true;
        return Result.pass("# /dev/" + device, "UUID=jsc-" + device + "  /  ext4  rw,relatime  0 1");
    }

    private Result enterChroot(final String point) {
        if (!point.equals("/mnt")) {
            return Result.fail("chroot: cannot change root directory to '" + point + "': No such file or directory");
        }
        if (!base) {
            return Result.fail("chroot: failed to run command '/bin/bash': No such file or directory");
        }
        chroot = true;
        return Result.pass();
    }

    private Result emerge(final String line, final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("emerge: command not found");
        }
        if (!chroot) {
            return Result.fail("emerge: this must be run inside the new system (chroot /mnt)");
        }
        if (!env.mirror()) {
            return Result.fail("!!! Could not resolve mirror://mainframe", "!!! Synchronization failed");
        }
        if (line.startsWith("emerge-webrsync") || line.contains("--sync")) {
            synced = true;
            return Result.pass(">>> Synchronizing the portage tree from mirror://mainframe ... done");
        }
        if (!synced) {
            return Result.fail("!!! The portage tree is empty. Run emerge-webrsync or emerge --sync first.");
        }
        if (line.contains("gentoo-sources")) {
            kernelReadyAt = env.now() + env.kernelBuildTicks();
            return Result.pass(">>> Emerging (1 of 1) sys-kernel/gentoo-sources", ">>> Compiling ... (about "
                    + (env.kernelBuildTicks() / 20) + "s; run genkernel when it finishes)");
        }
        return Result.pass(">>> Emerging " + line.substring("emerge".length()).trim() + " ... done");
    }

    private Result genkernel(final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("genkernel: command not found");
        }
        if (!chroot) {
            return Result.fail("genkernel: this must be run inside the new system (chroot /mnt)");
        }
        if (kernelReadyAt < 0) {
            return Result.fail("* ERROR: no kernel sources found. emerge sys-kernel/gentoo-sources first.");
        }
        if (env.now() < kernelReadyAt) {
            return Result.fail("* kernel sources are still compiling (" + ((kernelReadyAt - env.now()) / 20) + "s left)");
        }
        kernelBuilt = true;
        return Result.pass("* Gentoo Linux Genkernel", "* kernel: >> Compiling 6.8-jsc bzImage ... done", "* Kernel compiled successfully!");
    }

    private Result grub(final String arg) {
        if (!chroot) {
            return Result.fail("grub-install: error: cannot find EFI directory (run this inside the new system).");
        }
        final String dev = deviceName(arg);
        if (!dev.equals(device)) {
            return Result.fail("grub-install: error: cannot find a device for /dev/" + (dev.isEmpty() ? "?" : dev) + ".");
        }
        if (distro == Distro.GENTOO && !kernelBuilt) {
            return Result.fail("grub-install: error: no kernel image found in /boot (run genkernel first).");
        }
        bootloader = true;
        return Result.pass("Installing for i386-pc platform.", "Installation finished. No error reported.");
    }

    private Result passwd() {
        if (!chroot) {
            return Result.fail("passwd: you are changing the live medium's password, not the new system's (chroot /mnt first)");
        }
        password = true;
        return Result.pass("passwd: password updated successfully");
    }

    private Result exit() {
        if (!chroot) {
            return Result.pass("logout");
        }
        chroot = false;
        return Result.pass();
    }

    private Result reboot() {
        if (chroot) {
            return Result.fail("reboot: you are inside the chroot. exit first.");
        }
        final List<String> missing = new ArrayList<>();
        if (!base) {
            missing.add("no base system installed");
        }
        if (distro == Distro.ARCH && !fstab) {
            missing.add("no fstab (genfstab)");
        }
        if (distro == Distro.GENTOO && !kernelBuilt) {
            missing.add("no kernel (genkernel)");
        }
        if (!bootloader) {
            missing.add("no bootloader (grub-install)");
        }
        if (!password) {
            missing.add("no root password (passwd)");
        }
        if (!missing.isEmpty()) {
            final List<String> out = new ArrayList<>();
            out.add("The new system will not boot:");
            for (final String m : missing) {
                out.add("  - " + m);
            }
            return new Result(false, out, false);
        }
        return new Result(true, List.of("Rebooting into the new system ..."), true);
    }

    private Result help() {
        return distro == Distro.ARCH
                ? Result.pass("lsblk | mkfs.ext4 /dev/sdX | mount /dev/sdX /mnt | pacstrap /mnt base linux",
                        "genfstab -U /mnt >> /mnt/etc/fstab | arch-chroot /mnt | grub-install /dev/sdX | passwd | exit | reboot")
                : Result.pass("lsblk | mkfs.ext4 /dev/sdX | mount /dev/sdX /mnt | tar xpf stage3-amd64.tar.xz -C /mnt",
                        "chroot /mnt | emerge --sync | emerge sys-kernel/gentoo-sources | genkernel all",
                        "grub-install /dev/sdX | passwd | exit | reboot");
    }

    /*
     * Persistence: one line per thing remembered, written as name and value, so the console state stays free of
     * NBT here and this class stays pure.
     *
     * <p>Named rather than counted on purpose. A sequence this long grows, and a state written as a row of
     * unlabelled fields would mean every new thing remembered shifts everything after it and throws away what
     * was saved before. By name, a state written by an older build simply says less, and what it does not say
     * takes the value a fresh install would have.
     */

    /** Separates one remembered thing from the next; a value carrying one is escaped rather than cut. */
    private static final char LINE = '\n';

    public String serialize() {
        final StringBuilder out = new StringBuilder();
        put(out, "distro", distro.serializedName());
        put(out, "device", device);
        put(out, "formatted", formatted);
        put(out, "mounted", mounted);
        put(out, "base", base);
        put(out, "fstab", fstab);
        put(out, "chroot", chroot);
        put(out, "synced", synced);
        put(out, "kernel_ready_at", Long.toString(kernelReadyAt));
        put(out, "kernel_built", kernelBuilt);
        put(out, "bootloader", bootloader);
        put(out, "password", password);
        return out.toString();
    }

    public static LiveInstallState deserialize(final String s) {
        final java.util.Map<String, String> saved = read(s);
        final Distro distro = Distro.find(saved.getOrDefault("distro", ""));
        if (distro == null) {
            return null;
        }
        final LiveInstallState st = new LiveInstallState(distro);
        st.device = saved.getOrDefault("device", "");
        st.formatted = flag(saved, "formatted");
        st.mounted = flag(saved, "mounted");
        st.base = flag(saved, "base");
        st.fstab = flag(saved, "fstab");
        st.chroot = flag(saved, "chroot");
        st.synced = flag(saved, "synced");
        st.kernelReadyAt = number(saved, "kernel_ready_at", -1L);
        st.kernelBuilt = flag(saved, "kernel_built");
        st.bootloader = flag(saved, "bootloader");
        st.password = flag(saved, "password");
        return st;
    }

    private static void put(final StringBuilder out, final String name, final boolean value) {
        put(out, name, value ? "1" : "0");
    }

    private static void put(final StringBuilder out, final String name, final String value) {
        if (!out.isEmpty()) {
            out.append(LINE);
        }
        out.append(name).append('=').append(escape(value));
    }

    /** What a saved state says, by name; anything it does not name is simply absent. */
    private static java.util.Map<String, String> read(final String s) {
        final java.util.Map<String, String> saved = new java.util.LinkedHashMap<>();
        if (s == null || s.isEmpty()) {
            return saved;
        }
        for (final String line : s.split("\n", -1)) {
            final int split = line.indexOf('=');
            if (split > 0) {
                saved.put(line.substring(0, split), unescape(line.substring(split + 1)));
            }
        }
        return saved;
    }

    private static boolean flag(final java.util.Map<String, String> saved, final String name) {
        return "1".equals(saved.get(name));
    }

    private static long number(final java.util.Map<String, String> saved, final String name, final long fallback) {
        try {
            return Long.parseLong(saved.getOrDefault(name, Long.toString(fallback)));
        } catch (final NumberFormatException wrong) {
            return fallback;
        }
    }

    /** A value with the two characters that would break a line written so they cannot. */
    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String unescape(final String value) {
        final StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            final char next = value.charAt(++i);
            out.append(next == 'n' ? '\n' : next);
        }
        return out.toString();
    }
}
