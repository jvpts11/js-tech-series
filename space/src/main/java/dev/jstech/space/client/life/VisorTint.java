/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.life.LifeSupport;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The view through a visor lowered over the helmet's own, in first person: a little gold, as through the gold visors
 * of Apollo's helmets, as strongly as the visor's module says. Only its look: the Sun is looked at as anywhere else,
 * and nothing whites the view out.
 */
public final class VisorTint {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "visor_tint");
    /* The gold of the visor. */
    private static final int VISOR_GOLD = 0xD4A23C;
    private static final int OPAQUE = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final int RGB = 0xFFFFFF;

    private VisorTint() {
    }

    /** Draws the tint over the world, under the HUD, once, from the mod's client entry point. */
    public static void listen(final IEventBus modEventBus) {
        modEventBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, VisorTint::draw));
    }

    /** How strongly the visors {@code player} wears on the head tint their view, 0 with none lowered, to 1. */
    public static double tint(final ClientLevel level, final Player player) {
        double tint = 0;
        for (final SuitModule module : LifeSupport.modules(level, player.getItemBySlot(EquipmentSlot.HEAD))) {
            if (module.kind() == SuitModule.Kind.VISOR) {
                tint += module.amount();
            }
        }
        return Math.min(1, tint);
    }

    private static void draw(final GuiGraphics graphics, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final Player player = minecraft.player;
        if (level == null || player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        final double tint = tint(level, player);
        if (tint > 0) {
            final int alpha = Math.round((float) tint * OPAQUE) << ALPHA_SHIFT;
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha | VISOR_GOLD & RGB);
        }
    }
}
