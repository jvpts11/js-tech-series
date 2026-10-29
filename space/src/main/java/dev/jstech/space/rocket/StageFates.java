/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.platform.LandingPlatforms;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Where a stage goes as it comes loose, by what its rocket's flight computer set for it. A stage set to land on a
 * platform lands there when it can come back (it has landing legs and an engine), the platform stands formed, is wide
 * enough for it and holds no other stage: the platform is kept for it. A stage set to a fall zone falls somewhere in
 * the zone. Any other stage falls anywhere in the world already made: somewhere the game has loaded round one of the
 * players in its world, never where new land would have to be made, or, with no player there, where it came loose.
 */
public final class StageFates {

    private StageFates() {
    }

    /**
     * Where the stage {@code stage} of {@code rocket}, the moving structure {@code body} once it is loose, goes, coming
     * loose above {@code over} in {@code level}; {@code seed} picks the place within a zone or the world.
     */
    public static Outcome decide(final ServerLevel level, final Map<ResourceLocation, RocketPart> parts,
                                 final Rockets.Rocket rocket, final int stage, final StructureUuid body,
                                 final Vec3 over, final long seed) {
        final RandomSource random = RandomSource.create(seed);
        final Optional<StageFate> set = rocket.fate(stage);
        if (set.isPresent() && set.get() instanceof StageFate.Platform(final UUID platform)
                && RocketStages.canComeBack(parts, rocket.stack(), stage)) {
            final Optional<LandingPlatforms.Entry> entry = LandingPlatforms.get(level.getServer(), platform);
            if (entry.isPresent() && entry.get().takes(RocketStages.diameter(parts, rocket.stack(), stage))
                    && LandingPlatforms.reserve(level.getServer(), platform, body)) {
                final Optional<Vec3> spot = LandingPlatforms.landingSpot(level.getServer(), entry.get());
                if (spot.isPresent()) {
                    return new Outcome.Lands(platform, entry.get().dimension(), spot.get());
                }
                LandingPlatforms.release(level.getServer(), platform);
            }
        }
        if (set.isPresent() && set.get() instanceof StageFate.FallZone zone) {
            return new Outcome.Falls(zone.dimension(), inZone(zone, random), true);
        }
        return new Outcome.Falls(level.dimension(), anywhere(level, over, random), false);
    }

    /* A column of the zone, the same share of its every part. */
    private static BlockPos inZone(final StageFate.FallZone zone, final RandomSource random) {
        final double angle = random.nextDouble() * Math.PI * 2;
        final double far = Math.sqrt(random.nextDouble()) * zone.radius();
        return new BlockPos((int) Math.floor(zone.centre().getX() + 0.5 + Math.cos(angle) * far),
                zone.centre().getY(), (int) Math.floor(zone.centre().getZ() + 0.5 + Math.sin(angle) * far));
    }

    /*
     * A loaded column round a player of the level, picked at random among the players and the chunks the game holds
     * round them; where the stage came loose when no player is there or nothing round them is loaded.
     */
    private static BlockPos anywhere(final ServerLevel level, final Vec3 over, final RandomSource random) {
        final List<ServerPlayer> players = new ArrayList<>(level.players());
        final int reach = level.getServer().getPlayerList().getViewDistance();
        while (!players.isEmpty()) {
            final ServerPlayer player = players.remove(random.nextInt(players.size()));
            final ChunkPos centre = player.chunkPosition();
            for (int tries = 0; tries < (2 * reach + 1) * (2 * reach + 1); tries++) {
                final int cx = centre.x + random.nextInt(2 * reach + 1) - reach;
                final int cz = centre.z + random.nextInt(2 * reach + 1) - reach;
                if (level.getChunkSource().getChunkNow(cx, cz) != null) {
                    return new BlockPos((cx << 4) + random.nextInt(16), (int) Math.floor(over.y),
                            (cz << 4) + random.nextInt(16));
                }
            }
        }
        return BlockPos.containing(over);
    }

    /** Where a stage goes. */
    public sealed interface Outcome permits Outcome.Lands, Outcome.Falls {

        /** The world it ends up in. */
        ResourceKey<Level> dimension();

        /**
         * It comes back and lands on its platform, which is kept for it.
         *
         * @param platform  the platform's id
         * @param dimension the platform's world
         * @param spot      the middle of the platform's top, where its engine comes down
         */
        record Lands(UUID platform, ResourceKey<Level> dimension, Vec3 spot) implements Outcome {
        }

        /**
         * It falls, striking the ground of a column.
         *
         * @param dimension the world it falls in
         * @param column    the column it falls on; its height is where it comes loose, the ground found as it falls
         * @param inZone    whether it falls in the fall zone it was set to
         */
        record Falls(ResourceKey<Level> dimension, BlockPos column, boolean inZone) implements Outcome {
        }
    }
}
