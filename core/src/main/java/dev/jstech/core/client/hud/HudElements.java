/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.JsCore;
import dev.jstech.core.gui.HudStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Every mod's HUD elements, declared once from the mod's client set-up and drawn by the Core in one layer of the
 * game's HUD, under the chat: in each corner, the elements that show this frame are stacked from the corner inwards in
 * the order they were declared, so the mods share the corners without drawing over one another. The layer hides with
 * the rest of the HUD when the player hides it.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class HudElements {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hud_elements");
    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, Declared> BY_ID = new ConcurrentHashMap<>();

    private HudElements() {
    }

    /**
     * Declares {@code element}, known as {@code id}, in {@code corner}.
     *
     * @throws IllegalStateException when an element of that id is declared already
     */
    public static void declare(final ResourceLocation id, final HudStack.Corner corner, final IHudElement element) {
        final Declared declared = new Declared(id, corner, element);
        if (BY_ID.putIfAbsent(id, declared) != null) {
            throw new IllegalStateException("the HUD element " + id + " is declared twice");
        }
        DECLARED.add(declared);
    }

    /** Where each element of {@code corner} that shows now is drawn, by its id; for a test to look at. */
    public static Map<ResourceLocation, HudStack.Point> placedIn(final HudStack.Corner corner) {
        final Minecraft minecraft = Minecraft.getInstance();
        final List<Declared> shown = shownIn(minecraft, corner);
        final List<HudStack.Point> points = HudStack.place(corner, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), sizes(minecraft, shown));
        final Map<ResourceLocation, HudStack.Point> out = new LinkedHashMap<>();
        for (int i = 0; i < shown.size(); i++) {
            out.put(shown.get(i).id(), points.get(i));
        }
        return out;
    }

    @SubscribeEvent
    public static void onRegisterLayers(final RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, LAYER, HudElements::render);
    }

    private static void render(final GuiGraphics graphics, final DeltaTracker delta) {
        if (DECLARED.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        for (final HudStack.Corner corner : List.of(HudStack.Corner.TOP_LEFT, HudStack.Corner.TOP_RIGHT,
                HudStack.Corner.BOTTOM_LEFT, HudStack.Corner.BOTTOM_RIGHT)) {
            final List<Declared> shown = shownIn(minecraft, corner);
            if (shown.isEmpty()) {
                continue;
            }
            final List<HudStack.Point> points = HudStack.place(corner, graphics.guiWidth(), graphics.guiHeight(),
                    sizes(minecraft, shown));
            for (int i = 0; i < shown.size(); i++) {
                shown.get(i).element().render(graphics, delta, points.get(i).x(), points.get(i).y());
            }
        }
    }

    private static List<Declared> shownIn(final Minecraft minecraft, final HudStack.Corner corner) {
        final List<Declared> shown = new ArrayList<>();
        for (final Declared declared : DECLARED) {
            if (declared.corner() == corner && declared.element().shown(minecraft)) {
                shown.add(declared);
            }
        }
        return shown;
    }

    private static List<HudStack.Size> sizes(final Minecraft minecraft, final List<Declared> shown) {
        return shown.stream().map(declared -> declared.element().size(minecraft)).toList();
    }

    private record Declared(ResourceLocation id, HudStack.Corner corner, IHudElement element) {
    }
}
