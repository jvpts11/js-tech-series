/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

/**
 * A microblock as an item: one item for every shape, thickness and material, which ride on it as a component, named
 * after the block it was cut from. Used on a block, it stands against the face clicked; used inside a space, against
 * the face of it the side clicked looks to.
 */
public final class MicroblockItem extends Item {

    public MicroblockItem(final Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final Microblock.MicroblockPiece piece = stack.get(CoreSpaces.PIECE.get());
        if (piece == null) {
            return GameText.component(MicroblockTexts.MICROBLOCK);
        }
        return GameText.component(MicroblockTexts.nameOf(piece.shape(), piece.size())
                .with(GameText.of(piece.material().getName()), piece.size()));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getItemInHand();
        final Microblock.MicroblockPiece piece = stack.get(CoreSpaces.PIECE.get());
        if (piece == null) {
            return InteractionResult.PASS;
        }
        final Level level = context.getLevel();
        final Optional<SpacePlacement.Placement> placement = SpacePlacement.target(level, context.getClickedPos(),
                context.getClickLocation(), context.getClickedFace(), piece);
        if (placement.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!SpacePlacement.place(level, placement.get())) {
            return InteractionResult.FAIL;
        }
        final SoundType sound = piece.material().defaultBlockState().getSoundType();
        final Player player = context.getPlayer();
        level.playSound(null, placement.get().pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }
}
