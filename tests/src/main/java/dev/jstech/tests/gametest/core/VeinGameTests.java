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
import dev.jstech.core.jstechseries.SeriesDeposits;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.MovedOres;
import dev.jstech.core.worldgen.vein.OreTable;
import dev.jstech.core.worldgen.vein.Prospecting;
import dev.jstech.core.worldgen.vein.ScatteredOres;
import dev.jstech.core.worldgen.vein.VeinBalance;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.core.worldgen.vein.VeinPainter;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's veins: each laid into a test's arena turns each rock into its own variant of the ore, and only of ores
 * some mod uses; the settings turn a material off; a vein is dense at its middle; the game's scattered iron, copper and
 * gold stand aside while ores lie in veins, and other mods' scattered ores when the server tries moving them; the kinds
 * travel through their files; and in a generated land, veins lie where the field says and an instrument finds them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class VeinGameTests {

    private static final String ARENA = "empty";
    private static final int SIDE = 8;
    private static final int SIDE1 = SIDE + 1;
    private static final int MIDDLE = 4;
    private static final int ZONES_SEARCHED = 64;
    private static final ResourceLocation MOVED_ORE = TestWorldGen.LAND_ORE.id();

    private VeinGameTests() {
    }

    @GameTest(template = ARENA)
    public static void vein_turnsEachRockIntoItsOwnOre(final GameTestHelper helper) {
        layers(helper, Blocks.DEEPSLATE, Blocks.STONE, Blocks.GRANITE);
        helper.setBlock(new BlockPos(MIDDLE, 2, MIDDLE), Blocks.DIRT);
        final Deposits.Entry iron = Deposits.get(TestWorldGen.IRON_VEINS).orElseThrow();
        paint(helper, iron, 1.0);
        final Block tinInGranite = OreTable.oreOf(SeriesMaterials.TIN.id(), HostRocks.GRANITE);
        final Block tinInStone = OreTable.oreOf(SeriesMaterials.TIN.id(), HostRocks.STONE);
        final Block tinInDeepslate = OreTable.oreOf(SeriesMaterials.TIN.id(), HostRocks.DEEPSLATE);
        helper.assertTrue(tinInGranite != null && tinInStone != null && tinInDeepslate != null,
                "the tests use tin's ore in granite, stone and deepslate");
        helper.assertTrue(count(helper, 1, Blocks.DEEPSLATE_IRON_ORE) + count(helper, 1, tinInDeepslate) > 0,
                "deepslate turns to deepslate's ores");
        helper.assertTrue(count(helper, 1, Blocks.IRON_ORE) + count(helper, 1, tinInStone) == 0,
                "and never to stone's");
        helper.assertTrue(count(helper, 2, Blocks.IRON_ORE) + count(helper, 2, tinInStone) > 0,
                "stone turns to stone's ores, the game's own iron among them");
        helper.assertTrue(count(helper, 3, tinInGranite) > 0, "granite turns to granite's tin");
        final Block ironInGranite = OreTable.oreOf(SeriesMaterials.IRON.id(), HostRocks.GRANITE);
        final int other = SIDE1 * SIDE1 - count(helper, 3, Blocks.GRANITE) - count(helper, 3, tinInGranite)
                - (ironInGranite == null ? 0 : count(helper, 3, ironInGranite));
        helper.assertTrue(other == 0, "and only to granite's own ores, whichever mods use there, not " + other
                + " blocks of others");
        helper.assertBlockState(new BlockPos(MIDDLE, 2, MIDDLE), state -> state.is(Blocks.DIRT),
                () -> "what is no rock is left as it was");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void vein_laysNothingOfAnOreNoModUses(final GameTestHelper helper) {
        layers(helper, Blocks.GRANITE, Blocks.GRANITE, Blocks.GRANITE);
        final int laid = paint(helper, Deposits.get(TestWorldGen.GRANITE_GOLD).orElseThrow(), 1.0);
        helper.assertTrue(laid == 0, "gold's ore in granite is used by no mod, so none is laid, not " + laid);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void vein_leavesOutAMaterialTheSettingsTurnOff(final GameTestHelper helper) {
        layers(helper, Blocks.GRANITE, Blocks.GRANITE, Blocks.GRANITE);
        VeinBalance.setWithoutOre(List.of(SeriesMaterials.TIN.id().toString()));
        try {
            paint(helper, Deposits.get(TestWorldGen.IRON_VEINS).orElseThrow(), 1.0);
            final Block tin = OreTable.oreOf(SeriesMaterials.TIN.id(), HostRocks.GRANITE);
            final int laid = count(helper, 1, tin) + count(helper, 2, tin) + count(helper, 3, tin);
            helper.assertTrue(laid == 0, "with tin's ore turned off, the granite gets none of it, not " + laid);
        } finally {
            VeinBalance.setWithoutOre(List.of());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void vein_isDenseAtItsMiddleAndThinsOutwards(final GameTestHelper helper) {
        layers(helper, Blocks.STONE, Blocks.STONE, Blocks.STONE);
        final Deposits.Entry iron = Deposits.get(TestWorldGen.IRON_VEINS).orElseThrow();
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, 2, MIDDLE));
        final VeinField.Vein vein = new VeinField.Vein(0, middle.getX(), middle.getY(), middle.getZ(), 6, 6, 9L);
        paint(helper, iron, vein, 0.8);
        int nearOre = 0;
        int near = 0;
        int farOre = 0;
        int far = 0;
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                final double across = Math.hypot(x - MIDDLE, z - MIDDLE);
                for (int y = 1; y <= 3; y++) {
                    final boolean ore = !helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE);
                    if (across < 2) {
                        near++;
                        nearOre += ore ? 1 : 0;
                    } else if (across > 4 && across < 6) {
                        far++;
                        farOre += ore ? 1 : 0;
                    }
                }
            }
        }
        helper.assertTrue((double) nearOre / near > (double) farOre / far,
                "more of the vein is ore at its middle (" + nearOre + "/" + near + ") than out by its edge ("
                        + farOre + "/" + far + ")");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scatteredOres_standAsideWhileOresLieInVeins(final GameTestHelper helper) {
        final ScatteredOres game = new ScatteredOres(false);
        final ScatteredOres other = new ScatteredOres(true);
        final boolean wasOn = VeinBalance.enabled();
        final boolean wasMoving = VeinBalance.moveOthers();
        try {
            VeinBalance.setEnabled(true);
            VeinBalance.setMoveOthers(false);
            helper.assertFalse(game.laid(), "the game's scattered iron is not laid while ores lie in veins");
            helper.assertTrue(other.laid(), "another mod's scattered ore is, unless the server tries moving it");
            VeinBalance.setMoveOthers(true);
            helper.assertFalse(other.laid(), "and is not while it tries");
            VeinBalance.setEnabled(false);
            helper.assertTrue(game.laid() && other.laid(), "with veins off, every scattered ore is laid again");
        } finally {
            VeinBalance.setEnabled(wasOn);
            VeinBalance.setMoveOthers(wasMoving);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void biomes_layVeinsAndFilterTheScatteredOres(final GameTestHelper helper) {
        final Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        final Registry<PlacedFeature> placed = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE);
        final PlacedFeature veins = placed.get(ResourceLocation.fromNamespaceAndPath("jscore", "veins"));
        final PlacedFeature scatteredIron = placed.get(OrePlacements.ORE_IRON_UPPER);
        final List<Holder<PlacedFeature>> plains = features(biomes.getHolderOrThrow(Biomes.PLAINS));
        helper.assertTrue(plains.stream().anyMatch(feature -> feature.value() == veins), "plains lay the veins");
        helper.assertFalse(plains.stream().anyMatch(feature -> feature.value() == scatteredIron),
                "and not the game's scattered iron as it was");
        helper.assertTrue(plains.stream().anyMatch(feature -> feature.value().feature() == scatteredIron.feature()
                        && filterOf(feature).filter(filter -> !filter.laid()).isPresent()),
                "but the same ore behind the filter, which lays none while ores lie in veins");
        final List<Holder<PlacedFeature>> land = features(biomes.getHolderOrThrow(TestWorldGen.STEPPE.key()));
        helper.assertTrue(land.stream().anyMatch(feature -> feature.unwrapKey().isEmpty()
                        && filterOf(feature).filter(ScatteredOres::laid).isPresent()),
                "another mod's scattered ore in the test land is behind the filter too, laid while not moved");
        helper.assertTrue(MovedOres.deposits().stream().anyMatch(entry -> entry.id()
                        .equals(MOVED_ORE.withPrefix("moved/")) && !entry.targets().isEmpty()),
                "and it has a vein of its own to be moved into");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void deposits_comeFromTheirFilesAndTravelThroughThem(final GameTestHelper helper) {
        final List<Deposits.Entry> all = Deposits.all();
        for (final ResourceLocation id : List.of(SeriesDeposits.IRON, SeriesDeposits.COPPER, SeriesDeposits.GOLD,
                SeriesDeposits.NETHER_GOLD, SeriesDeposits.TIN, TestWorldGen.IRON_VEINS)) {
            // A mod whose own deposits stand in for a plain vein supersedes it, its file and all.
            helper.assertTrue(all.stream().anyMatch(entry -> entry.id().equals(id))
                    != Deposits.superseded().contains(id), "the vein " + id + " is laid unless it is superseded");
            helper.assertTrue(Deposits.FILES.get(id).isPresent(), "from a file the data generation wrote");
            helper.assertTrue(Deposits.FILES.get(id).get().equals(Deposits.declared().get(id)),
                    "which says what its declaration says");
        }
        final DepositType type = Deposits.declared().get(TestWorldGen.IRON_VEINS);
        final JsonElement written = DepositType.CODEC.encodeStart(JsonOps.INSTANCE, type).getOrThrow();
        helper.assertTrue(DepositType.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow().equals(type),
                "a kind of vein reads back from its file");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void testLand_laysVeinsWhereTheFieldSaysAndAnInstrumentFindsThem(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "veins_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final VeinField.Vein vein = LandSurvey.vein(land, TestWorldGen.IRON_VEINS, ZONES_SEARCHED);
            helper.assertTrue(vein != null, "the test land holds a vein of its iron");
            land.getChunk(vein.x() >> 4, vein.z() >> 4);
            int laid = 0;
            for (int x = vein.x() - 3; x <= vein.x() + 3; x++) {
                for (int z = vein.z() - 3; z <= vein.z() + 3; z++) {
                    for (int y = vein.y() - 2; y <= vein.y() + 2; y++) {
                        final Block there = land.getBlockState(new BlockPos(x, y, z)).getBlock();
                        laid += there == Blocks.IRON_ORE || there == OreTable.oreOf(SeriesMaterials.TIN.id(),
                                HostRocks.STONE) || there == OreTable.oreOf(SeriesMaterials.IRON.id(),
                                HostRocks.TUFF) ? 1 : 0;
                    }
                }
            }
            helper.assertTrue(laid > 0, "its middle is ore in the generated land: " + laid + " blocks");
            final Optional<Prospecting.VeinReading> reading = Prospecting.sample(land,
                    new BlockPos(vein.x(), vein.y(), vein.z()));
            helper.assertTrue(reading.isPresent() && reading.get().deposit().equals(TestWorldGen.IRON_VEINS),
                    "standing in it, an instrument reads its kind");
            final double iron = reading.get().composition().getOrDefault(SeriesMaterials.IRON.id(), 0.0);
            helper.assertTrue(Math.abs(iron - 0.75) < 1e-9 && reading.get().blocks() > 0,
                    "three quarters iron, and its size, not " + reading.get());
            final BlockPos east = new BlockPos(vein.x() + 30, vein.y(), vein.z());
            final Optional<Prospecting.Prospect> towards = Prospecting.prospect(land, east, 48).stream()
                    .filter(prospect -> prospect.deposit().equals(TestWorldGen.IRON_VEINS)
                            && prospect.direction().x < -0.9).findFirst();
            helper.assertTrue(towards.isPresent() && towards.get().strength() > 0 && towards.get().strength() < 1,
                    "thirty blocks east of it, an instrument finds it to the west, and how strongly it shows");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    /* Lays three layers of rock in the arena, from y 1 up. */
    private static void layers(final GameTestHelper helper, final Block low, final Block middle, final Block high) {
        final Block[] rocks = {low, middle, high};
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), rocks[y - 1]);
                }
            }
        }
    }

    /* Lays a vein of {@code entry} filling the arena, wholly dense at its middle. */
    private static int paint(final GameTestHelper helper, final Deposits.Entry entry, final double density) {
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, 2, MIDDLE));
        return paint(helper, entry, new VeinField.Vein(0, middle.getX(), middle.getY(), middle.getZ(), 12, 4, 3L),
                density);
    }

    private static int paint(final GameTestHelper helper, final Deposits.Entry entry, final VeinField.Vein vein,
                             final double density) {
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        return VeinPainter.paint(helper.getLevel(), entry, vein, density, Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()));
    }

    /* How many blocks of {@code block} the arena's layer at {@code y} holds. */
    private static int count(final GameTestHelper helper, final int y, final Block block) {
        int found = 0;
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                found += helper.getBlockState(new BlockPos(x, y, z)).is(block) ? 1 : 0;
            }
        }
        return found;
    }

    private static List<Holder<PlacedFeature>> features(final Holder<Biome> biome) {
        return biome.value().getGenerationSettings().features().stream().flatMap(HolderSet::stream).toList();
    }

    /* The filter a feature's placement starts with, when it starts with one. */
    private static Optional<ScatteredOres> filterOf(final Holder<PlacedFeature> feature) {
        final List<PlacementModifier> placement = feature.value().placement();
        return !placement.isEmpty() && placement.getFirst() instanceof ScatteredOres filter ? Optional.of(filter)
                : Optional.empty();
    }
}
