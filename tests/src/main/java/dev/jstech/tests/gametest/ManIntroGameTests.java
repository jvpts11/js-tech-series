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
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * {@code man intro}: FreeBSD's welcome names it as a way to learn the system, so it has to answer with a
 * real page rather than the generic "no manual entry" every other unknown name gets.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ManIntroGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");

    private ManIntroGameTests() {
    }

    @GameTest(template = ARENA)
    public static void manIntro_printsARealPageOnFreeBsd(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 80);
                    final List<String> page = text(shell.run("man intro", cli));
                    helper.assertFalse(says(page, "No manual entry"),
                            "intro is a real page, not an unknown name; got " + page);
                    helper.assertTrue(says(page, "NAME") && says(page, "DESCRIPTION"),
                            "a real page has its parts; got " + page);
                    helper.assertTrue(says(page, "man") && says(page, "apropos") && says(page, "whatis"),
                            "it points to the other ways of finding a command; got " + page);
                })
                .thenSucceed();
    }

    /** {@code intro} is a page, not a command: typed alone at the prompt, a real FreeBSD answers not found. */
    @GameTest(template = ARENA)
    public static void intro_isNotACommandOnItsOwn(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 80);
                    final List<String> reply = text(shell.run("intro", cli));
                    helper.assertTrue(says(reply, "not found"),
                            "intro alone is not a command a real FreeBSD has; got " + reply);
                })
                .thenSucceed();
    }

    /**
     * FreeBSD's sh has no {@code help}: typed there, it is not found like any other word, and the line says where
     * to look instead, the two ways the welcome names.
     */
    @GameTest(template = ARENA)
    public static void help_isNotFoundOnFreeBsdAndPointsToAproposAndMan(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 80);
                    final List<String> reply = text(shell.run("help", cli));
                    helper.assertTrue(reply.equals(List.of("help: not found. Try: apropos <word>, or man intro")),
                            "help is not a FreeBSD command, and the answer points the way; got " + reply);
                })
                .thenSucceed();
    }

    private static List<String> text(final CliShell.Response response) {
        final List<String> out = new ArrayList<>();
        for (final CliLine line : response.lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }

    /** A powered Mainframe with a full build and a disk, with FreeBSD installed on it. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no MainframeBlockEntity at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(FREEBSD)) {
            throw new IllegalStateException("failed to install FreeBSD on the test Mainframe");
        }
        return mainframe;
    }
}
