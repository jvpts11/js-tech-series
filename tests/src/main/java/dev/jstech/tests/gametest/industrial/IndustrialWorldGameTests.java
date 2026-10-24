/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.jstechseries.SeriesDeposits;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.text.Text;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.core.worldgen.vein.VeinPainter;
import dev.jstech.industrial.world.IndustrialDeposits;
import dev.jstech.industrial.world.ProspectReport;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluidMachines;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's world: its deposit types stand in for J's Core's plain veins and bring the ore of their minerals'
 * elements; a material of a deposit lies only in its part of a vein or its biomes; the prospecting pick tells a vein's
 * direction and strength and a reservoir's depth in words, never coordinates, and the scanner what a vein holds; and a
 * well pump drains the reservoir below it block by block.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialWorldGameTests {

    private static final String ARENA = "empty";
    private static final int SIDE = 8;
    private static final int MIDDLE = 4;
    private static final int ZONES_SEARCHED = 64;
    private static final double WEIGHT = 1e-9;
    private static final int WELL_TICKS = 40;

    private IndustrialWorldGameTests() {
    }

    @GameTest(template = ARENA)
    public static void deposits_standInForTheCoresPlainVeins(final GameTestHelper helper) {
        final List<ResourceLocation> laid = Deposits.all().stream().map(Deposits.Entry::id).toList();
        for (final ResourceLocation plain : List.of(SeriesDeposits.IRON, SeriesDeposits.COPPER, SeriesDeposits.GOLD,
                SeriesDeposits.TIN)) {
            helper.assertFalse(laid.contains(plain), "the Core's plain vein " + plain + " is laid no more");
        }
        helper.assertTrue(laid.contains(SeriesDeposits.NETHER_GOLD), "the Nether's gold stays the Core's");
        for (final ResourceLocation deposit : IndustrialDeposits.all()) {
            helper.assertTrue(laid.contains(deposit), "the deposit type " + deposit + " is laid");
            helper.assertTrue(Deposits.FILES.get(deposit).isPresent()
                            && Deposits.FILES.get(deposit).get().equals(Deposits.declared().get(deposit)),
                    "from a file the data generation wrote, saying what its declaration says");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void deposit_bringsTheOreOfItsMineralsElements(final GameTestHelper helper) {
        final DepositType porphyry = Deposits.declared().get(IndustrialDeposits.PORPHYRY_COPPER);
        helper.assertTrue(Math.abs(weightOf(porphyry, SeriesMaterials.COPPER.id())
                - 4 * IndustrialDeposits.ELEMENT_ORE_SHARE) < WEIGHT, "copper's ore, a quarter of chalcopyrite's");
        helper.assertTrue(Math.abs(weightOf(porphyry, SeriesMaterials.IRON.id())
                - 3 * IndustrialDeposits.ELEMENT_ORE_SHARE) < WEIGHT, "iron's, a quarter of pyrite's and magnetite's");
        helper.assertTrue(porphyry.rocks().containsAll(List.of(HostRocks.GRANITE.id(), HostRocks.ANDESITE.id(),
                HostRocks.STONE.id(), HostRocks.DEEPSLATE.id())), "it crosses its rocks and those of the game's ores");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void zone_laysAMaterialOnlyInItsPartOfTheVein(final GameTestHelper helper) {
        stone(helper);
        final DepositType zoned = type(List.of(
                new DepositType.Mineral(SeriesMaterials.IRON.id(), 1, DepositType.Zone.UPPER, Optional.empty()),
                new DepositType.Mineral(SeriesMaterials.GOLD.id(), 1, DepositType.Zone.LOWER, Optional.empty())));
        paint(helper, zoned);
        helper.assertTrue(count(helper, 3, Blocks.IRON_ORE) > 0 && count(helper, 3, Blocks.GOLD_ORE) == 0,
                "above the middle, the upper zone's iron alone");
        helper.assertTrue(count(helper, 1, Blocks.GOLD_ORE) > 0 && count(helper, 1, Blocks.IRON_ORE) == 0,
                "below it, the lower zone's gold alone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void zone_laysAMaterialOnlyInItsBiomes(final GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getBiome(helper.absolutePos(new BlockPos(MIDDLE, 2, MIDDLE)))
                .is(BiomeTags.IS_OVERWORLD), "the arena is in an overworld biome");
        stone(helper);
        paint(helper, type(List.of(
                new DepositType.Mineral(SeriesMaterials.IRON.id(), 1, DepositType.Zone.ALL,
                        Optional.of(BiomeTags.IS_OVERWORLD)),
                new DepositType.Mineral(SeriesMaterials.GOLD.id(), 1, DepositType.Zone.ALL,
                        Optional.of(BiomeTags.IS_NETHER)))));
        int iron = 0;
        int gold = 0;
        for (int y = 1; y <= 3; y++) {
            iron += count(helper, y, Blocks.IRON_ORE);
            gold += count(helper, y, Blocks.GOLD_ORE);
        }
        helper.assertTrue(iron > 0 && gold == 0, "the overworld's material, never the Nether's: " + iron + ", "
                + gold);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void prospecting_saysDirectionStrengthAndDepthInWords(final GameTestHelper helper) {
        helper.assertTrue(ProspectReport.direction(new Vec3(1, 0, -1)) == ProspectReport.NORTH_EAST,
                "north is towards negative z, east towards positive x");
        helper.assertTrue(ProspectReport.direction(new Vec3(-1, -0.2, 0.05)) == ProspectReport.WEST, "west");
        helper.assertTrue(ProspectReport.direction(new Vec3(0.1, -1, 0)) == ProspectReport.BELOW, "right below");
        helper.assertTrue(ProspectReport.direction(Vec3.ZERO) == ProspectReport.HERE, "right here");
        helper.assertTrue(ProspectReport.strength(0.2) == ProspectReport.TRACES
                && ProspectReport.strength(0.5) == ProspectReport.VEIN
                && ProspectReport.strength(0.9) == ProspectReport.RICH, "traces, a vein, a rich vein");
        helper.assertTrue(ProspectReport.depth(37) == 40 && ProspectReport.depth(3) == ProspectReport.DEPTH_STEP,
                "a depth in tens of blocks");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void pick_findsTheLandsVeinAndReservoirAndTheScannerReadsThem(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "prospect_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final VeinField.Vein vein = LandSurvey.vein(land, TestWorldGen.IRON_VEINS, ZONES_SEARCHED);
            helper.assertTrue(vein != null, "the test land holds a vein of its iron");
            final BlockPos middle = new BlockPos(vein.x(), vein.y(), vein.z());
            final List<String> east = english(ProspectReport.pick(land, middle.east(30)));
            helper.assertTrue(east.stream().anyMatch(line -> line.contains("Iron") && line.endsWith("to the west")),
                    "thirty blocks east of it, the pick finds iron to the west: " + east);
            final List<String> scanned = english(ProspectReport.scan(land, middle));
            helper.assertTrue(scanned.getFirst().startsWith("About ") && scanned.stream()
                    .anyMatch(line -> line.startsWith("Iron: ")), "the scanner reads its ore and its iron: "
                    + scanned);
            final Reservoirs.Reservoir reservoir = LandSurvey.reservoir(land, TestWorldGen.BRINES, ZONES_SEARCHED);
            helper.assertTrue(reservoir != null, "the test land holds a reservoir of its brine");
            final BlockPos above = new BlockPos(reservoir.body().x(), reservoir.body().y() + 30, reservoir.body().z());
            final List<String> over = english(ProspectReport.pick(land, above));
            helper.assertTrue(over.stream().anyMatch(line -> line.contains("about 30 blocks down, right below")),
                    "above it, the pick tells it right below and about how deep: " + over);
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void wellPump_drainsTheReservoirBelowItBlockByBlock(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "well_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final Reservoirs.Reservoir reservoir = LandSurvey.reservoir(land, TestWorldGen.BRINES, ZONES_SEARCHED);
            helper.assertTrue(reservoir != null, "the test land holds a reservoir of its brine");
            final BlockPos top = new BlockPos(reservoir.body().x(), reservoir.body().y()
                    + (int) Math.ceil(reservoir.body().thickness()) + 4, reservoir.body().z());
            land.getChunk(top.getX() >> 4, top.getZ() >> 4);
            final long before = Reservoirs.left(land, reservoir);
            final BlockState state = TestFluidMachines.WELL_PUMP.get().defaultBlockState();
            land.setBlockAndUpdate(top, state);
            final TestFluidMachines.WellPump pump = (TestFluidMachines.WellPump) land.getBlockEntity(top);
            helper.assertTrue(pump != null, "the well pump stands over it");
            for (int tick = 0; tick < WELL_TICKS; tick++) {
                pump.power().generate(pump.power().capacity());
                MachineBlockEntity.serverTick(land, top, state, pump);
            }
            final int pumped = pump.tank().getFluidAmount();
            helper.assertTrue(pump.tank().getFluid().getFluid() == Fluids.WATER && pumped >= 1_000,
                    "it pumps the brine's water: " + pumped);
            helper.assertTrue(Reservoirs.left(land, reservoir) == before - pumped,
                    "the reservoir holds less by what was pumped");
            land.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    private static double weightOf(final DepositType type, final ResourceLocation material) {
        return type.minerals().stream().filter(mineral -> mineral.material().equals(material))
                .mapToDouble(DepositType.Mineral::weight).sum();
    }

    private static DepositType type(final List<DepositType.Mineral> minerals) {
        return new DepositType(minerals, 0, 0, 1, 12, 4, 1, Optional.empty(), 0, List.of());
    }

    /* Three layers of stone in the arena, from y 1 up. */
    private static void stone(final GameTestHelper helper) {
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /* Lays a vein of {@code type} filling the arena, its middle at y 2, wholly dense there. */
    private static void paint(final GameTestHelper helper, final DepositType type) {
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, 2, MIDDLE));
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        VeinPainter.paint(helper.getLevel(), new Deposits.Entry(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                        "zoned"), type, List.of()), new VeinField.Vein(0, middle.getX(), middle.getY(), middle.getZ(),
                        12, 4, 7L), 1.0, Math.min(low.getX(), high.getX()), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), Math.max(low.getZ(), high.getZ()));
    }

    private static int count(final GameTestHelper helper, final int y, final Block block) {
        int found = 0;
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                found += helper.getBlockState(new BlockPos(x, y, z)).is(block) ? 1 : 0;
            }
        }
        return found;
    }

    private static List<String> english(final List<Text> lines) {
        return lines.stream().map(Text::english).toList();
    }
}
