/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What every screen opened on a monitor has round it, whichever screen it is: the power strip on the frame's left.
 * Drawn over the finished screen and asked about a click before the screen is, so no screen can draw over it or take
 * its clicks.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class MonitorChrome {

    private MonitorChrome() {
    }

    @SubscribeEvent
    public static void onRender(final ScreenEvent.Render.Post event) {
        final MonitorFrameStyle.Geometry frame = frameOf(event.getScreen());
        if (frame == null) {
            return;
        }
        final TextKey tip = PowerStrip.render(event.getGuiGraphics(), frame.x(), frame.y(), event.getMouseX(),
                event.getMouseY());
        if (tip != null) {
            event.getGuiGraphics().renderTooltip(event.getScreen().getMinecraft().font, GameText.component(tip),
                    event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onClick(final ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !(event.getScreen() instanceof AbstractContainerScreen<?> container)
                || !(container.getMenu() instanceof IMonitorMenu at)) {
            return;
        }
        final MonitorFrameStyle.Geometry frame = frameOf(event.getScreen());
        if (frame != null && PowerStrip.click(event.getMouseX(), event.getMouseY(), frame.x(), frame.y(),
                at.hostPos(), at.monitorPos())) {
            event.setCanceled(true);
        }
    }

    /* The frame of a screen opened on a monitor, or null for any other screen. */
    @Nullable
    private static MonitorFrameStyle.Geometry frameOf(final Screen screen) {
        if (screen instanceof AbstractComputerScreen<?> computer && computer.getMenu() instanceof IMonitorMenu) {
            return computer.frameBounds();
        }
        if (screen instanceof DesktopScreen desktop) {
            return desktop.frameBounds();
        }
        return null;
    }
}
