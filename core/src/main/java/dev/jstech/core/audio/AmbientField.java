/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import net.minecraft.resources.ResourceLocation;

/**
 * A kind of place many sources together make: a datacenter out of racks, a factory floor out of machines. When enough
 * sources of the field stand close together they stop being heard one by one and the field's bed is heard instead,
 * from their middle, louder the more of them there are.
 *
 * @param id        what sources name to belong to it
 * @param bed       the looping sound of the whole place
 * @param threshold how many sources close together make the place
 * @param radius    how far from the place's middle a source may stand and still belong to it, in blocks
 */
public record AmbientField(ResourceLocation id, SoundKey bed, int threshold, double radius) {

    public AmbientField {
        if (!bed.spec().loop()) {
            throw new IllegalArgumentException(bed.id() + " does not loop, so it cannot be the sound of a place");
        }
        if (threshold < 2) {
            throw new IllegalArgumentException("a place is made of at least two sources: " + threshold);
        }
    }
}
