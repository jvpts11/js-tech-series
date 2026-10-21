/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.client.light.IDynamicLightSource;
import dev.jstech.core.vehicle.VehicleBuild;
import dev.jstech.core.vehicle.VehicleControlKeys;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleLightRole;
import dev.jstech.core.vehicle.VehicleLights;
import dev.jstech.core.vehicle.VehiclePartDef;
import dev.jstech.core.vehicle.VehicleSwitches;
import dev.jstech.core.vehicle.Vehicles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * The vehicles on a player's game: drawn with their models, their items drawn as their kinds say, their lights lighting
 * the world, the headlights the way ahead as far as they reach, their engines heard as they run, and a manual
 * gearbox's changes felt at once by whoever drives it.
 */
public final class VehiclesClient {

    /* A player sees the lights of the vehicles this near, in blocks. */
    private static final double SEES = 64;
    /* A headlight lights the way at these shares of its reach ahead. */
    private static final double[] BEAM = {0.25, 0.55, 0.9};

    private VehiclesClient() {
    }

    /** Hands the buses what the player's game answers, once, as it starts. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerEntityRenderer(Vehicles.ENTITY.get(), VehicleRenderer::new));
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerItem(
                new VehicleItemRenderer().extensions(), Vehicles.ITEM.get(), Vehicles.PART_ITEM.get()));
        DynamicLightsClient.provide(VehiclesClient::lights);
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof VehicleEntity vehicle) {
                SoundDirector.track(vehicle);
            }
        });
        NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, event -> {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof VehicleEntity vehicle) {
                SoundDirector.untrack(vehicle);
            }
        });
        KeyActionsClient.onPress(VehicleControlKeys.SHIFT_UP, () -> shiftHere(1));
        KeyActionsClient.onPress(VehicleControlKeys.SHIFT_DOWN, () -> shiftHere(-1));
    }

    /* The vehicle the player drives on this game changes gear here too, for its game moves it. */
    private static void shiftHere(final int by) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle.getControllingPassenger() == player) {
            vehicle.askShift(by);
        }
    }

    /* The lights of the vehicles near the player, the headlights' along the way ahead. */
    private static List<IDynamicLightSource.Light> lights(final ClientLevel level) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }
        final List<IDynamicLightSource.Light> out = new ArrayList<>();
        final boolean blink = VehicleLights.blinkOn(level.getGameTime());
        for (final VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class,
                new AABB(player.position(), player.position()).inflate(SEES))) {
            final VehicleBuild build = vehicle.build();
            if (build == null) {
                continue;
            }
            final VehicleSwitches switches = vehicle.switches();
            final Vec3 ahead = Vec3.directionFromRotation(0, vehicle.getYRot());
            final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
            for (final VehicleBuild.PartAt<VehiclePartDef.Light> lamp : build.lights()) {
                final VehiclePartDef.Light spec = lamp.spec();
                final float shine = VehicleLights.shine(spec.role(), switches, lamp.health(), blink);
                if (shine <= 0) {
                    continue;
                }
                final Vec3 at = lamp.place().at();
                final Vec3 where = vehicle.position().add(left.scale(at.x)).add(0, at.y, 0).add(ahead.scale(at.z));
                final int level15 = Math.max(1, Math.round(spec.level() * shine));
                out.add(new IDynamicLightSource.Light(where, level15, spec.colour()));
                if (spec.role() == VehicleLightRole.HEAD) {
                    for (final double share : BEAM) {
                        out.add(new IDynamicLightSource.Light(where.add(ahead.scale(spec.reach() * share)),
                                Math.max(1, level15 - (int) Math.round(share * 4)), spec.colour()));
                    }
                }
            }
        }
        return out;
    }
}
