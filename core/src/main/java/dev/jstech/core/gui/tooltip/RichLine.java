/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * A line of a tooltip with items and pictures in its text, as a recipe's or a machine's tooltip says "needs 4 [iron
 * ingot] and [bucket]": built piece by piece, and drawn on the player's game as one line.
 *
 * <pre>{@code
 * RichLine line = RichLine.builder().text(Component.literal("Needs 4 ")).item(new ItemStack(Items.IRON_INGOT))
 *         .text(Component.literal(" and ")).item(new ItemStack(Items.BUCKET)).build();
 * }</pre>
 *
 * <p>Hand it to a tooltip as the game hands any picture to one, from {@code Item#getTooltipImage} or by adding it in
 * {@code RenderTooltipEvent.GatherComponents}.
 *
 * @param pieces its pieces, in order
 */
public record RichLine(List<Piece> pieces) implements TooltipComponent {

    public RichLine {
        pieces = List.copyOf(pieces);
    }

    /** A line built piece by piece. */
    public static Builder builder() {
        return new Builder();
    }

    /** A piece of a line. */
    public sealed interface Piece {

        /**
         * Text.
         *
         * @param text the text
         */
        record Text(Component text) implements Piece {
        }

        /**
         * An item's picture, sixteen pixels square.
         *
         * @param stack the item
         */
        record Item(ItemStack stack) implements Piece {

            public Item {
                stack = stack.copy();
            }

            /** The item, a copy. */
            @Override
            public ItemStack stack() {
                return stack.copy();
            }
        }

        /**
         * A sprite of the GUI atlas.
         *
         * @param sprite the sprite
         * @param width  how wide it is drawn
         * @param height how tall it is drawn
         */
        record Image(ResourceLocation sprite, int width, int height) implements Piece {
        }
    }

    /** Builds a line piece by piece. */
    public static final class Builder {

        private final List<Piece> pieces = new ArrayList<>();

        private Builder() {
        }

        /** Adds text. */
        public Builder text(final Component text) {
            pieces.add(new Piece.Text(text));
            return this;
        }

        /** Adds an item's picture. */
        public Builder item(final ItemStack stack) {
            pieces.add(new Piece.Item(stack));
            return this;
        }

        /** Adds a sprite of the GUI atlas, {@code width} by {@code height}. */
        public Builder image(final ResourceLocation sprite, final int width, final int height) {
            pieces.add(new Piece.Image(sprite, width, height));
            return this;
        }

        /** The line. */
        public RichLine build() {
            return new RichLine(pieces);
        }
    }
}
