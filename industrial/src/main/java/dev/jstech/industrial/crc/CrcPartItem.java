/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A part that goes in a place of a Computational Research Centre: a processor, or what cools them. */
public class CrcPartItem extends Item {

    private final CrcPart part;

    /* Watts to the kilowatts a tooltip says. */
    private static final double KILO = 1_000.0;

    public CrcPartItem(final Properties properties, final CrcPart part) {
        super(properties);
        this.part = Objects.requireNonNull(part, "part");
    }

    /** The part it is. */
    public CrcPart part() {
        return this.part;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        final Text tier = this.part.tier().text();
        final String kilowatts = String.valueOf(Math.round(this.part.watts() / KILO));
        final Text says = switch (this.part.cooling()) {
            case NONE -> CrcTexts.PROCESSOR.with(tier, String.valueOf(Math.round(this.part.research())), kilowatts);
            case AIR -> CrcTexts.HEAT_SINK.with(tier, kilowatts);
            case WATER -> CrcTexts.LIQUID_COOLER.with(tier, kilowatts);
            case HEAT_PIPE -> CrcTexts.HEAT_PIPE_COOLER.with(tier, kilowatts);
        };
        lines.add(GameText.component(says).withStyle(ChatFormatting.GRAY));
        lines.add(GameText.component(CrcTexts.GOES_IN).withStyle(ChatFormatting.DARK_GRAY));
    }
}
