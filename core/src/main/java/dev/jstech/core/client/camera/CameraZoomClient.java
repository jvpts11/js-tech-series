/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.JsCore;
import dev.jstech.core.camera.CameraOptics;
import dev.jstech.core.camera.ZoomItem;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Looking through a lens on a player's game: while an item that magnifies is held in use, or a mod's block has set a
 * magnification (a telescope looked into), the field of view narrows as the lens narrows it, the mouse turns the view
 * as much slower, the mouse wheel steps between the item's powers, and the item's picture covers the screen around
 * the lens, as the spyglass's does.
 */
public final class CameraZoomClient {

    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "lens");
    /* The power last chosen for each item, so looking through it again starts there. */
    private static final Map<Item, Integer> CHOSEN = new HashMap<>();
    private static double set = 1.0;

    private CameraZoomClient() {
    }

    /** Magnifies the player's view by {@code power}, as a telescope looked into does, until {@link #clear}. */
    public static void set(final double power) {
        set = Mth.clamp(power, 1.0, CameraOptics.MOST_POWER);
    }

    /** Takes back a magnification a mod set. */
    public static void clear() {
        set = 1.0;
    }

    /** How many times the player's view is magnified now: 1 for not at all. */
    public static double power() {
        final Optional<ZoomItem> held = lookingThrough();
        if (held.isPresent()) {
            return held.get().powers().get(index(held.get()));
        }
        return set;
    }

    static void onRegisterLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OVERLAY, CameraZoomClient::drawLens);
    }

    static void onFov(final ViewportEvent.ComputeFov event) {
        final double power = power();
        if (power > 1.0 && event.usedConfiguredFov()) {
            event.setFOV(CameraOptics.fieldOfView(event.getFOV(), power));
        }
    }

    static void onTurn(final CalculatePlayerTurnEvent event) {
        final double power = power();
        if (power > 1.0) {
            event.setMouseSensitivity(CameraOptics.sensitivity(event.getMouseSensitivity(), power));
        }
    }

    static void onScroll(final InputEvent.MouseScrollingEvent event) {
        final Optional<ZoomItem> held = lookingThrough();
        if (held.isEmpty() || Minecraft.getInstance().screen != null || event.getScrollDeltaY() == 0) {
            return;
        }
        event.setCanceled(true);
        final ZoomItem item = held.get();
        CHOSEN.put(item, CameraOptics.step(index(item), item.powers().size(), event.getScrollDeltaY() > 0));
    }

    /* The item that magnifies the player is looking through now, if any. */
    private static Optional<ZoomItem> lookingThrough() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()) {
            return Optional.empty();
        }
        final ItemStack using = player.getUseItem();
        return using.getItem() instanceof ZoomItem item ? Optional.of(item) : Optional.empty();
    }

    private static int index(final ZoomItem item) {
        return Math.min(CHOSEN.getOrDefault(item, 0), item.powers().size() - 1);
    }

    /* The item's picture over the screen, as large as its height, the rest of the screen dark around it. */
    private static void drawLens(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Optional<ZoomItem> held = lookingThrough();
        if (held.isEmpty() || held.get().overlay().isEmpty() || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        final int side = Math.min(g.guiWidth(), g.guiHeight());
        final int x = (g.guiWidth() - side) / 2;
        final int y = (g.guiHeight() - side) / 2;
        g.blit(held.get().overlay().get(), x, y, 0, 0.0F, 0.0F, side, side, side, side);
        final int dark = CameraPalette.get().outsideLens();
        g.fill(0, 0, g.guiWidth(), y, dark);
        g.fill(0, y + side, g.guiWidth(), g.guiHeight(), dark);
        g.fill(0, y, x, y + side, dark);
        g.fill(x + side, y, g.guiWidth(), y + side, dark);
    }
}
