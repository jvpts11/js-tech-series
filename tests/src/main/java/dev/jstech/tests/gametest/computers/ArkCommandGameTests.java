/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.TestShell.text;

/**
 * 67ark at the prompt, as 7-Zip's command line has it: files packed into an archive that weighs less, listed, and
 * taken back out whole, a file already there left as it is; and no command at all until the program is installed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ArkCommandGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* A log, which repeats itself and so packs hard. */
    private static final String LOG = "settled done 64 by Bench (IQL)\n".repeat(40);

    private ArkCommandGameTests() {
    }

    @GameTest(template = ARENA)
    public static void ark_packsListsAndTakesOutWhatItPacked(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(said(cli, "67ark l logs.ark").contains("not found"),
                            "67ark is no command until it is installed");
                    computer.console().install(Programs.ARK.toString());
                    cli.writeFile("ops.txt", LOG);
                    cli.writeFile("notes.txt", "keep the smelter fed");
                    final String packed = said(cli, "67ark a logs.ark ops.txt notes.txt");
                    helper.assertTrue(packed.contains("Vaultis") && packed.contains("Adding  ops.txt")
                            && packed.contains("Adding  notes.txt") && packed.contains("2 files, "),
                            "it says what it added and how much it saved; got " + packed);
                    final String listed = said(cli, "67ark l logs.ark");
                    helper.assertTrue(listed.contains("ops.txt") && listed.contains("notes.txt")
                            && listed.contains("2 files"), "the listing names both and totals them; got " + listed);
                    cli.deleteFile("ops.txt");
                    final String taken = said(cli, "67ark x logs.ark");
                    helper.assertTrue(taken.contains("Extracting  ops.txt"), "it takes the log out; got " + taken);
                    helper.assertTrue(taken.contains("notes.txt is already there"),
                            "and leaves a file already there as it is; got " + taken);
                    helper.assertValueEqual(cli.readFile("ops.txt").message().english(), LOG,
                            "the log comes back whole");
                })
                .thenSucceed();
    }

    /* What the prompt of {@code cli} printed for {@code line}. */
    private static String said(final ServerCliComputer cli, final String line) {
        return text(CliCommands.shellFor(cli, 80).run(line, cli));
    }
}
