/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A repair kit ready to register: used on a damaged machine and held, it gives back {@code perSecond} health a second
 * while the player keeps it on the machine, one use of the kit for each point given back, until the machine is whole
 * or the kit is spent.
 */
public class RepairKitItem extends Item implements IRepairKit {

    private final int perSecond;

    /* The longest a kit is held, in ticks: until it is let go, the machine is whole, or the kit is spent. */
    private static final int HELD_UP_TO = 72_000;
    private static final int TICKS_A_SECOND = 20;

    /** A kit of {@code uses} points of health, giving back {@code perSecond} a second. */
    public RepairKitItem(final Properties properties, final int uses, final int perSecond) {
        super(properties.durability(uses));
        if (perSecond <= 0) {
            throw new IllegalArgumentException("a repair kit gives back some health a second");
        }
        this.perSecond = perSecond;
    }

    @Override
    public int repairsPerSecond(final ItemStack stack) {
        return this.perSecond;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineBlockEntity machine) || !machine.health().damaged()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return HELD_UP_TO;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.BRUSH;
    }

    /* Each tick it is held on a damaged machine, it gives back what a tick's share of its rate comes to. */
    @Override
    public void onUseTick(final Level level, final LivingEntity entity, final ItemStack stack, final int left) {
        if (!(level instanceof ServerLevel) || !(entity instanceof Player player)) {
            return;
        }
        final BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK || !(level.getBlockEntity(hit.getBlockPos())
                instanceof MachineBlockEntity machine) || !machine.health().damaged()) {
            player.stopUsingItem();
            return;
        }
        final int held = HELD_UP_TO - left;
        final int rate = repairsPerSecond(stack);
        final int points = (held + 1) * rate / TICKS_A_SECOND - held * rate / TICKS_A_SECOND;
        if (points <= 0) {
            return;
        }
        final int given = machine.repair(points);
        if (given > 0) {
            stack.hurtAndBreak(given, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
        if (!machine.health().damaged() || stack.isEmpty()) {
            player.stopUsingItem();
        }
    }
}
