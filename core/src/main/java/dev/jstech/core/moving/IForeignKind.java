/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A kind of moving structure of another mod, a contraption or a ship of its own, as an adapter behind that mod's
 * guard tells the kit of it: the ones whose blocks may be in a box of the world, the region of the world it keeps
 * its blocks in, if it keeps them in one, and where a place of that region is in the world. The kit sees each as an
 * obstacle that moves ({@link ForeignMover}), never takes the blocks of that region into a structure of its own, and
 * asks the region's places of the world where they are.
 */
public interface IForeignKind {

    /** What a player calls the mod of these structures, as a refusal names it. */
    String name();

    /** Hands {@code into} each structure of this kind in {@code level} whose blocks may be in {@code box}. */
    void near(Level level, AABB box, Consumer<ForeignMover> into);

    /** Whether {@code pos} is in the region this kind keeps its structures' blocks in. */
    boolean holds(Level level, BlockPos pos);

    /** Where the place {@code place} of that region is in the world now; any other place as it is. */
    Vec3 inWorld(Level level, Vec3 place);

    /**
     * Whether {@code entity} is one of these structures, which its own mod places each tick: the kit's structures meet
     * it by its blocks, and never push it aside as they push what stands in their way.
     */
    default boolean isMover(final Entity entity) {
        return false;
    }
}
