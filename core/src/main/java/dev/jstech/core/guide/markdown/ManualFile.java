/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.util.Optional;

/**
 * A manual's Markdown file as a mod or a resource pack ships it,
 * {@code assets/<namespace>/jstech/manuals/<folder>/<language>/<path>.md}.
 *
 * @param namespace the namespace it is under
 * @param folder    the manual's folder it is in
 * @param language  the language it is written in, as the game names it: {@code en_us}, {@code pt_br}
 * @param path      where it is in the language's folder, with no {@code .md}: {@code geiger_counter},
 *                  {@code hazards/geiger_counter}
 * @param text      what it says
 */
public record ManualFile(String namespace, String folder, String language, String path, String text) {

    /** The folder every manual's files are in, under a namespace's assets. */
    public static final String ROOT = "jstech/manuals";
    /** The language a file falls back on when the reader's has none. */
    public static final String ENGLISH = "en_us";
    public static final String EXTENSION = ".md";

    /** Where it is, as the log names it. */
    public String where() {
        return "assets/" + this.namespace + "/" + ROOT + "/" + this.folder + "/" + this.language + "/" + this.path
                + EXTENSION;
    }

    /** Its name, the last part of its path. */
    public String name() {
        return this.path.substring(this.path.lastIndexOf('/') + 1);
    }

    /**
     * The file at {@code location}, a path under a namespace's assets as a resource pack lists it,
     * {@code jstech/manuals/<folder>/<language>/<path>.md}, saying {@code text}; none when it is not one.
     */
    public static Optional<ManualFile> at(final String namespace, final String location, final String text) {
        final String prefix = ROOT + "/";
        if (!location.startsWith(prefix) || !location.endsWith(EXTENSION)) {
            return Optional.empty();
        }
        final String[] parts = location.substring(prefix.length(), location.length() - EXTENSION.length()).split("/",
                3);
        if (parts.length < 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ManualFile(namespace, parts[0], parts[1], parts[2], text));
    }
}
