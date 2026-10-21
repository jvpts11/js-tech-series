/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * A vehicle as an item: used on the ground, it is placed there facing the way its placer looks, theirs, and their
 * key cut for it when its kind has keys. A vehicle picked up carries all it was and held, its parts, fuel and load, and
 * is placed again as it was.
 */
public class VehicleItem extends Item {

    public VehicleItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getItemInHand();
        final VehicleStack carried = stack.get(Vehicles.VEHICLE.get());
        if (carried == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        final Vec3 at = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        final float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0;
        final ServerPlayer placer = context.getPlayer() instanceof ServerPlayer player ? player : null;
        final Optional<VehicleEntity> made = Vehicles.make(level, carried.vehicle(), at, yaw, carried.variant(),
                placer);
        if (made.isEmpty()) {
            return InteractionResult.FAIL;
        }
        final VehicleEntity vehicle = made.get();
        if (carried.state().isPresent()) {
            vehicle.loadState(carried.state().get());
            vehicle.moveTo(at.x, at.y, at.z, yaw, 0);
        } else if (placer != null) {
            final ItemStack key = Vehicles.keyFor(vehicle);
            if (!key.isEmpty()) {
                placer.getInventory().placeItemBackInInventory(key);
            }
        }
        stack.consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final VehicleStack carried = stack.get(Vehicles.VEHICLE.get());
        if (carried == null) {
            return super.getName(stack);
        }
        return Vehicles.anywhere(carried.vehicle()).<Component>map(def -> Component.translatable(def.name()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        final VehicleStack carried = stack.get(Vehicles.VEHICLE.get());
        if (carried != null && carried.state().isPresent()) {
            lines.add(GameText.component(VehicleTexts.CARRIES_STATE));
        }
    }
}
