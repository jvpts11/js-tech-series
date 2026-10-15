/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.EnergyReadings;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * The clamp meter in hand: used on a wire of an energy's line it clamps on that wire, on a block that holds energy it
 * clamps on the block; used in the air it is held up and its display read. While held it reads every tick.
 */
public final class ClampMeterItem extends Item {

    public ClampMeterItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.SUCCESS;
        }
        final BlockPos pos = context.getClickedPos();
        final Optional<MeterClamp> clamp = clampAt(server, pos, player);
        if (clamp.isEmpty()) {
            return InteractionResult.PASS;
        }
        context.getItemInHand().set(ClampMeter.CLAMP.get(), clamp.get());
        final Component name = clamp.get().cable().flatMap(CoreCables.REGISTRY::getOptional)
                .map(type -> type.stack().getHoverName())
                .orElseGet(() -> level.getBlockState(pos).getBlock().getName());
        player.displayClientMessage(GameText.component(ClampMeter.CLAMPED_ONTO.with(GameText.of(name))), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            ClampMeter.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot,
                              final boolean selected) {
        if (entity instanceof ServerPlayer player && (selected || player.getOffhandItem() == stack)) {
            MeterSessions.tick(player, stack);
        }
    }

    /*
     * What the meter clamps on at {@code pos}: the wire of an energy's line the player aims at in a cable block (or
     * the block's first such wire), or a block that holds energy; none for anything else.
     */
    private static Optional<MeterClamp> clampAt(final ServerLevel level, final BlockPos pos, final Player player) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            final Vec3 eye = player.getEyePosition();
            final Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
            final Wire aimed = cable.aim(eye, end).wire();
            final Optional<CableType> type = aimed != null && Energies.ofGrid(aimed.type().grid()).isPresent()
                    ? Optional.of(aimed.type()) : EnergyReadings.energyWireAt(level, pos);
            final Optional<ResourceLocation> id = type.map(CableType::id);
            return id.map(found -> new MeterClamp(level.dimension(), pos.immutable(), Optional.of(found)));
        }
        return EnergyReadings.block(level, pos).map(reading -> new MeterClamp(level.dimension(), pos.immutable(),
                Optional.empty()));
    }
}
