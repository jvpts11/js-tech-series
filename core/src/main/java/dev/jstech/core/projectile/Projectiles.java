/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.projectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Sends projectiles on their way. */
public final class Projectiles {

    private Projectiles() {
    }

    /**
     * Fires a projectile of {@code type} from {@code from} along {@code direction}, at {@code speed} blocks a tick,
     * credited to {@code owner} when there is one: a turret, a player's launcher.
     *
     * @return the projectile, already in the world
     * @throws IllegalArgumentException when the type makes no projectile in that level
     * @throws IllegalStateException    when the level refuses the projectile, as a client level does
     */
    public static <T extends CoreProjectile> T fire(final Level level, @Nullable final Entity owner,
                                                    final EntityType<T> type, final Vec3 from, final Vec3 direction,
                                                    final float speed) {
        final T projectile = type.create(level);
        if (projectile == null) {
            throw new IllegalArgumentException("the entity type " + type + " makes no projectile here");
        }
        projectile.setOwner(owner);
        projectile.setPos(from);
        projectile.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
        if (!level.addFreshEntity(projectile)) {
            throw new IllegalStateException("the projectile was not added to the level");
        }
        return projectile;
    }

    /** Fires it from {@code shooter}'s eyes the way they look, credited to them. */
    public static <T extends CoreProjectile> T fireFrom(final Entity shooter, final EntityType<T> type,
                                                        final float speed) {
        return fire(shooter.level(), shooter, type, shooter.getEyePosition(), shooter.getLookAngle(), speed);
    }
}
