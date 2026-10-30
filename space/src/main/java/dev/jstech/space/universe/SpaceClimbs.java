/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.network.ClimbWorldsPayload;
import dev.jstech.space.orbit.OrbitObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The way up from a world to its system's space and back, with no loading screen ever: a player or a moving
 * structure, a rocket or a ship, that climbs a few hundred blocks over the world's build limit passes into the space,
 * over the spot of the world's low orbit its place below is laid under ({@link SystemLayout#orbitSpot}), going on as
 * it went; one that goes down through the floor of the low orbits passes back into the world under it, over the place
 * that spot stands over, high above the ground. Nothing but sky is seen at either end, so the passage is drawn
 * through, with no screen, and the world's sphere is drawn on both sides of it.
 */
public final class SpaceClimbs {

    /**
     * How far over a world's build limit it becomes its space, in blocks: room for the climb the approved views show,
     * the far map and the bending horizon on the way up, the painted sphere near the top.
     */
    public static final int ABOVE_THE_LIMIT = 700;
    /** How far under the floor of the low orbits one passes back down into the world below, in blocks. */
    public static final int UNDER_THE_FLOOR = 96;
    /** How far under where its space begins one coming down from it arrives in a world. */
    public static final int ARRIVE_UNDER = 20;
    /* How long one who passed waits before passing again, in ticks: never to and fro across the line. */
    private static final long REST = 40;
    /* How far round a structure's box one aboard may be, in blocks, to go with it. */
    private static final double ABOARD = 3;
    /* How high over a spot taken by blocks one arriving in space is put, looking for room, in blocks. */
    private static final int ROOM = 2;

    /* When each one last passed, by the tick of the level they left. */
    private static final Map<Entity, Long> PASSED = new WeakHashMap<>();

    private SpaceClimbs() {
    }

    /** Hands the game's bus what watches the climbs, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class,
                event -> tellWorldsBelow(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class,
                event -> tellWorldsBelow(event.getEntity()));
    }

    /** The height in {@code level} above which a world becomes its space. */
    public static int ceiling(final ServerLevel level) {
        return level.getMaxBuildHeight() + ABOVE_THE_LIMIT;
    }

    /** The height in a system's space below which one passes down into the world under them. */
    public static double floor() {
        return SystemLayout.ORBIT_Y - UNDER_THE_FLOOR;
    }

    private static void tick(final ServerLevel level) {
        final Optional<SpaceWorld> world = SpaceWorlds.ofDimension(level, level.dimension());
        final Optional<SolarBody> body = world.flatMap(found -> idOf(level, found)).flatMap(
                SolarSystemWorlds::bodyOf);
        if (world.isPresent() && body.isPresent()) {
            climbOut(level, world.get(), body.get());
            return;
        }
        if (SystemSpaces.systemOf(level.dimension()).filter(StarSystems.SOLAR_SYSTEM::equals).isPresent()) {
            comeDown(level);
        }
    }

    /* Everyone and everything over the ceiling of a world goes up into its space. */
    private static void climbOut(final ServerLevel level, final SpaceWorld world, final SolarBody body) {
        final double ceiling = ceiling(level);
        final double width = Math.max(1, level.getWorldBorder().getSize());
        ServerLevel space = null;
        for (final MovingStructureEntity entity : structures(level)) {
            final MovingStructure structure = entity.structure();
            if (structure != null && structure.pose().y() > ceiling && rested(entity, level)) {
                space = space != null ? space : SystemSpaces.spaceOf(level.getServer(), world.system());
                final double[] spot = SystemLayout.orbitSpot(body, SphereMap.longitude(structure.pose().x(), width),
                        SphereMap.latitude(structure.pose().z(), width));
                announceAboard(entity);
                // Told as a thing in the world's orbit, its blocks still here to be looked over.
                OrbitObjects.arrived(level, structure, body);
                MovingStructures.travel(structure, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                PASSED.put(entity, level.getGameTime());
            }
        }
        for (final ServerPlayer player : List.copyOf(level.players())) {
            if (player.getY() > ceiling && !aboard(player) && rested(player, level)) {
                space = space != null ? space : SystemSpaces.spaceOf(level.getServer(), world.system());
                final double[] spot = SystemLayout.orbitSpot(body, SphereMap.longitude(player.getX(), width),
                        SphereMap.latitude(player.getZ(), width));
                pass(player, space, new Vec3(spot[0], roomAt(space, spot[0], spot[1]), spot[1]));
            }
        }
    }

    /* Everyone and everything under the floor of a system's space, over a world, goes down into it. */
    private static void comeDown(final ServerLevel space) {
        final double floor = floor();
        for (final MovingStructureEntity entity : structures(space)) {
            final MovingStructure structure = entity.structure();
            if (structure == null || structure.pose().y() >= floor || !rested(entity, space)) {
                continue;
            }
            final Below below = below(space, structure.pose().x(), structure.pose().z());
            if (below != null) {
                announceAboard(entity);
                OrbitObjects.forget(space.getServer(), structure.id().value());
                MovingStructures.travel(structure, below.level(), below.at());
                PASSED.put(entity, space.getGameTime());
            }
        }
        for (final ServerPlayer player : List.copyOf(space.players())) {
            if (player.getY() >= floor || aboard(player) || !rested(player, space)) {
                continue;
            }
            final Below below = below(space, player.getX(), player.getZ());
            if (below != null) {
                pass(player, below.level(), below.at());
            }
        }
    }

    /* The world under a spot of a system's space, and where in it one coming down there arrives; none over none. */
    private static @Nullable Below below(final ServerLevel space, final double x, final double z) {
        final SolarBody body = SystemLayout.over(x, z);
        if (body == null) {
            return null;
        }
        final Optional<ServerLevel> world = SpaceWorlds.get(space, SolarSystemWorlds.worldOf(body))
                .map(found -> space.getServer().getLevel(found.dimension()));
        if (world.isEmpty() || world.get() == null) {
            return null;
        }
        final ServerLevel level = world.get();
        final double width = Math.max(1, level.getWorldBorder().getSize());
        final double[] place = SystemLayout.placeUnder(body, x, z);
        return new Below(level, new Vec3(SphereMap.x(place[0], width), ceiling(level) - ARRIVE_UNDER,
                SphereMap.z(place[1], width)));
    }

    /* Sends a player across with no screen, going on as they went. */
    private static void pass(final ServerPlayer player, final ServerLevel to, final Vec3 at) {
        final ServerLevel from = player.serverLevel();
        to.getChunkAt(BlockPos.containing(at));
        DimensionTravel.announce(player, DimensionTravel.SEAMLESS);
        PASSED.put(player, from.getGameTime());
        player.changeDimension(new DimensionTransition(to, at, player.getDeltaMovement(), player.getYRot(),
                player.getXRot(), DimensionTransition.DO_NOTHING));
        PASSED.put(player, to.getGameTime());
    }

    /* The players on or in a structure about to cross see it through no screen as they go with it. */
    private static void announceAboard(final MovingStructureEntity entity) {
        final MovingStructure structure = entity.structure();
        if (structure == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        for (final ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                structure.worldBox().inflate(ABOARD))) {
            DimensionTravel.announce(player, DimensionTravel.SEAMLESS);
            PASSED.put(player, level.getGameTime());
        }
    }

    /* Whether a player rides or stands on a structure, which takes them across with it. */
    private static boolean aboard(final ServerPlayer player) {
        return StructureFooting.standingOn(player) != null || player.getRootVehicle() instanceof MovingStructureEntity;
    }

    /* Whether {@code entity} has rested long enough since it last passed, here or in another level. */
    private static boolean rested(final Entity entity, final ServerLevel level) {
        final Long last = PASSED.get(entity);
        return last == null || Math.abs(level.getGameTime() - last) >= REST;
    }

    /* The height at a spot of space where there is room to arrive: the floor of the low orbits, or over what stands. */
    private static double roomAt(final ServerLevel space, final double x, final double z) {
        final BlockPos.MutableBlockPos at = BlockPos.containing(x, SystemLayout.ORBIT_Y, z).mutable();
        space.getChunkAt(at);
        while (at.getY() < space.getMaxBuildHeight() - ROOM
                && !(space.getBlockState(at).isAir() && space.getBlockState(at.above()).isAir())) {
            at.move(0, 1, 0);
        }
        return at.getY();
    }

    /* A player arriving in the Solar System's space is told the worlds under it, as their climbs draw them. */
    private static void tellWorldsBelow(final Player entity) {
        if (!(entity instanceof ServerPlayer player)
                || SystemSpaces.systemOf(player.level().dimension()).filter(StarSystems.SOLAR_SYSTEM::equals)
                .isEmpty()) {
            return;
        }
        final List<ClimbWorldsPayload.ClimbWorld> worlds = new ArrayList<>();
        for (final SolarBody body : SolarBody.values()) {
            SpaceWorlds.get(player.serverLevel(), SolarSystemWorlds.worldOf(body))
                    .map(found -> player.server.getLevel(found.dimension()))
                    .ifPresent(level -> worlds.add(new ClimbWorldsPayload.ClimbWorld(body.name(),
                            level.getMaxBuildHeight(), FarMaps.seaOf(level),
                            Math.max(1, level.getWorldBorder().getSize()))));
        }
        PacketDistributor.sendToPlayer(player, new ClimbWorldsPayload(worlds));
    }

    private static List<? extends MovingStructureEntity> structures(final ServerLevel level) {
        return level.getEntities(EntityTypeTest.forClass(MovingStructureEntity.class), any -> true);
    }

    /* The id {@code world} is declared under. */
    private static Optional<ResourceLocation> idOf(final ServerLevel level, final SpaceWorld world) {
        return SpaceWorlds.all(level).entrySet().stream().filter(entry -> entry.getValue().equals(world))
                .map(Map.Entry::getKey).findFirst();
    }

    /* A world under a spot of space, and where one coming down arrives in it. */
    private record Below(ServerLevel level, Vec3 at) {
    }
}
