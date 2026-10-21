/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.item.ItemMode;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.text.GameText;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The physics tool's own item: used on a moving structure it grabs it while the button is held, pushes it, or freezes
 * and frees it, as its mode says; see {@link PhysicsTool}.
 */
public final class PhysicsToolItem extends Item {

    /* The most it holds with and pushes with, at its whole strength: newtons, and newton seconds. */
    private static final double MOST_HOLD = 600_000;
    private static final double MOST_PUSH = 150_000;
    /* How long a hold lasts once it is not renewed, and the time between two pushes, in ticks. */
    private static final int HOLD_TICKS = 3;
    private static final int PUSH_EVERY = 8;
    /* How long the button may be held, in ticks: as long as the player likes. */
    private static final int HELD_FOR = 72_000;
    private static final double HUNDRED = 100;

    public PhysicsToolItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final PhysicsTool.Aim aim = PhysicsTool.aim(player);
        if (aim == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(GameText.component(PhysicsTool.NOTHING_AIMED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        final ItemMode mode = PhysicsTool.mode(stack);
        if (mode.equals(PhysicsTool.GRAB_MODE)) {
            PhysicsTool.grab(player, aim);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide() && aim.body() instanceof MovingStructure structure) {
            if (mode.equals(PhysicsTool.PUSH_MODE)) {
                MovingStructures.push(structure, player.getLookAngle().scale(MOST_PUSH * share(stack)), aim.at());
                player.getCooldowns().addCooldown(this, PUSH_EVERY);
            } else {
                final boolean freeze = !MovingStructures.frozen(structure);
                MovingStructures.freeze(structure, freeze);
                player.displayClientMessage(GameText.component((freeze ? PhysicsTool.FROZEN : PhysicsTool.FREED)
                        .with(structure.entity().getName().getString())), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity user, final ItemStack stack, final int left) {
        if (!(user instanceof Player player)) {
            return;
        }
        final PhysicsTool.Grab grab = PhysicsTool.grabbed(player);
        if (grab == null || !grab.body().ready() || grab.body().level() != level
                || grab.body() instanceof MovingStructure gone && gone.entity().isRemoved()) {
            player.releaseUsingItem();
            return;
        }
        if (!level.isClientSide() && grab.body() instanceof MovingStructure structure) {
            final Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(grab.distance()));
            MovingStructures.hold(structure, grab.place(), target, MOST_HOLD * share(stack),
                    level.getGameTime() + HOLD_TICKS);
        }
    }

    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity user, final int left) {
        if (user instanceof Player player) {
            final PhysicsTool.Grab grab = PhysicsTool.letGo(player);
            if (!level.isClientSide() && grab != null && grab.body() instanceof MovingStructure structure) {
                MovingStructures.letGo(structure);
            }
        }
    }

    @Override
    public void onStopUsing(final ItemStack stack, final LivingEntity user, final int left) {
        releaseUsing(stack, user.level(), user, left);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity user) {
        return HELD_FOR;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }

    /* The tool's strength in {@code stack}, as a share of its whole. */
    private static double share(final ItemStack stack) {
        return PhysicsTool.strength(stack) / HUNDRED;
    }
}
