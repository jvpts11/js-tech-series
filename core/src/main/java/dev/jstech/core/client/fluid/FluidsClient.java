/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.fluid;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.content.ModContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The declared fluids on a player's game: each drawn with the textures and the tint it was declared with, in the
 * world and in its bucket, and a liquid poured into the world drawn see-through, as the game's water is.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class FluidsClient {

    private FluidsClient() {
    }

    @SubscribeEvent
    public static void onClientExtensions(final RegisterClientExtensionsEvent event) {
        for (final ModContent content : ModContent.all()) {
            for (final FluidEntry fluid : content.declaredFluids()) {
                final FluidEntry.Look look = fluid.look();
                event.registerFluidType(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return look.still();
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return look.flowing();
                    }

                    @Override
                    public int getTintColor() {
                        return look.tint();
                    }
                }, fluid.type());
            }
        }
    }

    /* A bucket's fluid is drawn in the fluid's own tint. */
    @SubscribeEvent
    public static void onItemColours(final RegisterColorHandlersEvent.Item event) {
        for (final ModContent content : ModContent.all()) {
            for (final FluidEntry fluid : content.declaredFluids()) {
                fluid.bucket().ifPresent(bucket -> event.register(new DynamicFluidContainerModel.Colors(),
                        bucket.get()));
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (final ModContent content : ModContent.all()) {
                for (final FluidEntry fluid : content.declaredFluids()) {
                    if (!fluid.gas()) {
                        ItemBlockRenderTypes.setRenderLayer(fluid.source(), RenderType.translucent());
                        ItemBlockRenderTypes.setRenderLayer(fluid.flowing(), RenderType.translucent());
                    }
                }
            }
        });
    }
}
