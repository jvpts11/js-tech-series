/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.audio.media.MediaBalance;
import dev.jstech.core.config.ConfigValidator;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.config.CoreConfigRegistry;
import dev.jstech.core.config.IConfigLogger;
import dev.jstech.core.config.IConfigValidationResult;
import dev.jstech.core.language.ExecutionBalance;
import dev.jstech.core.operation.OperationBalance;
import dev.jstech.tests.JsTests;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' balance settings: every one declared, each defaulting to what the engine holds when nothing is set,
 * each pulled into its range, and each passed on into the balance it sets. The balance is the server's, so a test
 * that sets it puts it back before it ends.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoreConfigKeysGameTests {

    private static final String ARENA = "empty";

    private CoreConfigKeysGameTests() {
    }

    @GameTest(template = ARENA)
    public static void balance_declaresEverySetting(final GameTestHelper helper) {
        final CoreConfigRegistry registry = CoreConfigKeys.registry();
        same(helper, 13, registry.size(), "the settings declared");
        for (final String path : new String[] {
                "balance.hdd_latency_ticks", "balance.ssd_latency_ticks", "balance.nvme_latency_ticks",
                "balance.operation_waiting_timeout_ticks", "balance.operation_priority_aging_ticks",
                "balance.subframe_efficiency_factor", "balance.orphaned_operations_expiry_hours",
                "balance.program_machine_micros", "balance.program_server_micros",
                "media.download_kilobytes_per_second", "media.upload_kilobytes_per_second",
                "media.max_file_megabytes", "media.player_quota_megabytes"}) {
            helper.assertTrue(registry.isWhitelisted(path), path + " must be declared");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_defaultsToWhatTheEngineHolds(final GameTestHelper helper) {
        same(helper, OperationBalance.DEFAULT_HDD_LATENCY_TICKS, CoreConfigKeys.HDD_LATENCY_TICKS.defaultValue(),
                "the hard disk's latency");
        same(helper, OperationBalance.DEFAULT_WAITING_TIMEOUT_TICKS,
                CoreConfigKeys.OPERATION_WAITING_TIMEOUT_TICKS.defaultValue(), "the waiting timeout");
        same(helper, OperationBalance.DEFAULT_SUBFRAME_EFFICIENCY_FACTOR,
                CoreConfigKeys.SUBFRAME_EFFICIENCY_FACTOR.defaultValue(), "the Subframe's share");
        same(helper, OperationBalance.DEFAULT_ORPHANED_OPERATIONS_EXPIRY_HOURS,
                CoreConfigKeys.ORPHANED_OPERATIONS_EXPIRY_HOURS.defaultValue(), "the expiry");
        same(helper, ExecutionBalance.DEFAULT_MACHINE_MICROS, CoreConfigKeys.PROGRAM_MACHINE_MICROS.defaultValue(),
                "a machine's time");
        same(helper, ExecutionBalance.DEFAULT_SERVER_MICROS, CoreConfigKeys.PROGRAM_SERVER_MICROS.defaultValue(),
                "the server's time");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_pullsEachSettingIntoItsRange(final GameTestHelper helper) {
        final ConfigValidator validator = new ConfigValidator(IConfigLogger.NOOP);

        final IConfigValidationResult<Integer> latency = validator.validate(CoreConfigKeys.HDD_LATENCY_TICKS, 5000);
        final IConfigValidationResult<Integer> deadline = validator.validate(CoreConfigKeys.PROGRAM_SERVER_MICROS, 1);
        final IConfigValidationResult<Double> word =
                validator.validate(CoreConfigKeys.SUBFRAME_EFFICIENCY_FACTOR, "fast");

        helper.assertTrue(latency instanceof IConfigValidationResult.Clamped<Integer> && latency.value() == 200,
                "a latency above its range: " + latency);
        helper.assertTrue(deadline instanceof IConfigValidationResult.Clamped<Integer> && deadline.value() == 100,
                "a deadline below its floor: " + deadline);
        helper.assertTrue(word instanceof IConfigValidationResult.Rejected<Double> && word.value() == 0.6,
                "a word for a fraction: " + word);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_passesEachSettingOnIntoTheBalance(final GameTestHelper helper) {
        try {
            CoreConfigKeys.apply(CoreConfigKeys.HDD_LATENCY_TICKS, 15);
            CoreConfigKeys.apply(CoreConfigKeys.SSD_LATENCY_TICKS, 4);
            CoreConfigKeys.apply(CoreConfigKeys.NVME_LATENCY_TICKS, 2);
            CoreConfigKeys.apply(CoreConfigKeys.OPERATION_WAITING_TIMEOUT_TICKS, 600);
            CoreConfigKeys.apply(CoreConfigKeys.OPERATION_PRIORITY_AGING_TICKS, 100);
            CoreConfigKeys.apply(CoreConfigKeys.SUBFRAME_EFFICIENCY_FACTOR, 0.8);
            CoreConfigKeys.apply(CoreConfigKeys.ORPHANED_OPERATIONS_EXPIRY_HOURS, 2);
            CoreConfigKeys.apply(CoreConfigKeys.PROGRAM_MACHINE_MICROS, 250);
            CoreConfigKeys.apply(CoreConfigKeys.PROGRAM_SERVER_MICROS, 4000);
            CoreConfigKeys.apply(CoreConfigKeys.MEDIA_DOWNLOAD_KILOBYTES_PER_SECOND, 200);
            CoreConfigKeys.apply(CoreConfigKeys.MEDIA_UPLOAD_KILOBYTES_PER_SECOND, 100);
            CoreConfigKeys.apply(CoreConfigKeys.MEDIA_MAX_FILE_MEGABYTES, 0);
            CoreConfigKeys.apply(CoreConfigKeys.MEDIA_PLAYER_QUOTA_MEGABYTES, 64);

            same(helper, 15, OperationBalance.hddLatencyTicks(), "the hard disk's latency");
            same(helper, 4, OperationBalance.ssdLatencyTicks(), "the SSD's latency");
            same(helper, 2, OperationBalance.nvmeLatencyTicks(), "the NVMe drive's latency");
            same(helper, 600, OperationBalance.waitingTimeoutTicks(), "the waiting timeout");
            same(helper, 100, OperationBalance.priorityAgingTicks(), "the priority aging");
            same(helper, 0.8, OperationBalance.subframeEfficiencyFactor(), "the Subframe's share");
            same(helper, 2 * OperationBalance.TICKS_PER_HOUR, OperationBalance.orphanedOperationsExpiryTicks(),
                    "the expiry");
            same(helper, 250_000L, ExecutionBalance.machineNanos(), "a machine's time");
            same(helper, 4_000_000L, ExecutionBalance.serverNanos(), "the server's time");
            same(helper, 200 * 1024 / 20, MediaBalance.downloadBytesPerTick(), "the download rate");
            same(helper, 100 * 1024 / 20, MediaBalance.uploadBytesPerTick(), "the upload rate");
            same(helper, 0L, MediaBalance.maxFileBytes(), "a server whose owner set none takes no recording");
            same(helper, 64L * 1024 * 1024, MediaBalance.playerQuotaBytes(), "a player's quota");
        } finally {
            OperationBalance.reset();
            ExecutionBalance.reset();
            MediaBalance.reset();
        }
        helper.succeed();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
