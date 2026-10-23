/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.audio.media.MediaBalance;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigValidator;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.config.IConfigLogger;
import dev.jstech.core.config.IConfigValidationResult;
import dev.jstech.core.jstechseries.operation.OperationBalance;
import dev.jstech.core.jstechseries.operation.OperationSettings;
import dev.jstech.tests.JsTests;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's balance file and the series' Operations engine's: every setting declared, each defaulting to what the
 * engine holds when nothing is set, each pulled into its range, and each passed into the balance it sets when the
 * file is read. The balance is the server's, so a test that reads values into it puts the file back as it was before
 * it ends.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoreConfigKeysGameTests {

    private static final String ARENA = "empty";

    private CoreConfigKeysGameTests() {
    }

    @GameTest(template = ARENA)
    public static void balance_declaresEverySetting(final GameTestHelper helper) {
        same(helper, List.of("media.download_kilobytes_per_second", "media.upload_kilobytes_per_second",
                        "media.max_file_megabytes", "media.player_quota_megabytes", "calendar.days_per_season",
                        "world.chunks_per_owner", "world.chunks_server_budget", "world.chunks_while_away",
                        "world.chunks_away_hours", "protection.team_damage",
                        "protection.audit",
                        "protection.audit_entries", "protection.robots_per_player",
                        "protection.structures_per_player", "protection.edits_per_player", "diagnostics.stall_millis",
                        "diagnostics.count_network", "diagnostics.warn_operators", "elements.discoverers_name",
                        "elements.require_ium", "elements.longest_name", "hazards.radiation",
                        "hazards.natural_radioactivity", "hazards.mobs_suffer", "hazards.lungs",
                        "hazards.body_temperature", "hazards.item_hazards", "hazards.dissonance", "hazards.pollution",
                        "hazards.withering",
                        "explosions.consequences", "explosions.blocks_looked_at", "explosions.blocks_broken",
                        "explosions.show_game_explosions", "fire.remains", "fire.wind", "fire.smoke",
                        "heat.pipes_freeze",
                        "heat.boil_off", "heat.overheating_damages", "heat.ignites",
                        "heat.hot_surfaces_burn", "mechanical.parts_break", "moving_structures.most_blocks",
                        "moving_structures.physics_millis", "moving_structures.split", "moving_structures.gravity",
                        "moving_structures.drag", "moving_structures.lift", "moving_structures.thrust",
                        "moving_structures.buoyancy", "moving_structures.levitation", "moving_structures.magnetism",
                        "moving_structures.wind", "moving_structures.impact_damage",
                        "moving_structures.physics_tool_recipe", "multiblock.blocks_per_second",
                        "multiblock.build_reach", "tools.most_broken", "tools.hardness_margin", "tools.most_placed",
                        "tools.most_copied", "rooms.decompression_pull", "rooms.fire_in_pure_oxygen",
                        "rooms.fire_needs_oxygen", "rooms.mobs_breathe", "rooms.plants_need_air",
                        "rooms.blocks_burst", "rooms.largest_room", "veins.enabled", "veins.rarity", "veins.spread",
                        "veins.density",
                        "veins.without_ore", "veins.try_moving_other_ores", "world_events.enabled",
                        "world_events.frequency", "world_events.disabled", "world_events.harmless"),
                CoreConfigKeys.FILE.keys().stream().map(ConfigKey::dottedPath).toList(), "the settings declared");
        same(helper, "jstech-balance.toml", CoreConfigKeys.FILE.fileName(), "the file's name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void balance_passesEachSettingIntoTheBalanceAsTheFileIsRead(final GameTestHelper helper) {
        final Map<String, Object> before = CoreConfigKeys.FILE.toPlain();
        final Map<String, Object> media = new LinkedHashMap<>();
        media.put("download_kilobytes_per_second", 200);
        media.put("upload_kilobytes_per_second", 100);
        media.put("max_file_megabytes", 0);
        media.put("player_quota_megabytes", 64);
        try {
            CoreConfigKeys.FILE.read(Map.of(ConfigFile.VERSION_KEY, 1, CoreConfigKeys.MEDIA, media));

            same(helper, 200 * 1024 / 20, MediaBalance.downloadBytesPerTick(), "the download rate");
            same(helper, 100 * 1024 / 20, MediaBalance.uploadBytesPerTick(), "the upload rate");
            same(helper, 0L, MediaBalance.maxFileBytes(), "a server whose owner set none takes no recording");
            same(helper, 64L * 1024 * 1024, MediaBalance.playerQuotaBytes(), "a player's quota");
        } finally {
            CoreConfigKeys.FILE.read(before);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operations_declareTheEnginesSettingsInAFileOfTheirOwn(final GameTestHelper helper) {
        same(helper, List.of("engine.waiting_timeout_ticks", "engine.priority_aging_ticks",
                "engine.orphaned_expiry_hours"),
                OperationSettings.FILE.keys().stream().map(ConfigKey::dottedPath).toList(), "the settings declared");
        same(helper, "jstech-operations.toml", OperationSettings.FILE.fileName(), "the file's name");
        same(helper, OperationBalance.DEFAULT_WAITING_TIMEOUT_TICKS,
                OperationSettings.WAITING_TIMEOUT_TICKS.defaultValue(), "the waiting timeout");
        same(helper, OperationBalance.DEFAULT_ORPHANED_OPERATIONS_EXPIRY_HOURS,
                OperationSettings.ORPHANED_EXPIRY_HOURS.defaultValue(), "the expiry");
        final IConfigValidationResult<Integer> waiting = new ConfigValidator(IConfigLogger.NOOP)
                .validate(OperationSettings.WAITING_TIMEOUT_TICKS, 1);
        helper.assertTrue(waiting instanceof IConfigValidationResult.Clamped<Integer> && waiting.value() == 20,
                "a waiting time below its floor: " + waiting);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operations_passEachSettingIntoTheEngineAsTheFileIsRead(final GameTestHelper helper) {
        final Map<String, Object> before = OperationSettings.FILE.toPlain();
        final Map<String, Object> engine = new LinkedHashMap<>();
        engine.put("waiting_timeout_ticks", 600);
        engine.put("priority_aging_ticks", 100);
        engine.put("orphaned_expiry_hours", 2);
        try {
            OperationSettings.FILE.read(Map.of(ConfigFile.VERSION_KEY, 1, OperationSettings.ENGINE, engine));

            same(helper, 600, OperationBalance.waitingTimeoutTicks(), "the waiting timeout");
            same(helper, 100, OperationBalance.priorityAgingTicks(), "the priority aging");
            same(helper, 2 * OperationBalance.TICKS_PER_HOUR, OperationBalance.orphanedOperationsExpiryTicks(),
                    "the expiry");
        } finally {
            OperationSettings.FILE.read(before);
        }
        helper.succeed();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
