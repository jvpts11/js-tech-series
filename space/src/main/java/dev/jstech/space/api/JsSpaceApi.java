/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

/**
 * What J's Space promises to anything built on it, and the one place that promise is dated.
 *
 * <p>The promise is this package: everything public in it and everything its public faces reach. How settled each
 * part is, and how long something lives once it is marked as going, are the series' answers rather than this mod's:
 * see J's Core's.
 */
public final class JsSpaceApi {

    /**
     * The number of the shape of this API, raised by one whenever anything is added to it.
     *
     * <p>An add-on that uses something added later can say so by refusing to load below the number that added it.
     * The list of what each number added is kept beside J's Core's tests, in {@code api/space.txt}, one line per type
     * and member.
     *
     * <pre>{@code
     * if (JsSpaceApi.VERSION < 2) {
     *     throw new IllegalStateException("MyAddon needs J's Space with API version 2 or newer");
     * }
     * }</pre>
     */
    public static final int VERSION = 8;

    private JsSpaceApi() {
    }
}
