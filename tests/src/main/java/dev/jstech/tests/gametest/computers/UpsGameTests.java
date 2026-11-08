/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.os.boot.DiskChecks;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The UPSes: the MF PowerGuard feeds the computers standing against it from the line and charges on top, carries them
 * on its battery when the line goes, shuts them down cleanly at a tenth of it, and protects nothing above what it
 * carries; it takes the cable on its back and sides only, and holds a line one class too high off them. A rack's UPS
 * carries its bays, its battery expansions adding to it. A drive shelf extends the server above it, and a rack's PDU
 * lays its bays' power open to the network's language. The draw is on in this batch, as the energy's tests have it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class UpsGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = TestComputerEnergy.BATCH;
    private static final int SETTLE = 4;
    private static final int A_SECOND = 20;
    /* Long enough for a machine to test itself, come up, and say goodbye. */
    private static final int PATIENT = 1_200;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /* The UPS east of the computer, facing north: its back is south, its sides east and west. */
    private static final BlockPos UPS = new BlockPos(3, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    /* A little over a tenth of the Standard UPS's battery: the computer runs on it for a moment, then is shut down. */
    private static final long NEARLY_LOW = UpsRating.POWERGUARD_800.batteryJoules() / 10 + 2_000L;

    /* The energy's batch turns the draw on before its tests and off after them, these among them. */
    private UpsGameTests() {
    }

    /** On the line, it feeds the computer against it and charges its battery on top; the computer knows it. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void ups_feedsTheComputerAgainstItAndChargesOnTop(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final AtomicLong charged = new AtomicLong();
        helper.onEachTick(() -> TestComputerEnergy.feed(line(ups)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isRunning(), "fed through the UPS, the computer starts");
                    charged.set(ups.charge());
                })
                .thenExecuteAfter(A_SECOND, () -> {
                    helper.assertTrue(ups.reading().mode() == UpsReading.Mode.ON_LINE, "the line carries it; got "
                            + ups.reading());
                    helper.assertTrue(ups.charge() > charged.get(), "and the battery charges on top: "
                            + charged.get() + " J, then " + ups.charge() + " J");
                    helper.assertTrue(pc.upsReading().mode() == UpsReading.Mode.ON_LINE,
                            "the computer knows its UPS; got " + pc.upsReading());
                })
                .thenSucceed();
    }

    /** The line goes: the battery carries the computer on, and both say so. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void ups_carriesTheComputerWhenTheLineGoes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final AtomicBoolean feeding = new AtomicBoolean(true);
        final AtomicLong full = new AtomicLong();
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(line(ups));
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isRunning(), "fed, the computer runs");
                    ups.setCharge(UpsRating.POWERGUARD_800.batteryJoules());
                    full.set(ups.charge());
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(ups.reading().onBattery(), "the UPS goes to its battery"))
                .thenExecuteAfter(3 * A_SECOND, () -> {
                    helper.assertTrue(pc.isRunning() && !pc.stoppedSuddenly(), "the battery carries it on");
                    helper.assertTrue(ups.reading().onBattery() && pc.upsReading().onBattery(),
                            "on the battery, as the UPS and the computer say; got " + ups.reading() + " and "
                                    + pc.upsReading());
                    helper.assertTrue(ups.charge() < full.get(), "and the battery runs down");
                })
                .thenSucceed();
    }

    /** At a tenth of the battery, the system shuts itself down cleanly: nothing sudden, no disk left to check. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT, batch = BATCH)
    public static void ups_shutsTheComputerDownCleanlyAtATenth(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(line(ups));
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "fed, the computer comes up"))
                .thenExecute(() -> {
                    ups.setCharge(NEARLY_LOW);
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "at a tenth it is shut down"))
                .thenExecute(() -> {
                    helper.assertFalse(pc.stoppedSuddenly(), "cleanly, not suddenly");
                    helper.assertTrue(pc.crashPage() == null, "with no page");
                    helper.assertFalse(DiskChecks.unclean(pc.diskInSlot(0)), "and its disk left clean");
                    helper.assertTrue(ups.charge() > 0L, "the battery not run out");
                })
                .thenSucceed();
    }

    /** Started again on so low a battery, the computer is shut down again, as real UPS software does. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT, batch = BATCH)
    public static void ups_aComputerStartedOnALowBatteryIsShutDownAgain(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(line(ups));
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "fed, the computer comes up"))
                .thenExecute(() -> {
                    ups.setCharge(NEARLY_LOW);
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "at a tenth it is shut down"))
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(pc.isRunning() || pc.poweringOff(),
                        "switched on, it starts"))
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "and the low battery shuts it down again"))
                .thenExecute(() -> helper.assertFalse(pc.stoppedSuddenly(), "cleanly again"))
                .thenSucceed();
    }

    /** One starting over as the battery falls to a tenth is let finish its goodbye, then shut down, once. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT, batch = BATCH)
    public static void ups_aComputerStartingOverAtATenthFinishesItsGoodbye(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(line(ups));
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "fed, the computer comes up"))
                .thenExecute(() -> {
                    pc.restart();
                    helper.assertTrue(pc.goingDown(), "it says goodbye before starting over");
                    ups.setCharge(UpsRating.POWERGUARD_800.batteryJoules() / 10);
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.goingDown(), "the goodbye ends, not told to start again"))
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "and the low battery shuts it down"))
                .thenExecute(() -> helper.assertFalse(pc.stoppedSuddenly(), "cleanly"))
                .thenSucceed();
    }

    /** A computer drawing more than the UPS carries runs from the line in bypass, and goes dark with the line. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void ups_aboveWhatItCarriesItIsInBypassAndProtectsNothing(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.VINTAGE_UPS.get());
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(line(ups));
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isRunning(), "fed, the computer runs");
                    helper.assertTrue(pc.lineWatts() > UpsRating.POWERGUARD_300.carryWatts(),
                            "it draws more than the 300 carries: " + pc.lineWatts() + " W");
                    ups.setCharge(UpsRating.POWERGUARD_300.batteryJoules());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(ups.reading().mode() == UpsReading.Mode.BYPASS, "in bypass; got "
                            + ups.reading());
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "the line gone, the battery carries nothing"))
                .thenExecute(() -> helper.assertTrue(pc.stoppedSuddenly(), "and it went dark suddenly"))
                .thenSucceed();
    }

    /** The cable plugs in on its back and sides: the energy port is there, not on its front or its top. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void ups_takesTheLineOnItsBackAndSidesOnly(final GameTestHelper helper) {
        placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final BlockPos at = helper.absolutePos(UPS);
        for (final Direction inlet : new Direction[] {Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            helper.assertTrue(helper.getLevel().getCapability(EnergyPorts.PORT, at, inlet) != null,
                    "an inlet on its " + inlet + " face");
        }
        for (final Direction shut : new Direction[] {Direction.NORTH, Direction.UP, Direction.DOWN}) {
            helper.assertTrue(helper.getLevel().getCapability(EnergyPorts.PORT, at, shut) == null,
                    "no inlet on its " + shut + " face");
        }
        helper.succeed();
    }

    /** A line one class too high does not burn it: it refuses the line, and a computer on it runs on the battery. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void ups_holdsALineOneClassTooHighOffTheComputer(final GameTestHelper helper) {
        final UpsBlockEntity ups = placeUps(helper, ComputingModule.STANDARD_UPS.get());
        final long now = helper.getLevel().getGameTime();
        final EnergyStore inlet = (EnergyStore) line(ups);
        inlet.meetWrongClass(WrongClass.BURNS, JsEnergy.MEDIUM, now);
        helper.assertFalse(inlet.damaged(), "one class too high does not burn it");
        helper.assertTrue(inlet.overvoltage(now), "it only refuses the line");
        inlet.meetWrongClass(WrongClass.BURNS, JsEnergy.LOW, now);
        helper.assertFalse(inlet.damaged(), "nor does a surge");
        helper.succeed();
    }

    /** The rack's UPS carries the powered bays on its battery when the line goes, and says so. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void rackUps_carriesTheBaysWhenTheLineGoes(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(rack.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(rack.getServers().insertItem(3, new ItemStack(ComputingModule.RACK_UPS.get()),
                            false).isEmpty(), "a Rack UPS mounts");
                    rack.setBayPower(0, true);
                    helper.assertTrue(rack.bayPowerOn(0) && rack.drawnWatts() > 0.0, "fed, the bay runs");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    rack.setUpsCharge(UpsRating.RACK.batteryJoules());
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(rack.upsReading().onBattery(), "the UPS goes to its battery"))
                .thenExecuteAfter(3 * A_SECOND, () -> {
                    helper.assertTrue(rack.bayPowerOn(0), "the battery carries the bay on");
                    helper.assertTrue(rack.upsReading().onBattery(), "on the battery; got " + rack.upsReading());
                })
                .thenSucceed();
    }

    /** At a tenth of its battery the rack's UPS shuts the bays down cleanly. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT, batch = BATCH)
    public static void rackUps_shutsTheBaysDownAtATenth(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(rack.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack.getServers().insertItem(3, new ItemStack(ComputingModule.RACK_UPS.get()), false);
                    rack.setBayPower(0, true);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    rack.setUpsCharge(UpsRating.RACK.batteryJoules() / 10 + 200L);
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(rack.bayPowerOn(0), "at a tenth the bay is shut down"))
                .thenExecute(() -> helper.assertTrue(rack.upsReading().onBattery(),
                        "still on the battery, which did not run out; got " + rack.upsReading()))
                .thenSucceed();
    }

    /** A Rack UPS mounted empty on a live line stands on the line and charges, the bays running on. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void rackUps_mountedEmptyOnALiveLineCharges(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        helper.onEachTick(() -> TestComputerEnergy.feed(rack.energyPort()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack.setBayPower(0, true);
                    helper.assertTrue(rack.bayPowerOn(0), "fed, the bay runs");
                })
                .thenExecuteAfter(SETTLE, () -> rack.getServers().insertItem(3,
                        new ItemStack(ComputingModule.RACK_UPS.get()), false))
                .thenExecuteAfter(A_SECOND, () -> {
                    helper.assertTrue(rack.bayPowerOn(0), "the bay runs on");
                    helper.assertTrue(rack.upsReading().mode() == UpsReading.Mode.ON_LINE
                            && rack.upsReading().permille() > 0, "on the line, charging; got " + rack.upsReading());
                })
                .thenSucceed();
    }

    /** In an outage a bay switched on starts on the rack's UPS, as it would on the line. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void rackUps_startsABayInAnOutage(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(rack.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack.getServers().insertItem(3, new ItemStack(ComputingModule.RACK_UPS.get()), false);
                    rack.setBayPower(0, false);
                    rack.setUpsCharge(UpsRating.RACK.batteryJoules());
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertTrue(rack.upsReading().onBattery(),
                        "with nothing running, the UPS still knows the line went"))
                .thenExecute(() -> helper.assertTrue(rack.setBayPower(0, true), "the bay starts on the battery"))
                .thenExecuteAfter(A_SECOND, () -> helper.assertTrue(rack.bayPowerOn(0), "and runs on it"))
                .thenSucceed();
    }

    /** Every Rack UPS holds 2.7 MJ for up to 3 kW, and every battery expansion adds 2.7 MJ. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void rackUps_expansionsAddToItsBattery(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        helper.assertTrue(RackUnitItem.rackUps(ServerRackBlockEntity.CAPACITY_U,
                rack.getServers()::getStackInSlot) == null, "no Rack UPS, no UPS");
        rack.getServers().insertItem(3, new ItemStack(ComputingModule.RACK_UPS.get()), false);
        rack.getServers().insertItem(4, new ItemStack(ComputingModule.UPS_BATTERY_EXPANSION.get()), false);
        rack.getServers().insertItem(5, new ItemStack(ComputingModule.UPS_BATTERY_EXPANSION.get()), false);
        final UpsRating rating = RackUnitItem.rackUps(ServerRackBlockEntity.CAPACITY_U,
                rack.getServers()::getStackInSlot);
        helper.assertTrue(rating != null && rating.batteryJoules() == 8_100_000L && rating.carryWatts() == 3_000,
                "a Rack UPS and two expansions hold 8.1 MJ for 3 kW; got " + rating);
        helper.succeed();
    }

    /** A drive shelf right under a server adds its ten bays to the server's; one with no server above serves none. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void driveShelf_extendsTheServerRightAboveIt(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        rack.getServers().setStackInSlot(0, new ItemStack(ComputingModule.STORAGE_SERVER.get()));
        helper.assertTrue(rack.getServers().insertItem(2, new ItemStack(ComputingModule.DRIVE_SHELF.get()), false)
                .isEmpty(), "the shelf mounts under the 2U server");
        int inserted = 0;
        while (inserted < 3 * 10 && rack.insertDrive(0, nvmeDrive())) {
            inserted++;
        }
        helper.assertTrue(rack.claimedDriveStacks(0).size() == 20, "the server's ten bays and the shelf's ten; got "
                + rack.claimedDriveStacks(0).size());
        helper.assertTrue(DriveVolumes.isDisk(rack.getFrontSlots().getStackInSlot(2 * RackLayout.SLOTS_PER_U)),
                "the shelf's first bay holds a drive");
        final long storageBefore = rack.storageModCount(0);
        rack.getServers().setStackInSlot(2, ItemStack.EMPTY);
        helper.assertTrue(rack.claimedDriveStacks(0).size() == 10, "without the shelf, its own ten; got "
                + rack.claimedDriveStacks(0).size());
        helper.assertTrue(rack.storageModCount(0) > storageBefore,
                "and the network is told the server's storage changed");
        rack.getServers().setStackInSlot(2, new ItemStack(ComputingModule.DRIVE_SHELF.get()));
        rack.getServers().setStackInSlot(0, new ItemStack(ComputingModule.SERVER.get()));
        helper.assertTrue(rack.claimedDriveStacks(0).size() == 4,
                "a 1U server leaves a row between it and the shelf, which then serves nobody; got "
                        + rack.claimedDriveStacks(0).size());
        helper.succeed();
    }

    /** With a PDU, QUERY power lists the rack's servers and UPDATE power switches them; without, there is nothing. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void pdu_layTheBaysPowerOpenToTheNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null,
                        "the Mainframe runs its network"))
                .thenExecuteAfter(SETTLE + 6, () -> {
                    helper.assertTrue(query(helper, mainframe).rows().isEmpty(), "no PDU, no row");
                    helper.assertFalse(run(helper, mainframe, "UPDATE power SET on = no").ok(),
                            "and nothing to switch");
                    helper.assertTrue(rack.getServers().insertItem(4, new ItemStack(ComputingModule.PDU.get()),
                            false).isEmpty(), "a PDU mounts");
                    rack.setBayPower(0, true);
                    final IqlTable power = query(helper, mainframe);
                    helper.assertTrue(power.rows().size() == 1 && "1".equals(power.cell(0, "row").english())
                            && "yes".equals(power.cell(0, "on").english()), "its server is listed, on; got "
                            + power.rows());
                    final IqlEngine.Outcome off = run(helper, mainframe, "UPDATE power SET on = no WHERE row = 1");
                    helper.assertTrue(off.ok() && !rack.bayPowerOn(0), "UPDATE switches it off: "
                            + off.message());
                    helper.assertTrue("no".equals(query(helper, mainframe).cell(0, "on").english()),
                            "and the table says so");
                    final IqlEngine.Outcome on = run(helper, mainframe, "UPDATE power SET on = yes");
                    helper.assertTrue(on.ok() && rack.bayPowerOn(0), "and on again: " + on.message());
                })
                .thenSucceed();
    }

    /* Places a UPS of {@code block} east of the computer, facing north. */
    private static UpsBlockEntity placeUps(final GameTestHelper helper, final Block block) {
        helper.setBlock(UPS, block);
        if (helper.getBlockEntity(UPS) instanceof UpsBlockEntity ups) {
            return ups;
        }
        throw new IllegalStateException("no UPS at " + UPS);
    }

    /* The UPS's inlet, on its back, which a test feeds as a cable would. */
    private static IEnergyPort line(final UpsBlockEntity ups) {
        final IEnergyPort port = ups.energyPort(Direction.SOUTH);
        if (port == null) {
            throw new IllegalStateException("the UPS has no inlet on its back");
        }
        return port;
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }

    private static ItemStack nvmeDrive() {
        return new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return NetworkFixtures.runIql(mainframe, new ServerCliComputer(mainframe, helper.getLevel()), statement);
    }

    private static IqlTable query(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return run(helper, mainframe, "QUERY power").table();
    }
}
