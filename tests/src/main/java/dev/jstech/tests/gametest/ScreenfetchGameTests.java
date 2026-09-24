/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * screenfetch draws each system's real logo in that logo's colours, never wraps a row under itself, keeps
 * FreeBSD's as it has always been, and runs at Frames' Command Prompt too.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ScreenfetchGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final int SETTLE = 2;

    /* The desktop's terminal window as it used to open: fewer columns than the logo and its readout take. */
    private static final int NARROW = 46;

    private ScreenfetchGameTests() {
    }

    /** Ubuntu's logo is red with white lettering, and the readout's user, host and labels are red too. */
    @GameTest(template = ARENA)
    public static void screenfetch_drawsUbuntuInItsOwnColours(final GameTestHelper helper) {
        final MainframeBlockEntity machine = withSystem(helper, "ubuntu");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<CliLine> out = fetch(helper, machine, "apt install screenfetch",
                            TermBuffer.MONITOR_COLUMNS);
                    helper.assertTrue(has(out.get(3), "dMMMNy", CliStyle.BRIGHT),
                            "the lettering inside the ring is white; got " + out.get(3).spans());
                    helper.assertTrue(has(out.get(0), "player", CliStyle.RED),
                            "the user is in the logo's red; got " + out.get(0).spans());
                    helper.assertTrue(has(out.get(2), "OS:", CliStyle.RED),
                            "and so is every label; got " + out.get(2).spans());
                    helper.assertTrue(out.get(2).text().contains("Ubuntu"), "with its value beside it");
                })
                .thenSucceed();
    }

    /** A terminal narrower than the logo and its readout cuts the rows at its edge and never wraps them. */
    @GameTest(template = ARENA)
    public static void screenfetch_cutsAtTheEdgeOfANarrowTerminal(final GameTestHelper helper) {
        final MainframeBlockEntity machine = withSystem(helper, "ubuntu");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<CliLine> out = fetch(helper, machine, "apt install screenfetch", NARROW);
                    helper.assertValueEqual(out.size(), 20, "one row per row of the logo, none added by wrapping");
                    for (final CliLine line : out) {
                        helper.assertTrue(line.text().length() <= NARROW,
                                "no row passes the edge; got " + line.text().length() + ": " + line.text());
                    }
                    helper.assertTrue(out.get(9).text().startsWith("ossyNMMMNyMMhssssssssssssss"),
                            "the logo stays whole; got " + out.get(9).text());
                })
                .thenSucceed();
    }

    /** FreeBSD's is printed as it always was: every row, the readout included, in its one red. */
    @GameTest(template = ARENA)
    public static void screenfetch_leavesFreeBsdAsItWas(final GameTestHelper helper) {
        final MainframeBlockEntity machine = withSystem(helper, "freebsd");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<CliLine> out = fetch(helper, machine, "pkg install screenfetch",
                            TermBuffer.MONITOR_COLUMNS);
                    for (final CliLine line : out) {
                        helper.assertTrue(line.spans().stream().allMatch(span -> span.style() == CliStyle.RED),
                                "each row is all red; got " + line.spans());
                    }
                    helper.assertTrue(out.get(1).text().startsWith("  s` `.....---.......--.```   -/"),
                            "under its own mark; got " + out.get(1).text());
                })
                .thenSucceed();
    }

    /** On Frames it is a package of Frames' own manager, and draws the flag with the Frames version beside it. */
    @GameTest(template = ARENA)
    public static void screenfetch_runsAtFramesCommandPrompt(final GameTestHelper helper) {
        final MainframeBlockEntity machine = withSystem(helper, "frames_xp");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<CliLine> out = fetch(helper, machine, "pckmgr install screenfetch",
                            TermBuffer.MONITOR_COLUMNS);
                    final StringBuilder all = new StringBuilder();
                    for (final CliLine line : out) {
                        all.append(line.text()).append('\n');
                    }
                    helper.assertTrue(all.toString().contains("Kernel: Frames NT 5.1"),
                            "the version Frames XP gives for itself; got " + all);
                    helper.assertTrue(has(out.get(2), "OS:", CliStyle.GREEN),
                            "the labels in the flag's second colour; got " + out.get(2).spans());
                    helper.assertTrue(all.toString().contains("Et:::ztt33EEEL"), "under the flag; got " + all);
                })
                .thenSucceed();
    }

    /* Whether the line has a run that reads those words, in English, in that colour. */
    private static boolean has(final CliLine line, final String words, final CliStyle style) {
        for (final CliSpan span : line.spans()) {
            if (span.style() == style && span.english().equals(words)) {
                return true;
            }
        }
        return false;
    }

    /* Installs screenfetch with that command, from a Mirror on the machine itself, and runs it. */
    private static List<CliLine> fetch(final GameTestHelper helper, final MainframeBlockEntity machine,
                                       final String install, final int columns) {
        final ServerCliComputer cli = new ServerCliComputer(machine, helper.getLevel());
        final CliShell shell = CliCommands.shellFor(cli, columns);
        machine.installMirror();
        shell.run(install, cli);
        TestWorldBuilder.finishSetup(machine, helper.getLevel(), helper.absolutePos(WHERE));
        return shell.run("screenfetch", cli).lines();
    }

    private static MainframeBlockEntity withSystem(final GameTestHelper helper, final String system) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + WHERE);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system))) {
            throw new IllegalStateException("failed to install " + system + " on the test Mainframe");
        }
        return mainframe;
    }
}
