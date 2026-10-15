/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.energy.EnergyLoss;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.overhead.HungWire;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.overhead.WireEnd;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Overhead lines: a wire hung between two connectors carries their energy, losing as much as its length; a relay passes
 * a line across two spans, and a feedthrough through a wall; a wire too long, one something stands in the way of, one
 * hung twice and one there is not wire enough for are refused; an insulator that goes lets its wire fall; the line's
 * item hangs a wire from the first insulator it is used on to the next; a live wire shocks whoever touches it, a dead
 * one nobody; and lightning on the line burns its machines, unless an arrester on it takes the surge to earth.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OverheadLinesGameTests {

    private static final String ARENA = "empty";
    private static final String WIDE = "bench";
    private static final BlockPos SOURCE = new BlockPos(1, 2, 4);
    private static final BlockPos SINK = new BlockPos(7, 2, 4);
    private static final int COIL = 64;

    private OverheadLinesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void hung_carriesEnergyLosingAsMuchAsItsLength(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        final OverheadLines.Outcome outcome = hang(helper, SOURCE, 0, SINK, 0, COIL);
        helper.assertTrue(outcome.hung(), "the wire is hung: " + outcome.said());
        final HungWire wire = OverheadLines.WIRES.get(helper.getLevel()).wires().stream()
                .filter(each -> each.touches(helper.absolutePos(SOURCE))).findFirst().orElseThrow();
        helper.assertTrue(wire.blocks() == 6, "six blocks of wire: " + wire.length());
        // Its six blocks each lose the low class's thousandth.
        final long sent = EnergyLoss.toSend(TestEnergyEnds.TAKES, 6 * TestEnergies.LOW_LOSS);
        helper.succeedWhen(() -> {
            final LineSurvey survey = survey(helper, wire).orElse(null);
            helper.assertTrue(survey != null && survey.moved() == sent, "what the source sent: " + survey);
            helper.assertTrue(survey.lost() == sent - TestEnergyEnds.TAKES, "what the wire lost: " + survey.lost());
            helper.assertTrue(TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SINK)).stored() > 0,
                    "the energy arrives");
        });
    }

    @GameTest(template = ARENA)
    public static void relay_passesTheLineAcrossTwoSpans(final GameTestHelper helper) {
        final BlockPos pole = new BlockPos(4, 2, 7);
        ends(helper, SOURCE, SINK);
        helper.setBlock(pole, Blocks.END_ROD);
        helper.assertTrue(hang(helper, SOURCE, 0, pole, 0, COIL).hung() && hang(helper, pole, 0, SINK, 0, COIL).hung(),
                "both spans are hung");
        helper.succeedWhen(() -> helper.assertTrue(TestEnergyEnds.storeAt(helper.getLevel(),
                helper.absolutePos(SINK)).stored() > 0, "the energy crosses the pole"));
    }

    @GameTest(template = ARENA)
    public static void feedthrough_takesTheWireThroughAWall(final GameTestHelper helper) {
        final BlockPos source = new BlockPos(4, 2, 1);
        final BlockPos wall = new BlockPos(4, 2, 4);
        final BlockPos sink = new BlockPos(4, 2, 7);
        ends(helper, source, sink);
        helper.setBlock(wall, Blocks.MUD_BRICKS);
        helper.assertTrue(hang(helper, source, 0, wall, 0, COIL).hung() && hang(helper, wall, 1, sink, 0, COIL).hung(),
                "a wire is hung to each face of the wall");
        helper.succeedWhen(() -> helper.assertTrue(TestEnergyEnds.storeAt(helper.getLevel(),
                helper.absolutePos(sink)).stored() > 0, "the energy goes through the wall"));
    }

    @GameTest(template = WIDE)
    public static void hang_refusesWhatCannotBeHung(final GameTestHelper helper) {
        final BlockPos near = new BlockPos(2, 2, 2);
        final BlockPos far = new BlockPos(20, 2, 2);
        ends(helper, near, far);
        helper.assertTrue(!hang(helper, near, 0, far, 0, COIL).hung(), "a wire past the line's longest span");
        final BlockPos left = new BlockPos(2, 2, 10);
        final BlockPos right = new BlockPos(10, 2, 10);
        ends(helper, left, right);
        helper.setBlock(new BlockPos(6, 2, 10), Blocks.STONE);
        helper.assertTrue(!hang(helper, left, 0, right, 0, COIL).hung(), "a wire through the stone it sags into");
        helper.setBlock(new BlockPos(6, 2, 10), Blocks.AIR);
        helper.assertTrue(!hang(helper, left, 0, right, 0, 3).hung(), "a wire there is not wire enough for");
        helper.assertTrue(hang(helper, left, 0, right, 0, COIL).hung(), "and the same wire with wire enough");
        helper.assertTrue(!hang(helper, right, 0, left, 0, COIL).hung(), "but not twice");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void brokenInsulator_letsItsWireFall(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        helper.assertTrue(hang(helper, SOURCE, 0, SINK, 0, COIL).hung(), "the wire is hung");
        helper.setBlock(SINK, Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(OverheadLines.WIRES.get(helper.getLevel()).at(helper.absolutePos(SOURCE)).isEmpty(),
                    "the wire falls");
            helper.assertItemEntityPresent(Items.STRING, SINK, 2.0);
        });
    }

    @GameTest(template = ARENA)
    public static void item_hangsFromTheFirstInsulatorToTheNext(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "overhead_item".getBytes(StandardCharsets.UTF_8)), "overhead_item"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STRING, COIL));
        use(helper, player, SOURCE);
        helper.assertTrue(player.getMainHandItem().get(OverheadLines.START_AT.get()) != null,
                "the first insulator is where the wire is hung from");
        use(helper, player, SINK);
        helper.assertTrue(OverheadLines.WIRES.get(helper.getLevel()).joins(new WireEnd(helper.absolutePos(SOURCE), 0),
                new WireEnd(helper.absolutePos(SINK), 0)), "the next one is where it is hung to");
        helper.assertTrue(player.getMainHandItem().getCount() == COIL - 6
                && player.getMainHandItem().get(OverheadLines.START_AT.get()) == null,
                "six blocks of wire are taken, and the hand is free to hang the next");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void liveWire_shocksWhoeverTouchesIt(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        helper.assertTrue(hang(helper, SOURCE, 0, SINK, 0, COIL).hung(), "the wire is hung");
        final Pig pig = beneathTheWire(helper);
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(),
                "a pig whose head touches the live wire is shocked"));
    }

    @GameTest(template = ARENA)
    public static void deadWire_shocksNobody(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SOURCE)).setStored(0L);
        helper.assertTrue(hang(helper, SOURCE, 0, SINK, 0, COIL).hung(), "the wire is hung");
        final Pig pig = beneathTheWire(helper);
        helper.runAfterDelay(4 * OverheadLines.TOUCH_EVERY, () -> {
            helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "a wire with nothing to give hurts nobody");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void lightning_burnsTheMachinesOnTheLine(final GameTestHelper helper) {
        ends(helper, SOURCE, SINK);
        helper.assertTrue(hang(helper, SOURCE, 0, SINK, 0, COIL).hung(), "the wire is hung");
        helper.runAfterDelay(2, () -> {
            final LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
            helper.assertTrue(bolt != null, "a bolt of lightning");
            bolt.moveTo(helper.absoluteVec(new Vec3(4.5, 3.0, 4.5)));
            helper.getLevel().addFreshEntity(bolt);
            helper.assertTrue(TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SINK)).damaged(),
                    "the surge burns the sink");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void arrester_takesTheSurgeToEarth(final GameTestHelper helper) {
        final BlockPos arrester = new BlockPos(4, 2, 1);
        ends(helper, SOURCE, SINK);
        helper.setBlock(arrester, Blocks.IRON_BLOCK);
        helper.assertTrue(hang(helper, SOURCE, 0, SINK, 0, COIL).hung()
                && hang(helper, SOURCE, 0, arrester, 0, COIL).hung(), "the line runs to the sink and the arrester");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!OverheadLines.struck(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 4.5))),
                    "the surge goes to earth");
            helper.assertTrue(!TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(SINK)).damaged(),
                    "and the sink is spared");
            helper.succeed();
        });
    }

    /* A pig under the middle of the wire from the source to the sink, its head against the wire's sag. */
    private static Pig beneathTheWire(final GameTestHelper helper) {
        final Pig pig = helper.spawn(EntityType.PIG, new Vec3(4.5, 2.0, 4.5));
        pig.setNoAi(true);
        return pig;
    }

    /* A source of the tests' energy, full, and a sink, each with a connector on top. */
    private static void ends(final GameTestHelper helper, final BlockPos source, final BlockPos sink) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(source));
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(sink));
        helper.setBlock(source, Blocks.CUT_COPPER);
        helper.setBlock(sink, Blocks.EXPOSED_CUT_COPPER);
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(source)).setStored(100_000L);
    }

    private static OverheadLines.Outcome hang(final GameTestHelper helper, final BlockPos from, final int fromAnchor,
                                              final BlockPos to, final int toAnchor, final int coil) {
        return OverheadLines.hang(null, helper.getLevel(), new WireEnd(helper.absolutePos(from), fromAnchor),
                new WireEnd(helper.absolutePos(to), toAnchor), TestEnergies.HUNG_LINE.get(),
                new ItemStack(Items.STRING, coil));
    }

    private static void use(final GameTestHelper helper, final FakePlayer player, final BlockPos at) {
        final BlockPos pos = helper.absolutePos(at);
        OverheadLines.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)), TestEnergies.HUNG_LINE.get());
    }

    /* What the part of the line the hung wire is in did in the last tick. */
    private static Optional<LineSurvey> survey(final GameTestHelper helper, final HungWire wire) {
        final OptionalLong number = CoreGrids.places(helper.getLevel()).find(wire.place());
        return number.isEmpty() ? Optional.empty() : CoreGrids.mover(helper.getLevel(),
                TestEnergies.HUNG_LINE.get().grid()).flatMap(mover -> mover.survey(number.getAsLong()));
    }
}
