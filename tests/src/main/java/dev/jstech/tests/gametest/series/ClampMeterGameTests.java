/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.energy.EnergyReadings;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.energy.MeterClamp;
import dev.jstech.core.jstechseries.energy.MeterReading;
import dev.jstech.core.jstechseries.energy.MeterSessions;
import dev.jstech.core.jstechseries.energy.MeterState;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.testkit.TestCables;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The clamp meter and what it reads: a wire of a line shows what crosses it, which way and its ceiling; a block shows
 * what goes in and out of each face; the meter clamps on either, reads every tick it is held, counts what crossed since
 * it was clamped, and reads nothing with its dial off.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ClampMeterGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos SOURCE = new BlockPos(1, 2, 2);
    private static final BlockPos SINK = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));

    private ClampMeterGameTests() {
    }

    @GameTest(template = ARENA)
    public static void reading_ofAWireSaysWhatCrossesWhichWayAndItsCeiling(final GameTestHelper helper) {
        line(helper);
        helper.succeedWhen(() -> {
            final Optional<EnergyReadings.LineReading> reading = EnergyReadings.line(helper.getLevel(),
                    helper.absolutePos(RUN.get(1)), TestEnergies.LOW_LINE.get());
            helper.assertTrue(reading.isPresent() && reading.get().perTick() > 0, "energy crosses the wire");
            helper.assertTrue(reading.get().toward() == Direction.EAST, "toward the sink, to the east: "
                    + reading.get().toward());
            helper.assertTrue(reading.get().ceiling() == TestEnergies.LINE_RATE && !reading.get().full(),
                    "under its ceiling");
            helper.assertTrue(reading.get().lineClass() == TestEnergies.LOW, "on a low line");
        });
    }

    @GameTest(template = ARENA)
    public static void reading_ofABlockSaysWhatGoesThroughEachFace(final GameTestHelper helper) {
        line(helper);
        helper.succeedWhen(() -> {
            final Optional<EnergyReadings.BlockReading> sink = EnergyReadings.block(helper.getLevel(),
                    helper.absolutePos(SINK));
            helper.assertTrue(sink.isPresent(), "the sink holds energy");
            for (final EnergyReadings.FaceReading face : sink.get().faces()) {
                if (face.face() == Direction.WEST) {
                    helper.assertTrue(face.kind() == EnergyReadings.FaceReading.Kind.IN && face.perTick() > 0,
                            "energy comes in from the west");
                } else {
                    helper.assertTrue(face.kind() == EnergyReadings.FaceReading.Kind.IDLE,
                            "nothing through its other faces: " + face);
                }
            }
            helper.assertTrue(sink.get().net() > 0, "it fills");
        });
    }

    @GameTest(template = ARENA)
    public static void meter_clampsOnALineAndCountsWhatCrossedSince(final GameTestHelper helper) {
        line(helper);
        final FakePlayer player = player(helper, "line");
        final ItemStack meter = new ItemStack(ClampMeter.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        use(helper, player, RUN.get(1));
        final MeterClamp clamp = meter.get(ClampMeter.CLAMP.get());
        helper.assertTrue(clamp != null && clamp.onLine() && clamp.cable().orElseThrow()
                .equals(TestEnergies.LOW_LINE.getId()), "it clamps on the low wire");
        helper.onEachTick(() -> MeterSessions.tick(player, meter));

        helper.succeedWhen(() -> {
            final Optional<MeterReading> read = MeterSessions.lastReading(player.getUUID());
            helper.assertTrue(read.isPresent() && read.get().present() && read.get().line(), "it reads the line");
            helper.assertTrue(read.get().perTick() > 0 && read.get().total() > read.get().perTick(),
                    "and counts what crossed since it was clamped: " + read.get().total());
            helper.assertTrue(read.get().most() >= read.get().least(), "with the most and the least");
        });
    }

    @GameTest(template = ARENA)
    public static void meter_clampsOnABlockAndReadsItsFaces(final GameTestHelper helper) {
        line(helper);
        final FakePlayer player = player(helper, "block");
        final ItemStack meter = new ItemStack(ClampMeter.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        use(helper, player, SINK);
        final MeterClamp clamp = meter.get(ClampMeter.CLAMP.get());
        helper.assertTrue(clamp != null && !clamp.onLine(), "it clamps on the block");
        helper.onEachTick(() -> MeterSessions.tick(player, meter));

        helper.succeedWhen(() -> {
            final Optional<MeterReading> read = MeterSessions.lastReading(player.getUUID());
            helper.assertTrue(read.isPresent() && read.get().present() && !read.get().line()
                    && read.get().faces().size() == Direction.values().length, "it reads every face");
            helper.assertTrue(read.get().stored() > 0, "and what the block holds");
        });
    }

    @GameTest(template = ARENA)
    public static void meter_readsNothingWithItsDialOff(final GameTestHelper helper) {
        line(helper);
        final FakePlayer player = player(helper, "off");
        final ItemStack meter = new ItemStack(ClampMeter.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        use(helper, player, SINK);
        meter.set(ClampMeter.STATE.get(), new MeterState(MeterState.Dial.OFF, false, false, true, false));
        MeterSessions.forget(player.getUUID());
        MeterSessions.tick(player, meter);
        helper.assertTrue(MeterSessions.lastReading(player.getUUID()).isEmpty(), "a meter switched off reads nothing");
        helper.assertTrue(new MeterState(MeterState.Dial.OFF, false, false, true, false).turned().dial()
                == MeterState.Dial.OWN && MeterState.DEFAULT.turned().turned().dial() == MeterState.Dial.OFF,
                "its dial turns off, own unit, FE, and back");
        helper.succeed();
    }

    /* A low source, a low run and a low sink, the source full. */
    private static void line(final GameTestHelper helper) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(SOURCE));
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(SINK));
        helper.setBlock(SOURCE, Blocks.CUT_COPPER);
        helper.setBlock(SINK, Blocks.EXPOSED_CUT_COPPER);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestEnergies.LOW_LINE);
        }
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SOURCE)).setStored(500_000L);
    }

    /* A player of the test's own, so the tests running together never share one meter's reading. */
    private static FakePlayer player(final GameTestHelper helper, final String which) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                ("clamp_meter_" + which).getBytes(StandardCharsets.UTF_8)), "meter_" + which));
    }

    /* The player uses what they hold on the block at {@code relative}, from above. */
    private static void use(final GameTestHelper helper, final FakePlayer player, final BlockPos relative) {
        final BlockPos at = helper.absolutePos(relative);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
