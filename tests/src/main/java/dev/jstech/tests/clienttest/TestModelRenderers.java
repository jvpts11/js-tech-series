/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.mechanical.TurningParts;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitArmorModel;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitEntityRenderer;
import dev.jstech.core.client.model.kit.KitItemRenderer;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMechanical;
import dev.jstech.tests.TestModels;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The renderers of what the tests draw with the Core's models, registered as another mod registers its own: the press,
 * holding an iron ingot on its plate and counting the words its strike hands to code; the dock drawn from J's
 * Computers' files; the crate as an item; the helmet as armour; the walker, its head turning as it walks; and the
 * shaft, the gears and the pulleys turning as fast as their network turns them, the belts between pulleys running.
 */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestModelRenderers {

    /** How many words the press's animations handed to code; for a test to look at. */
    public static final AtomicInteger PRESS_WORDS = new AtomicInteger();
    /* How long the turbine's rotor takes to come to rest once it stops working, in seconds. */
    private static final double TURBINE_SPIN_DOWN = 0.5;

    private TestModelRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestModels.PRESS_ENTITY.get(), context -> KitBlockEntityRenderer
                .<TestModels.PressEntity>of(TestModels.PRESS.id())
                .driver(new PressDriver())
                .held((press, locator) -> locator.equals("plate") ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY)
                .build());
        event.registerBlockEntityRenderer(TestModels.DOCK_ENTITY.get(), context -> KitBlockEntityRenderer
                .<TestModels.DockEntity>look(TestModels.DOCK).build());
        event.registerEntityRenderer(TestModels.WALKER_ENTITY.get(), context -> KitEntityRenderer.of(context,
                TestModels.WALKER.id(), (walker, animator, seconds) -> animator.play("main", "Walk", seconds)));
        event.registerBlockEntityRenderer(TestModels.TURBINE_ENTITY.get(), context -> KitBlockEntityRenderer
                .<TestModels.TurbineEntity>formedOnly(TestModels.TURBINE.id())
                .driver((turbine, animator, seconds) -> {
                    if (turbine.running()) {
                        animator.play("main", "working", seconds);
                    } else if (animator.controller("main").playing() != null) {
                        animator.controller("main").stop(seconds, TURBINE_SPIN_DOWN);
                    }
                })
                .build());
        TurningParts.draw(TestMechanical.SHAFT, TestModels.SHAFT.id());
        TurningParts.draw(TestMechanical.SMALL_GEAR, TestModels.GEAR.id());
        TurningParts.draw(TestMechanical.LARGE_GEAR, TestModels.GEAR.id());
        TurningParts.draw(TestMechanical.SMALL_PULLEY, TestModels.SMALL_PULLEY.id());
        TurningParts.draw(TestMechanical.LARGE_PULLEY, TestModels.LARGE_PULLEY.id());
        TurningParts.register(event, TestMechanical.PART.get());
    }

    @SubscribeEvent
    public static void onRegisterExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(KitItemRenderer.of(TestModels.CRATE.id()).extensions(), TestModels.CRATE_ITEM.get());
        event.registerItem(KitItemRenderer.of(TestModels.ARMS.id()).extensions(), TestModels.ARMS_ITEM.get());
        event.registerItem(KitArmorModel.of(TestModels.HELMET.id()).extensions(), TestModels.HELMET_ITEM.get());
    }

    /* The press plays what the server tells it to; the words its animations hand to code are counted. */
    private static final class PressDriver implements IKitDriver<TestModels.PressEntity> {

        @Override
        public void drive(final TestModels.PressEntity press, final ModelAnimator animator, final double seconds) {
            // What it plays comes from the server.
        }

        @Override
        public void instruction(final TestModels.PressEntity press, final ModelAnimation animation,
                                final String word) {
            PRESS_WORDS.incrementAndGet();
        }
    }
}
