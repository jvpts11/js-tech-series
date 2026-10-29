/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.jstechseries.hazard.HazardDevice;
import dev.jstech.core.jstechseries.hazard.client.HazardDevicesClient;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.SuitAir;
import dev.jstech.space.life.SuitControls;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The suit's controls on the HUD, while a suit's chest piece is worn: in the dosimeter's corner, stacked with it, and
 * the screen's edges tinted as the helmet's rim when something is wrong, amber with little oxygen left and red with a
 * hole. Its key holds the controls up close.
 */
public final class SuitControlsHud implements IHudElement {

    /** The controls' element on the HUD. */
    public static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "suit_controls");
    /** The share of the tanks left under which the oxygen is low. */
    public static final double LOW_SHARE = 0.1;
    private static final ResourceLocation RIM = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "suit_rim");
    /* A pound an inch in kilopascals, and the carbon dioxide the suit's scrubbers keep, as a share of the bar. */
    private static final double KILOPASCALS_A_PSI = 6.894757;
    private static final double SCRUBBED = 0.1;
    /* The rim's tint: how many bands deep, each how thick, and the alpha at the edge. */
    private static final int RIM_BANDS = 12;
    private static final int RIM_BAND = 3;
    private static final int RIM_ALPHA_LOW = 90;
    private static final int RIM_ALPHA_HOLED = 120;
    private static final int RIM_AMBER = 0xFF9628;
    private static final int RIM_RED = 0xE6281E;
    private static final int ALPHA_SHIFT = 24;

    /** Declares the element, the rim and the key, once, from the mod's client entry point. */
    public static void listen(final IEventBus modEventBus) {
        HudElements.declare(HUD, () -> HazardDevicesClient.corner(HazardDevice.POCKET_DOSIMETER),
                new SuitControlsHud());
        modEventBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, RIM, SuitControlsHud::rim));
        KeyActionsClient.onPress(SuitControls.LOOK, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen == null && reading(minecraft.player).isPresent()) {
                minecraft.setScreen(new SuitControlsScreen());
            }
        });
    }

    /** What the controls read for {@code player}: none where it wears no suit's chest piece. */
    public static Optional<Reading> reading(final Player player) {
        final Level level = player.level();
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final Optional<SpaceSuit> suit = SuitAir.suitOf(level, chest);
        if (suit.isEmpty()) {
            return Optional.empty();
        }
        final int capacity = LifeSupport.capacity(level, chest, suit.get());
        final int air = LifeSupport.air(level, chest, suit.get());
        final boolean whole = SpaceSuits.worn(player).isPresent();
        final boolean pressed = whole && air > 0;
        final State state = !whole ? State.OPEN : SuitAir.holes(chest) > 0 ? State.HOLED
                : air <= capacity * LOW_SHARE ? State.LOW : State.SEALED;
        return Optional.of(new Reading(pressed ? SuitAir.pressure(chest) / KILOPASCALS_A_PSI : 0, air,
                capacity <= 0 ? 0 : (double) air / capacity, pressed ? SCRUBBED : 0, state,
                SuitControls.lit(chest)));
    }

    @Override
    public boolean shown(final Minecraft minecraft) {
        return minecraft.player != null && !minecraft.options.hideGui && reading(minecraft.player).isPresent();
    }

    @Override
    public HudStack.Size size(final Minecraft minecraft) {
        return new HudStack.Size(SuitControlsArt.WIDTH, SuitControlsArt.HEIGHT);
    }

    @Override
    public void render(final GuiGraphics graphics, final DeltaTracker delta, final int x, final int y) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            reading(minecraft.player).ifPresent(reading -> SuitControlsArt.draw(graphics, minecraft.font, x, y, 1,
                    reading));
        }
    }

    /* The helmet's rim tinted round the screen's edges while the controls warn. */
    private static void rim(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        final Optional<Reading> reading = reading(minecraft.player);
        if (reading.isEmpty() || reading.get().state() != State.LOW && reading.get().state() != State.HOLED) {
            return;
        }
        final boolean holed = reading.get().state() == State.HOLED;
        final int colour = holed ? RIM_RED : RIM_AMBER;
        final int edge = holed ? RIM_ALPHA_HOLED : RIM_ALPHA_LOW;
        final int w = g.guiWidth();
        final int h = g.guiHeight();
        for (int band = 0; band < RIM_BANDS; band++) {
            final int alpha = edge * (RIM_BANDS - band) / RIM_BANDS / 2;
            final int argb = alpha << ALPHA_SHIFT | colour;
            final int in = band * RIM_BAND;
            final int out = in + RIM_BAND;
            g.fill(in, in, w - in, out, argb);
            g.fill(in, h - out, w - in, h - in, argb);
            g.fill(in, out, out, h - out, argb);
            g.fill(w - out, out, w - in, h - out, argb);
        }
    }

    /** What the controls show of the suit's state. */
    public enum State {
        /** Worn whole, its tanks holding air. */
        SEALED,
        /** Little oxygen left in the tanks. */
        LOW,
        /** Holed, leaking until patched. */
        HOLED,
        /** Not worn whole: the helmet or another piece off. */
        OPEN
    }

    /**
     * What the controls read.
     *
     * @param psi           the suit's pressure, in pounds an inch
     * @param secondsLeft   the seconds of breathing left in the tanks
     * @param oxygen        the tanks' share left, 0 to 1
     * @param carbonDioxide the carbon dioxide in the suit, as a share of the bar
     * @param state         the suit's state
     * @param light         whether the face is lit
     */
    public record Reading(double psi, int secondsLeft, double oxygen, double carbonDioxide, State state,
                          boolean light) {
    }
}
