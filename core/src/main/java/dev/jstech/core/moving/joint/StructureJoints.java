/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.joint;

import dev.jstech.core.JsCore;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.physics.JointRows;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * The joints between moving structures, and between them and the world: hinges and bearings, sliders and pistons,
 * springs, ropes and chains with their winches, tow bars, ball joints and welds ({@link JointKind}). Kept with the
 * world by the dimension and sent to the players there, so their game draws the ropes; worked in the physics of the
 * structures they hold, each structure moving by its physics working with the others it is joined to as one.
 *
 * <pre>{@code
 * StructureJoints.join(level, JointKind.Hinge.FREE,
 *         StructureJoints.Anchor.on(door, hingeAt, Vec3.Y_AXIS), StructureJoints.Anchor.world(hingeAt, Vec3.Y_AXIS),
 *         Optional.empty());
 * }</pre>
 */
public final class StructureJoints {

    private static final DimensionState<JointBook> BOOK = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_joints"), JointBook.CODEC, JointBook.EMPTY)
            .synced(ByteBufCodecs.fromCodecWithRegistries(JointBook.CODEC))
            .dimension();
    /* Each level's joints by the structures they touch, made again when its book changes; on the server's thread. */
    private static final Map<Level, Index> INDEX = new WeakHashMap<>();

    private StructureJoints() {
    }

    /** Registers the book of joints, once, from the Core's constructor. */
    public static void listen() {
        CoreStates.register(BOOK);
    }

    /**
     * Joins {@code a} and {@code b} by a joint of {@code kind}, each at its anchor's place, turned as they stand now:
     * a hinge turns from here, a weld holds them as they are. One of them, at least, is on a structure.
     *
     * @param breaksAt the load it breaks at, in newtons or newton metres; none for one that never breaks
     * @throws IllegalArgumentException when neither end is on a structure
     */
    public static StructureJoint join(final ServerLevel level, final JointKind kind, final Anchor a, final Anchor b,
                                      final Optional<Double> breaksAt) {
        if (a.structure() == null && b.structure() == null) {
            throw new IllegalArgumentException("a joint holds at least one moving structure");
        }
        final Vector3d axis = new Vector3d(a.axis().x, a.axis().y, a.axis().z);
        if (axis.lengthSquared() < 1.0e-12) {
            axis.set(0, 1, 0);
        }
        axis.normalize();
        final Vector3d normal = new Vector3d();
        JointRows.perpendiculars(axis, normal, new Vector3d());
        final Vec3 worldAxis = new Vec3(axis.x, axis.y, axis.z);
        final Vec3 worldNormal = new Vec3(normal.x, normal.y, normal.z);
        final StructureJoint joint = new StructureJoint(UUID.randomUUID(), a.end(worldAxis, worldNormal),
                b.end(worldAxis, worldNormal), kind, breaksAt);
        BOOK.update(level, book -> book.with(joint));
        wake(level, joint);
        return joint;
    }

    /** Takes away the joint named {@code id}: the structures it held are let go. */
    public static void remove(final ServerLevel level, final UUID id) {
        final StructureJoint joint = byId(level, id);
        if (joint == null) {
            return;
        }
        BOOK.update(level, book -> book.without(id));
        wake(level, joint);
    }

    /** Puts {@code joint} in place of the one of its name: a rope reeled in, an end moved. */
    public static void replace(final ServerLevel level, final StructureJoint joint) {
        BOOK.update(level, book -> book.with(joint));
    }

    /** Takes away every joint touching the structure {@code id}, as it goes for good. */
    public static void forget(final ServerLevel level, final StructureUuid id) {
        if (of(level, id).isEmpty()) {
            return;
        }
        BOOK.update(level, book -> book.changed(joint -> joint.touches(id) ? null : joint));
    }

    /** Every joint of {@code level}. */
    public static List<StructureJoint> all(final ServerLevel level) {
        return BOOK.get(level).joints();
    }

    /** The joint named {@code id} in {@code level}, or null. */
    public static @Nullable StructureJoint byId(final ServerLevel level, final UUID id) {
        return index(level).byId.get(id);
    }

    /** The joints touching the structure {@code id} in {@code level}: none, cheaply, for a structure joined to none. */
    public static List<StructureJoint> of(final ServerLevel level, final StructureUuid id) {
        final List<StructureJoint> found = index(level).byStructure.get(id);
        return found == null ? List.of() : found;
    }

    /** Changes every joint of {@code level} as {@code change} makes it, one it makes null taken away. */
    public static void change(final ServerLevel level, final UnaryOperator<StructureJoint> change) {
        BOOK.update(level, book -> book.changed(change));
    }

    /** On a player's game, the joints of the dimension the player is in, as the server last sent them. */
    public static List<StructureJoint> seen() {
        return BOOK.client().joints();
    }

    /* The index of {@code level}'s joints, made again when its book has changed. */
    private static Index index(final ServerLevel level) {
        final JointBook book = BOOK.get(level);
        final Index known = INDEX.get(level);
        if (known != null && known.book == book) {
            return known;
        }
        final Index made = new Index(book);
        INDEX.put(level, made);
        return made;
    }

    /* Wakes the structures a joint holds, moving by their physics, for them to feel it come or go. */
    private static void wake(final ServerLevel level, final StructureJoint joint) {
        for (final JointEnd end : List.of(joint.a(), joint.b())) {
            end.structure().map(id -> MovingStructures.byId(level, id)).ifPresent(structure -> {
                final StructurePhysics physics = structure.physics();
                if (physics != null) {
                    physics.wake();
                }
            });
        }
    }

    /**
     * Where a joint's end is to be: on a structure, or on the world when it has none, at the world's place
     * {@code at}, with the joint's axis {@code axis} there, a way of the world.
     *
     * @param structure the structure the end is on, or null for the world
     * @param at        where the end is, in the world, as the structure stands now
     * @param axis      the joint's axis, in the world: a hinge's, a slider's
     */
    public record Anchor(@Nullable MovingStructure structure, Vec3 at, Vec3 axis) {

        /** An end on {@code structure} at the world's place {@code at}, the axis {@code axis}. */
        public static Anchor on(final MovingStructure structure, final Vec3 at, final Vec3 axis) {
            return new Anchor(structure, at, axis);
        }

        /** An end on the world at {@code at}, the axis {@code axis}. */
        public static Anchor world(final Vec3 at, final Vec3 axis) {
            return new Anchor(null, at, axis);
        }

        /* This anchor as an end, its ways those of the joint in the world turned into its own. */
        JointEnd end(final Vec3 worldAxis, final Vec3 worldNormal) {
            if (structure == null) {
                return new JointEnd(Optional.empty(), at, worldAxis, worldNormal);
            }
            final StructureFrame frame = structure.frame();
            final Vec3 place = new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z),
                    frame.yardZ(at.x, at.y, at.z));
            return new JointEnd(Optional.of(structure.id()), place, unturned(frame, worldAxis),
                    unturned(frame, worldNormal));
        }

        private static Vec3 unturned(final StructureFrame frame, final Vec3 way) {
            return new Vec3(frame.unturnX(way.x, way.y, way.z), frame.unturnY(way.x, way.y, way.z),
                    frame.unturnZ(way.x, way.y, way.z));
        }
    }

    /* A level's joints, by name and by the structures they touch. */
    private static final class Index {

        private final JointBook book;
        private final Map<UUID, StructureJoint> byId = new HashMap<>();
        private final Map<StructureUuid, List<StructureJoint>> byStructure = new HashMap<>();

        private Index(final JointBook book) {
            this.book = book;
            for (final StructureJoint joint : book.joints()) {
                byId.put(joint.id(), joint);
                joint.a().structure().ifPresent(id -> byStructure.computeIfAbsent(id, any -> new ArrayList<>())
                        .add(joint));
                joint.b().structure().ifPresent(id -> {
                    if (!joint.a().on(id)) {
                        byStructure.computeIfAbsent(id, any -> new ArrayList<>()).add(joint);
                    }
                });
            }
        }
    }
}
