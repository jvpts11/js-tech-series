/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.room.Rooms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the sealed rooms of every block that changes. The game's events tell of blocks broken, placed, blown up and
 * pushed, but not of a block changed by a command, by another mod's machine or without telling its neighbours; a room
 * opened by one of those would stay sealed. The game calls this method for every block that changes, by any means: it
 * only notes where, and the rooms are worked out again later, away from the change.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void jscore$noteForRooms(final BlockPos pos, final BlockState before, final BlockState after,
                                     final CallbackInfo info) {
        Rooms.noteChange((ServerLevel) (Object) this, pos, before, after);
    }
}
