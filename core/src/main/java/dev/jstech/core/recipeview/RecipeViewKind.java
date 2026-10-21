/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipeview;

import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of recipe as the recipe viewers show it, declared once for JEI, EMI and REI alike: its id and title, where
 * its recipes come from (the game's recipes of one type, or anything else a mod lists), the blocks and items that work
 * them, how large a page is, and how one recipe's page is laid out ({@link RecipeView}). Declared from a mod's
 * constructor with {@link RecipeViews}:
 *
 * <pre>{@code
 * public static final RecipeViewKind<RecipeHolder<SmelterRecipe>> SMELTING = RecipeViews
 *         .recipes(MODID, "smelting", SMELTING_TITLE, ModRecipes.SMELTING)
 *         .workstations(() -> List.of(new ItemStack(ModBlocks.SMELTER)))
 *         .size(82, 26)
 *         .layout((holder, page) -> page.input(0, 4, holder.value().input())
 *                 .arrow(26, 5, holder.value().ticks())
 *                 .output(60, 4, holder.value().result()))
 *         .declare();
 * }</pre>
 *
 * @param <T> what one recipe is: a game's recipe in its holder, or any value a mod lists
 */
public final class RecipeViewKind<T> {

    private final ResourceLocation id;
    private final TextKey title;
    private final Class<? super T> type;
    private final boolean holders;
    private final Function<Level, ? extends Collection<? extends T>> recipes;
    private final Supplier<? extends List<ItemStack>> workstations;
    private final @Nullable Supplier<ItemStack> icon;
    private final Function<List<T>, Size> size;
    private final IRecipeViewLayout<T> layout;
    private final @Nullable Function<T, ResourceLocation> idOf;

    private RecipeViewKind(final Builder<T> builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.type = builder.type;
        this.holders = builder.holders;
        this.recipes = builder.recipes;
        this.workstations = builder.workstations;
        this.icon = builder.icon;
        this.size = builder.size;
        this.layout = Objects.requireNonNull(builder.layout, "a kind of recipe view needs its layout");
        this.idOf = builder.idOf;
    }

    /** Its id, which each viewer's category takes, so a bookmark names the kind it came from. */
    public ResourceLocation id() {
        return this.id;
    }

    /** Its title above its pages. */
    public TextKey title() {
        return this.title;
    }

    /** The class of what one recipe is, which JEI asks for. */
    public Class<? super T> type() {
        return this.type;
    }

    /** Whether one recipe is a game's recipe in its holder. */
    public boolean holders() {
        return this.holders;
    }

    /** Its recipes in {@code level}, the player's own world. */
    public List<T> recipes(final Level level) {
        return new ArrayList<>(this.recipes.apply(level));
    }

    /** The blocks and items that work its recipes, each shown beside its pages. */
    public List<ItemStack> workstations() {
        return this.workstations.get().stream().filter(stack -> !stack.isEmpty()).toList();
    }

    /** What its category is shown by: the icon it was given, or its first workstation, or nothing. */
    public ItemStack icon() {
        if (this.icon != null) {
            return this.icon.get();
        }
        final List<ItemStack> stations = workstations();
        return stations.isEmpty() ? ItemStack.EMPTY : stations.getFirst();
    }

    /** How large its pages are, for {@code recipes}. */
    public Size size(final List<T> recipes) {
        return this.size.apply(recipes);
    }

    /** One recipe's page. */
    public RecipeView view(final T recipe) {
        final RecipeView.Builder page = new RecipeView.Builder();
        this.layout.lay(recipe, page);
        return page.build();
    }

    /** One recipe's own id, when it has one. */
    public @Nullable ResourceLocation idOf(final T recipe) {
        return this.idOf == null ? null : this.idOf.apply(recipe);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /**
     * How large a page is, in pixels.
     *
     * @param width  its width
     * @param height its height
     */
    public record Size(int width, int height) {
    }

    /**
     * Builds a kind; {@link #declare()} ends it.
     *
     * @param <T> what one recipe is
     */
    public static final class Builder<T> {

        private final ResourceLocation id;
        private final TextKey title;
        private final Class<? super T> type;
        private final boolean holders;
        private Function<Level, ? extends Collection<? extends T>> recipes = level -> List.of();
        private Supplier<? extends List<ItemStack>> workstations = List::of;
        private @Nullable Supplier<ItemStack> icon;
        private Function<List<T>, Size> size = recipes -> new Size(RecipeView.SLOT, RecipeView.SLOT);
        private @Nullable IRecipeViewLayout<T> layout;
        private @Nullable Function<T, ResourceLocation> idOf;

        Builder(final ResourceLocation id, final TextKey title, final Class<? super T> type, final boolean holders) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title");
            this.type = Objects.requireNonNull(type, "type");
            this.holders = holders;
        }

        /** Where its recipes come from, in the player's own world. */
        public Builder<T> recipes(final Function<Level, ? extends Collection<? extends T>> recipes) {
            this.recipes = Objects.requireNonNull(recipes, "recipes");
            return this;
        }

        /** The blocks and items that work its recipes. */
        public Builder<T> workstations(final Supplier<? extends List<ItemStack>> workstations) {
            this.workstations = Objects.requireNonNull(workstations, "workstations");
            return this;
        }

        /** What its category is shown by, rather than its first workstation. */
        public Builder<T> icon(final Supplier<ItemStack> icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        /** Every page this large. */
        public Builder<T> size(final int width, final int height) {
            final Size fixed = new Size(width, height);
            this.size = recipes -> fixed;
            return this;
        }

        /** Pages as large as {@code size} says for all its recipes: the largest one's room, say. */
        public Builder<T> sizedBy(final Function<List<T>, Size> size) {
            this.size = Objects.requireNonNull(size, "size");
            return this;
        }

        /** How one recipe's page is laid out. */
        public Builder<T> layout(final IRecipeViewLayout<T> layout) {
            this.layout = Objects.requireNonNull(layout, "layout");
            return this;
        }

        /** Each recipe's own id. */
        public Builder<T> ids(final Function<T, ResourceLocation> idOf) {
            this.idOf = Objects.requireNonNull(idOf, "idOf");
            return this;
        }

        /** The kind, not declared: for a mod's own bridge, or one the Core makes of what is declared otherwise. */
        public RecipeViewKind<T> build() {
            return new RecipeViewKind<>(this);
        }

        /** Declares the kind, from the mod's constructor, so every recipe viewer shows it. */
        public RecipeViewKind<T> declare() {
            final RecipeViewKind<T> kind = build();
            RecipeViews.add(kind);
            return kind;
        }
    }
}
