/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.man;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.ModGuide;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The entries of the manuals every mod of the series declares, as {@code man} finds them at a prompt: by the last part
 * of the entry's id ("graphics_cards", or "graphics-cards") or by its English title.
 *
 * <p>The machine only decides that there is such an entry; the page itself is laid out on the player's side, from the
 * manuals in the player's game and in the player's language.
 */
public final class ManualEntries {

    /** The characters a typed name may spell a gap with, all read alike. */
    private static final Pattern GAPS = Pattern.compile("[-_\\s]+");

    private ManualEntries() {
    }

    /** The id of the entry a word names, when one of the manuals has it. */
    public static Optional<String> find(final String asked) {
        final String wanted = loose(asked);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        for (final ModContent content : ModContent.all()) {
            for (final ModGuide guide : content.declaredGuides()) {
                final Map<String, String> english = guide.translations();
                for (final GuideEntry entry : guide.declaredEntries()) {
                    if (loose(GuideIds.path(entry.id())).equals(wanted)
                            || loose(english.getOrDefault(entry.titleKey(), "")).equals(wanted)) {
                        return Optional.of(entry.id());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static String loose(final String name) {
        return GAPS.matcher(name.strip().toLowerCase(Locale.ROOT)).replaceAll("_");
    }
}
