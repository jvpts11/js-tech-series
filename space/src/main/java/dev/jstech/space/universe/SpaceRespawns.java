/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.dimension.DimensionRespawns;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceWorlds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Where a player who dies on a round world other than the Overworld comes back, in this order: their bed on that
 * world, if it stands in a sealed room with air; else the cabin of the module they landed in there, if it is still
 * whole and has air; else home, in the Overworld, at their own bed there or the world's spawn.
 */
public final class SpaceRespawns {

    private SpaceRespawns() {
    }

    /** Gives J's Core the rule, once, from the mod's constructor. */
    public static void declare() {
        DimensionRespawns.add(SpaceRespawns::respawn);
    }

    private static Optional<DimensionTransition> respawn(final ServerPlayer player, final ServerLevel died,
                                                         final DimensionTransition planned) {
        if (died.dimension().equals(Level.OVERWORLD) || SpaceWorlds.ofDimension(died, died.dimension()).isEmpty()) {
            return Optional.empty();
        }
        if (planned.newLevel() == died && !planned.missingRespawnBlock() && hasAir(died, planned.pos())) {
            return Optional.empty();
        }
        final Optional<LandingCabins.Cabin> cabin = LandingCabins.of(player, died)
                .filter(landed -> landed.whole(died) && SealedRooms.breathableAt(died, landed.pos().above()));
        if (cabin.isPresent()) {
            return Optional.of(new DimensionTransition(died, Vec3.atBottomCenterOf(cabin.get().pos().above()),
                    Vec3.ZERO, player.getYRot(), 0, DimensionTransition.DO_NOTHING));
        }
        if (planned.newLevel().dimension().equals(Level.OVERWORLD)) {
            return Optional.empty();
        }
        final ServerLevel home = died.getServer().overworld();
        return Optional.of(new DimensionTransition(home, Vec3.atBottomCenterOf(home.getSharedSpawnPos()), Vec3.ZERO,
                home.getSharedSpawnAngle(), 0, DimensionTransition.DO_NOTHING));
    }

    /* Whether a bed's place stands in a sealed room whose air can be breathed. */
    private static boolean hasAir(final ServerLevel level, final Vec3 at) {
        final BlockPos pos = BlockPos.containing(at);
        return SealedRooms.roomBeside(level, pos).isPresent() && SealedRooms.breathableAt(level, pos);
    }
}
