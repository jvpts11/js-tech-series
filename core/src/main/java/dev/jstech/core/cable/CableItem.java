/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The item that lays a cable. Used on a cable block it lays its wire there, when the block takes it (its lane is free
 * and neither it nor what is there never shares a block); otherwise, or while sneaking, it lays a new cable block
 * against the face clicked, holding its wire alone.
 */
public final class CableItem extends Item {

    private final Supplier<CableType> type;

    public CableItem(final Properties properties, final Supplier<CableType> type) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
    }

    /** The cable it lays. */
    public CableType type() {
        return this.type.get();
    }

    /**
     * Lays a wire of {@code type} at {@code pos}: into the cable block there, or into a new one when the place is
     * free to build in. On the server; false when it could not.
     */
    public static boolean lay(final Level level, final BlockPos pos, final CableType type) {
        final Wire wire = Wire.of(type);
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            return cable.lay(wire);
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        if (!level.setBlock(pos, CoreCables.BLOCK.get().defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.lay(wire);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();
        final Player player = context.getPlayer();
        final boolean sneaking = player != null && player.isSecondaryUseActive();
        final Wire wire = Wire.of(type());
        BlockPos target = null;
        if (!sneaking && level.getBlockEntity(clicked) instanceof CableBlockEntity cable && cable.takes(wire)) {
            target = clicked;
        } else {
            final BlockPlaceContext place = new BlockPlaceContext(context);
            if (place.canPlace()) {
                target = place.getClickedPos();
            }
        }
        if (target == null) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!lay(level, target, type())) {
            return InteractionResult.FAIL;
        }
        final BlockState state = level.getBlockState(target);
        final SoundType sound = state.getSoundType(level, target, player);
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, state));
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }
}
