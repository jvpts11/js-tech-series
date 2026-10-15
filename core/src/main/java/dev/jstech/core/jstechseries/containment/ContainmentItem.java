/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import dev.jstech.core.energy.EnergyBlockItem;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A containment's item: its port's class, and what it kept when it was broken, held still. */
public final class ContainmentItem extends EnergyBlockItem {

    public ContainmentItem(final Block block, final Properties properties) {
        super(block, properties, JsEnergy.ENERGY, JsEnergy.LOW);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        final MatterStack kept = stack.get(SharedContainment.CONTAINED.get());
        if (kept != null) {
            lines.add(GameText.component(SharedContainment.holds(kept)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        super.appendHoverText(stack, context, lines, flag);
    }
}
