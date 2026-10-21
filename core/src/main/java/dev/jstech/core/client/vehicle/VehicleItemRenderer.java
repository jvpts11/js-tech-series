/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.IHeldThings;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.vehicle.VehicleDef;
import dev.jstech.core.vehicle.VehiclePartDef;
import dev.jstech.core.vehicle.VehiclePartStack;
import dev.jstech.core.vehicle.VehicleParts;
import dev.jstech.core.vehicle.VehicleStack;
import dev.jstech.core.vehicle.Vehicles;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Draws a vehicle's item and a part's: as the item its kind names for its look, turned and sized as that item is in
 * each place it is drawn; and a vehicle with none, as its own model brought down to a block's size, seen from above
 * and aside in a slot as a block is.
 */
public final class VehicleItemRenderer extends BlockEntityWithoutLevelRenderer {

    /* A slot shows a model seen from above and aside, as the game shows a block. */
    private static final float SLOT_TILT = 30;
    private static final float SLOT_TURN = 225;
    private static final float FILLS = 0.9F;

    public VehicleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** What the game asks an item for to be drawn this way. */
    public IClientItemExtensions extensions() {
        final VehicleItemRenderer renderer = this;
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
    }

    @Override
    public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack poses,
                             final MultiBufferSource buffers, final int light, final int overlay) {
        final VehicleStack vehicle = stack.get(Vehicles.VEHICLE.get());
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        Optional<ResourceLocation> icon = Optional.empty();
        Optional<VehicleDef> def = Optional.empty();
        if (vehicle != null) {
            def = Vehicles.anywhere(vehicle.vehicle());
            icon = def.flatMap(VehicleDef::icon);
        } else if (part != null) {
            icon = VehicleParts.anywhere(part.part()).flatMap(VehiclePartDef::icon);
        }
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        if (icon.isPresent() || def.isEmpty()) {
            final ItemStack drawn = new ItemStack(icon.map(BuiltInRegistries.ITEM::get).orElse(Items.IRON_INGOT));
            Minecraft.getInstance().getItemRenderer().renderStatic(drawn, context, light, overlay, poses, buffers,
                    Minecraft.getInstance().level, 0);
        } else {
            model(stack, context, def.get(), poses, buffers, light, overlay);
        }
        poses.popPose();
    }

    /* The vehicle's own model, brought down to a block's size. */
    private static void model(final ItemStack stack, final ItemDisplayContext context, final VehicleDef def,
                              final PoseStack poses, final MultiBufferSource buffers, final int light,
                              final int overlay) {
        final Optional<KitModel> found = def.model().flatMap(KitModels::get);
        if (found.isEmpty()) {
            return;
        }
        final KitInstance instance = KitAnimations.of(stack, context, found.get());
        instance.animate(KitClock.frame(), KitClock.now(), false, IAnimationListener.NONE);
        if (context == ItemDisplayContext.GUI) {
            poses.mulPose(Axis.XP.rotationDegrees(SLOT_TILT));
            poses.mulPose(Axis.YP.rotationDegrees(SLOT_TURN));
        }
        final float largest = Math.max(1, Math.max(def.body().width(), def.body().height()));
        final float scale = FILLS / largest;
        poses.scale(scale, scale, scale);
        poses.translate(0, -def.body().height() / 2, 0);
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, stack, poses, buffers, light, overlay,
                IHeldThings.none(), Minecraft.getInstance().level, false);
    }
}
