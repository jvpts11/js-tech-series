/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A recipe a player moved into the machine whose screen is open, with a recipe viewer's transfer button: its
 * ingredients from the player's inventory into the machine's inputs, and the recipe picked among those they make.
 *
 * @param recipe the recipe
 * @param all    whether to move as many sets as a stack holds; one set otherwise
 */
public record MachineRecipePayload(ResourceLocation recipe, boolean all) implements CustomPacketPayload {

    public static final Type<MachineRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "machine_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipePayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, MachineRecipePayload::recipe,
                    ByteBufCodecs.BOOL, MachineRecipePayload::all, MachineRecipePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
