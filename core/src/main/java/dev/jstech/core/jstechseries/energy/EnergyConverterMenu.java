/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SyncedValue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * A converter's menu: the player's inventory, what its screen shows of it, and the controls of its face, each a button
 * the server answers: the switch, the direction, the class keys of a configurable converter and the limit's
 * thumbwheels.
 */
public final class EnergyConverterMenu extends CoreMenu {

    private final SyncedValue<ConverterView> view;

    /** The buttons: the switch, the two ways, a class key from the low class, a drum turned up or down, the prefix. */
    public static final int SWITCH = 0;
    public static final int TO_FE = 1;
    public static final int TO_JE = 2;
    public static final int FIRST_KEY = 10;
    public static final int FIRST_DRUM_UP = 20;
    public static final int FIRST_DRUM_DOWN = 30;
    public static final int PREFIX = 40;

    public EnergyConverterMenu(final int containerId, final Inventory inventory,
                               final EnergyConverterBlockEntity converter) {
        super(EnergyConverters.CONVERTER_MENU.get(), containerId, inventory, MenuValidity.blockEntity(converter));
        final PlayerSlots player = playerInventory(inventory, new GuiLayout.SlotPosition(
                EnergyConverterLayout.INVENTORY_X, EnergyConverterLayout.INVENTORY_Y));
        shiftClick(player.main(), player.hotbar());
        shiftClick(player.hotbar(), player.main());
        this.view = synced("view", ConverterView.STREAM_CODEC, ConverterView.NONE, converter::view);
        button(SWITCH, who -> converter.setOn(!converter.on()));
        button(TO_FE, who -> converter.setToJe(false));
        button(TO_JE, who -> converter.setToJe(true));
        for (final VoltageClass each : VoltageClass.values()) {
            button(FIRST_KEY + each.rank() - 1, who -> converter.setVoltage(each));
        }
        for (int drum = 0; drum < ConverterLimit.DRUMS; drum++) {
            final int turned = drum;
            button(FIRST_DRUM_UP + drum, who -> converter.turnDrum(turned, true));
            button(FIRST_DRUM_DOWN + drum, who -> converter.turnDrum(turned, false));
        }
        button(PREFIX, who -> converter.turnPrefix());
    }

    public EnergyConverterMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, EnergyConverterBlockEntity.class));
    }

    /** What the screen shows of the converter, as the server last said. */
    public ConverterView view() {
        return this.view.get();
    }
}
