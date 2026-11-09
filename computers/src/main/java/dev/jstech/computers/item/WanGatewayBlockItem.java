/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * Block item for the WAN Gateway Computer: the cabinet's own model in the slot, as it comes with no card in its line
 * bay, plus the machine's tooltip.
 */
@TextHolder
public class WanGatewayBlockItem extends CabinetBlockItem {

    /**
     * How a gateway's cabinet sits in an item slot: 4 blocks wide is its longest side, and its model spans x -1.5..2.5,
     * y 0..2, z -0.5..1.5 blocks around the origin, which is the second block of the bottom front row, so its middle
     * moves by this much.
     */
    private static final Fit GATEWAY_FIT = new Fit(64.0F, -0.5F, -1.0F, -0.5F);
    /*
     * The parts shown only when fitted or lit: the disks' lamps, the run, network and fault lamps, the light bars, and
     * a line module with its lamps where a card is in; the slot shows each port's blank.
     */
    private static final List<String> FITTED = List.of("disk_", "lamp_", "lightbar_", "line_1", "line_2", "line_3",
            "line_4", "line_ok_", "line_alarm_");
    private static final TextKey TOOLTIP =
            TextKey.of("item.jsc.wan_gateway.tooltip", "Forms a 4x2x2 structure when placed");

    public WanGatewayBlockItem(final Block block, final Item.Properties properties, final String model) {
        super(block, properties, ComputingLooks.WAN_GATEWAY, model, GATEWAY_FIT, FITTED);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(TOOLTIP).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
