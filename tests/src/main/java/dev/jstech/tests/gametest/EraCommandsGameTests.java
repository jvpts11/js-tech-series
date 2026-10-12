/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.EraKit;
import dev.jstech.tests.testkit.EraNetwork;
import dev.jstech.tests.testkit.JourneyCommands;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * Every command the system of each era's Personal Computer offers, run on that computer on its own network with the
 * line the journeys type for it ({@link JourneyCommands}): each answers without an error.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EraCommandsGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BENCH = "bench";
    private static final int WITHIN = 400;
    private static final int WIDTH = 80;

    private EraCommandsGameTests() {
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void vintage_everyCommandAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.VINTAGE);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void legacy_everyCommandAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.LEGACY);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void transition_everyCommandAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.TRANSITION);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void standard_everyCommandAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.STANDARD);
    }

    @GameTest(template = BENCH, timeoutTicks = WITHIN + 20)
    public static void advanced_everyCommandAnswersWithoutAnError(final GameTestHelper helper) {
        answers(helper, EraKit.ADVANCED);
    }

    /** The lines typed at the era's computer that answer with an error, each with what it said. */
    private static List<String> failures(final ServerCliComputer computer) {
        final List<String> failures = new ArrayList<>();
        for (final String line : JourneyCommands.lines(computer)) {
            final CliShell.Response response = CliCommands.shellFor(computer, WIDTH).run(line, computer);
            final String error = firstError(response.lines());
            LOGGER.info("[JSC-ERA-CMD] {} -> {}", line, error == null ? "ok" : error);
            if (error != null) {
                failures.add(line + ": " + error);
            }
        }
        return failures;
    }

    private static void answers(final GameTestHelper helper, final EraKit kit) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), kit);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(network.computer().isRunning()
                        && network.computer().networkUuid() != null, kit + ": the computer is up on the network"))
                .thenExecute(() -> {
                    final List<String> failures = failures(new ServerCliComputer(network.computer(),
                            helper.getLevel()));
                    helper.assertTrue(failures.isEmpty(), kit + ": " + failures.size() + " commands answer with an"
                            + " error: " + String.join(" | ", failures));
                })
                .thenSucceed();
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
