/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMounts;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.multiblock.FormedPartBlock;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Two ships docked collar to collar: once a ship's DOCK brings its collar to touch another's, in line with it, the two
 * are made one, the ship docking held by the other where it stands, going wherever it goes, its own flight still; a
 * player inside either uses the collar over their head and goes through it into the other ship, under its collar,
 * neither cabin's air let out; the docking ship's autopilot key lets it go, set a block clear of the other along the
 * collars' way, flying by its own flight again.
 */
public final class ShipDocks {

    /* How near two collars' mouths are to stand docked, in blocks. */
    private static final double TOUCHING = 0.5;
    /* How far a ship let go is set off the other, in blocks. */
    private static final double CLEAR = 1.0;
    /* How far under its collar a ship's docking room may be looked for, in blocks. */
    private static final int ROOM_BELOW = 6;
    private static final double HALF = 0.5;
    /*
     * How high over a block's foot a floor may be, in blocks, to be stood on; how far over it the room a player
     * standing needs is tried from, a hair, so the floor itself is not in the way.
     */
    private static final double LOW_FLOOR = 0.5;
    private static final double OFF_THE_FLOOR = 0.01;

    private ShipDocks() {
    }

    /** Hands the game's bus the collars' use, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, ShipDocks::used);
    }

    /**
     * {@code ship}'s DOCK has brought its collar to touch {@code other}'s, in line: the two are made one at the next
     * tick, once the structures' tick that found it is done, the ship held by the other where it stands.
     */
    static void join(final ServerLevel level, final MovingStructure ship, final MovingStructure other) {
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
            if (MovingStructures.byId(level, ship.id()) != ship || MovingStructures.byId(level, other.id()) != other
                    || StructureMounts.parentOf(ship) != null) {
                return;
            }
            final Optional<BlockPos> clamp = ShipCollars.collarBlock(level, other);
            if (MovingStructures.dock(other, ship, clamp).ok() && ship.motion() instanceof ShipFlight flight) {
                MovingStructures.move(ship, flight.engaged(Optional.empty()));
            }
        }));
    }

    /** Whether {@code ship} is docked to another ship, held by it. */
    public static boolean docked(final MovingStructure ship) {
        return StructureMounts.parentOf(ship) != null;
    }

    /**
     * Lets {@code ship} go from the ship it is docked to, set clear of it along the way its collar faces, its own
     * flight its rule again, its throttle closed; whether it was docked.
     */
    public static boolean undock(final MovingStructure ship) {
        final MovingStructure host = StructureMounts.parentOf(ship);
        if (host == null || !(ship.level() instanceof ServerLevel level)
                || !(ship.motion() instanceof ShipFlight flight)) {
            return false;
        }
        final StructureFrame frame = ship.frame();
        // Off the other, back from its own collar, which is on its top and faces the other's.
        final Vec3 away = new Vec3(frame.turnX(0, 1, 0), frame.turnY(0, 1, 0), frame.turnZ(0, 1, 0)).reverse();
        if (!MovingStructures.undock(ship, away).ok()) {
            return false;
        }
        // Let go, the game would have it drift by its physics: it flies by its own flight, a block clear of the other.
        final StructurePose at = ship.pose();
        ship.teleport(at.movedTo(at.x() + away.x * CLEAR, at.y() + away.y * CLEAR, at.z() + away.z * CLEAR));
        final ShipFlightModel.State was = flight.state();
        MovingStructures.move(ship, flight.with(new ShipFlightModel.State(0, 0, 0, was.charge(), 0, was.nozzles()))
                .engaged(Optional.empty()));
        return true;
    }

    /** The ship docked to {@code ship} collar to collar: the one holding it, or the one it holds; none. */
    public static Optional<MovingStructure> partner(final MovingStructure ship) {
        final MovingStructure host = StructureMounts.parentOf(ship);
        if (host != null) {
            return Optional.of(host);
        }
        final List<MovingStructure> held = StructureMounts.children(ship);
        return held.stream().filter(each -> each.motion() instanceof ShipFlight).findFirst();
    }

    /**
     * Takes {@code player} through {@code ship}'s collar into the ship docked to it, standing under its collar, in its
     * docking room; whether they went.
     */
    public static boolean passThrough(final ServerPlayer player, final MovingStructure ship) {
        final Optional<MovingStructure> other = partner(ship);
        if (other.isEmpty() || !(ship.level() instanceof ServerLevel level)) {
            return false;
        }
        final Optional<Vec3> ours = ShipCollars.mouth(level, ship);
        final Optional<Vec3> theirs = ShipCollars.mouth(level, other.get());
        final Optional<Vec3> room = under(level, other.get(), player);
        if (ours.isEmpty() || theirs.isEmpty() || room.isEmpty() || ours.get().distanceTo(theirs.get()) > TOUCHING) {
            return false;
        }
        final StructureFrame frame = other.get().frame();
        final Vec3 at = room.get();
        player.teleportTo(level, frame.worldX(at.x, at.y, at.z), frame.worldY(at.x, at.y, at.z),
                frame.worldZ(at.x, at.y, at.z), player.getYRot(), player.getXRot());
        StructureFooting.stood(player, other.get(), BlockPos.containing(at.x, at.y - 1, at.z));
        return true;
    }

    /* A player inside a docked ship uses the collar over their head: through it into the other ship. */
    private static void used(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        final BlockPos pos = event.getPos();
        if (!FormedPartBlock.originalAt(level, pos, level.getBlockState(pos)).is(ShipKitBlocks.DOCKING_COLLAR.get())
                || !(MovingStructures.at(level, pos) instanceof MovingStructure ship)) {
            return;
        }
        if (passThrough(player, ship)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /*
     * Where {@code player} stands under {@code ship}'s collar, in its yard: down from it, the first floor under the
     * collar's middle with room over it for them standing, by the shapes of the ship's blocks, a cabin's floor and
     * ceiling being parts of its blocks; none with none near.
     */
    private static Optional<Vec3> under(final ServerLevel level, final IStructureBody ship,
                                        final ServerPlayer player) {
        if (!(ship instanceof MovingStructure structure)) {
            return Optional.empty();
        }
        final Optional<Vec3> mouth = ShipCollars.inYard(level, structure);
        if (mouth.isEmpty()) {
            return Optional.empty();
        }
        final double x = Math.floor(mouth.get().x) + HALF;
        final double z = Math.floor(mouth.get().z) + HALF;
        final EntityDimensions size = player.getDimensions(Pose.STANDING);
        final double half = size.width() / 2;
        final int collar = (int) Math.round(mouth.get().y) - 1;
        for (int y = collar - 1; y > collar - ROOM_BELOW; y--) {
            // The floor right under their feet in this block, and nothing in the way of them standing on it.
            final AABB under = new AABB(x - half, y - 1, z - half, x + half, y + LOW_FLOOR, z + half);
            double floor = Double.NEGATIVE_INFINITY;
            for (final VoxelShape shape : level.getBlockCollisions(player, under)) {
                for (final AABB part : shape.toAabbs()) {
                    floor = part.intersects(under) ? Math.max(floor, part.maxY) : floor;
                }
            }
            if (floor == Double.NEGATIVE_INFINITY || floor > y + LOW_FLOOR) {
                continue;
            }
            final AABB standing = new AABB(x - half, floor + OFF_THE_FLOOR, z - half, x + half, floor + size.height(),
                    z + half);
            boolean clear = true;
            for (final VoxelShape shape : level.getBlockCollisions(player, standing)) {
                clear &= shape.isEmpty();
            }
            if (clear) {
                return Optional.of(new Vec3(x, floor, z));
            }
        }
        return Optional.empty();
    }
}
