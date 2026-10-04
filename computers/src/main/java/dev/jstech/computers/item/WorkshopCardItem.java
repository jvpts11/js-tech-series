/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A personal-use card: the crafting table, furnace, enchanting table or anvil on an expansion card, which a Personal
 * Computer's Workshop program works with the player's own items.
 */
@TextHolder
public class WorkshopCardItem extends SpecItem<WorkshopCardSpec> implements IExpansionCardItem {

    private static final TextKey CRAFTING_TABLE = TextKey.of("jsc.item.workshop_card.crafting_table",
            "A crafting table, on a card");
    private static final TextKey FURNACE = TextKey.of("jsc.item.workshop_card.furnace",
            "A furnace that needs no fuel, on a card");
    private static final TextKey ENCHANTING = TextKey.of("jsc.item.workshop_card.enchanting",
            "An enchanting table with every bookshelf, on a card");
    private static final TextKey ANVIL = TextKey.of("jsc.item.workshop_card.anvil",
            "An anvil that never wears, on a card");
    private static final TextKey WORKS_IN = TextKey.of("jsc.item.workshop_card.works_in",
            "Used by the Workshop on a Personal Computer, from the Legacy on");
    private static final TextKey SLOTS = TextKey.of("jsc.item.workshop_card.slots", "PCI, AGP or PCI Express  -  %s W");

    public WorkshopCardItem(final Properties properties, final WorkshopCardSpec spec) {
        super(properties, spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final TextKey purpose = switch (spec().card()) {
            case CRAFTING_TABLE -> CRAFTING_TABLE;
            case FURNACE -> FURNACE;
            case ENCHANTING -> ENCHANTING;
            case ANVIL -> ANVIL;
        };
        tooltip.add(GameText.component(purpose).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(WORKS_IN).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(SLOTS.with(spec().tdpWatts())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
