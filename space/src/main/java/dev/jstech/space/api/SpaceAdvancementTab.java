/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;

/**
 * The tabs of J's Space's advancements: leaving the Earth, then a tab for each world reached. An add-on hangs an
 * advancement of its own under one of them by naming the tab's root, or any advancement in it, as its parent.
 *
 * <pre>{@code
 * Advancement.Builder.advancement().parent(AdvancementSubProvider.createPlaceholder(
 *         SpaceAdvancementTab.THE_MOON.root().toString()))
 * }</pre>
 */
public enum SpaceAdvancementTab {

    /** The first rockets, satellites, crews, walks and dockings, and the stages that land again. */
    LEAVING_THE_EARTH("leaving_the_earth"),
    /** The Moon: seeing the Earth rise over it, landing, driving, building a station. */
    THE_MOON("the_moon"),
    /** Mars: landing through its thin air, its blue sunsets, its water. */
    MARS("mars");

    private final String path;

    SpaceAdvancementTab(final String path) {
        this.path = path;
    }

    /** The tab's name, which the ids of its advancements start with. */
    public String path() {
        return this.path;
    }

    /** The tab's root, which every other advancement of the tab hangs from. */
    public ResourceLocation root() {
        return advancement("root");
    }

    /** The advancement of the tab called {@code name}. */
    public ResourceLocation advancement(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, this.path + "/" + name);
    }
}
