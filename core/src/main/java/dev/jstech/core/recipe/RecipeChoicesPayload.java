/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the recipes that fit what is in the open window's grid or furnace, what each makes and which is
 * used; none when one recipe fits, or none, and the chooser hides.
 *
 * @param containerId the window they are of
 * @param recipes     the recipes that fit, by id
 * @param results     what each makes, in the same order
 * @param chosen      the place of the one used
 * @param furnace     whether the window is a furnace's, whose choice is kept for what it smelts
 * @param input       what the furnace smelts, for its words; empty for a grid
 */
public record RecipeChoicesPayload(int containerId, List<ResourceLocation> recipes, List<ItemStack> results,
                                   int chosen, boolean furnace, ItemStack input) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RecipeChoicesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "recipe_choices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeChoicesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RecipeChoicesPayload::containerId,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeChoicesPayload::recipes,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeChoicesPayload::results,
                    ByteBufCodecs.VAR_INT, RecipeChoicesPayload::chosen,
                    ByteBufCodecs.BOOL, RecipeChoicesPayload::furnace,
                    ItemStack.OPTIONAL_STREAM_CODEC, RecipeChoicesPayload::input,
                    RecipeChoicesPayload::new);

    public RecipeChoicesPayload {
        recipes = List.copyOf(recipes);
        results = List.copyOf(results);
    }

    /** Nothing to choose between in the window {@code containerId}. */
    public static RecipeChoicesPayload none(final int containerId) {
        return new RecipeChoicesPayload(containerId, List.of(), List.of(), 0, false, ItemStack.EMPTY);
    }

    @Override
    public CustomPacketPayload.Type<RecipeChoicesPayload> type() {
        return TYPE;
    }
}
