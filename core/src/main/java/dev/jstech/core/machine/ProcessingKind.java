/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * A kind of processing machine, as its recipes know it: grinding, pressing, mixing. Each is a recipe type of the mod
 * that declares it, with {@link ModContent#processing}, and all of them read and write the same
 * {@link ProcessingRecipe}. It also knows its name and the machines that work it, which is all a recipe viewer needs
 * to show its recipes under that name with those machines beside them.
 */
public final class ProcessingKind {

    private final ResourceLocation id;
    private final TextKey title;
    private final Supplier<RecipeType<ProcessingRecipe>> type;
    private final Supplier<RecipeSerializer<ProcessingRecipe>> serializer;
    // Declared while the mod's classes load, read once the game runs: a list safe to read while it was written.
    private final List<Supplier<? extends ItemLike>> machines = new CopyOnWriteArrayList<>();

    public ProcessingKind(final ResourceLocation id, final TextKey title,
                          final Supplier<RecipeType<ProcessingRecipe>> type,
                          final Supplier<RecipeSerializer<ProcessingRecipe>> serializer) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.serializer = serializer;
    }

    /** The recipe type's id, which the files of its recipes name as their type. */
    public ResourceLocation id() {
        return id;
    }

    /** What players call the kind: the heading its recipes are shown under. */
    public TextKey title() {
        return title;
    }

    public RecipeType<ProcessingRecipe> type() {
        return type.get();
    }

    public RecipeSerializer<ProcessingRecipe> serializer() {
        return serializer.get();
    }

    /**
     * Names another machine that works recipes of this kind, beside the one it was declared with: a bigger grinder,
     * the same grinder of a later era. The first one named is the kind's icon.
     */
    public ProcessingKind alsoWorkedBy(final Supplier<? extends ItemLike> machine) {
        machines.add(machine);
        return this;
    }

    /** The machines that work recipes of this kind, the one it was declared with first. */
    public List<ItemLike> machines() {
        return machines.stream().<ItemLike>map(Supplier::get).toList();
    }

    /** The first recipe of this kind the input makes, or none. */
    public Optional<RecipeHolder<ProcessingRecipe>> find(final Level level, final ProcessingInput input) {
        return level.getRecipeManager().getRecipeFor(type(), input, level);
    }

    /** Every recipe of this kind the level knows. */
    public List<RecipeHolder<ProcessingRecipe>> all(final Level level) {
        return level.getRecipeManager().getAllRecipesFor(type());
    }

    /** The serializer the kind's recipes are read and sent with, made for the declaring mod to register. */
    public RecipeSerializer<ProcessingRecipe> newSerializer() {
        final MapCodec<ProcessingRecipe> codec = ProcessingRecipe.codec(this);
        final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> stream = ProcessingRecipe.streamCodec(this);
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<ProcessingRecipe> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
                return stream;
            }
        };
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
