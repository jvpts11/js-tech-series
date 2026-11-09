/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.AnchorCardSpec;
import dev.jstech.core.region.ChunkQuotas;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a Network Anchor Card's tooltip says: its era, the chunks it keeps loaded round its computer, how many of their
 * quota the player who reads it keeps loaded already, and what it draws.
 */
@TextHolder
final class AnchorCardTooltip {

    private static final TextKey OWN_CHUNK =
            TextKey.of("jsc.item.anchor_card.own_chunk", "Keeps this computer's chunk loaded");
    private static final TextKey SQUARE = TextKey.of("jsc.item.anchor_card.square", "Keeps %s x %s chunks loaded");
    private static final TextKey ROUND = TextKey.of("jsc.item.anchor_card.round", "round this computer");
    private static final TextKey QUOTA = TextKey.of("jsc.item.anchor_card.quota", "Your quota: %s of %s chunks");
    private static final TextKey DRAWS = TextKey.of("jsc.item.anchor_card.draws", "Draws %s W");

    private AnchorCardTooltip() {
    }

    static void append(final AnchorCardSpec spec, final List<Component> tooltip) {
        HardwareTooltip.appendEra(tooltip, spec.era());
        if (spec.reach() == 0) {
            tooltip.add(GameText.component(OWN_CHUNK).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(GameText.component(SQUARE.with(spec.side(), spec.side())).withStyle(ChatFormatting.AQUA));
            tooltip.add(GameText.component(ROUND).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(GameText.component(QUOTA.with(ChunkQuotas.held(), ChunkQuotas.limit()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(DRAWS.with(spec.tdpWatts())).withStyle(ChatFormatting.GRAY));
    }
}
