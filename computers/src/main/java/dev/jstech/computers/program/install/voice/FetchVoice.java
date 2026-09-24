/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
public final class FetchVoice {

    /** How the Mirror is addressed, which is the only address anything in this world is fetched from. */
    public static final String MIRROR = "mirror://mainframe";

    /** The host inside that address, which is what gets resolved and connected to. */
    private static final String HOST = "mainframe";

    /** The fetcher's own name, which it signs its errors with. */
    private static final String TOOL = "wget";

    private static final TextKey RESOLVED = TextKey.of("jsc.install.fetch_voice.resolved", "Resolving %s... done.");
    private static final TextKey CONNECTED =
            TextKey.of("jsc.install.fetch_voice.connected", "Connecting to %s... connected.");
    private static final TextKey REQUEST_SENT =
            TextKey.of("jsc.install.fetch_voice.request_sent", "Mirror request sent, awaiting response... %s");
    private static final TextKey LENGTH = TextKey.of("jsc.install.fetch_voice.length", "Length: %s");
    private static final TextKey SAVING_TO = TextKey.of("jsc.install.fetch_voice.saving_to", "Saving to: '%s'");
    private static final TextKey SAVED =
            TextKey.of("jsc.install.fetch_voice.saved", "%s (%s) - '%s' saved [%s/%s]");
    private static final TextKey NOT_RESOLVED = TextKey.of("jsc.install.fetch_voice.not_resolved",
            "Resolving %s... failed: Name or service not known.");
    private static final TextKey UNABLE_TO_RESOLVE =
            TextKey.of("jsc.install.fetch_voice.unable_to_resolve", "unable to resolve host address '%s'");

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
        /* The answer's status, its length and its type are the protocol's, the same in every language. */
        final String status = "200 OK";
        final String length = bytes + " (" + sizeMb + "M) [application/x-xz]";
        final String speed = String.format(Locale.ROOT, "%.1f MB/s", rate);
        return TtyScript.script()
                .say("--" + stamp.dated() + "--  " + MIRROR + path)
                .say(RESOLVED.with(HOST))
                .pause(6)
                .say(CONNECTED.with(HOST))
                .pause(4)
                .say(REQUEST_SENT.with(status))
                .say(LENGTH.with(length))
                .say(SAVING_TO.with(file))
                .say("")
                .redraw(ticks, progress -> Bars.fetch(file, progress, sizeMb, rate, seconds))
                .say("")
                .say(SAVED.with(stamp.after(ticks).dated(), speed, file, bytes, bytes))
                .say("")
                .effect(saved)
                .done();
    }

    /**
     * What the fetcher says when what it was pointed at cannot be found: no Mainframe on the network is
     * running the Mirror, or the address names somewhere that is not on this world's network at all.
     */
    public static TtyScript unreachable(final String url, final WorldStamp stamp) {
        final int scheme = url.indexOf("://");
        final String rest = scheme < 0 ? url : url.substring(scheme + 3);
        final String host = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
        return TtyScript.script()
                .say("--" + stamp.dated() + "--  " + url)
                .pause(10)
                .say(NOT_RESOLVED.with(host))
                .say(CliTexts.SAID_BY.with(TOOL, UNABLE_TO_RESOLVE.with(host)))
                .done();
    }
}
