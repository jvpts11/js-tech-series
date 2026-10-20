/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the players near a moving structure hear and see what its blocks do: a door opening, a chest's lid, a block
 * broken, a note played. The game sends such news to the players near the place it happened, and the place of a
 * structure's block is out in the yard; it is sent to the players near where the block is in the world, and their
 * game puts its sound and its particles there. News of any other place goes as before.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void jscore$sendNewsOfStructuresWhereTheyAre(@Nullable final Player except, final double x,
                                                         final double y, final double z, final double radius,
                                                         final ResourceKey<Level> dimension, final Packet<?> packet,
                                                         final CallbackInfo info) {
        if (!YardLayout.isYard(x)) {
            return;
        }
        final ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        final Vec3 there = MovingStructures.inWorld(level, new Vec3(x, y, z));
        if (!YardLayout.isYard(there.x)) {
            info.cancel();
            ((PlayerList) (Object) this).broadcast(except, there.x, there.y, there.z, radius, dimension, packet);
        }
    }
}
