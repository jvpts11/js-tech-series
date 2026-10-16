/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.worldgen.place.Finding;
import dev.jstech.core.worldgen.structure.CoreStructures;
import dev.jstech.core.worldgen.structure.FixedPlacement;
import dev.jstech.core.worldgen.structure.PlacedStructure;
import dev.jstech.core.worldgen.structure.SettlementPlan;
import dev.jstech.core.worldgen.structure.Settlements;
import dev.jstech.core.worldgen.structure.SpreadPlacement;
import dev.jstech.core.worldgen.structure.StructureFit;
import dev.jstech.core.worldgen.structure.StructurePlacer;
import dev.jstech.core.worldgen.structure.Weathering;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's structures as the world generates them: their kinds reach the registries; a spread keeps clear of the
 * world's origin and of other structures; the wear of a structure goes by rule and age, keeps the worn block's facing
 * and is the same each time; foundations reach down to the ground; and in a generated land a worn hut stands where
 * the land suits it and is found there, a monument at its one place only, and a town grows a stage a day from its
 * plan into the chunks that are loaded. The GameTest server's worlds are made without structures, so these are placed
 * where their placements pick, as the world would have placed them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PlacedStructureGameTests {

    private static final String ARENA = "empty";
    private static final int PLACES_TRIED = 400;
    private static final int CHUNKS_SEARCHED = 32;

    private PlacedStructureGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declarations_reachTheRegistriesAsTheCoresKinds(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final Registry<Structure> structures = registries.registryOrThrow(Registries.STRUCTURE);
        final Registry<StructureSet> sets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        helper.assertTrue(structures.get(TestWorldGen.HUT.id()) instanceof PlacedStructure hut
                        && hut.type() == CoreStructures.PLACED.get() && hut.fit().equals(TestWorldGen.HUT.fit())
                        && hut.foundation().map(state -> state.is(Blocks.STONE_BRICKS)).orElse(false),
                "the hut is a placed structure, with its fit and its foundations");
        helper.assertTrue(sets.get(TestWorldGen.HUT.id()).placement() instanceof SpreadPlacement spread
                        && spread.minDistance() == 64 && spread.clearOf() == 1 && spread.spacing() == 8,
                "its set spreads it clear of the origin and of others");
        helper.assertTrue(sets.get(TestWorldGen.MONUMENT.id()).placement() instanceof FixedPlacement fixed
                        && fixed.chunks().equals(List.of(new ChunkPos(TestWorldGen.MONUMENT_CHUNK, 0))),
                "the monument's set places it at its one chunk");
        helper.assertTrue(structures.get(TestWorldGen.TOWN.id()) instanceof PlacedStructure town
                        && town.growth().equals(TestWorldGen.TOWN.growth()) && town.size() == 4
                        && town.expansionHack(), "the town grows, four pieces deep, its streets holding houses");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                        .get(TestWorldGen.HUT.id()).list().getFirst() instanceof Weathering.Processor,
                "the hut is worn by the Core's weathering");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void fit_refusesGroundTooSteepTooLowOrTooHigh(final GameTestHelper helper) {
        final StructureFit fit = TestWorldGen.HUT.fit();
        helper.assertTrue(fit.suits(60, 66), "ground that rises as far as it allows suits it");
        helper.assertTrue(!fit.suits(60, 67), "steeper ground does not");
        helper.assertTrue(!fit.suits(39, 40) && !fit.suits(121, 122), "nor ground below or above its heights");
        helper.assertTrue(StructureFit.ANYWHERE.suits(-2000, 2000), "and any ground suits a structure of anywhere");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void weathering_wearsByRuleAndAgeKeepingFacingAndAlike(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        final StructureTemplate.StructureBlockInfo whole = wear(level, rule(1.0), 1, BlockPos.ZERO, stairs);
        helper.assertTrue(whole.state().is(Blocks.BIRCH_STAIRS)
                        && whole.state().getValue(StairBlock.FACING) == Direction.EAST,
                "a sure rule wears every block, keeping its facing");
        helper.assertTrue(wear(level, rule(1.0), 1, BlockPos.ZERO, Blocks.STONE.defaultBlockState()).state()
                .is(Blocks.STONE), "and leaves the blocks it does not match");
        int halfWorn = 0;
        int newWorn = 0;
        boolean alike = true;
        for (int i = 0; i < PLACES_TRIED; i++) {
            final BlockPos at = new BlockPos(i % 20, 0, i / 20);
            final boolean once = wear(level, rule(0.5), 1, at, stairs).state().is(Blocks.BIRCH_STAIRS);
            alike &= once == wear(level, rule(0.5), 1, at, stairs).state().is(Blocks.BIRCH_STAIRS);
            halfWorn += once ? 1 : 0;
            newWorn += wear(level, rule(0.5), 0, at, stairs).state().is(Blocks.BIRCH_STAIRS) ? 1 : 0;
        }
        helper.assertTrue(halfWorn > PLACES_TRIED / 4 && halfWorn < PLACES_TRIED * 3 / 4,
                "a rule of half wears about half the blocks, not " + halfWorn);
        helper.assertTrue(alike, "the same at each place each time");
        helper.assertTrue(newWorn == 0, "a new structure wears none");
        final JsonElement written = Weathering.Processor.CODEC.codec().encodeStart(JsonOps.INSTANCE,
                new Weathering(List.of(rule(0.4)), 0.5).processor()).getOrThrow();
        final Weathering read = Weathering.Processor.CODEC.codec().parse(JsonOps.INSTANCE, written).getOrThrow()
                .weathering();
        helper.assertTrue(read.age() == 0.5 && read.rules().size() == 1 && read.rules().getFirst().chance() == 0.4
                && read.rules().getFirst().worn().is(Blocks.BIRCH_STAIRS), "the processor reads back from its file");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void foundations_reachDownFromTheLowestLayerToTheGround(final GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                if (x >= 1 && x <= 3 && z >= 1 && z <= 3 && !(x == 3 && z == 3)) {
                    helper.setBlock(new BlockPos(x, 5, z), Blocks.OAK_PLANKS);
                }
            }
        }
        final BlockPos low = helper.absolutePos(new BlockPos(1, 5, 1));
        final BlockPos high = helper.absolutePos(new BlockPos(3, 7, 3));
        final BoundingBox box = BoundingBox.fromCorners(low, high);
        PlacedStructure.found(helper.getLevel(), box, box.inflatedBy(16), Blocks.STONE_BRICKS.defaultBlockState());
        for (int y = 2; y <= 4; y++) {
            helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(1, y, 1));
            helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(2, y, 3));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, y, 3));
        }
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 1));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 3, 0));
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void spread_keepsClearOfTheOriginAndOfTheTown(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy("spread");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final ChunkGeneratorStructureState state = land.getChunkSource().getGeneratorState();
            final Registry<StructureSet> sets = land.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            final SpreadPlacement huts = (SpreadPlacement) sets.get(TestWorldGen.HUT.id()).placement();
            final SpreadPlacement towns = (SpreadPlacement) sets.get(TestWorldGen.TOWN.id()).placement();
            int placed = 0;
            for (int x = -CHUNKS_SEARCHED; x <= CHUNKS_SEARCHED; x++) {
                for (int z = -CHUNKS_SEARCHED; z <= CHUNKS_SEARCHED; z++) {
                    if (!huts.isStructureChunk(state, x, z)) {
                        continue;
                    }
                    placed++;
                    final ChunkPos chunk = new ChunkPos(x, z);
                    final long far = (long) chunk.getMiddleBlockX() * chunk.getMiddleBlockX()
                            + (long) chunk.getMiddleBlockZ() * chunk.getMiddleBlockZ();
                    helper.assertTrue(far >= 64L * 64L, "no hut within 64 blocks of the origin, but at " + chunk);
                    for (int i = x - 1; i <= x + 1; i++) {
                        for (int j = z - 1; j <= z + 1; j++) {
                            helper.assertTrue(!towns.isStructureChunk(state, i, j),
                                    "no hut within a chunk of a town, but at " + chunk);
                        }
                    }
                }
            }
            helper.assertTrue(placed > 0, "huts are placed");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void testLand_standsAWornHutWhereTheLandSuitsIt(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy("hut");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final StructureStart start = placeWhereSpread(land, TestWorldGen.HUT.id());
            helper.assertTrue(start != null, "a hut stands at one of the places its spread picks");
            final Optional<Finding.Found> found = Finding.find(land, start.getChunkPos().getMiddleBlockPosition(64),
                    new Finding.StructureTarget(TestWorldGen.HUTS), Finding.EXACT);
            helper.assertTrue(found.isPresent() && new ChunkPos(found.get().near()).equals(start.getChunkPos()),
                    "the instruments find it, as the world knows it");
            final BoundingBox box = start.getBoundingBox();
            int oak = 0;
            int birch = 0;
            for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                    box.maxY(), box.maxZ())) {
                final BlockState block = land.getBlockState(at);
                oak += block.is(Blocks.OAK_PLANKS) ? 1 : 0;
                birch += block.is(Blocks.BIRCH_PLANKS) ? 1 : 0;
            }
            helper.assertTrue(oak > 0 && birch > 0, "its oak planks half worn to birch, not " + oak + " oak and "
                    + birch + " birch");
            final long far = (long) start.getChunkPos().getMiddleBlockX() * start.getChunkPos().getMiddleBlockX()
                    + (long) start.getChunkPos().getMiddleBlockZ() * start.getChunkPos().getMiddleBlockZ();
            helper.assertTrue(far >= 64L * 64L, "far enough from the origin");
            helper.assertTrue(!land.getFluidState(new BlockPos(box.getCenter().getX(), box.minY(),
                    box.getCenter().getZ())).isSource(), "on dry land");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void testLand_standsTheMonumentAtItsOnePlaceOnly(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy("monument");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final ChunkGeneratorStructureState state = land.getChunkSource().getGeneratorState();
            final FixedPlacement fixed = (FixedPlacement) land.registryAccess().registryOrThrow(
                    Registries.STRUCTURE_SET).get(TestWorldGen.MONUMENT.id()).placement();
            helper.assertTrue(fixed.isStructureChunk(state, TestWorldGen.MONUMENT_CHUNK, 0),
                    "the monument is placed at its chunk");
            for (final ChunkPos elsewhere : List.of(new ChunkPos(0, 0), new ChunkPos(TestWorldGen.MONUMENT_CHUNK + 1,
                    0), new ChunkPos(TestWorldGen.MONUMENT_CHUNK, 12), new ChunkPos(-TestWorldGen.MONUMENT_CHUNK, 0))) {
                helper.assertTrue(!fixed.isStructureChunk(state, elsewhere.x, elsewhere.z),
                        "and nowhere else, as at " + elsewhere);
            }
            final ChunkPos at = new ChunkPos(TestWorldGen.MONUMENT_CHUNK, 0);
            final Optional<StructureStart> placed = StructurePlacer.place(land, TestWorldGen.MONUMENT.id(), at);
            helper.assertTrue(placed.isPresent(), "it stands there");
            final int ground = land.getChunkSource().getGenerator().getFirstOccupiedHeight(at.getMiddleBlockX(),
                    at.getMiddleBlockZ(), Heightmap.Types.WORLD_SURFACE_WG, land, land.getChunkSource().randomState());
            // A template's ground is its layer the game's jigsaw says, one above its lowest for the game's own.
            final PoolElementStructurePiece piece = (PoolElementStructurePiece) placed.get().getPieces().getFirst();
            helper.assertTrue(piece.getBoundingBox().minY() + piece.getGroundLevelDelta()
                    == ground - TestWorldGen.MONUMENT_SUNK, "sunk into the ground as deep as it says");
            final Optional<Finding.Found> found = Finding.find(land, BlockPos.ZERO,
                    new Finding.StructureTarget(TestWorldGen.MONUMENTS), Finding.EXACT);
            helper.assertTrue(found.isPresent() && new ChunkPos(found.get().near()).equals(at),
                    "and the instruments find it there, though the game's own search passes over fixed places");
            helper.assertTrue(StructurePlacer.place(land, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                    "nowhere"), at).isEmpty(), "a structure no mod registered is not placed");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void testLand_growsATownAStageADayFromItsPlan(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = copy("town");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final StructureStart start = placeWhereSpread(land, TestWorldGen.TOWN.id());
            helper.assertTrue(start != null, "a town stands at one of the places its spread picks");
            final ChunkPos at = start.getChunkPos();
            final SettlementPlan plan = start.getPieces().stream().filter(SettlementPlan.class::isInstance)
                    .map(SettlementPlan.class::cast).findFirst().orElse(null);
            helper.assertTrue(plan != null && plan.planned().stream().anyMatch(later -> later.stage() == 2)
                    && plan.planned().stream().anyMatch(later -> later.stage() == 3),
                    "the stages it has yet to grow are kept in its plan");
            helper.assertTrue(plan.planned().stream().anyMatch(later -> later.piece().toString().contains("/houses/"))
                    || start.getPieces().stream().anyMatch(piece -> piece.toString().contains("/houses/")),
                    "its streets hold houses");
            final List<BoundingBox> earlier = new ArrayList<>();
            start.getPieces().stream().filter(piece -> !(piece instanceof SettlementPlan))
                    .forEach(piece -> earlier.add(piece.getBoundingBox()));
            final StructurePiece second = largest(plan, 2, List.of());
            plan.planned().stream().filter(later -> later.stage() == 2)
                    .forEach(later -> earlier.add(later.piece().getBoundingBox()));
            final StructurePiece third = largest(plan, 3, earlier);
            helper.assertTrue(third != null, "a piece of the third stage lies apart from those before it");
            BoundingBox whole = start.getBoundingBox();
            for (final SettlementPlan.Planned later : plan.planned()) {
                whole = BoundingBox.encapsulatingBoxes(List.of(whole, later.piece().getBoundingBox())).orElse(whole);
            }
            loadAround(land, whole);
            final String key = TestWorldGen.TOWN.id() + "@" + at.x + "," + at.z;
            helper.assertTrue(Settlements.of(land).containsKey(key), "the town is noted as it is placed");
            helper.assertTrue(Settlements.of(land).get(key).standing() == 1, "standing at its first stage");
            final Map<BlockPos, BlockState> secondBefore = snapshot(land, second.getBoundingBox());
            final Map<BlockPos, BlockState> thirdBefore = snapshot(land, third.getBoundingBox());
            helper.assertTrue(Settlements.advance(land, key, 1), "a day passes");
            helper.assertTrue(Settlements.of(land).get(key).standing() == 2, "and the town stands at its second stage");
            helper.assertTrue(changed(land, secondBefore) > 0, "whose pieces are built");
            helper.assertTrue(changed(land, thirdBefore) == 0, "but not the third's yet: " + diff(land, thirdBefore));
            helper.assertTrue(Settlements.advance(land, key, 1) && Settlements.of(land).get(key).standing() == 3
                    && changed(land, thirdBefore) > 0, "another day, and its third stage is built");
            helper.assertTrue(Settlements.advance(land, key, 5) && Settlements.of(land).get(key).standing() == 3,
                    "and it grows no further than it was laid out");
            helper.assertTrue(!Settlements.advance(land, "nowhere@0,0", 1), "a settlement not noted does not grow");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    /*
     * Places the structure {@code id} at the first of the chunks round the origin its spread picks where it stands, as
     * the world would place it there; null when it stands at none of them. The GameTest server's worlds are made
     * without structures, so its own test land generates none by itself.
     */
    private static StructureStart placeWhereSpread(final ServerLevel land, final ResourceLocation id) {
        final ChunkGeneratorStructureState state = land.getChunkSource().getGeneratorState();
        final SpreadPlacement spread = (SpreadPlacement) land.registryAccess().registryOrThrow(
                Registries.STRUCTURE_SET).get(id).placement();
        for (int x = -CHUNKS_SEARCHED; x <= CHUNKS_SEARCHED; x++) {
            for (int z = -CHUNKS_SEARCHED; z <= CHUNKS_SEARCHED; z++) {
                if (spread.isStructureChunk(state, x, z)) {
                    final Optional<StructureStart> placed = StructurePlacer.place(land, id, new ChunkPos(x, z));
                    if (placed.isPresent()) {
                        return placed.get();
                    }
                }
            }
        }
        return null;
    }

    /* The largest piece of {@code stage} in the plan whose box meets none of {@code apart}, or null. */
    private static StructurePiece largest(final SettlementPlan plan, final int stage, final List<BoundingBox> apart) {
        StructurePiece best = null;
        long most = -1;
        for (final SettlementPlan.Planned later : plan.planned()) {
            final BoundingBox box = later.piece().getBoundingBox();
            // Apart over the ground: a piece laid on the land's surface builds above or below its nominal box.
            if (later.stage() != stage || apart.stream().anyMatch(other -> other.intersects(box.minX(), box.minZ(),
                    box.maxX(), box.maxZ()))) {
                continue;
            }
            final long size = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
            if (size > most) {
                most = size;
                best = later.piece();
            }
        }
        return best;
    }

    private static Weathering.Rule rule(final double chance) {
        return new Weathering.Rule(new BlockMatchTest(Blocks.OAK_STAIRS), Blocks.BIRCH_STAIRS.defaultBlockState(),
                chance);
    }

    private static StructureTemplate.StructureBlockInfo wear(final ServerLevel level, final Weathering.Rule rule,
                                                             final double age, final BlockPos at,
                                                             final BlockState block) {
        final StructureTemplate.StructureBlockInfo info = new StructureTemplate.StructureBlockInfo(at, block, null);
        return new Weathering(List.of(rule), age).processor().processBlock(level, BlockPos.ZERO, at, info, info,
                new StructurePlaceSettings());
    }

    /* Loads every chunk the box lies over, fully, so every piece in it is placed. */
    private static void loadAround(final ServerLevel land, final BoundingBox box) {
        for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
            for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                final LevelChunk chunk = land.getChunk(cx, cz);
                Settlements.look(land, chunk);
            }
        }
    }

    private static Map<BlockPos, BlockState> snapshot(final ServerLevel land, final BoundingBox box) {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            blocks.put(at.immutable(), land.getBlockState(at));
        }
        return blocks;
    }

    private static int changed(final ServerLevel land, final Map<BlockPos, BlockState> before) {
        int count = 0;
        for (final Map.Entry<BlockPos, BlockState> one : before.entrySet()) {
            count += land.getBlockState(one.getKey()).equals(one.getValue()) ? 0 : 1;
        }
        return count;
    }

    private static String diff(final ServerLevel land, final Map<BlockPos, BlockState> before) {
        final StringBuilder out = new StringBuilder();
        int shown = 0;
        for (final Map.Entry<BlockPos, BlockState> one : before.entrySet()) {
            final BlockState now = land.getBlockState(one.getKey());
            if (!now.equals(one.getValue()) && shown++ < 6) {
                out.append(one.getKey().toShortString()).append(' ').append(one.getValue()).append("->").append(now)
                        .append("; ");
            }
        }
        return out.toString();
    }

    private static ResourceKey<Level> copy(final String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                name + "_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
