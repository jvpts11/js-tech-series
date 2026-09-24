/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A RAM module item.
 */
@TextHolder
public class RamItem extends SpecItem<RamSpec> {

    private static final TextKey BUFFER = TextKey.of("jsc.item.ram.buffer", "%s items buffer  -  %s");

    public RamItem(final Properties properties, final RamSpec spec) {
        super(properties, spec);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final RamSpec spec = spec();
        tooltip.add(GameText.component(BUFFER.with(spec.bufferItems(), spec.generation()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(HardwareTooltip.WATTS.with(spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
