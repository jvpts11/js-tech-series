/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * The structure type {@code jscore:placed}: the game's jigsaw structures, of one piece or a large layout of many, with
 * what they lack: they stand only where the land suits them ({@link StructureFit}: slope, heights, dry land), sunk
 * into the ground as deep as they say, with foundations reaching down from their lowest blocks to the ground (one on
 * foundations stands level with the highest of its ground, so nothing of it is buried on a slope), and, for
 * a settlement, growing over time to the size it was laid out at ({@link Growth}).
 *
 * <pre>{@code
 * { "type": "jscore:placed", "biomes": "#minecraft:is_overworld", "step": "surface_structures",
 *   "terrain_adaptation": "beard_thin", "start_pool": "mymod:outpost/start", "size": 6,
 *   "max_distance_from_center": 80, "fit": {"slope": 6, "dry": true},
 *   "foundation": {"Name": "minecraft:cobblestone"}, "growth": {"stages": 4, "built": 1, "days": 3},
 *   "use_expansion_hack": true }
 * }</pre>
 */
public final class PlacedStructure extends Structure {

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final int reach;
    private final StructureFit fit;
    private final Optional<BlockState> foundation;
    private final Optional<Growth> growth;
    private final boolean expansionHack;

    private static final int DEEPEST_FOUNDATION = 64;
    private static final int MOST_PIECES_DEEP = 20;
    private static final int WIDEST = 128;
    private static final int UPDATE = 2;
    public static final MapCodec<PlacedStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(PlacedStructure::startPool),
            Codec.intRange(0, MOST_PIECES_DEEP).optionalFieldOf("size", 1).forGetter(PlacedStructure::size),
            Codec.intRange(1, WIDEST).optionalFieldOf("max_distance_from_center", 80).forGetter(PlacedStructure::reach),
            StructureFit.CODEC.optionalFieldOf("fit", StructureFit.ANYWHERE).forGetter(PlacedStructure::fit),
            BlockState.CODEC.optionalFieldOf("foundation").forGetter(PlacedStructure::foundation),
            Growth.CODEC.optionalFieldOf("growth").forGetter(PlacedStructure::growth),
            Codec.BOOL.optionalFieldOf("use_expansion_hack", false).forGetter(PlacedStructure::expansionHack))
            .apply(instance, PlacedStructure::new));

    public PlacedStructure(final Structure.StructureSettings settings, final Holder<StructureTemplatePool> startPool,
                           final int size, final int reach, final StructureFit fit,
                           final Optional<BlockState> foundation, final Optional<Growth> growth,
                           final boolean expansionHack) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.reach = reach;
        this.fit = fit;
        this.foundation = foundation;
        this.growth = growth;
        this.expansionHack = expansionHack;
    }

    public Holder<StructureTemplatePool> startPool() {
        return this.startPool;
    }

    /** How many pieces deep its jigsaw lays out from its start. */
    public int size() {
        return this.size;
    }

    /** How far from its start its pieces may reach, in blocks. */
    public int reach() {
        return this.reach;
    }

    public StructureFit fit() {
        return this.fit;
    }

    public Optional<BlockState> foundation() {
        return this.foundation;
    }

    public Optional<Growth> growth() {
        return this.growth;
    }

    /**
     * Whether its low pieces make room above them for the taller pieces their jigsaws face inward to, as the game's
     * villages do: a street of a village holds its houses within its own box, and without the room they never fit.
     */
    public boolean expansionHack() {
        return this.expansionHack;
    }

    @Override
    public StructureType<?> type() {
        return CoreStructures.PLACED.get();
    }

    @Override
    public void afterPlace(final WorldGenLevel level, final StructureManager structures,
                           final ChunkGenerator generator, final RandomSource random, final BoundingBox chunkBox,
                           final ChunkPos chunk, final PiecesContainer pieces) {
        this.foundation.ifPresent(block -> {
            for (final StructurePiece piece : pieces.pieces()) {
                if (!(piece instanceof SettlementPlan)) {
                    found(level, piece.getBoundingBox(), chunkBox, block);
                }
            }
        });
    }

    /**
     * Lays foundations under {@code box} within {@code chunkBox}: under each block of its lowest layer that stands, a
     * column of {@code block} down to the ground.
     */
    public static void found(final WorldGenLevel level, final BoundingBox box, final BoundingBox chunkBox,
                             final BlockState block) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), chunkBox.minX()); x <= Math.min(box.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(box.minZ(), chunkBox.minZ()); z <= Math.min(box.maxZ(), chunkBox.maxZ()); z++) {
                if (level.getBlockState(at.set(x, box.minY(), z)).canBeReplaced()) {
                    continue;
                }
                for (int y = box.minY() - 1; y > Math.max(level.getMinBuildHeight(), box.minY() - DEEPEST_FOUNDATION)
                        && level.getBlockState(at.set(x, y, z)).canBeReplaced(); y--) {
                    level.setBlock(at, block, UPDATE);
                }
            }
        }
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
        final ChunkPos chunk = context.chunkPos();
        final int x = chunk.getMiddleBlockX();
        final int z = chunk.getMiddleBlockZ();
        final int out = this.fit.footprint();
        final ChunkGenerator generator = context.chunkGenerator();
        final LevelHeightAccessor height = context.heightAccessor();
        final RandomState random = context.randomState();
        final int middle = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
        int lowest = middle;
        int highest = middle;
        for (final int[] corner : new int[][] {{-out, -out}, {out, -out}, {-out, out}, {out, out}}) {
            final int ground = generator.getFirstOccupiedHeight(x + corner[0], z + corner[1],
                    Heightmap.Types.WORLD_SURFACE_WG, height, random);
            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }
        if (!this.fit.suits(lowest, highest) || this.fit.dry() && generator.getFirstOccupiedHeight(x, z,
                Heightmap.Types.OCEAN_FLOOR_WG, height, random) < middle) {
            return Optional.empty();
        }
        // On foundations it stands level with the highest of its ground, nothing of it buried, the foundations
        // reaching down to the rest; without, on the ground at its middle, as the game's own structures do.
        final BlockPos start = new BlockPos(x, (this.foundation.isPresent() ? highest : middle) - this.fit.sunk(),
                z);
        final Optional<Structure.GenerationStub> laid = JigsawPlacement.addPieces(context, this.startPool,
                Optional.empty(), this.size, start, this.expansionHack, Optional.empty(), this.reach,
                PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING);
        if (laid.isEmpty() || this.growth.isEmpty()) {
            return laid;
        }
        final Growth grows = this.growth.get();
        return Optional.of(new Structure.GenerationStub(laid.get().position(), builder -> {
            // The whole layout is laid out now; only its first stages are built, the rest kept in the plan.
            final List<StructurePiece> all = laid.get().getPiecesBuilder().build().pieces();
            final List<SettlementPlan.Planned> later = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                final int stage = grows.stageOf(i, all.size());
                if (stage <= grows.built()) {
                    builder.addPiece(all.get(i));
                } else {
                    later.add(new SettlementPlan.Planned(stage, all.get(i)));
                }
            }
            if (!later.isEmpty() && !all.isEmpty()) {
                builder.addPiece(new SettlementPlan(all.getFirst().getBoundingBox(), later));
            }
        }));
    }
}
