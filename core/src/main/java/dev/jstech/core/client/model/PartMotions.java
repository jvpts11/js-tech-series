/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.multipart.ISpaceHolder;
import dev.jstech.core.multipart.MovingParts;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.multipart.SpaceParts;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The parts that move, drawn on the player's game: each kind marked as moving ({@link PartType#moving()}) hands its
 * {@link IPartMotion} here from its mod's client setup, and each frame every space in sight noted as holding one
 * ({@link MovingParts}) has the moving pieces of its parts drawn, each turned to its face and lit by the light at the
 * block. A world with no moving part draws nothing here.
 *
 * <pre>{@code
 * modBus.addListener(FMLClientSetupEvent.class, event -> PartMotions.register(MyParts.FAN.get(),
 *         (pose, buffers, busy, time, light) -> drawBlades(pose, buffers, busy ? time * 18F : 0F, light)));
 * }</pre>
 */
public final class PartMotions {

    private static final Map<PartType<?>, IPartMotion> MOTIONS = new ConcurrentHashMap<>();
    private static final double BLOCKS_A_CHUNK = 16.0;
    private static final float MIDDLE = 0.5F;

    private PartMotions() {
    }

    /** Hands {@code motion} the drawing of the moving pieces of the parts of {@code type}, a kind marked moving. */
    public static void register(final PartType<?> type, final IPartMotion motion) {
        if (!type.moves()) {
            throw new IllegalArgumentException("the part type " + type.id() + " is not marked as moving; mark it "
                    + "with moving() where it is made");
        }
        MOTIONS.put(type, motion);
    }

    /** What draws the moving pieces of {@code type}'s parts, or none. */
    public static @Nullable IPartMotion of(final PartType<?> type) {
        return MOTIONS.get(type);
    }

    /** Hands the game's bus the drawing, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, PartMotions::onRender);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> MovingParts.clear());
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final Frustum frustum = event.getFrustum();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final float time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final double reach = minecraft.options.getEffectiveRenderDistance() * BLOCKS_A_CHUNK;
        final boolean[] drew = {false};
        MovingParts.visit(pos -> {
            if (!(level.getBlockEntity(pos) instanceof ISpaceHolder holder) || !holder.parts().anyMoving()) {
                return false;
            }
            if (pos.distToCenterSqr(camera) <= reach * reach && frustum.isVisible(new AABB(pos))) {
                drew[0] |= draw(holder.parts(), pos, pose, buffers, camera, time,
                        LevelRenderer.getLightColor(level, pos));
            }
            return true;
        });
        if (drew[0]) {
            buffers.endBatch();
        }
    }

    /* The moving pieces of every part of the space that has them; whether any was drawn. */
    private static boolean draw(final SpaceParts parts, final BlockPos pos, final PoseStack pose,
                                final MultiBufferSource buffers, final Vec3 camera, final float time,
                                final int light) {
        boolean drew = false;
        for (final Direction face : Direction.values()) {
            final PartType<?> type = parts.type(face);
            final IPartMotion motion = type == null ? null : MOTIONS.get(type);
            if (motion == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            pose.rotateAround(FaceTurn.of(face), MIDDLE, MIDDLE, MIDDLE);
            motion.draw(pose, buffers, parts.busy(face), time, light);
            pose.popPose();
            drew = true;
        }
        return drew;
    }
}
