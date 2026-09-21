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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The work a machine is left with: a line put in the background, and a line waiting for an hour.
 *
 * <p>What matters here is that the machine really does it on its own tick, with nobody typing: a line left
 * with a computer moves items on the network while the player is elsewhere, which is the whole reason a
 * computer is worth leaving switched on.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineJobGameTests {

    private MachineJobGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    /** A network with something in it, and a machine to type at. */
    private static PersonalComputerBlockEntity wire(final GameTestHelper helper,
                                                    final ResourceLocation system) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(2, 2, 1));
        rack.getServerStorage(0).insert(Items.COBBLESTONE, 2048);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        lab.installOs(system);
        return lab;
    }

    /**
     * A line ending in {@code &} is left with the machine, and the machine runs it on its own tick: by the
     * time anybody looks again it has been done and the job is off the list.
     */
    @GameTest(template = ARENA)
    public static void anAmpersand_leavesTheLineWithTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity lab = wire(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = shell(helper, lab, "interac get 64 cobblestone --to local &");
                    helper.assertTrue(says(said, "[1]"), "it is left as job one; got " + said);
                    helper.assertTrue(lab.console().jobs().all().size() == 1,
                            "and the machine is keeping it; it has " + lab.console().jobs().all());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(lab.console().jobs().isEmpty(),
                            "the machine ran it on its own and the job is done; it still has "
                                    + lab.console().jobs().all());
                    final List<String> none = shell(helper, lab, "jobs");
                    helper.assertTrue(says(none, "no jobs"), "and nothing is left on the list; got " + none);
                })
                .thenSucceed();
    }

    /** The same thing in the other family's words, and its own way of listing and taking one off. */
    @GameTest(template = ARENA)
    public static void start_andAt_areTheSameListInTheDosFamilysWords(final GameTestHelper helper) {
        final PersonalComputerBlockEntity lab = wire(helper, FRAMES_XP);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> started = shell(helper, lab, "START INTERAC LIST");
                    helper.assertTrue(says(started, "Started job"), "START leaves it running; got " + started);

                    final List<String> scheduled =
                            shell(helper, lab, "AT 06:00 /EVERY:M,W,F INTERAC GET 64 COBBLESTONE /LOCAL");
                    helper.assertTrue(says(scheduled, "Added a new job"),
                            "AT leaves it for an hour; got " + scheduled);

                    final List<String> listed = shell(helper, lab, "AT");
                    helper.assertTrue(says(listed, "06:00") && says(listed, "M W F"),
                            "and AT alone lists what it is set to do; got " + listed);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> listed = shell(helper, lab, "AT");
                    helper.assertTrue(says(listed, "06:00"),
                            "the scheduled one stays on the list after the other has run; got " + listed);
                    final int id = lab.console().jobs().all().get(0).id();
                    final List<String> gone = shell(helper, lab, "AT " + id + " /DELETE");
                    helper.assertTrue(says(gone, "Deleted job"), "and one can be taken off; got " + gone);
                    helper.assertTrue(lab.console().jobs().isEmpty(), "the list is empty after");
                })
                .thenSucceed();
    }

    /** What a machine was left with goes with it through a save, or it would be no use leaving anything. */
    @GameTest(template = ARENA)
    public static void whatAMachineWasLeftWith_survivesASave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity lab = wire(helper, DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    shell(helper, lab, "crontab 06:00 interac get 64 cobblestone --to local");
                    helper.assertTrue(lab.console().jobs().all().size() == 1, "one job is on the list");

                    final net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
                    lab.console().save(saved);
                    lab.console().load(saved);

                    helper.assertTrue(lab.console().jobs().all().size() == 1,
                            "and it is still there after a save; the machine has "
                                    + lab.console().jobs().all());
                    final List<String> listed = shell(helper, lab, "crontab -l");
                    helper.assertTrue(says(listed, "06:00") && says(listed, "cobblestone"),
                            "with its hour and its line; got " + listed);
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
