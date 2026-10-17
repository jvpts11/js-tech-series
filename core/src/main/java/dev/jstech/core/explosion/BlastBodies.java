/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import dev.jstech.core.audio.acoustics.Acoustics;
import dev.jstech.core.audio.acoustics.EarRing;
import dev.jstech.core.audio.acoustics.EarRingPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/*
 * What the shock wave does to a body it reaches: the overpressure where it stands, less what the walls between it and
 * the blast took, hurts it and throws it away; inside the fireball it catches fire where there is air to burn, and in
 * a vacuum the heat of it scorches. A player's ears ring as the wave arrives, as loud as it was there.
 */
final class BlastBodies {

    /* What share of a throw lifts the body as well, and the heat a body in the fireball takes in a vacuum. */
    private static final double LIFT = 0.4;
    private static final float HEAT_FLASH = 4F;
    /* How long a body in the fireball burns, in seconds; longer when the blast was made to set fires. */
    private static final float BURNS_FOR = 3F;
    private static final float BURNS_FOR_INCENDIARY = 8F;
    /* Nearer than this to the centre, a body is thrown straight up. */
    private static final double AT_THE_CENTRE = 1.0e-3;

    private BlastBodies() {
    }

    /* Strikes {@code body}, the wave having reached it. */
    static void strike(final Detonation detonation, final Entity body) {
        if (!body.isAlive() || body.isSpectator() || !detonation.mayHurt(body)
                || body.ignoreExplosion(detonation.explosion())) {
            return;
        }
        final ServerLevel level = detonation.level();
        final Vec3 centre = detonation.centre();
        final Vec3 middle = body.getBoundingBox().getCenter();
        final double distance = middle.distanceTo(centre);
        final double free = detonation.medium().overpressure(detonation.tnt(), distance);
        final double pressure = free > BlastMath.HARMLESS ? free * shielding(detonation, middle) : 0;
        final double harm = BlastMath.harm(pressure);
        if (harm > 0) {
            body.invulnerableTime = 0;
            body.hurt(level.damageSources().explosion(detonation.explosion()), (float) harm);
        }
        double thrown = BlastMath.thrown(pressure);
        if (body instanceof LivingEntity living) {
            thrown *= 1 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        }
        final boolean flying = body instanceof Player player && player.isCreative() && player.getAbilities().flying;
        if (thrown > 0 && !flying) {
            final Vec3 away = distance > AT_THE_CENTRE ? middle.subtract(centre).scale(1 / distance)
                    : new Vec3(0, 1, 0);
            body.push(away.x * thrown, away.y * thrown + LIFT * thrown, away.z * thrown);
            body.hurtMarked = true;
        }
        if (distance <= detonation.fireball()) {
            if (detonation.burns()) {
                body.igniteForSeconds(detonation.incendiary() ? BURNS_FOR_INCENDIARY : BURNS_FOR);
            } else {
                body.invulnerableTime = 0;
                body.hurt(level.damageSources().inFire(), HEAT_FLASH);
            }
        }
    }

    /*
     * What share of the wave reaches {@code to}: each whole block standing between it and the centre takes what it
     * stands, as a share of the wave there; a wall it cannot break shields it wholly.
     */
    static double shielding(final Detonation detonation, final Vec3 to) {
        final Vec3 centre = detonation.centre();
        final double length = to.distanceTo(centre);
        if (length <= AT_THE_CENTRE) {
            return 1;
        }
        final ServerLevel level = detonation.level();
        final BlastRay ray = new BlastRay(centre.x, centre.y, centre.z, (to.x - centre.x) / length,
                (to.y - centre.y) / length, (to.z - centre.z) / length);
        ray.advance();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        double share = 1;
        while (ray.nextDistance() < length) {
            ray.advance();
            at.set(ray.x(), ray.y(), ray.z());
            if (!level.isLoaded(at) || detonation.opened(at.asLong())) {
                continue;
            }
            final BlockState state = level.getBlockState(at);
            if (state.isAir() || !state.isCollisionShapeFullBlock(level, at)) {
                continue;
            }
            final double free = detonation.medium().overpressure(detonation.tnt(), ray.distance());
            share -= BlastStrength.of(level, at, state, detonation.explosion()) / free;
            if (share <= 0) {
                return 0;
            }
        }
        return share;
    }

    /* Rings {@code player}'s ears as the wave reaches them. */
    static void ring(final Detonation detonation, final ServerPlayer player) {
        final EarRing ring = Acoustics.ringOf(player, detonation.centre(),
                detonation.earReach() / EarRing.REACH_PER_POWER);
        if (ring.rings() && player.connection != null) {
            PacketDistributor.sendToPlayer(player, new EarRingPayload((float) ring.strength(), ring.ticks()));
        }
    }
}
