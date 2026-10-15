/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.fluid;

import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.material.MaterialLiquid;
import dev.jstech.core.material.MaterialLiquids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The declared fluids on a player's game: each drawn with the textures and the tint it was declared with, in the
 * world and in its bucket, and a liquid poured into the world drawn see-through, as the game's water is. The materials'
 * liquids the installed mods use are the game's water in their material's colour.
 */
public final class FluidsClient {

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    private FluidsClient() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientExtensionsEvent.class, FluidsClient::onClientExtensions);
        modBus.addListener(RegisterColorHandlersEvent.Item.class, FluidsClient::onItemColours);
        modBus.addListener(FMLClientSetupEvent.class, FluidsClient::onClientSetup);
    }

    public static void onClientExtensions(final RegisterClientExtensionsEvent event) {
        // A material's liquid is the game's water drawn in its material's colour.
        for (final MaterialLiquid liquid : MaterialLiquids.registered()) {
            final int tint = 0xFF << 24 | liquid.material().colour();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return WATER_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return WATER_FLOW;
                }

                @Override
                public int getTintColor() {
                    return tint;
                }
            }, liquid.fluid().getFluidType());
        }
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
    public static void onItemColours(final RegisterColorHandlersEvent.Item event) {
        for (final Item bucket : MaterialLiquids.buckets()) {
            event.register(new DynamicFluidContainerModel.Colors(), bucket);
        }
        for (final ModContent content : ModContent.all()) {
            for (final FluidEntry fluid : content.declaredFluids()) {
                fluid.bucket().ifPresent(bucket -> event.register(new DynamicFluidContainerModel.Colors(),
                        bucket.get()));
            }
        }
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (final MaterialLiquid liquid : MaterialLiquids.registered()) {
                ItemBlockRenderTypes.setRenderLayer(liquid.fluid(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(BuiltInRegistries.FLUID.get(liquid.id().withPrefix("flowing_")),
                        RenderType.translucent());
            }
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
