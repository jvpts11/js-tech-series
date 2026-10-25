/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.NetworkCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a network adapter's tooltip says, by what it gives the machine it sits in: the fibre of the backbone. */
@TextHolder
final class NetworkCardTooltip {

    private static final TextKey FIBRE =
            TextKey.of("jsc.item.network_card.fibre", "Lets a Mainframe, a rack or a CMC take the fibre backbone");

    private NetworkCardTooltip() {
    }

    static void append(final NetworkCardSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(FIBRE).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendSlot(tooltip, spec.bus());
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
