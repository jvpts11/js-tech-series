/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.descent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.rocket.SpaceAhead;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * A rocket coming down to a world from its orbit, as its flight computer flies it. In the system's space it leaves its
 * orbit for the spot over the landing and sinks through the floor of the low orbits, passing into the world high over
 * the place below; there it falls, glowing where there is air, slows under its parachutes where there is air, and
 * brakes on its engines to its legs where there is little or none, steering a little toward the spot as it comes, till
 * it stands on the ground.
 *
 * @param world  the world it comes down on, by its id among the worlds
 * @param spaceX the spot of the system's space over the landing
 * @param spaceZ the spot of the system's space over the landing
 * @param x      where it lands in the world, west to east
 * @param z      where it lands in the world, north to south
 * @param speed  how fast it comes down now, in blocks a tick
 * @param air    how much air the world has: {@link #NO_AIR}, {@link #THIN_AIR} or {@link #THICK_AIR}
 * @param chutes whether it has parachutes to open: a capsule not burnt up
 * @param brakes whether it can brake on its engines to its legs: legs, and the propellant
 * @param phase  where it is in its coming down
 */
public record RocketDescent(ResourceLocation world, double spaceX, double spaceZ, double x, double z, double speed,
                            int air, boolean chutes, boolean brakes, DescentPhase phase) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "descent");
    public static final MapCodec<RocketDescent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("world").forGetter(RocketDescent::world),
            Codec.DOUBLE.fieldOf("space_x").forGetter(RocketDescent::spaceX),
            Codec.DOUBLE.fieldOf("space_z").forGetter(RocketDescent::spaceZ),
            Codec.DOUBLE.fieldOf("x").forGetter(RocketDescent::x),
            Codec.DOUBLE.fieldOf("z").forGetter(RocketDescent::z),
            Codec.DOUBLE.fieldOf("speed").forGetter(RocketDescent::speed),
            Codec.INT.fieldOf("air").forGetter(RocketDescent::air),
            Codec.BOOL.fieldOf("chutes").forGetter(RocketDescent::chutes),
            Codec.BOOL.fieldOf("brakes").forGetter(RocketDescent::brakes),
            StableCodecs.byName(DescentPhase.class).fieldOf("phase").forGetter(RocketDescent::phase)
    ).apply(instance, RocketDescent::new));
    /** How much air a world has, for coming down through it. */
    public static final int NO_AIR = 0;
    public static final int THIN_AIR = 1;
    public static final int THICK_AIR = 2;
    /* How fast it moves in the system's space, falls through a world's sky, and steers toward the spot, a tick. */
    private static final double SPACE_SPEED = 2.0;
    private static final double FALLING = 3.0;
    private static final double STEERING = 0.3;
    /* How high over the ground the parachutes open, and how fast they let it down in thick air and in thin. */
    private static final double CHUTES_AT = 150;
    private static final double UNDER_THICK = 0.35;
    private static final double UNDER_THIN = 1.0;
    /*
     * How high over the ground its engines start braking where the air is thin, how fast they let it down for every
     * block it still has to fall, and how fast it touches down on its legs.
     */
    private static final double BRAKING_AT = 40;
    private static final double BRAKING_PACE = 40;
    private static final double TOUCHING = 0.15;
    /* How quickly it slows and gathers speed, a tick. */
    private static final double SLOWING = 0.08;
    private static final double GATHERING = 0.05;

    public RocketDescent {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(phase, "phase");
    }

    /** Declares the rule, once, from the mod's constructor, so a descent is saved and read back with its structure. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /**
     * The descent to the block {@code x}, {@code z} of {@code world}, under the spot {@code spot} of the system's
     * space, through {@code air}, with parachutes or not and with what it takes to brake to its legs or not.
     */
    public static RocketDescent of(final ResourceLocation world, final double[] spot, final double x, final double z,
                                   final int air, final boolean chutes, final boolean brakes) {
        return new RocketDescent(world, spot[0], spot[1], x, z, 0, air, chutes, brakes, DescentPhase.DEORBIT);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!(context.world() instanceof ServerLevel level) || context.structure() == null) {
            return new Step(now, this);
        }
        return SystemSpaces.systemOf(level.dimension()).isPresent() ? inSpace(level, now)
                : inSky(level, now, context.structure().worldBox());
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public boolean readsWorld() {
        return true;
    }

    /* Over the spot first, then down through the floor of the low orbits, where it passes into the world below. */
    private Step inSpace(final ServerLevel level, final StructurePose now) {
        final double dx = this.spaceX - now.x();
        final double dz = this.spaceZ - now.z();
        final double away = Math.sqrt(dx * dx + dz * dz);
        final StructurePose next;
        if (away > SPACE_SPEED) {
            next = now.movedTo(now.x() + dx / away * SPACE_SPEED, now.y(), now.z() + dz / away * SPACE_SPEED);
        } else {
            next = now.movedTo(this.spaceX, Math.max(SpaceClimbs.floor() - SPACE_SPEED, now.y() - SPACE_SPEED),
                    this.spaceZ);
        }
        SpaceAhead.hold(level, now.x(), now.y(), now.z());
        SpaceAhead.hold(level, next.x(), next.y(), next.z());
        if (!SpaceAhead.ready(level, next.x(), next.y(), next.z())) {
            return new Step(now, this);
        }
        return new Step(next, this);
    }

    /* Down through the world's sky to the ground under it, as the air there lets it come. */
    private Step inSky(final ServerLevel level, final StructurePose now, final AABB box) {
        SpaceAhead.hold(level, now.x(), now.y(), now.z());
        SpaceAhead.hold(level, this.x, now.y(), this.z);
        final OptionalInt ground = groundUnder(level, box);
        if (ground.isEmpty() || !SpaceAhead.ready(level, now.x(), now.y() - FALLING, now.z())) {
            // The ground under it is not loaded yet: it waits for it, high up.
            return new Step(now, this);
        }
        final double height = box.minY - ground.getAsInt();
        final DescentPhase phase = phaseAt(height);
        final double wanted = switch (phase) {
            case PARACHUTES -> this.air == THICK_AIR ? UNDER_THICK : UNDER_THIN;
            case BRAKING -> Math.clamp(height / BRAKING_PACE, TOUCHING, FALLING);
            default -> FALLING;
        };
        final double speed = wanted < this.speed ? Math.max(wanted, this.speed - SLOWING)
                : Math.min(wanted, this.speed + GATHERING);
        final double steerX = Mth.clamp(this.x + 0.5 - now.x(), -STEERING, STEERING);
        final double steerZ = Mth.clamp(this.z + 0.5 - now.z(), -STEERING, STEERING);
        if (height <= speed) {
            // On the ground: it stands there.
            return new Step(now.movedTo(now.x() + steerX, now.y() - height, now.z() + steerZ), null);
        }
        return new Step(now.movedTo(now.x() + steerX, now.y() - speed, now.z() + steerZ),
                new RocketDescent(this.world, this.spaceX, this.spaceZ, this.x, this.z, speed, this.air, this.chutes,
                        this.brakes, phase));
    }

    /* Where it is in its coming down at {@code height} over the ground. */
    private DescentPhase phaseAt(final double height) {
        if (this.air != NO_AIR && height > CHUTES_AT) {
            return DescentPhase.ENTRY;
        }
        if (this.air == THICK_AIR || this.air == THIN_AIR && height > BRAKING_AT) {
            return this.chutes ? DescentPhase.PARACHUTES : DescentPhase.ENTRY;
        }
        return this.brakes ? DescentPhase.BRAKING : DescentPhase.ENTRY;
    }

    /* The highest ground under {@code box}'s foot, its every column; none while any of them is not loaded. */
    private static OptionalInt groundUnder(final ServerLevel level, final AABB box) {
        int highest = level.getMinBuildHeight();
        for (int bx = Mth.floor(box.minX); bx <= Mth.floor(box.maxX - 1.0e-6); bx++) {
            for (int bz = Mth.floor(box.minZ); bz <= Mth.floor(box.maxZ - 1.0e-6); bz++) {
                if (!level.hasChunk(SectionPos.blockToSectionCoord(bx), SectionPos.blockToSectionCoord(bz))) {
                    return OptionalInt.empty();
                }
                highest = Math.max(highest, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz));
            }
        }
        return OptionalInt.of(highest);
    }
}
