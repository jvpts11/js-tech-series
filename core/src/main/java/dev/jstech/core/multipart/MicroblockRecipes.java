/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.cable.CableItem;
import dev.jstech.core.registry.CoreItems;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The crafting grid's microblock recipes, rules in code since they hold for every block of every mod ({@link Cuts}):
 * the saw's cuts, two pieces joined, and a cover joined with a cable into a facade. The saw is worn by each cut and
 * stays in the grid.
 */
public final class MicroblockRecipes {

    /** The saw's cuts. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SawCut>> SAW_CUT =
            CoreItems.CONTENT.recipeSerializer("saw_cut", () -> new SimpleCraftingRecipeSerializer<>(SawCut::new));
    /** Two pieces joined. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Join>> JOIN =
            CoreItems.CONTENT.recipeSerializer("microblock_join", () -> new SimpleCraftingRecipeSerializer<>(Join::new));
    /** A cover and a cable joined into a facade. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Facade>> FACADE =
            CoreItems.CONTENT.recipeSerializer("facade", () -> new SimpleCraftingRecipeSerializer<>(Facade::new));

    private MicroblockRecipes() {
    }

    /** Declares the recipes' serializers, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The microblock {@code stack} is, or none for anything else. */
    public static @Nullable Microblock.MicroblockPiece pieceOf(final ItemStack stack) {
        return stack.getItem() instanceof MicroblockItem ? stack.get(CoreSpaces.PIECE.get()) : null;
    }

    /* The block {@code stack} places, when microblocks are cut from it; none otherwise. */
    private static @Nullable Block wholeOf(final ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && MicroblockMaterials.cuttable(item.getBlock())
                ? item.getBlock() : null;
    }

    /* The only two things in the grid, in their order, or none when there are more or fewer. */
    private static int @Nullable [] two(final CraftingInput input) {
        int first = -1;
        int second = -1;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                if (first < 0) {
                    first = i;
                } else if (second < 0) {
                    second = i;
                } else {
                    return null;
                }
            }
        }
        return second < 0 ? null : new int[] {first, second};
    }

    /** The saw beside a block or a microblock: above it, two halves; beside it, cut across; at a corner, hollowed. */
    public static final class SawCut extends CustomRecipe {

        public SawCut(final CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(final CraftingInput input, final Level level) {
            return made(input).isPresent();
        }

        @Override
        public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
            return made(input).orElse(ItemStack.EMPTY);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
            final NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
            for (int i = 0; i < input.size(); i++) {
                final ItemStack stack = input.getItem(i);
                if (stack.is(CoreSpaces.SAWS)) {
                    left.set(i, worn(stack));
                }
            }
            return left;
        }

        @Override
        public boolean canCraftInDimensions(final int width, final int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return SAW_CUT.get();
        }

        /* What the grid's saw makes of its piece, or nothing. */
        private static Optional<ItemStack> made(final CraftingInput input) {
            final int[] found = two(input);
            if (found == null) {
                return Optional.empty();
            }
            final boolean firstSaw = input.getItem(found[0]).is(CoreSpaces.SAWS);
            final int saw = firstSaw ? found[0] : found[1];
            final int other = firstSaw ? found[1] : found[0];
            if (!input.getItem(saw).is(CoreSpaces.SAWS)) {
                return Optional.empty();
            }
            final ItemStack cut = input.getItem(other);
            final Block whole = wholeOf(cut);
            final Microblock.MicroblockPiece piece = pieceOf(cut);
            if (whole == null && piece == null) {
                return Optional.empty();
            }
            final Block material = whole != null ? whole : piece.material();
            final Cuts.Piece what = whole != null ? Cuts.Piece.WHOLE : piece.cut();
            final Cuts.Way way = wayOf(input.width(), saw, other);
            if (way == null) {
                return Optional.empty();
            }
            return Cuts.cut(what, way).map(made -> CoreSpaces.stack(new Microblock.MicroblockPiece(
                    made.piece().shape(), made.piece().size(), material)).copyWithCount(made.count()));
        }

        /* Where the saw lies from the piece in a grid {@code width} wide. */
        private static Cuts.@Nullable Way wayOf(final int width, final int saw, final int piece) {
            final int dx = saw % width - piece % width;
            final int dy = saw / width - piece / width;
            if (dx == 0 && dy == -1) {
                return Cuts.Way.HALVES;
            }
            if (dy == 0 && Math.abs(dx) == 1) {
                return Cuts.Way.ACROSS;
            }
            return Math.abs(dx) == 1 && Math.abs(dy) == 1 ? Cuts.Way.HOLLOWED : null;
        }

        /* The saw a cut more worn; none once it is worn out. */
        private static ItemStack worn(final ItemStack saw) {
            final ItemStack kept = saw.copyWithCount(1);
            if (!kept.isDamageableItem()) {
                return kept;
            }
            kept.setDamageValue(kept.getDamageValue() + 1);
            return kept.getDamageValue() >= kept.getMaxDamage() ? ItemStack.EMPTY : kept;
        }
    }

    /** Two pieces of one shape and block joined into one as thick as both, or the block itself. */
    public static final class Join extends CustomRecipe {

        public Join(final CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(final CraftingInput input, final Level level) {
            return made(input).isPresent();
        }

        @Override
        public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
            return made(input).orElse(ItemStack.EMPTY);
        }

        @Override
        public boolean canCraftInDimensions(final int width, final int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return JOIN.get();
        }

        private static Optional<ItemStack> made(final CraftingInput input) {
            final int[] found = two(input);
            if (found == null) {
                return Optional.empty();
            }
            final Microblock.MicroblockPiece a = pieceOf(input.getItem(found[0]));
            final Microblock.MicroblockPiece b = pieceOf(input.getItem(found[1]));
            if (a == null || b == null || a.material() != b.material()) {
                return Optional.empty();
            }
            return Cuts.join(a.cut(), b.cut()).map(joined -> joined.whole() ? new ItemStack(a.material())
                    : CoreSpaces.stack(new Microblock.MicroblockPiece(joined.shape(), joined.size(),
                    a.material())));
        }
    }

    /** A cover joined with a piece of any cable: a facade of the cover's block. */
    public static final class Facade extends CustomRecipe {

        public Facade(final CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(final CraftingInput input, final Level level) {
            return made(input).isPresent();
        }

        @Override
        public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
            return made(input).orElse(ItemStack.EMPTY);
        }

        @Override
        public boolean canCraftInDimensions(final int width, final int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return FACADE.get();
        }

        private static Optional<ItemStack> made(final CraftingInput input) {
            final int[] found = two(input);
            if (found == null) {
                return Optional.empty();
            }
            final ItemStack first = input.getItem(found[0]);
            final ItemStack cover = first.getItem() instanceof CableItem ? input.getItem(found[1]) : first;
            final ItemStack cable = first.getItem() instanceof CableItem ? first : input.getItem(found[1]);
            final Microblock.MicroblockPiece piece = pieceOf(cover);
            if (piece == null || !(cable.getItem() instanceof CableItem)) {
                return Optional.empty();
            }
            return Cuts.facade(piece.cut()).map(facade -> CoreSpaces.stack(new Microblock.MicroblockPiece(
                    facade.shape(), facade.size(), piece.material())));
        }
    }
}
