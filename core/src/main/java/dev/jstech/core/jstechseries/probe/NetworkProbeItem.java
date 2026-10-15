/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.text.GameText;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The network probe in hand: used on a wire of any line it links to that wire, and reads the part of the line it is
 * in; used on a part that turns, it reads the part's network of shafts; used in the air it is held up and its display
 * read. While held and on it reads every tick.
 */
public final class NetworkProbeItem extends Item {

    public NetworkProbeItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        if (player != null && MechanicalPorts.at(level, context.getClickedPos(), null) != null) {
            // A part that turns: the probe reads its network of shafts.
            if (level instanceof ServerLevel server) {
                context.getItemInHand().set(NetworkProbe.LINK.get(), new ProbeLink(server.dimension(),
                        context.getClickedPos().immutable(), NetworkProbe.MECHANICAL));
                player.displayClientMessage(GameText.component(NetworkProbe.LINKED_TO.with(
                        NetworkProbe.SHAFTS.text())), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof CableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        final Vec3 eye = player.getEyePosition();
        final Wire wire = cable.aim(eye, eye.add(player.getLookAngle().scale(player.blockInteractionRange() + 1.0)))
                .wire();
        if (wire == null || wire.type().id() == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        context.getItemInHand().set(NetworkProbe.LINK.get(), new ProbeLink(server.dimension(),
                context.getClickedPos().immutable(), wire.type().id()));
        player.displayClientMessage(GameText.component(NetworkProbe.LINKED_TO.with(GameText.of(
                wire.type().stack().getHoverName()))), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            NetworkProbe.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot,
                              final boolean selected) {
        if (entity instanceof ServerPlayer player && (selected || player.getOffhandItem() == stack)) {
            ProbeSessions.tick(player, stack);
        }
    }
}
