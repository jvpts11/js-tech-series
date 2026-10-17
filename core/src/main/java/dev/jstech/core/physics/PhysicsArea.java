/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.region.Box;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A place with laws of its own: a box of blocks, the laws that hold in it, and how it ranks against another area it
 * overlaps (the higher's laws hold where both say).
 *
 * @param id       its name in its dimension, unique there
 * @param box      the blocks it covers
 * @param laws     the laws that hold in it
 * @param priority how it ranks against an area it overlaps
 */
public record PhysicsArea(String id, Box box, Laws laws, int priority) {

    public static final Codec<PhysicsArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PhysicsArea::id),
            Laws.BOX.fieldOf("box").forGetter(PhysicsArea::box),
            Laws.CODEC.fieldOf("laws").forGetter(PhysicsArea::laws),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(PhysicsArea::priority))
            .apply(instance, PhysicsArea::new));

    /** Whether {@code at} lies in it: within its blocks, their far faces excluded. */
    public boolean contains(final Vec3 at) {
        return at.x >= this.box.minX() && at.x < this.box.maxX() + 1 && at.y >= this.box.minY()
                && at.y < this.box.maxY() + 1 && at.z >= this.box.minZ() && at.z < this.box.maxZ() + 1;
    }

    /** The space its blocks fill. */
    public AABB bounds() {
        return new AABB(this.box.minX(), this.box.minY(), this.box.minZ(), this.box.maxX() + 1, this.box.maxY() + 1,
                this.box.maxZ() + 1);
    }
}
