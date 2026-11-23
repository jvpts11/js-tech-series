/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.world.item.ItemStack;

/**
 * The Gas Cylinder's icon on a player's game: its shoulder, the model's second layer, drawn in its gas's colour by
 * the European code when full of chlorine, in bare steel when empty, and its body left in its own colours.
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
}
