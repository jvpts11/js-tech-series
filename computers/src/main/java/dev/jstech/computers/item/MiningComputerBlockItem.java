/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.MiningComputerTexts;
import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * The item of a Mining Computer (computers/docs/design/teracoin.md): its open frame in the slot, as it comes, with no
 * riser on its rails; a rig two blocks wide says so.
 */
public class MiningComputerBlockItem extends CabinetBlockItem {

    private final boolean wide;

    /*
     * A rig fits the slot by its frame: one block, or two side by side, its model spanning x -1.5..0.5 blocks around
     * its origin as the model's file has it (the second block east of the origin), so its middle moves by this much.
     */
    private static final Fit ONE_BLOCK = new Fit(16.0F, 0.0F, -0.5F, 0.0F);
    private static final Fit TWO_WIDE = new Fit(32.0F, 0.5F, -0.5F, 0.0F);
    /* The parts the slot leaves out, as the rig comes: its risers and their lamps. */
    private static final List<String> FITTED = List.of("riser_", "lamp_");

    public MiningComputerBlockItem(final Block block, final Item.Properties properties, final Era era) {
        super(block, properties, ComputingLooks.MINING_COMPUTER, ComputingLooks.miningComputer(era),
                RigGeometry.of(era).width() > RigGeometry.TRANSITION.width() ? TWO_WIDE : ONE_BLOCK, FITTED);
        this.wide = RigGeometry.of(era).width() > RigGeometry.TRANSITION.width();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        if (wide) {
            tooltip.add(GameText.component(MiningComputerTexts.TWO_WIDE).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
