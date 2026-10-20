/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.YardLayout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets no creature be born in the yard: a structure's chunks tick as the land round a player does, so its crops grow
 * and its machines run, and the game would also spawn mobs in its dark rooms, out in the yard where no one is. The
 * world's own chunks spawn as before.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void jscore$noneBornInTheYard(final ServerLevel level, final LevelChunk chunk,
                                                 final NaturalSpawner.SpawnState state, final boolean friendly,
                                                 final boolean monsters, final boolean persistent,
                                                 final CallbackInfo info) {
        if (YardLayout.isYardChunk(chunk.getPos().x)) {
            info.cancel();
        }
    }
}
