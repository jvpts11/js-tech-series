/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The pipe marking on a face of a port bound to one product, as plants mark their pipes: what the face lets through
 * and which way it flows. A player saw a row of alike inlets and had to open the machine to learn which took what.
 *
 * @param pos     the port's place, packed
 * @param face    the face of it a pipe meets, out of the machine
 * @param product the product it lets through, a material's id
 * @param in      whether it flows into the machine
 */
public record PortMark(long pos, Direction face, ResourceLocation product, boolean in) {

    public static final Codec<PortMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(PortMark::pos),
            Direction.CODEC.fieldOf("face").forGetter(PortMark::face),
            ResourceLocation.CODEC.fieldOf("product").forGetter(PortMark::product),
            Codec.BOOL.fieldOf("in").forGetter(PortMark::in)).apply(instance, PortMark::new));
}
