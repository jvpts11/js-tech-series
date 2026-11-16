/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitItemRenderer;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The item of a chain machine's own terminal, what the recipe viewers show the machine's kinds of recipe by: the formed
 * machine's model at rest, made small enough for the whole of it to fit a block's room, so it sits in a slot as a block
 * does however many blocks the machine fills.
 */
public final class ChainMachineItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final ResourceLocation model;
    private final KitItemRenderer drawn;

    private static final double HALF = 0.5;

    private ChainMachineItemRenderer(final ResourceLocation model) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.model = model;
        this.drawn = KitItemRenderer.of(model);
    }

    /** What the game asks the item of the machine whose formed model is {@code model} for, to be drawn this way. */
    public static IClientItemExtensions extensions(final ResourceLocation model) {
        final ChainMachineItemRenderer renderer = new ChainMachineItemRenderer(model);
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
        final Optional<KitModel> found = KitModels.get(this.model);
        if (found.isEmpty()) {
            return;
        }
        final AABB box = found.get().bounds();
        final float fit = (float) (1 / Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())));
        final Vec3 middle = box.getCenter();
        poses.pushPose();
        // The machine's middle at the block's, its longest side the block's; the kit's renderer stands a model half a
        // block in on its own, which the last step takes back.
        poses.translate(HALF, HALF, HALF);
        poses.scale(fit, fit, fit);
        poses.translate(-middle.x - HALF, -middle.y, -middle.z - HALF);
        this.drawn.renderByItem(stack, context, poses, buffers, light, overlay);
        poses.popPose();
    }
}
