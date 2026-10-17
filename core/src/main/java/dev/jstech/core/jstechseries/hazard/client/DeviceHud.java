/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.jstechseries.hazard.DeviceState;
import dev.jstech.core.jstechseries.hazard.HazardDevice;
import dev.jstech.core.jstechseries.hazard.HazardDeviceSettings;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One hazard device on the HUD, drawn as the device itself at the size the player gave it: the Geiger counter and the
 * dosimeter with their faces live, and the map device with the land round the player and the hazard it shows on it.
 */
public final class DeviceHud implements IHudElement {

    private final HazardDevice device;

    /** The map device's case, and how big it is. */
    public static final ResourceLocation MAP = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/hazard/hazard_map.png");
    public static final int MAP_WIDTH = 106;
    public static final int MAP_HEIGHT = 144;
    /* The map device's face, its screen and its three buttons, each a button's left and their top. */
    private static final int MAP_FACE_X0 = 5;
    private static final int MAP_FACE_Y0 = 15;
    private static final int MAP_FACE_X1 = 99;
    private static final int MAP_FACE_Y1 = 126;
    private static final int SCREEN_X = 7;
    private static final int SCREEN_Y = 26;
    private static final int SCREEN_SIZE = 90;
    private static final List<Integer> BUTTONS = List.of(7, 39, 69);
    private static final int BUTTON_Y = 131;
    private static final int BUTTON_WIDTH = 10;
    /* A chunk on the device's screen, in pixels, and how many chunks wide the land it draws from is. */
    private static final int CELL = 8;
    private static final int PICTURE_CHUNKS = 14;
    /* How many chunks of land the device draws a frame, and how long its sweep takes to go round, in ticks. */
    private static final int CHUNKS_A_FRAME = 4;
    private static final float SWEEP_TICKS = 40;
    private static final float SMALL = 0.5F;
    private static @Nullable TerrainPicture picture;

    public DeviceHud(final HazardDevice device) {
        this.device = device;
    }

    /** Lets go of the map device's picture of the land, as the player leaves a world. */
    public static void forget() {
        if (picture != null) {
            picture.close();
            picture = null;
        }
    }

    /** The hazards a map shows, in the order they were declared: the mapped ones that are on. */
    public static List<HazardKind> mapped() {
        return Hazards.all().stream().filter(kind -> kind.mapped() && kind.enabled()).toList();
    }

    /** The hazard a map device set as {@code state} shows: the one it was set to, or the first mapped. */
    public static Optional<HazardKind> shown(final DeviceState state) {
        final List<HazardKind> kinds = mapped();
        return kinds.stream().filter(kind -> kind.id().toString().equals(state.shown())).findFirst()
                .or(() -> kinds.stream().findFirst());
    }

    @Override
    public boolean shown(final Minecraft minecraft) {
        return minecraft.player != null && !minecraft.options.hideGui
                && !HazardDevicesClient.carried(minecraft.player, this.device).isEmpty();
    }

    @Override
    public HudStack.Size size(final Minecraft minecraft) {
        final float size = HazardDeviceSettings.size(this.device);
        return new HudStack.Size(Math.round(width(this.device) * size), Math.round(height(this.device) * size));
    }

    @Override
    public void render(final GuiGraphics g, final DeltaTracker delta, final int x, final int y) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player == null) {
            return;
        }
        final ItemStack stack = HazardDevicesClient.carried(player, this.device);
        final float size = HazardDeviceSettings.size(this.device);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size, size, 1);
        switch (this.device) {
            case GEIGER_COUNTER -> drawGeiger(g, minecraft.font, 0, 0, 1, HazardDevices.state(stack));
            case POCKET_DOSIMETER -> drawDosimeter(g, minecraft.font, 0, 0, 1, HazardDevices.state(stack));
            case HAZARD_MAP -> drawMap(g, minecraft, player, HazardDevices.state(stack),
                    delta.getGameTimeDeltaPartialTick(false));
        }
        g.pose().popPose();
    }

    /** The Geiger counter set as {@code state}, live, its case's top left at {@code (x, y)}, in units of {@code s}. */
    public static void drawGeiger(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                                  final DeviceState state) {
        final Optional<HazardKind> radiation = DeviceReadings.radiation();
        DeviceArt.geiger(g, font, x, y, s, radiation.map(DeviceReadings::rateHere),
                radiation.map(DeviceReadings::rateUnit).orElse(""),
                GameText.resolve(HazardDevices.GEIGER.with(state.multiplier())), state.multiplier(),
                HazardDevicesClient.litLamps(), GameText.resolve(HazardDevices.RANGE),
                GameText.resolve(HazardDevices.SOUND), GameText.resolve(HazardDevices.OFF));
    }

    /** The dosimeter set as {@code state}, live, its case's top left at {@code (x, y)}, in units of {@code s}. */
    public static void drawDosimeter(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                                     final DeviceState state) {
        final Optional<HazardKind> radiation = DeviceReadings.radiation();
        final double dose = radiation.map(DeviceReadings::level).orElse(0.0);
        final Optional<HazardBand> band = radiation.flatMap(kind -> kind.band(dose));
        final int reached = radiation.map(kind -> band.map(found -> kind.bands().indexOf(found) + 1).orElse(0))
                .orElse(0);
        DeviceArt.dosimeter(g, font, x, y, s, radiation.map(DeviceReadings::level),
                radiation.map(kind -> GameText.resolve(kind.unit())).orElse(""),
                band.map(found -> GameText.resolve(found.name())).orElse(""), reached, state.light(),
                GameText.resolve(HazardDevices.DOSE), GameText.resolve(HazardDevices.LIGHT),
                GameText.resolve(HazardDevices.OFF));
    }

    /** The picture of the land the devices draw on, made the first time it is needed. */
    public static TerrainPicture picture() {
        if (picture == null) {
            picture = new TerrainPicture(PICTURE_CHUNKS, CELL);
        }
        return picture;
    }

    private void drawMap(final GuiGraphics g, final Minecraft minecraft, final Player player, final DeviceState state,
                         final float partial) {
        final Font font = minecraft.font;
        final DeviceArt.Colours c = DeviceArt.PALETTE.get();
        g.blit(MAP, 0, 0, 0, 0, MAP_WIDTH, MAP_HEIGHT, MAP_WIDTH, MAP_HEIGHT);
        final Optional<HazardKind> kind = shown(state);
        Draw.textScaled(g, font, GameText.resolve(HazardDevices.HAZARDS), MAP_FACE_X0 + 3, MAP_FACE_Y0 + 3, c.dim(),
                SMALL);
        kind.ifPresent(found -> {
            final String label = (GameText.resolve(found.shortName()) + " " + DeviceReadings.rateUnit(found))
                    .toUpperCase(Locale.ROOT);
            Draw.textScaled(g, font, label, MAP_FACE_X1 - 3 - (int) Math.ceil(font.width(label) * SMALL),
                    MAP_FACE_Y0 + 3, c.amber(), SMALL);
        });
        final TerrainPicture land = picture();
        land.update(player.chunkPosition(), CHUNKS_A_FRAME);
        final double px = player.getX() / 16;
        final double pz = player.getZ() / 16;
        final double sweep = (player.tickCount + partial) % SWEEP_TICKS / SWEEP_TICKS * Math.PI * 2;
        HazardMapDrawing.draw(g, land, SCREEN_X, SCREEN_Y, SCREEN_SIZE, SCREEN_SIZE, CELL, px, pz, kind, px, pz,
                player.getYRot(), sweep);
        if (kind.isEmpty()) {
            final String nothing = GameText.resolve(HazardDevices.NOTHING_MAPPED);
            Draw.textScaled(g, font, nothing, SCREEN_X + (SCREEN_SIZE - (int) Math.ceil(font.width(nothing) * SMALL))
                    / 2, SCREEN_Y + SCREEN_SIZE / 2 - 2, c.white(), SMALL);
        } else {
            final String here = GameText.resolve(HazardDevices.HERE.with(DeviceReadings.figure(
                    DeviceReadings.rateHere(kind.get())) + " " + DeviceReadings.rateUnit(kind.get())));
            Draw.textScaled(g, font, here, MAP_FACE_X0 + 3, MAP_FACE_Y1 - 8, c.white(), SMALL);
        }
        final List<HazardKind> kinds = mapped();
        for (int i = 0; i < BUTTONS.size(); i++) {
            final String word = i == BUTTONS.size() - 1 ? GameText.resolve(HazardDevices.OPEN)
                    : i < kinds.size() ? GameText.resolve(kinds.get(i).shortName()).toUpperCase(Locale.ROOT) : "";
            Draw.textScaled(g, font, word, BUTTONS.get(i) + BUTTON_WIDTH + 2, BUTTON_Y - 1, c.mould(), SMALL);
        }
    }

    private static int width(final HazardDevice device) {
        return switch (device) {
            case GEIGER_COUNTER -> DeviceArt.GEIGER_WIDTH;
            case POCKET_DOSIMETER -> DeviceArt.DOSIMETER_WIDTH;
            case HAZARD_MAP -> MAP_WIDTH;
        };
    }

    private static int height(final HazardDevice device) {
        return switch (device) {
            case GEIGER_COUNTER -> DeviceArt.GEIGER_HEIGHT;
            case POCKET_DOSIMETER -> DeviceArt.DOSIMETER_HEIGHT;
            case HAZARD_MAP -> MAP_HEIGHT;
        };
    }
}
