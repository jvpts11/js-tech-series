/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.program.DesktopShellPayloads;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.tests.JsTests;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A tool running in front of a machine's terminal, on a real machine ticking in a real world.
 *
 * <p>How a tool plays its script is held to account without the game. What these are for is the machine's
 * half: that it moves the tool along by itself, that the work is done at the end and not at the start, that
 * Ctrl+C leaves it not done, and that a tool written down in a save is found again and finishes when it would
 * have.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TerminalToolGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final int WIDTH = 64;

    /** How long the test tool works for, in ticks. */
    private static final int WORK = 20;

    /** Long enough for a server just mounted in a rack to be a running machine. */
    private static final int SETTLE = 8;

    private static final ResourceLocation MC_NET = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");

    /** The machines whose tool ran to its end, by the name each answers to, since a tool has nowhere to say. */
    private static final Set<String> FETCHED = ConcurrentHashMap.newKeySet();

    static {
        CliCommands.register(new FetchForTests());
    }

    private TerminalToolGameTests() {
    }

    @GameTest(template = ARENA)
    public static void aTool_holdsTheTerminalAndDoesItsWorkAtTheEnd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        if (computer == null) {
            return;
        }
        final TerminalTools.Turn opening = start(helper, computer);
        helper.assertTrue(opening.keyboard().busy(), "the tool has the keyboard, not the prompt");
        helper.assertTrue(text(opening).contains("fetching"), "and it said what it says at once: " + text(opening));
        helper.assertTrue(computer.console().foreground().running(), "the machine has a tool in front");
        helper.assertFalse(FETCHED.contains(idOf(helper, computer)), "and nothing is fetched at the start");

        final TerminalTools.Turn ignored = TerminalTools.typed(computer, helper.getLevel(), "lsblk");
        helper.assertTrue(ignored != null && ignored.lines().isEmpty(),
                "a line typed at a tool that asked nothing goes nowhere, as at a real terminal");

        helper.startSequence()
                .thenExecuteAfter(WORK / 2, () -> helper.assertFalse(FETCHED.contains(idOf(helper, computer)),
                        "half way through it has still fetched nothing"))
                .thenExecuteAfter(WORK, () -> {
                    helper.assertTrue(FETCHED.contains(idOf(helper, computer)),
                            "the machine moved the tool along by itself, to the end");
                    helper.assertFalse(computer.console().foreground().running(), "and the prompt is back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void interrupt_leavesTheWorkNotDone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        if (computer == null) {
            return;
        }
        start(helper, computer);
        helper.startSequence()
                .thenExecuteAfter(WORK / 2, () -> {
                    final TerminalTools.Turn stopped =
                            TerminalTools.typed(computer, helper.getLevel(), DesktopShellPayloads.INTERRUPT);
                    helper.assertTrue(stopped != null && stopped.ended(), "Ctrl+C ends it");
                    helper.assertTrue(text(stopped).contains("^C"), "and says so: " + text(stopped));
                    helper.assertFalse(stopped.keyboard().busy(), "the prompt is back at once");
                })
                .thenExecuteAfter(WORK * 2, () -> helper.assertFalse(FETCHED.contains(idOf(helper, computer)),
                        "and what it had not finished stays not done, however long it is left"))
                .thenSucceed();
    }

    /**
     * A tool written down in a save is found again and finishes when it would have.
     *
     * <p>What is written down is how the tool was made, not the tool: the line and the tick. Nothing it was
     * run for has happened yet, so typing the line again where nobody can see makes the same tool.
     */
    @GameTest(template = ARENA)
    public static void aTool_writtenDownInASave_isFoundAgainAndFinishes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        if (computer == null) {
            return;
        }
        start(helper, computer);
        helper.startSequence()
                .thenExecuteAfter(WORK / 2, () -> {
                    final CompoundTag saved = new CompoundTag();
                    computer.console().save(saved);
                    computer.console().load(saved);
                    helper.assertTrue(computer.console().foreground().running(), "it is written down as running");
                    helper.assertTrue(computer.console().foreground().tool() == null,
                            "though the tool itself did not survive the save");
                })
                .thenExecuteAfter(WORK, () -> {
                    helper.assertTrue(FETCHED.contains(idOf(helper, computer)),
                            "found again and carried on from where it was, to the end");
                    helper.assertFalse(computer.console().foreground().running(), "and the prompt is back");
                })
                .thenSucceed();
    }

    /**
     * A server mounted in a rack is a machine like any other: what is left running in front of its terminal
     * runs to its end by itself, with nobody at the rack.
     */
    @GameTest(template = ARENA)
    public static void aServerInARack_movesItsToolAlongByItself(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(WHERE) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no rack at " + WHERE);
        }
        rack.getServers().setStackInSlot(0, ComputingModule.defaultServer());
        rack.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(rack.installOs(MC_NET), "the server takes a system");
                    helper.assertTrue(start(helper, rack).keyboard().busy(), "the tool has the server's keyboard");
                })
                .thenExecuteAfter(WORK / 2, () -> helper.assertFalse(FETCHED.contains(idOf(helper, rack)),
                        "half way through it has fetched nothing"))
                .thenExecuteAfter(WORK, () -> {
                    helper.assertTrue(FETCHED.contains(idOf(helper, rack)),
                            "the rack moved its server's tool along, to the end");
                    helper.assertFalse(rack.consoleOf(0).foreground().running(), "and the prompt is back");
                })
                .thenSucceed();
    }

    private static TerminalTools.Turn start(final GameTestHelper helper, final IComputerTerminalHost computer) {
        FETCHED.remove(idOf(helper, computer));
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        final CliShell.Response response = CliCommands.shellFor(cli, WIDTH).run(FetchForTests.NAME, cli);
        if (response.started() == null) {
            helper.fail("the test command did not leave a tool running");
        }
        return TerminalTools.started(computer, helper.getLevel(), FetchForTests.NAME, response.started());
    }

    private static String idOf(final GameTestHelper helper, final IComputerTerminalHost computer) {
        return new ServerCliComputer(computer, helper.getLevel()).nodeId();
    }

    private static String text(final TerminalTools.Turn turn) {
        final StringBuilder out = new StringBuilder();
        for (final var line : turn.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** A running Legacy machine, which is all a tool needs to run on. */
    private static PersonalComputerBlockEntity machine(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no legacy personal computer at " + WHERE);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.togglePower();
        return computer;
    }

    /** A tool that fetches nothing, for a while, and says it has: enough of one to hold a terminal. */
    private static final class FetchForTests implements ICliCommand {

        private static final String NAME = "jstests-fetch";

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public String summary() {
            return "A tool that holds the terminal for a while, for the tests.";
        }

        @Override
        public void run(final CliContext context) {
            final String at = context.computer().nodeId();
            context.out().start(new TtyScriptProcess(TtyScript.script()
                    .say("fetching")
                    .flood(WORK, WORK * 2, index -> CliLine.plain("part " + index))
                    .effect(() -> FETCHED.add(at))
                    .say("fetched")
                    .done()));
        }
    }
}
