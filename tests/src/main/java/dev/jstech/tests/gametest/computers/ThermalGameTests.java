/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.core.heat.IThermalBudget;
import dev.jstech.core.heat.ThermalBudgets;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Temperature (computers/docs/design/temperature.md): a computer's case seats a cooler over each processor and fans in
 * the seats it has, a water cooler's radiator taking a pair of them; what they take away is its budget, and a machine
 * whose cooling falls short of what it holds heats up, throttles to hold 90 °C and trips at 100 °C, staying off until
 * it has cooled. A rack has one temperature for all of its machines, and a Vent Panel adds to its budget. J's Core
 * reads a computer's heat through its thermal budget. The temperature takes half a minute to get where a load leaves
 * it, so the tests settle it there as long enough would.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final double KELVIN = 273.15;
    /* The graphics cards a hot machine carries, with the supply that feeds them all. */
    private static final int HOT_CARDS = 4;

    private ThermalGameTests() {
    }

    /** A cooler of the case's era or an older one goes over the processor; a later one's mount holds nothing. */
    @GameTest(template = ARENA)
    public static void coolerSeat_takesACoolerOfTheCasesEraOrAnOlderOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final int seat = pc.hardwareLayout().coolerStart();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isValidForSlot(seat, stack(CoolingItems.VINTAGE_STOCK_COOLER.get())),
                            "an older cooler's mount still holds");
                    helper.assertTrue(pc.isValidForSlot(seat, stack(CoolingItems.STANDARD_TOWER_COOLER.get())),
                            "a tower cooler of the case's own era goes in");
                    final ItemStack later = stack(CoolingItems.ADVANCED_STOCK_COOLER.get());
                    helper.assertTrue(!pc.isValidForSlot(seat, later)
                            && pc.refusal(seat, later) == PartRefusals.COOLER_TOO_NEW, "a later cooler is refused");
                    helper.assertTrue(!pc.isValidForSlot(seat, stack(CoolingItems.STANDARD_CASE_FAN.get())),
                            "a fan is no cooler");
                })
                .thenSucceed();
    }

    /** The Neutral case seats three fans, one at the rear and two at the front; a fourth seat has no place. */
    @GameTest(template = ARENA)
    public static void fanSeats_areTheCasesOwn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final ComputerHardwareLayout layout = pc.hardwareLayout();
        final ItemStack fan = stack(CoolingItems.STANDARD_CASE_FAN.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.computer().cooling().equals(CaseCooling.NEUTRAL), "the Neutral case");
                    helper.assertTrue(pc.isValidForSlot(layout.fanStart() + 2, fan), "its third seat takes a fan");
                    helper.assertTrue(!pc.isValidForSlot(layout.fanStart() + 3, fan)
                                    && pc.refusal(layout.fanStart() + 3, fan) == PartRefusals.NO_FAN_SEAT,
                            "a fourth seat it does not have");
                    helper.assertTrue(pc.isValidForSlot(layout.fanStart(),
                            stack(CoolingItems.LEGACY_CASE_FAN.get())), "an older fan fits");
                })
                .thenSucceed();
    }

    /** A water cooler's radiator takes the High Performance case's top pair of seats, which take no fan then. */
    @GameTest(template = ARENA)
    public static void waterCooler_takesTheTopPairOfAHighPerformanceCase(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity pc = blockEntity(helper);
        final ComputerHardwareLayout layout = pc.hardwareLayout();
        final ItemStack fan = stack(CoolingItems.STANDARD_CASE_FAN.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isValidForSlot(layout.coolerStart(),
                            stack(CoolingItems.STANDARD_AQUALOOP_240.get())), "the water cooler has room");
                    pc.getHardware().setStackInSlot(layout.coolerStart(),
                            stack(CoolingItems.STANDARD_AQUALOOP_240.get()));
                    for (final int seat : new int[] {1, 2}) {
                        helper.assertTrue(!pc.fanSeatOpen(seat) && !pc.isValidForSlot(layout.fanStart() + seat, fan)
                                        && pc.refusal(layout.fanStart() + seat, fan) == PartRefusals.UNDER_RADIATOR,
                                "its radiator sits in top seat " + seat);
                    }
                    helper.assertTrue(pc.isValidForSlot(layout.fanStart(), fan)
                            && pc.isValidForSlot(layout.fanStart() + 3, fan), "the rear and the front still take fans");
                })
                .thenSucceed();
    }

    /** A water cooler finds no room where fans already hold the pair its radiator would take. */
    @GameTest(template = ARENA)
    public static void waterCooler_findsNoRoomWhereFansHoldItsPair(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final int seat = pc.hardwareLayout().coolerStart();
        final ItemStack water = stack(CoolingItems.STANDARD_AQUALOOP_360.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    // The Neutral case's only pair is its front one, which the test computer's fans fill.
                    helper.assertTrue(!pc.isValidForSlot(seat, water)
                            && pc.refusal(seat, water) == PartRefusals.NO_RADIATOR_ROOM, "no free pair, no radiator");
                    final ItemStackHandler hw = pc.getHardware();
                    hw.setStackInSlot(pc.hardwareLayout().fanStart() + 1, ItemStack.EMPTY);
                    hw.setStackInSlot(pc.hardwareLayout().fanStart() + 2, ItemStack.EMPTY);
                    helper.assertTrue(pc.isValidForSlot(seat, water), "the front pair free, the radiator goes in");
                })
                .thenSucceed();
    }

    /** The budget is the case's own, its cooler's and its fans': the spec's gaming PC sits well under 80 °C. */
    @GameTest(template = ARENA)
    public static void budget_addsTheCaseTheCoolerAndTheFans(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::settleHeat)
                .thenExecuteAfter(1, () -> {
                    final ThermalState thermal = pc.thermal();
                    helper.assertTrue(thermal.budgetWatts() == 200 + 150 + 3 * 50, "the Neutral case, the Standard "
                            + "stock cooler and three fans take away 500 W: " + thermal);
                    helper.assertTrue(thermal.fans() == 3 && thermal.fanSeats() == 3 && thermal.bareProcessors() == 0,
                            "three fans of three, the processor cooled: " + thermal);
                    helper.assertTrue(thermal.celsius() < 80 && !thermal.throttled() && pc.heatClock() == 1.0,
                            "cooled for what it holds, it runs at full speed: " + thermal);
                })
                .thenSucceed();
    }

    /** With no cooler and no fans it heats up and throttles to hold 90 °C: its capacity and its clock fall. */
    @GameTest(template = ARENA)
    public static void heat_throttlesAMachineItsCoolingFallsShortOf(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicLong cool = new AtomicLong();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    cool.set(pc.capacity());
                    uncool(pc);
                })
                .thenExecuteAfter(1, pc::settleHeat)
                .thenExecuteAfter(1, () -> {
                    final ThermalState thermal = pc.thermal();
                    helper.assertTrue(thermal.throttled() && thermal.celsius() == 90, "it holds 90 °C: " + thermal);
                    helper.assertTrue(pc.heatClock() < 1.0 && pc.capacity() < cool.get(),
                            "its clock and its capacity fall: " + pc.heatClock() + ", " + pc.capacity() + " of "
                                    + cool.get());
                    helper.assertTrue(thermal.bareProcessors() == 1 && thermal.lack() == ThermalState.Lack.BARE,
                            "its screen says the processor has no cooler: " + thermal);
                    helper.assertTrue(pc.isRunning(), "a throttled machine runs on");
                })
                .thenSucceed();
    }

    /** Past what a quarter of its clock holds it trips: dark at once, and off until it has cooled below 60 °C. */
    @GameTest(template = ARENA)
    public static void heat_tripsAndKeepsTheMachineOffUntilItCools(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> overheat(pc))
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(pc.isRunning(), "four graphics cards on a supply that carries them run");
                    pc.settleHeat();
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(!pc.isRunning() && pc.stoppedSuddenly(), "it tripped and went dark at once");
                    final ThermalState thermal = pc.thermal();
                    helper.assertTrue(thermal.cooling() && thermal.lastTrip() >= 100,
                            "it is cooling from a trip at 100 °C or more: " + thermal);
                    pc.togglePower();
                    helper.assertTrue(!pc.isRunning(), "too hot, the button brings nothing up");
                })
                .thenExecuteAfter(1, pc::settleHeat)
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(!pc.thermal().cooling(), "cooled to its surroundings: " + pc.thermal());
                    pc.togglePower();
                    helper.assertTrue(pc.isRunning(), "cool again, it starts");
                    helper.assertTrue(pc.thermal().lastTrip() > 0, "its next self-test still has the trip to warn of");
                })
                .thenSucceed();
    }

    /** A machine saved the tick it tripped comes back still too hot to start, cooling from where it tripped. */
    @GameTest(template = ARENA)
    public static void heat_keepsATripThroughASave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> overheat(pc))
                .thenExecuteAfter(1, pc::settleHeat)
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(!pc.isRunning() && pc.thermal().cooling(), "it tripped: " + pc.thermal());
                    final HolderLookup.Provider registries = helper.getLevel().registryAccess();
                    pc.loadWithComponents(pc.saveWithoutMetadata(registries), registries);
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(pc.thermal().cooling() && pc.thermal().celsius() >= 90,
                            "still cooling from its trip after the save: " + pc.thermal());
                    pc.togglePower();
                    helper.assertTrue(!pc.isRunning(), "too hot to start after the save too");
                })
                .thenSucceed();
    }

    /** J's Core reads a computer's heat: its load, its budget, its temperature in kelvin and its clock. */
    @GameTest(template = ARENA)
    public static void heat_isReadThroughTheCoresThermalBudget(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::settleHeat)
                .thenExecuteAfter(1, () -> {
                    final IThermalBudget budget = ThermalBudgets.at(helper.getLevel(), helper.absolutePos(PC), null);
                    helper.assertTrue(budget != null, "a computer answers the Core's thermal budget");
                    final ThermalState thermal = pc.thermal();
                    helper.assertTrue(budget.loadWatts() == pc.partsWatts()
                                    && budget.budgetWatts() == thermal.budgetWatts()
                                    && Math.abs(budget.temperature() - (thermal.celsius() + KELVIN)) < 1e-6
                                    && budget.performance() == pc.heatClock(),
                            "the budget reads the computer's own: " + budget.loadWatts() + " W, "
                                    + budget.budgetWatts() + " W, " + budget.temperature() + " K");
                })
                .thenSucceed();
    }

    /** The Device Manager tells the cooler, the case fans that turn, and how hot the processor runs. */
    @GameTest(template = ARENA)
    public static void deviceManager_tellsTheCoolingAndTheHeat(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final DeviceMap map = DeviceMaps.of(helper.getLevel(), pc);
                    helper.assertTrue(map.cooling().parts().size() == 2, "the cooler and the case fans: "
                            + map.cooling().parts());
                    final DeviceMap.Noted fans = map.cooling().parts().getLast();
                    helper.assertTrue(fans.name().equals(DeviceMap.CASE_FANS.text())
                            && fans.note().equals(DeviceMap.OF.with(3, 3)), "three fans of three: " + fans);
                    helper.assertTrue(!map.cooling().processorHeat().isEmpty(), "how hot the processor runs");
                    helper.assertTrue(map.batteries().isEmpty(), "no UPS, no battery");
                })
                .thenSucceed();
    }

    /** The Mainframe's cabinet takes 1,500 W away on its own, and a cooler over each socket adds to it. */
    @GameTest(template = ARENA)
    public static void mainframe_coolsWithItsCabinetAndACoolerOverEachSocket(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(mainframe.thermal().budgetWatts() == CaseCooling.MAINFRAME.caseWatts(),
                            "the cabinet alone: " + mainframe.thermal());
                    final ItemStack tower = stack(CoolingItems.STANDARD_TOWER_COOLER.get());
                    helper.assertTrue(mainframe.isValidForSlot(MainframeBlockEntity.COOLER_SLOTS_START, tower),
                            "a cooler goes over its first socket");
                    mainframe.getHardware().setStackInSlot(MainframeBlockEntity.COOLER_SLOTS_START, tower);
                    helper.assertTrue(mainframe.thermal().budgetWatts() == CaseCooling.MAINFRAME.caseWatts() + 250,
                            "and adds what it takes away: " + mainframe.thermal());
                })
                .thenSucceed();
    }

    /** A Vent Panel adds 500 W to its rack's budget and is drawn in the colour of the rack it is in. */
    @GameTest(template = ARENA)
    public static void ventPanel_addsItsShareInTheRacksColour(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final int before = rack.thermalBudgetWatts();
                    final int row = 7;
                    rack.getServers().setStackInSlot(row, stack(ComputingModule.VENT_PANEL.get()));
                    helper.assertTrue(rack.thermalBudgetWatts() == before + RackThermals.VENT_PANEL_W,
                            "the panel adds its 500 W: " + before + " W, then " + rack.thermalBudgetWatts() + " W");
                    helper.assertTrue(rack.unitCodeAt(row) == ServerRackBlockEntity.UNIT_VENT_PANEL_VINTAGE
                            + HardwareEra.partsIndex(rack.rackEra()), "drawn in its rack's colour");
                })
                .thenSucceed();
    }

    /* The Personal Computer at the arena's computer. */
    private static PersonalComputerBlockEntity blockEntity(final GameTestHelper helper) {
        if (helper.getBlockEntity(PC) instanceof PersonalComputerBlockEntity pc) {
            return pc;
        }
        throw new IllegalStateException("no Personal Computer at " + PC);
    }

    /*
     * Leaves the computer far hotter than its case alone takes away: no cooler, no fans, and four graphics cards on a
     * supply that carries them. The supply goes in first: a card more on the old one would ask more than it gives.
     */
    private static void overheat(final PersonalComputerBlockEntity pc) {
        uncool(pc);
        final ItemStackHandler hw = pc.getHardware();
        hw.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, stack(HardwareItems.PSU_1600P.get()));
        for (int card = 0; card < HOT_CARDS; card++) {
            hw.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + card,
                    stack(ComputingModule.GPU_HD_7970.get()));
        }
    }

    /* Takes the computer's cooler and fans out, leaving it cooled by its case alone. */
    private static void uncool(final PersonalComputerBlockEntity pc) {
        final ComputerHardwareLayout layout = pc.hardwareLayout();
        for (int slot = layout.coolerStart(); slot < layout.fanStart() + layout.fanCount(); slot++) {
            pc.getHardware().setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static ItemStack stack(final ItemLike item) {
        return new ItemStack(item);
    }
}
