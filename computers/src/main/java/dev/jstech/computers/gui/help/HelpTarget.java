/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import java.util.Optional;

/**
 * Where a link of a help page leads: a chapter, section or entry of a manual, a command's page, a manual's contents or
 * its index. Written into a link as text, and read back when the link is followed.
 *
 * @param kind   what kind of page it is
 * @param manual the manual's id, or empty for a command's page
 * @param id     the chapter's, section's or entry's id, the command's name, or empty
 */
public record HelpTarget(Kind kind, String manual, String id) {

    /** What separates the three parts of a target written out; no id or name of the series holds one. */
    private static final String JOINT = "|";

    public HelpTarget {
        manual = manual == null ? "" : manual;
        id = id == null ? "" : id;
    }

    /** A chapter, section or entry of a manual. */
    public static HelpTarget node(final String manual, final String id) {
        return new HelpTarget(Kind.NODE, manual, id);
    }

    /** A command's manual page. */
    public static HelpTarget command(final String name) {
        return new HelpTarget(Kind.COMMAND, "", name);
    }

    /** A manual's contents. */
    public static HelpTarget contents(final String manual) {
        return new HelpTarget(Kind.CONTENTS, manual, "");
    }

    /** A manual's index. */
    public static HelpTarget index(final String manual) {
        return new HelpTarget(Kind.INDEX, manual, "");
    }

    /** The target read back from a link, or nothing when the text is not one. */
    public static Optional<HelpTarget> read(final String written) {
        final String[] parts = written.split("\\|", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }
        for (final Kind kind : Kind.values()) {
            if (kind.word().equals(parts[0])) {
                return Optional.of(new HelpTarget(kind, parts[1], parts[2]));
            }
        }
        return Optional.empty();
    }

    /** The target written into a link. */
    public String written() {
        return this.kind.word() + JOINT + this.manual + JOINT + this.id;
    }

    /** What kind of page a target is. */
    public enum Kind {
        NODE("node"),
        COMMAND("command"),
        CONTENTS("contents"),
        INDEX("index");

        private final String word;

        Kind(final String word) {
            this.word = word;
        }

        /** How the kind is written into a link. */
        public String word() {
            return this.word;
        }
    }
}
