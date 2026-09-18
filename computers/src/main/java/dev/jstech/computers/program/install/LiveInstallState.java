/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * A disk the machine really has, as the live shell sees it.
     *
     * @param name   the device name the shell gives it, {@code sda} for the first
     * @param sizeMb how big it is, so the tools print the disk the player actually put in
     */
    public record Device(String name, int sizeMb) {
    }

    /**
     * What the shell needs from the world for one command.
     *
     * @param cores     how many the processor has, which caps how much of a compile can happen at once
     * @param mhz       how fast it runs, which is the other half of how long a compile takes
     * @param eraFactor how much faster than the earliest machines this one unpacks and writes
     * @param uefi      whether this machine's firmware boots the modern way, which decides whether the disk
     *                  needs a partition of its own for the bootloader and which target it is installed for
     */
    public record Env(List<Device> devices, boolean mirror, long now, int cores, int mhz, int eraFactor,
                      boolean uefi) {

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

    /**
     * A partition written onto a disk.
     *
     * @param number where it sits on the disk, so {@code sda1} is number one
     * @param sizeMb how big it was asked to be, or 0 for one that took whatever was left
     * @param esp    whether it is the partition the firmware looks in for a bootloader
     */
    public record Partition(int number, int sizeMb, boolean esp) {

        /** The device name of this partition on that disk. */
        public String on(final String disk) {
            return disk + this.number;
        }

        /** What a partition table calls it. */
        public String type() {
            return this.esp ? "EFI System" : "Linux filesystem";
        }
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

    /**
     * Every verb {@link #run} answers to, which is also every verb the shell of a live medium accepts.
     *
     * <p>It lives here, beside the switch that answers them, because it used to live in the shell as a second
     * list and the two drifted: {@code echo} and {@code pacman} were answered here and refused there, so the
     * line that sets the build options and the one that installs a package afterwards could not be typed at
     * all. One list cannot disagree with itself.
     */
    public static final List<String> VERBS = List.of(
            "ls", "cat", "less", "more", "cd", "echo",
            "lsblk", "fdisk", "mkfs.ext4", "mkfs", "mkfs.fat", "mkfs.vfat", "mount",
            "pacstrap", "pacman", "wget", "curl", "tar", "genfstab", "arch-chroot", "chroot",
            "emerge-webrsync", "emerge", "genkernel", "hostname", "mkinitcpio",
            "grub-install", "grub-mkconfig", "passwd", "exit", "reboot", "help");

    /** Where a live session starts, and what its prompt writes as a tilde. */
    private static final String HOME = "/root";

    /** Where the disk being installed onto hangs while the session is outside it. */
    private static final String MOUNT = "/mnt";

    /** Where the build options of the source distribution live, inside the system being built. */
    private static final String PORTAGE_CONF = "/etc/portage/make.conf";

    /**
     * The host every fetch in this sequence names.
     *
     * <p>The network in this world is the one the player built, so what a tool resolves and connects to is
     * the machine serving it rather than an address on the real internet.
     */
    private static final String MIRROR = "distfiles.mainframe";

    /** How many files the package tree holds, which the sync reports and nothing else depends on. */
    private static final int TREE_FILES = 214_483;

    /** The kernel release this distribution's medium installs, which its boot images are named after. */
    private static final String ARCH_RELEASE = "6.11.5-arch1-1";

    /** The driver number every disk of this kind answers to, which a device listing prints first. */
    private static final int SCSI_DISK = 8;

    /** How many device numbers one disk takes up, which is what leaves room for its partitions. */
    private static final int MINORS_PER_DISK = 16;

    /**
     * How much work compiling a kernel is, in megahertz-seconds.
     *
     * <p>A balancing number like every other here: it is the figure that put a single-job build at about half a
     * minute on the machine the sequence was written against, so a faster processor or more of it at once cuts
     * it from there.
     */
    private static final long COMPILE_WORK = 64_000L;

    /** What one extra package weighs, a balancing figure like the rest of them. */
    private static final int PACKAGE_MB = 64;

    /**
     * What a base install fetches.
     *
     * <p>Not the whole system: {@code pacstrap base linux} and a stage 3 bring a system that boots and nothing
     * more, and everything else arrives afterwards, a package at a time, because the player asked for it. The
     * figure is the usual kind of estimate.
     */
    private static final int BASE_MB = 256;

    private final Distro distro;
    /*
     * The files the installation touches, by their whole path, and the directories they sit in. Not a
     * filesystem: the few files a person really reads or writes while installing by hand, so that what the
     * steps wrote is what reading them shows. A guide the medium carries is put here when the session starts
     * and is never saved, since it belongs to the medium and not to the install.
     */
    private final Map<String, String> files = new LinkedHashMap<>();
    private final Set<String> dirs = new LinkedHashSet<>();
    /*
     * The partitions written onto each disk, by the disk's name. A disk with none is a disk nobody has
     * partitioned, which a machine of the older firmware can still be installed onto whole.
     */
    private final Map<String, List<Partition>> tables = new LinkedHashMap<>();
    private String cwd = HOME;
    /** The disk the partition editor is open on, empty when it is not open. */
    private String editing = "";
    /*
     * What the editor has been told so far, which is not the disk yet. The real one keeps every change in
     * memory until it is told to write, and says so when it opens; walking away with q has to really throw
     * the changes away or that promise is a lie.
     */
    private final List<Partition> draft = new ArrayList<>();
    /** Whether the disk being edited has been given a table that can carry a boot partition. */
    private boolean gpt;
    /** The partition mounted where the firmware looks for a bootloader, empty when none is. */
    private String espMount = "";
    /** The partition made ready to hold a bootloader, empty when none has been. */
    private String espDevice = "";
    private boolean espFormatted;
    private String device = "";       // the formatted target, e.g. "sda" or "sda2"
    private boolean formatted;
    private boolean mounted;
    private boolean fetched;          // gentoo: the stage 3 archive is on the disk, ready to unpack
    private boolean base;             // pacstrap / stage3 done
    private boolean fstab;
    private boolean chroot;
    private boolean synced;           // gentoo: portage tree synced
    /*
     * Work that takes time. A step that is not instant says how long it will be and leaves the tick it ends
     * at here; the steps that need it wait, and whoever is watching the console is shown how far along it is.
     * One piece of work at a time, which is all a shell doing one thing at a time can have.
     */
    private long busyUntil = -1;
    private long busyTotal;
    private String busyWhat = "";
    private String busyDone = "";
    private boolean sources;          // gentoo: the kernel sources are emerged
    private boolean kernelBuilt;      // gentoo: genkernel done
    private boolean bootloader;
    /** Whether the bootloader has been given its list of what to start. */
    private boolean grubConfig;
    /** Whether the image the kernel is handed at boot has been built, which only one of the two needs. */
    private boolean initramfs;
    /** The name the player gave the machine, empty while they have not. */
    private String chosenName = "";
    /*
     * The packages asked for inside the new system. Kept because they were asked for and waited on, so they
     * belong to the system that comes out of this rather than to the session that built it.
     */
    private final Set<String> asked = new LinkedHashSet<>();
    private boolean password;

    public LiveInstallState(final Distro distro) {
        this.distro = distro;
        this.dirs.add("/");
        this.dirs.add(HOME);
        this.dirs.add("/mnt");
        this.files.put(HOME + "/install.txt", guide(distro));
    }

    /**
     * The guide the live medium carries, which is the one thing on it that tells a player what to do.
     *
     * <p>The real medium of one of these ships exactly this: a plain file in the root user's home naming the
     * steps in order. It is written from the steps this sequence really accepts, so following it works.
     */
    private static String guide(final Distro distro) {
        if (distro == Distro.ARCH) {
            return String.join("\n",
                    "Installing Arch Linux by hand.",
                    "",
                    "  1. lsblk                                  see the disks",
                    "  2. mkfs.ext4 /dev/sdX                      make a filesystem",
                    "  3. mount /dev/sdX /mnt                     mount it",
                    "  4. pacstrap /mnt base linux                the base system, over the network Mirror",
                    "  5. genfstab -U /mnt >> /mnt/etc/fstab      write the filesystem table",
                    "  6. arch-chroot /mnt                        enter the new system",
                    "  7. hostname <name>                         name the machine",
                    "  8. mkinitcpio -P                           the image the kernel is handed at boot",
                    "  9. grub-install /dev/sdX                   the bootloader",
                    " 10. grub-mkconfig -o /boot/grub/grub.cfg    what it should start",
                    " 11. passwd                                  a root password",
                    " 12. exit, then reboot",
                    "",
                    "A machine that boots the modern way needs a partition for the bootloader: fdisk, then",
                    "t <n> uefi, mkfs.fat -F32 /dev/sdXn, and mount it at /mnt/boot.",
                    "",
                    "Read a file with cat or less. A Mainframe on this network must run the Mirror.");
        }
        return String.join("\n",
                "Installing Gentoo by hand.",
                "",
                "  1. lsblk                                  see the disks",
                "  2. mkfs.ext4 /dev/sdX                      make a filesystem",
                "  3. mount /dev/sdX /mnt                     mount it",
                "  4. wget " + GentooInstallOutput.STAGE3 + "      fetch the stage 3",
                "  5. tar xpvf " + GentooInstallOutput.STAGE3 + "   unpack it over /mnt",
                "  6. chroot /mnt                             enter the new system",
                "  7. emerge --sync                           fetch the portage tree",
                "  8. emerge sys-kernel/gentoo-sources        the kernel sources",
                "  9. genkernel all                           build the kernel: a real compile, a real wait",
                " 10. hostname <name>                         name the machine",
                " 11. grub-install /dev/sdX                   the bootloader",
                " 12. grub-mkconfig -o /boot/grub/grub.cfg    what it should start",
                " 13. passwd, then exit, then reboot",
                "",
                "A machine that boots the modern way needs a partition for the bootloader: fdisk, then",
                "t <n> uefi, mkfs.fat -F32 /dev/sdXn, and mount it at /mnt/boot.",
                "",
                "Read a file with cat or less. A Mainframe on this network must run the Mirror.");
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

    /**
     * The live shell prompt for the current state, in the distribution's own style.
     *
     * <p>It names where the session is standing, as those prompts do, with the root user's home written as a
     * tilde. Inside the new system it is that system's own root that is shown, not the mount point it hangs off
     * outside, because from in there that is where you are.
     */
    public String prompt() {
        if (this.editingTable()) {
            // The editor takes the shell over while it is open, and says so the way it always has.
            return "Command (m for help):";
        }
        final String where = HOME.equals(cwd) ? "~" : cwd;
        if (distro == Distro.ARCH) {
            return chroot ? "[root@archiso " + where + "]#" : "root@archiso " + where + " #";
        }
        return chroot ? "(chroot) livecd " + where + " #" : "livecd " + where + " #";
    }

    /** The name the player gave the machine while installing it, empty when they gave none. */
    public String chosenName() {
        return chosenName;
    }

    /** The packages the player asked for inside the new system, in the order they asked. */
    public List<String> askedFor() {
        return List.copyOf(asked);
    }

    /** The filesystem table the installation wrote, empty when it never wrote one. */
    public String filesystemTable() {
        final String table = files.get(MOUNT + "/etc/fstab");
        return table == null ? "" : table;
    }

    /** The host name the live medium reports. */
    public String hostname() {
        return distro == Distro.ARCH ? "archiso" : "livecd";
    }

    /** Whether a step that takes time is still running. */
    public boolean busy(final long nowTick) {
        return busyUntil >= 0 && nowTick < busyUntil;
    }

    /** The tick the work under way finishes at, or -1 when none is. */
    public long busyUntil() {
        return busyUntil;
    }

    /** How long the whole of it takes, so a console watching it can say how far along it is. */
    public long busyTotal() {
        return busyTotal;
    }

    /** What the work under way is, by the name the tool doing it would print. */
    public String busyWhat() {
        return busyWhat;
    }

    /** The line to say when it finishes, which is the tool's own way of handing over to the next step. */
    public String busyDone() {
        return busyDone;
    }

    /**
     * How long compiling the kernel takes on this machine, with what the player asked of it.
     *
     * <p>Two things decide it and the player owns one of them. The processor is the machine's: cores times
     * clock is how much work it can get through. How much of it happens at once is written in the build
     * options, and a number larger than the machine has cores buys nothing, because the cores are the ceiling.
     * Left alone, it builds one thing at a time, which is what it really does when nobody has said otherwise.
     */
    public long compileTicks(final Env env) {
        final int jobs = Math.max(1, Math.min(makeJobs(), Math.max(1, env.cores())));
        final long seconds = Math.max(5L, Math.min(1800L,
                COMPILE_WORK / (long) Math.max(100, env.mhz()) / jobs));
        return seconds * 20L;
    }

    /** How many jobs the build options ask for, which is one until somebody writes otherwise. */
    public int makeJobs() {
        final String conf = files.get(MOUNT + PORTAGE_CONF);
        if (conf == null) {
            return 1;
        }
        final Matcher found =
                Pattern.compile("MAKEOPTS\\s*=\\s*\"?[^\"\\n]*-j\\s*(\\d+)").matcher(conf);
        return found.find() ? Math.max(1, Integer.parseInt(found.group(1))) : 1;
    }

    /**
     * How many megabytes a second this machine's connection manages, which the fetch lines report.
     *
     * <p>Worked back out of how long the fetch is going to take rather than stated on its own, so the rate
     * a tool prints and the time the player actually waits are the same number seen from two sides.
     */
    private static double megabytesPerSecond(final int sizeMb, final long ticks) {
        return sizeMb / Math.max(0.05, ticks / 20.0);
    }

    /**
     * Where a tool's output stops being what it says and starts being what it does.
     *
     * <p>Everything up to and including that line is printed at once, because the real tool prints it at
     * once; everything after it arrives while the work does.
     */
    private static int after(final List<String> lines, final String marker) {
        final int at = lines.indexOf(marker);
        return at < 0 ? lines.size() : at + 1;
    }

    /** How long fetching that many megabytes over the network takes this machine. */
    private static long fetchTicks(final int sizeMb, final Env env) {
        return SetupTiming.networkTicks(sizeMb, false,
                Math.max(1, env.eraFactor()));
    }

    /** Sets a step running for a while, which is how every step that is not instant says so. */
    private void takesTime(final long now, final long ticks, final String what, final String done) {
        busyUntil = now + Math.max(1L, ticks);
        busyTotal = Math.max(1L, ticks);
        busyWhat = what;
        busyDone = done;
    }

    /**
     * Sets a step running whose output arrives while it runs rather than after it.
     *
     * <p>This is how the real tools behave and it is the reason a long step is watchable at all: a package
     * manager fetching a hundred megabytes does not sit silent and then print everything, it prints a line
     * per package as each one lands. The lines are kept here and handed out in order as the step proceeds,
     * so the terminal fills the way it fills on a real machine instead of waiting and then flooding.
     */
    private void takesTime(final long now, final long ticks, final String what, final List<String> done) {
        this.takesTime(now, ticks, what, String.join(String.valueOf(LINE), done));
    }

    /**
     * The part of a running step's output that has arrived by the given quarter of the way through it.
     *
     * @param quarter how far along the step is, in quarters, where the fourth is the end of it
     */
    public List<String> busyLinesThrough(final int quarter) {
        if (busyDone.isEmpty()) {
            return List.of();
        }
        final String[] all = busyDone.split(String.valueOf(LINE), -1);
        final int through = Math.max(0, Math.min(all.length, all.length * Math.max(0, quarter) / 4));
        final int from = Math.max(0, Math.min(through, all.length * Math.max(0, quarter - 1) / 4));
        return List.of(all).subList(from, through);
    }

    /** Whether the partition editor is open, which is a shell of its own with its own one-letter commands. */
    public boolean editingTable() {
        return !editing.isEmpty();
    }

    /** Runs one command line against the state. */
    public Result run(final String line, final Env env) {
        final String[] parts = line.trim().split("\\s+");
        final String cmd = parts.length == 0 ? "" : parts[0].toLowerCase(Locale.ROOT);
        final String arg1 = parts.length > 1 ? parts[1] : "";
        if (this.editingTable()) {
            return table(cmd, parts, env);
        }
        return switch (cmd) {
            case "fdisk" -> fdisk(arg1, env);
            case "mkfs.fat", "mkfs.vfat" -> mkfat(parts, env);
            case "echo" -> echo(line);
            case "pacman" -> pacman(parts, env);
            case "ls" -> ls(arg1);
            case "cat" -> cat(arg1, false);
            case "less", "more" -> cat(arg1, true);
            case "cd" -> cd(arg1);
            case "lsblk" -> lsblk(env);
            case "mkfs.ext4", "mkfs" -> mkfs(cmd.equals("mkfs") ? (parts.length > 2 ? parts[2] : "") : arg1, env);
            case "mount" -> mount(arg1, parts.length > 2 ? parts[2] : "");
            case "pacstrap" -> pacstrap(arg1, env);
            case "wget", "curl" -> fetchStage3(line, env);
            case "tar" -> unpackStage3(parts, line, env);
            case "genfstab" -> genfstab(line, env);
            case "arch-chroot", "chroot" -> enterChroot(arg1, env);
            case "emerge-webrsync", "emerge" -> emerge(line, env);
            case "genkernel" -> genkernel(env);
            case "grub-install" -> grub(arg1, env);
            case "grub-mkconfig" -> grubConfig(line, env);
            case "mkinitcpio" -> initramfs();
            case "hostname" -> hostname(parts);
            case "passwd" -> passwd();
            case "exit" -> exit();
            case "reboot" -> reboot(env);
            case "help" -> help();
            default -> Result.fail(cmd + ": command not found");
        };
    }

    /**
     * Turns what the player typed into the whole path it names.
     *
     * <p>Inside the new system a path is that system's own: {@code /etc/fstab} typed in there is the file the
     * step outside wrote to {@code /mnt/etc/fstab}, because that is the same file seen from inside. It is the
     * one thing about a chroot that has to be true for any of the rest to make sense.
     */
    private String resolve(final String typed) {
        final String path = typed.isEmpty() ? cwd : typed;
        final String whole = path.startsWith("/") ? path : (cwd.equals("/") ? "" : cwd) + "/" + path;
        final String tidy = tidy(whole);
        return chroot ? tidy(MOUNT + tidy) : tidy;
    }

    /** A path with its dots resolved and its trailing slash gone, so two ways of writing one agree. */
    private static String tidy(final String path) {
        final Deque<String> parts = new ArrayDeque<>();
        for (final String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                parts.pollLast();
                continue;
            }
            parts.addLast(part);
        }
        return parts.isEmpty() ? "/" : "/" + String.join("/", parts);
    }

    /** How a whole path reads back to somebody standing inside the new system. */
    private String asTyped(final String whole) {
        if (!chroot) {
            return whole;
        }
        return whole.equals(MOUNT) ? "/" : whole.startsWith(MOUNT + "/") ? whole.substring(MOUNT.length()) : whole;
    }

    /** Writes a file the installation made, creating the directories above it. */
    private void write(final String whole, final String content) {
        files.put(whole, content);
        String at = whole;
        while (at.lastIndexOf('/') > 0) {
            at = at.substring(0, at.lastIndexOf('/'));
            dirs.add(at);
        }
        dirs.add("/");
    }

    private Result ls(final String arg) {
        final String whole = resolve(arg);
        if (files.containsKey(whole)) {
            return Result.pass(asTyped(whole));
        }
        if (!dirs.contains(whole)) {
            return Result.fail("ls: cannot access '" + (arg.isEmpty() ? asTyped(cwd) : arg)
                    + "': No such file or directory");
        }
        final SortedSet<String> here = new TreeSet<>();
        final String prefix = whole.equals("/") ? "/" : whole + "/";
        for (final String dir : dirs) {
            if (dir.startsWith(prefix) && !dir.equals(whole)) {
                here.add(name(dir.substring(prefix.length())));
            }
        }
        for (final String file : files.keySet()) {
            if (file.startsWith(prefix)) {
                here.add(name(file.substring(prefix.length())));
            }
        }
        return here.isEmpty() ? Result.pass() : Result.pass(String.join("  ", here));
    }

    /** The first step of a path under a directory, which is what that directory lists. */
    private static String name(final String rest) {
        final int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private Result cat(final String arg, final boolean paged) {
        if (arg.isEmpty()) {
            return Result.fail(paged ? "Usage: less <file>" : "Usage: cat <file>");
        }
        final String whole = resolve(arg);
        if (dirs.contains(whole)) {
            return Result.fail("cat: " + arg + ": Is a directory");
        }
        final String content = files.get(whole);
        if (content == null) {
            return Result.fail((paged ? arg + ": " : "cat: " + arg + ": ") + "No such file or directory");
        }
        final List<String> out = new ArrayList<>(List.of(content.split("\n", -1)));
        if (paged) {
            // The pager has nowhere to page to on a screen this size, so it says what a pager says at the end.
            out.add("(END)");
        }
        return new Result(true, List.copyOf(out), false);
    }

    private Result cd(final String arg) {
        final String whole = resolve(arg.isEmpty() ? HOME : arg);
        if (files.containsKey(whole)) {
            return Result.fail("cd: " + arg + ": Not a directory");
        }
        if (!dirs.contains(whole)) {
            return Result.fail("cd: " + arg + ": No such file or directory");
        }
        cwd = asTyped(whole);
        return Result.pass();
    }

    /** Opens the partition editor on a disk, which takes the shell over until it is written or left. */
    private Result fdisk(final String arg, final Env env) {
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return Result.fail("Usage: fdisk /dev/<device>");
        }
        final Device disk = env.find(dev);
        if (disk == null) {
            return Result.fail("fdisk: cannot open /dev/" + dev + ": No such file or directory");
        }
        editing = dev;
        draft.clear();
        draft.addAll(tables.getOrDefault(dev, List.of()));
        gpt = !draft.isEmpty();
        return new Result(true, LiveInstallOutput.fdisk(dev, megabytes(disk.sizeMb())), false);
    }

    /**
     * One command inside the partition editor.
     *
     * <p>The one-letter commands of the real thing, and the same promise it makes: nothing reaches the disk
     * until {@code w}, and {@code q} walks away from everything typed since it opened.
     */
    private Result table(final String cmd, final String[] parts, final Env env) {
        switch (cmd) {
            case "m", "help" -> {
                return Result.pass("  g   create a new empty GPT partition table",
                        "  n   add a new partition            (n <size>, e.g. n 512M; no size takes the rest)",
                        "  t   change a partition type        (t <number> uefi)",
                        "  d   delete a partition             (d <number>)",
                        "  p   print the partition table",
                        "  w   write the table to disk and exit",
                        "  q   quit without saving changes");
            }
            case "g" -> {
                gpt = true;
                draft.clear();
                return Result.pass("Created a new GPT disklabel.");
            }
            case "p" -> {
                return print(env);
            }
            case "n" -> {
                if (!gpt) {
                    return Result.fail("Partition type not known. Create a disklabel first (g).");
                }
                final int size = megabytesOf(parts.length > 1 ? parts[1] : "");
                final int number = draft.size() + 1;
                draft.add(new Partition(number, size, false));
                return Result.pass("Created a new partition " + number + " of type 'Linux filesystem' and of size "
                        + (size > 0 ? megabytes(size) : "the rest of the disk") + ".");
            }
            case "t" -> {
                final int number = numberOf(parts.length > 1 ? parts[1] : "");
                final String type = parts.length > 2 ? parts[2].toLowerCase(Locale.ROOT) : "";
                if (number < 1 || number > draft.size()) {
                    return Result.fail("Partition " + (number < 1 ? "?" : number) + " does not exist yet!");
                }
                if (!type.equals("uefi") && !type.equals("efi") && !type.equals("linux")) {
                    return Result.fail("Usage: t <number> uefi|linux");
                }
                final Partition was = draft.get(number - 1);
                draft.set(number - 1, new Partition(was.number(), was.sizeMb(), !type.equals("linux")));
                return Result.pass("Changed type of partition " + number + " to '"
                        + draft.get(number - 1).type() + "'.");
            }
            case "d" -> {
                final int number = numberOf(parts.length > 1 ? parts[1] : "");
                if (number < 1 || number > draft.size()) {
                    return Result.fail("Partition " + (number < 1 ? "?" : number) + " does not exist yet!");
                }
                draft.remove(number - 1);
                renumber(draft);
                return Result.pass("Partition " + number + " has been deleted.");
            }
            case "w" -> {
                tables.put(editing, List.copyOf(draft));
                editing = "";
                draft.clear();
                return Result.pass("The partition table has been altered.",
                        "Calling ioctl() to re-read partition table.", "Syncing disks.");
            }
            case "q" -> {
                // Everything typed since it opened goes with it, which is what the opening line promised.
                editing = "";
                draft.clear();
                return Result.pass();
            }
            default -> {
                return Result.fail(cmd + ": unknown command", "Command (m for help):");
            }
        }
    }

    /** The table as the editor prints it, which is the disk's own size and what has been laid on it. */
    private Result print(final Env env) {
        final Device disk = env.find(editing);
        final List<String> out = new ArrayList<>();
        out.add("Disk /dev/" + editing + ": " + megabytes(disk == null ? 0 : disk.sizeMb()));
        out.add("Disklabel type: " + (gpt ? "gpt" : "dos"));
        if (draft.isEmpty()) {
            out.add("(no partitions)");
            return new Result(true, List.copyOf(out), false);
        }
        out.add("Device      Size            Type");
        for (final Partition part : draft) {
            out.add(String.format(Locale.ROOT, "%-11s %-15s %s", "/dev/" + part.on(editing),
                    part.sizeMb() > 0 ? megabytes(part.sizeMb()) : "rest", part.type()));
        }
        return new Result(true, List.copyOf(out), false);
    }

    /** After a delete, the ones below move up, exactly as the numbers on a real table do. */
    private static void renumber(final List<Partition> draft) {
        for (int i = 0; i < draft.size(); i++) {
            final Partition was = draft.get(i);
            draft.set(i, new Partition(i + 1, was.sizeMb(), was.esp()));
        }
    }

    /** A size as a person writes it at a partition editor: {@code 512M}, {@code 1G}, or nothing for the rest. */
    private static int megabytesOf(final String written) {
        if (written.isEmpty()) {
            return 0;
        }
        final String digits = written.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        final int value = Integer.parseInt(digits);
        final char unit = Character.toUpperCase(written.charAt(written.length() - 1));
        return unit == 'G' ? value * 1024 : unit == 'K' ? Math.max(1, value / 1024) : value;
    }

    private static int numberOf(final String written) {
        try {
            return Integer.parseInt(written.trim());
        } catch (final NumberFormatException wrong) {
            return -1;
        }
    }

    /** Megabytes as a partition editor writes them. */
    private static String megabytes(final int mb) {
        if (mb >= 1024 * 1024 && mb % (1024 * 1024) == 0) {
            return mb / (1024 * 1024) + " TiB";
        }
        if (mb >= 1024 && mb % 1024 == 0) {
            return mb / 1024 + " GiB";
        }
        return mb + " MiB";
    }

    /**
     * Lists the machine's block devices, in the columns the real tool lists them in.
     *
     * <p>Partitions hang off their disk on a drawn branch, in the plain characters the real tool falls back
     * to when the terminal is not told it can draw lines, which this one is not.
     */
    private Result lsblk(final Env env) {
        final List<String> out = new ArrayList<>();
        out.add(LiveInstallOutput.lsblkHeader());
        int index = 0;
        for (final Device disk : env.devices()) {
            out.add(LiveInstallOutput.lsblkRow(disk.name(), SCSI_DISK, index * MINORS_PER_DISK,
                    disk.sizeMb(), "disk", mountOf(disk.name())));
            final List<Partition> parts = tables.getOrDefault(disk.name(), List.of());
            for (int i = 0; i < parts.size(); i++) {
                final Partition part = parts.get(i);
                final String name = part.on(disk.name());
                final String branch = (i == parts.size() - 1 ? "`-" : "|-") + name;
                out.add(LiveInstallOutput.lsblkRow(branch, SCSI_DISK,
                        index * MINORS_PER_DISK + part.number(),
                        part.sizeMb() > 0 ? part.sizeMb() : rest(disk, parts),
                        "part", mountOf(name)));
            }
            index++;
        }
        return new Result(true, List.copyOf(out), false);
    }

    /** What is left of a disk once the partitions that asked for a size have had theirs. */
    private static long rest(final Device disk, final List<Partition> parts) {
        long left = disk.sizeMb();
        for (final Partition part : parts) {
            left -= Math.max(0, part.sizeMb());
        }
        return Math.max(0, left);
    }

    /** Where that device is mounted right now, which is what the listing's last column says. */
    private String mountOf(final String name) {
        if (mounted && name.equals(device)) {
            return MOUNT;
        }
        return !espMount.isEmpty() && name.equals(espDevice) ? espMount : "";
    }

    /** The partition of that name, or null when nothing on this machine is called it. */
    private Partition partitionOf(final String name) {
        for (final Map.Entry<String, List<Partition>> disk : tables.entrySet()) {
            for (final Partition part : disk.getValue()) {
                if (part.on(disk.getKey()).equals(name)) {
                    return part;
                }
            }
        }
        return null;
    }

    /** Makes the filesystem the firmware reads a bootloader out of, which only the modern one needs. */
    private Result mkfat(final String[] parts, final Env env) {
        String arg = "";
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("-") && !parts[i].equals("32")) {
                arg = parts[i];
            }
        }
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return Result.fail("Usage: mkfs.fat -F32 /dev/<partition>");
        }
        final Partition part = partitionOf(dev);
        if (part == null) {
            return Result.fail("mkfs.fat: unable to open " + dev + ": No such file or directory");
        }
        if (!part.esp()) {
            return Result.fail("mkfs.fat: " + dev + " is not an EFI System partition.",
                    "       (set its type first: fdisk, then t " + part.number() + " uefi)");
        }
        espDevice = dev;
        espFormatted = true;
        espMount = "";
        return new Result(true, LiveInstallOutput.mkfsFat(), false);
    }

    private static String deviceName(final String arg) {
        return arg.startsWith("/dev/") ? arg.substring(5) : arg;
    }

    private Result mkfs(final String arg, final Env env) {
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return Result.fail("Usage: mkfs.ext4 /dev/<device>");
        }
        final Partition part = partitionOf(dev);
        if (part == null && !env.has(dev)) {
            return Result.fail("mke2fs: No such file or directory while trying to determine filesystem size");
        }
        if (part != null && part.esp()) {
            return Result.fail("mke2fs: " + dev + " is the EFI System partition.",
                    "       (that one holds the bootloader: mkfs.fat -F32 /dev/" + dev + ")");
        }
        /*
         * A disk somebody partitioned is not a disk to write a filesystem straight onto: the real tool refuses
         * rather than quietly wiping the table somebody just made.
         */
        if (part == null && !tables.getOrDefault(dev, List.of()).isEmpty()) {
            return Result.fail("/dev/" + dev + " contains a gpt partition table.",
                    "mke2fs: will not make a filesystem here; use one of its partitions.");
        }
        device = dev;
        formatted = true;
        mounted = false;
        base = false;
        final long sizeMb = part != null && part.sizeMb() > 0 ? part.sizeMb() : sizeOf(dev, env);
        return new Result(true, LiveInstallOutput.mke2fs(dev, sizeMb, sizeMb), false);
    }

    /** How big that device is, by the disks the machine really has in it. */
    private static long sizeOf(final String dev, final Env env) {
        for (final Device disk : env.devices()) {
            if (dev.startsWith(disk.name())) {
                return disk.sizeMb();
            }
        }
        return 0;
    }

    private Result mount(final String arg, final String point) {
        final String dev = deviceName(arg);
        if (dev.isEmpty() || point.isEmpty()) {
            return Result.fail("mount: bad usage", "Try 'mount /dev/<device> /mnt'.");
        }
        /*
         * The place the firmware reads a bootloader out of hangs inside the new system, so it is mounted after
         * the root is and under it, exactly where the bootloader will go looking for it.
         */
        if (point.equals(MOUNT + "/boot")) {
            if (!mounted) {
                return Result.fail("mount: " + point + ": mount point does not exist.",
                        "       (mount the root first: mount /dev/<partition> /mnt)");
            }
            if (!espFormatted || !dev.equals(espDevice)) {
                return Result.fail("mount: " + point + ": wrong fs type on /dev/" + dev + ".",
                        "       (make it first: mkfs.fat -F32 /dev/" + dev + ")");
            }
            espMount = point;
            return Result.pass();
        }
        if (!point.equals(MOUNT)) {
            return Result.fail("mount: " + point + ": mount point does not exist.");
        }
        if (!formatted || !dev.equals(device)) {
            return Result.fail("mount: /mnt: wrong fs type, bad option, bad superblock on /dev/" + dev + ".",
                    "       (format it first: mkfs.ext4 /dev/" + dev + ")");
        }
        mounted = true;
        /* The real one says nothing when it works, and saying something would be the tell that this is not it. */
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
        laidOut();
        final long ticks = fetchTicks(BASE_MB, env);
        final List<String> all = ArchInstallOutput.pacstrap(MOUNT, megabytesPerSecond(BASE_MB, ticks));
        final int said = after(all, ":: Retrieving packages...");
        takesTime(env.now(), ticks, "the base system", all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    /**
     * Pulls the archive of a working system down from the mirror, which is its own step.
     *
     * <p>A command of its own, as the handbook has it, rather than folded into the unpack: fetching and
     * unpacking are two tools that print two different things, and one command quietly doing both would be
     * this mod's summary of the installation rather than the installation.
     */
    private Result fetchStage3(final String line, final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("wget: command not found");
        }
        if (!line.contains("stage3")) {
            return Result.fail("wget: missing URL", "Usage: wget [OPTION]... [URL]...");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (!env.mirror()) {
            return Result.fail("Resolving " + MIRROR + "... failed: Name or service not known.",
                    "wget: unable to resolve host address '" + MIRROR + "'");
        }
        final long ticks = fetchTicks(BASE_MB, env);
        final List<String> all =
                GentooInstallOutput.wget(MIRROR, BASE_MB, megabytesPerSecond(BASE_MB, ticks));
        final int said = after(all, "Saving to: '" + GentooInstallOutput.STAGE3 + "'");
        fetched = true;
        write(cwd + "/" + GentooInstallOutput.STAGE3, "");
        takesTime(env.now(), ticks, GentooInstallOutput.STAGE3, all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    /**
     * Lays the fetched archive out over the mounted disk, which is what makes the disk a system.
     *
     * <p>The archiver says nothing unless it is asked to, and names every path it writes when it is, which
     * is why the handbook asks it to: a screenful of paths going past is the only sign that a step with no
     * progress bar and several minutes of work in it is doing anything at all.
     */
    private Result unpackStage3(final String[] parts, final String line, final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("tar: command not found");
        }
        if (!line.contains("stage3")) {
            return Result.fail("tar: Cowardly refusing to create an empty archive",
                    "Try 'tar --help' or 'tar --usage' for more information.");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (!fetched) {
            return Result.fail("tar: " + GentooInstallOutput.STAGE3 + ": Cannot open: No such file or directory",
                    "tar: Error is not recoverable: exiting now");
        }
        if (!mounted) {
            return Result.fail("tar: " + MOUNT + ": Cannot open: No such file or directory",
                    "tar: Error is not recoverable: exiting now");
        }
        base = true;
        laidOut();
        /* Laying it out off the disk it is already on is quicker than pulling it over the network was. */
        final long ticks = Math.max(20L, fetchTicks(BASE_MB, env) / 3L);
        final boolean naming = parts.length > 1 && parts[1].replace("-", "").contains("v");
        if (!naming) {
            /* Asked to work quietly it works quietly, which is what it does and why the handbook asks it not to. */
            takesTime(env.now(), ticks, GentooInstallOutput.STAGE3, List.of());
            return new Result(true, List.of(), false);
        }
        final List<String> all = GentooInstallOutput.unpack();
        final int said = after(all, "./etc/");
        takesTime(env.now(), ticks, GentooInstallOutput.STAGE3, all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    private Result genfstab(final String line, final Env env) {
        if (distro != Distro.ARCH) {
            return Result.fail("genfstab: command not found");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (!base) {
            return Result.fail("genfstab: /mnt/etc does not exist (install the base system first)");
        }
        if (!line.contains("/mnt")) {
            return Result.fail("Usage: genfstab -U /mnt >> /mnt/etc/fstab");
        }
        fstab = true;
        // The table is written where it was told to go, so reading it back shows what this step really put there.
        final List<String> table = ArchInstallOutput.fstab(device, rootUuid(env), espDevice,
                espDevice.isEmpty() ? "" : LiveInstallOutput.shortUuid(espDevice, sizeOf(espDevice, env)));
        write(MOUNT + "/etc/fstab", String.join(String.valueOf(LINE), table));
        return new Result(true, table, false);
    }

    private Result enterChroot(final String point, final Env env) {
        if (!point.equals("/mnt")) {
            return Result.fail("chroot: cannot change root directory to '" + point + "': No such file or directory");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (!base) {
            return Result.fail("chroot: failed to run command '/bin/bash': No such file or directory");
        }
        chroot = true;
        // A chroot drops you at the root of the system you entered, which is what its prompt then shows.
        cwd = "/";
        return Result.pass();
    }

    /**
     * Says something, or puts it in a file.
     *
     * <p>How a line of configuration gets written without an editor, which is what the build options of the
     * source distribution are set with: {@code echo 'MAKEOPTS="-j4"' >> /etc/portage/make.conf}. A full screen
     * editor belongs with the editor work; this is the way anybody in a hurry does it anyway.
     */
    private Result echo(final String line) {
        final String said = line.length() > 4 ? line.substring(4).trim() : "";
        final int append = said.lastIndexOf(">>");
        final int over = append >= 0 ? -1 : said.lastIndexOf('>');
        if (append < 0 && over < 0) {
            return Result.pass(unquote(said));
        }
        final int at = append >= 0 ? append : over;
        final String text = unquote(said.substring(0, at).trim());
        final String target = said.substring(at + (append >= 0 ? 2 : 1)).trim();
        if (target.isEmpty()) {
            return Result.fail("bash: syntax error near unexpected token `newline'");
        }
        final String whole = resolve(target);
        if (dirs.contains(whole)) {
            return Result.fail("bash: " + target + ": Is a directory");
        }
        final String had = append >= 0 ? files.get(whole) : null;
        write(whole, had == null || had.isEmpty() ? text : had + "\n" + text);
        return Result.pass();
    }

    /** A value with the quotes a shell would have eaten taken off. */
    private static String unquote(final String text) {
        if (text.length() >= 2 && (text.charAt(0) == '\'' || text.charAt(0) == '"')
                && text.charAt(text.length() - 1) == text.charAt(0)) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    /** Installs an extra package inside the new system, over the Mirror, like everything else here. */
    private Result pacman(final String[] parts, final Env env) {
        if (distro != Distro.ARCH) {
            return Result.fail("pacman: command not found");
        }
        if (!chroot) {
            return Result.fail("pacman: this must be run inside the new system (arch-chroot /mnt)");
        }
        if (parts.length < 3 || !parts[1].startsWith("-S")) {
            return Result.fail("error: no operation specified (use -S <package>)");
        }
        if (!env.mirror()) {
            return Result.fail("error: failed retrieving file 'core.db' from mirror://mainframe",
                    "error: failed to synchronize all databases (unexpected error)");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        final String named = parts[2];
        final long ticks = fetchTicks(PACKAGE_MB, env);
        asked.add(named);
        final List<String> all = ArchInstallOutput.pacman(named, megabytesPerSecond(PACKAGE_MB, ticks));
        final int said = after(all, ":: Retrieving packages...");
        takesTime(env.now(), ticks, named, all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    /** The identifier of the filesystem the system is installed on, which is what names it everywhere. */
    private String rootUuid(final Env env) {
        return LiveInstallOutput.uuid(device, sizeOf(device, env));
    }

    /**
     * What a step says when something the machine is already doing has to finish first.
     *
     * <p>A real terminal would not have given the prompt back at all, so this stands in for the prompt that
     * is not there: it names what is running and how much of it is left.
     */
    private Result stillWorking(final Env env) {
        return Result.fail(busyWhat + ": still running (" + ((busyUntil - env.now()) / 20) + "s left).");
    }

    /** The directories a base system brings with it, which is what makes them there to look in. */
    private void laidOut() {
        dirs.add(MOUNT + "/etc");
        dirs.add(MOUNT + "/boot");
        dirs.add(MOUNT + "/root");
        dirs.add(MOUNT + "/usr");
        dirs.add(MOUNT + "/var");
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
        if (line.startsWith("emerge-webrsync")) {
            synced = true;
            return new Result(true, GentooInstallOutput.webrsync(MIRROR, TREE_FILES), false);
        }
        if (line.contains("--sync")) {
            synced = true;
            return new Result(true, GentooInstallOutput.sync(MIRROR, TREE_FILES), false);
        }
        if (!synced) {
            return Result.fail("!!! The portage tree is empty. Run emerge-webrsync or emerge --sync first.");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        final int jobs = Math.max(1, Math.min(makeJobs(), Math.max(1, env.cores())));
        if (line.contains("gentoo-sources")) {
            sources = true;
            /*
             * Kernel sources are fetched and laid out, never compiled: the package is a directory of source
             * code, and the half-hour of compiling it that everybody remembers belongs to the next step.
             */
            final long ticks = fetchTicks(PACKAGE_MB, env);
            return merging("sys-kernel/gentoo-sources", GentooInstallOutput.KERNEL,
                    "linux-" + GentooInstallOutput.KERNEL + ".tar.xz", jobs, true, ticks, env);
        }
        final String named = line.substring("emerge".length()).trim();
        if (named.isEmpty()) {
            return Result.fail("!!! No packages given.");
        }
        // Anything else asked for comes over the Mirror like everything else, and takes as long as it is big.
        asked.add(named);
        final String atom = named.contains("/") ? named : "app-misc/" + named;
        final String plain = atom.substring(atom.indexOf('/') + 1);
        return merging(atom, "1.0", plain + "-1.0.tar.xz", jobs, false, fetchTicks(PACKAGE_MB, env), env);
    }

    /**
     * Starts one package merging, printing what the tool has said by the time the work begins.
     *
     * <p>The split is where the real one's own is: it tells you what it is about to do and what it is
     * fetching straight away, and everything from the unpacking onwards arrives as it happens.
     */
    private Result merging(final String atom, final String version, final String archive, final int jobs,
                           final boolean sources, final long ticks, final Env env) {
        final List<String> all = GentooInstallOutput.emerge(atom, version, archive, jobs, sources);
        final int said = after(all, ">>> Unpacking source...");
        takesTime(env.now(), ticks, atom, all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    private Result genkernel(final Env env) {
        if (distro != Distro.GENTOO) {
            return Result.fail("genkernel: command not found");
        }
        if (!chroot) {
            return Result.fail("genkernel: this must be run inside the new system (chroot /mnt)");
        }
        if (!sources) {
            return Result.fail("* ERROR: no kernel sources found. emerge sys-kernel/gentoo-sources first.");
        }
        if (busy(env.now())) {
            return Result.fail("* ERROR: the kernel sources are still being laid out ("
                    + ((busyUntil - env.now()) / 20) + "s left)");
        }
        kernelBuilt = true;
        /*
         * This is the long one. Compiling a kernel is what a machine's processor is really for, so it is the
         * one step whose wait is set by how many cores the player put in and how fast they run.
         */
        final int jobs = Math.max(1, Math.min(makeJobs(), Math.max(1, env.cores())));
        final List<String> all = GentooInstallOutput.genkernel(GentooInstallOutput.KERNEL, jobs);
        final int said = after(all, "* kernel: >> Initializing ...");
        takesTime(env.now(), compileTicks(env), "the kernel", all.subList(said, all.size()));
        return new Result(true, List.copyOf(all.subList(0, said)), false);
    }

    /**
     * Installs the bootloader, for the firmware this machine really has.
     *
     * <p>A machine of the older firmware takes it on the disk itself and is named a disk; a machine of the
     * modern one takes it in a partition of its own, is named nothing, and needs that partition mounted where
     * it will be looked for. Saying one platform on every machine, as this did, was the plainest way of
     * pretending the two are the same thing.
     */
    private Result grub(final String arg, final Env env) {
        if (!chroot) {
            return Result.fail("grub-install: error: cannot find EFI directory (run this inside the new system).");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (distro == Distro.GENTOO && !kernelBuilt) {
            return Result.fail("grub-install: error: no kernel image found in /boot (run genkernel first).");
        }
        if (env.uefi()) {
            if (espMount.isEmpty()) {
                return Result.fail("grub-install: error: failed to get canonical path of '/boot/efi'.",
                        "       (this machine boots the modern way: make an EFI partition, then",
                        "        mkfs.fat -F32 /dev/sdXn and mount /dev/sdXn /mnt/boot)");
            }
            bootloader = true;
            write(MOUNT + "/boot/EFI/BOOT/BOOTX64.EFI", "");
            return Result.pass("Installing for x86_64-efi platform.",
                    "Installation finished. No error reported.");
        }
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return Result.fail("grub-install: error: install device isn't specified.");
        }
        /*
         * On the older firmware it goes onto the disk rather than into a partition, so being named the
         * partition the system sits in is the mistake this catches.
         */
        if (!dev.equals(diskOf(device))) {
            return Result.fail("grub-install: error: cannot find a device for /dev/" + dev + ".",
                    "       (it goes on the disk, not in a partition: grub-install /dev/" + diskOf(device) + ")");
        }
        bootloader = true;
        return Result.pass("Installing for i386-pc platform.", "Installation finished. No error reported.");
    }

    /** The disk a device name sits on: {@code sda2} is on {@code sda}, and {@code sda} is its own. */
    private static String diskOf(final String name) {
        int end = name.length();
        while (end > 0 && Character.isDigit(name.charAt(end - 1))) {
            end--;
        }
        return name.substring(0, end);
    }

    /**
     * Writes the bootloader's own list of what it can start, which is the step that makes it able to.
     *
     * <p>Generated from the system that is really installed, so reading the file back shows this machine's
     * system on the disk it is on rather than a line written in advance.
     */
    private Result grubConfig(final String line, final Env env) {
        if (!chroot) {
            return Result.fail("grub-mkconfig: command not found");
        }
        if (busy(env.now())) {
            return stillWorking(env);
        }
        if (!bootloader) {
            return Result.fail("/usr/bin/grub-mkconfig: line 1: /boot/grub: No such file or directory",
                    "       (install the bootloader first: grub-install)");
        }
        if (!line.contains("-o")) {
            return Result.fail("Usage: grub-mkconfig -o /boot/grub/grub.cfg");
        }
        final String name = distro == Distro.ARCH ? "Arch Linux" : "Gentoo Linux";
        final String image = kernelImage();
        final String uuid = rootUuid(env);
        final String config = String.join(String.valueOf(LINE),
                "# generated by grub-mkconfig",
                "menuentry '" + name + "' {",
                "        set root='hd0'",
                "        linux " + image + " root=UUID=" + uuid + " rw",
                "        initrd " + image.replace("vmlinuz", "initramfs") + ".img",
                "}");
        write(MOUNT + "/boot/grub/grub.cfg", config);
        grubConfig = true;
        return new Result(true, LiveInstallOutput.grubMkconfig(List.of(image), false), false);
    }

    /** Where this distribution puts the kernel it installs, which is not where the other one puts it. */
    private String kernelImage() {
        return distro == Distro.ARCH
                ? "/boot/vmlinuz-linux"
                : "/boot/vmlinuz-" + GentooInstallOutput.KERNEL + "-gentoo";
    }

    /** Builds the image the kernel is handed at boot, which on this distribution is its own step. */
    private Result initramfs() {
        if (distro != Distro.ARCH) {
            return Result.fail("mkinitcpio: command not found");
        }
        if (!chroot) {
            return Result.fail("mkinitcpio: this must be run inside the new system (arch-chroot /mnt)");
        }
        write(MOUNT + "/boot/initramfs-linux.img", "");
        write(MOUNT + "/boot/initramfs-linux-fallback.img", "");
        initramfs = true;
        return new Result(true, ArchInstallOutput.mkinitcpio(ARCH_RELEASE), false);
    }

    /**
     * Names the machine, which is the first thing in this sequence the player chooses rather than performs.
     *
     * <p>Written into the new system's own file, so it is there to read back and, when the install finishes,
     * there to carry over: a machine installed by hand answers to the name its installer was given.
     */
    private Result hostname(final String[] parts) {
        if (!chroot) {
            return Result.fail("hostname: this must be run inside the new system");
        }
        if (parts.length > 2) {
            // A name is one word: two of them is somebody who has not been told that yet.
            return Result.fail("hostname: the specified hostname is invalid");
        }
        final String name = parts.length > 1 ? parts[1].trim() : "";
        if (name.isEmpty()) {
            final String written = files.get(MOUNT + "/etc/hostname");
            return Result.pass(written == null || written.isEmpty() ? hostname() : written);
        }
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9-]{0,14}")) {
            return Result.fail("hostname: the specified hostname is invalid",
                    "       (letters, digits and dashes, up to 15, starting with a letter or digit)");
        }
        write(MOUNT + "/etc/hostname", name);
        chosenName = name;
        return Result.pass();
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
        // Back out on the medium, where the session was standing before it went in.
        cwd = HOME;
        return Result.pass();
    }

    private Result reboot(final Env env) {
        if (chroot) {
            return Result.fail("reboot: you are inside the chroot. exit first.");
        }
        if (busy(env.now())) {
            // Restarting on top of something still arriving would leave the system half of what was asked for.
            return stillWorking(env);
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
        if (distro == Distro.ARCH && !initramfs) {
            missing.add("no initramfs (mkinitcpio -P)");
        }
        if (!bootloader) {
            missing.add("no bootloader (grub-install)");
        }
        /*
         * A bootloader with nothing to start is a bootloader that starts nothing, so the list it is given
         * counts as much as the bootloader itself.
         */
        if (!grubConfig) {
            missing.add("nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)");
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
                        "genfstab -U /mnt >> /mnt/etc/fstab | arch-chroot /mnt | grub-install /dev/sdX",
                        "passwd | exit | reboot",
                        "ls | cd | cat | less        the whole of it is written in /root/install.txt")
                : Result.pass("lsblk | mkfs.ext4 /dev/sdX | mount /dev/sdX /mnt | tar xpf stage3-amd64.tar.xz -C /mnt",
                        "chroot /mnt | emerge --sync | emerge sys-kernel/gentoo-sources | genkernel all",
                        "grub-install /dev/sdX | passwd | exit | reboot",
                        "ls | cd | cat | less        the whole of it is written in /root/install.txt");
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
        put(out, "fetched", fetched);
        put(out, "base", base);
        put(out, "fstab", fstab);
        put(out, "chroot", chroot);
        put(out, "synced", synced);
        put(out, "busy_until", Long.toString(busyUntil));
        put(out, "busy_total", Long.toString(busyTotal));
        put(out, "busy_what", busyWhat);
        put(out, "busy_done", busyDone);
        put(out, "sources", sources);
        put(out, "kernel_built", kernelBuilt);
        put(out, "bootloader", bootloader);
        put(out, "grub_config", grubConfig);
        put(out, "initramfs", initramfs);
        put(out, "chosen_name", chosenName);
        put(out, "asked", String.join(" ", asked));
        put(out, "password", password);
        put(out, "cwd", cwd);
        put(out, "editing", editing);
        put(out, "gpt", gpt);
        put(out, "esp_device", espDevice);
        put(out, "esp_formatted", espFormatted);
        put(out, "esp_mount", espMount);
        for (final Map.Entry<String, List<Partition>> disk : tables.entrySet()) {
            final StringBuilder written = new StringBuilder();
            for (final Partition part : disk.getValue()) {
                if (!written.isEmpty()) {
                    written.append(',');
                }
                written.append(part.number()).append(':').append(part.sizeMb())
                        .append(':').append(part.esp() ? 1 : 0);
            }
            put(out, "table:" + disk.getKey(), written.toString());
        }
        /*
         * Only what the installation made: the guide the medium carries is put back when the session is built
         * again, because it belongs to the medium rather than to the work done on it.
         */
        for (final String dir : dirs) {
            put(out, "dir:" + dir, "1");
        }
        for (final Map.Entry<String, String> file : files.entrySet()) {
            if (!file.getKey().equals(HOME + "/install.txt")) {
                put(out, "file:" + file.getKey(), file.getValue());
            }
        }
        return out.toString();
    }

    public static LiveInstallState deserialize(final String s) {
        final Map<String, String> saved = read(s);
        final Distro distro = Distro.find(saved.getOrDefault("distro", ""));
        if (distro == null) {
            return null;
        }
        final LiveInstallState st = new LiveInstallState(distro);
        st.device = saved.getOrDefault("device", "");
        st.formatted = flag(saved, "formatted");
        st.mounted = flag(saved, "mounted");
        st.fetched = flag(saved, "fetched");
        st.base = flag(saved, "base");
        st.fstab = flag(saved, "fstab");
        st.chroot = flag(saved, "chroot");
        st.synced = flag(saved, "synced");
        st.busyUntil = number(saved, "busy_until", -1L);
        st.busyTotal = number(saved, "busy_total", 0L);
        st.busyWhat = saved.getOrDefault("busy_what", "");
        st.busyDone = saved.getOrDefault("busy_done", "");
        st.sources = flag(saved, "sources");
        st.kernelBuilt = flag(saved, "kernel_built");
        st.bootloader = flag(saved, "bootloader");
        st.grubConfig = flag(saved, "grub_config");
        st.initramfs = flag(saved, "initramfs");
        st.chosenName = saved.getOrDefault("chosen_name", "");
        for (final String pkg : saved.getOrDefault("asked", "").split(" ")) {
            if (!pkg.isEmpty()) {
                st.asked.add(pkg);
            }
        }
        st.password = flag(saved, "password");
        st.cwd = saved.getOrDefault("cwd", HOME);
        st.editing = saved.getOrDefault("editing", "");
        st.gpt = flag(saved, "gpt");
        st.espDevice = saved.getOrDefault("esp_device", "");
        st.espFormatted = flag(saved, "esp_formatted");
        st.espMount = saved.getOrDefault("esp_mount", "");
        for (final Map.Entry<String, String> line : saved.entrySet()) {
            if (line.getKey().startsWith("dir:")) {
                st.dirs.add(line.getKey().substring(4));
            } else if (line.getKey().startsWith("file:")) {
                st.files.put(line.getKey().substring(5), line.getValue());
            } else if (line.getKey().startsWith("table:")) {
                st.tables.put(line.getKey().substring(6), partitions(line.getValue()));
            }
        }
        return st;
    }

    /** A partition table read back from the one line it was written as. */
    private static List<Partition> partitions(final String written) {
        if (written.isEmpty()) {
            return List.of();
        }
        final List<Partition> table = new ArrayList<>();
        for (final String one : written.split(",")) {
            final String[] field = one.split(":");
            if (field.length == 3) {
                try {
                    table.add(new Partition(Integer.parseInt(field[0]), Integer.parseInt(field[1]),
                            field[2].equals("1")));
                } catch (final NumberFormatException wrong) {
                    return List.copyOf(table);
                }
            }
        }
        return List.copyOf(table);
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
    private static Map<String, String> read(final String s) {
        final Map<String, String> saved = new LinkedHashMap<>();
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

    private static boolean flag(final Map<String, String> saved, final String name) {
        return "1".equals(saved.get(name));
    }

    private static long number(final Map<String, String> saved, final String name, final long fallback) {
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
