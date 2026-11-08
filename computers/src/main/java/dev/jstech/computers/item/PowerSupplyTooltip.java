/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.PsuTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What a power supply's tooltip says: its rating and how efficient it is, and how worn it is, amber once degraded and
 * red once failed; a redundant supply's units each on a line.
 */
@TextHolder
final class PowerSupplyTooltip {

    private static final TextKey RATING = TextKey.of("jsc.item.psu.rating", "%s W  -  %s%% efficient");
    private static final TextKey REDUNDANT = TextKey.of("jsc.item.psu.redundant", "%s units, each carries the load");

    private PowerSupplyTooltip() {
    }

    static void append(final PsuSpec spec, final ItemStack stack, final List<Component> tooltip) {
        tooltip.add(GameText.component(RATING.with(spec.wattage(), spec.efficiencyPercent()))
                .withStyle(ChatFormatting.GRAY));
        if (spec.redundant()) {
            tooltip.add(GameText.component(REDUNDANT.with(spec.units())).withStyle(ChatFormatting.GRAY));
        }
        if (!ComputersServerConfig.psuFailures()) {
            return;
        }
        final PsuCondition condition = PsuConditions.of(stack);
        final ChatFormatting colour = condition.failed(spec) ? ChatFormatting.RED
                : condition.degraded(spec) ? ChatFormatting.GOLD : ChatFormatting.GRAY;
        for (final Text line : PsuTexts.state(spec, condition)) {
            tooltip.add(GameText.component(line).withStyle(colour));
        }
    }
}
