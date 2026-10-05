/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import dev.jstech.core.JsCore;
import dev.jstech.core.vehicle.DriverInput;
import dev.jstech.core.vehicle.DriverInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * A player drives a vehicle with the keys they walk with: ahead and back, left and right to turn, jump to climb or
 * leap. Sneaking is the game's own way off a vehicle, so a flying one sinks on the sprint key instead.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class VehicleControls {

    private VehicleControls() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        DriverInputs.readLocalPlayerWith(player -> player instanceof LocalPlayer local
                ? new DriverInput(local.input.forwardImpulse, local.input.leftImpulse, local.input.jumping,
                Minecraft.getInstance().options.keySprint.isDown())
                : DriverInput.NONE);
    }
}
