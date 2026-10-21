/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the player chose, in the open window, the recipe {@code recipe} among those that fit; the server
 * takes it only when it is one of those it told of for that window.
 *
 * @param containerId the window
 * @param recipe      the recipe chosen, by id
 */
public record RecipeChosenPayload(int containerId, ResourceLocation recipe) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RecipeChosenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "recipe_chosen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeChosenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RecipeChosenPayload::containerId,
                    ResourceLocation.STREAM_CODEC, RecipeChosenPayload::recipe,
                    RecipeChosenPayload::new);

    @Override
    public CustomPacketPayload.Type<RecipeChosenPayload> type() {
        return TYPE;
    }
}
