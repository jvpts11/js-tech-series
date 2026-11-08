/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.workshop.UpdateCost;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTestServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * The computers of a test run draw no energy: the tests that build a running computer bring no generator, and what
 * they test is not the energy. The few that test it turn the draw on in a batch of their own ({@link #drawing}) and
 * off again when they end, and feed their machines by hand ({@link #feed}, {@link #trickle}); the same goes for what
 * the personal-use cards cost ({@link #costing}), whether power supplies wear ({@link #wearing}) and whether a sudden
 * shutdown loses what is in transit ({@link #losing}). A game started by hand is left as its settings say.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestComputerEnergy {

    /** The batch of the tests that turn the draw on: they run alone, so no other test sees it on. */
    public static final String BATCH = "jsc_computer_energy";
    /** The batch of the tests that make the cards cost levels instead of energy. */
    public static final String XP_BATCH = "jsc_update_cost_xp";
    /** The batch of the tests that let power supplies last for ever. */
    public static final String NO_WEAR_BATCH = "jsc_psu_no_wear";
    /** The batch of the tests that have a sudden shutdown give everything back. */
    public static final String NO_LOSS_BATCH = "jsc_item_loss_off";
    private static final String CLIENT_TESTS = "jstestkit.clienttests";
    /* As much as a store takes from the grid in a tick. */
    private static final long A_TICK = 4_000L;
    /* What a weak line gives in a tick: something, and far less than any computer draws. */
    private static final long A_TRICKLE = 1L;

    private TestComputerEnergy() {
    }

    @SubscribeEvent
    public static void onStarting(final ServerStartingEvent event) {
        if (event.getServer() instanceof GameTestServer || Boolean.getBoolean(CLIENT_TESTS)) {
            drawing(false);
            costing(UpdateCost.ENERGY);
            wearing(true);
            losing(true);
        }
    }

    /** Has a sudden shutdown lose what the Operations carry, or give it back, for {@link #NO_LOSS_BATCH}. */
    public static void losing(final boolean on) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.LOSE_ON_POWER_LOSS, on);
    }

    /** Gives {@code port} a tick's trickle, as a weak generator on an overloaded line does. */
    public static void trickle(final IEnergyPort port) {
        port.receive(A_TRICKLE, false);
    }

    /** Turns power supplies' wear on or off, for the tests of {@link #NO_WEAR_BATCH}. */
    public static void wearing(final boolean on) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.PSU_FAILURES, on);
    }

    /** Turns the computers' draw on or off for the tests of {@link #BATCH}. */
    public static void drawing(final boolean on) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.DRAW_ENERGY, on);
    }

    /** Sets what the personal-use cards cost, for the tests of a batch of their own. */
    public static void costing(final UpdateCost cost) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.UPDATE_COST, cost.serializedName());
    }

    /** Fills {@code port} with a tick's worth from the grid, as a cable at full flow would. */
    public static void feed(final IEnergyPort port) {
        port.receive(A_TICK, false);
    }
}
