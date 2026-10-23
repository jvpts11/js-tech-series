/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.api;

import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * The four tabs of J's Industrial's advancements, by the phases of its industry. An add-on hangs an advancement of its
 * own under one of them by naming the tab's root, or any advancement in it, as its parent.
 *
 * <pre>{@code
 * Advancement.Builder.advancement().parent(AdvancementSubProvider.createPlaceholder(
 *         IndustrialAdvancementTab.FOUNDATION.root().toString()))
 * }</pre>
 */
public enum IndustrialAdvancementTab {

    /** T0 to T2: the first machines, power, steel, chemistry. */
    FOUNDATION("foundation"),
    /** T3 to T5: multiblocks, the grid, the Clean Room, nuclear power. */
    INDUSTRIALISATION("industrialisation"),
    /** Research and accelerators, one advancement for each major discovery. */
    DISCOVERY("discovery"),
    /** T6 to T9 and the Aleph. */
    TRANSCENDENCE("transcendence");

    private final String path;

    IndustrialAdvancementTab(final String path) {
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
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, this.path + "/" + name);
    }
}
