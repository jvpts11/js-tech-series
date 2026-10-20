/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the particles of a moving structure's blocks that the game hands straight to its particles where the blocks
 * are: the pieces of a block broken, the cracks of one being broken. The game gives them off at the block's place, out
 * in the yard where no one sees them; a particle born there is moved to where the block is drawn in the world, its
 * speed turned as the structure is. A flame, smoke or a drip goes by the level's drawing first, and is put there
 * ({@code LevelRendererMixin}). Every other particle is let be.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(method = "add", at = @At("HEAD"))
    private void jscore$giveOffWhereTheBlockIs(final Particle particle, final CallbackInfo info) {
        final ParticleMixin open = (ParticleMixin) particle;
        final double x = open.jscore$x();
        if (!YardLayout.isYard(x) || Minecraft.getInstance().level == null) {
            return;
        }
        final double y = open.jscore$y();
        final double z = open.jscore$z();
        final IStructureBody body = MovingStructures.at(Minecraft.getInstance().level,
                BlockPos.containing(x, y, z));
        if (body == null) {
            return;
        }
        // Given off this tick, as the structure is drawn when the tick has just begun: where it was the tick before.
        final StructureFrame frame = body instanceof ClientStructure drawn ? drawn.previousFrame() : body.frame();
        final double wx = frame.worldX(x, y, z);
        final double wy = frame.worldY(x, y, z);
        final double wz = frame.worldZ(x, y, z);
        particle.setPos(wx, wy, wz);
        open.jscore$setXo(wx);
        open.jscore$setYo(wy);
        open.jscore$setZo(wz);
        final double dx = open.jscore$xd();
        final double dy = open.jscore$yd();
        final double dz = open.jscore$zd();
        open.jscore$setXd(frame.turnX(dx, dy, dz));
        open.jscore$setYd(frame.turnY(dx, dy, dz));
        open.jscore$setZd(frame.turnZ(dx, dy, dz));
    }
}
