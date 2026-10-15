/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.energy.ClampMeterLayout;
import dev.jstech.core.jstechseries.energy.MeterReading;
import dev.jstech.core.jstechseries.energy.MeterState;
import dev.jstech.core.jstechseries.energy.MeterView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Optional;
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
 * The clamp meter on the player's game: holding it up on its display, and its reading beside the crosshair while it is
 * in hand, clamped, switched on and set to SHOW.
 */
public final class ClampMeterClient {

    /** The HUD layer of the reading beside the crosshair. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "clamp_meter");

    private static final int FROM_CROSSHAIR = 10;
    private static final int PADDING = 4;
    private static final int LINE = 11;
    private static final int GROUND = 0x96 << 24;

    private ClampMeterClient() {
    }

    /** Hands the mod's bus what this answers and says what holds the meter up, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        ClampMeter.useOpener(hand -> Minecraft.getInstance().setScreen(new ClampMeterScreen(hand)));
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, ClampMeterClient::renderHud));
    }

    /** The meter the player holds, in the main hand first, or nothing. */
    public static ItemStack heldMeter(final Minecraft minecraft) {
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack held = minecraft.player.getItemInHand(hand);
            if (held.is(ClampMeter.ITEM.get())) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void renderHud(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ItemStack meter = heldMeter(minecraft);
        if (meter.isEmpty() || minecraft.screen != null || minecraft.options.hideGui
                || meter.get(ClampMeter.CLAMP.get()) == null) {
            return;
        }
        final MeterState state = ClampMeter.state(meter);
        final MeterReading reading = MeterView.shown(state.hold());
        final Optional<Energy> energy = Energies.byId(reading.energy());
        if (state.dial() == MeterState.Dial.OFF || !state.show() || !reading.present() || energy.isEmpty()) {
            return;
        }
        final boolean fe = state.dial() == MeterState.Dial.FE;
        final String first;
        final String second;
        if (reading.line()) {
            final Text rate = fe ? Energies.FE.rate(reading.ratio().toFe(reading.perTick()))
                    : energy.get().rate(reading.perTick());
            first = GameText.resolve(rate) + reading.toward().map(face -> "  "
                    + GameText.resolve(ClampMeter.toward(face).text())).orElse("");
            final String full = reading.perTick() >= reading.ceiling() ? "  " + GameText.resolve(ClampMeter.FULL)
                    : "";
            second = rated(energy.get(), reading) + full;
        } else {
            first = GameText.resolve(ClampMeter.STORED.with(fe ? Energies.FE.amount(reading.ratio()
                    .toFe(reading.stored())) : energy.get().amount(reading.stored()), fe ? Energies.FE.amount(reading
                    .ratio().toFe(reading.capacity())) : energy.get().amount(reading.capacity()),
                    (reading.perTick() > 0 ? ClampMeter.FILLING : reading.perTick() < 0 ? ClampMeter.EMPTYING
                            : ClampMeter.STEADY).text()));
            second = rated(energy.get(), reading);
        }
        final int x = g.guiWidth() / 2 + FROM_CROSSHAIR;
        final int y = g.guiHeight() / 2 + FROM_CROSSHAIR / 2;
        final int width = Math.max(minecraft.font.width(first), (int) Math.ceil(minecraft.font.width(second)
                * ClampMeterLayout.SMALL)) + PADDING * 2;
        final int height = second.isEmpty() ? LINE + PADDING : LINE * 2 + PADDING;
        final ClampMeterScreen.Colours c = ClampMeterScreen.PALETTE.get();
        g.fill(x, y, x + width, y + height, GROUND);
        Draw.text(g, minecraft.font, first, x + PADDING, y + PADDING / 2 + 1, c.white());
        if (!second.isEmpty()) {
            Draw.textScaled(g, minecraft.font, second, x + PADDING, y + LINE + PADDING / 2 + 1,
                    reading.line() && reading.perTick() >= reading.ceiling() ? c.amber() : c.text(),
                    ClampMeterLayout.SMALL);
        }
    }

    /* The class of the line or the port, with its rating; empty for an energy with no classes. */
    private static String rated(final Energy energy, final MeterReading reading) {
        final Optional<EnergyClass> named = reading.lineClass().isEmpty() ? Optional.empty()
                : energy.classNamed(reading.lineClass());
        return named.map(found -> GameText.resolve(found.name().text()) + found.ratingKey()
                .map(rating -> " " + GameText.resolve(rating.text())).orElse("")).orElse("");
    }
}
