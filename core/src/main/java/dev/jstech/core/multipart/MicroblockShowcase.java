/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.cable.CableItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * What a recipe viewer shows of the microblocks' rules: each rule is one entry, not one for every block, and its
 * frames go through every block microblocks are cut from, every piece and every way the rule works. A frame is a
 * crafting grid three by three and what it makes, worked out by the rules themselves ({@link Cuts}), so what a viewer
 * shows is what the grid makes. Worked out once, the first time a viewer asks.
 */
public final class MicroblockShowcase {

    private static final Map<Rule, List<Frame>> FRAMES = Collections.synchronizedMap(new EnumMap<>(Rule.class));
    private static final int GRID = 9;
    private static final int CORNER = 0;
    private static final int ABOVE = 1;
    private static final int LEFT = 3;
    private static final int MIDDLE = 4;

    private MicroblockShowcase() {
    }

    /** Every frame of {@code rule}'s entry, block after block. */
    public static List<Frame> frames(final Rule rule) {
        return FRAMES.computeIfAbsent(rule, MicroblockShowcase::make);
    }

    /** Every block microblocks are cut from, in the order the game registered them. */
    public static List<Block> materials() {
        final List<Block> out = new ArrayList<>();
        for (final Block block : BuiltInRegistries.BLOCK) {
            if (MicroblockMaterials.cuttable(block)) {
                out.add(block);
            }
        }
        return out;
    }

    /** Forgets the frames, as the game's content changes. */
    public static void forget() {
        FRAMES.clear();
    }

    private static List<Frame> make(final Rule rule) {
        final List<Frame> out = new ArrayList<>();
        final List<ItemStack> saws = saws();
        final List<ItemStack> cables = cables();
        int n = 0;
        for (final Block material : materials()) {
            switch (rule) {
                case CUT -> {
                    final ItemStack saw = saws.get(n++ % saws.size());
                    cut(out, saw, material, Cuts.Piece.WHOLE, ABOVE, Cuts.Way.HALVES);
                    cut(out, saw, material, face(MicroShape.SLAB), ABOVE, Cuts.Way.HALVES);
                    cut(out, saw, material, face(MicroShape.PANEL), ABOVE, Cuts.Way.HALVES);
                    cut(out, saw, material, face(MicroShape.SLAB), LEFT, Cuts.Way.ACROSS);
                    cut(out, saw, material, new Cuts.Piece(MicroShape.POST, MicroShape.SLAB), LEFT, Cuts.Way.ACROSS);
                    cut(out, saw, material, face(MicroShape.COVER), CORNER, Cuts.Way.HOLLOWED);
                }
                case JOIN -> {
                    join(out, material, face(MicroShape.SLAB), face(MicroShape.SLAB));
                    join(out, material, face(MicroShape.PANEL), face(MicroShape.PANEL));
                    join(out, material, face(MicroShape.COVER), face(MicroShape.COVER));
                    join(out, material, face(MicroShape.COVER), face(MicroShape.PANEL));
                }
                case FACADE -> {
                    if (!cables.isEmpty()) {
                        final ItemStack cable = cables.get(n++ % cables.size());
                        final Cuts.Piece cover = face(MicroShape.COVER);
                        Cuts.facade(cover).ifPresent(facade -> out.add(frame(LEFT, stackOf(material, cover), MIDDLE,
                                cable, stackOf(material, facade))));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static void cut(final List<Frame> out, final ItemStack saw, final Block material, final Cuts.Piece piece,
                            final int sawAt, final Cuts.Way way) {
        Cuts.cut(piece, way).ifPresent(made -> out.add(frame(sawAt, saw, MIDDLE, stackOf(material, piece),
                stackOf(material, made.piece()).copyWithCount(made.count()))));
    }

    private static void join(final List<Frame> out, final Block material, final Cuts.Piece a, final Cuts.Piece b) {
        Cuts.join(a, b).ifPresent(joined -> out.add(frame(LEFT, stackOf(material, a), MIDDLE, stackOf(material, b),
                stackOf(material, joined))));
    }

    /* A grid holding two things, at two of its places, and what it makes. */
    private static Frame frame(final int first, final ItemStack one, final int second, final ItemStack other,
                               final ItemStack output) {
        final List<ItemStack> grid = new ArrayList<>(Collections.nCopies(GRID, ItemStack.EMPTY));
        grid.set(first, one);
        grid.set(second, other);
        return new Frame(grid, output);
    }

    /* The item of a piece of {@code material}, or of the block itself for the whole. */
    private static ItemStack stackOf(final Block material, final Cuts.Piece piece) {
        return piece.whole() ? new ItemStack(material)
                : CoreSpaces.stack(new Microblock.MicroblockPiece(piece.shape(), piece.size(), material));
    }

    private static Cuts.Piece face(final int size) {
        return new Cuts.Piece(MicroShape.FACE, size);
    }

    /* Every saw of any mod, in the saws' tag; a stand-in for none, so a frame is never left without one. */
    private static List<ItemStack> saws() {
        final List<ItemStack> out = new ArrayList<>();
        for (final Holder<Item> saw : BuiltInRegistries.ITEM.getTagOrEmpty(CoreSpaces.SAWS)) {
            out.add(new ItemStack(saw));
        }
        return out.isEmpty() ? List.of(ItemStack.EMPTY) : out;
    }

    /* Every cable of any mod, a piece of which makes a facade with a cover. */
    private static List<ItemStack> cables() {
        final List<ItemStack> out = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (item instanceof CableItem) {
                out.add(new ItemStack(item));
            }
        }
        return out;
    }

    /** The rules a viewer shows, one entry each. */
    public enum Rule {
        /** The saw's cuts: halves, across, hollowed. */
        CUT("cut"),
        /** Two pieces of one shape and block joined. */
        JOIN("join"),
        /** A cover joined with a cable into a facade. */
        FACADE("facade");

        private final String id;

        Rule(final String id) {
            this.id = id;
        }

        /** The rule's name in ids. */
        public String id() {
            return this.id;
        }
    }

    /**
     * One frame of a rule's entry: the crafting grid, three by three, empty places empty, and what it makes.
     *
     * @param grid   the grid, row by row
     * @param output what it makes
     */
    public record Frame(List<ItemStack> grid, ItemStack output) {

        public Frame {
            grid = List.copyOf(grid);
            Objects.requireNonNull(output, "output");
        }
    }
}
