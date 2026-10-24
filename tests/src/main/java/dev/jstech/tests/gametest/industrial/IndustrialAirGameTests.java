/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.transit.Capsule;
import dev.jstech.core.transit.CapsuleRoute;
import dev.jstech.core.transit.CapsuleTransit;
import dev.jstech.industrial.line.CompressedAir;
import dev.jstech.industrial.line.PneumaticPost;
import dev.jstech.industrial.line.PneumaticStation;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestMatterEnds;
import dev.jstech.tests.TestPneumatics;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's compressed air and pneumatic post: a machine with an air port, fed by a Compressed Air Line, works
 * fifteen percent faster and spends its air, beside the same machine without; a station sends what it is given by its
 * routes, iron one way and the rest another, each capsule taking its trip's time and kept with the world on its way;
 * and without air a station sends nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialAirGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AIR = new BlockPos(1, 2, 2);
    private static final BlockPos FED = new BlockPos(3, 2, 2);
    private static final BlockPos UNFED = new BlockPos(3, 2, 5);
    private static final BlockPos SENDER = new BlockPos(1, 2, 4);
    private static final BlockPos IRON_STATION = new BlockPos(7, 2, 4);
    private static final BlockPos REST_STATION = new BlockPos(4, 2, 6);
    private static final long FILL = 4_000L;
    private static final int COBBLESTONE = 16;
    private static final int IRON_ADDRESS = 3;
    private static final int REST_ADDRESS = 7;
    private static final int SENDER_ADDRESS = 1;

    private IndustrialAirGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void airPort_makesAMachineFasterAndSpendsItsAir(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        TestMatterEnds.clear(level, helper.absolutePos(AIR));
        helper.setBlock(AIR, Blocks.CHISELED_COPPER);
        TestMatterEnds.tankAt(level, helper.absolutePos(AIR), MatterState.GAS)
                .insert(new MatterStack(CompressedAir.air(), FILL), false);
        TestCables.lay(helper, AIR.east(), TestCableTypes.COMPRESSED_AIR_LINE);
        helper.setBlock(FED, TestPneumatics.AIR_PRESS.get());
        helper.setBlock(UNFED, TestPneumatics.AIR_PRESS.get());
        final TestPneumatics.AirPress fed = helper.getBlockEntity(FED);
        final TestPneumatics.AirPress unfed = helper.getBlockEntity(UNFED);
        helper.onEachTick(() -> {
            for (final TestPneumatics.AirPress press : List.of(fed, unfed)) {
                press.power().generate(press.power().capacity());
                if (press.getInventory().getStackInSlot(0).isEmpty()) {
                    press.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, COBBLESTONE));
                }
            }
        });
        final int faster = (int) Math.ceil(TestPneumatics.PRESSES_IN / (1 + CompressedAir.SPEED_GAIN));
        helper.succeedWhen(() -> {
            helper.assertTrue(fed.getMaxProgress() == faster, "with air, a press takes " + faster + " ticks: "
                    + fed.getMaxProgress());
            helper.assertTrue(unfed.getMaxProgress() == TestPneumatics.PRESSES_IN,
                    "without, its own " + TestPneumatics.PRESSES_IN + ": " + unfed.getMaxProgress());
            final long left = TestMatterEnds.amountAt(level, helper.absolutePos(AIR), MatterState.GAS,
                    CompressedAir.air()) + fed.airPort().orElseThrow().held();
            helper.assertTrue(left < FILL, "and it spends its air: " + left + " of " + FILL + " left");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void station_sendsByItsRoutesEachCapsuleInItsTripsTime(final GameTestHelper helper) {
        final PneumaticStation sender = post(helper);
        sender.airPort().tank().insert(new MatterStack(CompressedAir.air(), PneumaticPost.AIR_A_SEND * 4), false);
        final int trip = (int) PneumaticPost.RULES.ticksFor(helper.absolutePos(SENDER)
                .distManhattan(helper.absolutePos(IRON_STATION)));
        helper.assertTrue(sender.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "a station takes what a route sends somewhere");
        helper.assertTrue(sender.items().insertItem(1, new ItemStack(Items.COAL, 8), false).isEmpty(),
                "and what it sends the rest of");
        final long[] seen = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(inFlight(helper, IRON_STATION).isPresent(),
                        "the iron leaves in a capsule"))
                .thenExecute(() -> {
                    final Capsule capsule = inFlight(helper, IRON_STATION).orElseThrow();
                    seen[0] = helper.getLevel().getGameTime();
                    helper.assertTrue(capsule.arrives() - seen[0] <= trip && capsule.arrives() - seen[0] >= trip - 1,
                            "a trip of " + trip + " ticks for its distance: " + (capsule.arrives() - seen[0]));
                    helper.assertTrue(chest(helper, IRON_STATION).isEmpty(), "and it is not there yet");
                    keptWithTheWorld(helper, capsule);
                })
                .thenWaitUntil(() -> helper.assertTrue(chest(helper, IRON_STATION).getItem(0).is(Items.IRON_INGOT),
                        "the iron arrives at station " + IRON_ADDRESS + " and is unloaded"))
                .thenWaitUntil(() -> helper.assertTrue(chest(helper, REST_STATION).getItem(0).is(Items.COAL),
                        "the coal at station " + REST_ADDRESS))
                .thenExecute(() -> helper.assertTrue(sender.airPort().held() == PneumaticPost.AIR_A_SEND * 2,
                        "each capsule spent its air: " + sender.airPort().held()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void station_sendsNothingWithoutAir(final GameTestHelper helper) {
        final PneumaticStation sender = post(helper);
        sender.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        helper.startSequence()
                .thenIdle(PneumaticPost.RULES.every() * 2)
                .thenExecute(() -> {
                    helper.assertTrue(inFlight(helper, IRON_STATION).isEmpty()
                            && sender.outgoing().getStackInSlot(0).getCount() == 8, "without air it waits");
                    sender.airPort().tank().insert(new MatterStack(CompressedAir.air(), PneumaticPost.AIR_A_SEND),
                            false);
                })
                .thenWaitUntil(() -> helper.assertTrue(sender.outgoing().getStackInSlot(0).isEmpty(),
                        "with a capsule's air, it sends"))
                .thenExecute(() -> helper.assertTrue(sender.airPort().held() == 0, "and spends it"))
                .thenSucceed();
    }

    /*
     * A sender of address 1 joined by a run of tubes to a station of address 3, which iron goes to, and one of
     * address 7, which everything else goes to, each facing a chest.
     */
    private static PneumaticStation post(final GameTestHelper helper) {
        final PneumaticStation sender = station(helper, SENDER, Direction.WEST, SENDER_ADDRESS);
        station(helper, IRON_STATION, Direction.SOUTH, IRON_ADDRESS);
        station(helper, REST_STATION, Direction.SOUTH, REST_ADDRESS);
        for (int x = SENDER.getX() + 1; x < IRON_STATION.getX(); x++) {
            TestCables.lay(helper, new BlockPos(x, SENDER.getY(), SENDER.getZ()), TestCableTypes.CAPSULE_TUBE);
        }
        TestCables.lay(helper, new BlockPos(REST_STATION.getX(), REST_STATION.getY(), REST_STATION.getZ() - 1),
                TestCableTypes.CAPSULE_TUBE);
        helper.setBlock(IRON_STATION.south(), Blocks.CHEST);
        helper.setBlock(REST_STATION.south(), Blocks.CHEST);
        sender.setRoutes(List.of(new CapsuleRoute(ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0)),
                IRON_ADDRESS)));
        sender.setOtherwise(REST_ADDRESS);
        return sender;
    }

    private static PneumaticStation station(final GameTestHelper helper, final BlockPos at, final Direction facing,
                                            final int address) {
        helper.setBlock(at, TestPneumatics.CAPSULE_STATION.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        final PneumaticStation station = helper.getBlockEntity(at);
        station.setAddress(address);
        return station;
    }

    private static Optional<Capsule> inFlight(final GameTestHelper helper, final BlockPos to) {
        final BlockPos target = helper.absolutePos(to);
        return CapsuleTransit.inFlight(helper.getLevel()).stream().filter(capsule -> capsule.to().equals(target))
                .findFirst();
    }

    private static ChestBlockEntity chest(final GameTestHelper helper, final BlockPos station) {
        return helper.getBlockEntity(station.south());
    }

    /* A capsule on its way saved as its world saves it reads back as the same capsule. */
    private static void keptWithTheWorld(final GameTestHelper helper, final Capsule capsule) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        final Tag saved = Capsule.CODEC.encodeStart(ops, capsule).getOrThrow();
        final Capsule read = Capsule.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(read.from().equals(capsule.from()) && read.to().equals(capsule.to())
                        && read.arrives() == capsule.arrives() && read.items().size() == capsule.items().size()
                        && ItemStack.matches(read.items().getFirst(), capsule.items().getFirst()),
                "a capsule on its way is kept with the world");
    }
}
