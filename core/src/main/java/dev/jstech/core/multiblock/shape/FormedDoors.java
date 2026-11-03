/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.shape;

import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.room.Rooms;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The doors of formed models: pieces with a leaf ({@link FormedPiece#shut}) that a player shuts and opens by using any
 * block of the piece, a door frame aimed at, a leaf. Whether a door stands shut is kept by the blocks its leaf stands
 * in and the block it works by, each keeping it for itself ({@link FormedPartBlockEntity#movedDoors}), so the leaf is
 * solid in its blocks while it is shut, wherever the structure goes with them, and the players see it. A door stands
 * open as its structure forms, or shut where its piece says so. A mod may keep a door as it is, as an airlock does,
 * by cancelling {@link FormedDoorEvent}.
 */
public final class FormedDoors {

    private static final double HALF = 0.5;
    private static final int AXES = 3;

    private FormedDoors() {
    }

    /**
     * The door the point {@code at} of the block at {@code pos} is on, as a block of a formed model; none for a point
     * on no door.
     *
     * @param at the point, in the same space as {@code pos}
     */
    public static Optional<Door> at(final Level level, final BlockPos pos, final Vec3 at) {
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
            if (piece.isPresent() && piece.get().door()) {
                return Optional.of(of(shape.get(), place, pos, piece.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * The door called {@code door} of the formed model the block at {@code pos} stands as the block {@code place} of;
     * none for a model with no such door, or whose shape file is not known.
     */
    public static Optional<Door> named(final Level level, final BlockPos pos, final FormedPlace place,
                                       final String door) {
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return Optional.empty();
        }
        return shape.get().piece(door).filter(FormedPiece::door).map(piece -> of(shape.get(), place, pos, piece));
    }

    /** Whether {@code door} stands shut, as the block it works by keeps it. */
    public static boolean shut(final BlockGetter level, final Door door) {
        return shut(level, door.uses(), door.piece(), door.startsShut());
    }

    /**
     * Whether the door {@code piece}, working by the block at {@code uses}, stands shut, as that block keeps it: as
     * it stood as its structure formed ({@code startsShut}), unless it was moved since; for a model's drawing.
     */
    public static boolean shut(final BlockGetter level, final BlockPos uses, final String piece,
                               final boolean startsShut) {
        final boolean moved = level.getBlockEntity(uses) instanceof FormedPartBlockEntity cell
                && cell.movedDoors().contains(piece);
        return startsShut != moved;
    }

    /**
     * Shuts {@code door}, or opens it, as no one's use: its leaf's blocks and the block it works by are told, and the
     * rooms its leaf bounds are found again, its air going through it only while it is open.
     */
    public static void set(final Level level, final Door door, final boolean shut) {
        for (final BlockPos at : door.blocks()) {
            if (level.getBlockEntity(at) instanceof FormedPartBlockEntity cell) {
                cell.moveDoor(door.piece(), shut != door.startsShut());
            }
        }
        if (level instanceof ServerLevel server) {
            door.leaf().forEach(at -> Rooms.resealed(server, at));
        }
    }

    /**
     * {@code player} uses {@code door}: it shuts if it is open, opens if it is shut, unless a mod keeps it as it is
     * ({@link FormedDoorEvent}); whether it moved.
     */
    public static boolean use(final ServerLevel level, final @Nullable Player player, final Door door) {
        final boolean shutting = !shut(level, door);
        if (NeoForge.EVENT_BUS.post(new FormedDoorEvent(level, player, door, shutting)).isCanceled()) {
            return false;
        }
        set(level, door, shutting);
        return true;
    }

    /**
     * Where the block {@code cell} ({@link MeshCells#key}) of a formed model is, the block at {@code pos} standing as
     * its block {@code place}: its middle from this one's, turned as the model stands.
     */
    public static BlockPos blockOf(final BlockPos pos, final FormedPlace place, final long cell) {
        final double[] middle = {MeshCells.x(cell) - MeshCells.x(place.cell()) + HALF,
            MeshCells.y(cell) - MeshCells.y(place.cell()) + HALF, MeshCells.z(cell) - MeshCells.z(place.cell())
                + HALF};
        final double[] turned = new double[AXES];
        CellSpace.toWorld(middle, place.turn(), turned);
        return pos.offset((int) Math.floor(turned[0]), (int) Math.floor(turned[1]), (int) Math.floor(turned[2]));
    }

    /* The door {@code piece} of {@code shape}, the block at {@code pos} standing as its block {@code place}. */
    private static Door of(final FormedShape shape, final FormedPlace place, final BlockPos pos,
                           final FormedPiece piece) {
        final List<BlockPos> leaf = new ArrayList<>();
        for (final long cell : shape.leaf(piece.name()).keySet()) {
            leaf.add(blockOf(pos, place, cell));
        }
        return new Door(place.model(), piece.name(), blockOf(pos, place, piece.usesCell().orElseThrow()), leaf,
                piece.startsShut());
    }

    /**
     * A door of a formed model where it stands.
     *
     * @param model      the formed model
     * @param piece      its name among the model's pieces
     * @param uses       the block it works by, which keeps whether it is shut
     * @param leaf       the blocks its leaf stands in while it is shut
     * @param startsShut whether it stood shut as its structure formed
     */
    public record Door(ResourceLocation model, String piece, BlockPos uses, List<BlockPos> leaf, boolean startsShut) {

        public Door {
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(piece, "piece");
            Objects.requireNonNull(uses, "uses");
            leaf = List.copyOf(leaf);
        }

        /** Every block that keeps whether it is shut: the one it works by, and its leaf's. */
        public List<BlockPos> blocks() {
            final Set<BlockPos> out = new LinkedHashSet<>();
            out.add(this.uses);
            out.addAll(this.leaf);
            return List.copyOf(out);
        }
    }
}
