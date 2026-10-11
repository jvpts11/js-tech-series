/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.CraftingComputerLayout;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Crafting Computer's assembly screen: its era's tower case, and on its front panel what it moves a tick, what its
 * Crafting Cards add (how many stages of a craft they run at once and how much faster, or that it has none), how full
 * its recipe ROM is and whether it is on a network.
 */
public class CraftingComputerScreen extends AbstractTowerAssemblyScreen<CraftingComputerMenu> {

    public CraftingComputerScreen(final CraftingComputerMenu menu, final Inventory inventory,
                                  final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return CraftingComputerLayout.geometry(caseEra());
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_CRAFTING);
    }

    @Override
    protected TextKey nameHint() {
        return AssemblyTexts.NAME_THIS_COMPUTER;
    }

    @Override
    protected List<Readout> readouts() {
        final int factor = menu.craftFactorX100();
        /*
         * The Crafting Card is an accelerator with two stats: how many of a craft's stages this computer runs at once,
         * summed over its cards, and how much faster than its processor; what that comes to a tick is its tooltip.
         */
        final Readout craft = factor > 0
                ? new Readout(GameText.resolve(AssemblyTexts.CRAFT),
                        GameText.resolve(AssemblyTexts.CRAFT_THREADS.with(menu.craftThreads(), formatFactor(factor))),
                        JsTechTheme.accent(), List.of(GameText.component(AssemblyTexts.CRAFT_THROUGHPUT.with(
                                JsTechTheme.fmt(menu.craftThroughput())))))
                : new Readout(GameText.resolve(AssemblyTexts.CRAFT), GameText.resolve(AssemblyTexts.NO_CARD),
                        JsTechTheme.amber());
        return List.of(capacity(List.of()), craft,
                new Readout(GameText.resolve(AssemblyTexts.RECIPE_ROM),
                        GameText.resolve(AssemblyTexts.OF.with(menu.romUsed(), menu.romLimit())), JsTechTheme.text()),
                network(List.of()));
    }

    private static String formatFactor(final int factorX100) {
        if (factorX100 % 100 == 0) {
            return Integer.toString(factorX100 / 100);
        }
        return String.format(Locale.ROOT, "%.1f", factorX100 / 100.0);
    }
}
