/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.gui.layout.CoalGeneratorLayout;
import dev.jstech.industrial.menu.CoalGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Coal Generator's screen: its fuel slot, a flame that burns down with the fuel and an FE gauge showing the
 * energy made, all placed by the generator's layout, which the menu placed the slot by too.
 */
public class CoalGeneratorScreen extends CoreContainerScreen<CoalGeneratorMenu> {

    private final GuiLayout layout = CoalGeneratorLayout.layout();

    public CoalGeneratorScreen(final CoalGeneratorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CoalGeneratorLayout.WIDTH;
        this.imageHeight = CoalGeneratorLayout.HEIGHT;
        this.inventoryLabelY = CoalGeneratorLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final CoalGeneratorBlockEntity generator = menu.generator();
        MachineScreenSupport.drawEnergyTooltip(g, font, leftPos, topPos, layout.boxAt("energy"),
                generator.getEnergy(), mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final MachineScreenSupport.Colours colours = MachineScreenSupport.colours();
        final CoalGeneratorBlockEntity generator = menu.generator();
        MachineScreenSupport.drawPanel(g, leftPos, topPos, imageWidth, imageHeight);
        drawSlotFrames(g, colours.slotBorder(), colours.slotFill());
        final GuiLayout.Box flame = layout.boxAt("flame");
        final int x = leftPos + flame.x();
        final int y = topPos + flame.y();
        g.fill(x, y, x + flame.width(), y + flame.height(), colours.flameEmpty());
        final int max = generator.getMaxBurnTime();
        final int lit = max > 0 ? generator.getBurnTime() * flame.height() / max : 0;
        if (lit > 0) {
            g.fill(x, y + flame.height() - lit, x + flame.width(), y + flame.height(), colours.flameFull());
        }
        MachineScreenSupport.drawEnergyGauge(g, leftPos, topPos, layout.boxAt("energy"), generator.getEnergy());
    }
}
