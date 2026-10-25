/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.crafting.CraftingCards;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What a Crafting Card's tooltip says: the accelerator a Crafting Computer needs to execute recipes, the interfaces it
 * drives, and the bench recipes its own ROM keeps, which travel with the card.
 */
@TextHolder
final class CraftingCardTooltip {

    private static final TextKey PURPOSE = TextKey.of("jsc.item.crafting_card.purpose", "Crafting accelerator (FPGA)");
    private static final TextKey THREADS_ONE =
            TextKey.of("jsc.item.crafting_card.threads_one", "Threads  -  %s thread");
    private static final TextKey THREADS_MANY =
            TextKey.of("jsc.item.crafting_card.threads_many", "Threads  -  %s threads");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.item.crafting_card.throughput", "Throughput  -  %sx CPU");
    private static final TextKey DRIVES = TextKey.of("jsc.item.crafting_card.drives",
            "Drives %s Crafting Interfaces");
    private static final TextKey ROM = TextKey.of("jsc.item.crafting_card.rom", "ROM  -  %s of %s bench recipes");
    private static final TextKey ROM_ENTRY = TextKey.of("jsc.item.crafting_card.rom_entry", "  - %s");
    /** The most recipes the tooltip names before it says how many more there are. */
    private static final int LISTED = 8;
    private static final TextKey MORE = TextKey.of("jsc.item.crafting_card.more", "  and %s more");

    private CraftingCardTooltip() {
    }

    static void append(final CraftingCardSpec spec, final ItemStack stack, final List<Component> tooltip) {
        tooltip.add(GameText.component(PURPOSE).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component((spec.threads() == 1 ? THREADS_ONE : THREADS_MANY).with(spec.threads()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(THROUGHPUT.with(spec.cpuFactor())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(DRIVES.with(spec.interfaces())).withStyle(ChatFormatting.GRAY));
        final List<CraftingPattern> held = CraftingCards.rom(stack);
        tooltip.add(GameText.component(ROM.with(held.size(), spec.romSize())).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(LISTED, held.size()); i++) {
            tooltip.add(GameText.component(ROM_ENTRY.with(held.get(i).displayText()))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (held.size() > LISTED) {
            tooltip.add(GameText.component(MORE.with(held.size() - LISTED)).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(GameText.component(HardwareTooltip.TIER_POWER_BUS.with(spec.tier().text(), spec.tdpWatts(),
                spec.bus().label())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
