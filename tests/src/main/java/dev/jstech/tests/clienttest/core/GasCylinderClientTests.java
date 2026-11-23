/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.item.ItemStateClient;
import dev.jstech.core.gui.tooltip.TooltipBar;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.world.item.ItemStack;

/**
 * The Gas Cylinder on a player's game: its icon's shoulder, the model's second layer, drawn in its gas's colour by the
 * European code when full of chlorine, in bare steel when empty, its body left in its own colours; and its tooltip's
 * bar in its shoulder's colour.
 */
public final class GasCylinderClientTests {

    private static final int SHOULDER = 1;
    private static final int BODY = 0;
    private static final int UNTINTED = -1;
    private static final long FULL = 10_000L;
    private static final int SETTLE = 10;

    private GasCylinderClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void icon_paintsItsShoulderInItsGasColour(final ClientTestContext ctx) {
        final ItemStack empty = new ItemStack(GasCylinders.CYLINDER.get());
        final ItemStack chlorine = new ItemStack(GasCylinders.CYLINDER.get());
        chlorine.getCapability(MatterCapabilities.GAS_ITEM).insert(new MatterStack(Elements.of(Element.CHLORINE),
                FULL), false);
        ctx.thenGive(0, empty.copy(), chlorine.copy())
                .thenAssert(SETTLE, () -> ctx.mc().getItemColors().getColor(empty, SHOULDER)
                        == GasCylinders.SHOULDERS.get().empty(), "an empty cylinder's shoulder is bare steel")
                .thenAssert(0, () -> ctx.mc().getItemColors().getColor(chlorine, SHOULDER)
                        == GasCylinders.SHOULDERS.get().chlorine(), "a chlorine cylinder's shoulder is yellow")
                .thenAssert(0, () -> ctx.mc().getItemColors().getColor(chlorine, BODY) == UNTINTED,
                        "and its body keeps its own colours")
                .thenScreenshot(SETTLE, "gas-cylinders");
    }

    /**
     * Its tooltip's bar in its shoulder's colour for a gas the European code names, chlorine's yellow, and in the
     * Core's gas colour for one it does not, ammonia.
     */
    @ClientTest(timeoutTicks = 100)
    public static void tooltip_paintsItsBarInItsShouldersColour(final ClientTestContext ctx) {
        final ItemStack chlorine = new ItemStack(GasCylinders.CYLINDER.get());
        final ItemStack ammonia = new ItemStack(GasCylinders.CYLINDER.get());
        ctx.thenAssert(0, () -> ItemStateClient.gasPaint(chlorine, Elements.of(Element.CHLORINE))
                        .equals(new TooltipBar.Paint.Colour(GasCylinders.SHOULDERS.get().chlorine())),
                "a chlorine cylinder's bar is the yellow of its shoulder")
                .thenAssert(0, () -> ItemStateClient.gasPaint(ammonia, SharedCatalogue.AMMONIA)
                        == TooltipBar.Paint.GAS, "an ammonia cylinder's, a gas the code does not name, the Core's");
    }
}
