/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * The piece of a growing settlement that holds the rest of its layout, the pieces still to be built, each with the
 * stage it is built in. It places nothing itself; its box is the settlement's first piece's, so it marks no chunk as
 * part of the settlement before something is built there. Saved with the settlement, so the layout the settlement
 * grows into is the one it generated with.
 */
public final class SettlementPlan extends StructurePiece {

    private final List<Planned> planned;

    private static final String PLANNED = "planned";
    private static final String STAGE = "stage";
    private static final String PIECE = "piece";
    private static final String ID = "id";

    public SettlementPlan(final BoundingBox box, final List<Planned> planned) {
        super(CoreStructures.SETTLEMENT_PLAN.get(), 0, box);
        this.planned = List.copyOf(planned);
    }

    /** The plan as a settlement's saved data holds it. */
    public SettlementPlan(final StructurePieceSerializationContext context, final CompoundTag tag) {
        super(CoreStructures.SETTLEMENT_PLAN.get(), tag);
        final List<Planned> read = new ArrayList<>();
        for (final Tag one : tag.getList(PLANNED, Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) one;
            final CompoundTag piece = entry.getCompound(PIECE);
            final ResourceLocation id = ResourceLocation.tryParse(piece.getString(ID));
            final StructurePieceType type = id == null ? null : BuiltInRegistries.STRUCTURE_PIECE.get(id);
            if (type != null) {
                read.add(new Planned(entry.getInt(STAGE), type.load(context, piece)));
            }
        }
        this.planned = List.copyOf(read);
    }

    /** The pieces still to be built, each with its stage. */
    public List<Planned> planned() {
        return this.planned;
    }

    @Override
    public void postProcess(final WorldGenLevel level, final StructureManager structures,
                            final ChunkGenerator generator, final RandomSource random, final BoundingBox box,
                            final ChunkPos chunk, final BlockPos pivot) {
        // The plan builds nothing as the world generates: the settlement's growth builds its pieces later.
    }

    @Override
    protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
        final ListTag list = new ListTag();
        for (final Planned one : this.planned) {
            final CompoundTag entry = new CompoundTag();
            entry.putInt(STAGE, one.stage());
            entry.put(PIECE, one.piece().createTag(context));
            list.add(entry);
        }
        tag.put(PLANNED, list);
    }

    /**
     * A piece of the layout still to be built.
     *
     * @param stage the stage it is built in, from 1
     * @param piece the piece, as the layout laid it out
     */
    public record Planned(int stage, StructurePiece piece) {

        public Planned {
            Objects.requireNonNull(piece, "piece");
        }
    }
}
