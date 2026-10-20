/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jstech.core.moving.MovingStructureEntity;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a player see a moving structure as far as its blocks reach, not as far as its middle: the game shows an entity
 * to players within their view of its place and in a chunk they see, and a structure a hundred blocks long whose end
 * is in sight would vanish while its middle is out of it. For the kit's structures alone the reach is the player's
 * view plus the structure's own size, wherever its middle stands.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow
    @Final
    Entity entity;

    @WrapOperation(method = "updatePlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int jscore$seeStructuresAsFarAsTheyReach(final int range, final int view,
                                                     final Operation<Integer> original) {
        final int seen = original.call(range, view);
        if (!(entity instanceof MovingStructureEntity)) {
            return seen;
        }
        final AABB box = entity.getBoundingBox();
        final double reach = Math.sqrt(box.getXsize() * box.getXsize() + box.getZsize() * box.getZsize()) / 2;
        return view + (int) Math.ceil(reach);
    }

    @WrapOperation(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;"
            + "isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean jscore$seeStructuresOverAnyChunk(final ChunkMap map, final ServerPlayer player, final int x,
                                                    final int z, final Operation<Boolean> original) {
        return entity instanceof MovingStructureEntity || original.call(map, player, x, z);
    }
}
