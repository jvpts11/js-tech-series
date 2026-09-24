/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.menu.ReceivingBusMenu;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Configuration screen for the Receiving Bus. All drawing lives in {@link AbstractBusScreen}; this only
 * supplies the title and the filter hint.
 */
public class ReceivingBusScreen extends AbstractBusScreen<ReceivingBusMenu> {

    public ReceivingBusScreen(final ReceivingBusMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected TextKey windowTitle() {
        return BusTexts.RECEIVING_TITLE;
    }

    @Override
    protected TextKey filterHint() {
        return BusTexts.RECEIVING_HINT;
    }
}
