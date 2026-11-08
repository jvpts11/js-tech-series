/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What the tooltip of a processor's cooler or a case fan says: what it takes away, what a water cooler's radiator
 * takes from the case, and the era it is made in (computers/docs/design/temperature.md).
 */
@TextHolder
final class CoolingTooltip {

    static final TextKey TAKES_AWAY = TextKey.of("jsc.item.cooling.takes_away", "Takes away %s W");
    private static final TextKey OVER_PROCESSOR = TextKey.of("jsc.item.cooling.over_processor",
            "Sits over a processor's socket");
    private static final TextKey RADIATOR = TextKey.of("jsc.item.cooling.radiator",
            "Its radiator takes two of the case's fan seats: the top's, or the front's");
    private static final TextKey FAN_SIZE = TextKey.of("jsc.item.cooling.fan_size", "%s mm, for a case's fan seat");
    private static final TextKey FITS = TextKey.of("jsc.item.cooling.fits", "Fits a case of its era or a later one");

    private CoolingTooltip() {
    }

    static void append(final CoolerSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(TAKES_AWAY.with(spec.takesAwayWatts())).withStyle(ChatFormatting.AQUA));
        tooltip.add(GameText.component(OVER_PROCESSOR).withStyle(ChatFormatting.GRAY));
        if (spec.radiatorFans() > 0) {
            tooltip.add(GameText.component(RADIATOR).withStyle(ChatFormatting.GRAY));
        }
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(FITS).withStyle(ChatFormatting.DARK_GRAY));
    }

    static void append(final CaseFanSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(TAKES_AWAY.with(spec.takesAwayWatts())).withStyle(ChatFormatting.AQUA));
        tooltip.add(GameText.component(FAN_SIZE.with(spec.sizeMm())).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(FITS).withStyle(ChatFormatting.DARK_GRAY));
    }
}
