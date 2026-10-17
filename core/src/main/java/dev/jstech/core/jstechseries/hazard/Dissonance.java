/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.hazard.HazardEffects;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.IHazardEffect;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.Pull;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A hazard of the physics kit: where the laws of physics round a body depart from the normal ones (time running at
 * another pace, a pull that is not down, light that bends, space that wraps, liquids that rise), the difference builds
 * up in the body, faster the further the laws are from ours, and falls back in normal laws. The bands: I warps the
 * sight, II lets the body's own gravity drift, III makes time stutter, the body caught back where it was and hungry,
 * IV throws it in small jumps through space and harms it slowly, V tears it apart. A bubble of normal laws round the
 * body ({@link IDissonanceShield}, an armour module or a block that makes a zone) keeps it from building up. A planet's
 * own gravity is not a law of its own, and builds up nothing.
 */
@TextHolder
public final class Dissonance {

    public static final TextKey NAME = TextKey.of("jscore.hazard.dissonance", "Dissonance");
    public static final TextKey SHORT = TextKey.of("jscore.hazard.dissonance.short", "DIS");
    public static final TextKey PERCENT = TextKey.of("jscore.hazard.dissonance.unit", "%");
    public static final TextKey SIGHT = TextKey.of("jscore.hazard.dissonance.sight", "Sight warps");
    public static final TextKey GRAVITY = TextKey.of("jscore.hazard.dissonance.gravity", "Gravity drifts");
    public static final TextKey TIME = TextKey.of("jscore.hazard.dissonance.time", "Time stutters");
    public static final TextKey SPACE = TextKey.of("jscore.hazard.dissonance.space", "Jumping through space");
    public static final TextKey APART = TextKey.of("jscore.hazard.dissonance.apart", "Coming apart");
    public static final TextKey TREATMENT = TextKey.of("jscore.hazard.dissonance.treatment",
            "Go back to normal laws, or stand in a bubble of them");
    public static final TextKey DIED = TextKey.of("death.attack.jscore.dissonance",
            "%1$s came apart between the laws");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.dissonance.player",
            "%1$s came apart between the laws while fighting %2$s");

    /** What dissonance kills as, its own death message. */
    public static final ResourceKey<DamageType> TORN = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "dissonance"));
    /* How fast it builds up for each unit the laws are from ours, in percent a second, and how it falls back. */
    private static final double RISE = 1;
    private static final double RECOVERY = 0.5;
    private static final double RECOVERY_SHARE = 0.02;
    /* How much faster it falls back in a bubble of normal laws. */
    private static final double BUBBLE = 2;
    /* How often, a second, gravity drifts, time stutters and space jumps, and how far. */
    private static final double DRIFTS = 0.4;
    private static final double DRIFT = 0.3;
    private static final double STUTTERS = 0.25;
    private static final double FARTHEST_STUTTER = 8;
    private static final double JUMPS = 0.3;
    private static final int JUMP = 3;
    private static final int TRIES = 6;
    private static final List<IDissonanceShield> SHIELDS = new CopyOnWriteArrayList<>();
    /* Where each body was a second ago, for time to catch it back there. */
    private static final Map<LivingEntity, Vec3> WAS = new WeakHashMap<>();

    /** How far the laws round a body are from ours, built up in percent of what kills. */
    public static final HazardKind KIND = Hazards.declare(HazardKind.builder(HazardDevices.DISSONANCE, NAME, PERCENT)
            .band(10, SIGHT, HazardEffects.effect(MobEffects.CONFUSION, 0))
            .band(25, GRAVITY, HazardEffects.effect(MobEffects.CONFUSION, 0), drift())
            .band(45, TIME, HazardEffects.effect(MobEffects.CONFUSION, 0), HazardEffects.effect(MobEffects.HUNGER, 1),
                    stutter())
            .band(70, SPACE, HazardEffects.effect(MobEffects.CONFUSION, 0), HazardEffects.effect(MobEffects.HUNGER, 1),
                    jump(), HazardEffects.damage(0.5F))
            .band(100, APART, HazardEffects.effect(MobEffects.CONFUSION, 0), jump(), HazardEffects.damage(2))
            .recovers(RECOVERY).recoversShare(RECOVERY_SHARE).treatedBy(TREATMENT).deadlyAs(TORN)
            .enabledWhen(HazardBalance::dissonance).upTo(120).shortName(SHORT).build());

    static {
        KIND.addSource((body, level) -> shielded(body) ? 0 : deviation(Physics.lawsOf(body)) * RISE);
        KIND.addRecovery((body, level) -> shielded(body) ? BUBBLE : 0);
    }

    private Dissonance() {
    }

    /** Declares the hazard, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Adds a bubble of normal laws, from a mod's constructor: an armour module, a block that makes a zone. */
    public static void shield(final IDissonanceShield shield) {
        SHIELDS.add(Objects.requireNonNull(shield, "shield"));
    }

    /** Whether {@code body} stands in a bubble of normal laws now. */
    public static boolean shielded(final LivingEntity body) {
        for (final IDissonanceShield shield : SHIELDS) {
            if (shield.shields(body)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far {@code laws} are from ours: how far gravity is from straight down at its usual strength, twice how far
     * time runs from its usual pace, how much light bends, shimmers, darkens or glows with nothing to make it, one for
     * space that wraps or leads elsewhere, one for liquids that rise and a half for an explosion that goes out slowly.
     */
    public static double deviation(final Laws laws) {
        double far = 0;
        if (laws.gravity().isPresent()) {
            far += switch (laws.gravity().get()) {
                case Pull.Scaled scaled -> Math.abs(scaled.g() - 1);
                case Pull.Towards towards -> towards(towards);
                case Pull.Centre centre -> centre.g() + 1;
            };
        }
        far += 2 * Math.abs(laws.timeRate() - 1);
        far += laws.light().map(light -> Math.abs(light.shift()) + light.lens() + light.mirage() + light.dark()
                + light.glow()).orElse(0.0);
        far += laws.space().isPresent() ? 1 : 0;
        far += laws.matter().map(matter -> (matter.fluidsRise() ? 1 : 0) + (matter.explosionSpeed() > 0 ? 0.5 : 0))
                .orElse(0.0);
        return far;
    }

    /* How far a pull along a direction is from ours, straight down at its usual strength. */
    private static double towards(final Pull.Towards towards) {
        final Vec3 way = new Vec3(towards.x(), towards.y(), towards.z());
        if (way.lengthSqr() == 0) {
            return towards.g();
        }
        return way.normalize().scale(towards.g()).distanceTo(new Vec3(0, -1, 0));
    }

    /* Gravity drifts: now and then the body is nudged a little, any way and up. */
    private static IHazardEffect drift() {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                final RandomSource random = body.getRandom();
                if (random.nextDouble() < DRIFTS) {
                    body.push((random.nextDouble() - 0.5) * DRIFT * 2, DRIFT, (random.nextDouble() - 0.5) * DRIFT * 2);
                    body.hurtMarked = true;
                }
            }
        };
    }

    /* Time stutters: now and then the body is caught back where it was a second ago. */
    private static IHazardEffect stutter() {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                final Vec3 was;
                synchronized (WAS) {
                    was = WAS.put(body, body.position());
                }
                if (was != null && body.getRandom().nextDouble() < STUTTERS
                        && was.distanceTo(body.position()) < FARTHEST_STUTTER) {
                    move(body, was);
                }
            }
        };
    }

    /* Space jumps: now and then the body is thrown a few blocks, to somewhere it fits. */
    private static IHazardEffect jump() {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                final RandomSource random = body.getRandom();
                if (random.nextDouble() >= JUMPS) {
                    return;
                }
                for (int i = 0; i < TRIES; i++) {
                    final Vec3 to = body.position().add(random.nextInt(JUMP * 2 + 1) - JUMP, random.nextInt(3) - 1,
                            random.nextInt(JUMP * 2 + 1) - JUMP);
                    if (body.level().noCollision(body, body.getBoundingBox().move(to.subtract(body.position())))) {
                        move(body, to);
                        return;
                    }
                }
            }
        };
    }

    /* Moves {@code body} to {@code to} at once, a player's game told; a player with no game to tell stays. */
    private static void move(final LivingEntity body, final Vec3 to) {
        if (body instanceof ServerPlayer player && player.connection == null) {
            return;
        }
        body.teleportTo(to.x, to.y, to.z);
    }
}
