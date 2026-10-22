/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleLean;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

/**
 * Whoever rides a leaning vehicle drawn leaning with it, on every player's game: a pilot banks with the aircraft
 * banking into a turn, a driver tips forward with the car going down a slope, each about where they sit, as the
 * vehicle's model is drawn. Their seat already leans with the body ({@link VehicleLean}); this turns them in it.
 */
final class VehicleRiders {

    /* Those turned as they are drawn this frame, set straight again once drawn. */
    private static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private VehicleRiders() {
    }

    /** Hands the game's bus the drawing of riders, once. */
    static void listen() {
        // Last, so a renderer that draws an entity its own way and stops the game's draws it as it would have.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Pre.class, VehicleRiders::before);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Post.class, VehicleRiders::after);
    }

    private static void before(final RenderLivingEvent.Pre<?, ?> event) {
        final LivingEntity rider = event.getEntity();
        if (!(rider.getVehicle() instanceof VehicleEntity vehicle)
                || !VehicleLean.leans(vehicle.leanPitch(), vehicle.leanRoll())) {
            return;
        }
        final float yaw = Mth.rotLerp(event.getPartialTick(), vehicle.yRotO, vehicle.getYRot());
        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(new Quaternionf(VehicleLean.tilt(yaw, vehicle.leanPitch(),
                vehicle.leanRoll())));
        TURNED.add(rider);
    }

    private static void after(final RenderLivingEvent.Post<?, ?> event) {
        if (TURNED.remove(event.getEntity())) {
            event.getPoseStack().popPose();
        }
    }
}
