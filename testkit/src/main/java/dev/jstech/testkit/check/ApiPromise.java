/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

/**
 * One thing a mod promises: a type, or a member of one, in the words the API's photo writes it with.
 *
 * @param owner        the type's whole name, nested types joined with dots
 * @param what         {@code type ...} for the type itself, or the member as a caller sees it
 * @param experimental whether it, or a type it sits in, carries {@code @ApiStatus.Experimental}
 */
public record ApiPromise(String owner, String what, boolean experimental) {

    /** How the line declaring a type starts, so it sorts before the type's members and is told from them. */
    public static final String TYPE = "type ";
    /** How a member or a type marked as going starts, after the word for a type if it is one. */
    public static final String DEPRECATED = "deprecated ";

    /** The promise as the photo keys it: its owner, two spaces, and what it says. */
    public String key() {
        return owner + "  " + what;
    }

    /** Whether it is a type rather than a member of one. */
    public boolean isType() {
        return what.startsWith(TYPE);
    }
}
