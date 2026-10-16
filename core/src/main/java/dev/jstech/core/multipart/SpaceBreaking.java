/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Breaking a space of parts takes out the one part the player looks at and leaves the rest standing; the space goes
 * only with its last part, which it then gives back as any block gives its drops.
 */
public final class SpaceBreaking {

    private SpaceBreaking() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, SpaceBreaking::onBlockBreak);
    }

    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof SpaceBlock)) {
            return;
        }
        final BlockPos pos = event.getPos();
        final Player player = event.getPlayer();
        final SpaceParts.Hit aimed = SpaceBlock.strikeAimOf(level, pos, player);
        if (aimed == null || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)) {
            return;
        }
        event.setCanceled(true);
        final ItemStack back = takeOut(level, space.parts(), aimed.slot());
        if (back.isEmpty()) {
            return;
        }
        if (!player.getAbilities().instabuild && !player.getInventory().add(back)) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), back);
        }
    }

    /**
     * Takes the part in {@code slot} out of {@code parts}, its device's contents dropped and its block's breaking sound
     * played, and gives its item back; nothing when no part stands there. A space of parts alone left empty goes.
     */
    public static ItemStack takeOut(final ServerLevel level, final SpaceParts parts, final int slot) {
        final ItemStack back = takeFrom(level, parts, slot);
        SpaceBlock.vacateIfEmpty(level, parts.hostPos());
        return back;
    }

    private static ItemStack takeFrom(final ServerLevel level, final SpaceParts parts, final int slot) {
        final BlockPos pos = parts.hostPos();
        final Microblock block = parts.removeMicroblock(slot);
        if (block != null) {
            final SoundType sound = block.piece().material().defaultBlockState().getSoundType();
            level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
            return CoreSpaces.stack(block.piece());
        }
        if (!SpaceSlots.isFace(slot)) {
            return ItemStack.EMPTY;
        }
        final BlockState small = parts.removeSmall(Direction.from3DDataValue(SpaceSlots.faceOf(slot)));
        if (small != null) {
            final SoundType sound = small.getSoundType();
            level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
            return SmallBlocks.itemOf(small);
        }
        final IFacePart device = parts.remove(Direction.from3DDataValue(SpaceSlots.faceOf(slot)));
        if (device == null) {
            return ItemStack.EMPTY;
        }
        device.dropContents(level);
        level.playSound(null, pos, SoundType.METAL.getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
        return device.partItem();
    }
}
