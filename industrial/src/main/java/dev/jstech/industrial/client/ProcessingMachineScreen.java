/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.core.client.gui.screen.AbstractMachineScreen;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.industrial.blockentity.ProcessingMachineBlockEntity;
import dev.jstech.industrial.gui.layout.ProcessingMachineLayout;
import dev.jstech.industrial.menu.ProcessingMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.ToIntFunction;

/**
 * The screen of a processing machine: its slots, a progress bar in the machine's own colour and an FE gauge, all
 * placed by the machine's layout, which the menu placed the slots by too.
 */
public class ProcessingMachineScreen extends AbstractMachineScreen<ProcessingMachineMenu> {

    private final ToIntFunction<MachineScreenSupport.Colours> progressColour;
    private final GuiLayout layout = ProcessingMachineLayout.layout();

    public ProcessingMachineScreen(final ProcessingMachineMenu menu, final Inventory inventory,
                                   final Component title,
                                   final ToIntFunction<MachineScreenSupport.Colours> progressColour) {
        super(menu, inventory, title);
        this.progressColour = progressColour;
        this.imageWidth = ProcessingMachineLayout.WIDTH;
        this.imageHeight = ProcessingMachineLayout.HEIGHT;
        this.inventoryLabelY = ProcessingMachineLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final GuiLayout.Box energy = layout.boxAt("energy");
        if (hover(mouseX, mouseY, energy.x(), energy.y(), energy.width(), energy.height())) {
            final ProcessingMachineBlockEntity machine = menu.machine();
            g.renderTooltip(font, Component.literal(machine.getEnergy().getEnergyStored() + " / "
                    + machine.getEnergy().getMaxEnergyStored() + " FE"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final MachineScreenSupport.Colours colours = MachineScreenSupport.colours();
        final ProcessingMachineBlockEntity machine = menu.machine();
        MachineScreenSupport.drawPanel(g, leftPos, topPos, imageWidth, imageHeight);
        drawSlotFrames(g, colours.slotBorder(), colours.slotFill());
        final GuiLayout.Box progress = layout.boxAt("progress");
        MachineScreenSupport.drawProgressBar(g, leftPos + progress.x(), topPos + progress.y(), progress.width(),
                progress.height(), machine.getProgress(), machine.getMaxProgress(),
                progressColour.applyAsInt(colours));
        final GuiLayout.Box energy = layout.boxAt("energy");
        MachineScreenSupport.drawEnergyBar(g, leftPos + energy.x(), topPos + energy.y(), energy.width(),
                energy.height(), machine.getEnergy().getEnergyStored(), machine.getEnergy().getMaxEnergyStored());
    }
}
