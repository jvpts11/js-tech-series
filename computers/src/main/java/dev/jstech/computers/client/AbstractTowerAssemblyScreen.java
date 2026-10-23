/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.menu.AbstractAssemblyComputerMenu;
import dev.jstech.computers.operation.payload.RenamePcPayload;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The assembly screen of a tower, the case the Personal, Crafting and Cluster Management Computers share: their name in
 * the header, their state, the power button, the auto start and side panel switches and the three lamps are the same,
 * and each tower says what its readouts show.
 */
public abstract class AbstractTowerAssemblyScreen<M extends AbstractAssemblyComputerMenu>
        extends AbstractMachineAssemblyScreen<M> {

    protected AbstractTowerAssemblyScreen(final M menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected HardwareEra caseEra() {
        final HardwareEra era = menu.hardwareEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected NameField nameField() {
        return new NameField(GameText.component(nameHint()), menu.customName(),
                RenamePcPayload.MAX_LEN, s -> PacketDistributor.sendToServer(new RenamePcPayload(menu.computerPos(), s)));
    }

    @Override
    protected String stateWord() {
        return GameText.resolve(!menu.buildValid() ? AssemblyTexts.OFFLINE
                : menu.isRunning() ? AssemblyTexts.ONLINE : AssemblyTexts.READY);
    }

    @Override
    protected int stateColour() {
        return !menu.buildValid() ? JsTechTheme.red() : menu.isRunning() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    protected List<ServiceSwitch> serviceSwitches() {
        return List.of(
                new ServiceSwitch(GameText.resolve(AssemblyTexts.AUTO_START), menu.isAutoStart(),
                        AbstractAssemblyComputerMenu.BUTTON_AUTOSTART),
                new ServiceSwitch(GameText.resolve(AssemblyTexts.SIDE_PANEL), menu.isSidePanelOff(),
                        AbstractAssemblyComputerMenu.BUTTON_SIDE_PANEL));
    }

    @Override
    protected boolean[] lampsLit() {
        return new boolean[] {menu.isRunning(), menu.diskBusy(), menu.isOnNetwork()};
    }

    @Override
    protected boolean boardIn() {
        return menu.hasBoard();
    }

    @Override
    protected int powerButton() {
        return AbstractAssemblyComputerMenu.BUTTON_POWER;
    }

    @Override
    protected boolean powerActive() {
        return !menu.isAutoStart();
    }

    @Override
    protected int powerLamp() {
        return menu.isRunning() ? JsTechTheme.green() : JsTechTheme.red();
    }

    @Override
    protected Component refusal(final Slot slot, final ItemStack carried) {
        return menu.refusal(slot, carried);
    }

    @Override
    protected List<Component> powerTooltip() {
        if (!menu.buildValid()) {
            return List.of(GameText.component(AssemblyTexts.NOT_A_COMPUTER),
                    GameText.component(AssemblyTexts.NEEDS_PARTS).withStyle(ChatFormatting.GOLD),
                    GameText.component(AssemblyTexts.THEN_SWITCH_ON).withStyle(ChatFormatting.GRAY));
        }
        if (menu.isAutoStart()) {
            return List.of(GameText.component(AssemblyTexts.AUTO_KEEPS_ON).withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    /** The capacity readout every tower shows first: what it moves a tick, lit while it runs. */
    protected Readout capacity(final List<Component> tooltip) {
        return new Readout(GameText.resolve(AssemblyTexts.CAPACITY),
                JsTechTheme.fmt(menu.capacity()) + " " + GameText.resolve(AssemblyTexts.ITEMS_PER_TICK),
                menu.isRunning() ? JsTechTheme.accent() : JsTechTheme.text(), tooltip);
    }

    /** The network readout: linked, lit, or a dash. */
    protected Readout network(final List<Component> tooltip) {
        return new Readout(GameText.resolve(AssemblyTexts.NETWORK),
                menu.isOnNetwork() ? GameText.resolve(AssemblyTexts.LINKED) : "--",
                menu.isOnNetwork() ? JsTechTheme.accent() : JsTechTheme.dim(), tooltip);
    }

    /** What its empty name field says. */
    protected abstract TextKey nameHint();
}
