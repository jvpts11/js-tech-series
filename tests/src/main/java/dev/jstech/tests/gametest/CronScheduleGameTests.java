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
import dev.jstech.computers.program.job.JobWhen;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Locale;

/**
 * The scheduler's own switch: with it off, a job on a schedule stays where it was left instead of running at
 * its hour, and turning it back on lets the same job run the next time its hour comes round. A line put in the
 * background with {@code &} is not the scheduler's own and is not covered here; {@link MachineJobGameTests}
 * covers that one and the job list itself.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CronScheduleGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final int WIDTH = 80;

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    private CronScheduleGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cronOff_leavesAScheduledJobWaitingAndCronOnRunsItAgain(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(new BlockPos(2, 2, 2), DEBIAN);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    /*
                     * Scheduled at the hour the shared GameTest level's clock is already showing, rather than
                     * setting that clock itself: other tests running in the same batch keep their own jobs (a
                     * crontab set for another hour) off this one's world time.
                     */
                    final int hour = JobWhen.hourOf(helper.getLevel().getDayTime());
                    computer.console().settings().setCronEnabled(false);
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    CliCommands.shellFor(cli, WIDTH).run(
                            String.format(Locale.ROOT, "crontab %02d:00 echo hi", hour), cli);
                    helper.assertTrue(computer.console().jobs().all().size() == 1, "the job is on the schedule");
                    helper.assertTrue(computer.console().jobs().all().get(0).lastRun() < 0, "not run yet");
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(computer.console().jobs().all().get(0).lastRun() < 0,
                            "cron off leaves a scheduled job waiting rather than running it, however many "
                                    + "ticks pass, and the job stays on the list rather than being dropped");
                    computer.console().settings().setCronEnabled(true);
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(computer.console().jobs().all().get(0).lastRun() >= 0,
                            "switching cron back on lets the machine run the job that was waiting");
                })
                .thenSucceed();
    }
}
