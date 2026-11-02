/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.ShapePreview;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a Structure Projector sees of its multiblock in the world: each place where it stands, and whether the right
 * block is there, nothing is (missing), or another block is (wrong); how many of each; and what is still missing, by
 * block. Each place that must be left empty, as a dry dock's pit, is right when nothing stands in it and wrong
 * otherwise, the block there to be cleared. Worked out on whichever side reads it, from the world that side holds.
 *
 * @param preview  the multiblock laid out as the projector shows it
 * @param terminal where its terminal stands
 * @param places   each place where a block goes, the bottom layer first
 * @param clears   each place that must be left empty, the bottom layer first
 */
public record ProjectorView(ShapePreview preview, BlockPos terminal, List<Place> places, List<Place> clears) {

    public ProjectorView {
        places = List.copyOf(places);
        clears = List.copyOf(clears);
    }

    /** What {@code projector} sees in {@code level}, when it has a multiblock to show. */
    public static Optional<ProjectorView> of(final BlockGetter level, final StructureProjectorBlockEntity projector) {
        return projector.preview().map(preview -> of(level, preview, projector.terminalOf(preview)));
    }

    /**
     * What is seen in {@code level} of {@code preview} with its terminal at {@code terminal}; a block merged into a
     * formed multiblock's model is seen as the block it was.
     */
    public static ProjectorView of(final BlockGetter level, final ShapePreview preview, final BlockPos terminal) {
        final List<Place> places = new ArrayList<>();
        for (final ShapePreview.Ghost ghost : preview.ghosts()) {
            final BlockPos at = terminal.offset(ghost.x(), ghost.y(), ghost.z());
            places.add(new Place(at, ghost, stateOf(FormedPartBlock.originalAt(level, at, level.getBlockState(at)),
                    ghost)));
        }
        final List<Place> clears = new ArrayList<>();
        for (final ShapePreview.Ghost ghost : preview.clears()) {
            final BlockPos at = terminal.offset(ghost.x(), ghost.y(), ghost.z());
            // The game's invisible light block, which a building lit through keeps in its empty places, is empty too.
            final BlockState there = level.getBlockState(at);
            clears.add(new Place(at, ghost, there.isAir() || there.is(Blocks.LIGHT) ? State.PLACED : State.WRONG));
        }
        return new ProjectorView(preview, terminal, places, clears);
    }

    /** How many places that must be left empty hold a block, which is to be cleared. */
    public int toClear() {
        return (int) this.clears.stream().filter(place -> place.state() == State.WRONG).count();
    }

    /** The places that must be left empty shown when the projector shows up to {@code layer}, as {@link #upTo}. */
    public List<Place> clearsUpTo(final int layer) {
        return layer <= 0 ? this.clears : this.clears.stream().filter(place -> place.ghost().layer() < layer)
                .toList();
    }

    /** How many places hold the right block. */
    public int placed() {
        return count(State.PLACED);
    }

    /** How many places are still empty. */
    public int missing() {
        return count(State.MISSING);
    }

    /** How many places hold another block. */
    public int wrong() {
        return count(State.WRONG);
    }

    /** What is still missing, by what is shown there (a block id, or a tag as {@code #namespace:path}), most first. */
    public Map<String, Integer> missingByBlock() {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final Place place : this.places) {
            if (place.state() == State.MISSING && !place.ghost().terminal() && place.ghost().first() != null) {
                counts.merge(place.ghost().first(), 1, Integer::sum);
            }
        }
        final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        final Map<String, Integer> out = new LinkedHashMap<>();
        sorted.forEach(entry -> out.put(entry.getKey(), entry.getValue()));
        return out;
    }

    /** The places shown when the projector shows up to {@code layer} (from 1; 0 for every layer). */
    public List<Place> upTo(final int layer) {
        return layer <= 0 ? this.places : this.places.stream().filter(place -> place.ghost().layer() < layer)
                .toList();
    }

    private int count(final State state) {
        return (int) this.places.stream().filter(place -> place.state() == state).count();
    }

    /*
     * Whether the block at a place is right: one of its blocks, or of its tags; for the terminal's place whose block
     * is not known, any block. An empty place, or one a block could be placed in, is missing; anything else is wrong.
     */
    private static State stateOf(final BlockState state, final ShapePreview.Ghost ghost) {
        if (ghost.terminal() && ghost.blocks().isEmpty()) {
            return state.isAir() ? State.MISSING : State.PLACED;
        }
        final String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (ghost.blocks().contains(id)) {
            return State.PLACED;
        }
        for (final String tag : ghost.tags()) {
            final ResourceLocation tagId = ResourceLocation.tryParse(tag);
            if (tagId != null && state.is(TagKey.create(Registries.BLOCK, tagId))) {
                return State.PLACED;
            }
        }
        return state.isAir() || state.canBeReplaced() ? State.MISSING : State.WRONG;
    }

    /**
     * A place of the multiblock in the world.
     *
     * @param pos   where it is
     * @param ghost what goes there
     * @param state whether it is right, missing or wrong
     */
    public record Place(BlockPos pos, ShapePreview.Ghost ghost, State state) {
    }

    /** What a place holds. */
    public enum State {
        /** The right block. */
        PLACED,
        /** Nothing yet. */
        MISSING,
        /** Another block. */
        WRONG
    }
}
