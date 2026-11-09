/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.hardware.WanCardSpec;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a WAN Interface Card's tooltip says: that it is one, of which era, the line it takes and how fast that line
 * goes, that it takes the older lines too, and that only a WAN Gateway Computer seats it.
 */
@TextHolder
final class WanCardTooltip {

    private static final TextKey KIND = TextKey.of("jsc.item.wan_card.kind", "WAN Interface Card, %s");
    private static final TextKey LINE = TextKey.of("jsc.item.wan_card.line", "Line: %s, %s it/t");
    private static final TextKey OLDER = TextKey.of("jsc.item.wan_card.older", "Takes the older lines too");
    private static final TextKey ONLY = TextKey.of("jsc.item.wan_card.only", "Only in a WAN Gateway");

    private WanCardTooltip() {
    }

    static void append(final WanCardSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(KIND.with(spec.era().named())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(LINE.with(DataLinkNames.of(spec.line()), spec.itemsPerTick()))
                .withStyle(ChatFormatting.AQUA));
        // The Vintage's telephone line is the first, so its modem has no older line to take.
        if (spec.era().isAtLeast(HardwareEra.LEGACY)) {
            tooltip.add(GameText.component(OLDER).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(GameText.component(ONLY).withStyle(ChatFormatting.GOLD));
    }
}
