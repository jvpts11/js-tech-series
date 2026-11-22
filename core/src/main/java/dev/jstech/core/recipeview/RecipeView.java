/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipeview;

import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * One recipe's page in a recipe viewer, as a kind's layout lays it ({@link RecipeViewKind}): its slots, each an input,
 * an output or a catalyst at a place, holding items or fluids; its lines of text; its sprites; and its arrow. JEI, EMI
 * and REI each draw it the way they draw their own pages, so a mod lays its page once for all three. Places are in
 * pixels from the page's top left, a slot's being the top left of its frame of 18 pixels. A page may be laid on a
 * machine's own face, drawn behind its slots, which then stand in the face's own ports and sight glasses.
 *
 * @param slots   its slots, in the order they were laid
 * @param lines   its lines of text
 * @param sprites the sprites drawn on it
 * @param arrow   its arrow, if it has one
 * @param face    the machine's face it is laid on, if it is
 */
public record RecipeView(List<Slot> slots, List<Line> lines, List<Sprite> sprites, Optional<Arrow> arrow,
                         Optional<Face> face) {

    /** A slot's frame, in pixels. */
    public static final int SLOT = 18;
    /** What a slot shows within its frame, in pixels: an item's room. */
    public static final int SHOWN = 16;

    public RecipeView {
        slots = List.copyOf(slots);
        lines = List.copyOf(lines);
        sprites = List.copyOf(sprites);
        Objects.requireNonNull(arrow, "arrow");
        Objects.requireNonNull(face, "face");
    }

    /** The slots of one role, in the order they were laid. */
    public List<Slot> slots(final Role role) {
        return this.slots.stream().filter(slot -> slot.role() == role).toList();
    }

    /** What a slot is to its recipe. */
    public enum Role {
        /** What the recipe takes. */
        INPUT,
        /** What the recipe makes. */
        OUTPUT,
        /** What the recipe needs but does not use up. */
        CATALYST
    }

    /**
     * A slot.
     *
     * @param role     what it is to the recipe
     * @param x        its frame's left
     * @param y        its frame's top
     * @param items    the items it shows, one at a time; empty for a fluid's slot
     * @param fluids   the fluids it shows, one at a time; empty for an item's slot
     * @param capacity how much of a fluid fills its slot, a fluid's slot being drawn as full as it holds of that
     * @param chance   how likely an output comes, 1 for always
     * @param tooltip  the lines its tooltip adds to what it holds
     * @param width    how wide what it shows is, a pixel in from its frame's left: an item's 16, a sight glass's own
     * @param height   how tall what it shows is
     */
    public record Slot(Role role, int x, int y, List<ItemStack> items, List<FluidStack> fluids, int capacity,
                       float chance, List<Component> tooltip, int width, int height) {

        public Slot {
            Objects.requireNonNull(role, "role");
            items = items.stream().map(ItemStack::copy).toList();
            fluids = fluids.stream().map(FluidStack::copy).toList();
            tooltip = List.copyOf(tooltip);
        }

        /** The same slot, showing {@code width} by {@code height} a pixel in from its frame's top left. */
        public Slot sized(final int width, final int height) {
            return new Slot(this.role, this.x, this.y, this.items, this.fluids, this.capacity, this.chance,
                    this.tooltip, width, height);
        }

        /** Whether it shows fluids rather than items. */
        public boolean holdsFluid() {
            return !this.fluids.isEmpty();
        }
    }

    /**
     * A line of text.
     *
     * @param x      its left
     * @param y      its top
     * @param width  how wide it may run
     * @param text   what it says
     * @param colour its colour, {@code 0xAARRGGBB}
     */
    public record Line(int x, int y, int width, Component text, int colour) {
    }

    /**
     * A sprite of the game's GUI atlas, drawn on the page.
     *
     * @param sprite its id, as {@code jscore:recipe/lock}
     * @param x      its left
     * @param y      its top
     * @param width  its width
     * @param height its height
     */
    public record Sprite(ResourceLocation sprite, int x, int y, int width, int height) {
    }

    /**
     * The arrow from what a recipe takes to what it makes, filling in the recipe's time.
     *
     * @param x     its left
     * @param y     its top
     * @param ticks how long it takes to fill; 0 for an arrow that does not fill
     */
    public record Arrow(int x, int y, int ticks) {
    }

    /**
     * The machine's face a page is laid on, drawn behind its slots from its top left, as the machine draws it at work.
     * Matter is no viewer's ingredient, so the face's own sight glasses show it: filled with each matter's colour and
     * named under the glass, its amount said on the glass's tooltip.
     *
     * @param id     the face, as the player's game has it drawn for this page's kind
     * @param lcd    what the face's LCD says of the recipe: that it runs, and for how long
     * @param said   what each word written on the face says on this page, by the word as written: its translation,
     *               or what the recipe makes of it, as a byproduct's chance
     * @param matter the matter each of the machine's tanks takes or gives, by the tank's place among its tanks
     */
    public record Face(ResourceLocation id, List<Component> lcd, Map<String, String> said,
                       Map<Integer, MatterStack> matter) {

        public Face {
            Objects.requireNonNull(id, "id");
            lcd = List.copyOf(lcd);
            said = Map.copyOf(said);
            matter = Map.copyOf(matter);
        }
    }

    /** Lays a page out; a kind's layout is handed one for each recipe. */
    public static final class Builder {

        private final List<Slot> slots = new ArrayList<>();
        private final List<Line> lines = new ArrayList<>();
        private final List<Sprite> sprites = new ArrayList<>();
        private @Nullable Arrow arrow;
        private @Nullable Face face;

        /** A slot of what the recipe takes, holding any of {@code items}, its frame's top left at {@code x, y}. */
        public Builder input(final int x, final int y, final List<ItemStack> items) {
            return slot(new Slot(Role.INPUT, x, y, items, List.of(), 0, 1.0f, List.of(), SHOWN, SHOWN));
        }

        /** A slot of what the recipe takes, holding what {@code ingredient} takes. */
        public Builder input(final int x, final int y, final Ingredient ingredient) {
            return input(x, y, List.of(ingredient.getItems()));
        }

        /** A slot of a fluid the recipe takes, any of {@code fluids}, drawn full at {@code capacity}. */
        public Builder fluidInput(final int x, final int y, final List<FluidStack> fluids, final int capacity) {
            return slot(new Slot(Role.INPUT, x, y, List.of(), fluids, capacity, 1.0f, List.of(), SHOWN, SHOWN));
        }

        /** A slot of what the recipe needs but does not use up. */
        public Builder catalyst(final int x, final int y, final List<ItemStack> items) {
            return slot(new Slot(Role.CATALYST, x, y, items, List.of(), 0, 1.0f, List.of(), SHOWN, SHOWN));
        }

        /** A slot of what the recipe makes. */
        public Builder output(final int x, final int y, final ItemStack item) {
            return output(x, y, List.of(item));
        }

        /** A slot of what the recipe makes, any of {@code items}. */
        public Builder output(final int x, final int y, final List<ItemStack> items) {
            return slot(new Slot(Role.OUTPUT, x, y, items, List.of(), 0, 1.0f, List.of(), SHOWN, SHOWN));
        }

        /** A slot of a fluid the recipe makes, drawn full at {@code capacity}. */
        public Builder fluidOutput(final int x, final int y, final FluidStack fluid, final int capacity) {
            return slot(new Slot(Role.OUTPUT, x, y, List.of(), List.of(fluid), capacity, 1.0f, List.of(), SHOWN,
                    SHOWN));
        }

        /** Makes the last slot's output come by {@code chance}, from 0 to 1. */
        public Builder chance(final float chance) {
            final Slot last = last();
            this.slots.set(this.slots.size() - 1, new Slot(last.role(), last.x(), last.y(), last.items(),
                    last.fluids(), last.capacity(), Math.clamp(chance, 0.0f, 1.0f), last.tooltip(), last.width(),
                    last.height()));
            return this;
        }

        /** Adds {@code line} to the last slot's tooltip. */
        public Builder tooltip(final Component line) {
            final Slot last = last();
            final List<Component> lines = new ArrayList<>(last.tooltip());
            lines.add(line);
            this.slots.set(this.slots.size() - 1, new Slot(last.role(), last.x(), last.y(), last.items(),
                    last.fluids(), last.capacity(), last.chance(), lines, last.width(), last.height()));
            return this;
        }

        /** Makes the last slot show {@code width} by {@code height}, as a sight glass of a machine's face is. */
        public Builder sized(final int width, final int height) {
            this.slots.set(this.slots.size() - 1, last().sized(width, height));
            return this;
        }

        /** Lays the page on a machine's face, as {@code face} says of it. */
        public Builder face(final Face face) {
            this.face = Objects.requireNonNull(face, "face");
            return this;
        }

        /** A line of text, its top left at {@code x, y}, running at most {@code width}, in {@code colour}. */
        public Builder line(final int x, final int y, final int width, final Component text, final int colour) {
            this.lines.add(new Line(x, y, width, text, colour));
            return this;
        }

        /** A sprite of the game's GUI atlas. */
        public Builder sprite(final ResourceLocation sprite, final int x, final int y, final int width,
                              final int height) {
            this.sprites.add(new Sprite(sprite, x, y, width, height));
            return this;
        }

        /** The arrow, filling in {@code ticks}. */
        public Builder arrow(final int x, final int y, final int ticks) {
            this.arrow = new Arrow(x, y, Math.max(0, ticks));
            return this;
        }

        /** The page as laid. */
        public RecipeView build() {
            return new RecipeView(this.slots, this.lines, this.sprites, Optional.ofNullable(this.arrow),
                    Optional.ofNullable(this.face));
        }

        private Builder slot(final Slot slot) {
            this.slots.add(slot);
            return this;
        }

        private Slot last() {
            if (this.slots.isEmpty()) {
                throw new IllegalStateException("a slot's chance or tooltip comes after the slot is laid");
            }
            return this.slots.getLast();
        }
    }
}
