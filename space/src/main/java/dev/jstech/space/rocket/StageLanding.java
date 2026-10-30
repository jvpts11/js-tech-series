/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;

/**
 * A spent stage coming back to its platform, as Falcon 9's first stage does, as the rule that moves its structure:
 * it flies back to a point high over the platform, then comes straight down on its engine, braking harder as the
 * ground comes up, and touches down gently on the platform's middle, where it rests.
 *
 * @param x       where its pose stands once landed, east
 * @param y       where its pose stands once landed, up
 * @param z       where its pose stands once landed, south
 * @param falling whether it has turned to come straight down
 */
public record StageLanding(double x, double y, double z, boolean falling) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "stage_landing");
    public static final MapCodec<StageLanding> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(StageLanding::x),
            Codec.DOUBLE.fieldOf("y").forGetter(StageLanding::y),
            Codec.DOUBLE.fieldOf("z").forGetter(StageLanding::z),
            Codec.BOOL.fieldOf("falling").forGetter(StageLanding::falling)
    ).apply(instance, StageLanding::new));
    /* How high over the platform it turns to come down, in blocks. */
    private static final double TURN_OVER = 64;
    /*
     * How fast it flies back, comes down at most and touches down, in blocks a second; how hard its landing burn
     * brakes, in metres a second a second; a tick, in seconds.
     */
    private static final double BACK = 40;
    private static final double FASTEST = 60;
    private static final double TOUCH = 2;
    private static final double BRAKE = 12;
    private static final double TICK = 1.0 / 20;

    /** Declares the rule, once, from the mod's constructor, so a landing is saved and read back with its structure. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /** A stage coming back to land with its pose at {@code (x, y, z)}. */
    public static StageLanding to(final double x, final double y, final double z) {
        return new StageLanding(x, y, z, false);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!this.falling) {
            final double dx = this.x - now.x();
            final double dy = this.y + TURN_OVER - now.y();
            final double dz = this.z - now.z();
            final double far = Math.sqrt(dx * dx + dy * dy + dz * dz);
            final double reach = BACK * TICK;
            if (far <= reach) {
                return new Step(now.movedTo(this.x, this.y + TURN_OVER, this.z),
                        new StageLanding(this.x, this.y, this.z, true));
            }
            return new Step(now.movedTo(now.x() + dx / far * reach, now.y() + dy / far * reach,
                    now.z() + dz / far * reach), this);
        }
        final double height = now.y() - this.y;
        final double speed = Math.max(TOUCH, Math.min(FASTEST, Math.sqrt(2 * BRAKE * Math.max(0, height))));
        if (height <= speed * TICK) {
            // Down on the platform: it rests there.
            return new Step(now.movedTo(this.x, this.y, this.z), null);
        }
        return new Step(now.movedTo(this.x, now.y() - speed * TICK, this.z), this);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
