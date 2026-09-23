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
 */
public final class PortsVoices {

    /** How many columns a fetched file's name is set in, so its figures line up under each other. */
    private static final int NAME_COLUMNS = 44;

    /** How long a phase that does nothing to watch takes to say it is done. */
    private static final int PHASE_TICKS = 8;

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
                .say(Tint.line("Looking up the Mirror for the ports tree... ", Tint.green("found"), "."))
                .pause(6)
                .say("Fetching snapshot tag from the Mirror... done.")
                .pause(4)
                .say("Fetching snapshot generated at " + stamp.dated() + ":")
                .redraw(ticks, p -> snapshotBar(sizeKb, p))
                .say("Extracting snapshot... done.")
                .pause(6)
                .say("Verifying snapshot integrity... done.")
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
            script.say("Removing old files and directories... done.")
                    .pause(4)
                    .say("Extracting new files:");
        }
        return script.flood(ticks, origins.size(),
                        index -> new CliLine("/usr/ports/" + origins.get(index) + "/", CliStyle.DIM))
                .effect(laid)
                .redraw(10, p -> p >= 1.0 ? indexed.get() : CliLine.plain("Building new INDEX files... "))
                .done();
    }

    /** How the index ends when the tree was written: how many ports, and what they take on the disk. */
    public static CliLine indexed(final int ports, final String onDisk) {
        return CliLine.plain(String.format(Locale.ROOT, "Building new INDEX files... done. %,d ports, %s on this"
                + " disk.", ports, onDisk));
    }

    /** How it ends when the disk filled before the tree was all written. */
    public static CliLine noSpace() {
        return new CliLine("/usr/ports: No space left on device", CliStyle.ERROR);
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
            script.say(phase("Staging for " + port.pkgName()))
                    .pause(PHASE_TICKS)
                    .say(phase("Installing for " + port.pkgName()))
                    .pause(PHASE_TICKS)
                    .say(Tint.line(arrow(), "   Registering installation for " + port.pkgName()))
                    .effect(installed)
                    .say(Tint.line(Tint.green("Built for this machine: "), SourceAdvantage.PERCENT
                            + "% less memory, disk and processor than the package."));
        }
        if (cleaned != null) {
            script.say(phase("Cleaning for " + port.pkgName()))
                    .pause(4)
                    .effect(cleaned);
        }
        return script.done();
    }

    /** The source fetched from the Mirror, checked, unpacked, configured and compiled. */
    private static void build(final TtyScript.Builder script, final Port port) {
        if (!port.license().isEmpty()) {
            script.say(phase("License " + port.license() + " accepted by the user"));
        }
        final double seconds = Math.max(1, port.fetchTicks()) / 20.0;
        final int configuring = Math.max(10, port.buildTicks() * 15 / 100);
        final int compiling = Math.max(20, port.buildTicks() * 85 / 100);
        final int files = Math.max(3, (int) Math.round(port.sourceMb() * 4));
        final String flags = "scc -O2 -pipe -march=native";
        script.say("=> " + port.distfile() + " doesn't seem to exist in /usr/ports/distfiles/.")
                .say("=> Attempting to fetch from the Mirror.")
                .redraw(port.fetchTicks(), p -> fetched(port.distfile(), port.sourceMb(), seconds, p))
                .say(phase("Extracting for " + port.pkgName()))
                .say("=> SHA256 Checksum OK for " + port.distfile() + ".")
                .pause(PHASE_TICKS)
                .say(phase("Configuring for " + port.pkgName()))
                .pause(configuring)
                .say(phase("Building for " + port.pkgName()))
                .flood(compiling, files, index -> CliLine.plain(flags + " -c -o "
                        + PortageVoices.sourceAt(index) + ".asm " + PortageVoices.sourceAt(index) + ".sg"))
                .say(flags + " -o " + port.name() + " " + PortageVoices.sourceAt(0) + ".asm "
                        + PortageVoices.sourceAt(1) + ".asm " + PortageVoices.sourceAt(2) + ".asm");
    }

    /** The fetcher's line: while it comes, how far along; once it has, how big it was and how fast it came. */
    private static CliLine fetched(final String file, final double sizeMb, final double seconds,
                                   final double progress) {
        final double kb = sizeMb * 1024.0;
        final long rate = Math.max(1L, Math.round(kb / seconds));
        if (progress >= 1.0) {
            return CliLine.plain(String.format(Locale.ROOT, "%-" + NAME_COLUMNS + "s %8s %5d kBps    %02ds",
                    file, size(kb), rate, Math.round(seconds) % 60));
        }
        final long left = (long) Math.ceil(seconds * (1.0 - progress));
        return CliLine.plain(String.format(Locale.ROOT, "%-" + NAME_COLUMNS + "s %3d%% of %8s %5d kBps %02dm%02ds",
                file, (int) Math.floor(progress * 100), size(kb), rate, left / 60, left % 60));
    }

    /** The snapshot coming, as a bar filling up and then done. */
    private static CliLine snapshotBar(final double sizeKb, final double progress) {
        final int width = 30;
        final int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * width);
        return Tint.line(String.format(Locale.ROOT, "ports.tar.gz %8s  [", size(sizeKb)),
                Tint.green("#".repeat(filled)), " ".repeat(width - filled) + "]",
                progress >= 1.0 ? "  done" : String.format(Locale.ROOT, " %3d%%", (int) Math.floor(progress * 100)));
    }

    /** A size the way the fetcher writes one: kilobytes, until it is big enough for megabytes. */
    private static String size(final double kb) {
        return kb < 10_240.0 ? Math.max(1L, Math.round(kb)) + " kB" : Math.round(kb / 1024.0) + " MB";
    }

    /** A phase of the port's, opened with its arrow. */
    private static CliLine phase(final String text) {
        return Tint.line(arrow(), "  " + text);
    }

    /** The arrow every phase of a port opens with. */
    private static CliSpan arrow() {
        return new CliSpan("===>", CliStyle.ACCENT);
    }
}
