/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.os.SourceAdvantage;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * What FreeBSD's ports say: {@code portsnap} fetching the tree and laying it out, and a port's {@code make} taking
 * it through its phases.
 *
 * <p>The ports speak plainly, a phase to a line, each opened with the arrow the ports have always opened them with,
 * and they say what they are doing to which version of which program the whole way down. What they fetch comes
 * from the network's Mirror, and the compiler going by is the Sigma compiler, told to build for the processor it
 * is running on, since that is the reason for building a port at all.
 *
 * <p>Their sentences are the player's language; the compiler's command lines, the fetcher's figures and the paths
 * laid out are the same in every language, as the real ones are.
 */
@TextHolder
public final class PortsVoices {

    /** How many columns a fetched file's name is set in, so its figures line up under each other. */
    private static final int NAME_COLUMNS = 44;

    /** How long a phase that does nothing to watch takes to say it is done. */
    private static final int PHASE_TICKS = 8;

    /** Where a port's sources are kept once fetched. */
    private static final String DISTFILES = "/usr/ports/distfiles/";

    /** Where the tree is laid out, which is what a full disk names when it refuses the rest of it. */
    private static final String TREE = "/usr/ports";

    private static final TextKey LOOKING_UP =
            TextKey.of("jsc.install.ports_voices.looking_up", "Looking up the Mirror for the ports tree...");
    private static final TextKey FOUND = TextKey.of("jsc.install.ports_voices.found", "found");
    private static final TextKey FETCHING_TAG =
            TextKey.of("jsc.install.ports_voices.fetching_tag", "Fetching snapshot tag from the Mirror... done.");
    private static final TextKey FETCHING_SNAPSHOT =
            TextKey.of("jsc.install.ports_voices.fetching_snapshot", "Fetching snapshot generated at %s:");
    private static final TextKey EXTRACTING_SNAPSHOT =
            TextKey.of("jsc.install.ports_voices.extracting_snapshot", "Extracting snapshot... done.");
    private static final TextKey VERIFYING =
            TextKey.of("jsc.install.ports_voices.verifying", "Verifying snapshot integrity... done.");
    private static final TextKey REMOVING_OLD =
            TextKey.of("jsc.install.ports_voices.removing_old", "Removing old files and directories... done.");
    private static final TextKey EXTRACTING_NEW =
            TextKey.of("jsc.install.ports_voices.extracting_new", "Extracting new files:");
    private static final TextKey BUILDING_INDEX =
            TextKey.of("jsc.install.ports_voices.building_index", "Building new INDEX files...");
    private static final TextKey INDEXED = TextKey.of("jsc.install.ports_voices.indexed",
            "Building new INDEX files... done. %s ports, %s on this disk.");
    private static final TextKey NO_SPACE =
            TextKey.of("jsc.install.ports_voices.no_space", "%s: No space left on device");
    private static final TextKey DONE = TextKey.of("jsc.install.ports_voices.done", "done");
    private static final TextKey STAGING = TextKey.of("jsc.install.ports_voices.staging", "Staging for %s");
    private static final TextKey INSTALLING = TextKey.of("jsc.install.ports_voices.installing", "Installing for %s");
    private static final TextKey REGISTERING =
            TextKey.of("jsc.install.ports_voices.registering", "Registering installation for %s");
    private static final TextKey BUILT_HERE =
            TextKey.of("jsc.install.ports_voices.built_here", "Built for this machine:");
    private static final TextKey LESS = TextKey.of("jsc.install.ports_voices.less",
            "%s%% less memory, disk and processor than the package.");
    private static final TextKey CLEANING = TextKey.of("jsc.install.ports_voices.cleaning", "Cleaning for %s");
    private static final TextKey LICENSE =
            TextKey.of("jsc.install.ports_voices.license", "License %s accepted by the user");
    private static final TextKey NOT_FETCHED =
            TextKey.of("jsc.install.ports_voices.not_fetched", "%s doesn't seem to exist in %s.");
    private static final TextKey ATTEMPTING =
            TextKey.of("jsc.install.ports_voices.attempting", "Attempting to fetch from the Mirror.");
    private static final TextKey EXTRACTING = TextKey.of("jsc.install.ports_voices.extracting", "Extracting for %s");
    private static final TextKey CHECKSUM_OK =
            TextKey.of("jsc.install.ports_voices.checksum_ok", "SHA256 Checksum OK for %s.");
    private static final TextKey CONFIGURING =
            TextKey.of("jsc.install.ports_voices.configuring", "Configuring for %s");
    private static final TextKey BUILDING = TextKey.of("jsc.install.ports_voices.building", "Building for %s");

    private PortsVoices() {
    }

    /**
     * A port to build.
     *
     * @param pkgName    its name and version, which every phase names
     * @param name       the program it builds, which is the file the link writes
     * @param license    the licence it is under, or empty for a port that declares none
     * @param distfile   the file its source comes in
     * @param sourceMb   how big that file is
     * @param fetchTicks how long the machine's connection takes over it
     * @param buildTicks how long the machine's processor takes to build it
     */
    public record Port(String pkgName, String name, String license, String distfile, double sourceMb,
                       int fetchTicks, int buildTicks) {
    }

    /**
     * Fetching the tree as one snapshot from the Mirror.
     *
     * @param sizeKb  how big the snapshot is
     * @param ticks   how long the machine's connection takes over it
     * @param fetched what having it means to the machine, done once it has come
     */
    public static TtyScript fetch(final double sizeKb, final int ticks, final WorldStamp stamp,
                                  final Runnable fetched) {
        return TtyScript.script()
                .say(Tint.line(LOOKING_UP, " ", Tint.green(FOUND.text()), "."))
                .pause(6)
                .say(FETCHING_TAG)
                .pause(4)
                .say(FETCHING_SNAPSHOT.with(stamp.dated()))
                .redraw(ticks, p -> snapshotBar(sizeKb, p))
                .say(EXTRACTING_SNAPSHOT)
                .pause(6)
                .say(VERIFYING)
                .pause(4)
                .effect(fetched)
                .done();
    }

    /**
     * Laying the tree out under {@code /usr/ports}, a folder to a line, and indexing it.
     *
     * @param update  whether a tree already there is being brought up to date, which only names what changed
     * @param origins the folders laid out, each as its category and its name
     * @param ticks   how long writing them takes
     * @param laid    what having them written means to the machine
     * @param indexed the line that says how it went, asked for once they are written
     */
    public static TtyScript extract(final boolean update, final List<String> origins, final int ticks,
                                    final Runnable laid, final Supplier<CliLine> indexed) {
        final TtyScript.Builder script = TtyScript.script();
        if (update) {
            script.say(REMOVING_OLD)
                    .pause(4)
                    .say(EXTRACTING_NEW);
        }
        return script.flood(ticks, origins.size(),
                        index -> new CliLine(TREE + "/" + origins.get(index) + "/", CliStyle.DIM))
                .effect(laid)
                .redraw(10, p -> p >= 1.0 ? indexed.get() : Tint.line(BUILDING_INDEX, " "))
                .done();
    }

    /** How the index ends when the tree was written: how many ports, and what they take on the disk. */
    public static CliLine indexed(final int ports, final String onDisk) {
        final String count = String.format(Locale.ROOT, "%,d", ports);
        return CliLine.plain(INDEXED.with(count, onDisk));
    }

    /** How it ends when the disk filled before the tree was all written. */
    public static CliLine noSpace() {
        return new CliLine(NO_SPACE.with(TREE), CliStyle.ERROR);
    }

    /**
     * A port through the phases asked for: built, installed and cleaned, or any of those on their own.
     *
     * @param built     what having it built means to the machine, or null when it is not to be built
     * @param installed what having it installed means, or null when it is not to be installed
     * @param cleaned   what cleaning it means, or null when it is not to be cleaned
     */
    public static TtyScript make(final Port port, @Nullable final Runnable built,
                                 @Nullable final Runnable installed, @Nullable final Runnable cleaned) {
        final TtyScript.Builder script = TtyScript.script();
        if (built != null) {
            build(script, port);
            script.effect(built);
        }
        if (installed != null) {
            script.say(phase(STAGING.with(port.pkgName())))
                    .pause(PHASE_TICKS)
                    .say(phase(INSTALLING.with(port.pkgName())))
                    .pause(PHASE_TICKS)
                    .say(Tint.line(arrow(), "   ", REGISTERING.with(port.pkgName())))
                    .effect(installed)
                    .say(Tint.line(Tint.green(BUILT_HERE.text()), Tint.green(" "),
                            LESS.with(SourceAdvantage.PERCENT)));
        }
        if (cleaned != null) {
            script.say(phase(CLEANING.with(port.pkgName())))
                    .pause(4)
                    .effect(cleaned);
        }
        return script.done();
    }

    /** The source fetched from the Mirror, checked, unpacked, configured and compiled. */
    private static void build(final TtyScript.Builder script, final Port port) {
        if (!port.license().isEmpty()) {
            script.say(phase(LICENSE.with(port.license())));
        }
        final double seconds = Math.max(1, port.fetchTicks()) / 20.0;
        final int configuring = Math.max(10, port.buildTicks() * 15 / 100);
        final int compiling = Math.max(20, port.buildTicks() * 85 / 100);
        final int files = Math.max(3, (int) Math.round(port.sourceMb() * 4));
        script.say(Tint.line("=> ", NOT_FETCHED.with(port.distfile(), DISTFILES)))
                .say(Tint.line("=> ", ATTEMPTING))
                .redraw(port.fetchTicks(), p -> fetched(port.distfile(), port.sourceMb(), seconds, p))
                .say(phase(EXTRACTING.with(port.pkgName())))
                .say(Tint.line("=> ", CHECKSUM_OK.with(port.distfile())))
                .pause(PHASE_TICKS)
                .say(phase(CONFIGURING.with(port.pkgName())))
                .pause(configuring)
                .say(phase(BUILDING.with(port.pkgName())))
                .flood(compiling, files, PortsVoices::compiled)
                .say(linked(port));
    }

    /** The compiler's line for the file at a place in the build, a command line like any other. */
    private static CliLine compiled(final int index) {
        final String source = PortageVoices.sourceAt(index);
        return CliLine.plain(Text.literal("scc -O2 -pipe -march=native -c -o " + source + ".asm " + source + ".sg"));
    }

    /** The compiler's last line, which links what it compiled into the port's program. */
    private static CliLine linked(final Port port) {
        return CliLine.plain(Text.literal("scc -O2 -pipe -march=native -o " + port.name() + " "
                + PortageVoices.sourceAt(0) + ".asm " + PortageVoices.sourceAt(1) + ".asm "
                + PortageVoices.sourceAt(2) + ".asm"));
    }

    /** The fetcher's line: while it comes, how far along; once it has, how big it was and how fast it came. */
    private static CliLine fetched(final String file, final double sizeMb, final double seconds,
                                   final double progress) {
        final double kb = sizeMb * 1024.0;
        final long rate = Math.max(1L, Math.round(kb / seconds));
        if (progress >= 1.0) {
            return CliLine.plain(Text.literal(String.format(Locale.ROOT, "%-" + NAME_COLUMNS
                    + "s %8s %5d kBps    %02ds", file, size(kb), rate, Math.round(seconds) % 60)));
        }
        final long left = (long) Math.ceil(seconds * (1.0 - progress));
        return CliLine.plain(Text.literal(String.format(Locale.ROOT, "%-" + NAME_COLUMNS
                + "s %3d%% of %8s %5d kBps %02dm%02ds", file, (int) Math.floor(progress * 100), size(kb), rate,
                left / 60, left % 60)));
    }

    /** The snapshot coming, as a bar filling up and then done. */
    private static CliLine snapshotBar(final double sizeKb, final double progress) {
        final int width = 30;
        final int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * width);
        return Tint.line(Text.literal(String.format(Locale.ROOT, "ports.tar.gz %8s  [", size(sizeKb))),
                Tint.green("#".repeat(filled)), " ".repeat(width - filled) + "]",
                progress >= 1.0 ? Tint.line("  ", DONE)
                        : Text.literal(String.format(Locale.ROOT, " %3d%%", (int) Math.floor(progress * 100))));
    }

    /** A size the way the fetcher writes one: kilobytes, until it is big enough for megabytes. */
    private static String size(final double kb) {
        return kb < 10_240.0 ? Math.max(1L, Math.round(kb)) + " kB" : Math.round(kb / 1024.0) + " MB";
    }

    /** A phase of the port's, opened with its arrow. */
    private static CliLine phase(final Text text) {
        return Tint.line(arrow(), "  ", text);
    }

    /** The arrow every phase of a port opens with. */
    private static CliSpan arrow() {
        return new CliSpan("===>", CliStyle.ACCENT);
    }
}
