/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a robot digs with whatever tool it carries, of any mod: it takes the one that digs a block fastest into its hand,
 * and digs at the pace a player would with it. Without a tool it digs with its own arm, as fast as a wooden pickaxe.
 */
public final class RobotTools {

    /** How fast a robot's own arm digs, as a tool's speed: a wooden pickaxe's. */
    public static final float ARM_SPEED = 2.0F;
    /* What a player's dig takes for each point of a block's hardness, with a tool of speed one. */
    private static final float TICKS_PER_HARDNESS_AT_SPEED_ONE = 30.0F;

    private RobotTools() {
    }

    /**
     * Puts the tool that digs {@code state} fastest of those {@code robot} holds and carries into its main hand, the
     * one it held going back into its inventory where the tool was; gives the tool in its hand, empty for its arm.
     */
    public static ItemStack takeBest(final RobotEntity robot, final BlockState state) {
        final ItemStack held = robot.getMainHandItem();
        float best = speed(held, state);
        int slot = -1;
        final SimpleContainer inventory = robot.inventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            final float each = speed(inventory.getItem(i), state);
            if (each > best) {
                best = each;
                slot = i;
            }
        }
        if (slot >= 0) {
            final ItemStack tool = inventory.getItem(slot);
            inventory.setItem(slot, held);
            robot.setItemInHand(InteractionHand.MAIN_HAND, tool);
        }
        return robot.getMainHandItem();
    }

    /** How many ticks digging {@code state}, of {@code hardness}, takes with {@code tool}. */
    public static int digTicks(final float hardness, final ItemStack tool, final BlockState state) {
        final float speed = Math.max(ARM_SPEED, speed(tool, state));
        return Math.max(1, (int) Math.ceil(hardness * TICKS_PER_HARDNESS_AT_SPEED_ONE / speed));
    }

    /* How fast {@code stack} digs {@code state}: nothing for a tool that would get nothing out of it. */
    private static float speed(final ItemStack stack, final BlockState state) {
        if (stack.isEmpty() || state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) {
            return 0.0F;
        }
        return stack.getDestroySpeed(state);
    }
}
