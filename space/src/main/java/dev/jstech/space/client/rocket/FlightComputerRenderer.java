/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputer;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws the formed parts of a rocket whose plan its flight computer keeps, once its hangar let the rocket go: from the
 * computer, wherever the rocket is carried, aboard a crawler or set down on the pad, as the hangar's terminal drew them
 * in the bay.
 */
public final class FlightComputerRenderer implements BlockEntityRenderer<FlightComputerBlockEntity> {

    /* How far the rocket is drawn from, in blocks, and round a computer how far its rocket may reach. */
    private static final int SEEN_FROM = 256;
    private static final double REACH = 16;
    private static final double TALLEST = 256;
    private static final int CHUNK = 16;

    public FlightComputerRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the models are drawn as they are.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(FlightComputer.ENTITY.get(), FlightComputerRenderer::new));
    }

    @Override
    public void render(final FlightComputerBlockEntity computer, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Level level = computer.getLevel();
        final Optional<RocketDesign> rocket = computer.rocket();
        final Optional<BlockPos> foot = computer.foot();
        final List<Integer> formed = computer.formed();
        if (level == null || rocket.isEmpty() || foot.isEmpty() || formed.isEmpty()) {
            return;
        }
        final List<RocketPartModels.Placed> placed = RocketPartModels.of(RocketParts.all(level),
                RocketKits.all(level), rocket.get().stack(), false).stream()
                .filter(model -> formed.contains(model.section())).toList();
        poses.pushPose();
        poses.translate(foot.get().getX() + 0.5, foot.get().getY(), foot.get().getZ() + 0.5);
        // The cabin's live panels show the flight as the computer reads it, and on the ground the launch panel's count.
        RocketPartsRenderer.draw(computer, placed, computer.readings().orElseGet(() ->
                CabinReadings.AT_REST.clocked(computer.clock())), poses, buffers, light, overlay);
        poses.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final FlightComputerBlockEntity computer) {
        return true;
    }

    /* Round its rocket, so the rocket is drawn while any of it is in sight. */
    @Override
    public AABB getRenderBoundingBox(final FlightComputerBlockEntity computer) {
        final AABB self = new AABB(computer.getBlockPos());
        return computer.foot().map(foot -> self.minmax(new AABB(computer.getBlockPos().offset(foot))
                .inflate(REACH, 0, REACH).expandTowards(0, TALLEST, 0))).orElse(self);
    }

    /* As far as the game draws anything: a rocket climbing to space is seen from its pad all the way up. */
    @Override
    public int getViewDistance() {
        return Math.max(SEEN_FROM, Minecraft.getInstance().options.getEffectiveRenderDistance() * CHUNK);
    }
}
