/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.probe.SignalSessions;
import dev.jstech.core.signal.Band;
import dev.jstech.core.signal.Frequency;
import dev.jstech.core.signal.Heard;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.Reception;
import dev.jstech.core.signal.SignalRadio;
import dev.jstech.core.signal.Signals;
import dev.jstech.core.signal.Tuning;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestRadios;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Redstone and signals: a receiver gives out what its transmitter is fed, on its owner's frequency only, the team's and
 * the private ones being two; two transmitters on one frequency clash, and their receivers give nothing until one is
 * taken off; a transmitter is heard as far as its power, less for every wall, and a relay of its team carries it
 * further, and to its partner far away; a receiver's neighbours are told only of a change, however often the air is
 * looked over; a sensor speaks its chosen reading in redstone and to a comparator; and the air tells a probe what is
 * heard where it is, whose, and the team's frequencies that are lost there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SignalsGameTests {

    private static final String ARENA = "empty";
    private static final String WIDE = "bench";
    /* How long a test waits on the air: a recheck of every frequency and a little more. */
    private static final int WAIT = SignalRadio.RECHECK_EVERY + 40;

    private SignalsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void receiver_givesWhatItsTransmitterIsFed(final GameTestHelper helper) {
        final UUID owner = owner("fed");
        final BlockPos lamp = new BlockPos(7, 2, 5);
        radio(helper, new BlockPos(1, 2, 4), TestRadios.TRANSMITTER.get(), owner, Tuning.on(Band.PRIVATE, 5));
        final RadioBlockEntity receiver = radio(helper, new BlockPos(7, 2, 4), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 5));
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.setBlock(new BlockPos(0, 2, 4), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(receiver.signal() == Reading.FULL, "the receiver gives what the transmitter is fed: "
                    + receiver.signal());
            helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
        });
    }

    @GameTest(template = ARENA)
    public static void receiver_hearsNoOtherOwnersPrivateFrequency(final GameTestHelper helper) {
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(1, 2, 4), TestRadios.TRANSMITTER.get(),
                owner("mine"), Tuning.on(Band.PRIVATE, 7));
        final RadioBlockEntity receiver = radio(helper, new BlockPos(7, 2, 4), TestRadios.RECEIVER.get(),
                owner("theirs"), Tuning.on(Band.PRIVATE, 7));
        sendSoon(helper, transmitter, Reading.FULL);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(receiver.signal() == 0, "another owner's private frequency is not heard");
            helper.assertTrue(!SignalRadio.reception(helper.getLevel(), receiver.getBlockPos()).heard(),
                    "nothing is heard on it");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void teamAndPrivate_areTwoFrequencies(final GameTestHelper helper) {
        final UUID owner = owner("bands");
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(1, 2, 4), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.TEAM, 9));
        final RadioBlockEntity team = radio(helper, new BlockPos(7, 2, 3), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.TEAM, 9));
        final RadioBlockEntity own = radio(helper, new BlockPos(7, 2, 5), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 9));
        sendSoon(helper, transmitter, 11);
        helper.succeedWhen(() -> {
            helper.assertTrue(team.signal() == 11, "the team's frequency 9 is heard: " + team.signal());
            helper.assertTrue(own.signal() == 0, "the owner's own frequency 9 is another one");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void clash_silencesTheReceiversUntilOneIsTakenOff(final GameTestHelper helper) {
        final UUID owner = owner("clash");
        final BlockPos second = new BlockPos(1, 2, 6);
        final RadioBlockEntity first = radio(helper, new BlockPos(1, 2, 2), TestRadios.TRANSMITTER.get(), owner,
                Tuning.on(Band.PRIVATE, 3));
        final RadioBlockEntity other = radio(helper, second, TestRadios.TRANSMITTER.get(), owner,
                Tuning.on(Band.PRIVATE, 3));
        final RadioBlockEntity receiver = radio(helper, new BlockPos(7, 2, 4), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 3));
        sendSoon(helper, first, Reading.FULL);
        sendSoon(helper, other, 4);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(first.clashing() && other.clashing(), "both transmitters show the clash");
            helper.assertTrue(receiver.signal() == 0, "the receiver gives nothing in a clash");
            helper.assertTrue(SignalRadio.reception(helper.getLevel(), receiver.getBlockPos()).equals(
                    Reception.CLASH), "and hears the clash");
            helper.setBlock(second, Blocks.AIR);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!first.clashing(), "the one left clashes no more");
                helper.assertTrue(receiver.signal() == Reading.FULL, "the receiver gives its value: "
                        + receiver.signal());
                helper.succeed();
            });
        });
    }

    @GameTest(template = WIDE)
    public static void reach_endsWhereThePowerEnds(final GameTestHelper helper) {
        final UUID owner = owner("reach");
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(2, 2, 10), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 1));
        final RadioBlockEntity near = radio(helper, new BlockPos(32, 2, 10), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 1));
        final RadioBlockEntity far = radio(helper, new BlockPos(40, 2, 10), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 1));
        sendSoon(helper, transmitter, 9);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(near.signal() == 9, "30 blocks off, within its power, the value arrives whole: "
                    + near.signal());
            final Reception edge = SignalRadio.reception(helper.getLevel(), near.getBlockPos());
            helper.assertTrue(edge.bars() == 1, "near the edge of its reach, with one bar: " + edge.bars());
            helper.assertTrue(far.signal() == 0, "38 blocks off, beyond its power, nothing arrives");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE)
    public static void walls_shortenTheReach(final GameTestHelper helper) {
        final UUID owner = owner("walls");
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(2, 2, 10), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 2));
        final RadioBlockEntity open = radio(helper, new BlockPos(22, 2, 10), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 2));
        final RadioBlockEntity walled = radio(helper, new BlockPos(2, 2, 30), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 2));
        // Four blocks of stone take sixteen blocks of its thirty-two: twenty blocks off is out of reach behind them.
        for (int z = 14; z < 18; z++) {
            helper.setBlock(new BlockPos(2, 2, z), Blocks.STONE);
        }
        sendSoon(helper, transmitter, 6);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(open.signal() == 6, "twenty blocks off in open air, it is heard");
            helper.assertTrue(walled.signal() == 0, "twenty blocks off behind four of stone, it is not");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE)
    public static void relay_ofTheTeamCarriesItFurther(final GameTestHelper helper) {
        final UUID owner = owner("relay");
        final UUID stranger = owner("relay-stranger");
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(2, 2, 4), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 4));
        radio(helper, new BlockPos(30, 2, 4), TestRadios.RELAY.get(), owner, Tuning.DEFAULT);
        final RadioBlockEntity beyond = radio(helper, new BlockPos(46, 2, 4), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 4));
        final RadioBlockEntity otherTransmitter = radio(helper, new BlockPos(2, 2, 40), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 5));
        radio(helper, new BlockPos(30, 2, 40), TestRadios.RELAY.get(), stranger, Tuning.DEFAULT);
        final RadioBlockEntity unrelayed = radio(helper, new BlockPos(46, 2, 40), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 5));
        sendSoon(helper, transmitter, 13);
        sendSoon(helper, otherTransmitter, 13);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(beyond.signal() == 13, "44 blocks off, through its team's relay, it is heard: "
                    + beyond.signal());
            helper.assertTrue(unrelayed.signal() == 0, "a stranger's relay carries nothing of it");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE)
    public static void relay_sendsAgainAtItsPartnerFarAway(final GameTestHelper helper) {
        final UUID owner = owner("partner");
        final BlockPos far = new BlockPos(42, 2, 42);
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(2, 2, 2), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 8));
        final RadioBlockEntity near = radio(helper, new BlockPos(4, 2, 2), TestRadios.RELAY.get(), owner,
                Tuning.DEFAULT);
        radio(helper, far, TestRadios.RELAY.get(), owner, Tuning.DEFAULT);
        final RadioBlockEntity receiver = radio(helper, new BlockPos(45, 2, 45), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 8));
        // The partner could as well be in another dimension: what the near relay hears, the far one sends again.
        near.link(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(far)));
        sendSoon(helper, transmitter, 10);
        helper.succeedWhen(() -> helper.assertTrue(receiver.signal() == 10,
                "the far relay sends again what the near one hears: " + receiver.signal()));
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT + 100)
    public static void receiver_tellsItsNeighboursOnlyOfAChange(final GameTestHelper helper) {
        final UUID owner = owner("storm");
        final BlockPos counter = new BlockPos(7, 2, 5);
        final RadioBlockEntity transmitter = radio(helper, new BlockPos(1, 2, 4), TestRadios.TRANSMITTER.get(),
                owner, Tuning.on(Band.PRIVATE, 6));
        final RadioBlockEntity receiver = radio(helper, new BlockPos(7, 2, 4), TestRadios.RECEIVER.get(), owner,
                Tuning.on(Band.PRIVATE, 6));
        helper.setBlock(counter, TestRadios.COUNTER.get());
        final BlockPos at = helper.absolutePos(counter);
        sendSoon(helper, transmitter, 7);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(receiver.signal() == 7, "the value arrives: " + receiver.signal());
            final int before = TestRadios.updatesAt(at);
            // However often the air is looked over, a reception that did not change tells nobody.
            helper.runAfterDelay(WAIT, () -> {
                helper.assertTrue(TestRadios.updatesAt(at) == before, "no update while nothing changes: "
                        + (TestRadios.updatesAt(at) - before));
                transmitter.send(3);
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(receiver.signal() == 3, "the new value arrives");
                    helper.assertTrue(TestRadios.updatesAt(at) == before + 1, "one update for one change: "
                            + (TestRadios.updatesAt(at) - before));
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void sensor_speaksItsChosenReadingInRedstone(final GameTestHelper helper) {
        final BlockPos sensorAt = new BlockPos(4, 2, 4);
        final BlockPos comparatorAt = new BlockPos(4, 2, 5);
        final BlockPos counter = new BlockPos(3, 2, 4);
        helper.setBlock(sensorAt, TestRadios.SENSOR.get());
        helper.setBlock(counter, TestRadios.COUNTER.get());
        helper.setBlock(comparatorAt, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,
                Direction.NORTH));
        final TestRadios.LightSensorBlockEntity sensor = (TestRadios.LightSensorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(sensorAt));
        helper.runAfterDelay(25, () -> {
            final List<Reading> readings = sensor.readings();
            helper.assertTrue(readings.size() == 2, "it reads the sky's light and its height");
            helper.assertTrue(sensor.signal() == readings.get(0).strength(), "it speaks its first reading: "
                    + sensor.signal() + " for " + readings.get(0));
            helper.assertTrue(Signals.strengthAt(helper.getLevel(), helper.absolutePos(sensorAt), null)
                    == readings.get(0).strength(), "its output says the same to anything that reads sensors");
            final ComparatorBlockEntity comparator = (ComparatorBlockEntity) helper.getLevel().getBlockEntity(
                    helper.absolutePos(comparatorAt));
            helper.assertTrue(comparator.getOutputSignal() == sensor.signal(), "a comparator reads it: "
                    + comparator.getOutputSignal());
            final int before = TestRadios.updatesAt(helper.absolutePos(counter));
            sensor.choose(1);
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(sensor.signal() == readings.get(1).strength(), "chosen, it speaks its height: "
                        + sensor.signal());
                final int changes = readings.get(0).strength() == readings.get(1).strength() ? 0 : 1;
                helper.assertTrue(TestRadios.updatesAt(helper.absolutePos(counter)) - before == changes,
                        "its neighbours are told once, of the change only");
                helper.succeed();
            });
        });
    }

    @GameTest(template = WIDE)
    public static void air_tellsAProbeWhatIsHeardAndWhatIsLost(final GameTestHelper helper) {
        final UUID owner = owner("probe");
        final RadioBlockEntity heard = radio(helper, new BlockPos(2, 2, 2), TestRadios.TRANSMITTER.get(), owner,
                Tuning.on(Band.TEAM, 77));
        final RadioBlockEntity lost = radio(helper, new BlockPos(46, 2, 46), TestRadios.TRANSMITTER.get(), owner,
                Tuning.on(Band.TEAM, 78));
        final BlockPos standing = helper.absolutePos(new BlockPos(6, 2, 2));
        sendSoon(helper, heard, 12);
        sendSoon(helper, lost, 5);
        helper.runAfterDelay(12, () -> {
            final List<Heard> rows = SignalRadio.heardAt(helper.getLevel(), standing, owner).stream()
                    .filter(row -> owner.equals(row.owner())).toList();
            final Heard near = rows.stream().filter(row -> row.frequency().number() == 77).findFirst().orElse(null);
            final Heard far = rows.stream().filter(row -> row.frequency().number() == 78).findFirst().orElse(null);
            helper.assertTrue(near != null && near.bars() > 0 && near.value() == 12 && !near.clash(),
                    "the near frequency is heard, with its value: " + near);
            helper.assertTrue(near.from().pos().equals(heard.getBlockPos()), "from its transmitter");
            helper.assertTrue(far != null && far.bars() == 0, "the team's far frequency is listed as lost: " + far);
            helper.assertTrue(SignalRadio.heardAt(helper.getLevel(), standing, owner("probe-stranger")).stream()
                    .noneMatch(row -> row.frequency().number() == 78 && owner.equals(row.owner())),
                    "a stranger is not told of another team's lost frequencies");
            final Frequency team = new Frequency(Band.TEAM, CoreTeams.teamOf(helper.getLevel().getServer(), owner),
                    77);
            helper.assertTrue(SignalRadio.frequencyAt(helper.getLevel(), heard.getBlockPos()).equals(Optional.of(
                    team)), "the transmitter is on its owner's team's frequency 77");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void signalProbe_linksToABlockAndReadsItsRedstone(final GameTestHelper helper) {
        final BlockPos wire = new BlockPos(4, 2, 4);
        final BlockPos at = helper.absolutePos(wire);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.REDSTONE_BLOCK);
        helper.setBlock(wire, Blocks.REDSTONE_WIRE);
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final ItemStack probe = new ItemStack(SignalProbe.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        probe.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(at), Direction.UP, at, false)));
        final GlobalPos link = GlobalPos.of(helper.getLevel().dimension(), at);
        helper.assertTrue(link.equals(probe.get(SignalProbe.LINK.get())), "the probe is linked to the wire");
        helper.succeedWhen(() -> helper.assertTrue(SignalSessions.strengthAt(helper.getLevel().getServer(), link)
                .equals(OptionalInt.of(Reading.FULL)), "it reads the wire's strength"));
    }

    @GameTest(template = ARENA)
    public static void tuning_survivesSaving(final GameTestHelper helper) {
        final UUID pair = owner("pair");
        for (final Tuning tuning : List.of(Tuning.DEFAULT, Tuning.on(Band.TEAM, 42), Tuning.paired(pair))) {
            final Tag saved = Tuning.CODEC.encodeStart(NbtOps.INSTANCE, tuning).getOrThrow();
            helper.assertTrue(Tuning.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().equals(tuning),
                    "a tuning is read back as saved: " + tuning);
        }
        helper.succeed();
    }

    /*
     * Has {@code transmitter} send {@code value} once it has read what it is fed on being placed, which is nothing: a
     * transmitter sends what it is fed, and its kit lets a block send what it works out.
     */
    private static void sendSoon(final GameTestHelper helper, final RadioBlockEntity transmitter, final int value) {
        helper.runAfterDelay(2, () -> transmitter.send(value));
    }

    /* A radio of {@code block} at {@code pos}, owned by {@code owner} and tuned to {@code tuning}. */
    private static RadioBlockEntity radio(final GameTestHelper helper, final BlockPos pos, final Block block,
                                          final UUID owner, final Tuning tuning) {
        helper.setBlock(pos, block);
        final RadioBlockEntity radio = (RadioBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        radio.own(Ownership.of(owner));
        radio.tune(tuning);
        return radio;
    }

    /* An owner of each test's own, so the tests running together never share a frequency. */
    private static UUID owner(final String name) {
        return UUID.nameUUIDFromBytes(("jstests-signals-" + name).getBytes(StandardCharsets.UTF_8));
    }
}
