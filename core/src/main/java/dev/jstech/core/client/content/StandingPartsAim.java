/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.content;

import dev.jstech.core.content.IStandingParts;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The aim at the {@link IStandingParts parts that stand out of blocks}: once the game has worked out what the
 * crosshair is on, a look that crosses such a part nearer than that lands on the part's block instead, at the point
 * it meets the part, so the part is outlined and clicked as the block's own.
 *
 * <p>A part stands at most a block out of its own, so the blocks the look passes through and their neighbours are all
 * that can own one: a few dozen block lookups a frame, and nothing more while no such block is near.
 */
public final class StandingPartsAim {

    /** How far along the look each sample is taken, in blocks: under a block, so no block the look crosses is missed. */
    private static final double STEP = 0.25;

    private StandingPartsAim() {
    }

    /** Called after the game has aimed for the frame, with what it found already in {@code minecraft.hitResult}. */
    public static void afterPick(final Minecraft minecraft, final float partialTick) {
        final Entity camera = minecraft.getCameraEntity();
        final ClientLevel level = minecraft.level;
        if (camera == null || level == null || minecraft.player == null) {
            return;
        }
        final double reach = minecraft.player.blockInteractionRange();
        final Vec3 eye = camera.getEyePosition(partialTick);
        final Vec3 view = camera.getViewVector(partialTick);
        final HitResult found = minecraft.hitResult;
        final Look look = new Look(eye, eye.add(view.scale(reach)), Direction.getNearest(-view.x, -view.y, -view.z),
                found == null || found.getType() == HitResult.Type.MISS ? reach * reach
                        : found.getLocation().distanceToSqr(eye));
        final LongOpenHashSet looked = new LongOpenHashSet();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (double t = 0; t <= reach; t += STEP) {
            at.set(eye.x + view.x * t, eye.y + view.y * t, eye.z + view.z * t);
            consider(level, at, look, looked);
            for (final Direction side : Direction.values()) {
                consider(level, near.setWithOffset(at, side), look, looked);
            }
        }
        if (look.part != null) {
            minecraft.hitResult = look.part;
            minecraft.crosshairPickEntity = null;
        }
    }

    /* The parts of the block at {@code pos}, if it has any, against the look, each block once a frame. */
    private static void consider(final ClientLevel level, final BlockPos pos, final Look look,
                                 final LongOpenHashSet looked) {
        if (!looked.add(pos.asLong())) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IStandingParts parts)) {
            return;
        }
        for (final AABB box : parts.standingParts(state, level, pos)) {
            final Optional<Vec3> hit = box.move(pos).clip(look.eye, look.end);
            if (hit.isPresent() && hit.get().distanceToSqr(look.eye) < look.nearest) {
                look.nearest = hit.get().distanceToSqr(look.eye);
                look.part = new BlockHitResult(hit.get(), look.facing, pos.immutable(), false);
            }
        }
    }

    /* The look of one frame: from the eye to the end of the reach, and the nearest thing found on it so far. */
    private static final class Look {

        private final Vec3 eye;
        private final Vec3 end;
        private final Direction facing;
        private double nearest;
        @Nullable
        private BlockHitResult part;

        private Look(final Vec3 eye, final Vec3 end, final Direction facing, final double nearest) {
            this.eye = eye;
            this.end = end;
            this.facing = facing;
            this.nearest = nearest;
        }
    }
}
