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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a power supply's tooltip says: its rating and how efficient it is. */
@TextHolder
final class PowerSupplyTooltip {

    private static final TextKey RATING = TextKey.of("jsc.item.psu.rating", "%s W  -  %s%% efficient");

    private PowerSupplyTooltip() {
    }

    static void append(final PsuSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(RATING.with(spec.wattage(), spec.efficiencyPercent()))
                .withStyle(ChatFormatting.GRAY));
    }
}
