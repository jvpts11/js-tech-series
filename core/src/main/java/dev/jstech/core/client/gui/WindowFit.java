/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui;

import com.mojang.blaze3d.platform.Window;
import dev.jstech.core.client.gui.screen.IPanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps a screen of the series inside the window. While a container screen of the series whose image, or a panel
 * screen ({@link IPanelScreen}) whose panel, is bigger than the window at the player's interface scale is open, the
 * scale is lowered just enough for it to fit, by a fraction if need be (3 to 2.95, say), and put back as it closes.
 * The mouse, the slots and the tooltips follow the scale by themselves, as they do when a player changes it. A small
 * window, or a laptop's 1366 by 768 at scale 3, would otherwise cut off a screen's edges, its inventory's last row
 * among them. Screens of other mods and of the game are left as they are.
 */
public final class WindowFit {

    /* The package of every screen of the series and of the Core's kits, which its subclasses in other mods extend. */
    private static final String SERIES = "dev.jstech.";
    /* Two scales this close are one. */
    private static final double SAME = 1.0E-3;
    /* The screen fitted now, if any, and whether a fit is being laid out, so its new layout is not fitted again. */
    private static @Nullable Screen fitted;
    private static boolean fitting;

    private WindowFit() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> fit(event.getScreen()));
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, event -> closing(event.getScreen()));
        // A screen may grow without being laid out again, a tab of another height chosen, so it is looked at each tick.
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** The scale the interface is drawn at when nothing is fitted: the player's choice, as the game works it out. */
    public static double chosenScale() {
        final Minecraft game = Minecraft.getInstance();
        return game.getWindow().calculateScale(game.options.guiScale().get(), game.isEnforceUnicode());
    }

    /* Lowers the scale until {@code screen} fits, or puts back the chosen one when it fits at it. */
    private static void fit(final Screen screen) {
        final Minecraft game = Minecraft.getInstance();
        final Window window = game.getWindow();
        final Size size = sizeOf(screen);
        if (fitting || window.getWidth() <= 0 || window.getHeight() <= 0 || size == null && fitted == null) {
            return;
        }
        final double chosen = chosenScale();
        final double scale = size == null ? chosen : Math.min(chosen, Math.min(
                (double) window.getWidth() / size.width(), (double) window.getHeight() / size.height()));
        if (Math.abs(scale - window.getGuiScale()) >= SAME) {
            fitting = true;
            try {
                window.setGuiScale(scale);
                screen.resize(game, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            } finally {
                fitting = false;
            }
        }
        fitted = chosen - scale >= SAME ? screen : null;
    }

    /* The fitted screen closing: the chosen scale back, for whatever shows next, without laying the closing one out. */
    private static void closing(final Screen screen) {
        if (screen == fitted) {
            fitted = null;
            Minecraft.getInstance().getWindow().setGuiScale(chosenScale());
        }
    }

    private static void tick() {
        final Minecraft game = Minecraft.getInstance();
        if (game.screen != null) {
            fit(game.screen);
        } else if (fitted != null) {
            fitted = null;
            game.resizeDisplay();
        }
    }

    /* What {@code screen} needs the window to hold, or null for a screen this leaves alone. */
    private static @Nullable Size sizeOf(final Screen screen) {
        if (screen instanceof IPanelScreen panel) {
            return new Size(panel.panelWidth(), panel.panelHeight());
        }
        if (screen instanceof AbstractContainerScreen<?> container && ofTheSeries(screen)) {
            return new Size(container.getXSize(), container.getYSize());
        }
        return null;
    }

    private static boolean ofTheSeries(final Screen screen) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().startsWith(SERIES)) {
                return true;
            }
        }
        return false;
    }

    /* A screen's image or panel, in the interface's pixels. */
    private record Size(int width, int height) {
    }
}
