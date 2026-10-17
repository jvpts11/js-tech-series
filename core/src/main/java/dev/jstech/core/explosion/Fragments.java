/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import dev.jstech.core.heat.Thermal;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/*
 * The casing of a blast broken into fragments of its own material, flung out at the speed Gurney's equation gives and
 * slowed by the medium's drag: each flies straight out, the first body on its line takes what it still carries, and a
 * block on the way stops it. They outrun the shock wave, so they strike as the blast goes off; in a vacuum nothing
 * slows them.
 */
final class Fragments {

    /* What each fragment weighs, about, in kilograms, and the most a casing breaks into. */
    private static final double EACH = 0.002;
    private static final int MOST = 128;
    /* The farthest a fragment is followed, in blocks, and how far out it is when it leaves the centre's block. */
    private static final double FARTHEST = 160;
    private static final double CLEAR = 0.9;
    /* A casing of a material that gives no density is taken as steel, in kilograms a cubic metre. */
    private static final double STEEL = 7_850;
    /* The golden angle, which spreads the fragments evenly over the sphere. */
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    private Fragments() {
    }

    /*
     * Flies {@code kilograms} of {@code material} out of the blast {@code detonation} and strikes the bodies they meet;
     * how many fragments struck.
     */
    static int fly(final Detonation detonation, final Material material, final double kilograms) {
        final ServerLevel level = detonation.level();
        final int count = (int) Math.clamp(Math.round(kilograms / EACH), 1, MOST);
        final double each = kilograms / count;
        final double density = MaterialValues.value(material, Thermal.DENSITY, false).orElse(STEEL);
        final double speed = BlastMath.fragmentSpeed(kilograms, detonation.tnt());
        final double slowing = BlastMath.slowing(each, density, detonation.medium().density());
        final double reach = Math.min(FARTHEST, BlastMath.fragmentReach(each, speed, slowing));
        if (reach <= 0) {
            return 0;
        }
        final Vec3 centre = detonation.centre();
        final List<LivingEntity> bodies = level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(centre, reach * 2, reach * 2, reach * 2), body -> body.isAlive() && !body.isSpectator());
        if (bodies.isEmpty()) {
            return 0;
        }
        final Map<LivingEntity, Double> harm = new HashMap<>();
        // Turned by where the blast is, so two blasts do not throw the same pattern.
        final double turn = (centre.x * 0.37 + centre.y * 0.61 + centre.z * 0.89) % (2 * Math.PI);
        int struck = 0;
        for (int i = 0; i < count; i++) {
            final double up = 1 - 2 * (i + 0.5) / count;
            final double round = Math.sqrt(1 - up * up);
            final double angle = i * GOLDEN_ANGLE + turn;
            final Vec3 way = new Vec3(Math.cos(angle) * round, up, Math.sin(angle) * round);
            final Vec3 end = centre.add(way.scale(reach));
            LivingEntity nearest = null;
            double nearestAt = reach;
            Vec3 nearestHit = end;
            for (final LivingEntity body : bodies) {
                final Optional<Vec3> hit = body.getBoundingBox().clip(centre, end);
                if (hit.isPresent()) {
                    final double at = hit.get().distanceTo(centre);
                    if (at < nearestAt) {
                        nearest = body;
                        nearestAt = at;
                        nearestHit = hit.get();
                    }
                }
            }
            // The line is looked along from just out of the centre's block, which held the charge.
            if (nearest == null || level.clip(new ClipContext(centre.add(way.scale(Math.min(nearestAt, CLEAR))),
                    nearestHit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
                    .getType() == HitResult.Type.BLOCK) {
                continue;
            }
            final double left = speed * Math.exp(-nearestAt / slowing);
            harm.merge(nearest, BlastMath.fragmentHarm(BlastMath.kinetic(each, left)), Double::sum);
            struck++;
        }
        final DamageSource shrapnel = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(Explosions.SHRAPNEL), detonation.explosion().getDirectSourceEntity(),
                detonation.explosion().getIndirectSourceEntity());
        for (final Map.Entry<LivingEntity, Double> hit : harm.entrySet()) {
            if (detonation.mayHurt(hit.getKey())) {
                hit.getKey().invulnerableTime = 0;
                hit.getKey().hurt(shrapnel, hit.getValue().floatValue());
            }
        }
        return struck;
    }
}
