/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.registry.CoreAttachments;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What stands on each moving structure, and carrying it: every tick a structure moves, whatever stood on it in the
 * last ticks moves with it, turning as it turns, so a mob on a lift rises with it and a player walks a deck in flight
 * as the ground. A thing that jumps off a moving deck goes on with the deck's speed. A player's own game carries the
 * player and tells the server where on the deck they stand ({@link StructureFootingPayload}), and the server carries
 * them the same way, moving with them the place it checks their next step from, so a deck as fast as a rocket never
 * reads as the player moving too fast.
 */
public final class StructureFooting {

    /* After this many ticks off a structure, and out of its box, a thing has left it. */
    private static final int GRACE = 10;
    /* What stands on the structures, on the server and on a player's game; each side keeps its own. */
    private static final Map<Entity, Stand> SERVER = new WeakHashMap<>();
    private static final Map<Entity, Stand> CLIENT = new WeakHashMap<>();
    /* Where each player last said they stand, in the yard, for the move they send next. */
    private static final Map<ServerPlayer, Reported> REPORTED = new WeakHashMap<>();
    /* Where on a deck each player's game says the vehicle they drive stands. */
    private static final Map<ServerPlayer, Reported> REPORTED_VEHICLES = new WeakHashMap<>();
    /* On a player's game, what waits for the game to keep where it stood before this tick, and what is done then. */
    private static WeakReference<Entity> waiting = new WeakReference<>(null);
    private static @Nullable Consumer<Entity> waitingAction;
    /* On a player's game, who got up from a structure's seat this tick, set on its deck as the tick ends. */
    private static WeakReference<Entity> gotUp = new WeakReference<>(null);

    private StructureFooting() {
    }

    /**
     * Has {@code action} done to {@code entity} on a player's game once the game has kept where it stood before this
     * tick, as it does just before ticking it, in place of whatever waited before: the carrying of the player, or of
     * what they drive, by the deck under them. Carried before that, the tick's carrying would be kept as where they
     * stood, and drawn as a jump at the tick's end against a deck drawn going smoothly: they would shake on it.
     */
    public static void onceOldPositionKept(final Entity entity, final Consumer<Entity> action) {
        waiting = new WeakReference<>(entity);
        waitingAction = action;
    }

    /**
     * On a player's game, {@code entity} got up from a seat of a structure this tick, on it: as the tick ends, where it
     * stood before is set from where it stands on the structure now, as the structure stood the tick before
     * ({@link #settleGotUp()}). Got up after the game kept where it stood, that would be a tick older than the deck as
     * drawn, and it would be drawn sliding along the deck through the tick.
     */
    static void gotUp(final Entity entity) {
        if (entity.level().isClientSide()) {
            gotUp = new WeakReference<>(entity);
        }
    }

    /** The tick ended on a player's game: who got up in it is drawn from now on with the deck under them. */
    public static void settleGotUp() {
        holdGotUp();
        gotUp = new WeakReference<>(null);
    }

    /**
     * A frame is about to be drawn on a player's game: who got up from a seat since the last tick ended is drawn on
     * the deck in it, where they stood before set again from where they stand on it now, whatever set it since.
     */
    public static void holdGotUp() {
        final Entity entity = gotUp.get();
        if (entity == null || entity.isRemoved() || entity.isPassenger()) {
            return;
        }
        final IStructureBody body = standingOn(entity);
        if (body == null) {
            return;
        }
        final StructureFrame now = body.frame();
        final StructureFrame before = body.previousFrame();
        final double x = now.yardX(entity.getX(), entity.getY(), entity.getZ());
        final double y = now.yardY(entity.getX(), entity.getY(), entity.getZ());
        final double z = now.yardZ(entity.getX(), entity.getY(), entity.getZ());
        entity.xo = before.worldX(x, y, z);
        entity.yo = before.worldY(x, y, z);
        entity.zo = before.worldZ(x, y, z);
        entity.xOld = entity.xo;
        entity.yOld = entity.yo;
        entity.zOld = entity.zo;
    }

    /** The game kept where {@code entity} stood before its tick: what waited for that is done now. */
    public static void oldPositionKept(final Entity entity) {
        if (waitingAction == null || !entity.level().isClientSide() || waiting.get() != entity) {
            return;
        }
        final Consumer<Entity> action = waitingAction;
        waiting = new WeakReference<>(null);
        waitingAction = null;
        action.accept(entity);
    }

    /** {@code entity} stood on {@code body} this tick, on the block {@code support} of the yard. */
    public static void stood(final Entity entity, final IStructureBody body, final BlockPos support) {
        final Map<Entity, Stand> stands = side(entity.level());
        final Stand stand = stands.get(entity);
        if (stand != null && stand.body == body) {
            stand.since = 0;
            stand.support = support;
            return;
        }
        stands.put(entity, new Stand(body, support));
    }

    /**
     * What stands on {@code body} now, on the server, as its watchers are told it: those that stood on it this tick
     * or the last, none riding anything.
     */
    public static List<Entity> ridersOf(final IStructureBody body) {
        final Map<Entity, Stand> stands = side(body.level());
        if (stands.isEmpty()) {
            return List.of();
        }
        final List<Entity> riders = new ArrayList<>();
        for (final Map.Entry<Entity, Stand> each : stands.entrySet()) {
            final Stand stand = each.getValue();
            final Entity entity = each.getKey();
            if (stand.body == body && stand.since <= 1 && !entity.isRemoved() && !entity.isPassenger()) {
                riders.add(entity);
            }
        }
        return riders;
    }

    /** The structure {@code entity} stands on, or stood on in the last ticks; null for none. */
    public static @Nullable IStructureBody standingOn(final Entity entity) {
        if (!LevelMovingStructures.anyLoaded()) {
            return null;
        }
        final Stand stand = side(entity.level()).get(entity);
        return stand == null ? null : stand.body;
    }

    /** The block of the yard {@code entity} stood on this tick or the last, or null when it stands on none. */
    public static @Nullable BlockPos support(final Entity entity) {
        if (!LevelMovingStructures.anyLoaded()) {
            return null;
        }
        final Stand stand = side(entity.level()).get(entity);
        return stand == null || stand.since > 1 ? null : stand.support;
    }

    /**
     * Carries what stands on {@code body} by how it moved this tick, from its frame before to its frame now. The
     * player whose game this is is carried by {@link #carry(Entity, IStructureBody)} before it moves.
     */
    public static void carry(final IStructureBody body) {
        final Map<Entity, Stand> stands = side(body.level());
        if (stands.isEmpty()) {
            return;
        }
        final List<Entity> riders = new ArrayList<>();
        for (final Map.Entry<Entity, Stand> each : stands.entrySet()) {
            if (each.getValue().body == body && !each.getKey().isRemoved()) {
                riders.add(each.getKey());
            }
        }
        for (final Entity rider : riders) {
            if (!body.level().isClientSide() || !rider.isControlledByLocalInstance()) {
                carry(rider, body);
            }
        }
    }

    /** Carries {@code rider} by how {@code body} moved this tick. */
    public static void carry(final Entity rider, final IStructureBody body) {
        final StructureFrame before = body.previousFrame();
        final StructureFrame now = body.frame();
        if (before == now) {
            return;
        }
        final double yx = before.yardX(rider.getX(), rider.getY(), rider.getZ());
        final double yy = before.yardY(rider.getX(), rider.getY(), rider.getZ());
        final double yz = before.yardZ(rider.getX(), rider.getY(), rider.getZ());
        final double x = now.worldX(yx, yy, yz);
        final double y = now.worldY(yx, yy, yz);
        final double z = now.worldZ(yx, yy, yz);
        final double dx = x - rider.getX();
        final double dy = y - rider.getY();
        final double dz = z - rider.getZ();
        rider.setPos(x, y, z);
        if (rider instanceof ServerPlayer player && player.connection != null) {
            final ServerGamePacketListenerImpl connection = player.connection;
            connection.firstGoodX += dx;
            connection.firstGoodY += dy;
            connection.firstGoodZ += dz;
            connection.lastGoodX += dx;
            connection.lastGoodY += dy;
            connection.lastGoodZ += dz;
            return;
        }
        final float turn = (float) Mth.wrapDegrees(now.pose().yawDegrees() - before.pose().yawDegrees());
        if (Math.abs(turn) > 1.0e-4F) {
            rider.setYRot(rider.getYRot() + turn);
            if (rider instanceof LivingEntity living) {
                living.setYHeadRot(living.getYHeadRot() + turn);
                living.yBodyRot += turn;
            }
        }
    }

    /**
     * Ages what stands on the structures of {@code level}, once a tick: a thing off its structure for a few ticks and
     * out of its box has left it, and goes on with the speed the structure had where it left.
     */
    public static void tick(final Level level) {
        final Map<Entity, Stand> stands = side(level);
        if (stands.isEmpty()) {
            return;
        }
        final Iterator<Map.Entry<Entity, Stand>> each = stands.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<Entity, Stand> entry = each.next();
            final Entity entity = entry.getKey();
            final Stand stand = entry.getValue();
            if (entity.level() != level) {
                continue;
            }
            stand.since++;
            final boolean gone = entity.isRemoved() || LevelMovingStructures.of(level).byId(stand.body.id())
                    != stand.body;
            final AABB box = stand.body.worldBox();
            if (gone || stand.since > GRACE && !box.intersects(entity.getBoundingBox())) {
                each.remove();
                if (!gone && (!level.isClientSide() || entity.isControlledByLocalInstance())
                        && !(entity instanceof ServerPlayer)) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(speedAt(stand.body, entity.position())));
                }
            }
        }
    }

    /**
     * The blocks {@code moves} of {@code from} went to {@code to}, a piece parting or a structure joining another:
     * what stood on them stands on {@code to} now, on the same blocks where they went.
     */
    public static void moved(final IStructureBody from, final IStructureBody to, final Map<BlockPos, BlockPos> moves) {
        final Map<Entity, Stand> stands = side(from.level());
        for (final Stand stand : stands.values()) {
            if (stand.body == from) {
                final BlockPos went = moves.get(stand.support);
                if (went != null) {
                    stand.body = to;
                    stand.support = went;
                }
            }
        }
    }

    /** How fast the place {@code at} of {@code body} moved in its last tick, turning included, in blocks a tick. */
    public static Vec3 speedAt(final IStructureBody body, final Vec3 at) {
        final StructureFrame before = body.previousFrame();
        final StructureFrame now = body.frame();
        final double yx = before.yardX(at.x, at.y, at.z);
        final double yy = before.yardY(at.x, at.y, at.z);
        final double yz = before.yardZ(at.x, at.y, at.z);
        return new Vec3(now.worldX(yx, yy, yz) - at.x, now.worldY(yx, yy, yz) - at.y, now.worldZ(yx, yy, yz) - at.z);
    }

    /**
     * Where {@code player} says they stand, in the world as the server's structure stands now, for the move they
     * sent next; null when they stand on none.
     */
    public static @Nullable Vec3 reportedPlace(final ServerPlayer player) {
        if (REPORTED.isEmpty()) {
            return null;
        }
        final Reported reported = REPORTED.get(player);
        if (reported == null || reported.structure.entity().isRemoved()) {
            return null;
        }
        final StructureFrame frame = reported.structure.frame();
        final double[] at = reported.place;
        return new Vec3(frame.worldX(at[0], at[1], at[2]), frame.worldY(at[0], at[1], at[2]),
                frame.worldZ(at[0], at[1], at[2]));
    }

    /**
     * Where the vehicle {@code player} drives stands, by their game's word, in the world as the server's structure
     * stands now, for the vehicle's move they sent next; null when it stands on none.
     */
    public static @Nullable Vec3 reportedVehiclePlace(final ServerPlayer player) {
        if (REPORTED_VEHICLES.isEmpty()) {
            return null;
        }
        final Reported reported = REPORTED_VEHICLES.get(player);
        // A vehicle that went through a portal stands on no deck of the world it left.
        if (reported == null || reported.structure.entity().isRemoved() || reported.structure.level()
                != player.level()) {
            return null;
        }
        final StructureFrame frame = reported.structure.frame();
        final double[] at = reported.place;
        return new Vec3(frame.worldX(at[0], at[1], at[2]), frame.worldY(at[0], at[1], at[2]),
                frame.worldZ(at[0], at[1], at[2]));
    }

    /** A player's game said which structure the player, or the vehicle they drive, stands on, and where on it. */
    static void reported(final ServerPlayer player, final StructureFootingPayload payload) {
        final Map<ServerPlayer, Reported> kept = payload.vehicle() ? REPORTED_VEHICLES : REPORTED;
        if (payload.entity() < 0) {
            kept.remove(player);
            if (!payload.vehicle()) {
                player.setData(CoreAttachments.FOOTING.get(), Footing.NONE);
            }
            return;
        }
        final Entity entity = player.level().getEntity(payload.entity());
        if (!(entity instanceof MovingStructureEntity structureEntity) || structureEntity.structure() == null) {
            kept.remove(player);
            return;
        }
        final MovingStructure structure = structureEntity.structure();
        final double[] place = payload.place().clone();
        if (!(structure.worldBox().inflate(4).contains(new Vec3(structure.frame().worldX(place[0], place[1],
                place[2]), structure.frame().worldY(place[0], place[1], place[2]),
                structure.frame().worldZ(place[0], place[1], place[2]))))) {
            // A place nowhere near the structure is not standing on it, whatever the game says.
            kept.remove(player);
            return;
        }
        kept.put(player, new Reported(structure, place));
        if (payload.vehicle()) {
            return;
        }
        final float yaw = (float) Mth.wrapDegrees(player.getYRot() - structure.pose().yawDegrees());
        player.setData(CoreAttachments.FOOTING.get(), Footing.on(structure.id(), place[0], place[1], place[2], yaw));
    }

    /*
     * {@code player} got up from a seat of {@code structure} at {@code place} of its yard, on the server: their game
     * stands them there too, on the structure as it draws it, so their next move is taken from there before their
     * game first says where they stand; taken from the world instead, a deck their game draws a moment behind would
     * read as a wrong move, and the server would send them back where it has them, off the deck as they see it.
     */
    static void gotUpAt(final ServerPlayer player, final MovingStructure structure, final Vec3 place) {
        final double[] at = {place.x, place.y, place.z};
        REPORTED.put(player, new Reported(structure, at));
        final float yaw = (float) Mth.wrapDegrees(player.getYRot() - structure.pose().yawDegrees());
        player.setData(CoreAttachments.FOOTING.get(), Footing.on(structure.id(), at[0], at[1], at[2], yaw));
        // Their next move is checked from where they got up, not from the seat: the game keeps that till its tick.
        final ServerGamePacketListenerImpl connection = player.connection;
        if (connection != null) {
            final StructureFrame frame = structure.frame();
            connection.firstGoodX = frame.worldX(at[0], at[1], at[2]);
            connection.firstGoodY = frame.worldY(at[0], at[1], at[2]);
            connection.firstGoodZ = frame.worldZ(at[0], at[1], at[2]);
            connection.lastGoodX = connection.firstGoodX;
            connection.lastGoodY = connection.firstGoodY;
            connection.lastGoodZ = connection.firstGoodZ;
        }
    }

    private static Map<Entity, Stand> side(final Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    /* A thing standing on a structure: which, on which block of the yard, and how many ticks ago. */
    private static final class Stand {

        private IStructureBody body;
        private BlockPos support;
        private int since;

        private Stand(final IStructureBody body, final BlockPos support) {
            this.body = body;
            this.support = support;
        }
    }

    /* Where a player's game last said they stand. */
    private record Reported(MovingStructure structure, double[] place) {
    }
}
