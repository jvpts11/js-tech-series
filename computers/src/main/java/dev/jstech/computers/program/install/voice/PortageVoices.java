/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the source-based package manager says: syncing its tree, listing what it would merge, and merging.
 *
 * <p>This is the tool people mean when they say installing this distribution is something to watch. It tells
 * you what it would do and why before it does anything, asks, and then takes each package through its phases
 * out loud: fetched with a bar, checked, unpacked, patched, configured with a page of "checking for", compiled
 * with a line for every file, installed into an image and merged from there into the system, with the arrows
 * in green down the left the whole way. The installed system's merges are the same merges, because it is the
 * same tool.
 */
public final class PortageVoices {

    private static final String[] CHECKS = {"for a BSD-compatible install... /usr/bin/install -c",
        "whether build environment is sane... yes", "for a race-free mkdir -p... /bin/mkdir -p", "for gawk... gawk",
        "whether make sets $(MAKE)... yes", "build system type... x86_64-pc-linux-gnu",
        "host system type... x86_64-pc-linux-gnu", "for x86_64-pc-linux-gnu-gcc... x86_64-pc-linux-gnu-gcc",
        "whether the C compiler works... yes", "for C compiler default output file name... a.out",
        "whether we are cross compiling... no", "for suffix of object files... o",
        "whether the compiler supports GNU C... yes", "for stdio.h... yes", "for stdlib.h... yes",
        "for string.h... yes", "for unistd.h... yes", "for sys/types.h... yes", "for wchar.h... yes",
        "for size_t... yes", "for working alloca.h... yes", "for getopt_long... yes", "for snprintf... yes",
        "for strcasecmp... yes", "for pkg-config... /usr/bin/x86_64-pc-linux-gnu-pkg-config", "for ncursesw... yes",
        "whether NLS is requested... yes", "for msgfmt... /usr/bin/msgfmt",
        "for ld used by gcc... /usr/x86_64-pc-linux-gnu/bin/ld", "if the linker is GNU ld... yes",
        "for shared library run path origin... done"};

    private static final String[] SOURCES = ("alloc args buffer charset cmdline color config cursor display edit "
            + "error files global help history input keys main memory move nls options parse path prompt regex "
            + "screen search signal strutil syntax term text undo util winio").split(" ");

    private static final List<String> CONFIGURED = List.of("configure: creating ./config.status",
            "config.status: creating Makefile", "config.status: creating src/Makefile",
            "config.status: creating config.h", "config.status: executing depfiles commands");

    private PortageVoices() {
    }

    /**
     * One package about to be merged.
     *
     * @param atom       its category and name
     * @param was        the version it replaces, or empty when the package is new to the machine
     * @param archive    the file its source comes in
     * @param flagsOn    the flags it is built with
     * @param flagsOff   the flags it is built without, each with its minus
     * @param fetchTicks how long the machine's connection takes over its source
     * @param buildTicks how long the machine's processor takes to build it, or none for one that only unpacks
     */
    public record Merge(String atom, String version, String was, String archive, double sizeMb, String flagsOn,
                        String flagsOff, int fetchTicks, int buildTicks) {

        String full() {
            return this.atom + "-" + this.version;
        }

        String name() {
            return this.atom.substring(this.atom.indexOf('/') + 1);
        }

        String work() {
            return "/var/tmp/portage/" + this.full() + "/work";
        }
    }

    /** Fetching the whole package tree as one snapshot, checked and laid out. */
    public static TtyScript webrsync(final double sizeMb, final int fetchTicks, final int unpackTicks,
                                     final int files, final WorldStamp stamp, final Runnable synced) {
        final String snapshot = "gentoo-day" + stamp.day() + ".tar.xz";
        final double seconds = Math.max(1, fetchTicks) / 20.0;
        return TtyScript.script()
                .say(star("Latest snapshot date: day " + stamp.day()))
                .say(star(""))
                .say(star("Trying to retrieve the day " + stamp.day() + " snapshot from " + FetchVoice.MIRROR + " ..."))
                .pause(5)
                .say(star("Fetching file " + snapshot + ".md5sum ..."))
                .pause(4)
                .say(star("Fetching file " + snapshot + ".gpgsig ..."))
                .pause(4)
                .say(star("Fetching file " + snapshot + " ..."))
                .redraw(fetchTicks, p -> Bars.fetch(snapshot, p, sizeMb, sizeMb / seconds, seconds))
                .say(star("Checking digest ..."))
                .pause(8)
                .say(star("Checking signature ..."))
                .pause(10)
                .say("gpg: Good signature from \"Gentoo ebuild repository signing key\"")
                .say(star("Getting snapshot timestamp ..."))
                .pause(6)
                .say(star("Syncing local tree ..."))
                .pause(unpackTicks)
                .say("")
                .say(String.format(Locale.ROOT, "Number of files: %,d", files))
                .say(String.format(Locale.ROOT, "Number of created files: %,d", files - 1))
                .say("")
                .say(star("Cleaning up ..."))
                .pause(5)
                .effect(synced)
                .say("")
                .say(Tint.line(Tint.star(), Tint.yellow("IMPORTANT:"), " 14 news items need reading for repository"))
                .say(Tint.line(Tint.star(), "'gentoo'. Use ", Tint.green("eselect news read"), " to view new items."))
                .say("")
                .done();
    }

    /** Syncing an already-fetched tree against the Mirror, which is a different tool and sounds like one. */
    public static TtyScript sync(final int ticks, final int files, final Runnable synced) {
        return TtyScript.script()
                .say(Tint.line(Tint.arrows(), " Syncing repository 'gentoo' into '/var/db/repos/gentoo'..."))
                .say(star("Using keys from /usr/share/openpgp-keys/gentoo-release.asc"))
                .say(Bars.ok("Refreshing keys from keyserver ..."))
                .say(Tint.line(Tint.arrows(), " Starting rsync with rsync://mainframe/gentoo-portage"))
                .pause(ticks)
                .say(String.format(Locale.ROOT, "Number of files: %,d", files))
                .say("Number of created files: 0")
                .say(Tint.line(Tint.arrows(), " Checking server timestamp ..."))
                .pause(6)
                .effect(synced)
                .say(Tint.line(Tint.arrows(), " Syncing repository 'gentoo' ... done"))
                .say("")
                .say("Action: sync for repository 'gentoo', returned code = 0")
                .done();
    }

    /** The profiles the machine can be set to, with the star on the one in force. */
    public static List<CliLine> profiles(final int chosen) {
        final String[] names = {"default/linux/amd64/23.0 (stable)", "default/linux/amd64/23.0/systemd (stable)",
            "default/linux/amd64/23.0/desktop (stable)", "default/linux/amd64/23.0/desktop/gnome (stable)",
            "default/linux/amd64/23.0/desktop/plasma (stable)", "default/linux/amd64/23.0/no-multilib (stable)",
            "default/linux/amd64/23.0/hardened (stable)"};
        final List<CliLine> out = new ArrayList<>();
        out.add(Tint.line(Tint.green("Available profile symlink targets:")));
        for (int i = 0; i < names.length; i++) {
            out.add(Tint.line("  ", Tint.bright("[" + (i + 1) + "]"), "   " + names[i],
                    i + 1 == chosen ? Tint.line(" ", Tint.cyan("*")) : ""));
        }
        return out;
    }

    /**
     * Merging packages: the list of what would be merged, the question when asked for, and then each package
     * through every phase.
     *
     * @param ask    whether to stop and ask before merging anything
     * @param jobs   how many files are compiled at once
     * @param merged what having them merged means to the machine, done when the last one is and not before
     */
    public static TtyScript emerge(final List<Merge> merges, final boolean ask, final int jobs,
                                   final Runnable merged) {
        final TtyScript.Builder script = TtyScript.script();
        if (ask) {
            script.say("")
                    .say(Tint.line(Tint.green("These are the packages that would be merged, in order:")))
                    .say("");
        }
        script.redraw(20 + merges.size() * 4, p -> CliLine.plain("Calculating dependencies"
                + (p >= 1.0 ? "... done!" : ".".repeat(1 + (int) (p * 12) % 4))));
        if (ask) {
            listed(script, merges);
            script.ask(Tint.line(Tint.bright("Would you like to merge these packages?"), " [", Tint.green("Yes"),
                    "/", Tint.red("No"), "] "), answer -> answer.toLowerCase(Locale.ROOT).startsWith("n")
                    ? TtyScript.script().say("").say("Quitting.").say("").stop().done() : null);
        }
        script.say("").say(Tint.line(Tint.arrows(), " Verifying ebuild manifests"));
        for (int i = 0; i < merges.size(); i++) {
            one(script, merges.get(i), i + 1, merges.size(), jobs);
        }
        return script.effect(merged)
                .say("")
                .say(Tint.line(Tint.arrows(), String.format(Locale.ROOT,
                        " Jobs: %d of %d complete                 Load avg: %d.92, 2.84, 1.37",
                        merges.size(), merges.size(), Math.max(0, jobs - 1))))
                .say(Tint.line(Tint.arrows(), " Auto-cleaning packages..."))
                .say("")
                .say(Tint.line(Tint.arrows(), " No outdated packages were found on your system."))
                .say("")
                .say(star("GNU info directory index is up-to-date."))
                .done();
    }

    /** What generating the locales a file names prints, a job to each. */
    public static TtyScript localeGen(final List<String> locales, final int jobs, final int ticksEach,
                                      final Runnable generated) {
        final TtyScript.Builder script = TtyScript.script()
                .say(star("Generating " + locales.size() + " locales (this might take a while) with " + jobs
                        + (jobs == 1 ? " job" : " jobs")))
                .pause(8);
        for (int i = 0; i < locales.size(); i++) {
            script.pause(ticksEach)
                    .say(Bars.ok(" (" + (i + 1) + "/" + locales.size() + ") Generating " + locales.get(i) + " ..."));
        }
        return script.say(star("Generation complete"))
                .say(Bars.ok("Adding locales to archive ..."))
                .effect(generated)
                .done();
    }

    /** The list of what would be merged: new or an upgrade, the flags in their two colours, and the total. */
    private static void listed(final TtyScript.Builder script, final List<Merge> merges) {
        script.say(String.format(Locale.ROOT, "Dependency resolution took %.2f s (backtrack: 0/20).",
                1.9 + merges.size() * 0.77)).say("");
        double total = 0;
        int fresh = 0;
        for (final Merge merge : merges) {
            final boolean isNew = merge.was().isEmpty();
            fresh += isNew ? 1 : 0;
            total += merge.sizeMb();
            script.say(Tint.line("[", Tint.green("ebuild"), isNew ? "  " : "     ",
                    isNew ? Tint.green("N") : Tint.cyan("U"), isNew ? "     ] " : "  ] ", Tint.green(merge.full()),
                    isNew ? "" : Tint.line(" ", Tint.blue("[" + merge.was() + "]")),
                    "  USE=\"", Tint.red(merge.flagsOn()), merge.flagsOff().isEmpty() ? "" : " ",
                    Tint.blue(merge.flagsOff()), "\" ", String.format(Locale.ROOT, "%,d KiB",
                            Math.round(merge.sizeMb() * 1024))));
        }
        final int upgrades = merges.size() - fresh;
        script.say("")
                .say(Tint.line(Tint.bright("Total: " + merges.size() + (merges.size() == 1 ? " package" : " packages")),
                        " (" + (upgrades > 0 ? upgrades + (upgrades == 1 ? " upgrade" : " upgrades") : "")
                                + (upgrades > 0 && fresh > 0 ? ", " : "") + (fresh > 0 ? fresh + " new" : "")
                                + "), Size of downloads: " + String.format(Locale.ROOT, "%,d KiB",
                                Math.round(total * 1024))))
                .say("");
    }

    /** One package through every phase it has. */
    private static void one(final TtyScript.Builder script, final Merge merge, final int at, final int of,
                            final int jobs) {
        final String full = merge.full();
        final String unpackedAs = merge.archive().replaceAll("\\.tar\\..*$", "");
        final String source = merge.work() + "/" + unpackedAs;
        final String image = merge.work().replace("/work", "/image");
        final double seconds = Math.max(1, merge.fetchTicks()) / 20.0;
        script.say("")
                .say(Tint.line(Tint.arrows(), " Emerging (", Tint.yellow(String.valueOf(at)), " of ",
                        Tint.yellow(String.valueOf(of)), ") ", Tint.green(full), "::gentoo"))
                .pause(6)
                .say(Tint.line(Tint.arrows(), " Downloading '" + FetchVoice.MIRROR + "/distfiles/" + merge.archive()
                        + "'"))
                .redraw(merge.fetchTicks(), p -> Bars.fetch(merge.archive(), p, merge.sizeMb(),
                        merge.sizeMb() / seconds, seconds))
                .say(Bars.ok(merge.archive() + " BLAKE2B SHA512 size ;-) ..."))
                .pause(5)
                .say(Tint.line(Tint.arrows(), " Unpacking source..."))
                .say(Tint.line(Tint.arrows(), " Unpacking " + merge.archive() + " to " + merge.work()))
                .pause((int) Math.min(52, 8 + merge.sizeMb() * 0.28))
                .say(Tint.line(Tint.arrows(), " Source unpacked in " + merge.work()))
                .say(Tint.line(Tint.arrows(), " Preparing source in " + source + " ..."))
                .pause(7)
                .say(Bars.ok("Applying " + unpackedAs + "-gentoo-patchset.patch ..."))
                .pause(4)
                .say(Tint.line(Tint.arrows(), " Source prepared."));
        if (merge.buildTicks() > 0) {
            built(script, merge, source, jobs);
        }
        script.say("")
                .say(Tint.line(Tint.arrows(), " Install " + full + " into " + image))
                .pause(10)
                .say(Tint.line(Tint.arrows(), " Completed installing " + full + " into " + image))
                .say("")
                .say(star(String.format(Locale.ROOT, "Final size of build directory: %7d KiB",
                        Math.round(merge.sizeMb() * 9.4 * 1024))))
                .say(star(String.format(Locale.ROOT, "Final size of installed tree:  %7d KiB",
                        Math.round(merge.sizeMb() * 3.1 * 1024))))
                .say("")
                .say(Tint.line(Tint.arrows(), " Installing (", Tint.yellow(String.valueOf(at)), " of ",
                        Tint.yellow(String.valueOf(of)), ") ", Tint.green(full), "::gentoo"))
                .say(star("checking " + Math.round(80 + merge.sizeMb() * 22) + " files for package collisions"))
                .pause(9)
                .say(Tint.line(Tint.arrows(), " Merging " + full + " to /"));
        final List<CliLine> laid = List.of(CliLine.plain("--- /usr/"), CliLine.plain("--- /usr/bin/"),
                Tint.line(Tint.arrows(), " /usr/bin/" + merge.name()), CliLine.plain("--- /usr/share/"),
                CliLine.plain("--- /usr/share/doc/"),
                Tint.line(Tint.arrows(), " /usr/share/doc/" + merge.name() + "-" + merge.version() + "/"),
                Tint.line(Tint.arrows(), " /usr/share/doc/" + merge.name() + "-" + merge.version() + "/README.bz2"),
                CliLine.plain("--- /usr/share/man/"), CliLine.plain("--- /usr/share/man/man1/"),
                Tint.line(Tint.arrows(), " /usr/share/man/man1/" + merge.name() + ".1.bz2"));
        script.flood(8, laid.size(), laid::get)
                .say(Tint.line(Tint.arrows(), " " + full + " merged."))
                .say(Tint.line(Tint.arrows(), " Completed (", Tint.yellow(String.valueOf(at)), " of ",
                        Tint.yellow(String.valueOf(of)), ") ", Tint.green(full), "::gentoo"))
                .pause(6);
    }

    /** The configure page and the compile, which is where a build's time goes. */
    private static void built(final TtyScript.Builder script, final Merge merge, final String source,
                              final int jobs) {
        final int configuring = Math.max(10, merge.buildTicks() * 15 / 100);
        final int compiling = Math.max(20, merge.buildTicks() * 85 / 100);
        final int checks = Math.min(configuring * 2, 64);
        final int files = Math.min(compiling * TtyScript.MAX_LINES_PER_TICK, Math.max(40, compiling));
        final String library = merge.name().replaceAll("-.*", "");
        script.say(Tint.line(Tint.arrows(), " Configuring source in " + source + " ..."))
                .say(star("econf: updating config.sub with /usr/share/gnuconfig/config.sub"))
                .say("./configure --prefix=/usr --build=x86_64-pc-linux-gnu --host=x86_64-pc-linux-gnu "
                        + "--mandir=/usr/share/man --sysconfdir=/etc --localstatedir=/var/lib --libdir=/usr/lib64")
                .flood(configuring, checks, index -> CliLine.plain("checking " + CHECKS[index % CHECKS.length]))
                .sayAll(CONFIGURED.stream().map(CliLine::plain).toList())
                .say(Tint.line(Tint.arrows(), " Source configured."))
                .say(Tint.line(Tint.arrows(), " Compiling source in " + source + " ..."))
                .say("make -j" + jobs)
                .flood(compiling, files, index -> compiled(index, library))
                .say(Tint.line(Tint.arrows(), " Source compiled."))
                .say(Tint.line(Tint.arrows(), " Test phase [not enabled]: " + merge.full()));
    }

    /** The compiler's line for the file at a place in the build, worked out from the place alone. */
    private static CliLine compiled(final int index, final String library) {
        int hash = index * 0x9E3779B1;
        hash ^= hash >>> 15;
        final String file = SOURCES[Math.floorMod(hash, SOURCES.length)];
        return CliLine.plain(Math.floorMod(hash >>> 8, 6) != 0
                ? "x86_64-pc-linux-gnu-gcc -DHAVE_CONFIG_H -I. -O2 -pipe -march=native -c -o " + file + ".o "
                        + file + ".c"
                : "/bin/sh ./libtool --tag=CC --mode=compile x86_64-pc-linux-gnu-gcc -O2 -pipe -c -o lib" + library
                        + "_la-" + file + ".lo " + file + ".c");
    }

    private static CliLine star(final String text) {
        return Tint.line(Tint.star(), text);
    }
}
