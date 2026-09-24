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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
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
@TextHolder
final class GentooSteps {

    private final LiveFiles files;
    private final LiveDisks disks;
    private final LiveProgress progress;

    /** The archive of a base system, as the Mirror names it. */
    static final String STAGE3 = "stage3-vel64-openrc.tar.xz";

    /** The kernel the sources are of. */
    static final String KERNEL = "6.11.5";

    private static final String SOURCES_DIR = "/usr/src/linux";

    /** The target of the kernel's build that puts the built kernel where the bootloader looks for one. */
    private static final String INSTALL_TARGET = "install";

    /** How big the archive of a base system is, the usual kind of estimate. */
    private static final int STAGE3_MB = 256;

    /** How big a snapshot of the package tree is, and how many files it unpacks to. */
    private static final int TREE_MB = 48;
    private static final int TREE_FILES = 214_483;

    /** What bringing a fresh base system up to date merges: three real packages, two upgraded and one new. */
    private static final String[][] WORLD = {
        {"sys-libs/ncurses", "6.5_p20250125", "6.4_p20240414", "ncurses-6.5.tar.gz", "3.6", flags("cxx", "tinfo"),
            flags("-debug", "-doc")},
        {"app-shells/bash", "5.2_p37", "5.2_p26", "bash-5.2.tar.gz", "10.7", flags("net", "nls", "readline"),
            flags("-afs", "-plugins")},
        {"app-editors/vim", "9.1.0794", "", "vim-9.1.0794.tar.gz", "16.8", flags("acl", "crypt", "nls"),
            flags("-X", "-lua", "-perl")},
    };

    /*
     * The two files a base system arrives with that anybody reads. Files are data in the machine's own words, so
     * they read the same whatever language the player has.
     */
    private static final Text MAKE_CONF = Text.literal(String.join("\n",
            "# These settings were set by the catalyst build script that automatically",
            "# built this stage.",
            "# Please consult /usr/share/portage/config/make.conf.example for a more",
            "# detailed example.",
            "COMMON_FLAGS=\"-O2 -pipe\"",
            // One line for each of the two languages this world's software is written in.
            "SGFLAGS=\"${COMMON_FLAGS}\"",
            "SGSFLAGS=\"${COMMON_FLAGS}\"",
            "",
            "# This sets the language of build output to English.",
            "# Please keep this setting intact when reporting bugs.",
            "LC_MESSAGES=POSIX.utf8"));
    private static final Text FSTAB = Text.literal(String.join("\n",
            "# /etc/fstab: static file system information.",
            "#",
            "# <fs>                  <mountpoint>    <type>  <opts>          <dump> <pass>"));

    private static final TextKey WGET_MISSING_URL = TextKey.of("jsc.install.gentoo_steps.wget_missing_url",
            "wget: missing URL");
    private static final TextKey WGET_USAGE = TextKey.of("jsc.install.gentoo_steps.wget_usage",
            "Usage: wget [OPTION]... [URL]...");
    private static final TextKey REQUEST_SENT = TextKey.of("jsc.install.gentoo_steps.request_sent",
            "%s request sent, awaiting response... %s");
    private static final TextKey FETCH_ERROR = TextKey.of("jsc.install.gentoo_steps.fetch_error",
            "ERROR %s: %s.");
    private static final TextKey TAR_EMPTY = TextKey.of("jsc.install.gentoo_steps.tar_empty",
            "tar: Cowardly refusing to create an empty archive");
    private static final TextKey TAR_TRY_HELP = TextKey.of("jsc.install.gentoo_steps.tar_try_help",
            "Try 'tar --help' or 'tar --usage' for more information.");
    private static final TextKey TAR_CANNOT_OPEN = TextKey.of("jsc.install.gentoo_steps.tar_cannot_open",
            "tar: %s: Cannot open: No such file or directory");
    private static final TextKey TAR_NOT_RECOVERABLE = TextKey.of("jsc.install.gentoo_steps.tar_not_recoverable",
            "tar: Error is not recoverable: exiting now");
    private static final TextKey TAR_OVER_MEDIUM = TextKey.of("jsc.install.gentoo_steps.tar_over_medium",
            "tar: refusing to unpack a whole system over the live medium");
    private static final TextKey TAR_CD_FIRST = TextKey.of("jsc.install.gentoo_steps.tar_cd_first",
            "     (cd %s first, with the disk mounted there)");
    private static final TextKey EMERGE_NOT_YET = TextKey.of("jsc.install.gentoo_steps.emerge_not_yet",
            "emerge: command not found (the package manager is the new system's:");
    private static final TextKey CHROOT_FIRST = TextKey.of("jsc.install.gentoo_steps.chroot_first",
            "        chroot %s /bin/bash first)");
    private static final TextKey COULD_NOT_RESOLVE = TextKey.of("jsc.install.gentoo_steps.could_not_resolve",
            "!!! Could not resolve %s");
    private static final TextKey SYNC_FAILED = TextKey.of("jsc.install.gentoo_steps.sync_failed",
            "!!! Synchronization failed");
    private static final TextKey TREE_EMPTY = TextKey.of("jsc.install.gentoo_steps.tree_empty",
            "!!! The portage tree is empty. Run emerge-webrsync or emerge --sync first.");
    private static final TextKey FETCH_FAILED = TextKey.of("jsc.install.gentoo_steps.fetch_failed",
            "!!! Fetch failed");
    private static final TextKey NO_PACKAGES = TextKey.of("jsc.install.gentoo_steps.no_packages",
            "!!! No packages given.");
    private static final TextKey NO_EBUILDS = TextKey.of("jsc.install.gentoo_steps.no_ebuilds",
            "emerge: there are no ebuilds to satisfy \"%s\".");
    private static final TextKey ESELECT_NOT_FOUND = TextKey.of("jsc.install.gentoo_steps.eselect_not_found",
            "eselect: command not found");
    private static final TextKey NO_SYMLINK_TARGET = TextKey.of("jsc.install.gentoo_steps.no_symlink_target",
            "!!! Error: You didn't tell me what to set the symlink to");
    private static final TextKey EXITING = TextKey.of("jsc.install.gentoo_steps.exiting", "exiting");
    private static final TextKey NOT_VALID = TextKey.of("jsc.install.gentoo_steps.not_valid",
            "!!! Error: Target \"%s\" doesn't appear to be valid!");
    private static final TextKey NO_SOURCES = TextKey.of("jsc.install.gentoo_steps.no_sources",
            "!!! Error: No kernel sources found in /usr/src");
    private static final TextKey KERNEL_TARGETS = TextKey.of("jsc.install.gentoo_steps.kernel_targets",
            "Available kernel symlink targets:");
    private static final TextKey NO_NEWS = TextKey.of("jsc.install.gentoo_steps.no_news", "No news is good news.");
    private static final TextKey CANNOT_LOAD_MODULE = TextKey.of("jsc.install.gentoo_steps.cannot_load_module",
            "!!! Error: Can't load module %s");
    private static final TextKey NO_MAKEFILE = TextKey.of("jsc.install.gentoo_steps.no_makefile",
            "make: *** No targets specified and no makefile found.  Stop.");
    private static final TextKey FROM_ITS_SOURCES = TextKey.of("jsc.install.gentoo_steps.from_its_sources",
            "      (the kernel is built from its sources: cd %s)");
    private static final TextKey NO_VMLINUX = TextKey.of("jsc.install.gentoo_steps.no_vmlinux",
            "make: *** No rule to make target 'vmlinux'. Build the kernel first: make");
    private static final TextKey NO_RULE = TextKey.of("jsc.install.gentoo_steps.no_rule",
            "make: *** No rule to make target. Stop.");
    private static final TextKey KERNEL_NOT_YET = TextKey.of("jsc.install.gentoo_steps.kernel_not_yet",
            "%s: command not found (it is the new system's: chroot %s /bin/bash first)");
    private static final TextKey GENKERNEL_NO_SOURCES = TextKey.of("jsc.install.gentoo_steps.genkernel_no_sources",
            "* ERROR: no kernel sources found in /usr/src");
    private static final TextKey SOURCES_FIRST = TextKey.of("jsc.install.gentoo_steps.sources_first",
            "         (emerge sys-kernel/gentoo-sources first)");

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
            return LiveTurn.refused(WGET_MISSING_URL.text(), WGET_USAGE.text());
        }
        final WorldStamp stamp = WorldStamp.of(env.dayTime());
        if (!url.startsWith(FetchVoice.MIRROR) || !env.mirror()) {
            /* There is one network in this world, the one the player built, and one thing on it to fetch from. */
            return LiveTurn.running(FetchVoice.unreachable(url, stamp));
        }
        if (!url.contains("stage3")) {
            /* The date line, the protocol's name and the status the server sent are data; the rest is wget's. */
            return LiveTurn.refused(Text.literal("--" + stamp.dated() + "--  " + url),
                    REQUEST_SENT.with("Mirror", Text.literal("404 Not Found")),
                    FETCH_ERROR.with(404, Text.literal("Not Found")));
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
            return LiveTurn.refused(TAR_EMPTY.text(), TAR_TRY_HELP.text());
        }
        final boolean here = this.files.isFile(this.files.resolve(STAGE3));
        if (!here) {
            return LiveTurn.refused(TAR_CANNOT_OPEN.with(STAGE3), TAR_NOT_RECOVERABLE.text());
        }
        final boolean overTheDisk = this.files.resolve("").equals(this.files.root())
                || line.contains("-C " + this.files.root()) || line.contains("-C" + this.files.root());
        if (!this.disks.mounted() || !overTheDisk) {
            /*
             * Unpacked anywhere else it would land on the medium itself, which is memory and is gone at the
             * next restart. The real one would do it without a word; this one says why it will not.
             */
            return LiveTurn.refused(TAR_OVER_MEDIUM.text(), TAR_CD_FIRST.with(this.files.root()));
        }
        final boolean naming = parts.length > 1 && parts[1].replace("-", "").contains("v");
        final LiveInstallState.Device disk = env.find(LiveDisks.diskOf(this.disks.rootDevice()));
        return LiveTurn.running(ArchiveVoice.unpack(naming,
                LiveTimes.write(STAGE3_MB * 4, disk == null ? 1 : disk.speed()), this::laidOut));
    }

    /** The package manager, in every form the installation uses it in. */
    LiveTurn emerge(final String[] parts, final String line, final LiveInstallState.Env env) {
        if (!this.files.inside()) {
            return LiveTurn.refused(EMERGE_NOT_YET.text(), CHROOT_FIRST.with(this.files.root()));
        }
        final WorldStamp stamp = WorldStamp.of(env.dayTime());
        if (parts[0].equals("emerge-webrsync") || line.contains("--sync")) {
            if (!env.mirror()) {
                return LiveTurn.refused(COULD_NOT_RESOLVE.with(FetchVoice.MIRROR), SYNC_FAILED.text());
            }
            final Runnable synced = () -> this.progress.synced = true;
            return LiveTurn.running(parts[0].equals("emerge-webrsync")
                    ? PortageVoices.webrsync(TREE_MB, LiveTimes.fetch(TREE_MB, env),
                            LiveTimes.write(TREE_MB * 8, 4), TREE_FILES, stamp, synced)
                    : PortageVoices.sync(LiveTimes.fetch(TREE_MB / 4.0, env), TREE_FILES, synced));
        }
        if (!this.progress.synced) {
            return LiveTurn.refused(TREE_EMPTY.text());
        }
        if (!env.mirror()) {
            return LiveTurn.refused(COULD_NOT_RESOLVE.with(FetchVoice.MIRROR), FETCH_FAILED.text());
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
            return LiveTurn.refused(NO_PACKAGES.text());
        }
        // Looked up before anything is built, so a name the tree does not have stops the merge before it starts.
        final List<MirrorPackage> programs = new ArrayList<>();
        for (final String target : targets) {
            if (ofTheInstallItself(target)) {
                continue;
            }
            final MirrorPackage found = env.shelf().find(target, true);
            if (found == null) {
                return LiveTurn.refused(Text.EMPTY, NO_EBUILDS.with(target), Text.EMPTY);
            }
            programs.add(found);
        }
        final int jobs = LiveTimes.jobs(this.makeJobs(), env);
        return LiveTurn.running(PortageVoices.emerge(this.merges(targets, programs, jobs, env), ask, jobs,
                () -> this.merged(targets, programs)));
    }

    /** Whether that target is one of the installation's own steps rather than a program the Mirror keeps. */
    private static boolean ofTheInstallItself(final String target) {
        return target.equals("@world") || target.contains("gentoo-sources") || target.endsWith("grub");
    }

    /** The chooser: profiles, kernels, and the news nobody reads. */
    LiveTurn eselect(final String[] parts) {
        if (!this.files.inside()) {
            return LiveTurn.refused(ESELECT_NOT_FOUND.text());
        }
        final String what = parts.length > 1 ? parts[1] : "";
        final String action = parts.length > 2 ? parts[2] : "list";
        if (what.equals("profile")) {
            if (!action.equals("set")) {
                return LiveTurn.said(PortageVoices.profiles(this.progress.profileInForce()));
            }
            if (parts.length < 4) {
                return LiveTurn.refused(NO_SYMLINK_TARGET.text(), EXITING.text());
            }
            final int chosen = PortageVoices.profileOf(parts[3]);
            if (chosen == 0) {
                return LiveTurn.refused(NOT_VALID.with(parts[3]), EXITING.text());
            }
            this.progress.profile = chosen;
            return LiveTurn.silent();
        }
        if (what.equals("kernel")) {
            if (!this.progress.sources) {
                return LiveTurn.refused(NO_SOURCES.text());
            }
            if (action.equals("set")) {
                this.progress.kernelChosen = true;
                this.files.makeDir(this.files.inNewSystem(SOURCES_DIR));
                return LiveTurn.silent();
            }
            return LiveTurn.said(KERNEL_TARGETS.text(),
                    Text.literal("  [1]   linux-" + KERNEL + "-gentoo" + (this.progress.kernelChosen ? " *" : "")));
        }
        if (what.equals("news")) {
            return LiveTurn.said(NO_NEWS.text());
        }
        return LiveTurn.refused(CANNOT_LOAD_MODULE.with(what), EXITING.text());
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
            return LiveTurn.refused(NO_MAKEFILE.text(), FROM_ITS_SOURCES.with(SOURCES_DIR));
        }
        final boolean builds = line.matches("(?s)^make(\\s+-j\\s*\\d*)?\\s*(&&.*)?$");
        final boolean installs = line.contains("make " + INSTALL_TARGET) || line.endsWith(" " + INSTALL_TARGET);
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
            return LiveTurn.refused(NO_VMLINUX.text());
        }
        // What the kernel's build prints of the two quick steps is a build's technical line, which is data.
        if (line.contains("modules_install")) {
            return LiveTurn.said(Text.literal("  DEPMOD  /lib/modules/" + KERNEL + "-gentoo"));
        }
        if (installs) {
            this.kernelInstalled();
            return LiveTurn.said(Text.literal("  INSTALL /boot"));
        }
        return LiveTurn.refused(NO_RULE.text());
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
            return LiveTurn.refused(KERNEL_NOT_YET.with(tool, this.files.root()));
        }
        if (!this.progress.sources) {
            return LiveTurn.refused(GENKERNEL_NO_SOURCES.text(), SOURCES_FIRST.text());
        }
        return null;
    }

    private void kernelInstalled() {
        this.progress.kernelBuilt = true;
        this.progress.initramfs = true;
        this.files.write(this.files.inNewSystem("/boot/vmlinuz-" + KERNEL + "-gentoo-x86_64"), "");
    }

    /** What the packages asked for come to, each with how long this machine takes to fetch and to build it. */
    private List<PortageVoices.Merge> merges(final List<String> targets, final List<MirrorPackage> programs,
                                             final int jobs, final LiveInstallState.Env env) {
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
                        "", flags("-build", "-symlink"), LiveTimes.fetch(140, env), 0));
            } else if (target.endsWith("grub")) {
                out.add(new PortageVoices.Merge("sys-boot/grub", "2.12-r5", "", "grub-2.12.tar.xz", 6.4,
                        flags("fonts", "nls", "themes"), flags("-device-mapper", "-doc"), LiveTimes.fetch(6.4, env),
                        LiveTimes.build(6.4 * 3, jobs, env)));
            }
        }
        /* A program of the Mirror's is everything it is built from, the toolkit first and the program last. */
        for (final MirrorPackage program : programs) {
            for (final MirrorPackage.Piece piece : program.pieces()) {
                out.add(new PortageVoices.Merge(piece.atom(), piece.version(), "", piece.archive(), piece.sizeMb(),
                        piece.flagsOn(), piece.flagsOff(),
                        piece.archive().isEmpty() ? 0 : LiveTimes.fetch(piece.sizeMb(), env),
                        piece.compiles() ? LiveTimes.build(piece.sizeMb(), jobs, env) : 0));
            }
        }
        return out;
    }

    /** What having those packages merged means to the installation. */
    private void merged(final List<String> targets, final List<MirrorPackage> programs) {
        for (final String target : targets) {
            if (target.equals("@world")) {
                this.progress.worldUpdated = true;
            } else if (target.contains("gentoo-sources")) {
                this.progress.sources = true;
                this.files.makeDir(this.files.inNewSystem("/usr/src/linux-" + KERNEL + "-gentoo"));
            } else if (target.endsWith("grub")) {
                this.progress.grubPackage = true;
            }
        }
        /* Kept by what the program is and not by what was typed, so it is found again when the system comes up. */
        for (final MirrorPackage program : programs) {
            this.progress.ask(program.id());
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
        this.files.write(this.files.inNewSystem(MakeOpts.PATH), MAKE_CONF.english());
        this.files.write(this.files.inNewSystem("/etc/fstab"), FSTAB.english());
    }

    /** A package's flags as the package manager lists them, a word each: names of switches, which are data. */
    private static String flags(final String... each) {
        return String.join(" ", each);
    }
}
