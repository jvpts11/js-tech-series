/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a room's air does to the room: a fire goes out where there is too little oxygen; in air mostly of oxygen any
 * flame or lava sets alight at once everything round it that burns, an inferno; a block facing a greater difference in
 * pressure than it stands bursts; and air rushing out through a hole pulls things and players towards it.
 */
final class RoomConsequences {

    /* The most blocks that burst in one room in a second, and that an inferno sets alight. */
    private static final int MOST_BURST = 4;
    private static final int MOST_LIT = 32;
    /* How hard the strongest rush of air pulls, in blocks a tick each tick, at a block from the hole. */
    private static final double PULL = 0.12;

    private RoomConsequences() {
    }

    /** A second of what {@code room}'s air does to it. */
    static void second(final LevelRooms rooms, final Room room) {
        fire(rooms, room);
        burst(rooms, room);
    }

    /** A tick of the air rushing out of every room of {@code rooms} through its holes. */
    static void tick(final LevelRooms rooms) {
        final RoomStore store = rooms.store();
        if (store == null || !RoomBalance.decompressionPull()) {
            return;
        }
        for (final Room room : store.rooms()) {
            if (room.jet() > 0) {
                pull(rooms.level(), store, room);
            }
        }
    }

    /* A fire put out where the oxygen is too thin, or an inferno where the air is mostly oxygen. */
    private static void fire(final LevelRooms rooms, final Room room) {
        if (room.fires().isEmpty() && room.lava() == 0) {
            return;
        }
        final ServerLevel level = rooms.level();
        final RoomAir air = room.air();
        final double oxygen = air.partial(AirMix.OXYGEN);
        if (!room.fires().isEmpty() && RoomBalance.fireNeedsOxygen() && !GasMath.burns(oxygen)) {
            for (final long cell : room.fires().toLongArray()) {
                final BlockPos pos = BlockPos.of(cell);
                if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
                    rooms.set(pos, Blocks.AIR.defaultBlockState());
                }
                room.fire(cell, false);
            }
            return;
        }
        if (!RoomBalance.fireInPureOxygen() || !GasMath.inferno(air.share(AirMix.OXYGEN), oxygen)) {
            return;
        }
        int lit = 0;
        for (final RoomFace face : room.faces()) {
            if (lit >= MOST_LIT) {
                break;
            }
            final BlockState wall = level.getBlockState(face.block());
            final BlockPos cell = face.cell();
            if (wall.isFlammable(level, face.block(), face.side().getOpposite()) && level.getBlockState(cell).isAir()) {
                rooms.set(cell, BaseFireBlock.getState(level, cell));
                room.fire(cell.asLong(), true);
                lit++;
            }
        }
    }

    /* The blocks of the room's walls facing a greater difference in pressure than they stand burst. */
    private static void burst(final LevelRooms rooms, final Room room) {
        if (!RoomBalance.blocksBurst()) {
            return;
        }
        final RoomStore store = rooms.store();
        final ServerLevel level = rooms.level();
        final double inside = room.pressure();
        int burst = 0;
        for (final RoomFace face : room.faces()) {
            if (burst >= MOST_BURST) {
                break;
            }
            if (face.kind() == RoomFace.Kind.HOLE) {
                continue;
            }
            final Room other = store == null ? null : store.roomAt(face.beyond().asLong());
            final double beyond = other != null ? other.pressure() : face.outside();
            if (GasMath.bursts(face.rating(), inside, beyond) && !level.getBlockState(face.block()).isAir()) {
                level.destroyBlock(face.block(), true);
                burst++;
            }
        }
    }

    /* The things in a room pulled towards the nearest hole its air rushes out of. */
    private static void pull(final ServerLevel level, final RoomStore store, final Room room) {
        final List<Vec3> holes = new ArrayList<>();
        for (final RoomFace face : room.faces()) {
            if ((face.kind() == RoomFace.Kind.HOLE || face.kind() == RoomFace.Kind.DOOR) && face.leaks()
                    && store.roomAt(face.beyond().asLong()) == null) {
                holes.add(face.block().getCenter());
            }
        }
        if (holes.isEmpty()) {
            return;
        }
        // A room aboard a moving structure is in the yard; whoever is in it stands where the structure is.
        final AABB box = room.bounds();
        final IStructureBody aboard = YardLayout.isYard(box.minX)
                ? MovingStructures.at(level, BlockPos.containing(box.getCenter())) : null;
        final StructureFrame frame = aboard == null ? null : aboard.frame();
        final AABB where = frame == null ? box : worldBox(frame, box);
        for (final Entity entity : level.getEntities((Entity) null, where)) {
            final Vec3 at = frame == null ? entity.position() : yardOf(frame, entity.position());
            if (!room.cells().contains(BlockPos.containing(at).asLong()) || entity.isSpectator()
                    || entity instanceof ServerPlayer player && player.getAbilities().flying) {
                continue;
            }
            Vec3 nearest = holes.getFirst();
            for (final Vec3 hole : holes) {
                if (hole.distanceToSqr(at) < nearest.distanceToSqr(at)) {
                    nearest = hole;
                }
            }
            final Vec3 towards = nearest.subtract(at);
            final double away = Math.max(1, towards.length());
            final Vec3 pull = towards.normalize().scale(PULL * room.jet() / away);
            entity.addDeltaMovement(frame == null ? pull : new Vec3(frame.turnX(pull.x, pull.y, pull.z),
                    frame.turnY(pull.x, pull.y, pull.z), frame.turnZ(pull.x, pull.y, pull.z)));
            entity.hurtMarked = true;
        }
    }

    /* The box of the world the yard's {@code box} fills, as the structure's frame stands. */
    private static AABB worldBox(final StructureFrame frame, final AABB box) {
        final double[] world = frame.worldBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return new AABB(world[0], world[1], world[2], world[3], world[4], world[5]);
    }

    /* The yard's place of the world's place {@code at}, as the structure's frame stands. */
    private static Vec3 yardOf(final StructureFrame frame, final Vec3 at) {
        return new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z), frame.yardZ(at.x, at.y, at.z));
    }
}
