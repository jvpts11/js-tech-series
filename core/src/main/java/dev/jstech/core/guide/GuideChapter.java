/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Objects;

/**
 * A mod's chapter: every section and entry the mod declares, under its title, with the colour of its tab at the
 * edge of the manual and a paragraph that opens it.
 *
 * @param namespace the mod's id, which every section and entry of the chapter shares
 * @param order     where the chapter sits among the chapters of a manual holding several, smallest first
 * @param titleKey  its title's sentence, such as "J's Computers"
 * @param tab       the colour of its tab: the id of a declared palette whose role {@code tab} it is, so a resource
 *                  pack can recolour it, or a colour written {@code #AARRGGBB}; empty for the style's own
 * @param aboutKey  the paragraph on its opening page, saying what the chapter is about, or empty
 */
public record GuideChapter(String namespace, int order, String titleKey, String tab, String aboutKey) {

    public GuideChapter {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(titleKey, "titleKey");
        tab = tab == null ? "" : tab;
        aboutKey = aboutKey == null ? "" : aboutKey;
    }
}
