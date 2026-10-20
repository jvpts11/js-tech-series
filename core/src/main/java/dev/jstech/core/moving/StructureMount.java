/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Optional;
import net.minecraft.core.BlockPos;

/**
 * A moving structure held by another, as a turret on a ship, a helicopter's rotor or a hangar door, or a shuttle
 * docked to a station: where it stands is its parent's place for it, {@code local}, a pose in the parent's yard, so it
 * goes wherever the parent goes, and its own rule, if it has one, turns it there, in the parent's yard. The block of
 * the parent that holds it, its bearing or its clamp, is kept: when that block goes, the structure is let go.
 *
 * @param parent the structure holding it
 * @param local  where it stands on its parent: its pivot's place in the parent's yard, its rotation from that yard's
 *               axes
 * @param anchor the parent's block that holds it, a place of the parent's yard, or none
 */
public record StructureMount(StructureUuid parent, StructurePose local, Optional<BlockPos> anchor) {

    public static final Codec<StructureMount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureUuid.CODEC.fieldOf("parent").forGetter(StructureMount::parent),
            StructureCodecs.POSE.fieldOf("local").forGetter(StructureMount::local),
            BlockPos.CODEC.optionalFieldOf("anchor").forGetter(StructureMount::anchor)
    ).apply(instance, StructureMount::new));

    /** This mount with the structure standing at {@code next} on its parent. */
    public StructureMount at(final StructurePose next) {
        return new StructureMount(parent, next, anchor);
    }
}
