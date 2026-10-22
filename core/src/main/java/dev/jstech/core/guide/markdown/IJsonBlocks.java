/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import dev.jstech.core.guide.GuideBlock;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a block of a manual written as its file's JSON inside a Markdown file, a fence named for the kind of block:
 * a scene, or any block the Markdown has no shorter way to write. The game reads it as the manual's files are read;
 * a test with no game reads none.
 */
public interface IJsonBlocks {

    /** Reads no JSON at all. */
    IJsonBlocks NONE = new IJsonBlocks() {
        @Override
        public Set<String> kinds() {
            return Set.of();
        }

        @Override
        public Optional<GuideBlock> read(final String kind, final String json) {
            return Optional.empty();
        }
    };

    /** The kinds of block it reads, as a manual's file names them: {@code scene}, {@code views}. */
    Set<String> kinds();

    /** The block of {@code kind} that {@code json} writes, or none when it is not one. */
    Optional<GuideBlock> read(String kind, String json);
}
