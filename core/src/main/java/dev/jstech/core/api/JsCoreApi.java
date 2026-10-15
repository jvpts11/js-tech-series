/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/**
 * What the series promises to anything built on it, and the one place that promise is dated.
 *
 * <p>The promise is this package and the ones under it, the packages listed in {@link #PROMISED_PACKAGES}, and
 * everything their public faces reach. Anything else in the Core is the Core's own business and may change in any
 * release without a word, so a mod that reaches into it is a mod that will break.
 *
 * <p>A part of the promise is at one of three levels. What is not promised at all is <b>internal</b>. What is
 * promised but may still change, with a note in the changelog when it does, is <b>experimental</b>, and carries
 * {@code @ApiStatus.Experimental}: everything new starts there. What is promised and holds is <b>stable</b>, and a
 * part only gets there once two real mods use it, it has real tests, its documentation has an example that compiles,
 * what it saves and what it sends between the server and the players are defined, and it has gone through a whole
 * cycle without its shape changing.
 *
 * <p>The version below is not the Core's version: it is the number of the shape of this promise. It goes up by one
 * whenever something here is added, and an addon that wants to be sure asks for it. The promise is also written down
 * line by line, with the version each part came in, and a test holds the code to it; {@code docs/API.md} says how.
 */
public final class JsCoreApi {

    /**
     * The number of the shape of this API, raised by one whenever anything is added to it.
     *
     * <p>An addon that uses something added later can say so by refusing to load below the number that
     * added it, which is the whole reason this is a number and not a date. The list of what each number
     * added is kept in {@code core/src/test/resources/api/core.txt}, one line per type and member.
     *
     * <pre>{@code
     * if (JsCoreApi.VERSION < 3) {
     *     throw new IllegalStateException("MyAddon needs J's Core with API version 3 or newer");
     * }
     * }</pre>
     */
    public static final int VERSION = 8;

    /**
     * The Core's packages that are part of the promise beside this one, each a kit a mod may build on.
     *
     * <p>A kit enters this list once it is documented for a reader who knows nothing of it, starting with the
     * {@code package-info} of its package, and from then on everything public in the package, and every type of the
     * Core its public face reaches, is held to the promise like this package is. What it reaches has to be promised
     * too, so a promise can never hand a mod a type that is not.
     */
    @ApiStatus.Experimental
    public static final List<String> PROMISED_PACKAGES = List.of("dev.jstech.core.call", "dev.jstech.core.codec",
            "dev.jstech.core.optional", "dev.jstech.core.task");

    /**
     * How long something here lives once it is marked as going.
     *
     * <p>Anything marked {@code @Deprecated} stays for one whole cycle of the series and goes in the next.
     * A cycle is what the releases are counted in, so there is always a release where both the old way and
     * the new one work and an addon can move between them without a version of its own that does neither.
     */
    public static final int CYCLES_BEFORE_REMOVAL = 1;

    /**
     * Whether this API is settled.
     *
     * <p>It is not, and it says so rather than pretending. The computing side of the series is still being
     * built, and until J's Computers and J's Industrial are both finished in what they do and in how they
     * are written, anything here may change shape between releases. What is here works and is worth
     * building on; what it is called and what it takes may not survive. It settles when those two do,
     * which is also when the rest of the series starts building its own on top.
     */
    public static final boolean SETTLED = false;

    private JsCoreApi() {
    }
}
