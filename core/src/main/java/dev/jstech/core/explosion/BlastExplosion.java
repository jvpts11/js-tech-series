/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A blast as the game and other mods see it: the explosion the game's events name, its damage source, what a block
 * breaking by it drops and what goes off when it is struck (the game's TNT, any mod's explosive). Its radius is how
 * far it breaks stone, which the drops of a block take their chance from, as the game's own explosions do.
 */
public final class BlastExplosion extends Explosion {

    private final Detonation detonation;

    BlastExplosion(final ServerLevel level, @Nullable final Entity source, final Vec3 centre, final float radius,
                   final boolean fire, final boolean breaksBlocks, final Detonation detonation) {
        super(level, source, centre.x, centre.y, centre.z, radius, fire,
                breaksBlocks ? BlockInteraction.DESTROY_WITH_DECAY : BlockInteraction.KEEP);
        this.detonation = detonation;
    }

    /** The blast going out this is. */
    public Detonation detonation() {
        return this.detonation;
    }
}
