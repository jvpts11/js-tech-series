/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * An item held in use to charge and let go to act, as a bow is drawn: a railgun of J's Warfare, a charged scanner.
 * It is taken into use only when it is ready (it works, and is not waiting since its last use); let go, it acts with
 * how charged it was, from nothing to whole as its tool's charge time says, and when it acted it spends a use, heats
 * and starts its wait. Its mod says what it does ({@link #release}); the item is declared a tool with its charge,
 * power, heat and wait:
 *
 * <pre>{@code
 * CONTENT.item("railgun", Railgun::new).named("Railgun").holdsEnergy(500_000L, 5_000, 0)
 *         .tool(ToolSpec.builder().charges(40).cooldown(20).runsOn(ToolPower.energy(25_000))
 *                 .heats(new ToolHeat(100, 34, 5)).build())
 *         .register();
 * }</pre>
 */
public abstract class ChargingItem extends Item {

    /* How long it may be held in use: as long as a bow, far past any charge. */
    private static final int HELD_AT_MOST = 72_000;

    protected ChargingItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!Tools.ready(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return HELD_AT_MOST;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity user, final int timeLeft) {
        if (level.isClientSide() || !Tools.works(level, stack)) {
            return;
        }
        final float charge = Tools.charged(stack, getUseDuration(stack, user) - timeLeft);
        if (release(level, user, stack, charge)) {
            Tools.used(user, stack);
        }
    }

    /**
     * What it does let go, on the server, charged {@code charge} from nothing to whole; whether it acted, and so spent
     * a use. One let go too soon may do nothing and say so.
     */
    protected abstract boolean release(Level level, LivingEntity user, ItemStack stack, float charge);
}
