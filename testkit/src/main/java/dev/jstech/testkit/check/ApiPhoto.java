/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mod's photo of its API: the newest version already through a release, its stable one, and the version of every
 * line, each line a type or a member a mod built on it can reach, with the version of the API that brought it in the
 * shape written there.
 *
 * @param stable   the newest version whose parts are no longer marked experimental
 * @param versions the version of each line, by its key
 */
public record ApiPhoto(int stable, Map<String, Integer> versions) {

    /* A line of a photo: the version, then the type the line is about, then what it says of it. */
    private static final Pattern LINE = Pattern.compile("^(\\d+) (\\S+)  (.+)$");
    /* The photo's line naming the newest version whose parts are no longer marked experimental. */
    private static final Pattern STABLE_LINE = Pattern.compile("^stable (\\d+)$");

    /** The photo in {@code file}, or an empty one when there is none yet. */
    public static ApiPhoto read(final Path file) {
        if (!Files.isRegularFile(file)) {
            return new ApiPhoto(0, Map.of());
        }
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The photo a text says, as a photo's file writes it. */
    public static ApiPhoto parse(final String text) {
        final Map<String, Integer> versions = new LinkedHashMap<>();
        int stable = 0;
        for (final String line : text.split("\n")) {
            final String trimmed = line.strip();
            final Matcher entry = LINE.matcher(trimmed);
            final Matcher stableLine = STABLE_LINE.matcher(trimmed);
            if (entry.matches()) {
                versions.put(entry.group(2) + "  " + entry.group(3), Integer.parseInt(entry.group(1)));
            } else if (stableLine.matches()) {
                stable = Integer.parseInt(stableLine.group(1));
            }
        }
        return new ApiPhoto(stable, versions);
    }

    /** A photo written out: what it is and its stable version, then its lines, each type before its members. */
    public static String text(final String mod, final int stable, final List<String> lines) {
        final StringBuilder out = new StringBuilder();
        out.append("# The public API of the mod in the folder '").append(mod).append("': every type and member a mod\n")
                .append("# built on it can reach. The number before a line is the version of the API that brought\n")
                .append("# it in the shape written there. Whatever came after the stable version carries\n")
                .append("# @ApiStatus.Experimental; the stable version rises only once what came up to\n")
                .append("# it meets the bar docs/API.md sets for a stable part. Kept by the API surface test,\n")
                .append("# which writes to build/api-photo what this file would have to say when the API changes.\n")
                .append("stable ").append(stable).append('\n');
        for (final String line : lines) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /** The newest version any line came in. */
    public int newest() {
        int newest = 0;
        for (final int version : versions.values()) {
            newest = Math.max(newest, version);
        }
        return newest;
    }
}
