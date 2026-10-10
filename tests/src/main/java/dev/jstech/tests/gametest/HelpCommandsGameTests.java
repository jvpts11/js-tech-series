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
import dev.jstech.computers.gui.help.HelpViews;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.man.ManualEntries;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.TestShell.text;

/**
 * The help each system has for the manuals of the series, worked from its prompt: MC-DOS's and MC-NET's HELP taking
 * the whole screen, info on a Linux distribution, man finding a manual's entry on UNIX and FreeBSD, and the Help
 * program every desktop bundles under the name its system gave it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HelpCommandsGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final int WIDTH = 80;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final ResourceLocation MC_NET = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_net");
    private static final ResourceLocation DEBIAN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FREEBSD = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "freebsd");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    private static final ResourceLocation HELP = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "help_viewer");

    /** What each desktop calls its help, which is what its launcher shows. */
    private static final Map<String, String> NAMES = Map.of("frames_95", "Help", "frames_xp", "Help and Support",
            "frames_7", "Help and Support", "frames_10", "Get Help", "frames_11", "Get Help",
            "kde_plasma", "Help Center", "gnome", "Help", "cinnamon", "Help", "cde", "Help Viewer");

    private HelpCommandsGameTests() {
    }

    /** HELP on the DOS family's two systems gives the terminal to the full-screen help, opened on what was typed. */
    @GameTest(template = ARENA)
    public static void help_takesTheWholeScreenOnMcDosAndMcNet(final GameTestHelper helper) {
        final MainframeBlockEntity dos = mainframe(helper, new BlockPos(2, 2, 2), MC_DOS);
        final MainframeBlockEntity net = mainframe(helper, new BlockPos(6, 2, 2), MC_NET);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final CliShell.Response plain = run(helper, dos, "help");
                    helper.assertTrue(plain.handOver() != null && plain.handOver().editor().equals("help"),
                            "MC-DOS's HELP takes the terminal; got " + plain.handOver());
                    helper.assertTrue(plain.handOver().path().equals(HelpViews.dos("mc_dos", "")),
                            "on its contents; got " + plain.handOver().path());
                    helper.assertTrue(plain.lines().isEmpty(), "and prints nothing behind it; got " + plain.lines());

                    final CliShell.Response dir = run(helper, dos, "help dir");
                    helper.assertTrue(dir.handOver() != null && HelpViews.dosTopic(dir.handOver().path())
                            .equals("dir"), "HELP DIR opens on DIR; got " + dir.handOver());

                    final CliShell.Response onNet = run(helper, net, "help graphics_cards");
                    helper.assertTrue(onNet.handOver() != null
                                    && onNet.handOver().path().equals(HelpViews.dos("mc_net", "graphics_cards")),
                            "MC-NET's help names its own system in its title; got " + onNet.handOver());
                })
                .thenSucceed();
    }

    /** info is the Linux distributions' own, and opens on the node named after it. */
    @GameTest(template = ARENA)
    public static void info_opensOnLinuxAndNowhereElse(final GameTestHelper helper) {
        final MainframeBlockEntity linux = mainframe(helper, new BlockPos(2, 2, 2), DEBIAN);
        final MainframeBlockEntity freeBsd = mainframe(helper, new BlockPos(6, 2, 2), FREEBSD);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final CliShell.Response opened = run(helper, linux, "info graphics-cards");
                    helper.assertTrue(opened.handOver() != null && opened.handOver().editor().equals("info"),
                            "info takes the terminal on Linux; got " + opened.handOver());
                    helper.assertTrue(opened.handOver().path().equals(HelpViews.info("graphics-cards")),
                            "on the node typed; got " + opened.handOver().path());

                    final CliShell.Response elsewhere = run(helper, freeBsd, "info graphics-cards");
                    helper.assertTrue(elsewhere.handOver() == null, "FreeBSD has no info; got "
                            + elsewhere.handOver());
                })
                .thenSucceed();
    }

    /**
     * man on UNIX and FreeBSD opens a manual's entry in the pager when no command and no topic has the name, by the
     * last part of the entry's id or by its English title; a command's page still comes first, and Linux's man,
     * whose system reads the manuals through info, does not find them.
     */
    @GameTest(template = ARENA)
    public static void man_opensTheManualsEntriesOnUnixAndFreeBsd(final GameTestHelper helper) {
        final MainframeBlockEntity freeBsd = mainframe(helper, new BlockPos(2, 2, 2), FREEBSD);
        final MainframeBlockEntity unix = mainframe(helper, new BlockPos(6, 2, 2), UNIX);
        final MainframeBlockEntity linux = mainframe(helper, new BlockPos(10, 2, 2), DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final CliShell.Response entry = run(helper, freeBsd, "man graphics-cards");
                    helper.assertTrue(entry.handOver() != null && entry.handOver().editor().equals("man"),
                            "FreeBSD's man pages the entry; got " + entry.handOver());
                    helper.assertTrue(entry.handOver().path().equals(HelpViews.manual("jsc:graphics_cards")),
                            "the Computers' entry on graphics cards; got " + entry.handOver().path());

                    final CliShell.Response titled = run(helper, unix, "man Mainframes");
                    helper.assertTrue(titled.handOver() != null
                                    && titled.handOver().path().equals(HelpViews.manual("jsc:mainframes")),
                            "UNIX finds an entry by its title; got " + titled.handOver());

                    final CliShell.Response command = run(helper, freeBsd, "man ls");
                    helper.assertTrue(command.handOver() == null && text(command).contains("LS(1)"),
                            "a command's page is still printed; got " + text(command));

                    final CliShell.Response onLinux = run(helper, linux, "man graphics-cards");
                    helper.assertTrue(onLinux.handOver() == null && text(onLinux).contains("No manual entry"),
                            "Linux's man leaves the manuals to info; got " + text(onLinux));
                })
                .thenSucceed();
    }

    /** The machine finds every mod's entries by the name a player types, however its gaps are written. */
    @GameTest(template = ARENA)
    public static void manualEntries_findEveryModsEntriesByNameOrTitle(final GameTestHelper helper) {
        helper.assertTrue(ManualEntries.find("graphics_cards").equals(Optional.of("jsc:graphics_cards")),
                "by its id's last part");
        helper.assertTrue(ManualEntries.find("Graphics Cards (GPU)").equals(Optional.of("jsc:graphics_cards")),
                "by its title");
        helper.assertTrue(ManualEntries.find("coal-generator").equals(Optional.of("jsindustrial:coal_generator")),
                "an Industrial entry too");
        helper.assertTrue(ManualEntries.find("no such page").isEmpty(), "and nothing for a name nobody has");
        helper.succeed();
    }

    /** Every desktop bundles its help, under the name its system gave it. */
    @GameTest(template = ARENA)
    public static void helpViewer_isBundledOnEveryDesktopUnderItsOwnName(final GameTestHelper helper) {
        final ProgramSpec help = OsRegistry.getProgram(HELP);
        helper.assertTrue(help != null, "the help program is registered");
        for (final DesktopEnvironmentDef desktop : OsBootstrap.builtinDesktops()) {
            helper.assertTrue(desktop.bundles(HELP), desktop.id() + " bundles its help");
            final String expected = NAMES.get(desktop.id().getPath());
            helper.assertTrue(desktop.nameOf(help).english().equals(expected),
                    desktop.id() + " calls it " + expected + "; got " + desktop.nameOf(help).english());
        }
        helper.succeed();
    }

    private static CliShell.Response run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String line) {
        final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
        return CliCommands.newShell(cli.shellFamily(), WIDTH).run(line, cli);
    }

    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos pos,
                                                  final ResourceLocation osId) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no MainframeBlockEntity at " + pos);
        }
        TestWorldBuilder.installMainframeBuild(mainframe);
        mainframe.uninstallOs();
        if (!mainframe.installOs(osId)) {
            throw new IllegalStateException("failed to install " + osId + " on the test Mainframe");
        }
        mainframe.togglePower();
        return mainframe;
    }
}
