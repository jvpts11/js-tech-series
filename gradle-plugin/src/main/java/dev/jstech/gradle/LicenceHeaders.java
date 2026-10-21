/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The licence header every source starts with, as the series writes it: a block comment above the package line
 * naming the licence by its SPDX id ({@code SPDX-License-Identifier: MIT}), whose the copyright is
 * ({@code Copyright (C) 2026 Jane Doe}) and which work the file is part of ({@code This file is part of My Mod.}), each
 * on a line of its own with a blank line of the comment between them.
 *
 * <p>Pure: it reads and writes a source's text, and the plugin's tasks hand it the files.
 */
public final class LicenceHeaders {

    private static final Pattern SPDX = Pattern.compile("SPDX-License-Identifier:\\s*(\\S+)");
    private static final String NEWLINE = "\n";

    private LicenceHeaders() {
    }

    /**
     * The licence the source's header names, when it opens with a block comment that names one before anything
     * else; none when it has no such header.
     */
    public static Optional<String> licenceOf(final String source) {
        final String text = source.startsWith("﻿") ? source.substring(1) : source;
        final String opening = text.stripLeading();
        if (!opening.startsWith("/*")) {
            return Optional.empty();
        }
        final int end = opening.indexOf("*/");
        if (end < 0) {
            return Optional.empty();
        }
        final Matcher spdx = SPDX.matcher(opening.substring(0, end));
        return spdx.find() ? Optional.of(spdx.group(1)) : Optional.empty();
    }

    /**
     * What is wrong with the source's header, when something is: none at all, or another licence than
     * {@code licence}; nothing when {@code licence} is null and it has any header.
     */
    public static Optional<String> problemOf(final String source, final String licence) {
        final Optional<String> named = licenceOf(source);
        if (named.isEmpty()) {
            return Optional.of("has no licence header");
        }
        if (licence != null && !licence.isBlank() && !named.get().equals(licence)) {
            return Optional.of("names " + named.get() + ", not " + licence);
        }
        return Optional.empty();
    }

    /** The source with the header put before it, when it has none; as it is otherwise. */
    public static String withHeader(final String source, final String licence, final int year, final String holder,
                                    final String work) {
        if (licenceOf(source).isPresent()) {
            return source;
        }
        return header(licence, year, holder, work) + source.stripLeading();
    }

    /** The header, ending with the line after its closing. */
    public static String header(final String licence, final int year, final String holder, final String work) {
        return "/*" + NEWLINE
                + " * SPDX-License-Identifier: " + licence + NEWLINE
                + " *" + NEWLINE
                + " * Copyright (C) " + year + " " + holder + NEWLINE
                + " *" + NEWLINE
                + " * This file is part of " + work + "." + NEWLINE
                + " */" + NEWLINE;
    }
}
