/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.menu.ExportBusMenu;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Configuration screen for the Export Bus. All drawing lives in {@link AbstractBusScreen}; this only supplies the title and the filter hint.
 */
public class ExportBusScreen extends AbstractBusScreen<ExportBusMenu> {

    public ExportBusScreen(final ExportBusMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected TextKey windowTitle() {
        return BusTexts.EXPORT_TITLE;
    }

    @Override
    protected TextKey filterHint() {
        return BusTexts.EXPORT_HINT;
    }
}
