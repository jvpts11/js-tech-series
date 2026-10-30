/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.effect.Effects;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureAssembly;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.StageFate;
import dev.jstech.space.rocket.TankLoad;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket leaving its pad: at ignition the pad shakes, its deluge turning to steam and smoke rolling out; at release
 * the rocket's blocks become one moving structure by the exact places of its plan, each as it is, its formed parts
 * still formed and nothing checked again, its flight computer going with it and counting the flight; it is told as a
 * rocket with what its tanks hold, and it climbs on its engines' thrust, its plume following it.
 */
public final class RocketLaunch {

    /** The pad's ignition, the rocket's plume as it climbs. */
    public static final ResourceLocation IGNITION = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "launch");
    public static final ResourceLocation PLUME = ResourceLocation.fromNamespaceAndPath("jscore", "rocket_plume");
    /* How far the launch is seen and felt, in blocks. */
    private static final int SEEN = 256;
    private static final UUID NOBODY = new UUID(0, 0);

    private RocketLaunch() {
    }

    /** The engines light over the pad whose rocket's axis meets the deck at {@code axis}: the pad shakes and steams. */
    public static void ignite(final ServerLevel level, final BlockPos axis) {
        Effects.at(level, IGNITION, Vec3.atBottomCenterOf(axis)).reach(SEEN).send();
    }

    /**
     * Releases the rocket whose flight computer stands at {@code computer}, its LAUNCH pressed by {@code by}: its
     * blocks become one moving structure by the places of its plan, told as a rocket with its tanks' loads; none where
     * there is no rocket there, or its blocks cannot go.
     */
    public static Optional<MovingStructure> release(final ServerLevel level, final BlockPos computer,
                                                    @Nullable final UUID by) {
        if (!(level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket)
                || rocket.rocket().isEmpty() || rocket.foot().isEmpty()) {
            return Optional.empty();
        }
        final RocketDesign design = rocket.rocket().get();
        final BlockPos foot = computer.offset(rocket.foot().get());
        final Map<Integer, TankLoad> tanks = rocket.tanks();
        // Where its stages go, as its STG page set them: a fall zone round where it stands now.
        final Map<Integer, StageFate> fates = rocket.fates(level.dimension(), foot);
        final BuildPlan plan = RocketPlans.of(level, design.stack());
        final List<BlockPos> places = new ArrayList<>();
        for (final PlanSpot spot : plan.spots()) {
            final BlockPos at = foot.offset(spot.at());
            if (!level.getBlockState(at).isAir()) {
                places.add(at);
            }
        }
        // Its computer goes with it, its clock counting the flight from now.
        rocket.liftOff(level.getGameTime());
        final StructureResult made = StructureAssembly.blocks(level, places, Vec3.atBottomCenterOf(foot), by);
        if (!made.ok() || made.structure() == null) {
            rocket.liftOff(FlightComputerBlockEntity.NOT_LIFTED);
            return Optional.empty();
        }
        final MovingStructure body = made.structure();
        Rockets.register(level.getServer(), body.id(), design.stack(), by == null ? NOBODY : by);
        tanks.forEach((section, load) -> Rockets.load(level.getServer(), body.id(), section, load));
        fates.forEach((stage, fate) -> Rockets.fate(level.getServer(), body.id(), stage, fate));
        return Optional.of(body);
    }

    /** Sets the released rocket {@code body} climbing from where it stands, once it is ready, its plume after it. */
    public static void lift(final ServerLevel level, final MovingStructure body) {
        final double mass = Rockets.get(level.getServer(), body.id()).flatMap(rocket -> RocketStages.burns(
                RocketParts.all(level), rocket.stack(), Propellants.all(level), rocket.tanks()))
                .filter(burns -> !burns.isEmpty()).map(burns -> burns.get(0).startMass()).orElse(0.0);
        body.move(RocketAscent.lift(mass, body.pose().y()));
        if (body.entity() != null) {
            Effects.at(level, PLUME, new Vec3(body.pose().x(), body.pose().y(), body.pose().z()))
                    .facing(new Vec3(0, -1, 0)).following(body.entity()).reach(SEEN).send();
        }
    }
}
