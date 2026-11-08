/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.boot.DiskChecks;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.computers.workshop.WorkshopPrice;
import dev.jstech.computers.workshop.WorkshopRates;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the computers draw from the grid: through their power supplies, a computer without the energy does not start
 * and one that runs out goes dark, a rack's bays alike, on the red page of unstable power when the line still brought
 * a little; and what their personal-use cards' work costs in energy, owed by the computer and paid as the grid fills
 * it, new work waiting behind it. The draw is on in this batch alone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerEnergyGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = TestComputerEnergy.BATCH;
    private static final int SETTLE = 4;
    private static final int A_SECOND = 20;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    /** The card slots after the graphics card in the first. */
    private static final int FIRST_FREE_CARD_SLOT = PersonalComputerBlockEntity.GPU_SLOTS_START + 1;
    /* What the furnace test has the computer owe: some hundred ticks of paying it off. */
    private static final long OWED = 100_000L;

    private ComputerEnergyGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void drawOn(final ServerLevel level) {
        TestComputerEnergy.drawing(true);
    }

    @AfterBatch(batch = BATCH)
    public static void drawOff(final ServerLevel level) {
        TestComputerEnergy.drawing(false);
    }

    /** A computer with no energy reaching it does not start: its button is pressed and nothing comes up. */
    @GameTest(template = ARENA, timeoutTicks = 100, batch = BATCH)
    public static void power_aComputerWithNoEnergyDoesNotStart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(pc.isRunning(), "placed with no energy, it did not start");
                    pc.togglePower();
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(pc.isRunning() || pc.energized(),
                        "pressed again, still nothing comes up"))
                .thenSucceed();
    }

    /** Running, it draws its parts' watts over its supply's efficiency, a second's worth in twenty ticks. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void power_drawsThePartsWattsOverTheSupplysEfficiency(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        final AtomicLong before = new AtomicLong();
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(pc.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isRunning(), "with energy it starts");
                    helper.assertTrue(pc.lineWatts() > pc.partsWatts(), "the line gives more than the parts take: "
                            + pc.lineWatts() + " W for " + pc.partsWatts() + " W");
                    feeding.set(false);
                })
                .thenExecuteAfter(1, () -> before.set(pc.energyPort().stored()))
                .thenExecuteAfter(A_SECOND, () -> {
                    final long spent = before.get() - pc.energyPort().stored();
                    helper.assertTrue(spent >= pc.lineWatts() - 2 && spent <= pc.lineWatts() + 1,
                            "a second takes the line's watts in joules: " + spent + " J for " + pc.lineWatts()
                                    + " W");
                })
                .thenSucceed();
    }

    /** One that runs out of energy goes dark at once, as a computer whose plug is pulled does. */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = BATCH)
    public static void power_goesDarkWhenTheEnergyRunsOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(pc.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.isRunning(), "with energy it runs");
                    feeding.set(false);
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "without it, it goes dark"))
                .thenSucceed();
    }

    /**
     * A line that still brings something, only less than the computer draws, is unstable power: the system goes down
     * on its red page, the machine is dark after it, and the disk it had in use is left to be checked.
     */
    @GameTest(template = ARENA, timeoutTicks = 1_200, batch = BATCH)
    public static void power_unstablePowerBringsTheSystemDownOnItsRedPage(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicBoolean full = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (full.get()) {
                TestComputerEnergy.feed(pc.energyPort());
            } else {
                TestComputerEnergy.trickle(pc.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "fed, the computer comes up"))
                .thenExecute(() -> full.set(false))
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "on a trickle it goes dark"))
                .thenExecute(() -> {
                    final CrashPage page = pc.crashPage();
                    helper.assertTrue(page != null && page.cause() == CrashCause.UNSTABLE_POWER && page.red(),
                            "on the red page of unstable power; got " + page);
                    helper.assertTrue(pc.crashEndsDark(), "which ends dark, the power gone");
                    helper.assertTrue(DiskChecks.unclean(pc.diskInSlot(0)), "its disk left to be checked");
                })
                .thenSucceed();
    }

    /** A line cut altogether is a power loss: the machine goes dark at once, with no page. */
    @GameTest(template = ARENA, timeoutTicks = 1_200, batch = BATCH)
    public static void power_aCutLineGoesDarkWithNoPage(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final AtomicBoolean feeding = new AtomicBoolean(true);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(pc.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "fed, the computer comes up"))
                .thenExecute(() -> feeding.set(false))
                .thenWaitUntil(() -> helper.assertFalse(pc.isRunning(), "cut off, it goes dark"))
                .thenExecute(() -> {
                    helper.assertTrue(pc.crashPage() == null, "with no page");
                    helper.assertTrue(pc.stoppedSuddenly() && DiskChecks.unclean(pc.diskInSlot(0)),
                            "suddenly, its disk left to be checked");
                })
                .thenSucceed();
    }

    /** A rack's powered bays draw from the cabinet's store: without it they go dark and do not come back on. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = BATCH)
    public static void rack_baysDrawAndGoDarkWithoutEnergy(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        final AtomicBoolean feeding = new AtomicBoolean(false);
        helper.onEachTick(() -> {
            if (feeding.get()) {
                TestComputerEnergy.feed(rack.energyPort());
            }
        });
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(rack.bayPowerOn(0), "with no energy the bay went dark");
                    rack.toggleBayPower(0);
                    helper.assertFalse(rack.bayPowerOn(0), "and its switch brings nothing up");
                    feeding.set(true);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    rack.toggleBayPower(0);
                    helper.assertTrue(rack.bayPowerOn(0) && rack.drawnWatts() > 0.0,
                            "fed, it comes on and draws its supply's line");
                })
                .thenExecuteAfter(A_SECOND, () -> helper.assertTrue(rack.bayPowerOn(0), "and stays on while fed"))
                .thenSucceed();
    }

    /**
     * Paid in energy, an enchantment costs the player no levels: the computer owes ten kilojoules a level of the
     * table's, more work of the cards waits until that is paid, and the grid pays it off.
     */
    @GameTest(template = ARENA, timeoutTicks = 400, batch = BATCH)
    public static void workshop_enchantingIsOwedByTheComputerAndPaidOff(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final ServerPlayer player = player(helper);
        helper.onEachTick(() -> TestComputerEnergy.feed(pc.energyPort()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.ENCHANTING);
                    player.setExperienceLevels(40);
                    pc.workshop().put(Workshop.ENCHANT_ITEM, new ItemStack(Items.DIAMOND_SWORD));
                    helper.assertTrue(pc.workshop().offers(player).get(2).price()
                            .equals(new WorkshopPrice(0, 30_000L)), "the third offer costs 30 kJ and no levels");
                    helper.assertTrue(pc.workshop().enchant(player, 2), "it is taken");
                    helper.assertTrue(player.experienceLevel == 40, "the player paid no levels");
                    helper.assertTrue(pc.energyOwed() == 30_000L, "the computer owes 30 kJ; owes "
                            + pc.energyOwed());
                    pc.workshop().put(Workshop.ENCHANT_ITEM, new ItemStack(Items.DIAMOND_AXE));
                    helper.assertFalse(pc.workshop().enchant(player, 0), "the next waits until it is paid");
                })
                .thenWaitUntil(() -> helper.assertFalse(pc.owesEnergy(), "the grid pays it off; owes "
                        + pc.energyOwed()))
                .thenExecute(() -> {
                    helper.assertTrue(pc.isRunning(), "and the computer ran all along");
                    helper.assertTrue(pc.workshop().enchant(player, 0), "then the next is taken");
                    player.getInventory().clearContent();
                })
                .thenSucceed();
    }

    /** The Furnace Card waits while the computer owes for earlier work, and smelts once it is paid. */
    @GameTest(template = ARENA, timeoutTicks = 800, batch = BATCH)
    public static void workshop_furnaceWaitsWhileTheComputerOwes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        final int perItem = WorkshopRates.ticksPerItem(200, WorkshopRates.furnaceSpeed(HardwareEra.STANDARD));
        helper.onEachTick(() -> TestComputerEnergy.feed(pc.energyPort()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, pc::togglePower)
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    // More than the grid pays off while an item would smelt: a store refills a second's worth a tick.
                    pc.oweEnergy(OWED);
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_IRON, 1));
                })
                .thenExecuteAfter(perItem + SETTLE, () -> {
                    helper.assertTrue(pc.owesEnergy(), "it still owes; owes " + pc.energyOwed());
                    helper.assertTrue(pc.workshop().slot(Workshop.FURNACE_IN).getCount() == 1
                            && pc.workshop().progressTicks() == 0, "and owing, the furnace waits");
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.workshop().slot(Workshop.FURNACE_OUT).is(Items.IRON_INGOT),
                        "paid, it smelts; owes " + pc.energyOwed()))
                .thenExecute(() -> helper.assertTrue(pc.isRunning(), "and the computer ran all along"))
                .thenSucceed();
    }

    /** The grid reaches a computer through J's Energy's port always, and through FE while the series takes it. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void capability_offersTheEnergyPortAndFeWhileTheSeriesTakesIt(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final BlockPos at = helper.absolutePos(PC);
                    helper.assertTrue(helper.getLevel().getCapability(EnergyPorts.PORT, at, null) != null,
                            "the energy port is offered");
                    final boolean fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, at,
                            null) != null;
                    helper.assertTrue(fe == JsEnergy.speaksFe(), "FE is offered exactly while the series takes it");
                })
                .thenSucceed();
    }

    /** With the draw off, the server's choice for a world with no energy at all, a computer runs on nothing. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void power_runsFreeWithTheDrawOff(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(A_SECOND * 2, () -> helper.assertTrue(pc.isRunning() && pc.energized()
                        && pc.energyPort().stored() == 0L, "it runs with nothing in its store"))
                .thenSucceed();
    }

    /* Whether the computer's system is up: past its self-test, its boot menu and its coming-up. */
    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }

    /* Puts {@code card} in the first card slot after the graphics card's. */
    private static void install(final PersonalComputerBlockEntity pc, final WorkshopCard card) {
        pc.getHardware().setStackInSlot(FIRST_FREE_CARD_SLOT, UpdateDoor.cardItem(card));
    }

    /* A fake player of the test's own, with nothing on them. */
    private static ServerPlayer player(final GameTestHelper helper) {
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "energy-test"));
        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        return player;
    }
}
