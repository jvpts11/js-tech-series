/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.menu.AbstractCabinetMenu;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The assembly screen of a cabinet computer, the Mainframe and the WAN Gateway Computer alike: the cabinet's era, its
 * board and cooling faces, the cooling face's readouts with what the cabinet takes away on its own, and its power
 * button, which auto start keeps on.
 *
 * @param <M> the cabinet's menu
 */
public abstract class AbstractCabinetScreen<M extends AbstractCabinetMenu<?>> extends AbstractMachineAssemblyScreen<M> {

    /* Where the cabinet's own share stands among the cooling face's readouts: after the budget. */
    private static final int CABINET_ROW = 3;

    protected AbstractCabinetScreen(final M menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Era caseEra() {
        final Era era = menu.hardwareEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected AssemblyGeometry.Face face() {
        return menu.face();
    }

    @Override
    protected void showFace(final AssemblyGeometry.Face face) {
        menu.showFace(face);
    }

    @Override
    protected int faceButton(final AssemblyGeometry.Face face) {
        return face == AssemblyGeometry.Face.BOARD ? AbstractCabinetMenu.BUTTON_FACE_BOARD
                : AbstractCabinetMenu.BUTTON_FACE_COOLING;
    }

    @Override
    protected boolean coolingSlot(final Slot slot) {
        return menu.coolingSlot(slot);
    }

    @Override
    protected ThermalState thermal() {
        return menu.thermal();
    }

    /** The cooling face's readouts, with what the cabinet takes away on its own between the budget and the air. */
    @Override
    protected List<Readout> coolingReadouts() {
        final List<Readout> rows = new ArrayList<>(super.coolingReadouts());
        final String cabinet = JsTechTheme.fmt(CaseCooling.MAINFRAME.caseWatts());
        rows.add(CABINET_ROW, new Readout(GameText.resolve(AssemblyTexts.CABINET),
                GameText.resolve(AssemblyTexts.WATTS.with(cabinet)), JsTechTheme.text(),
                List.of(GameText.component(AssemblyTexts.CABINET_TIP.with(cabinet)))));
        return rows;
    }

    @Override
    protected boolean boardIn() {
        return menu.hasBoard();
    }

    @Override
    protected int powerButton() {
        return AbstractCabinetMenu.BUTTON_POWER;
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
        final List<Component> failure = psuFailure(menu.psu());
        if (!failure.isEmpty()) {
            return failure;
        }
        final List<Component> hot = tooHot(menu.thermal());
        if (!hot.isEmpty() && !menu.isRunning()) {
            return hot;
        }
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

    /** The service panel's auto start rocker, which every cabinet has first. */
    protected ServiceSwitch autoStartSwitch() {
        return new ServiceSwitch(GameText.resolve(AssemblyTexts.AUTO_START), menu.isAutoStart(),
                AbstractCabinetMenu.BUTTON_AUTOSTART);
    }
}
