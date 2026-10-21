/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.Nullable;

/**
 * The keys a vehicle is worked with besides the walking keys: its lights, its horn, the next seat, its ignition, a
 * manual gearbox's changes and whether it changes by itself, and its indicators, the sneak key with either setting the
 * hazard lights. The lights are G and the horn H until the player changes them; the rest have no key until given one.
 * Only whoever drives works the vehicle; anyone in it moves to the next seat.
 */
public final class VehicleControlKeys {

    public static final KeyAction LIGHTS = KeyActions.declare(KeyAction.builder(id("vehicle_lights"),
            VehicleTexts.LIGHTS_KEY).key(GLFW.GLFW_KEY_G).onServer((player, shift) -> driven(player, vehicle ->
            vehicle.setSwitches(vehicle.switches().lights(vehicle.switches().lights().next())))));
    public static final KeyAction HORN = KeyActions.declare(KeyAction.builder(id("vehicle_horn"),
            VehicleTexts.HORN_KEY).key(GLFW.GLFW_KEY_H).onServer((player, shift) -> driven(player,
            VehicleControlKeys::sound)));
    public static final KeyAction SEAT = KeyActions.declare(KeyAction.builder(id("vehicle_seat"),
            VehicleTexts.SEAT_KEY).onServer((player, shift) -> {
                if (player.getVehicle() instanceof VehicleEntity vehicle) {
                    vehicle.nextSeat(player);
                }
            }));
    public static final KeyAction IGNITION = KeyActions.declare(KeyAction.builder(id("vehicle_ignition"),
            VehicleTexts.IGNITION_KEY).onServer((player, shift) -> driven(player, vehicle ->
            vehicle.setSwitches(vehicle.switches().ignition(!vehicle.switches().ignition())))));
    public static final KeyAction SHIFT_UP = KeyActions.declare(KeyAction.builder(id("vehicle_shift_up"),
            VehicleTexts.SHIFT_UP_KEY).onServer((player, shift) -> driven(player, vehicle -> vehicle.askShift(1))));
    public static final KeyAction SHIFT_DOWN = KeyActions.declare(KeyAction.builder(id("vehicle_shift_down"),
            VehicleTexts.SHIFT_DOWN_KEY).onServer((player, shift) -> driven(player,
            vehicle -> vehicle.askShift(-1))));
    public static final KeyAction GEARBOX = KeyActions.declare(KeyAction.builder(id("vehicle_gearbox"),
            VehicleTexts.GEARBOX_KEY).onServer((player, shift) -> driven(player, vehicle ->
            vehicle.setSwitches(vehicle.switches().manual(!vehicle.switches().manual())))));
    public static final KeyAction INDICATE_LEFT = KeyActions.declare(KeyAction.builder(id("vehicle_indicate_left"),
            VehicleTexts.INDICATE_LEFT_KEY).onServer((player, shift) -> driven(player, vehicle ->
            indicate(vehicle, shift ? VehicleSwitches.Indicators.HAZARD : VehicleSwitches.Indicators.LEFT))));
    public static final KeyAction INDICATE_RIGHT = KeyActions.declare(KeyAction.builder(id("vehicle_indicate_right"),
            VehicleTexts.INDICATE_RIGHT_KEY).onServer((player, shift) -> driven(player, vehicle ->
            indicate(vehicle, shift ? VehicleSwitches.Indicators.HAZARD : VehicleSwitches.Indicators.RIGHT))));

    private VehicleControlKeys() {
    }

    /** Declares the keys, before the player's game makes its key bindings. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The vehicle {@code player} drives, or none. */
    public static @Nullable VehicleEntity drivenBy(final ServerPlayer player) {
        return player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getControllingPassenger() == player
                ? vehicle : null;
    }

    /** Sounds its horn, when it has one that works and is not silent. */
    public static void sound(final VehicleEntity vehicle) {
        final VehicleBuild build = vehicle.build();
        if (build == null || build.horn().isEmpty() || !(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        final SoundKey sound = build.horn().get().spec().sound().map(SoundKeys::find).orElse(null);
        if (sound != null) {
            Audio.at(level, vehicle.getX(), vehicle.getY() + 1, vehicle.getZ(), sound, 1, 1);
        }
    }

    private static void driven(final ServerPlayer player, final Consumer<VehicleEntity> does) {
        final VehicleEntity vehicle = drivenBy(player);
        if (vehicle != null) {
            does.accept(vehicle);
        }
    }

    /* Switches the indicators to {@code to}, or off when they already are. */
    private static void indicate(final VehicleEntity vehicle, final VehicleSwitches.Indicators to) {
        final VehicleSwitches now = vehicle.switches();
        vehicle.setSwitches(now.indicators(now.indicators() == to ? VehicleSwitches.Indicators.OFF : to));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
