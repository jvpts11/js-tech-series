/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hazard;

import dev.jstech.core.hazard.DamageKind;
import dev.jstech.core.hazard.HazardTexts;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.text.GameText;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a tooltip says of an item's hazards and resistances: each hazard it carries and what protects from it, in red,
 * and each kind of harm it stops, worn, with how much, in blue.
 */
public final class HazardTooltips {

    private HazardTooltips() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, HazardTooltips::onTooltip);
    }

    private static void onTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        for (final ItemHazards.Kind kind : ItemHazards.of(stack).keySet()) {
            event.getToolTip().add(GameText.component(HazardTexts.TOOLTIP_HAZARD.with(kind.name().text(),
                    kind.guard().text())).copy().withStyle(ChatFormatting.RED));
        }
        for (final Map.Entry<DamageKind, Double> resistance : Resistances.of(stack).entrySet()) {
            event.getToolTip().add(GameText.component(HazardTexts.TOOLTIP_RESISTANCE.with(
                    resistance.getKey().name().text(), Math.round(resistance.getValue() * 100)))
                    .copy().withStyle(ChatFormatting.BLUE));
        }
    }
}
