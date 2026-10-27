/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A Fieldbus Card's item. What every card says (what it draws, its slot, its era) J's Computers writes; the card adds
 * how many machines it reads, and where.
 */
@TextHolder
public class FieldbusCardItem extends Item {

    private final FieldbusCardSpec spec;

    /** How many machines the card reads. */
    public static final TextKey READS = TextKey.of("jsindustrial.item.fieldbus_card.reads",
            "Reads %s machines on the industrial control cable");

    public FieldbusCardItem(final Properties properties, final FieldbusCardSpec spec) {
        super(properties);
        this.spec = spec;
    }

    /** The card's sheet. */
    public FieldbusCardSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(GameText.component(READS.with(spec.machines())).withStyle(ChatFormatting.GRAY));
    }
}
