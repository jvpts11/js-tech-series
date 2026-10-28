/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.client.moving.StructureMeshes;
import dev.jstech.core.client.sky.GroundCovers;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a moving structure's blocks as the game draws the world's, though they are out in the yard:
 * <ul>
 *   <li>the game marks the section of a changed block to be built again, and a section of the yard is out of the
 *       land it builds; a section of the yard is marked for its structure's own drawing ({@link StructureMeshes});</li>
 *   <li>the game gives a block's flame, smoke or drip off at the block's place, and lets go of a particle that far
 *       from the eye before anything sees it; a particle of the yard is given off where its block is drawn now, its
 *       speed turned as the structure is;</li>
 *   <li>the game draws an entity only where the section of the world it stands in is built, and a structure's entity
 *       stands at its pivot, which may be in a section hidden or not built while its blocks are in sight; a
 *       structure's entity is drawn wherever it stands, its own box deciding whether it is seen.</li>
 *   <li>while a mod draws a level's ground itself ({@link GroundCovers}), the game's own blocks and clouds are not
 *       drawn, though everything hung on the stages of the blocks still is.</li>
 * </ul>
 * Every other section, particle and entity is let be.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void jscore$redrawStructures(final int x, final int y, final int z, final boolean now,
                                         final CallbackInfo info) {
        if (YardLayout.isYardChunk(x)) {
            StructureMeshes.dirty(x, y, z);
            info.cancel();
            return;
        }
        if (ClientMovingStructures.holdsBack(x, y, z)) {
            info.cancel();
        }
    }

    @WrapMethod(method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)"
            + "Lnet/minecraft/client/particle/Particle;")
    private @Nullable Particle jscore$particleWhereTheBlockIs(final ParticleOptions options, final boolean force,
                                                              final boolean decreased, final double x,
                                                              final double y, final double z, final double xSpeed,
                                                              final double ySpeed, final double zSpeed,
                                                              final Operation<Particle> original) {
        final ClientStructure structure = YardLayout.isYard(x) ? ClientMovingStructures.byPlot(
                YardLayout.plotOfBlock((int) Math.floor(x), (int) Math.floor(z))) : null;
        if (structure == null) {
            return original.call(options, force, decreased, x, y, z, xSpeed, ySpeed, zSpeed);
        }
        // Given off this tick, as the structure is drawn when the tick has just begun: where it was the tick before.
        final StructureFrame frame = structure.previousFrame();
        return original.call(options, force, decreased, frame.worldX(x, y, z), frame.worldY(x, y, z),
                frame.worldZ(x, y, z), frame.turnX(xSpeed, ySpeed, zSpeed), frame.turnY(xSpeed, ySpeed, zSpeed),
                frame.turnZ(xSpeed, ySpeed, zSpeed));
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean jscore$drawStructuresWhereverTheyStand(final LevelRenderer renderer, final BlockPos pos,
                                                           final Operation<Boolean> original) {
        return original.call(renderer, pos) || ClientMovingStructures.pivotAt(pos);
    }

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"))
    private void jscore$groundDrawnByItsMod(final VertexBuffer section, final Operation<Void> original) {
        if (!GroundCovers.covered()) {
            original.call(section);
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void jscore$cloudsDrawnByTheirMod(final PoseStack pose, final Matrix4f frustum, final Matrix4f projection,
                                              final float partialTick, final double camX, final double camY,
                                              final double camZ, final CallbackInfo info) {
        if (GroundCovers.covered()) {
            info.cancel();
        }
    }
}
