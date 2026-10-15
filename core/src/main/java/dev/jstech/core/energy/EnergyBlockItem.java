/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * The item of a block with a port of an energy, whose tooltip names the class of its port, as every such block's does:
 * "Port: Low voltage, 400 V".
 */
public class EnergyBlockItem extends BlockItem {

    private final Energy energy;
    private final EnergyClass portClass;

    public EnergyBlockItem(final Block block, final Properties properties, final Energy energy,
                           final EnergyClass portClass) {
        super(block, properties);
        this.energy = Objects.requireNonNull(energy, "energy");
        this.portClass = Objects.requireNonNull(portClass, "portClass");
    }

    /** The energy of its block's port. */
    public Energy energy() {
        return this.energy;
    }

    /** The class of its block's port. */
    public EnergyClass portClass() {
        return this.portClass;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        EnergyTooltips.port(this.energy, this.portClass).ifPresent(line ->
                lines.add(GameText.component(line).withStyle(ChatFormatting.GRAY)));
    }
}
