/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.advancement;

import dev.jstech.core.JsCore;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * An advancement's icon drawn as a picture rather than as an item, for what has no item to show: a rocket in flight,
 * the Moon, Mars. The game shows an advancement by an item stack, on its tab, in its tree, in its tooltip and in the
 * toast that announces it; a stack that carries a picture is drawn as that picture, 16 pixels square, wherever the
 * game would draw the item.
 *
 * <pre>{@code
 * this.root(AdvancementIcons.of("mymod:gui/advancements/rocket"), "Leaving the Earth", ...);
 * }</pre>
 */
public final class AdvancementIcons {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JsCore.MODID);

    /** The picture a stack is drawn as, {@code <namespace>:<path>} under {@code textures/}, without its extension. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> PICTURE =
            COMPONENTS.registerComponentType("picture", builder -> builder.persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private AdvancementIcons() {
    }

    /** Registers the picture's component, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    /** An icon drawn as {@code picture}, a texture written {@code <namespace>:<path>} under {@code textures/}. */
    public static ItemStack of(final String picture) {
        final ItemStack icon = new ItemStack(Items.PAPER);
        icon.set(PICTURE.get(), ResourceLocation.parse(picture));
        return icon;
    }

    /** The texture {@code stack} is drawn as, its whole path; none for a stack that is drawn as its item. */
    public static @Nullable ResourceLocation pictureOf(final ItemStack stack) {
        final ResourceLocation picture = stack.get(PICTURE.get());
        return picture == null ? null : picture.withPrefix("textures/").withSuffix(".png");
    }
}
