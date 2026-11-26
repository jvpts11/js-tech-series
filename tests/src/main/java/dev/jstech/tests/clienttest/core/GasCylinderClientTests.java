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
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.item.ItemGasTank;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.cylinder.GasMarking;
import dev.jstech.core.jstechseries.cylinder.client.MarkGasScreen;
import dev.jstech.core.material.Material;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

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
    private static final int SCREEN_WAIT = 120;

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
     * An empty cylinder used in the air opens the list of the Core's gases; searched and picked, Mark marks the
     * cylinder in the hand for the gas, which paints its shoulder and names it in its tooltip.
     */
    @ClientTest(timeoutTicks = 400)
    public static void marking_fromTheListOfGasesInTheAir(final ClientTestContext ctx) {
        final Material chloride = SeriesChemicals.HYDROGEN_CHLORIDE;
        ctx.thenServer(0, level -> {
                    ctx.serverPlayer().getInventory().clearContent();
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                            new ItemStack(GasCylinders.CYLINDER.get()));
                    final Vec3 at = ctx.serverPlayer().position();
                    ctx.serverPlayer().teleportTo(ctx.serverLevel(), at.x, at.y, at.z, 0.0F, -90.0F);
                })
                .then(SETTLE, () -> ctx.mc().gameMode.useItem(ctx.mc().player, InteractionHand.MAIN_HAND))
                .thenAwaitScreen(MarkGasScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.type("hyd"))
                .thenAssert(SETTLE, () -> ctx.screen(MarkGasScreen.class).shown().contains(chloride)
                        && ctx.screen(MarkGasScreen.class).shown().stream().allMatch(gas -> gas.name().english()
                        .toLowerCase(Locale.ROOT).contains("hyd") || gas.id().getPath().contains("hyd")),
                        "the search narrows the list to the gases it names")
                .then(0, () -> ctx.screen(MarkGasScreen.class).pick(chloride))
                .thenScreenshot(SETTLE, "mark-gas-cylinder")
                .then(0, () -> ctx.screen(MarkGasScreen.class).markPicked())
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenWaitUntilServer(level -> ItemGasTank.markOf(ctx.serverPlayer().getMainHandItem())
                                .equals(Optional.of(chloride)), SCREEN_WAIT, "the cylinder in the hand to be marked",
                        level -> "marked " + ItemGasTank.markOf(ctx.serverPlayer().getMainHandItem()))
                .thenAssert(SETTLE, () -> ctx.mc().getItemColors().getColor(ctx.mc().player.getMainHandItem(),
                        SHOULDER) != GasCylinders.SHOULDERS.get().empty(), "its shoulder is painted for the gas");
        if (ModList.get().isLoaded("jei")) {
            ctx.thenAssert(0, () -> CoreJeiPlugin.gasesListed().orElse(0) == GasMarking.gases().size(),
                    "JEI lists every gas of the Core")
                    .then(0, () -> ctx.mc().setScreen(new InventoryScreen(ctx.mc().player)))
                    .then(SETTLE, () -> CoreJeiPlugin.search("hydrogen"))
                    .thenScreenshot(SETTLE * 2, "jei-gases")
                    .then(0, () -> CoreJeiPlugin.search(""))
                    .then(0, () -> ctx.mc().setScreen(null));
        }
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
