/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Draws an item with a model of the Core's, wherever the game draws items: in a hand, held up in first person or seen
 * from outside, in a slot, on the ground, in a frame. The item's own model file says how it is turned and sized in
 * each place (its {@code display}, with {@code builtin/entity} for parent); the model stands on the middle of the
 * item's floor. An item moves apart in each place it is drawn, so the one in a slot does not play what the one in the
 * hand plays; a stack the server animates moves alike everywhere.
 *
 * <pre>{@code
 * event.registerItem(KitItemRenderer.of(ResourceLocation.fromNamespaceAndPath(MODID, "drill")).extensions(),
 *         MyItems.DRILL.get());
 * }</pre>
 */
public final class KitItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final Function<ItemStack, ResourceLocation> model;
    private final IKitDriver<ItemStack> driver;
    private final IHeldThings<ItemStack> held;

    private KitItemRenderer(final Function<ItemStack, ResourceLocation> model, final IKitDriver<ItemStack> driver,
                            final IHeldThings<ItemStack> held) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.model = model;
        this.driver = driver;
        this.held = held;
    }

    /** A renderer drawing every item of its kind with the model {@code model}. */
    public static KitItemRenderer of(final ResourceLocation model) {
        return of(stack -> model, IKitDriver.none(), IHeldThings.none());
    }

    /** A renderer drawing each stack with the model it names, driven and holding as given. */
    public static KitItemRenderer of(final Function<ItemStack, ResourceLocation> model,
                                     final IKitDriver<ItemStack> driver, final IHeldThings<ItemStack> held) {
        return new KitItemRenderer(Objects.requireNonNull(model, "model"), Objects.requireNonNull(driver, "driver"),
                Objects.requireNonNull(held, "held"));
    }

    /** What the game asks an item for to be drawn this way. */
    public IClientItemExtensions extensions() {
        final KitItemRenderer renderer = this;
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
        final Optional<KitModel> found = KitModels.get(this.model.apply(stack));
        if (found.isEmpty()) {
            return;
        }
        final KitInstance instance = KitAnimations.of(stack, context, found.get());
        final double seconds = KitClock.now();
        this.driver.drive(stack, instance.animator(), seconds);
        instance.animate(KitClock.frame(), seconds, this.driver.movesBones(), IAnimationListener.NONE);
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, stack, poses, buffers, light, overlay, this.held,
                Minecraft.getInstance().level, false);
        poses.popPose();
    }
}
