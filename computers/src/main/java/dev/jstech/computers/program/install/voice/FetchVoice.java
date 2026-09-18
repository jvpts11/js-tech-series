/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.tty.TtyScript;
import java.util.Locale;

/**
 * What the fetcher says while it pulls a file down from the Mirror.
 *
 * <p>There are no web addresses in this world. Everything a machine fetches comes from the Mirror a Mainframe
 * on its network runs, named the way the rest of the mod names it, so that is what the fetcher resolves,
 * connects to and asks. Everything else is the real tool's: the length in bytes and in short, the name it
 * saves under, a bar that fills with the rate beside it and the time left counting down, and the closing line
 * that says how fast it came and that all of it arrived.
 */
public final class FetchVoice {

    /** How the Mirror is addressed, which is the only address anything in this world is fetched from. */
    public static final String MIRROR = "mirror://mainframe";

    /** The host inside that address, which is what gets resolved and connected to. */
    private static final String HOST = "mainframe";

    private FetchVoice() {
    }

    /**
     * Fetching one file.
     *
     * @param path   where on the Mirror it is, from the root
     * @param sizeMb how big it is
     * @param ticks  how long this machine's connection takes over it
     * @param stamp  the world's date and time as the fetch starts
     * @param saved  what having the file means to the machine, done when it has all arrived and not before
     */
    public static TtyScript wget(final String path, final long sizeMb, final int ticks, final WorldStamp stamp,
                                 final Runnable saved) {
        final String file = path.substring(path.lastIndexOf('/') + 1);
        final long bytes = sizeMb * 1024L * 1024L;
        final double seconds = Math.max(1, ticks) / 20.0;
        final double rate = sizeMb / seconds;
        return TtyScript.script()
                .say("--" + stamp.dated() + "--  " + MIRROR + path)
                .say("Resolving " + HOST + "... done.")
                .pause(6)
                .say("Connecting to " + HOST + "... connected.")
                .pause(4)
                .say("Mirror request sent, awaiting response... 200 OK")
                .say("Length: " + bytes + " (" + sizeMb + "M) [application/x-xz]")
                .say("Saving to: '" + file + "'")
                .say("")
                .redraw(ticks, progress -> Bars.fetch(file, progress, sizeMb, rate, seconds))
                .say("")
                .say(String.format(Locale.ROOT, "%s (%.1f MB/s) - '%s' saved [%d/%d]",
                        stamp.after(ticks).dated(), rate, file, bytes, bytes))
                .say("")
                .effect(saved)
                .done();
    }

    /** What the fetcher says when no Mainframe on the network is running the Mirror. */
    public static TtyScript unreachable(final String path, final WorldStamp stamp) {
        return TtyScript.script()
                .say("--" + stamp.dated() + "--  " + MIRROR + path)
                .pause(10)
                .say("Resolving " + HOST + "... failed: Name or service not known.")
                .say("wget: unable to resolve host address '" + HOST + "'")
                .done();
    }
}
