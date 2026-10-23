/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.api.language.ExecutionBalance;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigValidator;
import dev.jstech.core.config.IConfigLogger;
import dev.jstech.core.config.IConfigValidationResult;
import dev.jstech.core.jstechseries.operation.OperationBalance;
import dev.jstech.tests.JsTests;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What J's Computers' machines lend the Operations engine and how long their programs run, in the mod's own settings
 * file: each defaulting to what the engine holds, each pulled into its range, and each passed into the balance it sets
 * as the file is read. The file is the server's, so the test that reads values into it puts it back as it was.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputersBalanceGameTests {

    private static final String ARENA = "empty";

    private ComputersBalanceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void balance_defaultsToWhatTheEngineHolds(final GameTestHelper helper) {
        same(helper, OperationBalance.DEFAULT_HDD_LATENCY_TICKS,
                ComputersServerConfig.HDD_LATENCY_TICKS.defaultValue(), "the hard disk's latency");
        same(helper, OperationBalance.DEFAULT_SUBFRAME_EFFICIENCY_FACTOR,
                ComputersServerConfig.SUBFRAME_EFFICIENCY_FACTOR.defaultValue(), "the Subframe's share");
        same(helper, ExecutionBalance.DEFAULT_MACHINE_MICROS,
                ComputersServerConfig.PROGRAM_MACHINE_MICROS.defaultValue(), "a machine's time");
        same(helper, ExecutionBalance.DEFAULT_SERVER_MICROS,
                ComputersServerConfig.PROGRAM_SERVER_MICROS.defaultValue(), "the server's time");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_pullsEachSettingIntoItsRange(final GameTestHelper helper) {
        final ConfigValidator validator = new ConfigValidator(IConfigLogger.NOOP);

        final IConfigValidationResult<Integer> latency =
                validator.validate(ComputersServerConfig.HDD_LATENCY_TICKS, 5000);
        final IConfigValidationResult<Integer> deadline =
                validator.validate(ComputersServerConfig.PROGRAM_SERVER_MICROS, 1);
        final IConfigValidationResult<Double> word =
                validator.validate(ComputersServerConfig.SUBFRAME_EFFICIENCY_FACTOR, "fast");

        helper.assertTrue(latency instanceof IConfigValidationResult.Clamped<Integer> && latency.value() == 200,
                "a latency above its range: " + latency);
        helper.assertTrue(deadline instanceof IConfigValidationResult.Clamped<Integer> && deadline.value() == 100,
                "a deadline below its floor: " + deadline);
        helper.assertTrue(word instanceof IConfigValidationResult.Rejected<Double> && word.value() == 0.6,
                "a word for a fraction: " + word);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_passesEachSettingIntoTheBalanceAsTheFileIsRead(final GameTestHelper helper) {
        final Map<String, Object> before = ComputersServerConfig.FILE.toPlain();
        final Map<String, Object> balance = new LinkedHashMap<>();
        balance.put("hdd_latency_ticks", 15);
        balance.put("ssd_latency_ticks", 4);
        balance.put("nvme_latency_ticks", 2);
        balance.put("subframe_efficiency_factor", 0.8);
        balance.put("program_machine_micros", 250);
        balance.put("program_server_micros", 4000);
        try {
            ComputersServerConfig.FILE.read(Map.of(ConfigFile.VERSION_KEY, 1, ComputersServerConfig.BALANCE,
                    balance));

            same(helper, 15, OperationBalance.hddLatencyTicks(), "the hard disk's latency");
            same(helper, 4, OperationBalance.ssdLatencyTicks(), "the SSD's latency");
            same(helper, 2, OperationBalance.nvmeLatencyTicks(), "the NVMe drive's latency");
            same(helper, 0.8, OperationBalance.subframeEfficiencyFactor(), "the Subframe's share");
            same(helper, 250_000L, ExecutionBalance.machineNanos(), "a machine's time");
            same(helper, 4_000_000L, ExecutionBalance.serverNanos(), "the server's time");
        } finally {
            ComputersServerConfig.FILE.read(before);
        }
        helper.succeed();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
