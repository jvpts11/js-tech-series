/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ExpansionBus;
import dev.jstech.computers.hardware.WifiCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a Wi-Fi card's tooltip says (computers/docs/design/wireless.md): its slot and the Wi-Fi it speaks, how fast it
 * goes and that the access point's speed wins when that is slower, and which computers take it. It draws nothing, so
 * no draw is given.
 */
@TextHolder
final class WifiCardTooltip {

    private static final TextKey SLOT = TextKey.of("jsc.item.wifi_card.slot", "%s · %s");
    private static final TextKey EXPRESS = TextKey.of("jsc.item.wifi_card.express", "PCI Express x1");
    private static final TextKey SPEED = TextKey.of("jsc.item.wifi_card.speed",
            "Up to %s items per tick, or the access point's speed if that is slower");
    private static final TextKey TAKEN_BY = TextKey.of("jsc.item.wifi_card.taken_by",
            "Personal, Crafting and Cluster Management Computers");

    private WifiCardTooltip() {
    }

    static void append(final WifiCardSpec spec, final List<Component> tooltip) {
        final Text slot = spec.bus().family() == ExpansionBus.PCI ? spec.bus().label() : EXPRESS.text();
        tooltip.add(GameText.component(SLOT.with(slot, spec.standard().cardName())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(SPEED.with(spec.standard().itemsPerTick())).withStyle(ChatFormatting.AQUA));
        tooltip.add(GameText.component(TAKEN_BY).withStyle(ChatFormatting.GRAY));
    }
}
