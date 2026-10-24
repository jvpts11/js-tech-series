/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a cabinet's item as the cabinet: the same model the world shows, scaled into the slot, as the machine comes,
 * empty and switched off. A cabinet is several blocks around its origin block, so it is shrunk to fit a
 * single block of room and re-centred on the item's own middle; otherwise it would render at full size, mostly
 * outside the slot. An item in a slot is a still photograph of the cabinet: nothing plays on it.
 */
public final class CabinetItemRenderer extends BlockEntityWithoutLevelRenderer {

    /* Which bones each cabinet's model hides in the slot, worked out once from their names. */
    private final Map<ModelData, boolean[]> asItComes = new IdentityHashMap<>();
    private long readAt = Long.MIN_VALUE;

    /** How much of the item's own block of room the cabinet fills. */
    private static final float FILL = 0.86F;
    /** Where the cabinet stands in the item's room before it is fitted: the middle, a hair above the floor. */
    private static final float FLOOR = 0.51F;

    private static @Nullable CabinetItemRenderer made;

    private CabinetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** What the game asks a cabinet's item for to be drawn this way; the renderer is made the first time it is. */
    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (made == null) {
                    made = new CabinetItemRenderer();
                }
                return made;
            }
        };
    }

    @Override
    public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack poses,
                             final MultiBufferSource buffers, final int light, final int overlay) {
        if (!(stack.getItem() instanceof CabinetBlockItem item)) {
            return;
        }
        final Optional<KitModel> found = KitModels.look(item.modelResource(), item.textureResource(),
                item.animationResource());
        if (found.isEmpty()) {
            return;
        }
        final KitInstance instance = KitAnimations.of(stack, context, found.get());
        // Shows the parts of the machine as it comes and hides the fitted and the lit ones.
        final ModelPose pose = instance.pose();
        final boolean[] hidden = hidden(item, found.get().data());
        for (int i = 0; i < hidden.length; i++) {
            pose.setHidden(i, hidden[i]);
        }
        poses.pushPose();
        poses.translate(0.5F, FLOOR, 0.5F);
        /*
         * 16 model units make a block: shrink the cabinet's longest side to FILL of one block, then bring the middle of
         * the cabinet onto the item's own middle. Scaling first means the offset is read in blocks of the cabinet's own
         * space, which is how the fit is written.
         */
        final CabinetBlockItem.Fit fit = item.fit();
        final float scale = FILL * 16.0F / fit.span();
        poses.scale(scale, scale, scale);
        poses.translate(fit.offsetX(), fit.offsetY(), fit.offsetZ());
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, stack, poses, buffers, light, overlay, null,
                Minecraft.getInstance().level, false);
        poses.popPose();
    }

    /* Which of a cabinet's bones its slot hides: the fitted and the lit ones, a model of one cabinet each. */
    private boolean[] hidden(final CabinetBlockItem item, final ModelData model) {
        if (this.readAt != KitModels.reloadedAt()) {
            this.asItComes.clear();
            this.readAt = KitModels.reloadedAt();
        }
        return this.asItComes.computeIfAbsent(model, data -> {
            final boolean[] out = new boolean[data.bones().size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = !item.shownInSlot(data.bones().get(i).name());
            }
            return out;
        });
    }
}
