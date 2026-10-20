/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens a particle's place, the place it was the tick before, and its speed, which the game keeps closed, so a
 * particle a block of a moving structure gives off can be put where that block is in the world
 * ({@code ParticleEngineMixin}).
 */
@Mixin(Particle.class)
public interface ParticleMixin {

    @Accessor("x")
    double jscore$x();

    @Accessor("y")
    double jscore$y();

    @Accessor("z")
    double jscore$z();

    @Accessor("xo")
    void jscore$setXo(double value);

    @Accessor("yo")
    void jscore$setYo(double value);

    @Accessor("zo")
    void jscore$setZo(double value);

    @Accessor("xd")
    double jscore$xd();

    @Accessor("yd")
    double jscore$yd();

    @Accessor("zd")
    double jscore$zd();

    @Accessor("xd")
    void jscore$setXd(double value);

    @Accessor("yd")
    void jscore$setYd(double value);

    @Accessor("zd")
    void jscore$setZd(double value);
}
