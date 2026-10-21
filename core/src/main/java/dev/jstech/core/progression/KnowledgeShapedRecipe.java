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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A shaped crafting recipe that asks for a piece of knowledge: it crafts only for a player whose team knows it, at the
 * crafting table or the inventory's grid, and is shown all the same, with a padlock, in the recipe viewers and the
 * manuals. It is written as a shaped recipe is, with the knowledge:
 *
 * <pre>{@code
 * {"type": "jscore:crafting_shaped_knowledge", "knowledge": "mymod:fusion",
 *  "pattern": [...], "key": {...}, "result": {...}}
 * }</pre>
 */
public final class KnowledgeShapedRecipe extends ShapedRecipe implements IKnowledgeGated {

    private final ResourceLocation knowledge;

    public static final MapCodec<KnowledgeShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("knowledge").forGetter(KnowledgeShapedRecipe::needs),
            ShapedRecipe.Serializer.CODEC.forGetter(recipe -> recipe)).apply(instance, KnowledgeShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeShapedRecipe> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, KnowledgeShapedRecipe::needs,
                    ShapedRecipe.Serializer.STREAM_CODEC, recipe -> recipe, KnowledgeShapedRecipe::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KnowledgeShapedRecipe>> SERIALIZER =
            CoreItems.CONTENT.recipeSerializer("crafting_shaped_knowledge", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<KnowledgeShapedRecipe> codec() {
                    return CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, KnowledgeShapedRecipe> streamCodec() {
                    return STREAM_CODEC;
                }
            });

    public KnowledgeShapedRecipe(final ResourceLocation knowledge, final ShapedRecipe shaped) {
        super(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(RegistryAccess.EMPTY),
                shaped.showNotification());
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
}
