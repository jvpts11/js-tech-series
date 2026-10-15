/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import dev.jstech.core.vehicle.DriverInput;
import dev.jstech.core.vehicle.DriverInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * A player drives a vehicle with the keys they walk with: ahead and back, left and right to turn, jump to climb or
 * leap. Sneaking is the game's own way off a vehicle, so a flying one sinks on the sprint key instead.
 */
public final class VehicleControls {

    private VehicleControls() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, VehicleControls::onClientSetup);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        DriverInputs.readLocalPlayerWith(player -> player instanceof LocalPlayer local
                ? new DriverInput(local.input.forwardImpulse, local.input.leftImpulse, local.input.jumping,
                Minecraft.getInstance().options.keySprint.isDown())
                : DriverInput.NONE);
    }
}
