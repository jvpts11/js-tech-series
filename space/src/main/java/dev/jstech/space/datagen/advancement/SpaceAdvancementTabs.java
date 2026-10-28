/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's advancement tabs, in the order the advancements screen lists them: leaving the Earth, then each world in
 * the order it is reached. Every tab stands on the night sky, which a player's game draws once and wide behind the
 * tree.
 */
public final class SpaceAdvancementTabs {

    /** The night sky's 16-pixel tile, which every tab names as its background. */
    public static final ResourceLocation NIGHT_SKY = ResourceLocation.parse(
            "jsspace:textures/gui/advancements/sky.png");

    private SpaceAdvancementTabs() {
    }

    /** A fresh set of the tabs, written down again: each provider reads its own. */
    public static List<AdvancementTab> all() {
        return List.of(new LeavingTheEarthAdvancements(), new TheMoonAdvancements(), new MarsAdvancements());
    }

    /** Every title and description of every tab, for the language file. */
    public static void translations(final BiConsumer<String, String> add) {
        for (final AdvancementTab tab : all()) {
            tab.translations(add);
        }
    }
}
