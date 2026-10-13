/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.progression.AxisStepReachedEvent;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionGate;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's progression: the eras and the tiers are axes a player moves along, only ever forward, each move told on
 * the game's bus once; a gate passes for a player who has reached its step, and reads from a data file.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {

    private static final String ARENA = "empty";

    private ProgressionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void axes_haveTheSeriesTwoRegistered(final GameTestHelper helper) {
        helper.assertTrue(ProgressionAxes.byId(ProgressionAxes.HARDWARE_ERA.id()) == ProgressionAxes.HARDWARE_ERA,
                "the eras are an axis");
        helper.assertTrue(ProgressionAxes.INDUSTRIAL_TIER.byName("t3") == IndustrialTier.T3,
                "the tiers are an axis, written t0 to t9");
        helper.assertTrue(ProgressionAxes.HARDWARE_ERA.byName("legacy") == HardwareEra.LEGACY,
                "an era is found by its saved name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reach_movesAPlayerOnlyForwardAndSaysSoOnce(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID player = UUID.randomUUID();
        final List<AxisStepReachedEvent> heard = new ArrayList<>();
        final Consumer<AxisStepReachedEvent> listener = event -> {
            if (event.player().equals(player)) {
                heard.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(AxisStepReachedEvent.class, listener);
        try {
            helper.assertTrue(PlayerProgress.reached(server, player, ProgressionAxes.HARDWARE_ERA)
                    == HardwareEra.VINTAGE, "a player starts at the first era");
            helper.assertTrue(PlayerProgress.reach(server, player, ProgressionAxes.HARDWARE_ERA,
                    HardwareEra.TRANSITION), "reaching the Transition moves them on");
            helper.assertTrue(!PlayerProgress.reach(server, player, ProgressionAxes.HARDWARE_ERA,
                    HardwareEra.LEGACY), "reaching an era already passed does nothing");
            helper.assertTrue(PlayerProgress.reached(server, player, ProgressionAxes.HARDWARE_ERA)
                    == HardwareEra.TRANSITION, "they stay at the furthest");
            helper.assertTrue(heard.size() == 1 && heard.getFirst().from() == HardwareEra.VINTAGE
                    && heard.getFirst().to() == HardwareEra.TRANSITION, "the move was told once; heard " + heard);
            helper.assertTrue(PlayerProgress.reached(server, player, ProgressionAxes.INDUSTRIAL_TIER)
                    == IndustrialTier.T0, "each axis is its own");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gate_passesOnceItsStepIsReachedAndReadsFromAFile(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID player = UUID.randomUUID();
        final ProgressionGate gate = ProgressionGate.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"axis\": \"jscore:industrial_tier\", \"step\": \"t4\"}")).getOrThrow();
        helper.assertTrue(gate.resolves() && !gate.passes(server, player), "the gate is shut at T0");
        PlayerProgress.set(server, player, ProgressionAxes.INDUSTRIAL_TIER, IndustrialTier.T5);
        helper.assertTrue(gate.passes(server, player), "and open past T4");
        final ProgressionGate missing = new ProgressionGate(ResourceLocation.fromNamespaceAndPath("nobody", "axis"),
                "one");
        helper.assertTrue(!missing.resolves() && !missing.passes(server, player),
                "a gate on an axis nobody registered stays shut");
        helper.succeed();
    }
}
