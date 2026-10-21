/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.client;

import dev.jstech.core.client.gui.data.DataScreens;
import dev.jstech.core.client.gui.data.IScreenLogic;
import dev.jstech.example.ExampleContent;
import dev.jstech.example.JsExample;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The example mod's half that only a player's game loads: the logbook's screen, declared in a file and needing no
 * code at all, since its one button presses the menu's button on the server. The machines' screens are the Core's.
 */
@Mod(value = JsExample.MODID, dist = Dist.CLIENT)
public final class JsExampleClient {

    /** The logbook screen's file, {@code assets/jsexample/jstech/screens/logbook.json}. */
    public static final ResourceLocation LOGBOOK_SCREEN = JsExample.id("logbook");

    public JsExampleClient(final IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> DataScreens.register(event,
                ExampleContent.LOGBOOK_MENU.get(), LOGBOOK_SCREEN, menu -> IScreenLogic.none()));
    }
}
