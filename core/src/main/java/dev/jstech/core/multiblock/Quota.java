/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Objects;

/**
 * A count a structure limits, as the tools that build it need it: its name, which blocks are counted under it, and
 * the least and the most of them the structure takes ("exactly one power entry"). Pure.
 *
 * @param name   the count's name, as the shape's limit names it
 * @param counts which blocks are counted under it, by id
 * @param limit  the least and the most of them
 */
public record Quota(String name, IBlockMatcher counts, Limit limit) {

    public Quota {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(limit, "limit");
    }
}
