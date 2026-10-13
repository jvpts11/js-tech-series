/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The test mod's entities drawn as nothing, as every entity a player's game meets has to be drawn somehow. */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestEntityRenderers {

    private TestEntityRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TestEntities.ROBOT.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.ROVER.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.BOLT.get(), NoopRenderer::new);
    }
}
