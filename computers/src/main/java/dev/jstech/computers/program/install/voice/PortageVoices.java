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
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
 *
 * <p>The manager's own sentences (its phases, its summaries, its question) are the player's language. What the
 * build prints (the configure page, the compiler, the files merged), the fetcher's and the signature checker's
 * lines, and the names, versions and flags of the packages read the same in every language, as the real ones do.
 */
@TextHolder
public final class PortageVoices {

    /*
     * What a configure script asks of the machine it finds itself on. The software of this world is written in
     * Sigma and built by the Sigma Compiler Collection into the assembly a machine runs, so that is the compiler
     * it looks for, the listings are what it expects to come out, and what it checks for is namespaces, since
     * the language has those where another would have headers.
     */
    private static final Text[] CHECKS = {
        Text.literal("checking for a BSD-compatible install... /usr/bin/install -c"),
        Text.literal("checking whether build environment is sane... yes"),
        Text.literal("checking for a race-free mkdir -p... /bin/mkdir -p"), Text.literal("checking for gawk... gawk"),
        Text.literal("checking whether make sets $(MAKE)... yes"),
        Text.literal("checking build system type... x86_64-pc-linux-gnu"),
        Text.literal("checking host system type... x86_64-pc-linux-gnu"),
        Text.literal("checking for x86_64-pc-linux-gnu-scc... x86_64-pc-linux-gnu-scc"),
        Text.literal("checking whether the Sigma compiler works... yes"),
        Text.literal("checking for Sigma compiler default output file name... a.asm"),
        Text.literal("checking whether we are cross compiling... no"),
        Text.literal("checking for suffix of listing files... asm"),
        Text.literal("checking whether the compiler supports Sigma Sharp... yes"),
        Text.literal("checking for namespace System.IO... yes"),
        Text.literal("checking for namespace System.Collections... yes"),
        Text.literal("checking for namespace System.Threading... yes"),
        Text.literal("checking for namespace System.Network... yes"),
        Text.literal("checking for namespace System.Machine... yes"),
        Text.literal("checking for namespace System.Utils... yes"), Text.literal("checking for Program.Args... yes"),
        Text.literal("checking for working File.Read... yes"), Text.literal("checking for File.Write... yes"),
        Text.literal("checking for Program.Exit... yes"), Text.literal("checking for Program.OnMessage... yes"),
        Text.literal("checking for pkg-config... /usr/bin/x86_64-pc-linux-gnu-pkg-config"),
        Text.literal("checking for ncursesw... yes"), Text.literal("checking whether NLS is requested... yes"),
        Text.literal("checking for msgfmt... /usr/bin/msgfmt"),
        Text.literal("checking for ld used by scc... /usr/x86_64-pc-linux-gnu/bin/ld"),
        Text.literal("checking if the linker is GNU ld... yes"),
        Text.literal("checking for shared library run path origin... done")};

    private static final String[] SOURCES = {"alloc", "args", "buffer", "charset", "cmdline", "color", "config",
        "cursor", "display", "edit", "error", "files", "global", "help", "history", "input", "keys", "main", "memory",
        "move", "nls", "options", "parse", "path", "prompt", "regex", "screen", "search", "signal", "strutil",
        "syntax", "term", "text", "undo", "util", "winio"};

    /** The profiles a machine of this architecture can be set to, in the order the chooser numbers them. */
    private static final List<String> PROFILES = List.of("default/linux/vel64/23.0",
            "default/linux/vel64/23.0/systemd", "default/linux/vel64/23.0/desktop",
            "default/linux/vel64/23.0/desktop/gnome", "default/linux/vel64/23.0/desktop/plasma",
            "default/linux/vel64/23.0/no-multilib", "default/linux/vel64/23.0/hardened");

    private static final List<Text> CONFIGURED = List.of(Text.literal("configure: creating ./config.status"),
            Text.literal("config.status: creating Makefile"), Text.literal("config.status: creating src/Makefile"),
            Text.literal("config.status: creating config.sg"),
            Text.literal("config.status: executing depfiles commands"));

    /** The repository every package comes from, where it is kept, and what vouches for it. */
    private static final String REPOSITORY = "gentoo";
    private static final String REPOSITORY_PATH = "/var/db/repos/gentoo";
    private static final String SIGNING_KEYS = "/usr/share/openpgp-keys/gentoo-release.asc";
    private static final String RSYNC_SOURCE = "rsync://mainframe/gentoo-portage";

    /** The blank room that pushes what follows it to the right-hand edge of the glass. */
    private static final CliSpan TO_THE_EDGE =
            new CliSpan(Text.EMPTY, CliStyle.PLAIN, new CliSpan.Fill(Bars.COLUMNS, true, true));

    private static final TextKey LATEST_SNAPSHOT =
            TextKey.of("jsc.install.portage_voices.latest_snapshot", "Latest snapshot date: day %s");
    private static final TextKey TRYING = TextKey.of("jsc.install.portage_voices.trying",
            "Trying to retrieve the day %s snapshot from %s ...");
    private static final TextKey FETCHING_FILE =
            TextKey.of("jsc.install.portage_voices.fetching_file", "Fetching file %s ...");
    private static final TextKey CHECKING_DIGEST =
            TextKey.of("jsc.install.portage_voices.checking_digest", "Checking digest ...");
    private static final TextKey CHECKING_SIGNATURE =
            TextKey.of("jsc.install.portage_voices.checking_signature", "Checking signature ...");
    private static final TextKey GETTING_TIMESTAMP =
            TextKey.of("jsc.install.portage_voices.getting_timestamp", "Getting snapshot timestamp ...");
    private static final TextKey SYNCING_TREE =
            TextKey.of("jsc.install.portage_voices.syncing_tree", "Syncing local tree ...");
    private static final TextKey CLEANING_UP = TextKey.of("jsc.install.portage_voices.cleaning_up", "Cleaning up ...");
    private static final TextKey IMPORTANT = TextKey.of("jsc.install.portage_voices.important", "IMPORTANT:");
    private static final TextKey NEWS_WAITING =
            TextKey.of("jsc.install.portage_voices.news_waiting", "%s news items need reading for repository");
    /* The command that reads the news stands out in its own colour, so the line is split round it. */
    private static final TextKey NEWS_USE = TextKey.of("jsc.install.portage_voices.news_use", "'%s'. Use");
    private static final TextKey NEWS_VIEW =
            TextKey.of("jsc.install.portage_voices.news_view", "to view new items.");
    private static final TextKey SYNCING_REPOSITORY = TextKey.of("jsc.install.portage_voices.syncing_repository",
            "Syncing repository '%s' into '%s'...");
    private static final TextKey USING_KEYS =
            TextKey.of("jsc.install.portage_voices.using_keys", "Using keys from %s");
    private static final TextKey REFRESHING_KEYS =
            TextKey.of("jsc.install.portage_voices.refreshing_keys", "Refreshing keys from keyserver ...");
    private static final TextKey STARTING_RSYNC =
            TextKey.of("jsc.install.portage_voices.starting_rsync", "Starting rsync with %s");
    private static final TextKey CHECKING_TIMESTAMP =
            TextKey.of("jsc.install.portage_voices.checking_timestamp", "Checking server timestamp ...");
    private static final TextKey SYNCED_REPOSITORY =
            TextKey.of("jsc.install.portage_voices.synced_repository", "Syncing repository '%s' ... done");
    private static final TextKey ACTION_SYNC = TextKey.of("jsc.install.portage_voices.action_sync",
            "Action: sync for repository '%s', returned code = %s");
    private static final TextKey PROFILE_TARGETS =
            TextKey.of("jsc.install.portage_voices.profile_targets", "Available profile symlink targets:");
    private static final TextKey WOULD_BE_MERGED = TextKey.of("jsc.install.portage_voices.would_be_merged",
            "These are the packages that would be merged, in order:");
    private static final TextKey CALCULATING =
            TextKey.of("jsc.install.portage_voices.calculating", "Calculating dependencies");
    private static final TextKey CALCULATED =
            TextKey.of("jsc.install.portage_voices.calculated", "Calculating dependencies... done!");
    private static final TextKey MERGE_QUESTION =
            TextKey.of("jsc.install.portage_voices.merge_question", "Would you like to merge these packages?");
    private static final TextKey QUITTING = TextKey.of("jsc.install.portage_voices.quitting", "Quitting.");
    private static final TextKey VERIFYING_MANIFESTS =
            TextKey.of("jsc.install.portage_voices.verifying_manifests", "Verifying ebuild manifests");
    private static final TextKey JOBS = TextKey.of("jsc.install.portage_voices.jobs", "Jobs: %s of %s complete");
    private static final TextKey LOAD_AVERAGE = TextKey.of("jsc.install.portage_voices.load_average", "Load avg: %s");
    private static final TextKey AUTO_CLEANING =
            TextKey.of("jsc.install.portage_voices.auto_cleaning", "Auto-cleaning packages...");
    private static final TextKey NO_OUTDATED = TextKey.of("jsc.install.portage_voices.no_outdated",
            "No outdated packages were found on your system.");
    private static final TextKey INFO_INDEX =
            TextKey.of("jsc.install.portage_voices.info_index", "GNU info directory index is up-to-date.");
    private static final TextKey RESOLUTION_TOOK = TextKey.of("jsc.install.portage_voices.resolution_took",
            "Dependency resolution took %s s (backtrack: 0/20).");
    private static final TextKey TOTAL_ONE = TextKey.of("jsc.install.portage_voices.total_one", "Total: %s package");
    private static final TextKey TOTAL_MANY =
            TextKey.of("jsc.install.portage_voices.total_many", "Total: %s packages");
    private static final TextKey UPGRADES_ONE =
            TextKey.of("jsc.install.portage_voices.upgrades_one", "%s upgrade");
    private static final TextKey UPGRADES_MANY =
            TextKey.of("jsc.install.portage_voices.upgrades_many", "%s upgrades");
    private static final TextKey NEW_ONES = TextKey.of("jsc.install.portage_voices.new_ones", "%s new");
    /** The upgrades and the new packages, when there are both. */
    private static final TextKey BOTH = TextKey.of("jsc.install.portage_voices.both", "%s, %s");
    private static final TextKey DOWNLOADS =
            TextKey.of("jsc.install.portage_voices.downloads", "(%s), Size of downloads: %s");
    /* The count of a package's place in the merge stands out in its own colour, so its line is split round it. */
    private static final TextKey EMERGING = TextKey.of("jsc.install.portage_voices.emerging", "Emerging");
    private static final TextKey INSTALLING = TextKey.of("jsc.install.portage_voices.installing", "Installing");
    private static final TextKey COMPLETED = TextKey.of("jsc.install.portage_voices.completed", "Completed");
    private static final TextKey OF = TextKey.of("jsc.install.portage_voices.of", "of");
    private static final TextKey INSTALL_INTO =
            TextKey.of("jsc.install.portage_voices.install_into", "Install %s into %s");
    private static final TextKey COMPLETED_INSTALLING = TextKey.of(
            "jsc.install.portage_voices.completed_installing", "Completed installing %s into %s");
    private static final TextKey BUILD_DIRECTORY_SIZE = TextKey.of(
            "jsc.install.portage_voices.build_directory_size", "Final size of build directory: %s");
    private static final TextKey INSTALLED_TREE_SIZE = TextKey.of(
            "jsc.install.portage_voices.installed_tree_size", "Final size of installed tree:  %s");
    private static final TextKey COLLISIONS = TextKey.of("jsc.install.portage_voices.collisions",
            "checking %s files for package collisions");
    private static final TextKey MERGING = TextKey.of("jsc.install.portage_voices.merging", "Merging %s to %s");
    private static final TextKey MERGED = TextKey.of("jsc.install.portage_voices.merged", "%s merged.");
    private static final TextKey DOWNLOADING =
            TextKey.of("jsc.install.portage_voices.downloading", "Downloading '%s'");
    private static final TextKey UNPACKING_SOURCE =
            TextKey.of("jsc.install.portage_voices.unpacking_source", "Unpacking source...");
    private static final TextKey UNPACKING = TextKey.of("jsc.install.portage_voices.unpacking", "Unpacking %s to %s");
    private static final TextKey SOURCE_UNPACKED =
            TextKey.of("jsc.install.portage_voices.source_unpacked", "Source unpacked in %s");
    private static final TextKey PREPARING_SOURCE =
            TextKey.of("jsc.install.portage_voices.preparing_source", "Preparing source in %s ...");
    private static final TextKey APPLYING = TextKey.of("jsc.install.portage_voices.applying", "Applying %s ...");
    private static final TextKey SOURCE_PREPARED =
            TextKey.of("jsc.install.portage_voices.source_prepared", "Source prepared.");
    private static final TextKey CONFIGURING_SOURCE =
            TextKey.of("jsc.install.portage_voices.configuring_source", "Configuring source in %s ...");
    private static final TextKey SOURCE_CONFIGURED =
            TextKey.of("jsc.install.portage_voices.source_configured", "Source configured.");
    private static final TextKey COMPILING_SOURCE =
            TextKey.of("jsc.install.portage_voices.compiling_source", "Compiling source in %s ...");
    private static final TextKey SOURCE_COMPILED =
            TextKey.of("jsc.install.portage_voices.source_compiled", "Source compiled.");
    private static final TextKey TEST_PHASE =
            TextKey.of("jsc.install.portage_voices.test_phase", "Test phase [not enabled]: %s");

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
        final String digest = snapshot + ".md5sum";
        final String signature = snapshot + ".gpgsig";
        final double seconds = Math.max(1, fetchTicks) / 20.0;
        return TtyScript.script()
                .say(star(LATEST_SNAPSHOT.with(stamp.day())))
                .say(star(Text.EMPTY))
                .say(star(TRYING.with(stamp.day(), FetchVoice.MIRROR)))
                .pause(5)
                .say(star(FETCHING_FILE.with(digest)))
                .pause(4)
                .say(star(FETCHING_FILE.with(signature)))
                .pause(4)
                .say(star(FETCHING_FILE.with(snapshot)))
                .redraw(fetchTicks, p -> Bars.fetch(snapshot, p, sizeMb, sizeMb / seconds, seconds))
                .say(star(CHECKING_DIGEST.text()))
                .pause(8)
                .say(star(CHECKING_SIGNATURE.text()))
                .pause(10)
                .say(Text.literal("gpg: Good signature from \"Gentoo ebuild repository signing key\""))
                .say(star(GETTING_TIMESTAMP.text()))
                .pause(6)
                .say(star(SYNCING_TREE.text()))
                .pause(unpackTicks)
                .say("")
                .say(Text.literal(String.format(Locale.ROOT, "Number of files: %,d", files)))
                .say(Text.literal(String.format(Locale.ROOT, "Number of created files: %,d", files - 1)))
                .say("")
                .say(star(CLEANING_UP.text()))
                .pause(5)
                .effect(synced)
                .say("")
                .sayAll(newsWaiting(14))
                .say("")
                .done();
    }

    /** Syncing an already-fetched tree against the Mirror, which is a different tool and sounds like one. */
    public static TtyScript sync(final int ticks, final int files, final Runnable synced) {
        return TtyScript.script()
                .say(arrowed(SYNCING_REPOSITORY.with(REPOSITORY, REPOSITORY_PATH)))
                .say(star(USING_KEYS.with(SIGNING_KEYS)))
                .say(Bars.ok(REFRESHING_KEYS.text()))
                .say(arrowed(STARTING_RSYNC.with(RSYNC_SOURCE)))
                .pause(ticks)
                .say(Text.literal(String.format(Locale.ROOT, "Number of files: %,d", files)))
                .say(Text.literal("Number of created files: 0"))
                .say(arrowed(CHECKING_TIMESTAMP.text()))
                .pause(6)
                .effect(synced)
                .say(arrowed(SYNCED_REPOSITORY.with(REPOSITORY)))
                .say("")
                .say(ACTION_SYNC.with(REPOSITORY, 0))
                .done();
    }

    /** The profiles the machine can be set to, with the star on the one in force. */
    public static List<CliLine> profiles(final int chosen) {
        final List<CliLine> out = new ArrayList<>();
        out.add(Tint.line(Tint.green(PROFILE_TARGETS.text())));
        for (int i = 0; i < PROFILES.size(); i++) {
            out.add(Tint.line("  ", Tint.bright("[" + (i + 1) + "]"),
                    Text.literal("   " + PROFILES.get(i) + " (stable)"),
                    i + 1 == chosen ? Tint.line(" ", Tint.cyan("*")) : ""));
        }
        return out;
    }

    /**
     * The number of the profile somebody named, by that number or by its whole name, or zero when what they
     * typed is neither.
     */
    public static int profileOf(final String typed) {
        final int byName = PROFILES.indexOf(typed) + 1;
        if (byName > 0) {
            return byName;
        }
        try {
            final int number = Integer.parseInt(typed);
            return number >= 1 && number <= PROFILES.size() ? number : 0;
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
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
        return emerge(merges, ask, jobs, 0, merged);
    }

    /**
     * As {@link #emerge(List, boolean, int, Runnable)}, on a system that has news waiting to be read, which it
     * mentions before anything else every time it is run until somebody reads them.
     *
     * @param news how many items are waiting, none for a system with nothing to say
     */
    public static TtyScript emerge(final List<Merge> merges, final boolean ask, final int jobs, final int news,
                                   final Runnable merged) {
        final TtyScript.Builder script = TtyScript.script();
        if (news > 0) {
            script.say("").sayAll(newsWaiting(news));
        }
        if (ask) {
            script.say("")
                    .say(Tint.line(Tint.green(WOULD_BE_MERGED.text())))
                    .say("");
        }
        script.redraw(20 + merges.size() * 4, p -> p >= 1.0 ? CliLine.plain(CALCULATED.text())
                : Tint.line(CALCULATING, ".".repeat(1 + (int) (p * 12) % 4)));
        if (ask) {
            listed(script, merges);
            /* The answers are the words the tool takes typed, which it takes in these words in every language. */
            script.ask(Tint.line(Tint.bright(MERGE_QUESTION.text()), " [", Tint.green(Text.literal("Yes")), "/",
                    Tint.red(Text.literal("No")), "] "), answer -> answer.toLowerCase(Locale.ROOT).startsWith("n")
                    ? TtyScript.script().say("").say(QUITTING).say("").stop().done() : null);
        }
        script.say("").say(arrowed(VERIFYING_MANIFESTS.text()));
        for (int i = 0; i < merges.size(); i++) {
            one(script, merges.get(i), i + 1, merges.size(), jobs);
        }
        return script.effect(merged)
                .say("")
                .say(jobsLine(merges.size(), jobs))
                .say(arrowed(AUTO_CLEANING.text()))
                .say("")
                .say(arrowed(NO_OUTDATED.text()))
                .say("")
                .say(star(INFO_INDEX.text()))
                .done();
    }

    /**
     * The closing count of jobs with the load beside it, the load pushed to the right-hand edge of the glass
     * the way the real one pushes it to the edge of the terminal it finds itself in.
     */
    private static CliLine jobsLine(final int merged, final int jobs) {
        final String load = String.format(Locale.ROOT, "%d.92, 2.84, 1.37", Math.max(0, jobs - 1));
        return Tint.line(Tint.arrows(), " ", JOBS.with(merged, merged), CliLine.of(TO_THE_EDGE),
                LOAD_AVERAGE.with(load));
    }

    /** The list of what would be merged: new or an upgrade, the flags in their two colours, and the total. */
    private static void listed(final TtyScript.Builder script, final List<Merge> merges) {
        final String took = String.format(Locale.ROOT, "%.2f", 1.9 + merges.size() * 0.77);
        script.say(RESOLUTION_TOOK.with(took)).say("");
        double total = 0;
        int fresh = 0;
        for (final Merge merge : merges) {
            fresh += merge.was().isEmpty() ? 1 : 0;
            total += merge.sizeMb();
            script.say(ebuild(merge));
        }
        final int upgrades = merges.size() - fresh;
        final Text packages = merges.size() == 1 ? TOTAL_ONE.with(1) : TOTAL_MANY.with(merges.size());
        final Text upgraded = upgrades == 1 ? UPGRADES_ONE.with(1) : UPGRADES_MANY.with(upgrades);
        final Text added = NEW_ONES.with(fresh);
        final Text counts = upgrades > 0 && fresh > 0 ? BOTH.with(upgraded, added)
                : upgrades > 0 ? upgraded : fresh > 0 ? added : Text.EMPTY;
        final String downloads = String.format(Locale.ROOT, "%,d KiB", Math.round(total * 1024));
        script.say("")
                .say(Tint.line(Tint.bright(packages), " ", DOWNLOADS.with(counts, downloads)))
                .say("");
    }

    /** One package in that list, the way the real one sets it out, which reads the same in every language. */
    private static CliLine ebuild(final Merge merge) {
        final boolean isNew = merge.was().isEmpty();
        final String size = String.format(Locale.ROOT, "%,d KiB", Math.round(merge.sizeMb() * 1024));
        return Tint.line("[", Tint.green(Text.literal("ebuild")), isNew ? "  " : "     ",
                isNew ? Tint.green(Text.literal("N")) : Tint.cyan(Text.literal("U")), isNew ? "     ] " : "  ] ",
                Tint.green(merge.full()), isNew ? "" : Tint.line(" ", Tint.blue("[" + merge.was() + "]")),
                Text.literal("  USE=\""), Tint.red(merge.flagsOn()), merge.flagsOff().isEmpty() ? "" : " ",
                Tint.blue(merge.flagsOff()), "\" ", size);
    }

    /** One package through every phase it has. */
    private static void one(final TtyScript.Builder script, final Merge merge, final int at, final int of,
                            final int jobs) {
        final String full = merge.full();
        final String unpackedAs = merge.archive().replaceAll("\\.tar\\..*$", "");
        final String source = merge.work() + "/" + unpackedAs;
        final String image = merge.work().replace("/work", "/image");
        final String buildSize = String.format(Locale.ROOT, "%7d KiB", Math.round(merge.sizeMb() * 9.4 * 1024));
        final String treeSize = String.format(Locale.ROOT, "%7d KiB", Math.round(merge.sizeMb() * 3.1 * 1024));
        script.say("")
                .say(counted(EMERGING, at, of, full))
                .pause(6);
        /* A package that is only a list of others has no source of its own to fetch, unpack or patch. */
        if (!merge.archive().isEmpty()) {
            fetched(script, merge, source, unpackedAs);
        }
        if (merge.buildTicks() > 0) {
            built(script, merge, source, jobs);
        }
        script.say("")
                .say(arrowed(INSTALL_INTO.with(full, image)))
                .pause(10)
                .say(arrowed(COMPLETED_INSTALLING.with(full, image)))
                .say("")
                .say(star(BUILD_DIRECTORY_SIZE.with(buildSize)))
                .say(star(INSTALLED_TREE_SIZE.with(treeSize)))
                .say("")
                .say(counted(INSTALLING, at, of, full))
                .say(star(COLLISIONS.with(Math.round(80 + merge.sizeMb() * 22))))
                .pause(9)
                .say(arrowed(MERGING.with(full, "/")));
        final String doc = "/usr/share/doc/" + merge.name() + "-" + merge.version() + "/";
        final List<CliLine> laid = List.of(directory("/usr/"), directory("/usr/bin/"),
                mergedFile("/usr/bin/" + merge.name()), directory("/usr/share/"), directory("/usr/share/doc/"),
                mergedFile(doc), mergedFile(doc + "README.bz2"), directory("/usr/share/man/"),
                directory("/usr/share/man/man1/"), mergedFile("/usr/share/man/man1/" + merge.name() + ".1.bz2"));
        script.flood(8, laid.size(), laid::get)
                .say(arrowed(MERGED.with(full)))
                .say(counted(COMPLETED, at, of, full))
                .pause(6);
    }

    /** The source coming down the wire, checked, unpacked and patched, which every package with a source does. */
    private static void fetched(final TtyScript.Builder script, final Merge merge, final String source,
                                final String unpackedAs) {
        final double seconds = Math.max(1, merge.fetchTicks()) / 20.0;
        final String address = FetchVoice.MIRROR + "/distfiles/" + merge.archive();
        final String patch = unpackedAs + "-gentoo-patchset.patch";
        script.say(arrowed(DOWNLOADING.with(address)))
                .redraw(merge.fetchTicks(), p -> Bars.fetch(merge.archive(), p, merge.sizeMb(),
                        merge.sizeMb() / seconds, seconds))
                .say(Bars.ok(Text.literal(merge.archive() + " BLAKE2B SHA512 size ;-) ...")))
                .pause(5)
                .say(arrowed(UNPACKING_SOURCE.text()))
                .say(arrowed(UNPACKING.with(merge.archive(), merge.work())))
                .pause((int) Math.min(52, 8 + merge.sizeMb() * 0.28))
                .say(arrowed(SOURCE_UNPACKED.with(merge.work())))
                .say(arrowed(PREPARING_SOURCE.with(source)))
                .pause(7)
                .say(Bars.ok(APPLYING.with(patch)))
                .pause(4)
                .say(arrowed(SOURCE_PREPARED.text()));
    }

    /** The configure page and the compile, which is where a build's time goes. */
    private static void built(final TtyScript.Builder script, final Merge merge, final String source,
                              final int jobs) {
        final int configuring = Math.max(10, merge.buildTicks() * 15 / 100);
        final int compiling = Math.max(20, merge.buildTicks() * 85 / 100);
        final int checks = Math.min(configuring * 2, 64);
        final int files = Math.min(compiling * TtyScript.MAX_LINES_PER_TICK, Math.max(40, compiling));
        final String library = merge.name().replaceAll("-.*", "");
        script.say(arrowed(CONFIGURING_SOURCE.with(source)))
                .say(star(Text.literal("econf: updating config.sub with /usr/share/gnuconfig/config.sub")))
                .say(Text.literal("./configure --prefix=/usr --build=x86_64-pc-linux-gnu --host=x86_64-pc-linux-gnu "
                        + "--mandir=/usr/share/man --sysconfdir=/etc --localstatedir=/var/lib --libdir=/usr/lib64"))
                .flood(configuring, checks, index -> CliLine.plain(CHECKS[index % CHECKS.length]))
                .sayAll(CONFIGURED.stream().map(CliLine::plain).toList())
                .say(arrowed(SOURCE_CONFIGURED.text()))
                .say(arrowed(COMPILING_SOURCE.with(source)))
                .say(Text.literal("make -j" + jobs))
                .flood(compiling, files, index -> compiled(index, library))
                .say(arrowed(SOURCE_COMPILED.text()))
                .say(arrowed(TEST_PHASE.with(merge.full())));
    }

    /**
     * The source file compiled at a place in a build, worked out from the place alone, so a build that is looked
     * at twice names the same files. Shared with the ports tree, which compiles the same kind of program.
     */
    static String sourceAt(final int index) {
        return SOURCES[Math.floorMod(spread(index), SOURCES.length)];
    }

    /** The compiler's line for the file at a place in the build, worked out from the place alone. */
    private static CliLine compiled(final int index, final String library) {
        final int hash = spread(index);
        final String file = sourceAt(index);
        /* A Sigma source goes in and the listing a machine runs comes out, which is this world's object file. */
        return CliLine.plain(Text.literal(Math.floorMod(hash >>> 8, 6) != 0
                ? "x86_64-pc-linux-gnu-scc -DHAVE_CONFIG -I. -O2 -pipe -march=native -c -o " + file + ".asm "
                        + file + ".sg"
                : "/bin/sh ./libtool --tag=SCC --mode=compile x86_64-pc-linux-gnu-scc -O2 -pipe -c -o lib"
                        + library + "_la-" + file + ".asm " + file + ".sg"));
    }

    private static CliLine star(final Text text) {
        return Tint.line(Tint.star(), text);
    }

    /** A line of the manager's own, led by its arrows. */
    private static CliLine arrowed(final Text text) {
        return Tint.line(Tint.arrows(), " ", text);
    }

    /** A package's place in the merge, which it names as it starts, installs and completes each one. */
    private static CliLine counted(final TextKey doing, final int at, final int of, final String full) {
        return Tint.line(Tint.arrows(), " ", doing, " (", Tint.yellow(String.valueOf(at)), " ", OF, " ",
                Tint.yellow(String.valueOf(of)), ") ", Tint.green(full), "::" + REPOSITORY);
    }

    /** The news waiting to be read, and how to read it. */
    private static List<CliLine> newsWaiting(final int items) {
        return List.of(Tint.line(Tint.star(), Tint.yellow(IMPORTANT.text()), " ", NEWS_WAITING.with(items)),
                Tint.line(Tint.star(), NEWS_USE.with(REPOSITORY), " ", Tint.green(Text.literal("eselect news read")),
                        " ", NEWS_VIEW));
    }

    /** A directory the merge passes through, which it names and does not write. */
    private static CliLine directory(final String path) {
        return CliLine.plain(Text.literal("--- " + path));
    }

    /** A file the merge writes, named after its arrows. */
    private static CliLine mergedFile(final String path) {
        return Tint.line(Tint.arrows(), Text.literal(" " + path));
    }

    /** A place in a build scattered over the whole range, so neighbouring places name unrelated files. */
    private static int spread(final int index) {
        final int hash = index * 0x9E3779B1;
        return hash ^ hash >>> 15;
    }
}
