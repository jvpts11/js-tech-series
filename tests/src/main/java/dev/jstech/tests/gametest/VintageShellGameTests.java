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
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.menushell.MenuShellListing;
import dev.jstech.computers.program.cli.menushell.MenuShellView;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the machine tells the Vintage text-mode shells: the MC-DOS Shell's folder, tree, drives and programs as DIR
 * reads them, a search by name, PACE's view of the player's home, and the two commands that hand the terminal over,
 * each only on its own system.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class VintageShellGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "mc_dos");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");

    private VintageShellGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dosShell_seesTheRootAsDirDoes(final GameTestHelper helper) {
        final ServerCliComputer shell = shellOn(helper, MC_DOS);
        final MenuShellListing listing = MenuShellListing.read(MenuShellView.answer(shell,
                MenuShellListing.view("C:\\")));
        helper.assertTrue(listing.found() && listing.dir().equals("C:\\"), "the root was read; got " + listing.dir());
        helper.assertTrue(listing.entries().stream().anyMatch(e -> !e.folder() && e.name().equals("AUTOEXEC")
                && e.ext().equalsIgnoreCase("BAT")), "AUTOEXEC.BAT is there, its extension apart");
        helper.assertTrue(listing.entries().stream().anyMatch(e -> e.folder() && e.name().equals("DOS")),
                "and the DOS folder");
        helper.assertTrue(listing.tree().stream().anyMatch(f -> f.equalsIgnoreCase("C:\\DOS")),
                "the tree has the DOS folder, written whole; got " + listing.tree());
        helper.assertTrue(listing.drives().stream().anyMatch(d -> d.letter() == 'C' && d.ready()),
                "drive C is listed and ready");
        helper.assertTrue(!listing.printer(), "no printer is linked");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dosShell_searchFindsFilesByTheirNames(final GameTestHelper helper) {
        final ServerCliComputer shell = shellOn(helper, MC_DOS);
        final MenuShellListing found = MenuShellListing.read(MenuShellView.answer(shell,
                MenuShellListing.search("*.SYS")));
        helper.assertTrue(found.results().stream().anyMatch(r -> r.equalsIgnoreCase("C:\\CONFIG.SYS")),
                "CONFIG.SYS is found, written whole; got " + found.results());
        helper.assertTrue(found.results().stream().noneMatch(r -> r.toUpperCase(Locale.ROOT).endsWith(".BAT")),
                "and nothing that is not like the name asked for");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pace_seesThePlayersHome(final GameTestHelper helper) {
        final ServerCliComputer shell = shellOn(helper, UNIX);
        final MenuShellListing listing = MenuShellListing.read(MenuShellView.answer(shell,
                MenuShellListing.view("/usr/player")));
        helper.assertTrue(listing.found(), "the home was read");
        helper.assertTrue(listing.entries().stream().anyMatch(e -> e.folder() && e.name().equals("Documents")),
                "Documents is in it; got " + listing.entries());
        helper.assertTrue(listing.tree().isEmpty() && listing.drives().isEmpty(),
                "a UNIX has no drive letters and PACE no tree");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shells_handTheTerminalOverOnlyOnTheirOwnSystems(final GameTestHelper helper) {
        final ServerCliComputer dos = shellOn(helper, MC_DOS);
        final CliShell.Response dosShell = CliCommands.shellFor(dos, 80).run("dosshell", dos);
        helper.assertTrue(dosShell.handOver() != null && dosShell.handOver().editor().equals("dosshell")
                && MenuShellListing.names(dosShell.handOver().path()), "DOSSHELL hands MC-DOS's terminal over");
        final CliShell.Response noPace = CliCommands.shellFor(dos, 80).run("pace", dos);
        helper.assertTrue(noPace.handOver() == null, "MC-DOS has no PACE");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pace_handsUnixsTerminalOver(final GameTestHelper helper) {
        final ServerCliComputer unix = shellOn(helper, UNIX);
        final CliShell.Response pace = CliCommands.shellFor(unix, 80).run("pace", unix);
        helper.assertTrue(pace.handOver() != null && pace.handOver().editor().equals("pace"),
                "pace hands the UNIX terminal over");
        final CliShell.Response noShell = CliCommands.shellFor(unix, 80).run("dosshell", unix);
        helper.assertTrue(noShell.handOver() == null, "UNIX has no MC-DOS Shell");
        helper.succeed();
    }

    private static ServerCliComputer shellOn(final GameTestHelper helper, final ResourceLocation os) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(PC, os);
        return new ServerCliComputer(pc, helper.getLevel());
    }
}
