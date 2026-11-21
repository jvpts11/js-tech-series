/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.multiblock.shape.FormedPieceAim;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.region.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The pieces of formed models round the player by the name their models give them, a seat, a console, a door, in
 * the world and aboard the moving structures near, each with the box round it in the world, the nearest first: what an
 * agent looks at by name, and the points of a piece to look at, its middle first, then the middles of its box's faces,
 * the nearest the eye first, for the first one the look meets the piece at.
 */
final class PlayPieces {

    private PlayPieces() {
    }

    /** The pieces called {@code name} within {@code radius} blocks of {@code eye}, the nearest first. */
    static List<FormedPieceAim.Aimed> near(final Level level, final Vec3 eye, final String name,
                                           final double radius) {
        final List<FormedPieceAim.Aimed> found = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final BlockPos at = BlockPos.containing(eye);
        final int reach = (int) Math.ceil(radius);
        collect(level, new Box(at.getX() - reach, at.getY() - reach, at.getZ() - reach, at.getX() + reach,
                at.getY() + reach, at.getZ() + reach), name, null, found, seen);
        if (LevelMovingStructures.anyLoaded()) {
            for (final IStructureBody body : LevelMovingStructures.of(level).near(new AABB(eye, eye).inflate(radius))) {
                if (body.ready()) {
                    collect(level, body.bounds(), name, body.aimFrame(), found, seen);
                }
            }
        }
        found.removeIf(piece -> piece.box().getCenter().distanceTo(eye) > radius);
        found.sort(Comparator.comparingDouble(piece -> piece.box().getCenter().distanceTo(eye)));
        return found;
    }

    /** The points of {@code box} to look at from {@code eye}: its middle, then its faces' middles, nearest first. */
    static List<Vec3> points(final AABB box, final Vec3 eye) {
        final Vec3 middle = box.getCenter();
        final List<Vec3> faces = new ArrayList<>(List.of(new Vec3(box.minX, middle.y, middle.z),
                new Vec3(box.maxX, middle.y, middle.z), new Vec3(middle.x, box.minY, middle.z),
                new Vec3(middle.x, box.maxY, middle.z), new Vec3(middle.x, middle.y, box.minZ),
                new Vec3(middle.x, middle.y, box.maxZ)));
        faces.sort(Comparator.comparingDouble(face -> face.distanceTo(eye)));
        final List<Vec3> out = new ArrayList<>();
        out.add(middle);
        out.addAll(faces);
        return out;
    }

    /*
     * The pieces called {@code name} of the formed blocks in {@code box}, of the world or of a structure's yard whose
     * frame is {@code frame}, each piece once however many blocks it stands in, its box in the world.
     */
    private static void collect(final Level level, final Box box, final String name,
                                @Nullable final StructureFrame frame, final List<FormedPieceAim.Aimed> found,
                                final Set<String> seen) {
        for (int cx = SectionPos.blockToSectionCoord(box.minX()); cx <= SectionPos.blockToSectionCoord(box.maxX());
             cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ()); cz <= SectionPos.blockToSectionCoord(box.maxZ());
                 cz++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (final BlockEntity entity : chunk.getBlockEntities().values()) {
                    final BlockPos pos = entity.getBlockPos();
                    if (!(entity instanceof IFormedPlaced) || !box.contains(pos.getX(), pos.getY(), pos.getZ())) {
                        continue;
                    }
                    final Optional<FormedPieceAim.Aimed> piece = FormedPieceAim.named(level, pos, name);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    final FormedPieceAim.Aimed inWorld = frame == null ? piece.get() : inWorld(piece.get(), frame);
                    if (seen.add(key(inWorld))) {
                        found.add(inWorld);
                    }
                }
            }
        }
    }

    private static FormedPieceAim.Aimed inWorld(final FormedPieceAim.Aimed inYard, final StructureFrame frame) {
        final AABB yard = inYard.box();
        final double[] world = frame.worldBox(yard.minX, yard.minY, yard.minZ, yard.maxX, yard.maxY, yard.maxZ);
        return new FormedPieceAim.Aimed(inYard.model(), inYard.piece(), new AABB(world[0], world[1], world[2],
                world[3], world[4], world[5]));
    }

    /* One piece, by its model, its name and its box to a tenth of a block. */
    private static String key(final FormedPieceAim.Aimed piece) {
        final AABB box = piece.box();
        return piece.model() + "/" + piece.piece() + "/" + Math.round(box.minX * 10) + "," + Math.round(box.minY * 10)
                + "," + Math.round(box.minZ * 10) + "," + Math.round(box.maxX * 10);
    }
}
