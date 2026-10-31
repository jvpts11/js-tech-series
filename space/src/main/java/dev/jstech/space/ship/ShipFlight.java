/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * A small ship that has left its dock, flown as a moving structure of J's Core a tick at a time: its blocks fly as
 * they stand, its machines work, its crew walk about inside. Its pilot at the helm, the cockpit's pilot seat, sends it
 * on along its nose, slows it, turns it and takes it up or down, by the controls they hold; with nobody at the helm
 * it slows to a stop and stays where it is, parked. Its rule goes on through a save, as the structure does.
 *
 * @param speed how fast it goes along its nose, in blocks a tick
 * @param climb how fast it goes up, in blocks a tick, down below nought
 * @param noseX which way its nose points among its blocks, across, as its plan laid it
 * @param noseZ which way its nose points among its blocks, along
 */
public record ShipFlight(double speed, double climb, int noseX, int noseZ) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_flight");
    public static final MapCodec<ShipFlight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("speed").forGetter(ShipFlight::speed),
            Codec.DOUBLE.fieldOf("climb").forGetter(ShipFlight::climb),
            Codec.INT.fieldOf("nose_x").forGetter(ShipFlight::noseX),
            Codec.INT.fieldOf("nose_z").forGetter(ShipFlight::noseZ)
    ).apply(instance, ShipFlight::new));
    /*
     * How it answers its controls, a tick at a time: the fastest it goes forward and back, how fast it gathers speed
     * and loses it with no hand on the throttle, the fastest it turns, in radians, and climbs.
     */
    private static final double MOST_AHEAD = 1.0;
    private static final double MOST_ASTERN = 0.15;
    private static final double GATHERS = 0.02;
    private static final double LOSES = 0.03;
    private static final double TURNS = Math.toRadians(2.25);
    private static final double MOST_CLIMB = 0.25;
    private static final double STILL = 1.0e-4;

    /** Registers the rule's kind, once, from the mod's constructor. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /** A ship at rest, its nose pointing {@code noseX}, {@code noseZ} among its blocks. */
    public static ShipFlight resting(final int noseX, final int noseZ) {
        return new ShipFlight(0, 0, noseX, noseZ);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!(context.world() instanceof ServerLevel level) || !(context.structure() instanceof MovingStructure ship)) {
            return new Step(now, this);
        }
        final StructureControls controls = ship.controls();
        final long tick = level.getGameTime();
        final boolean held = controls.piloted(tick) && controls.live(tick);
        final double throttle = held ? controls.value(StructureControls.ControlAxis.FORWARD, tick)
                - controls.value(StructureControls.ControlAxis.BACK, tick) : 0;
        final double turn = held ? controls.value(StructureControls.ControlAxis.LEFT, tick)
                - controls.value(StructureControls.ControlAxis.RIGHT, tick) : 0;
        final double lift = held ? controls.value(StructureControls.ControlAxis.UP, tick)
                - controls.value(StructureControls.ControlAxis.DOWN, tick) : 0;
        final double nextSpeed = toward(this.speed, throttle >= 0 ? throttle * MOST_AHEAD : throttle * MOST_ASTERN,
                throttle == 0 ? LOSES : GATHERS);
        final double nextClimb = toward(this.climb, lift * MOST_CLIMB, lift == 0 ? LOSES : GATHERS);
        if (Math.abs(nextSpeed) < STILL && Math.abs(nextClimb) < STILL && turn == 0) {
            // Parked: it stays where it is.
            return new Step(now, resting(this.noseX, this.noseZ));
        }
        final StructureFrame frame = context.frame();
        final double wayX = frame.turnX(this.noseX, 0, this.noseZ);
        final double wayZ = frame.turnZ(this.noseX, 0, this.noseZ);
        final double flat = Math.hypot(wayX, wayZ);
        final double ahead = flat < STILL ? 0 : nextSpeed / flat;
        final StructurePose moved = now.movedTo(now.x() + wayX * ahead, now.y() + nextClimb, now.z() + wayZ * ahead);
        final StructurePose next = turn == 0 ? moved : moved.turnedBy(0, 1, 0, turn * TURNS);
        return new Step(next, new ShipFlight(nextSpeed, nextClimb, this.noseX, this.noseZ));
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    /* The rule reads its pilot's controls, which only the structure itself holds. */
    @Override
    public boolean readsWorld() {
        return true;
    }

    /* {@code from} moved toward {@code to} by no more than {@code by}. */
    private static double toward(final double from, final double to, final double by) {
        return from < to ? Math.min(to, from + by) : Math.max(to, from - by);
    }
}
