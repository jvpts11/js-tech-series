/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.List;

/**
 * Texts joined into one, such as the names of several items in a list, each still read in the language of whoever
 * reads the whole.
 *
 * <p>Pure: no game types.
 */
@TextHolder
public final class TextLists {

    /*
     * Two texts with something between them. It says nothing of its own, so every language keeps it as it is; it is a
     * sentence only so that what it joins travels, and is read, as text.
     */
    private static final TextKey JOINED = TextKey.of("jscore.text.joined", "%s%s%s");

    private TextLists() {
    }

    /**
     * {@code parts} in order, with {@code separator} between each two. The separator is data: punctuation such as a
     * comma or an arrow, the same in every language.
     *
     * <p>The parts are paired off in halves rather than one after another, so a long list nests only as deep as its
     * length's logarithm and still travels whole where the depth of a text is limited.
     */
    public static Text join(final String separator, final List<? extends Text> parts) {
        if (parts.isEmpty()) {
            return Text.EMPTY;
        }
        return join(separator, parts, 0, parts.size());
    }

    private static Text join(final String separator, final List<? extends Text> parts, final int from, final int to) {
        if (to - from == 1) {
            return parts.get(from);
        }
        final int middle = (from + to) >>> 1;
        return JOINED.with(join(separator, parts, from, middle), separator, join(separator, parts, middle, to));
    }
}
