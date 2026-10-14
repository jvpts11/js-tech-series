/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.os.OsCapability;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.JourneyCommands;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * Every command of every system of each era, run on that era's network with the line the journeys type for it
 * ({@link JourneyCommands}): the network's own system on the Mainframe, and each of the era's systems in turn on the
 * Personal Computer, put on its disk as a fresh install. Each answers without an error.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EraCommandsGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BENCH = "bench";
    /** How long a machine may take to start on a system just put on its disk. */
    private static final int START_WITHIN = 600;
    private static final int TIMEOUT = 8_000;
    private static final int WIDTH = 80;

    private EraCommandsGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void vintage_everyCommandOfEverySystemAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.VINTAGE);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void legacy_everyCommandOfEverySystemAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.LEGACY);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void transition_everyCommandOfEverySystemAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.TRANSITION);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void standard_everyCommandOfEverySystemAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void advanced_everyCommandOfEverySystemAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.ADVANCED);
    }

    /** The systems a Personal Computer of {@code kit}'s era runs: every system of the era but the network's own. */
    public static List<ResourceLocation> systems(final EraKit kit) {
        return OsRegistry.oses().stream().filter(os -> os.minEra() == kit.era()
                        && os.capability() != OsCapability.NETWORK_GUI)
                .map(OsDef::id).sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private static void answers(final GameTestHelper helper, final EraKit kit) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), kit);
        final PersonalComputerBlockEntity computer = network.computer();
        final List<String> failures = new ArrayList<>();
        GameTestSequence sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        kit + ": the computer is up on the network"))
                .thenExecute(() -> failures.addAll(failures(helper, network.mainframe(),
                        "the Mainframe's " + EraKit.NETWORK_OS.getPath())));
        for (final ResourceLocation system : systems(kit)) {
            sequence = sequence.thenExecute(() -> {
                        computer.togglePower();
                        computer.formatDisk(0);
                        helper.assertTrue(computer.installOs(system), kit + ": " + system + " installs");
                        computer.togglePower();
                    })
                    .thenExecuteFor(START_WITHIN / 10, () -> { })
                    .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.hasOs()
                                    && system.equals(computer.installedOsId()),
                            kit + ": the computer starts " + system))
                    .thenExecute(() -> failures.addAll(failures(helper, computer, system.getPath())));
        }
        sequence.thenExecute(() -> helper.assertTrue(failures.isEmpty(), kit + ": " + failures.size()
                        + " lines answer with an error: " + String.join(" | ", failures)))
                .thenSucceed();
    }

    /** The lines typed at {@code host} that answer with an error, each with what it said. */
    private static List<String> failures(final GameTestHelper helper, final IComputerTerminalHost host,
                                         final String where) {
        final ServerCliComputer computer = new ServerCliComputer(host, helper.getLevel());
        final List<String> failures = new ArrayList<>();
        for (final String line : JourneyCommands.lines(computer)) {
            final CliShell.Response response = CliCommands.shellFor(computer, WIDTH).run(line, computer);
            final String error = firstError(response.lines());
            LOGGER.info("[JSC-ERA-CMD] {} | {} -> {}", where, line, error == null ? "ok" : error);
            if (error != null) {
                failures.add(where + ": " + line + ": " + error);
            }
        }
        return failures;
    }

    private static String firstError(final List<CliLine> lines) {
        for (final CliLine line : lines) {
            for (final CliSpan span : line.spans()) {
                if (span.style() == CliStyle.ERROR) {
                    return line.text();
                }
            }
        }
        return null;
    }
}
