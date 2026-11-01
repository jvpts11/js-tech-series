/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * What the game's own loading screen waits on: the server having placed the player and the land under their feet built
 * to be drawn. A passage that shows something else in its place waits on the same, and on more.
 */
@Mixin(ReceivingLevelScreen.class)
public interface ReceivingLevelScreenAccessor {

    /** Whether the land the game's loading screen waits for is there. */
    @Accessor("levelReceived")
    BooleanSupplier jscore$levelReceived();
}
