/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.client.rocket.RocketPartsRenderer;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a formed Rocket Hangar's crane and work platforms whole, as the approved hangar has them, and the rocket it
 * builds, as far as it is built: each part of its plan that has formed, its blocks merged and drawn as nothing, is
 * drawn with its own model where it stands on the bay's floor, its cabin at rest. The parts not yet formed are their
 * kit's blocks, drawn by the world as any block is.
 */
public final class HangarTerminalRenderer implements BlockEntityRenderer<HangarTerminalBlockEntity> {

    /* How far the rocket is drawn from, in blocks: as far as a rocket in a hangar is seen. */
    private static final int SEEN_FROM = 256;
    /* Round a plan whose bay is not known: as wide as the widest rocket, as tall as the tallest bay. */
    private static final double REACH = 16;
    private static final double TALLEST = 256;

    public HangarTerminalRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the models are drawn as they are.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(HangarBlocks.TERMINAL_ENTITY.get(), HangarTerminalRenderer::new));
    }

    @Override
    public void render(final HangarTerminalBlockEntity terminal, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Level level = terminal.getLevel();
        if (level == null) {
            return;
        }
        HangarPieces.draw(terminal, level, poses, buffers);
        final Optional<RocketDesign> plan = terminal.plan();
        final Optional<BlockPos> at = terminal.planAt();
        final List<Integer> formed = terminal.formedParts();
        if (plan.isEmpty() || at.isEmpty() || formed.isEmpty()) {
            return;
        }
        final List<RocketPartModels.Placed> placed = RocketPartModels.of(RocketParts.all(level),
                RocketKits.all(level), plan.get().stack(), false).stream()
                .filter(model -> formed.contains(model.section())).toList();
        final BlockPos from = terminal.getBlockPos();
        final BlockPos foot = at.get();
        poses.pushPose();
        poses.translate(foot.getX() + 0.5 - from.getX(), foot.getY() - from.getY(), foot.getZ() + 0.5 - from.getZ());
        final int lit = LevelRenderer.getLightColor(level, foot.above());
        RocketPartsRenderer.draw(terminal, placed, CabinReadings.AT_REST, poses, buffers, lit, overlay);
        poses.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final HangarTerminalBlockEntity terminal) {
        return true;
    }

    /* The whole bay, where the rocket stands, so it is drawn while any of it is in sight. */
    @Override
    public AABB getRenderBoundingBox(final HangarTerminalBlockEntity terminal) {
        final Optional<HangarBay> bay = terminal.bay();
        final AABB self = new AABB(terminal.getBlockPos());
        if (bay.isPresent()) {
            return self.minmax(new AABB(bay.get().low().getX(), bay.get().low().getY(), bay.get().low().getZ(),
                    bay.get().high().getX() + 1, bay.get().high().getY() + 1, bay.get().high().getZ() + 1));
        }
        return terminal.planAt().map(foot -> self.minmax(new AABB(foot).inflate(REACH, 0, REACH)
                .expandTowards(0, TALLEST, 0))).orElse(self);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }
}
