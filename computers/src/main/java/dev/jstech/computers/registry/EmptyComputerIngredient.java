/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * A computer as a recipe takes it, empty (computers/docs/design/recipes.md, "The desk computers"): a case with no
 * parts in it, never one a player has built, so that what a recipe makes of a Personal Computer (a Crafting Computer,
 * a Cluster Management Computer) never eats a computer's parts.
 *
 * @param computer the computer, as an item
 */
public record EmptyComputerIngredient(Item computer) implements ICustomIngredient {

    public static final MapCodec<EmptyComputerIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(EmptyComputerIngredient::computer))
            .apply(instance, EmptyComputerIngredient::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmptyComputerIngredient> STREAM_CODEC =
            ByteBufCodecs.registry(Registries.ITEM).map(EmptyComputerIngredient::new,
                    EmptyComputerIngredient::computer);

    @Override
    public boolean test(final ItemStack stack) {
        return stack.is(computer) && !stack.has(ComputingComponents.ASSEMBLED_HARDWARE.get());
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(new ItemStack(computer));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ComputingIngredients.EMPTY_COMPUTER.get();
    }
}
