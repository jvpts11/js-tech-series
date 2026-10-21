/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipeview;

import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * The kinds of recipe every mod declared for the recipe viewers, any kind and not only the machines': the Core shows
 * each in JEI, EMI and REI, whichever are installed, with nothing written for any one of them. The processing machines
 * a mod declares need no kind here: their pages are made from their declarations.
 */
public final class RecipeViews {

    private static final List<RecipeViewKind<?>> KINDS = new CopyOnWriteArrayList<>();

    private RecipeViews() {
    }

    /**
     * Starts a kind of the game's recipes of {@code type}, called {@code name} in the mod {@code modid}, each recipe
     * in its holder, under {@code title}.
     */
    @SuppressWarnings("unchecked")
    public static <R extends Recipe<?>> RecipeViewKind.Builder<RecipeHolder<R>> recipes(
            final String modid, final String name, final TextKey title, final Supplier<RecipeType<R>> type) {
        final RecipeViewKind.Builder<RecipeHolder<R>> builder = new RecipeViewKind.Builder<>(
                ResourceLocation.fromNamespaceAndPath(modid, name), title,
                (Class<? super RecipeHolder<R>>) (Class<?>) RecipeHolder.class, true);
        builder.ids(RecipeHolder::id);
        return builder.recipes(level -> recipesOf(level, type.get()));
    }

    /** Starts a kind of anything a mod lists as a recipe, each of class {@code type}, under {@code title}. */
    public static <T> RecipeViewKind.Builder<T> of(final String modid, final String name, final TextKey title,
                                                   final Class<T> type) {
        return new RecipeViewKind.Builder<>(ResourceLocation.fromNamespaceAndPath(modid, name), title, type, false);
    }

    /** Every kind declared, in the order they were. */
    public static List<RecipeViewKind<?>> declared() {
        return List.copyOf(KINDS);
    }

    /* The game's recipes of {@code type} in {@code level}, each in its holder. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R extends Recipe<?>> List<RecipeHolder<R>> recipesOf(final Level level,
                                                                         final RecipeType<R> type) {
        return (List<RecipeHolder<R>>) (List) level.getRecipeManager().getAllRecipesFor((RecipeType) type);
    }

    static void add(final RecipeViewKind<?> kind) {
        for (final RecipeViewKind<?> other : KINDS) {
            if (other.id().equals(kind.id())) {
                throw DeclarationError.of(kind.id().getNamespace(), "the kind of recipe view " + kind.id(),
                        "is declared twice", "give each kind its own name");
            }
        }
        KINDS.add(kind);
    }
}
