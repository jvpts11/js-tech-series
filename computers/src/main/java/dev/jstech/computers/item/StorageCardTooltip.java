/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What a storage card's tooltip says: the Vaultis Array, with the RAID mode set in its server's firmware and kept on
 * the card, or the Vaultis Cache, with how much of its drives' read latency it takes away.
 */
@TextHolder
final class StorageCardTooltip {

    private static final TextKey ARRAY = TextKey.of("jsc.item.storage_card.array",
            "RAID controller: joins its server's drives into one volume");
    private static final TextKey CACHE = TextKey.of("jsc.item.storage_card.cache",
            "Read cache: answers its server's drives faster");
    private static final TextKey UNCONFIGURED = TextKey.of("jsc.item.storage_card.unconfigured",
            "No array set: the drives stay separate (set one in the server's firmware)");
    private static final TextKey NEEDS = TextKey.of("jsc.item.storage_card.needs", "%s  -  needs %s drives or more");
    private static final TextKey CUT = TextKey.of("jsc.item.storage_card.cut", "-%s%% read latency on its drives");
    private static final TextKey SLOT = TextKey.of("jsc.item.storage_card.slot", "%s slot  -  %s W");

    private StorageCardTooltip() {
    }

    static void append(final StorageCardSpec spec, final ItemStack stack, final List<Component> tooltip) {
        if (spec.kind() == ExpansionCardKind.RAID_CONTROLLER) {
            tooltip.add(GameText.component(ARRAY).withStyle(ChatFormatting.GRAY));
            final RaidMode mode = StorageCards.raidMode(stack);
            tooltip.add(GameText.component(mode == RaidMode.NONE ? UNCONFIGURED.text()
                            : NEEDS.with(Text.literal(mode.name()), mode.minDrives()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(GameText.component(CACHE).withStyle(ChatFormatting.GRAY));
            tooltip.add(GameText.component(CUT.with(spec.readCutPercent())).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(GameText.component(SLOT.with(spec.bus().label(), spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
