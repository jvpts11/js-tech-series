/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.client.CaseFrontOutline;
import dev.jstech.computers.client.ComputingClientSetup;
import dev.jstech.computers.client.EjectButtonOutline;
import dev.jstech.computers.client.MachineKeyboard;
import dev.jstech.computers.client.StandingPartOutline;
import dev.jstech.computers.client.guide.ComputersGuideClient;
import dev.jstech.computers.client.monitor.MonitorButtonOutline;
import dev.jstech.computers.client.monitor.MonitorChrome;
import dev.jstech.computers.client.monitor.MonitorPainter;
import dev.jstech.computers.client.monitor.MonitorPictureCache;
import dev.jstech.computers.client.monitor.SessionFaces;
import dev.jstech.computers.client.os.DesktopCopies;
import dev.jstech.computers.client.os.ParkedEditors;
import dev.jstech.computers.config.ComputersClientConfig;
import dev.jstech.core.client.config.CoreConfigScreen;
import dev.jstech.core.client.config.IConfigBadge;
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

    /** The mod's mark on its settings screen: a monitor, its glass framed, on its stand. */
    private static final IConfigBadge MONITOR = (g, x, y, size, colour) -> {
        final int screenHeight = size * 2 / 3;
        g.fill(x, y, x + size, y + 1, colour);
        g.fill(x, y, x + 1, y + screenHeight, colour);
        g.fill(x + size - 1, y, x + size, y + screenHeight, colour);
        g.fill(x, y + screenHeight - 2, x + size, y + screenHeight, colour);
        final int middle = x + size / 2;
        g.fill(middle - 1, y + screenHeight, middle + 1, y + size - 1, colour);
        g.fill(x + size / 4, y + size - 1, x + size - size / 4, y + size, colour);
    };

    public JsComputersClient(IEventBus modEventBus, ModContainer container) {
        // The mod's settings on the Core's settings screen, reached from the mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new CoreConfigScreen(mod, parent, MONITOR));
        // The player's own settings, read on their game only: how the desktops move, and their pointer.
        ComputersClientConfig.register(modEventBus, container);
        // What the mod answers on the player's game: its screens and renderers, the monitors and their glass, the
        // outlines of the machines' fronts, the keys a machine takes first and what is forgotten on leaving a world.
        ComputingClientSetup.listen(modEventBus);
        ComputersGuideClient.listen(modEventBus);
        MachineKeyboard.listen();
        StandingPartOutline.listen();
        CaseFrontOutline.listen();
        EjectButtonOutline.listen();
        MonitorButtonOutline.listen();
        MonitorChrome.listen();
        MonitorPainter.listen();
        MonitorPictureCache.listen();
        SessionFaces.listen();
        DesktopCopies.listen();
        ParkedEditors.listen();
    }
}
