/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.IHeldThings;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.vehicle.VehicleBuild;
import dev.jstech.core.vehicle.VehicleDef;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleLights;
import dev.jstech.core.vehicle.VehiclePartDef;
import dev.jstech.core.vehicle.VehiclePartKind;
import dev.jstech.core.vehicle.VehicleSwitches;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a vehicle with the model of its kind, or of its variant: turned the way it faces, leaning as its wheels stand,
 * in its paint; each place's bone shown with a part there and hidden without; its wheels turning as it rolls, its
 * steering wheels turned, each wheel dropping into the ground under it on its spring; its doors swung open as they
 * are; and each light's {@code <place>_glow} bone shown only while the light shines. A kind with no model draws
 * nothing.
 */
public final class VehicleRenderer extends EntityRenderer<VehicleEntity> {

    /* The models whose bones this moves, each moved once. */
    private final Set<KitInstance> moved = Collections.newSetFromMap(new WeakHashMap<>());

    /* How far a steering wheel turns at a full steer, and a door swings open, in degrees. */
    private static final float STEER_DEGREES = 30;
    private static final float DOOR_DEGREES = 70;
    /* Model units are sixteenths of a block. */
    private static final float UNITS = 16;
    private static final String GLOW = "_glow";
    /* An engine's, a rotor's or a propeller's {@code <place>_spin} bone turns while the engine runs. */
    private static final String SPIN = "_spin";

    public VehicleRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(final VehicleEntity vehicle, final float yaw, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light) {
        final VehicleDef def = vehicle.def();
        final Optional<KitModel> found = def == null ? Optional.empty() : modelOf(vehicle, def).flatMap(KitModels::get);
        if (found.isPresent()) {
            final KitInstance instance = KitAnimations.of(vehicle, found.get());
            if (moved.add(instance)) {
                instance.animator().mover((pose, seconds) -> move(vehicle, pose));
            }
            final double seconds = KitClock.seconds(partialTick);
            instance.animate(KitClock.frame(), seconds, true, IAnimationListener.NONE);
            poses.pushPose();
            // A model faces north as it is drawn; an entity looking at yaw 0 faces south.
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, vehicle.yRotO,
                    vehicle.getYRot())));
            poses.mulPose(Axis.XP.rotationDegrees(vehicle.leanPitch()));
            poses.mulPose(Axis.ZP.rotationDegrees(vehicle.leanRoll()));
            KitRenderer.draw(instance, variant(vehicle, def), vehicle, poses, buffers, light,
                    OverlayTexture.NO_OVERLAY, IHeldThings.none(), vehicle.level(), true);
            poses.popPose();
        }
        super.render(vehicle, yaw, partialTick, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(final VehicleEntity vehicle) {
        final VehicleDef def = vehicle.def();
        return def == null ? MissingTextureAtlasSprite.getLocation() : modelOf(vehicle, def).flatMap(KitModels::get)
                .map(model -> model.texture(0)).orElse(MissingTextureAtlasSprite.getLocation());
    }

    /** The model a vehicle of {@code def} is drawn with: its variant's, else its kind's. */
    static Optional<ResourceLocation> modelOf(final VehicleEntity vehicle, final VehicleDef def) {
        return def.variant(vehicle.variant()).flatMap(VehicleDef.Variant::model).or(def::model);
    }

    /* What it is drawn as: in its paint, the bones of its empty places and dark lights hidden. */
    private static ModelDefinition.Variant variant(final VehicleEntity vehicle, final VehicleDef def) {
        final Set<String> hidden = new HashSet<>();
        final VehicleSwitches switches = vehicle.switches();
        final boolean blink = VehicleLights.blinkOn(vehicle.level().getGameTime());
        final VehicleBuild build = vehicle.build();
        for (final VehicleDef.Place place : def.places()) {
            if (vehicle.parts().at(place.name()).isEmpty()) {
                hidden.add(place.name());
                hidden.add(place.name() + GLOW);
            }
        }
        if (build != null) {
            for (final VehicleBuild.PartAt<VehiclePartDef.Light> lamp : build.lights()) {
                if (VehicleLights.shine(lamp.spec().role(), switches, lamp.health(), blink) <= 0) {
                    hidden.add(lamp.place().name() + GLOW);
                }
            }
        }
        final String texture = def.paint(vehicle.paint()).flatMap(VehicleDef.Paint::texture)
                .map(ResourceLocation::toString).orElse("");
        return new ModelDefinition.Variant(Map.of(), "", texture, hidden, Set.of());
    }

    /* Turns its wheels as it rolls and steers, drops each on its spring, and swings its doors. */
    private static void move(final VehicleEntity vehicle, final ModelPose pose) {
        final VehicleDef def = vehicle.def();
        final VehicleBuild build = vehicle.build();
        if (def == null || build == null) {
            return;
        }
        final double[] drops = vehicle.wheelDrops();
        for (int i = 0; i < build.wheels().size(); i++) {
            final VehicleBuild.PartAt<VehiclePartDef.Wheel> wheel = build.wheels().get(i);
            final int bone = pose.index(wheel.place().name());
            if (bone < 0) {
                continue;
            }
            final float steer = wheel.place().steers() ? vehicle.steering() * STEER_DEGREES : 0;
            pose.addEuler(bone, vehicle.wheelTurn(), steer, 0);
            if (i < drops.length) {
                pose.addMove(bone, 0, (float) -drops[i] * UNITS, 0);
            }
        }
        final VehicleSwitches switches = vehicle.switches();
        for (int door = 0; door < def.doors().size(); door++) {
            final int bone = pose.index(def.doors().get(door));
            if (bone >= 0 && switches.open(door)) {
                pose.addEuler(bone, 0, DOOR_DEGREES, 0);
            }
        }
        for (final VehicleDef.Place place : def.places()) {
            if (place.kind() == VehiclePartKind.ENGINE || place.kind() == VehiclePartKind.ROTOR
                    || place.kind() == VehiclePartKind.PROPELLER) {
                final int bone = pose.index(place.name() + SPIN);
                if (bone >= 0 && vehicle.running()) {
                    pose.addEuler(bone, 0, 0, (float) (vehicle.rpm() * 6 * KitClock.now() % 360));
                }
            }
        }
    }
}
