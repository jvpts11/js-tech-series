/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.Portables;
import dev.jstech.core.client.live.ILivePainter;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a portable in a hand and on the ground as its model, held as the game holds an item (its item model says
 * how, higher and smaller than a flat item), its screen lit with what it shows at a glance: its start, then its lock.
 * In a slot and a frame it is its flat icon, which the game draws without this.
 */
public final class PortableItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float HALF_TURN = 180.0F;

    private static @Nullable PortableItemRenderer made;

    private PortableItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** What the game asks a portable's item for to be drawn this way; the renderer is made the first time. */
    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (made == null) {
                    made = new PortableItemRenderer();
                }
                return made;
            }
        };
    }

    @Override
    public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack poses,
                             final MultiBufferSource buffers, final int light, final int overlay) {
        if (!(stack.getItem() instanceof PortableItem item)) {
            return;
        }
        final UUID id = Portables.id(stack);
        // Only a device on, and in a hand, lights its screen: on the ground it lies dark.
        final boolean lit = id != null && Portables.isOn(stack) && context != ItemDisplayContext.GROUND;
        final ILivePainter painter = lit ? (g, width, height, partialTick) -> MobileDisplay.paint(g, stack,
                item.model(), null, -1.0, -1.0) : null;
        poses.pushPose();
        poses.translate(0.5F, 0.0F, 0.5F);
        // The model's front looks north; a held item shows the face a flat item's picture is on, its south.
        poses.mulPose(Axis.YP.rotationDegrees(HALF_TURN));
        PortableBodies.draw(stack, item.model(), context, poses, buffers, light, overlay, new Glance(id), painter);
        poses.popPose();
    }

    /* The picture a device shows at a glance, one for each device. */
    private record Glance(@Nullable UUID device) {
    }
}
