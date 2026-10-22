/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.LineBreach;
import dev.jstech.core.explosion.BlastMath;
import dev.jstech.core.explosion.Detonation;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.matter.PressureBursts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestMatterEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Pressure that bursts: a tank forced past what it stands bursts, its block gone, with a blast of the energy the gas
 * held; its relief valve lets a little out before then and shuts again, though a valve too small for what is forced in
 * does not save it; only a tank of what is held under pressure bursts; and a gas line that holds less than the store
 * feeding it carries none of it and bursts, while one that holds enough carries it all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PressureBurstGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos VESSEL = new BlockPos(3, 2, 3);
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    /* A small vessel, so its blast stays small: ten millibuckets holding two hundred atmospheres. */
    private static final long VOLUME = 10L;
    private static final long HOLDS = 200L;
    private static final long BURSTS = 300L;
    private static final long RELIEF = 240L;
    private static final long RELIEF_RATE = 50L;
    private static final long COMPRESSOR = 400L;
    private static final double CUBIC_METRES = VOLUME / 1_000.0;
    /* What the test mod's tanks hold at the pressure a thin gas line bursts at, and at one it holds. */
    private static final long BURSTING_FILL = 64_000L;
    private static final long HELD_FILL = 9_000L;
    private static final double CLOSE = 0.5;

    private PressureBurstGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tank_pushedPastWhatItStandsBurstsWithTheForceOfWhatItHeld(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        helper.setBlock(VESSEL, Blocks.IRON_BLOCK);
        final MatterTank tank = new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).bursts(BURSTS);
        tank.insert(new MatterStack(hydrogen, VOLUME * HOLDS), false);
        helper.assertTrue(tank.insert(new MatterStack(hydrogen, 1L), true) == 0L, "full, it takes no more itself");
        helper.assertTrue(PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank)
                == PressureBursts.Held.HELD, "at the pressure it holds, it holds");
        helper.assertTrue(tank.push(new MatterStack(hydrogen, 1_100L), COMPRESSOR, false) == 1_100L
                && tank.pressureNow() == 310, "a compressor pushes it past what it stands: " + tank.pressureNow());
        final double joules = tank.burstEnergy();
        helper.assertTrue(Math.abs(joules - BlastMath.burstEnergy(310, CUBIC_METRES)) < 1,
                "the gas holds the energy of three hundred and nine atmospheres above the air: " + joules);

        helper.assertTrue(PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank)
                == PressureBursts.Held.BURST, "past what it stands, it bursts");
        helper.assertTrue(helper.getBlockState(VESSEL).isAir() && tank.held().isEmpty(),
                "its block is gone and what it held let out");
        final Optional<Detonation> blast = blastAt(helper, VESSEL, BuiltInRegistries.BLOCK.getKey(Blocks.IRON_BLOCK));
        helper.assertTrue(blast.isPresent() && Math.abs(blast.get().tnt() - BlastMath.tnt(joules)) < 1.0e-9,
                "a blast of that energy goes off where it stood: " + blast.map(Detonation::tnt));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void relief_letsALittleOutBeforeItBurstsAndShutsAgain(final GameTestHelper helper) {
        helper.setBlock(VESSEL, Blocks.IRON_BLOCK);
        final MatterTank tank = new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).relief(RELIEF,
                RELIEF_RATE).bursts(BURSTS);
        tank.push(new MatterStack(Elements.of(Element.HYDROGEN), 2_600L), COMPRESSOR, false);
        helper.assertTrue(PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank)
                        == PressureBursts.Held.RELIEVED && tank.pressureNow() == 255,
                "past its setting, the valve lets a tick's worth out: " + tank.pressureNow());
        for (int tick = 0; tick < 3; tick++) {
            PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank);
        }
        helper.assertTrue(PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank)
                        == PressureBursts.Held.HELD && tank.pressureNow() == RELIEF,
                "and shuts once the tank is back at its setting: " + tank.pressureNow());
        helper.assertTrue(helper.getBlockState(VESSEL).is(Blocks.IRON_BLOCK), "the tank whole");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void relief_tooSmallForWhatIsForcedInStillBursts(final GameTestHelper helper) {
        helper.setBlock(VESSEL, Blocks.IRON_BLOCK);
        final MatterTank tank = new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).bursts(BURSTS).relief(
                RELIEF, RELIEF_RATE);
        tank.push(new MatterStack(Elements.of(Element.HYDROGEN), 3_200L), COMPRESSOR, false);
        helper.assertTrue(PressureBursts.hold(helper.getLevel(), helper.absolutePos(VESSEL), tank)
                == PressureBursts.Held.BURST, "a tick's worth let out leaves it past what it stands, and it bursts");
        helper.assertTrue(helper.getBlockState(VESSEL).isAir(), "its block is gone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tank_burstsOnlyWhenItHoldsWhatIsUnderPressure(final GameTestHelper helper) {
        helper.assertTrue(refused(() -> new MatterTank(MatterState.SLURRY, VOLUME, HOLDS, () -> { }).bursts(BURSTS)),
                "a tank of slurry holds nothing under pressure, and never bursts");
        helper.assertTrue(refused(() -> new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).bursts(HOLDS)),
                "a tank bursts past the pressure it holds, not at it");
        helper.assertTrue(refused(() -> new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).bursts(BURSTS)
                .relief(BURSTS, RELIEF_RATE)), "a relief valve opens before the tank bursts");
        helper.assertTrue(new MatterTank(MatterState.GAS, VOLUME, HOLDS, () -> { }).burstsAt().isEmpty(),
                "and a tank that says nothing never bursts");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gasLine_holdingLessThanItsStoreCarriesNoneAndBursts(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        lay(helper);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(hydrogen, BURSTING_FILL), false);
        helper.succeedWhen(() -> {
            for (final BlockPos at : RUN) {
                helper.assertFalse(helper.getBlockState(at).is(CoreCables.BLOCK.get()),
                        "a gas at sixty-four atmospheres bursts a line that holds ten");
            }
            helper.assertTrue(TestMatterEnds.amountAt(helper.getLevel(), helper.absolutePos(INPUT), MatterState.GAS,
                    hydrogen) == 0L, "and none of it got through");
        });
    }

    @GameTest(template = ARENA)
    public static void gasLine_holdingEnoughCarriesItAll(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        lay(helper);
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(hydrogen, HELD_FILL), false);
        helper.succeedWhen(() -> {
            helper.assertTrue(TestMatterEnds.amountAt(helper.getLevel(), helper.absolutePos(INPUT), MatterState.GAS,
                    hydrogen) == HELD_FILL, "a gas at nine atmospheres goes all through a line that holds ten");
            helper.assertTrue(helper.getBlockState(RUN.get(1)).is(CoreCables.BLOCK.get()), "which stays whole");
        });
    }

    @GameTest(template = ARENA)
    public static void pipe_burstsWithTheForceOfWhatABlockOfItHeld(final GameTestHelper helper) {
        final BlockPos at = helper.absolutePos(VESSEL);
        PressureBursts.pipe(TestCableTypes.THIN_GAS_INSIDE).broken(helper.getLevel(), new LineBreach(at,
                TestCableTypes.THIN_GAS_LINE.get(), LineBreach.Reason.OVERPRESSURE, "jscore:hydrogen", 64));
        final double joules = BlastMath.burstEnergy(64, TestCableTypes.THIN_GAS_INSIDE / 1_000.0);
        final Optional<Detonation> blast = blastAt(helper, VESSEL, TestCableTypes.THIN_GAS_LINE.getId());
        helper.assertTrue(blast.isPresent() && Math.abs(blast.get().tnt() - BlastMath.tnt(joules)) < 1.0e-9,
                "the gas a block of the pipe held goes off with its energy: " + blast.map(Detonation::tnt));
        helper.succeed();
    }

    /* The output and the input of a thin gas line three blocks long, their tanks empty. */
    private static void lay(final GameTestHelper helper) {
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        helper.setBlock(OUTPUT, Blocks.CHISELED_COPPER);
        helper.setBlock(INPUT, Blocks.WAXED_CHISELED_COPPER);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.THIN_GAS_LINE);
        }
    }

    /* The blast of {@code explosive} going off at the middle of {@code at}, if one is. */
    private static Optional<Detonation> blastAt(final GameTestHelper helper, final BlockPos at,
                                                final ResourceLocation explosive) {
        final Vec3 centre = Vec3.atCenterOf(helper.absolutePos(at));
        return Explosions.running(helper.getLevel()).stream().filter(blast -> blast.explosive().equals(explosive)
                && blast.centre().distanceTo(centre) < CLOSE).findFirst();
    }

    /* Whether making what {@code made} makes is refused. */
    private static boolean refused(final Runnable made) {
        try {
            made.run();
            return false;
        } catch (final IllegalArgumentException | IllegalStateException refused) {
            return true;
        }
    }
}
