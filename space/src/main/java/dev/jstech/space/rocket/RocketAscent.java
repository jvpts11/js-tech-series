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
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.orbit.OrbitScale;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaternionf;

/**
 * A rocket's climb off its pad, as the rule that moves its structure: straight up, a block a metre, its stages' engines
 * pushing it by their thrust at the air the world has, its mass falling as they burn their propellant by the rocket
 * equation, the world's pull drawing it back. A rocket with thrust to spare climbs fast, a weak one slowly, one too
 * heavy for its engines sits on the pad burning; each stage that burns out hands the climb to the next. It goes on till
 * the rocket passes into space, which takes it there whole, or falls back to where it rose from.
 *
 * @param burn    which of its rocket's burns is lit, from the first
 * @param mass    the rocket's mass now, in kilograms
 * @param climb   how fast it climbs, in metres a second; below nought, it falls
 * @param from    the height it rose from
 */
public record RocketAscent(int burn, double mass, double climb, double from) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ascent");
    public static final MapCodec<RocketAscent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("burn").forGetter(RocketAscent::burn),
            Codec.DOUBLE.fieldOf("mass").forGetter(RocketAscent::mass),
            Codec.DOUBLE.fieldOf("climb").forGetter(RocketAscent::climb),
            Codec.DOUBLE.fieldOf("from").forGetter(RocketAscent::from)
    ).apply(instance, RocketAscent::new));
    /* A tick, in seconds. */
    private static final double TICK = 1.0 / 20;
    /* The share of its propellant a stage has left when its low lamp lights; the cabin's air its lamp lights at. */
    private static final float LOW = 0.1F;
    private static final float BREATHABLE = 0.5F;

    /** Declares the rule, once, from the mod's constructor, so a climb is saved and read back with its structure. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /** The climb of a rocket of {@code mass} kilograms lighting its first burn, from the height {@code from}. */
    public static RocketAscent lift(final double mass, final double from) {
        return new RocketAscent(0, mass, 0, from);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!(context.world() instanceof ServerLevel level) || context.structure() == null) {
            return new Step(now, this);
        }
        final DimensionRules rules = DimensionRulesData.of(level);
        if (rules.gravity() <= 0) {
            // Passed into space, where nothing pulls it: the climb is over, and what it burnt is gone from its tanks.
            RocketBurns.climbed(level, context.structure(), this.burn, this.mass);
            return new Step(now, null);
        }
        final List<StageMath.Burn> burns = burns(level, context.structure());
        final double pull = rules.gravity() * OrbitScale.STANDARD_GRAVITY;
        double thrust = 0;
        double mass = this.mass;
        int lit = this.burn;
        if (lit < burns.size()) {
            final StageMath.Burn burning = burns.get(lit);
            final double air = StageMath.air(rules.pressure());
            thrust = Math.max(0, burning.thrust() - (burning.thrust() - burning.seaLevelThrust()) * Math.max(0, air));
            // Its engines burn as much a second whatever the air: their sea-level thrust over their exhaust's speed.
            final double exhaust = exhaust(burning);
            if (exhaust > 0) {
                mass -= burning.seaLevelThrust() / exhaust * TICK;
            }
            if (mass <= burning.endMass()) {
                lit++;
                mass = lit < burns.size() ? burns.get(lit).startMass() : burning.endMass();
            }
        }
        double climb = this.climb + (mass > 0 ? thrust / mass : 0) * TICK - pull * TICK;
        // On the pad it cannot sink into the launcher: it waits there till its thrust lifts it.
        if (now.y() <= this.from && climb < 0) {
            climb = 0;
            if (lit >= burns.size()) {
                return new Step(now, null);
            }
        }
        final StructurePose next = now.movedTo(now.x(), Math.max(this.from, now.y() + climb * TICK), now.z());
        return new Step(next, new RocketAscent(lit, mass, climb, this.from));
    }

    /**
     * What the cabins of {@code structure}, the rocket this climbs, read of it, {@code clock} seconds after lift-off:
     * the propellant the burning stage has left and the whole rocket's, its climb, its engines on full while they
     * burn, its attitude by its pose, and {@code air}, the cabin's, {@code sealed} if a sealed room holds it. The
     * guidance lamp is lit all the climb; the engine's while they burn, the low one and the stage's as a stage nears
     * its end; the cabin's while its sealed air can be breathed.
     */
    public CabinReadings readings(final ServerLevel level, final MovingStructure structure, final int clock,
                                  final float air, final boolean sealed) {
        final List<StageMath.Burn> burns = burns(level, structure);
        float stage = 0;
        double left = 0;
        double all = 0;
        for (int k = 0; k < burns.size(); k++) {
            final StageMath.Burn each = burns.get(k);
            final double load = each.startMass() - each.endMass();
            all += load;
            if (k == this.burn) {
                final double held = Math.max(0, this.mass - each.endMass());
                stage = load > 0 ? (float) (held / load) : 0;
                left += held;
            } else if (k > this.burn) {
                left += load;
            }
        }
        final boolean burning = this.burn < burns.size();
        final EnumSet<CabinLamp> lit = EnumSet.of(CabinLamp.GUIDANCE);
        if (burning) {
            lit.add(CabinLamp.ENGINE);
            if (stage < LOW) {
                lit.add(CabinLamp.LOW_PROPELLANT);
                if (this.burn + 1 < burns.size()) {
                    lit.add(CabinLamp.STAGE);
                }
            }
        }
        if (sealed && air >= BREATHABLE) {
            lit.add(CabinLamp.CABIN_AIR);
        }
        final StructurePose pose = structure.pose();
        final CabinReadings.Attitude attitude = CabinReadings.attitude(new Quaternionf(pose.qx(), pose.qy(), pose.qz(),
                pose.qw()));
        return new CabinReadings(clock, stage, stage, (float) this.climb, burning ? 1 : 0, attitude.pitch(),
                attitude.roll(), attitude.heading(), air, all > 0 ? (float) (left / all) : 0, lit);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public boolean readsWorld() {
        return true;
    }

    /*
     * The burns of the rocket the structure is, with its tanks as they hold now and the engines of its holed parts
     * giving no thrust; none for one not a rocket.
     */
    static List<StageMath.Burn> burns(final ServerLevel level, final MovingStructure structure) {
        final Optional<Rockets.Rocket> rocket = Rockets.get(level.getServer(), structure.id());
        if (rocket.isEmpty()) {
            return List.of();
        }
        return RocketStages.burns(RocketParts.all(level), rocket.get().stack(), Propellants.all(level),
                rocket.get().tanks(), rocket.get().lost().keySet()).orElse(List.of());
    }

    /* The speed of a burn's exhaust, in metres a second, from the speed it gives and the mass it burns. */
    private static double exhaust(final StageMath.Burn burn) {
        final double ratio = burn.startMass() / Math.max(1e-9, burn.endMass());
        return ratio > 1 ? burn.seaLevelDeltaV() / Math.log(ratio) : 0;
    }
}
