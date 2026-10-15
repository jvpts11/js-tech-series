/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.probe.SignalProbeState;
import dev.jstech.core.jstechseries.probe.SignalReading;
import dev.jstech.core.jstechseries.probe.SignalView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The signal probe on the player's game: holding it up on its display, and its reading beside the crosshair while it
 * is in hand, on and set to SHOW: the redstone on the block it reads, and how many frequencies are heard here.
 */
public final class SignalProbeClient {

    /** The HUD layer of the reading beside the crosshair. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "signal_probe");

    private static final int FROM_CROSSHAIR = 10;
    private static final int PADDING = 4;
    private static final int LINE = 11;
    private static final int GROUND = 0x96 << 24;

    private SignalProbeClient() {
    }

    /** Hands the mod's bus what this answers and says what holds the probe up, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        SignalProbe.useOpener(hand -> Minecraft.getInstance().setScreen(new SignalProbeScreen(hand)));
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, SignalProbeClient::renderHud));
    }

    /** The probe the player holds, in the main hand first, or nothing. */
    public static ItemStack heldProbe(final Minecraft minecraft) {
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack held = minecraft.player.getItemInHand(hand);
            if (held.is(SignalProbe.ITEM.get())) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void renderHud(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ItemStack probe = heldProbe(minecraft);
        if (probe.isEmpty() || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        final SignalProbeState state = SignalProbe.state(probe);
        if (!state.on() || !state.show()) {
            return;
        }
        final SignalReading reading = SignalView.shown(state.hold());
        final String first = reading.linked() ? GameText.resolve(SignalProbe.HUD_WIRE.with(Text.literal(
                String.valueOf(reading.strength())))) : GameText.resolve(SignalProbe.TITLE);
        final String second = GameText.resolve(SignalProbe.HUD_HEARD.with(Text.literal(String.valueOf(
                reading.heard().size()))));
        final int x = g.guiWidth() / 2 + FROM_CROSSHAIR;
        final int y = g.guiHeight() / 2 + FROM_CROSSHAIR / 2;
        final int width = Math.max(minecraft.font.width(first), (int) Math.ceil(minecraft.font.width(second)
                * ProbeLayout.SMALL)) + PADDING * 2;
        final NetworkProbeScreen.Colours c = NetworkProbeScreen.PALETTE.get();
        g.fill(x, y, x + width, y + LINE * 2 + PADDING, GROUND);
        Draw.text(g, minecraft.font, first, x + PADDING, y + PADDING / 2 + 1, c.white());
        Draw.textScaled(g, minecraft.font, second, x + PADDING, y + LINE + PADDING / 2 + 1, c.text(),
                ProbeLayout.SMALL);
    }
}
