/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.client.ship.ShipPartsRenderer;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestShips;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The ship stand's renderer: the ship's formed parts on the stand, as J's Space draws them, on the block grid as the
 * shipyard lays them: its centre line along the stand's west side, its nose at the stand's north side, its gear on the
 * stand's floor, the ship lying toward the south.
 */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestShipRenderers {

    /* How far the hull's floor stands over the ground the gear stands on, in blocks. */
    private static final double GEAR = 2;
    /* How wide and how long a ship on the stand may be drawn, in blocks. */
    private static final double WIDE = 10;
    private static final double LONG = 24;
    private static final double TALL = 10;
    private static final int SEEN_FROM = 160;

    private TestShipRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestShips.DISPLAY_ENTITY.get(), context -> new DisplayRenderer());
    }

    /* Draws the stand's ship from the stand's north-west corner. */
    private static final class DisplayRenderer implements BlockEntityRenderer<TestShips.DisplayEntity> {

        @Override
        public void render(final TestShips.DisplayEntity stand, final float partialTick, final PoseStack poses,
                           final MultiBufferSource buffers, final int light, final int overlay) {
            if (stand.getLevel() == null) {
                return;
            }
            final ShipDesign ship = stand.design();
            poses.pushPose();
            poses.translate(0, GEAR, 0);
            ShipPartsRenderer.draw(stand, ShipPartModels.of(ShipParts.all(stand.getLevel()),
                    ShipKits.all(stand.getLevel()), ship.spine(), ship.hung(), stand.finish()), stand.readings(), poses,
                    buffers, light, overlay);
            poses.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(final TestShips.DisplayEntity stand) {
            return new AABB(stand.getBlockPos()).inflate(WIDE, 0, 0).expandTowards(0, TALL, LONG);
        }

        @Override
        public int getViewDistance() {
            return SEEN_FROM;
        }
    }
}
