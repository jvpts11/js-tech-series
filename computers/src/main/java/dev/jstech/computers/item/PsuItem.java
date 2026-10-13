/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A PSU item.
 */
@TextHolder
public class PsuItem extends SpecItem<PsuSpec> {

    private static final TextKey RATING = TextKey.of("jsc.item.psu.rating", "%s W  -  %s%% efficient");

    public PsuItem(final Properties properties, final PsuSpec spec) {
        super(properties, spec);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final PsuSpec spec = spec();
        tooltip.add(GameText.component(RATING.with(spec.wattage(), spec.efficiencyPercent()))
                .withStyle(ChatFormatting.GRAY));
    }
}
