/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * A piece of a formed model used from any block it stands in ({@link FormedPiece}): a player using a piece that works
 * by another block of the model than the one aimed at, a seat's back standing in the block over the seat, uses that
 * block, aimed at where they aimed, as if they had aimed at it there; so the whole piece outlined is the whole piece
 * used. A door used from any of its blocks shuts or opens ({@link FormedDoors}). On a server, where the use is
 * decided.
 */
public final class FormedPieceUses {

    private static final int AXES = 3;

    private FormedPieceUses() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class,
                FormedPieceUses::onUse);
    }

    /**
     * The block the piece the point {@code at} is on, of the block at {@code pos} standing as blocks of formed models,
     * works by, when that is another block than {@code pos}; none for a point on no such piece.
     *
     * @param at the point aimed at, in the same space as {@code pos}
     */
    public static Optional<BlockPos> worksBy(final Level level, final BlockPos pos, final Vec3 at) {
        if (!(level.getBlockEntity(pos) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        final double[] own = new double[AXES];
        for (final FormedPlace place : placed.places()) {
            final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
            if (shape.isEmpty()) {
                continue;
            }
            CellSpace.toModel(new double[] {at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ()}, place.turn(),
                    own);
            final Optional<FormedPiece> piece = shape.get().pieceAt(place, own);
            if (piece.isEmpty() || piece.get().usesCell().isEmpty()) {
                continue;
            }
            final long uses = piece.get().usesCell().getAsLong();
            if (uses == place.cell()) {
                return Optional.empty();
            }
            return Optional.of(FormedDoors.blockOf(pos, place, uses));
        }
        return Optional.empty();
    }

    /*
     * A block used on a server: a door shuts or opens ({@link FormedDoors}); a piece that works by another block is
     * used through that one.
     */
    private static void onUse(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final Vec3 at = event.getHitVec().getLocation();
        final Optional<FormedDoors.Door> door = FormedDoors.at(level, event.getPos(), at);
        if (door.isPresent()) {
            event.setCanceled(true);
            event.setCancellationResult(FormedDoors.use(level, player, door.get()) ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL);
            return;
        }
        final Optional<BlockPos> works = worksBy(level, event.getPos(), at);
        if (works.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.gameMode.useItemOn(player, level, event.getItemStack(), event.getHand(),
                new BlockHitResult(at, event.getHitVec().getDirection(), works.get(), false));
    }
}
