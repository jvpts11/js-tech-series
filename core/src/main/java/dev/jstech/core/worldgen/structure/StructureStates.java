/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import dev.jstech.core.JsCore;
import dev.jstech.core.region.Box;
import dev.jstech.core.region.RegionIndex;
import dev.jstech.core.region.SpatialIndex;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * What each structure of a world remembers of itself, kept with its dimension over its area: a chamber opened, a
 * terminal woken, a vault emptied. A mod reads and changes it by any place inside the structure, and hears of every
 * change on the game's event bus ({@link Changed}), so the blocks of a structure act together: a terminal powered up
 * wakes the doors of the base it stands in.
 *
 * <pre>{@code
 * StructureStates.at(level, pos).ifPresent(base -> StructureStates.update(level, base, state -> {
 *     state.putBoolean("awake", true);
 *     return state;
 * }));
 * }</pre>
 */
public final class StructureStates {

    /** Each structure's state, by its key, over its box. */
    public static final RegionIndex<CompoundTag> STATES = RegionIndex.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_states"), CompoundTag.CODEC);

    private StructureStates() {
    }

    /** The structure whose pieces hold {@code pos} in {@code level}, or none. */
    public static Optional<Placed> at(final ServerLevel level, final BlockPos pos) {
        final StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure -> true);
        if (!start.isValid()) {
            return Optional.empty();
        }
        final ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getKey(start.getStructure());
        return id == null ? Optional.empty() : Optional.of(new Placed(id, start.getChunkPos(),
                start.getBoundingBox()));
    }

    /** What {@code structure} remembers; empty when it remembers nothing yet. */
    public static CompoundTag get(final ServerLevel level, final Placed structure) {
        final SpatialIndex.Entry<String, CompoundTag> kept = STATES.get(level, structure.key());
        return kept == null ? new CompoundTag() : kept.value().copy();
    }

    /** Changes what {@code structure} remembers to what {@code change} makes of it, and tells the game's event bus. */
    public static CompoundTag update(final ServerLevel level, final Placed structure,
                                     final UnaryOperator<CompoundTag> change) {
        final CompoundTag before = get(level, structure);
        final CompoundTag after = Objects.requireNonNull(change.apply(before.copy()), "a state");
        final BoundingBox box = structure.box();
        STATES.put(level, structure.key(), Box.between(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ()), after);
        if (!after.equals(before)) {
            NeoForge.EVENT_BUS.post(new Changed(level, structure, before, after.copy()));
        }
        return after;
    }

    /**
     * A structure where it stands.
     *
     * @param structure its structure's id
     * @param start     the chunk it started in
     * @param box       the box its pieces lie in
     */
    public record Placed(ResourceLocation structure, ChunkPos start, BoundingBox box) {

        /** The key its state is kept under: its structure and the chunk it started in. */
        public String key() {
            return this.structure + "@" + this.start.x + "," + this.start.z;
        }
    }

    /**
     * Posted on the game's event bus when what a structure remembers changes.
     *
     * @see StructureStates#update
     */
    public static final class Changed extends Event {

        private final ServerLevel level;
        private final Placed structure;
        private final CompoundTag before;
        private final CompoundTag after;

        public Changed(final ServerLevel level, final Placed structure, final CompoundTag before,
                       final CompoundTag after) {
            this.level = level;
            this.structure = structure;
            this.before = before;
            this.after = after;
        }

        public ServerLevel level() {
            return this.level;
        }

        public Placed structure() {
            return this.structure;
        }

        /** What it remembered before. */
        public CompoundTag before() {
            return this.before.copy();
        }

        /** What it remembers now. */
        public CompoundTag after() {
            return this.after.copy();
        }
    }
}
