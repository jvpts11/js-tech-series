/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.industrial.circuit.Circuit;
import dev.jstech.tests.JsTests;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's circuits: one for each tier, the first three made by hand, each known by a tag of its own; making
 * an item of a circuit's tag moves its maker to the circuit's tier, and making anything else moves nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CircuitGameTests {

    private static final String ARENA = "empty";

    private CircuitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void circuits_areOneForEachTierTheFirstThreeByHand(final GameTestHelper helper) {
        final Set<TagKey<Item>> tags = new HashSet<>();
        for (final IndustrialTier tier : IndustrialTier.values()) {
            final Circuit circuit = Circuit.of(tier);
            helper.assertTrue(circuit.tier() == tier, "the circuit of " + tier + " defines it");
            helper.assertTrue(circuit.byHand() == tier.isAtMost(IndustrialTier.T2),
                    circuit + " is made by hand only up to the vacuum tube");
            helper.assertTrue(tags.add(circuit.tag()), circuit + " has a tag of its own: " + circuit.tag());
        }
        helper.assertTrue(Circuit.TRANSISTOR.tag().location().toString().equals("jsindustrial:circuits/transistor"),
                "a circuit's tag is named for it; got " + Circuit.TRANSISTOR.tag());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void circuit_madeMovesItsMakerToItsTier(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID player = UUID.randomUUID();
        // The test mod puts the Structure Void, which nothing makes, in the transistor circuit's tag.
        final ItemStack circuit = new ItemStack(Items.STRUCTURE_VOID);
        helper.assertTrue(circuit.is(Circuit.TRANSISTOR.tag()), "the test's circuit is in the transistor's tag");
        ProgressionTriggers.made(server, player, new ItemStack(Items.STONE));
        helper.assertTrue(PlayerProgress.reached(server, player, SeriesAxes.INDUSTRIAL_TIER) == IndustrialTier.T0,
                "making anything else moves nothing");
        ProgressionTriggers.made(server, player, circuit);
        helper.assertTrue(PlayerProgress.reached(server, player, SeriesAxes.INDUSTRIAL_TIER) == IndustrialTier.T3,
                "making a transistor circuit moves its maker to T3; got "
                        + PlayerProgress.reached(server, player, SeriesAxes.INDUSTRIAL_TIER));
        helper.succeed();
    }
}
