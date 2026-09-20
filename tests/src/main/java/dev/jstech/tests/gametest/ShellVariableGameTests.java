/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Giving a name a value at the prompt: setting one, reading it back, forgetting it, and finding it still
 * there after the machine has been off.
 *
 * <p>On a real machine because that is the whole point of these: a name belongs to the computer and not to
 * the window it was typed in, so what has to be held to account is that it is written down with everything
 * else the machine remembers and read back when it starts again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShellVariableGameTests {

    private ShellVariableGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private static PersonalComputerBlockEntity machine(final GameTestHelper helper,
                                                       final ResourceLocation system) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        computer.installOs(system);
        return computer;
    }

    /** The POSIX way: export sets it, the shell puts it in, unset takes it away, and a bare line sets one too. */
    @GameTest(template = ARENA)
    public static void export_setsANameThatTheShellThenPutsIn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    shell(helper, computer, "export BASE=/home/player/work");
                    final List<String> said = shell(helper, computer, "echo $BASE/notes.txt");
                    helper.assertTrue(says(said, "/home/player/work/notes.txt"),
                            "the shell put in what the name stands for; got " + said);

                    final List<String> listed = shell(helper, computer, "export");
                    helper.assertTrue(says(listed, "BASE=/home/player/work"),
                            "and the names there are read back; got " + listed);

                    shell(helper, computer, "unset BASE");
                    final List<String> gone = shell(helper, computer, "echo [$BASE]");
                    helper.assertTrue(says(gone, "[]"),
                            "a name that was forgotten comes out as nothing; got " + gone);

                    /* No verb at all, which is how these shells have always set a name. */
                    shell(helper, computer, "TOOLS=/opt/tools");
                    final List<String> bare = shell(helper, computer, "echo $TOOLS");
                    helper.assertTrue(says(bare, "/opt/tools"),
                            "a line that is nothing but a name and a value sets it; got " + bare);
                })
                .thenSucceed();
    }

    /** The DOS way: SET writes it, the shell reads it back between per cent signs, and SET alone lists. */
    @GameTest(template = ARENA)
    public static void set_setsANameOnTheFamilyThatWritesItThatWay(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    shell(helper, computer, "set BASE=C:\\work");
                    final List<String> said = shell(helper, computer, "echo %BASE%\\notes.txt");
                    helper.assertTrue(says(said, "C:\\work\\notes.txt"),
                            "the shell put in what the name stands for; got " + said);

                    final List<String> listed = shell(helper, computer, "set");
                    helper.assertTrue(says(listed, "BASE=C:\\work"),
                            "and SET on its own lists them; got " + listed);

                    shell(helper, computer, "set BASE=");
                    final List<String> gone = shell(helper, computer, "echo [%BASE%]");
                    helper.assertTrue(says(gone, "[]"),
                            "nothing after the equals sign forgets it; got " + gone);
                })
                .thenSucceed();
    }

    /** A name a player set is still there when the machine comes back, which is why it is worth setting. */
    @GameTest(template = ARENA)
    public static void aName_isStillThereAfterTheMachineHasBeenOff(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    shell(helper, computer, "export BASE=/srv/kept");

                    /* Written down and read back the way the world writes a machine down and reads it back. */
                    final CompoundTag written = new CompoundTag();
                    computer.console().save(written);
                    computer.console().clear();
                    helper.assertTrue(says(shell(helper, computer, "export"), "no names"),
                            "the name is gone once the machine has forgotten everything");

                    computer.console().load(written);
                    final List<String> back = shell(helper, computer, "echo $BASE");
                    helper.assertTrue(says(back, "/srv/kept"),
                            "and back when the machine is read again; got " + back);
                })
                .thenSucceed();
    }

    /** A name the player set stands over the one the machine answers for, as it does in every shell. */
    @GameTest(template = ARENA)
    public static void aNameThePlayerSet_standsOverTheOneTheMachineAnswersFor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> before = shell(helper, computer, "echo $USER");
                    helper.assertTrue(says(before, "player"), "the machine answers for it; got " + before);

                    shell(helper, computer, "export USER=admin");
                    final List<String> after = shell(helper, computer, "echo $USER");
                    helper.assertTrue(says(after, "admin"),
                            "and what was set stands over it; got " + after);
                })
                .thenSucceed();
    }

    private static List<String> shell(final GameTestHelper helper, final PersonalComputerBlockEntity on,
                                      final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        final List<String> out = new ArrayList<>();
        for (final CliLine line : CliCommands.shellFor(computer, WIDTH).run(command, computer).lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }
}
