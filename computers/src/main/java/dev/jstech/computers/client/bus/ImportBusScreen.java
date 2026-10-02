/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.menu.ImportBusMenu;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Import Bus's window. All of it lives in {@link AbstractBusScreen}; this only names it. */
public class ImportBusScreen extends AbstractBusScreen<ImportBusMenu> {

    public ImportBusScreen(final ImportBusMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected TextKey windowTitle() {
        return BusTexts.IMPORT_TITLE;
    }
}
