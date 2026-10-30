/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.depot;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.client.skin.Picture;
import dev.jstech.space.client.skin.SkinAtlas;
import dev.jstech.space.client.skin.SkinDrawing;
import dev.jstech.space.depot.DepotBlocks;
import dev.jstech.space.depot.DepotLayout;
import dev.jstech.space.depot.TransferPumpBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/**
 * Draws a formed propellant depot whole from its transfer pump, as the approved one is: the welded sphere in rings of
 * white plates, as wide as the cube it is built in; eight legs from its equator down to the ground, a brace from the
 * foot of each to the next; the vent stack on its top.
 */
public final class DepotRenderer implements BlockEntityRenderer<TransferPumpBlockEntity> {

    /* The sphere's rings from its foot to its top, and how many faces round; its legs and braces, in blocks. */
    private static final int RINGS = 14;
    private static final int FACETS = 40;
    private static final int LEGS = 8;
    private static final float LEG_IN = 0.98F;
    private static final float LEG = 0.45F;
    private static final float BRACE = 0.14F;
    private static final float BRACE_FOOT = 0.4F;
    private static final float BRACE_BELOW_EQUATOR = 1.2F;
    /* The vent stack: half its width, how far into the sphere it starts, how tall it stands over the top. */
    private static final float VENT_HALF = 0.35F;
    private static final float VENT_IN = 0.2F;
    private static final float VENT_HIGH = 2.2F;
    private static final int SEEN_FROM = 192;
    private static final float HALF = 0.5F;

    public DepotRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the depot is drawn from its measures each frame.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(DepotBlocks.PUMP_ENTITY.get(), DepotRenderer::new));
    }

    @Override
    public void render(final TransferPumpBlockEntity pump, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<DepotLayout.Depot> found = pump.depot();
        final Level level = pump.getLevel();
        if (found.isEmpty() || level == null) {
            return;
        }
        final DepotLayout.Depot depot = found.get();
        final SkinAtlas atlas = SkinAtlas.of(new Size(depot.edge()), () -> skins(depot.edge()));
        final SkinDrawing d = SkinDrawing.in(pose, buffers, atlas, level, pump.getBlockPos(), pump.getBlockPos(),
                Direction.EAST);
        final DepotSkins.Colours c = DepotSkins.PALETTE.get();
        final float r = depot.edge() / 2.0F;
        final BlockPos low = depot.low();
        final Vector3f centre = new Vector3f(low.getX() + r - HALF, low.getY() + r, low.getZ() + r - HALF);
        final float ground = low.getY() - depot.legs();
        for (int k = 0; k < RINGS; k++) {
            final double a0 = Math.PI * k / RINGS;
            final double a1 = Math.PI * (k + 1) / RINGS;
            d.band(centre, (float) (-r * Math.cos(a0)), (float) (-r * Math.cos(a1)),
                    (float) Math.max(0.02, r * Math.sin(a0)), (float) Math.max(0.02, r * Math.sin(a1)), FACETS,
                    "ring_" + k);
        }
        for (int k = 0; k < LEGS; k++) {
            final double a0 = k * 2 * Math.PI / LEGS;
            final double a1 = (k + 1) * 2 * Math.PI / LEGS;
            final Vector3f top = new Vector3f(centre.x() + r * LEG_IN * (float) Math.cos(a0), centre.y(),
                    centre.z() + r * LEG_IN * (float) Math.sin(a0));
            d.member(top, new Vector3f(top.x(), ground, top.z()), LEG, LEG, c.leg());
            d.member(new Vector3f(centre.x() + r * (float) Math.cos(a0), ground + BRACE_FOOT,
                            centre.z() + r * (float) Math.sin(a0)),
                    new Vector3f(centre.x() + r * (float) Math.cos(a1), centre.y() - BRACE_BELOW_EQUATOR,
                            centre.z() + r * (float) Math.sin(a1)), BRACE, BRACE, c.brace());
        }
        final float top = centre.y() + r;
        d.box(new float[] {centre.x() - VENT_HALF, top - VENT_IN, centre.z() - VENT_HALF},
                new float[] {centre.x() + VENT_HALF, top + VENT_HIGH, centre.z() + VENT_HALF},
                face -> face == SkinDrawing.Face.PLUS_Y ? SkinDrawing.Paint.tile(c.ventMouth())
                        : SkinDrawing.Paint.tile(c.vent()));
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final TransferPumpBlockEntity pump) {
        return true;
    }

    /* Round the whole depot, its legs and its vent stack. */
    @Override
    public AABB getRenderBoundingBox(final TransferPumpBlockEntity pump) {
        final BlockPos at = pump.getBlockPos();
        return pump.depot().map(depot -> new AABB(at).minmax(new AABB(at.offset(depot.low()))
                .expandTowards(depot.edge(), depot.edge() + VENT_HIGH + 1, depot.edge())
                .expandTowards(0, -depot.legs(), 0).inflate(1))).orElse(new AABB(at));
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /* The sphere's rings of plates for a sphere {@code edge} blocks wide, each sized to its band. */
    private static Map<String, Picture> skins(final int edge) {
        final Map<String, Picture> out = new HashMap<>();
        final double r = edge / 2.0;
        for (int k = 0; k < RINGS; k++) {
            final double a0 = Math.PI * k / RINGS;
            final double a1 = Math.PI * (k + 1) / RINGS;
            final double ya = -r * Math.cos(a0);
            final double yb = -r * Math.cos(a1);
            final double ra = Math.max(0.02, r * Math.sin(a0));
            final double rb = Math.max(0.02, r * Math.sin(a1));
            final double round = 2 * Math.PI * (ra + rb) / 2;
            out.put("ring_" + k, DepotSkins.ring(round, Math.hypot(yb - ya, rb - ra), 20 + k,
                    k == RINGS / 2 - 1 || k == RINGS / 2));
        }
        return out;
    }

    /* The atlas a depot's sphere needs, by its edge. */
    private record Size(int edge) {
    }
}
