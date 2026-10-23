/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.api.IndustrialAdvancementTab;
import dev.jstech.industrial.discovery.ElementDiscovery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Discovery: research and accelerators, one advancement for each major discovery. Its tab opens with a team's first
 * discovery, which the mod reports as its event {@code discovery_made}: the first synthesis of an element among them
 * ({@link ElementDiscovery}).
 */
public final class DiscoveryAdvancements extends AdvancementTab {

    /** The mod's event of a discovery made, whose first opens the tab. */
    public static final String DISCOVERY_MADE = ElementDiscovery.DISCOVERY_MADE.getPath();

    public DiscoveryAdvancements() {
        super(JsIndustrial.MODID, IndustrialAdvancementTab.DISCOVERY.path(),
                ResourceLocation.withDefaultNamespace("textures/block/lapis_block.png"));
        this.root(Items.SPYGLASS, "Discovery", "Make your first discovery", on(DISCOVERY_MADE));
    }
}
