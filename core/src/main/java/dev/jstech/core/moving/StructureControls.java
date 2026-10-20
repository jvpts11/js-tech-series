/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.id.IStableName;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * What a moving structure is told to do now, from its helm or its autopilot: forward, back, to the left, to the right,
 * up and down, each from nothing to full, and the way its forward is in the world. Its engines and wheels take them
 * by the way each looks against that forward: an engine pushing forward answers forward, one pushing up answers up; a
 * wheel rolling forward drives forward and back, and steers left and right. Told nothing for a second, it lets go,
 * every control back to nothing, so a pilot who left the helm leaves the structure to its signals.
 */
public final class StructureControls {

    private final Map<ControlAxis, Float> values = new EnumMap<>(ControlAxis.class);
    private Vec3 forward = new Vec3(0, 0, -1);
    private Vec3 up = new Vec3(0, 1, 0);
    private long toldAt = Long.MIN_VALUE / 2;
    private long pilotAt = Long.MIN_VALUE / 2;
    private boolean docking;

    /* How long controls last untold, in ticks. */
    private static final int LASTS = 20;
    /* An engine looking within this of a control's way answers it. */
    private static final double ANSWERS = 0.7;

    /**
     * Tells the structure what to do now, at the game's tick {@code now}: each control from 0 to 1, its forward and up
     * in the world, and whether its clamps are to close as if signalled.
     */
    public void tell(final long now, final Map<ControlAxis, Float> told, final Vec3 forwardWay, final Vec3 upWay,
                     final boolean dock) {
        values.clear();
        for (final Map.Entry<ControlAxis, Float> each : told.entrySet()) {
            values.put(each.getKey(), Math.max(0F, Math.min(1F, each.getValue())));
        }
        this.forward = forwardWay.lengthSqr() > 0 ? forwardWay.normalize() : forward;
        this.up = upWay.lengthSqr() > 0 ? upWay.normalize() : up;
        this.toldAt = now;
        this.docking = dock;
    }

    /**
     * Tells the structure what its pilot at a helm holds now, at the game's tick {@code now}: the autopilot, if it
     * has one, leaves the controls to the pilot while they hold the helm.
     */
    public void pilot(final long now, final Map<ControlAxis, Float> held, final Vec3 forwardWay, final Vec3 upWay) {
        tell(now, held, forwardWay, upWay, false);
        pilotAt = now;
    }

    /** Whether a pilot at a helm told it what to do in the last second. */
    public boolean piloted(final long now) {
        return now - pilotAt <= LASTS;
    }

    /** Lets go of every control. */
    public void release() {
        values.clear();
        docking = false;
        toldAt = Long.MIN_VALUE / 2;
    }

    /** Whether it was told anything in the last second, at the game's tick {@code now}. */
    public boolean live(final long now) {
        return now - toldAt <= LASTS;
    }

    /** How strongly {@code axis} is held now, from 0 to 1; nothing once untold a second. */
    public float value(final ControlAxis axis, final long now) {
        return live(now) ? values.getOrDefault(axis, 0F) : 0F;
    }

    /** Whether its clamps are to close as if signalled, now. */
    public boolean docking(final long now) {
        return live(now) && docking;
    }

    /** The way its forward is in the world, as last told. */
    public Vec3 forward() {
        return forward;
    }

    /** The way its up is in the world, as last told. */
    public Vec3 up() {
        return up;
    }

    /**
     * How strongly an engine pushing along {@code way}, a way of the world, is to push now: the control whose way it
     * looks along, from 0 to 1; nothing for an engine looking along none.
     */
    public float forEngine(final Vec3 way, final long now) {
        if (!live(now)) {
            return 0F;
        }
        final Vec3 right = forward.cross(up);
        if (way.dot(forward) > ANSWERS) {
            return value(ControlAxis.FORWARD, now);
        }
        if (way.dot(forward) < -ANSWERS) {
            return value(ControlAxis.BACK, now);
        }
        if (way.dot(up) > ANSWERS) {
            return value(ControlAxis.UP, now);
        }
        if (way.dot(up) < -ANSWERS) {
            return value(ControlAxis.DOWN, now);
        }
        if (way.dot(right) > ANSWERS) {
            return value(ControlAxis.RIGHT, now);
        }
        if (way.dot(right) < -ANSWERS) {
            return value(ControlAxis.LEFT, now);
        }
        return 0F;
    }

    /**
     * How a wheel rolling along {@code way}, a way of the world, is to drive now, from -1 back to 1 forward, by the
     * controls forward and back as it looks against the structure's forward.
     */
    public float forWheel(final Vec3 way, final long now) {
        final float drive = value(ControlAxis.FORWARD, now) - value(ControlAxis.BACK, now);
        if (way.dot(forward) > ANSWERS) {
            return drive;
        }
        if (way.dot(forward) < -ANSWERS) {
            return -drive;
        }
        return 0F;
    }

    /** How a wheel is to steer now, from -1 right to 1 left. */
    public float steering(final long now) {
        return value(ControlAxis.LEFT, now) - value(ControlAxis.RIGHT, now);
    }

    /** One control of a structure. */
    public enum ControlAxis implements IStableName {
        FORWARD("forward"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        UP("up"),
        DOWN("down");

        private final String serializedName;

        ControlAxis(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
