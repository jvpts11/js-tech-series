/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api;

/**
 * What the series promises to anything built on it, and the one place that promise is dated.
 *
 * <p>Everything an addon is meant to use is reached from this package or from the event this package
 * carries. Anything else in the Core is the Core's own business and may change in any release without a
 * word, so a mod that reaches into it is a mod that will break.
 *
 * <p>The version below is not the Core's version: it is the number of the shape of this promise. It goes
 * up by one whenever something here is added, and an addon that wants to be sure asks for it. What the
 * number does NOT promise yet is that the shape will hold: see the note on stability.
 *
 * <p>What is new carries {@code @ApiStatus.Experimental} from the cycle it came in until the cycle after its
 * first release, since until then no mod has used it in a released game and it is the part most likely to
 * move. The promise is also written down line by line, with the version each part came in, and a test holds
 * the code to it; {@code docs/API.md} says how.
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
    public static final int VERSION = 3;

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
