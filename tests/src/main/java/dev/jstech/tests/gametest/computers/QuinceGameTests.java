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
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.QuinceComputerBlock;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.QuinceMachines;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.MonitorPictures;
import dev.jstech.computers.monitor.SessionOpenings;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.os.QuinceStart;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quince's computers (computers/docs/design/quince.md): closed hardware, each machine with its own logic board alone;
 * the all-in-ones their own screen; their cables and power button behind; the firmware's Sad Quince when the parts
 * make no computer, and the start-up choices a sneaking power-on opens, which the self-test waits at.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class QuinceGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* Long enough for a Transition machine's self-test to run out, and the wait after it to show. */
    private static final int LONG_TEST = 900;
    private static final int PAST_ITS_END = 40;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final BlockPos BESIDE = new BlockPos(5, 2, 2);

    private QuinceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void quinceBoard_goesOnlyInItsOwnMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = place(helper, AT, ComputingModule.QRUSSET_WHITE.get());
        final PersonalComputerBlockEntity pc = place(helper, BESIDE,
                ComputingModule.TRANSITION_PERSONAL_COMPUTER.get());
        final int slot = PersonalComputerBlockEntity.MOTHERBOARD_SLOT;
        final ItemStack own = new ItemStack(QuinceItems.BOARD_QRUSSET_WHITE.get());
        helper.assertTrue(white.isValidForSlot(slot, own), "the white qRusset takes its own logic board");
        final ItemStack common = new ItemStack(HardwareItems.MOTHERBOARD_ATX_TRANSITION_775.get());
        helper.assertFalse(white.isValidForSlot(slot, common), "and no common board of its era");
        helper.assertFalse(white.isValidForSlot(slot, new ItemStack(QuinceItems.BOARD_PRO_2006.get())),
                "nor another Quince machine's");
        helper.assertFalse(pc.isValidForSlot(slot, own), "and its board goes in no other case");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void qRussetWhite_runsAndIsItsOwnScreen(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = built(helper, AT, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), HardwareEra.TRANSITION);
        final PersonalComputerBlockEntity tower = built(helper, BESIDE, ComputingModule.TRANSITION_RUSSET_PRO.get(),
                QuinceItems.BOARD_PRO_2006.get(), HardwareEra.TRANSITION);
        white.togglePower();
        tower.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(white.isRunning(), "the white qRusset runs on the Transition's parts");
                    helper.assertTrue(tower.isRunning(), "and so does the Russet Pro of the Transition");
                    helper.assertTrue(white.shows(white.getBlockPos()) && white.lit(),
                            "an all-in-one shows itself on its own glass, lit while it runs");
                    helper.assertFalse(tower.shows(tower.getBlockPos()) || tower.lit(),
                            "a tower has no glass of its own");
                    final IMonitorPicture glass = MonitorPictures.describe(helper.getLevel(), white);
                    helper.assertFalse(glass instanceof IMonitorPicture.Dark, "and the glass has a picture");
                    helper.assertTrue(white.quinceStart() == QuinceStart.EFI, "it starts as EFI does");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void partsThatMakeNoComputer_showTheSadQuince(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = place(helper, AT, ComputingModule.QRUSSET_WHITE.get());
        final PersonalComputerBlockEntity pc = place(helper, BESIDE,
                ComputingModule.TRANSITION_PERSONAL_COMPUTER.get());
        boardAndSupply(white, QuinceItems.BOARD_QRUSSET_WHITE.get(), HardwareEra.TRANSITION);
        boardAndSupply(pc, HardwareItems.MOTHERBOARD_ATX_TRANSITION_775.get(), HardwareEra.TRANSITION);
        white.togglePower();
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(white.hardwareFault() != null && MonitorBlock.standsAtFault(white),
                            "a Quince machine with no processor stands at its fault");
                    helper.assertTrue(MonitorBlock.entryFor(white, false) == MonitorBlock.Entry.POST,
                            "which its self-test shows");
                    final OpenPostPayload post = SessionOpenings.post(helper.getLevel(), white.getBlockPos(),
                            white.getBlockPos(), white);
                    helper.assertTrue(post.quince().fault() && post.halted(), "as the Sad Quince, standing");
                    helper.assertTrue(white.lit(), "on its own glass");
                    helper.assertFalse(MonitorBlock.standsAtFault(pc), "any other machine so built only beeps");
                    helper.assertTrue(MonitorBlock.entryFor(pc, false) == MonitorBlock.Entry.NO_POWER,
                            "and its screen stays dark");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = LONG_TEST)
    public static void sneakingPowerOn_waitsAtTheStartupChoices(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = built(helper, AT, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), HardwareEra.TRANSITION);
        white.askStartupChoices(true);
        white.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(white.atStartupChoices(),
                        "switched on while sneaking, it opens its start-up choices"))
                .thenWaitUntil(() -> helper.assertTrue(white.postRemaining() == 0, "its self-test runs out"))
                .thenExecuteAfter(PAST_ITS_END, () -> {
                    helper.assertTrue(white.needsPost() && white.atStartupChoices(),
                            "and it waits at the choices past the self-test's end");
                    final OpenPostPayload post = SessionOpenings.post(helper.getLevel(), white.getBlockPos(),
                            white.getBlockPos(), white);
                    helper.assertTrue(post.quince().choices(), "which its glass shows");
                    white.leaveStartupChoices();
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(white.needsPost(),
                        "a choice made, the self-test ends and the machine goes on"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void plainPowerOn_asksForNoChoices(final GameTestHelper helper) {
        final PersonalComputerBlockEntity white = built(helper, AT, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), HardwareEra.TRANSITION);
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                BESIDE);
        white.askStartupChoices(false);
        white.togglePower();
        pc.togglePower();
        pc.askStartupChoices(true);
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(white.atStartupChoices(), "switched on standing, it asks for none");
                    helper.assertFalse(pc.atStartupChoices(), "and a machine with no Quince firmware has none");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void allInOnes_takeTheirCablesAndPowerFromBehind(final GameTestHelper helper) {
        final QuinceComputerBlock white = ComputingModule.QRUSSET_WHITE.get();
        final QuinceComputerBlock tower = ComputingModule.TRANSITION_RUSSET_PRO.get();
        final BlockState facingNorth = white.defaultBlockState();
        final BlockState towerNorth = tower.defaultBlockState();
        final FacePorts aio = white.ports();
        final FacePorts pro = tower.ports();
        final Connection access = DataLines.of(new DataLink(DataLine.ACCESS, HardwareEra.TRANSITION));
        final Connection peripheral = PeripheralLine.of(HardwareEra.TRANSITION);
        helper.assertTrue(aio.accepts(facingNorth, Direction.SOUTH, access), "an all-in-one's access line behind");
        helper.assertFalse(aio.accepts(facingNorth, Direction.NORTH, access), "never on its front");
        helper.assertTrue(aio.accepts(facingNorth, Direction.SOUTH, peripheral), "its peripherals behind");
        helper.assertFalse(aio.accepts(facingNorth, Direction.EAST, peripheral), "never on its side");
        helper.assertTrue(pro.accepts(towerNorth, Direction.EAST, peripheral), "a tower's on any face");
        helper.assertTrue(QuinceMachines.QRUSSET_WHITE.powerBehind() && QuinceMachines.RUSSET_STUDIO.powerBehind(),
                "the all-in-ones and the Studio have their power button behind");
        helper.assertFalse(QuinceMachines.RUSSET_PRO_TRANSITION.powerBehind(), "a tower's is on its front");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void quinceMainframe_takesOnlyItsOwnBoard(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(AT, ComputingModule.QUINCE_MAINFRAME_STANDARD.get());
        final MainframeBlockEntity mainframe = world.blockEntity(AT, MainframeBlockEntity.class);
        final int slot = MainframeBlockEntity.MOTHERBOARD_SLOT;
        helper.assertTrue(mainframe.isValidForSlot(slot, new ItemStack(QuinceItems.BOARD_MAINFRAME_S.get())),
                "the Quince Mainframe takes Quince's logic board for it");
        helper.assertFalse(mainframe.isValidForSlot(slot, new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get())),
                "and not the Mainframe's MTX board");
        helper.assertValueEqual(((MainframeBlock) ComputingModule.QUINCE_MAINFRAME_STANDARD.get()).cabinetModel(),
                "quince_mainframe_standard", "drawn in Quince's cabinet");
        helper.succeed();
    }

    /* Places {@code block} at {@code at}, its front to the north, and gives back its entity. */
    private static PersonalComputerBlockEntity place(final GameTestHelper helper, final BlockPos at,
                                                     final Block block) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(at, block);
        return world.blockEntity(at, PersonalComputerBlockEntity.class);
    }

    /*
     * A machine on {@code board} with only a supply: a board and a supply, and nothing that makes a computer. Each part
     * goes in the slot the machine's own map has for it, a Quince case having no bays to count.
     */
    private static void boardAndSupply(final PersonalComputerBlockEntity machine, final Item board, final Era era) {
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(era)));
    }

    /* A machine built whole on {@code board}, from its era's parts, a system on its disk, switched off. */
    private static PersonalComputerBlockEntity built(final GameTestHelper helper, final BlockPos at, final Block block,
                                                     final Item board, final Era era) {
        final PersonalComputerBlockEntity machine = place(helper, at, block);
        boardAndSupply(machine, board, era);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(map.coolerStart(), new ItemStack(CoolingItems.TRANSITION_STOCK_COOLER.get()));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        machine.installOs(TestWorldBuilder.LEGACY_DESKTOP_OS);
        return machine;
    }
}
