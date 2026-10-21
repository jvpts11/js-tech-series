/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.model.kit.KitEntityRenderer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestModels;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The test mod's robots and shots drawn as nothing, as every entity a player's game meets has to be drawn somehow;
 * its creatures drawn as the test walker, walking, so one put in a world is seen.
 */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestEntityRenderers {

    private TestEntityRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TestEntities.ROBOT.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.WORKER.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.FLYER.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.DIVER.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.SLUG.get(), NoopRenderer::new);
        event.registerEntityRenderer(TestEntities.CRITTER.get(), context -> KitEntityRenderer.of(context,
                TestModels.WALKER.id(), (critter, animator, seconds) -> animator.play("main", "Walk", seconds)));
        event.registerEntityRenderer(TestEntities.CHOKER.get(), context -> KitEntityRenderer.of(context,
                TestModels.WALKER.id(), (choker, animator, seconds) -> animator.play("main", "Walk", seconds)));
    }
}
