/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the Arch package manager says, from the databases to the last hook, and the image builder it runs.
 *
 * <p>Anybody who has installed this distribution knows this tool by its shape: the dependency resolution, the
 * list of what is about to be installed with its versions, the two totals, the question, a bar per package
 * that really fills while it is fetched, the five checks each counting up to the total, then every package
 * installed by name, then the hooks, one of which builds the boot images and says so at length. The figures
 * are added up from the list rather than written down, so the totals are the sum of the list and the counters
 * count it.
 *
 * <p>The manager's and the image builder's sentences are the player's language. The packages, their sizes, and
 * what each hook says it does (which the hook's own file declares) are the same in every language.
 */
@TextHolder
public final class PacmanVoices {

    /** How big a package has to be before installing it is something to watch rather than a line going by. */
    private static final double BIG_MIB = 30.0;

    /** The column the two totals' figures start at, so they stand one over the other. */
    private static final int FIGURES_AT = 23;

    private static final TextKey CHECK_KEYS =
            TextKey.of("jsc.install.pacman_voices.check_keys", "checking keys in keyring");
    private static final TextKey CHECK_INTEGRITY =
            TextKey.of("jsc.install.pacman_voices.check_integrity", "checking package integrity");
    private static final TextKey CHECK_FILES =
            TextKey.of("jsc.install.pacman_voices.check_files", "loading package files");
    private static final TextKey CHECK_CONFLICTS =
            TextKey.of("jsc.install.pacman_voices.check_conflicts", "checking for file conflicts");
    private static final TextKey CHECK_SPACE =
            TextKey.of("jsc.install.pacman_voices.check_space", "checking available disk space");

    private static final TextKey[] CHECKS = {CHECK_KEYS, CHECK_INTEGRITY, CHECK_FILES, CHECK_CONFLICTS, CHECK_SPACE};

    /* What each hook says it does is written in the hook's own file, so it reads the same in every language. */
    private static final Text[] HOOKS = {Text.literal("Creating system user accounts..."),
        Text.literal("Updating journal message catalog..."), Text.literal("Reloading system manager configuration..."),
        Text.literal("Updating udev hardware database..."), Text.literal("Applying kernel sysctl settings..."),
        Text.literal("Creating temporary files..."), Text.literal("Reloading device manager configuration..."),
        Text.literal("Arming ConditionNeedsUpdate..."), Text.literal("Updating module dependencies..."),
        Text.literal("Updating linux initcpios..."), Text.literal("Reloading system bus configuration...")};

    /** The hook that builds the boot images, which is the one that has something of its own to say. */
    private static final Text IMAGES_HOOK = Text.literal("Updating linux initcpios...");

    private static final Text[] LATER_HOOKS = {Text.literal("Arming ConditionNeedsUpdate..."),
        Text.literal("Updating the info directory file...")};

    private static final String[] DEFAULT_HOOKS = {"base", "udev", "autodetect", "microcode", "modconf", "kms",
        "keyboard", "keymap", "consolefont", "block", "filesystems", "fsck"};

    /** The presets the images are built from, and the configuration they are built with. */
    private static final String PRESET = "/etc/mkinitcpio.d/linux.preset";
    private static final String CONFIGURATION = "/etc/mkinitcpio.conf";

    /**
     * What a base installation resolves to, which is not what was asked for.
     *
     * <p>Asking for the base group, a kernel and its firmware resolves to a list, and it is the list that is
     * printed, totalled and counted. Each is named and versioned the way the real one names it.
     */
    private static final List<Package> BASE = packages("iana-etc-20240814-1:0.4", "filesystem-2024.04.07-1:0.1",
            "linux-api-namespaces-6.10-1:1.4", "tzdata-2024b-2:0.4", "libsigma-2.40+r16-5:6.9",
            "scc-libs-14.2.1-3:37.4", "ncurses-6.5-3:1.1", "readline-8.2.013-1:0.3", "bash-5.2.037-1:1.8",
            "acl-2.3.2-1:0.1", "attr-2.5.2-1:0.1", "gmp-6.3.0-2:0.4", "zlib-1:1.3.1-2:0.1", "sqlite-3.46.1-1:1.9",
            "util-linux-libs-2.40.2-1:0.5", "e2fsprogs-1.47.1-4:1.2", "openssl-3.4.0-1:5.0", "libcap-2.71-1:0.8",
            "coreutils-9.5-2:2.9", "bzip2-1.0.8-6:0.1", "xz-5.6.3-1:0.8", "zstd-1.5.6-1:0.5", "file-5.45-1:0.4",
            "findutils-4.10.0-2:0.5", "gawk-5.3.1-1:1.4", "gettext-0.22.5-2:2.6", "grep-3.11-1:0.3",
            "gzip-1.13-4:0.1", "iproute2-6.11.0-1:1.2", "iputils-20240905-1:0.1", "licenses-20240728-1:0.1",
            "pacman-7.0.0.r3-1:1.0", "pciutils-3.13.0-2:0.1", "procps-ng-4.0.4-3:0.8", "psmisc-23.7-1:0.1",
            "sed-4.9-3:0.3", "shadow-4.16.0-1:1.2", "systemd-libs-256.7-1:1.2", "systemd-256.7-1:8.2",
            "systemd-sysvcompat-256.7-1:0.1", "tar-1.35-2:0.8", "util-linux-2.40.2-1:1.2", "kmod-33-3:0.1",
            "mkinitcpio-busybox-1.36.1-1:0.2", "mkinitcpio-40-2:0.1", "linux-6.11.5.arch1-1:139.6",
            "linux-firmware-20241017.22a6c7dc-1:130.9", "base-3-2:0.1");

    private static final TextKey CREATING_ROOT =
            TextKey.of("jsc.install.pacman_voices.creating_root", "Creating install root at %s");
    private static final TextKey INSTALLING_TO =
            TextKey.of("jsc.install.pacman_voices.installing_to", "Installing packages to %s");
    private static final TextKey SYNCHRONIZING =
            TextKey.of("jsc.install.pacman_voices.synchronizing", "Synchronizing package databases...");
    private static final TextKey RESOLVING =
            TextKey.of("jsc.install.pacman_voices.resolving", "resolving dependencies...");
    private static final TextKey CONFLICTS =
            TextKey.of("jsc.install.pacman_voices.conflicts", "looking for conflicting packages...");
    private static final TextKey PACKAGES = TextKey.of("jsc.install.pacman_voices.packages", "Packages (%s)");
    private static final TextKey DOWNLOAD_SIZE =
            TextKey.of("jsc.install.pacman_voices.download_size", "Total Download Size:");
    private static final TextKey INSTALLED_SIZE =
            TextKey.of("jsc.install.pacman_voices.installed_size", "Total Installed Size:");
    private static final TextKey PROCEED =
            TextKey.of("jsc.install.pacman_voices.proceed", "Proceed with installation? [Y/n]");
    private static final TextKey RETRIEVING =
            TextKey.of("jsc.install.pacman_voices.retrieving", "Retrieving packages...");
    private static final TextKey PROCESSING =
            TextKey.of("jsc.install.pacman_voices.processing", "Processing package changes...");
    private static final TextKey INSTALLING = TextKey.of("jsc.install.pacman_voices.installing", "installing %s");
    private static final TextKey RUNNING_HOOKS =
            TextKey.of("jsc.install.pacman_voices.running_hooks", "Running post-transaction hooks...");
    private static final TextKey BUILDING_IMAGE =
            TextKey.of("jsc.install.pacman_voices.building_image", "Building image from preset: %s: '%s'");
    private static final TextKey USING_DEFAULT =
            TextKey.of("jsc.install.pacman_voices.using_default", "Using default configuration file: '%s'");
    private static final TextKey STARTING_BUILD =
            TextKey.of("jsc.install.pacman_voices.starting_build", "Starting build: '%s'");
    private static final TextKey RUNNING_HOOK =
            TextKey.of("jsc.install.pacman_voices.running_hook", "Running build hook: [%s]");
    private static final TextKey GENERATING_DEPENDENCIES =
            TextKey.of("jsc.install.pacman_voices.generating_dependencies", "Generating module dependencies");
    private static final TextKey CREATING_IMAGE = TextKey.of("jsc.install.pacman_voices.creating_image",
            "Creating zstd-compressed initcpio image: '%s'");
    private static final TextKey EARLY_IMAGE = TextKey.of("jsc.install.pacman_voices.early_image",
            "Early uncompressed CPIO image generation successful");
    private static final TextKey IMAGE_GENERATED =
            TextKey.of("jsc.install.pacman_voices.image_generated", "Initcpio image generation successful");

    private PacmanVoices() {
    }

    /**
     * One package as the tool knows it.
     *
     * @param file its name and version as one word, which is what the list and the retrieval name it by
     * @param mib  what it costs to fetch
     */
    public record Package(String file, double mib) {

        /** The name alone, which is what the installation names it by. */
        String name() {
            return this.file.replaceAll("-[^-]+-[^-]+$", "");
        }
    }

    /** How much a base installation fetches, in mebibytes, which is what its time is worked out from. */
    public static double baseMib() {
        return total(BASE);
    }

    /**
     * Installing the base system into a mounted root.
     *
     * @param fetchTicks  how long the machine's connection takes over the whole download
     * @param writeTicks  how long the machine's disk takes to write it all out
     * @param release     the kernel the boot images are built for
     * @param installed   what having it installed means to the machine, done at the end and not before
     */
    public static TtyScript pacstrap(final String root, final int fetchTicks, final int writeTicks,
                                     final String release, final Runnable installed) {
        final double rate = total(BASE) / (Math.max(1, fetchTicks) / 20.0);
        final TtyScript.Builder script = TtyScript.script()
                .say(heading("==>", CREATING_ROOT.with(root)))
                .pause(6)
                .say(heading("==>", INSTALLING_TO.with(root)))
                .say(heading("::", SYNCHRONIZING.text()))
                .redraw(14, p -> Bars.retrieve("core", p, 0.1, rate))
                .redraw(28, p -> Bars.retrieve("extra", p, 7.7, rate));
        transaction(script, BASE, false, fetchTicks, writeTicks, HOOKS, release);
        return script.effect(installed).done();
    }

    /** Installing named packages inside the new system, where the question is really asked. */
    public static TtyScript install(final List<Package> packages, final int fetchTicks, final int writeTicks,
                                    final Runnable installed) {
        final TtyScript.Builder script = TtyScript.script();
        transaction(script, packages, true, fetchTicks, writeTicks, LATER_HOOKS, "");
        return script.effect(installed).done();
    }

    /** Building both boot images from the presets, every hook named as it runs. */
    public static TtyScript mkinitcpio(final String release, final int ticks, final Runnable built) {
        final TtyScript.Builder script = TtyScript.script();
        images(script, release, ticks);
        return script.effect(built).done();
    }

    /** The whole of one transaction: resolve, list, total, ask, retrieve, check, install, run the hooks. */
    private static void transaction(final TtyScript.Builder script, final List<Package> packages,
                                    final boolean ask, final int fetchTicks, final int writeTicks,
                                    final Text[] hooks, final String release) {
        final double total = total(packages);
        final double rate = total / (Math.max(1, fetchTicks) / 20.0);
        final int count = packages.size();
        script.say(RESOLVING)
                .pause(12)
                .say(CONFLICTS)
                .pause(8)
                .say("")
                .say(Tint.line(Tint.bright(PACKAGES.with(count)), " ", named(packages)))
                .say("")
                .say(sized(DOWNLOAD_SIZE, total))
                .say(sized(INSTALLED_SIZE, total * 4.1))
                .say("");
        final CliLine proceed = Tint.line(Tint.blue("::"), " ", Tint.bright(PROCEED.text()), " ");
        if (ask) {
            script.ask(proceed, answer -> answer.toLowerCase(Locale.ROOT).startsWith("n")
                    ? TtyScript.script().stop().done() : null);
        } else {
            script.say(proceed);
        }
        script.say(heading("::", RETRIEVING.text()));
        for (final Package one : packages) {
            /* Each takes the share of the download its size is of the whole, so the big ones are the wait. */
            final int ticks = (int) Math.round(fetchTicks * one.mib() / total);
            final String file = one.file() + "-x86_64";
            if (ticks < 4) {
                script.say(Bars.retrieve(file, 1.0, one.mib(), rate)).pause(Math.max(1, ticks));
            } else {
                script.redraw(ticks, p -> Bars.retrieve(file, p, one.mib(), rate));
            }
        }
        for (final TextKey check : CHECKS) {
            script.redraw(check == CHECK_INTEGRITY ? 30 : 10,
                    p -> Bars.counted(Math.max(1, (int) Math.round(p * count)), count, check.text(), p));
        }
        script.say(heading("::", PROCESSING.text()));
        for (int i = 0; i < count; i++) {
            final Package one = packages.get(i);
            final int at = i + 1;
            final int ticks = (int) Math.round(writeTicks * one.mib() / total);
            final Text installing = INSTALLING.with(one.name());
            if (one.mib() >= BIG_MIB && ticks >= 4) {
                script.redraw(ticks, p -> Bars.counted(at, count, installing, p));
            } else {
                script.say(Bars.counted(at, count, installing, 1.0)).pause(Math.max(1, ticks));
            }
        }
        script.say(heading("::", RUNNING_HOOKS.text()));
        final int width = String.valueOf(hooks.length).length();
        for (int i = 0; i < hooks.length; i++) {
            final String counter = String.format(Locale.ROOT, "(%" + width + "d/%d) ", i + 1, hooks.length);
            script.say(Tint.line(counter, hooks[i])).pause(5);
            if (IMAGES_HOOK.equals(hooks[i]) && !release.isEmpty()) {
                images(script, release, 180);
            }
        }
    }

    /** One build per preset: the ordinary image, and the one that carries every module in case. */
    private static void images(final TtyScript.Builder script, final String release, final int ticks) {
        for (final boolean fallback : new boolean[]{false, true}) {
            final String file = fallback ? "/boot/initramfs-linux-fallback.img" : "/boot/initramfs-linux.img";
            final String preset = fallback ? "fallback" : "default";
            /* The builder's own arguments, which read the same in every language. */
            final Text arguments = Text.literal("-k /boot/vmlinuz-linux -g " + file
                    + (fallback ? " -S autodetect" : ""));
            script.say(heading("==>", BUILDING_IMAGE.with(PRESET, preset)))
                    .say(heading("==>", USING_DEFAULT.with(CONFIGURATION)))
                    .say(sub(arguments))
                    .say(heading("==>", STARTING_BUILD.with(release)));
            for (final String hook : DEFAULT_HOOKS) {
                /* The fallback leaves out the one hook that keeps only what this machine needs. */
                if (fallback && hook.equals("autodetect")) {
                    continue;
                }
                script.say(sub(RUNNING_HOOK.with(hook)))
                        .pause(Math.max(1, ticks * (hook.equals("kms") || hook.equals("block") ? 5 : 2) / 100));
            }
            script.say(heading("==>", GENERATING_DEPENDENCIES.text()))
                    .pause(Math.max(1, ticks * 4 / 100))
                    .say(heading("==>", CREATING_IMAGE.with(file)))
                    .pause(Math.max(1, ticks * (fallback ? 20 : 11) / 100))
                    .say(sub(EARLY_IMAGE.text()))
                    .say(heading("==>", IMAGE_GENERATED.text()));
        }
    }

    private static CliLine heading(final String mark, final Text text) {
        return Tint.line(mark.equals("::") ? Tint.blue(mark) : Tint.green(mark), " ", Tint.bright(text));
    }

    private static CliLine sub(final Text text) {
        return Tint.line(Tint.blue("  ->"), " ", Tint.bright(text));
    }

    /** One of the two totals: its label, then its figure at the column both figures start at. */
    private static CliLine sized(final TextKey label, final double mib) {
        final String figure = String.format(Locale.ROOT, "%8.2f MiB", mib);
        return CliLine.of(Tint.bright(label.text()), CliSpan.pad(FIGURES_AT), CliSpan.plain(figure));
    }

    private static String named(final List<Package> packages) {
        final StringBuilder out = new StringBuilder();
        for (final Package one : packages) {
            if (!out.isEmpty()) {
                out.append("  ");
            }
            out.append(one.file());
        }
        return out.toString();
    }

    private static double total(final List<Package> packages) {
        double sum = 0;
        for (final Package one : packages) {
            sum += one.mib();
        }
        return sum;
    }

    /** A list written as words of {@code file:size}, which keeps fifty packages readable in one place. */
    private static List<Package> packages(final String... written) {
        final List<Package> out = new ArrayList<>();
        for (final String word : written) {
            final int colon = word.lastIndexOf(':');
            out.add(new Package(word.substring(0, colon), Double.parseDouble(word.substring(colon + 1))));
        }
        return List.copyOf(out);
    }
}
