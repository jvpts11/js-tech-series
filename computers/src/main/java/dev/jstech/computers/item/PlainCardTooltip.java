/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What the tooltip of a card of a kind this mod does not know says: what every card says, what it draws and the slot
 * it fits. The mod that brings the kind says the rest on its own item.
 */
final class PlainCardTooltip {

    private PlainCardTooltip() {
    }

    static void append(final IExpansionCardSpec spec, final @Nullable Era era, final List<Component> tooltip) {
        tooltip.add(GameText.component(HardwareTooltip.WATTS.with(spec.tdpWatts())).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendSlot(tooltip, spec.bus());
        HardwareTooltip.appendEra(tooltip, era);
    }
}
