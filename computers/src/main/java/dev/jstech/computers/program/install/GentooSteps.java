/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.install.voice.ArchiveVoice;
import dev.jstech.computers.program.install.voice.FetchVoice;
import dev.jstech.computers.program.install.voice.KernelVoices;
import dev.jstech.computers.program.install.voice.PortageVoices;
import dev.jstech.computers.program.install.voice.WorldStamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The steps that are Gentoo's own: fetching the archive of a base system and unpacking it, the package
 * tree, the merges, and the two ways to a kernel.
 *
 * <p>Each refuses what a real machine would refuse at that point, in the words the real tool uses, and each
 * that takes time is left running in front of the terminal rather than answered: what it is for is done when
 * it ends.
 */
final class GentooSteps {

    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress;

    /** The archive of a base system, as the Mirror names it. */
    static final String STAGE3 = "stage3-amd64-openrc.tar.xz";

    /** The kernel the sources are of. */
    static final String KERNEL = "6.11.5";

    private static final String SOURCES_DIR = "/usr/src/linux";

    /** How big the archive of a base system is, the usual kind of estimate. */
    private static final int STAGE3_MB = 256;

    /** How big a snapshot of the package tree is, and how many files it unpacks to. */
    private static final int TREE_MB = 48;
    private static final int TREE_FILES = 214_483;

    /** What bringing a fresh base system up to date merges: three real packages, two upgraded and one new. */
    private static final String[][] WORLD = {
        {"sys-libs/ncurses", "6.5_p20250125", "6.4_p20240414", "ncurses-6.5.tar.gz", "3.6", "cxx tinfo",
            "-debug -doc"},
        {"app-shells/bash", "5.2_p37", "5.2_p26", "bash-5.2.tar.gz", "10.7", "net nls readline", "-afs -plugins"},
        {"app-editors/vim", "9.1.0794", "", "vim-9.1.0794.tar.gz", "16.8", "acl crypt nls", "-X -lua -perl"},
    };

    GentooSteps(final LiveFiles files, final LiveDisks disks, final LiveProgress progress) {
        this.files = files;
        this.disks = disks;
        this.progress = progress;
    }

    /** How many jobs the build options ask for, which is one until somebody writes otherwise. */
    int makeJobs() {
        return MakeOpts.jobs(this.buildOptions());
    }

    /** The build options as they stand in the new system, or null while it has no such file. */
    String buildOptions() {
        return this.files.read(this.files.inNewSystem(MakeOpts.PATH));
    }

    /** Pulls the archive of a base system down from the Mirror, into wherever the session is standing. */
    LiveTurn wget(final String[] parts, final LiveInstallState.Env env) {
        final String url = parts.length > 1 ? parts[parts.length - 1] : "";
        if (url.isEmpty() || url.startsWith("-")) {
            return LiveTurn.refused("wget: missing URL", "Usage: wget [OPTION]... [URL]...");
        }
        final WorldStamp stamp = WorldStamp.of(env.dayTime());
        if (!url.startsWith(FetchVoice.MIRROR) || !env.mirror()) {
            /* There is one network in this world, the one the player built, and one thing on it to fetch from. */
            return LiveTurn.running(FetchVoice.unreachable(url, stamp));
        }
        if (!url.contains("stage3")) {
            return LiveTurn.refused("--" + stamp.dated() + "--  " + url,
                    "Mirror request sent, awaiting response... 404 Not Found", "ERROR 404: Not Found.");
        }
        final String saveAs = this.files.resolve(STAGE3);
        return LiveTurn.running(FetchVoice.wget("/gentoo/" + STAGE3, STAGE3_MB,
                LiveTimes.fetch(STAGE3_MB, env), stamp, () -> {
                    this.progress.fetched = true;
                    this.files.write(saveAs, "");
                }));
    }

    /** Lays the fetched archive out over the mounted disk, which is what makes the disk a system. */
    LiveTurn tar(final String[] parts, final String line, final LiveInstallState.Env env) {
        if (!line.contains("stage3")) {
            return LiveTurn.refused("tar: Cowardly refusing to create an empty archive",
                    "Try 'tar --help' or 'tar --usage' for more information.");
        }
        final boolean here = this.files.isFile(this.files.resolve(STAGE3));
        if (!here) {
            return LiveTurn.refused("tar: " + STAGE3 + ": Cannot open: No such file or directory",
                    "tar: Error is not recoverable: exiting now");
        }
        final boolean overTheDisk = this.files.resolve("").equals(this.files.root())
                || line.contains("-C " + this.files.root()) || line.contains("-C" + this.files.root());
        if (!this.disks.mounted() || !overTheDisk) {
            /*
             * Unpacked anywhere else it would land on the medium itself, which is memory and is gone at the
             * next restart. The real one would do it without a word; this one says why it will not.
             */
            return LiveTurn.refused("tar: refusing to unpack a whole system over the live medium",
                    "     (cd " + this.files.root() + " first, with the disk mounted there)");
        }
        final boolean naming = parts.length > 1 && parts[1].replace("-", "").contains("v");
        final LiveInstallState.Device disk = env.find(LiveDisks.diskOf(this.disks.rootDevice()));
        return LiveTurn.running(ArchiveVoice.unpack(naming,
                LiveTimes.write(STAGE3_MB * 4, disk == null ? 1 : disk.speed()), this::laidOut));
    }

    /** The package manager, in every form the installation uses it in. */
    LiveTurn emerge(final String[] parts, final String line, final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("emerge: command not found (the package manager is the new system's:",
                    "        chroot " + this.files.root() + " /bin/bash first)");
        }
        final WorldStamp stamp = WorldStamp.of(env.dayTime());
        if (parts[0].equals("emerge-webrsync") || line.contains("--sync")) {
            if (!env.mirror()) {
                return LiveTurn.refused("!!! Could not resolve " + FetchVoice.MIRROR,
                        "!!! Synchronization failed");
            }
            final Runnable synced = () -> this.progress.synced = true;
            return LiveTurn.running(parts[0].equals("emerge-webrsync")
                    ? PortageVoices.webrsync(TREE_MB, LiveTimes.fetch(TREE_MB, env),
                            LiveTimes.write(TREE_MB * 8, 4), TREE_FILES, stamp, synced)
                    : PortageVoices.sync(LiveTimes.fetch(TREE_MB / 4.0, env), TREE_FILES, synced));
        }
        if (line.contains("--config")) {
            this.progress.timezone = true;
            return LiveTurn.said(" * Updating /etc/localtime with /usr/share/zoneinfo/"
                    + zoneOr(this.files.read(this.files.inNewSystem("/etc/timezone")), "UTC"));
        }
        if (!this.progress.synced) {
            return LiveTurn.refused("!!! The portage tree is empty. Run emerge-webrsync or emerge --sync first.");
        }
        if (!env.mirror()) {
            return LiveTurn.refused("!!! Could not resolve " + FetchVoice.MIRROR, "!!! Fetch failed");
        }
        boolean ask = false;
        final List<String> targets = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            final String word = parts[i];
            if (word.equals("--ask") || (word.startsWith("-") && !word.startsWith("--") && word.contains("a"))) {
                ask = true;
            } else if (!word.startsWith("-")) {
                targets.add(word);
            }
        }
        if (targets.isEmpty()) {
            return LiveTurn.refused("!!! No packages given.");
        }
        final int jobs = LiveTimes.jobs(this.makeJobs(), env);
        return LiveTurn.running(PortageVoices.emerge(this.merges(targets, jobs, env), ask, jobs,
                () -> this.merged(targets)));
    }

    /** The chooser: profiles, kernels, and the news nobody reads. */
    LiveTurn eselect(final String[] parts) {
        if (!this.files.inside()) {
            return LiveTurn.refused("eselect: command not found");
        }
        final String what = parts.length > 1 ? parts[1] : "";
        final String action = parts.length > 2 ? parts[2] : "list";
        if (what.equals("profile")) {
            if (action.equals("set")) {
                this.progress.profileChosen = true;
                return LiveTurn.silent();
            }
            return LiveTurn.said(PortageVoices.profiles(1));
        }
        if (what.equals("kernel")) {
            if (!this.progress.sources) {
                return LiveTurn.refused("!!! Error: No kernel sources found in /usr/src");
            }
            if (action.equals("set")) {
                this.progress.kernelChosen = true;
                this.files.makeDir(this.files.inNewSystem(SOURCES_DIR));
                return LiveTurn.silent();
            }
            return LiveTurn.said("Available kernel symlink targets:",
                    "  [1]   linux-" + KERNEL + "-gentoo" + (this.progress.kernelChosen ? " *" : ""));
        }
        if (what.equals("news")) {
            return LiveTurn.said("No news is good news.");
        }
        return LiveTurn.refused("!!! Error: Can't load module " + what,
                "exiting");
    }

    /** The kernel builder, which does the whole of it and says little while it does. */
    LiveTurn genkernel(final LiveInstallState.Env env) {
        final LiveTurn refused = this.kernelRefused("genkernel");
        if (refused != null) {
            return refused;
        }
        final int jobs = LiveTimes.jobs(this.makeJobs(), env);
        return LiveTurn.running(KernelVoices.genkernel(KERNEL,
                LiveTimes.compile(LiveTimes.KERNEL_WORK, jobs, env), this::kernelInstalled));
    }

    /**
     * Building the kernel by hand, from where its sources are.
     *
     * <p>The three of the handbook: the build, the modules, the install. Typed as one line joined with
     * {@code &&} they run as one; typed apart, the build is the long one and the other two are quick, and
     * there is no kernel to boot until the last of them.
     */
    LiveTurn make(final String line, final LiveInstallState.Env env) {
        final LiveTurn refused = this.kernelRefused("make");
        if (refused != null) {
            return refused;
        }
        if (!this.files.cwd().equals(SOURCES_DIR)) {
            return LiveTurn.refused("make: *** No targets specified and no makefile found.  Stop.",
                    "      (the kernel is built from its sources: cd " + SOURCES_DIR + ")");
        }
        final boolean builds = line.matches("(?s)^make(\\s+-j\\s*\\d*)?\\s*(&&.*)?$");
        final boolean installs = line.contains("make install") || line.endsWith(" install");
        final Matcher asked = Pattern.compile("-j\\s*(\\d+)").matcher(line);
        final int jobs = LiveTimes.jobs(asked.find() ? Integer.parseInt(asked.group(1)) : this.makeJobs(), env);
        if (builds) {
            return LiveTurn.running(KernelVoices.make(KERNEL, jobs,
                    LiveTimes.compile(LiveTimes.KERNEL_WORK, jobs, env), () -> {
                        this.progress.kernelCompiled = true;
                        if (installs) {
                            this.kernelInstalled();
                        }
                    }));
        }
        if (!this.progress.kernelCompiled) {
            return LiveTurn.refused("make: *** No rule to make target 'vmlinux'. Build the kernel first: make");
        }
        if (line.contains("modules_install")) {
            return LiveTurn.said("  DEPMOD  /lib/modules/" + KERNEL + "-gentoo");
        }
        if (installs) {
            this.kernelInstalled();
            return LiveTurn.said("  INSTALL /boot");
        }
        return LiveTurn.refused("make: *** No rule to make target. Stop.");
    }

    /** Generates the locales the file names, a job to each. */
    LiveTurn localeGen(final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused("locale-gen: command not found");
        }
        final List<String> locales = new ArrayList<>(List.of("C.UTF-8"));
        final String file = this.files.read(this.files.inNewSystem("/etc/locale.gen"));
        for (final String each : (file == null ? "" : file).split("\n")) {
            if (!each.isBlank() && !each.startsWith("#")) {
                locales.add(each.trim().split("\\s+")[0]);
            }
        }
        final int jobs = LiveTimes.jobs(this.makeJobs(), env);
        return LiveTurn.running(PortageVoices.localeGen(locales, jobs,
                LiveTimes.compile(24_000L, jobs, env) / 2, () -> this.progress.localesGenerated = true));
    }

    /** Whether the table of what to mount, which this distribution has written by hand, names a root. */
    boolean fstabNamesARoot() {
        final String table = this.files.read(this.files.inNewSystem("/etc/fstab"));
        if (table == null) {
            return false;
        }
        for (final String line : table.split("\n")) {
            final String[] fields = line.trim().split("\\s+");
            if (!line.trim().startsWith("#") && fields.length >= 3 && fields[1].equals("/")) {
                return true;
            }
        }
        return false;
    }

    /** Why a kernel cannot be built yet, or null when it can. */
    private LiveTurn kernelRefused(final String tool) {
        if (!this.files.inside()) {
            return LiveTurn.refused(tool + ": command not found (it is the new system's: chroot "
                    + this.files.root() + " /bin/bash first)");
        }
        if (!this.progress.sources) {
            return LiveTurn.refused("* ERROR: no kernel sources found in /usr/src",
                    "         (emerge sys-kernel/gentoo-sources first)");
        }
        return null;
    }

    private void kernelInstalled() {
        this.progress.kernelBuilt = true;
        this.progress.initramfs = true;
        this.files.write(this.files.inNewSystem("/boot/vmlinuz-" + KERNEL + "-gentoo-x86_64"), "");
    }

    /** What the packages asked for come to, each with how long this machine takes to fetch and to build it. */
    private List<PortageVoices.Merge> merges(final List<String> targets, final int jobs,
                                             final LiveInstallState.Env env) {
        final List<PortageVoices.Merge> out = new ArrayList<>();
        for (final String target : targets) {
            if (target.equals("@world")) {
                for (final String[] one : WORLD) {
                    final double size = Double.parseDouble(one[4]);
                    out.add(new PortageVoices.Merge(one[0], one[1], one[2], one[3], size, one[5], one[6],
                            LiveTimes.fetch(size, env), LiveTimes.build(size, jobs, env)));
                }
            } else if (target.contains("gentoo-sources")) {
                /* Sources are fetched, unpacked and patched, never compiled: that is the next step's job. */
                out.add(new PortageVoices.Merge("sys-kernel/gentoo-sources", KERNEL, "", "linux-6.11.tar.xz", 140,
                        "", "-build -symlink", LiveTimes.fetch(140, env), 0));
            } else if (target.endsWith("grub")) {
                out.add(new PortageVoices.Merge("sys-boot/grub", "2.12-r5", "", "grub-2.12.tar.xz", 6.4,
                        "fonts nls themes", "-device-mapper -doc", LiveTimes.fetch(6.4, env),
                        LiveTimes.build(6.4 * 3, jobs, env)));
            } else {
                final String atom = target.contains("/") ? target : "app-misc/" + target;
                final String name = atom.substring(atom.indexOf('/') + 1).toLowerCase(Locale.ROOT);
                out.add(new PortageVoices.Merge(atom, "1.0", "", name + "-1.0.tar.xz", 8.0, "nls", "-debug",
                        LiveTimes.fetch(8.0, env), LiveTimes.build(8.0, jobs, env)));
            }
        }
        return out;
    }

    /** What having those packages merged means to the installation. */
    private void merged(final List<String> targets) {
        for (final String target : targets) {
            if (target.equals("@world")) {
                this.progress.worldUpdated = true;
            } else if (target.contains("gentoo-sources")) {
                this.progress.sources = true;
                this.files.makeDir(this.files.inNewSystem("/usr/src/linux-" + KERNEL + "-gentoo"));
            } else if (target.endsWith("grub")) {
                this.progress.grubPackage = true;
            } else {
                this.progress.ask(target);
            }
        }
    }

    /** The directories and files a base system brings with it, which is what makes them there to look in. */
    private void laidOut() {
        this.progress.startOver();
        this.progress.base = true;
        for (final String dir : new String[]{"/etc", "/etc/portage", "/boot", "/efi", "/root", "/usr", "/usr/src",
            "/var", "/proc", "/sys", "/dev", "/run"}) {
            this.files.makeDir(this.files.inNewSystem(dir));
        }
        this.files.write(this.files.inNewSystem(MakeOpts.PATH), String.join("\n",
                "# These settings were set by the catalyst build script that automatically",
                "# built this stage.",
                "# Please consult /usr/share/portage/config/make.conf.example for a more",
                "# detailed example.",
                "COMMON_FLAGS=\"-O2 -pipe\"",
                "CFLAGS=\"${COMMON_FLAGS}\"",
                "CXXFLAGS=\"${COMMON_FLAGS}\"",
                "FCFLAGS=\"${COMMON_FLAGS}\"",
                "FFLAGS=\"${COMMON_FLAGS}\"",
                "",
                "# This sets the language of build output to English.",
                "# Please keep this setting intact when reporting bugs.",
                "LC_MESSAGES=C.utf8"));
        this.files.write(this.files.inNewSystem("/etc/locale.gen"), String.join("\n",
                "# /etc/locale.gen: list all of the locales you want to have on your system.",
                "#",
                "# The format of each line:",
                "# <locale name> <charset>",
                "#",
                "#en_US.UTF-8 UTF-8",
                "#ja_JP.EUC-JP EUC-JP"));
        this.files.write(this.files.inNewSystem("/etc/fstab"), String.join("\n",
                "# /etc/fstab: static file system information.",
                "#",
                "# <fs>                  <mountpoint>    <type>  <opts>          <dump> <pass>"));
    }

    private static String zoneOr(final String written, final String fallback) {
        return written == null || written.isBlank() ? fallback : written.trim();
    }
}
