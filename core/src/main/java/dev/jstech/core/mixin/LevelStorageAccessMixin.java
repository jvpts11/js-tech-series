/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.config.ConfigFiles;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Says which world the game is opening: it takes hold of the world's folder before it loads the world's data, and
 * the world's own settings are read only later, as the server starts, so what reads a world's setting while its data
 * loads reads it from that folder.
 */
@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jscore$openWorld(final CallbackInfo info) {
        ConfigFiles.worldOpened(self().getLevelDirectory().path());
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void jscore$closeWorld(final CallbackInfo info) {
        ConfigFiles.worldClosed(self().getLevelDirectory().path());
    }

    private LevelStorageSource.LevelStorageAccess self() {
        return (LevelStorageSource.LevelStorageAccess) (Object) this;
    }
}
