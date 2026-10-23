/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen.advancement;

import dev.jstech.core.datagen.advancement.AdvancementTab;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.api.IndustrialAdvancementTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Transcendence, T6 to T9 and the Aleph. Its tab opens on reaching the sixth tier. */
public final class TranscendenceAdvancements extends AdvancementTab {

    public TranscendenceAdvancements() {
        super(JsIndustrial.MODID, IndustrialAdvancementTab.TRANSCENDENCE.path(),
                ResourceLocation.withDefaultNamespace("textures/block/obsidian.png"));
        this.root(Items.END_CRYSTAL, "Transcendence", "Reach the sixth tier",
                reached(SeriesAxes.INDUSTRIAL_TIER, IndustrialTier.T6));
    }
}
