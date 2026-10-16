/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Where on a block an effect plays, as its anchor says: the middle of a face, facing out of it; a locator of the model
 * the block is drawn with, turned as the block faces and moving as the model does; or, saying none, the middle of its
 * top. A locator the block's model has none of plays at its top too.
 */
public final class EffectAnchors {

    private static final float PIXEL = 1.0F / 16.0F;
    private static final double HALF = 0.5;

    private EffectAnchors() {
    }

    /** Where {@code anchor} is on the block at {@code pos} now, and which way it faces. */
    public static Placed of(final Level level, final BlockPos pos, final Optional<EffectDefinition.Anchor> anchor) {
        final EffectDefinition.Anchor said = anchor.orElse(EffectDefinition.Anchor.at(Direction.UP));
        Placed placed = null;
        if (!said.locator().isEmpty()) {
            placed = locator(level, pos, said.locator());
        }
        if (placed == null) {
            final Direction face = said.face().orElse(Direction.UP);
            placed = new Placed(Vec3.atCenterOf(pos).relative(face, HALF), Vec3.atLowerCornerOf(face.getNormal()));
        }
        return new Placed(placed.at().add(said.offset(0), said.offset(1), said.offset(2)), placed.way());
    }

    /** The locators of the model the block at {@code pos} is drawn with, by name; none for a block drawn otherwise. */
    public static List<String> locators(final Level level, final BlockPos pos) {
        final BlockEntity entity = level.getBlockEntity(pos);
        final List<String> out = new ArrayList<>();
        if (entity != null) {
            KitAnimations.drawn(entity).ifPresent(instance -> {
                for (final ModelBone bone : instance.model().data().bones()) {
                    for (final ModelLocator locator : bone.locators()) {
                        out.add(locator.name());
                    }
                }
            });
        }
        return List.copyOf(out);
    }

    /* The locator {@code name} of the block's drawn model, where it is in the world now; none if it has none. */
    private static Placed locator(final Level level, final BlockPos pos, final String name) {
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return null;
        }
        final Optional<KitInstance> drawn = KitAnimations.drawn(entity);
        if (drawn.isEmpty() || !locators(level, pos).contains(name)) {
            return null;
        }
        final KitInstance instance = drawn.get();
        final Vector3f at = instance.pose().locator(name, instance.world(), new Vector3f()).mul(PIXEL);
        final Vector3f up = new Vector3f(0, 1, 0);
        turn(level.getBlockState(pos), at);
        turn(level.getBlockState(pos), up);
        return new Placed(new Vec3(pos.getX() + HALF + at.x, pos.getY() + at.y, pos.getZ() + HALF + at.z),
                new Vec3(up.x, up.y, up.z));
    }

    /* Turns a point of a model facing north as the block faces, as the block's model is turned to be drawn. */
    private static void turn(final BlockState state, final Vector3f point) {
        final Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING)
                : Direction.NORTH;
        switch (facing) {
            case SOUTH -> point.rotateY((float) Math.PI);
            case WEST -> point.rotateY((float) (Math.PI / 2));
            case EAST -> point.rotateY((float) (-Math.PI / 2));
            case UP -> point.rotateX((float) (Math.PI / 2));
            case DOWN -> point.rotateX((float) (-Math.PI / 2));
            default -> {
                // A model faces north as it is drawn.
            }
        }
    }

    /**
     * Where an effect plays and which way it faces.
     *
     * @param at  where
     * @param way which way, a unit vector
     */
    public record Placed(Vec3 at, Vec3 way) {
    }
}
