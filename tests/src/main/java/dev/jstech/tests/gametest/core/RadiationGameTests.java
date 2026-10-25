/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.hazard.BlockDensity;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.hazard.HazardMaps;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Radiation;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.jstechseries.hazard.CosmicRadiation;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.IonizingRadiation;
import dev.jstech.core.jstechseries.hazard.NaturalRadiation;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMaterials;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Radiation on a server: weakened by the density of each block it crosses and by the square of the distance from its
 * source; the series' ionizing radiation dosing a body by what reaches it, halved by shielding worn, recovering under
 * the background, and dosing it from the dust on its skin until water washes it off; the minerals with uranium in them
 * and the sky giving off a little, a vein reading near the same wherever the sections' edges cut it, a roof stopping
 * the sky's; the land withering where the dose rate is high; nothing
 * at all while the world has radiation off; and the Geiger counter reading the rate onto its carrier's map.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RadiationGameTests {

    private static final String ARENA = "empty";
    /* Sources reach far, so the tests that place them run apart from the rest; those that turn a setting off, alone. */
    private static final String BATCH = "jscore_radiation";
    private static final String SETTINGS = "jscore_radiation_settings";
    private static final BlockPos SOURCE = new BlockPos(1, 2, 1);
    private static final double CLOSE = 1.0E-3;
    /* An arena 48 across, wide enough to lay a vein anywhere the sections' edges fall. */
    private static final String WIDE = "bench";
    /* How far a vein's reading may differ, cut by the sections' edges, from the same vein whole in one section. */
    private static final double CUT_ALIKE = 0.2;
    private static final int SECTION = 16;

    private RadiationGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void transmission_isWeakenedByTheDensityOfEachBlockCrossed(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Vec3 from = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
        final Vec3 to = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 2)));
        final BlockPos wall = new BlockPos(3, 2, 2);
        // Three blocks between the ends; air takes a little too.
        final double air = IonizingRadiation.RAYS.transmission(BlockDensity.AIR);
        close(helper, air * air * air, Radiation.transmission(level, from, to, IonizingRadiation.RAYS),
                "through air");
        for (final Block block : new Block[] {Blocks.STONE, Blocks.WATER, Blocks.GOLD_BLOCK}) {
            helper.setBlock(wall, block);
            final double density = block == Blocks.STONE ? 2_700 : block == Blocks.WATER ? 1_000 : 19_300;
            close(helper, IonizingRadiation.RAYS.transmission(density) * air * air,
                    Radiation.transmission(level, from, to, IonizingRadiation.RAYS), "through " + block);
        }
        helper.assertTrue(Radiation.transmission(level, from, to, IonizingRadiation.RAYS) < 0.01,
                "a block of gold stops nearly all of it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void rate_fallsWithTheSquareOfTheDistanceAndStopsWithItsSource(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Vec3 centre = Vec3.atCenterOf(helper.absolutePos(SOURCE));
        final Vec3 two = centre.add(2, 0, 0);
        final Vec3 four = centre.add(4, 0, 0);
        final double ambientTwo = Radiation.rateAt(level, two, IonizingRadiation.RAYS);
        final double ambientFour = Radiation.rateAt(level, four, IonizingRadiation.RAYS);
        Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 100);
        try {
            close(helper, 100, Radiation.strength(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS),
                    "what the block gives off");
            final double air = IonizingRadiation.RAYS.transmission(BlockDensity.AIR);
            close(helper, 25 * air, Radiation.rateAt(level, two, IonizingRadiation.RAYS) - ambientTwo,
                    "at two blocks, through one of air");
            close(helper, 6.25 * air * air * air, Radiation.rateAt(level, four, IonizingRadiation.RAYS) - ambientFour,
                    "a quarter of it at four");
        } finally {
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 0);
        }
        close(helper, ambientTwo, Radiation.rateAt(level, two, IonizingRadiation.RAYS), "gone with its source");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void dose_buildsUpFromWhatReachesTheBodyHalvedByShielding(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Player bare = standing(helper, new BlockPos(3, 1, 1));
        final Player shielded = standing(helper, new BlockPos(1, 1, 3));
        shielded.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 3_600);
        try {
            final double reaching = Radiation.rateOn(bare, level, IonizingRadiation.RAYS);
            Hazards.second(bare, level);
            Hazards.second(shielded, level);
            close(helper, IonizingRadiation.DOSE.perSecond(reaching), Hazards.level(bare, IonizingRadiation.DOSE),
                    "a second of what reaches the bare body");
            close(helper, IonizingRadiation.DOSE.perSecond(reaching), Hazards.exposure(bare, IonizingRadiation.DOSE),
                    "what came at it");
            close(helper, Hazards.exposure(shielded, IonizingRadiation.DOSE) / 2,
                    Hazards.level(shielded, IonizingRadiation.DOSE), "half of it through the helmet");
        } finally {
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 0);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void dose_recoversSlowlyUnderTheBackground(final GameTestHelper helper) {
        final Player player = standing(helper, new BlockPos(3, 1, 3));
        Hazards.set(player, IonizingRadiation.DOSE, 1);
        Hazards.second(player, helper.getLevel());
        helper.assertTrue(Hazards.exposure(player, IonizingRadiation.DOSE) > 0, "the sky still reaches it");
        close(helper, 1 - 0.0005, Hazards.level(player, IonizingRadiation.DOSE), "it recovers all the same");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void contamination_dosesTheBodyAndIsKeptOffByASuit(final GameTestHelper helper) {
        final Player player = standing(helper, new BlockPos(3, 1, 3));
        Hazards.set(player, IonizingRadiation.CONTAMINATION, 1_000);
        Hazards.second(player, helper.getLevel());
        final double left = 1_000 * (1 - 0.0005);
        close(helper, left, Hazards.level(player, IonizingRadiation.CONTAMINATION), "it sheds slowly on dry land");
        close(helper, IonizingRadiation.DOSE.perSecond(IonizingRadiation.contaminationRate(left)),
                Hazards.level(player, IonizingRadiation.DOSE), "what is left doses at a hundredth of a millisievert an"
                        + " hour a kilobecquerel");
        helper.assertTrue(IonizingRadiation.contaminationRate(1_000) == 10, "ten for a thousand");
        final Player suited = standing(helper, new BlockPos(1, 1, 3));
        suited.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        close(helper, 0.9, Resistances.total(suited, IonizingRadiation.CONTAMINATION_PROTECTION),
                "the suit keeps nine tenths of the dust off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void contamination_isWashedOffInWater(final GameTestHelper helper) {
        final BlockPos pool = new BlockPos(2, 2, 2);
        helper.setBlock(pool.below(), Blocks.STONE);
        helper.setBlock(pool, Blocks.WATER);
        final Cow cow = helper.spawn(EntityType.COW, pool);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cow.isInWater(), "the cow stands in the water"))
                .thenExecute(() -> {
                    Hazards.set(cow, IonizingRadiation.CONTAMINATION, 1_000);
                    Hazards.second(cow, helper.getLevel());
                    close(helper, 1_000 - 1_000 * 0.0005 * 401, Hazards.level(cow, IonizingRadiation.CONTAMINATION),
                            "a fifth washed off in a second");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = SETTINGS)
    public static void natural_mineralsWithUraniumInThemGiveOffALittle(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Block ore = MaterialUses.oreBlock(TestMaterials.TESTITE, HostRocks.GRANITE).get();
        helper.assertTrue(NaturalRadiation.bears(ore.defaultBlockState()), "testite bears uranium");
        helper.assertFalse(NaturalRadiation.bears(Blocks.GRANITE.defaultBlockState()), "granite does not");
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ore);
                // Set straight, with none of the game's events, so its section is counted again as a mod would ask.
                NaturalRadiation.forget(level, helper.absolutePos(new BlockPos(x, 1, z)));
            }
        }
        final Vec3 above = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 4, 2)));
        final double vein = NaturalRadiation.at(level, above, IonizingRadiation.RAYS);
        helper.assertTrue(vein > 0.001 && vein < 0.1, "a little over the vein, never harmful; " + vein);
        HazardBalance.setNaturalRadioactivity(false);
        try {
            close(helper, 0, NaturalRadiation.at(level, above, IonizingRadiation.RAYS), "none when the world says not");
        } finally {
            HazardBalance.setNaturalRadioactivity(true);
        }
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = BATCH)
    public static void natural_aVeinReadsAlikeWhereverTheSectionsEdgesCutIt(final GameTestHelper helper) {
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        // Whole in one section, then across the edges of four, as an arena at 14 of 16 lays it.
        final double whole = veinReading(helper, Math.floorMod(4 - origin.getX(), SECTION) + 1,
                Math.floorMod(4 - origin.getZ(), SECTION) + 1);
        final double cut = veinReading(helper, Math.floorMod(15 - origin.getX(), SECTION) + SECTION,
                Math.floorMod(15 - origin.getZ(), SECTION) + SECTION);
        helper.assertTrue(whole > 0.001, "the vein whole gives off a little: " + whole);
        helper.assertTrue(Math.abs(cut / whole - 1) < CUT_ALIKE, "cut by the sections' edges, the vein reads near "
                + "the same, its rock not shielding itself: " + cut + " against " + whole);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void cosmic_isStoppedByTheBlocksAbove(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos under = new BlockPos(3, 2, 3);
        final Vec3 at = Vec3.atCenterOf(helper.absolutePos(under));
        final double open = CosmicRadiation.at(level, at);
        helper.assertTrue(open > 0, "the sky gives off a little; " + open);
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(3, y, 3), Blocks.STONE);
        }
        helper.assertTrue(CosmicRadiation.at(level, at) < open * 0.05, "three blocks of stone stop most of it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void withering_comesWhereTheDoseRateIsHigh(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos near = helper.absolutePos(SOURCE.east());
        helper.assertTrue(Withering.contamination(level, near) < 1, "the land is clean");
        Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 1_000);
        try {
            helper.assertTrue(Withering.contamination(level, near) >= 1, "it withers beside a hot source");
        } finally {
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 0);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SETTINGS)
    public static void radiation_turnedOffBuildsUpNothing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Player player = standing(helper, new BlockPos(3, 1, 1));
        Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 3_600);
        HazardBalance.setRadiation(false);
        try {
            Hazards.second(player, level);
            close(helper, 0, Hazards.level(player, IonizingRadiation.DOSE), "no dose");
            helper.assertFalse(IonizingRadiation.DOSE.enabled(), "the dose is off");
        } finally {
            HazardBalance.setRadiation(true);
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 0);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void geigerCounter_readsTheRateOntoItsCarriersMap(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = GameTestPlayers.join(helper, "radiation_map");
        try {
            final Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 1)));
            player.setPos(at.x, at.y, at.z);
            player.getInventory().setItem(0, new ItemStack(HazardDevices.GEIGER_COUNTER.get()));
            helper.assertTrue(HazardMaps.measured(player).contains(IonizingRadiation.DOSE), "it measures radiation");
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 36);
            Hazards.second(player, level);
            HazardMaps.second(player);
            final double shown = IonizingRadiation.DOSE.shownRate(Hazards.exposure(player, IonizingRadiation.DOSE));
            final Optional<HazardMap.Reading> reading = player.getData(CoreAttachments.HAZARD_MAP).reading(
                    level.dimension().location(), IonizingRadiation.DOSE.id(), new ChunkPos(player.blockPosition())
                            .toLong());
            helper.assertTrue(reading.isPresent() && Math.abs(reading.get().value() - shown) < shown * 0.01,
                    "the chunk's reading is the rate here; " + reading + " for " + shown);
        } finally {
            Radiation.emit(level, helper.absolutePos(SOURCE), IonizingRadiation.RAYS, 0);
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    /* The game's mock player, standing on {@code feet}. */
    private static Player standing(final GameTestHelper helper, final BlockPos feet) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(feet));
        player.setPos(at.x, at.y, at.z);
        return player;
    }

    /*
     * The rate a 3 by 3 vein of testite laid at the arena's {@code (x0, 1, z0)} gives off three blocks over its middle;
     * the vein taken away again after.
     */
    private static double veinReading(final GameTestHelper helper, final int x0, final int z0) {
        lay(helper, x0, z0, MaterialUses.oreBlock(TestMaterials.TESTITE, HostRocks.GRANITE).get());
        final double read = NaturalRadiation.at(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(
                new BlockPos(x0 + 1, 4, z0 + 1))), IonizingRadiation.RAYS);
        lay(helper, x0, z0, Blocks.AIR);
        return read;
    }

    /* Lays {@code block} over the arena's 3 by 3 from {@code (x0, 1, z0)}, its sections counted again. */
    private static void lay(final GameTestHelper helper, final int x0, final int z0, final Block block) {
        for (int x = x0; x < x0 + 3; x++) {
            for (int z = z0; z < z0 + 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), block);
                NaturalRadiation.forget(helper.getLevel(), helper.absolutePos(new BlockPos(x, 1, z)));
            }
        }
    }

    /* Within a thousandth of what is expected, however small: a dose a second is a few millionths of a sievert. */
    private static void close(final GameTestHelper helper, final double expected, final double actual,
                              final String what) {
        helper.assertTrue(Math.abs(expected - actual) <= 1.0E-12 + Math.abs(expected) * CLOSE,
                what + ": expected " + expected + ", got " + actual);
    }
}
