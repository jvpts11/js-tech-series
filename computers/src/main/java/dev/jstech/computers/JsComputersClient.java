/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.config.ComputersClientConfig;
import dev.jstech.core.client.config.CoreConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only mod entry point for J's Computers.
 */
@Mod(value = JsComputers.MODID, dist = Dist.CLIENT)
public class JsComputersClient {

    public JsComputersClient(IEventBus modEventBus, ModContainer container) {
        // The mod's settings on the Core's settings screen, reached from the mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new);
        // The player's own settings, read on their game only: how the desktops move, and their pointer.
        ComputersClientConfig.register(modEventBus, container);
    }
}
