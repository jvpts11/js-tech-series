/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.registry.CoreItems;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A shapeless crafting recipe that asks for a piece of knowledge, as {@link KnowledgeShapedRecipe} is a shaped one:
 *
 * <pre>{@code
 * {"type": "jscore:crafting_shapeless_knowledge", "knowledge": "mymod:fusion",
 *  "ingredients": [...], "result": {...}}
 * }</pre>
 */
public final class KnowledgeShapelessRecipe extends ShapelessRecipe implements IKnowledgeGated {

    private final ResourceLocation knowledge;

    public static final MapCodec<KnowledgeShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("knowledge").forGetter(KnowledgeShapelessRecipe::needs),
                    RecipeSerializer.SHAPELESS_RECIPE.codec().forGetter(recipe -> recipe))
                    .apply(instance, KnowledgeShapelessRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeShapelessRecipe> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, KnowledgeShapelessRecipe::needs,
                    RecipeSerializer.SHAPELESS_RECIPE.streamCodec(), recipe -> recipe,
                    KnowledgeShapelessRecipe::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KnowledgeShapelessRecipe>> SERIALIZER =
            CoreItems.CONTENT.recipeSerializer("crafting_shapeless_knowledge", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<KnowledgeShapelessRecipe> codec() {
                    return CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, KnowledgeShapelessRecipe> streamCodec() {
                    return STREAM_CODEC;
                }
            });

    public KnowledgeShapelessRecipe(final ResourceLocation knowledge, final ShapelessRecipe shapeless) {
        super(shapeless.getGroup(), shapeless.category(), shapeless.getResultItem(RegistryAccess.EMPTY),
                copyOf(shapeless.getIngredients()));
        this.knowledge = knowledge;
    }

    /** Declares the recipe's serializer, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The knowledge it asks for. */
    public ResourceLocation needs() {
        return this.knowledge;
    }

    @Override
    public Optional<ResourceLocation> knowledge() {
        return Optional.of(this.knowledge);
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return KnowledgeGate.craftable(level, knowledge()) && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    private static NonNullList<Ingredient> copyOf(final NonNullList<Ingredient> ingredients) {
        final NonNullList<Ingredient> out = NonNullList.create();
        out.addAll(ingredients);
        return out;
    }
}
