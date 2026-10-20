/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.joint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The joints of a dimension, kept with the world.
 *
 * @param joints every joint, in the order they were made
 */
public record JointBook(List<StructureJoint> joints) {

    /** A dimension with no joint. */
    public static final JointBook EMPTY = new JointBook(List.of());
    public static final Codec<JointBook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureJoint.CODEC.listOf().fieldOf("joints").forGetter(JointBook::joints)
    ).apply(instance, JointBook::new));

    public JointBook {
        joints = List.copyOf(joints);
    }

    /** This book with {@code joint} too, in place of one of the same name. */
    public JointBook with(final StructureJoint joint) {
        final List<StructureJoint> next = new ArrayList<>(joints.size() + 1);
        boolean replaced = false;
        for (final StructureJoint each : joints) {
            if (each.id().equals(joint.id())) {
                next.add(joint);
                replaced = true;
            } else {
                next.add(each);
            }
        }
        if (!replaced) {
            next.add(joint);
        }
        return new JointBook(next);
    }

    /** This book without the joint named {@code id}. */
    public JointBook without(final UUID id) {
        final List<StructureJoint> next = new ArrayList<>(joints);
        return next.removeIf(each -> each.id().equals(id)) ? new JointBook(next) : this;
    }

    /** This book with each joint as {@code change} makes it, a joint it makes null taken away. */
    public JointBook changed(final UnaryOperator<StructureJoint> change) {
        final List<StructureJoint> next = new ArrayList<>(joints.size());
        boolean any = false;
        for (final StructureJoint each : joints) {
            final StructureJoint made = change.apply(each);
            any |= made != each;
            if (made != null) {
                next.add(made);
            }
        }
        return any ? new JointBook(next) : this;
    }
}
