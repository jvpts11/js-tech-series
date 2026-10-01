/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The Core's start on a player's game: its settings on NeoForge's settings screen, reached from the mods list, each
 * setting under the name and the tooltip its language file gives it.
 */
@Mod(value = JsCore.MODID, dist = Dist.CLIENT)
public final class JsCoreClient {

    public JsCoreClient(final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
