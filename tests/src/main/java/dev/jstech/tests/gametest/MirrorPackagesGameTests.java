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
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.TerminalHand;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every package the network's Mirror has for a distribution, installed with that distribution's own package manager:
 * Arch's pacman and Gentoo's emerge, which builds each from its source. Each is on the machine afterwards, and one that
 * brings a command of its own answers to it at the prompt. One that belongs on another kind of machine is refused
 * with the kind it belongs on. A program of windows alone, typed at the prompt of a system with no desktop, cannot
 * open a display; it is opened on a desktop, which is the client's.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MirrorPackagesGameTests {

    private static final String BENCH = "bench";
    private static final int TIMEOUT = 2_000;
    /** How long the computer may take to start on the distribution just put on its disk. */
    private static final int START_WITHIN = 600;
    private static final int WIDTH = 80;
    private static final int TRANSCRIPT = 600;

    private MirrorPackagesGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void arch_installsEveryPackageOfTheMirrorWithPacman(final GameTestHelper helper) {
        every(helper, "arch", "pacman -S ");
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void gentoo_mergesEveryPackageOfTheMirrorWithEmerge(final GameTestHelper helper) {
        every(helper, "gentoo", "emerge ");
    }

    /** Each desktop goes on its distribution from the Mirror with that distribution's package manager. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void desktops_goOnTheirDistributionsFromTheMirror(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        final String[][] pairs = {{"ubuntu", "apt install kde_plasma", "kde_plasma"},
            {"debian", "apt install gnome", "gnome"}, {"fedora", "dnf install cinnamon", "cinnamon"},
            {"arch", "pacman -S cde", "cde"}};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        "the computer is up on the network"))
                .thenExecute(() -> {
                    network.mainframe().installMirror();
                    final List<String> failures = new ArrayList<>();
                    for (final String[] pair : pairs) {
                        computer.togglePower();
                        computer.formatDisk(0);
                        helper.assertTrue(computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                                pair[0])), pair[0] + " installs");
                        computer.togglePower();
                        final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
                        final String said = hand.yes(pair[1]);
                        final ResourceLocation desktop = computer.installedDesktopId();
                        if (desktop == null || !desktop.getPath().equals(pair[2])) {
                            failures.add(pair[0] + ": " + pair[1] + " left the desktop " + desktop + ": "
                                    + said.strip());
                        }
                    }
                    helper.assertTrue(failures.isEmpty(), String.join(" | ", failures));
                })
                .thenSucceed();
    }

    private static void every(final GameTestHelper helper, final String distro, final String manager) {
        final ResourceLocation os = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, distro);
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        "the computer is up on the network"))
                .thenExecute(() -> {
                    network.mainframe().installMirror();
                    computer.togglePower();
                    computer.formatDisk(0);
                    helper.assertTrue(computer.installOs(os), distro + " installs");
                    computer.togglePower();
                })
                .thenExecuteFor(START_WITHIN / 10, () -> { })
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && os.equals(computer.installedOsId()),
                        "the computer starts " + distro))
                .thenExecute(() -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(ShellFamily.POSIX, WIDTH);
                    final List<String> names = new ArrayList<>();
                    for (final ICliComputer.PackageInfo offered : cli.packagesAvailable()) {
                        if (!offered.community() && !offered.installed()
                                && !offered.description().endsWith("(service)")) {
                            names.add(offered.name());
                        }
                    }
                    helper.assertTrue(!names.isEmpty(), "the Mirror has packages for " + distro);
                    final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
                    final List<String> failures = new ArrayList<>();
                    for (final String name : names) {
                        final String said = hand.yes(manager + name);
                        final ProgramSpec spec = specOf(name);
                        final HostScope belongs = spec == null ? HostScope.ANY : spec.hostScope();
                        if (belongs != HostScope.ANY && belongs != HostScope.PERSONAL_COMPUTER) {
                            if (installed(cli, name)) {
                                failures.add(name + " went onto a Personal Computer though it belongs on " + belongs);
                            }
                            continue;
                        }
                        if (!installed(cli, name)) {
                            failures.add(name + " was not installed:\n" + hand.transcript(TRANSCRIPT));
                            continue;
                        }
                        if (shell.find(name) == null) {
                            // A program of windows alone has nowhere to show them while the system has no desktop.
                            final String shown = spec != null && spec.kind() == ProgramKind.APP
                                    && computer.installedDesktopId() == null
                                    ? hand.glance(name) : "cannot open display";
                            if (!shown.contains("cannot open display")) {
                                failures.add(name + " shows itself in windows, and the prompt does not say it"
                                        + " cannot open a display: " + shown.strip());
                            }
                            continue;
                        }
                        final String ran = hand.glance(name);
                        if (ran.contains("command not found")) {
                            failures.add(name + " is installed but its command is not found: " + ran.strip());
                        }
                        // Whatever the program left in front of the terminal is put away before the next one.
                        computer.console().foreground().clear();
                    }
                    helper.assertTrue(failures.isEmpty(), distro + ": " + failures.size() + " of " + names.size()
                            + " packages failed: " + String.join(" | ", failures));
                })
                .thenSucceed();
    }

    /* The program of that command, or null when none goes by it. */
    private static ProgramSpec specOf(final String command) {
        for (final ProgramSpec spec : Programs.all()) {
            if (spec.commandName().equals(command)) {
                return spec;
            }
        }
        return null;
    }

    private static boolean installed(final ServerCliComputer cli, final String name) {
        for (final ICliComputer.PackageInfo offered : cli.packagesAvailable()) {
            if (offered.name().equals(name)) {
                return offered.installed();
            }
        }
        return false;
    }
}
