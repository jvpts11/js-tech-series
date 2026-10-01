/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import java.util.List;

/** The comment lines a file carries above each place in it, which a format writes down when it has room for them. */
@FunctionalInterface
public interface IConfigComments {

    /** A file with no comments at all. */
    IConfigComments NONE = path -> List.of();

    /**
     * The lines to write above the value or the section at {@code path}, each without the format's comment mark; the
     * empty path is the top of the file. No lines means no comment.
     */
    List<String> at(List<String> path);
}
