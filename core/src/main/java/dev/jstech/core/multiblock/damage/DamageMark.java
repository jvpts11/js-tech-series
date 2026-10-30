/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The record of a strike on a formed model, as the blocks it reached keep it and send it to the players: the stamp
 * by its name, the point it struck and the way the surface faced there in the model's own space as the model stands
 * facing north, in blocks, the damage's size and the seed of its rough rim. From it the server and every player's
 * game make the same damage; taking it away undoes all of it.
 *
 * @param stamp the stamp's name, among {@link DamageStamps}
 * @param x     the point it struck
 * @param y     the point it struck
 * @param z     the point it struck
 * @param nx    the way the surface faced there
 * @param ny    the way the surface faced there
 * @param nz    the way the surface faced there
 * @param size  how big the damage is, in blocks
 * @param seed  what its rough rim and its petals fall by
 * @param lost  the block that stood where its point is the middle of, when that block was lost: its place leaves the
 *              model and its shape until a block of the same kind is put back there; none for a strike that loses
 *              none
 */
public record DamageMark(ResourceLocation stamp, double x, double y, double z, float nx, float ny, float nz,
                         float size, long seed, Optional<BlockState> lost) {

    /** How a block keeps it and sends it. */
    public static final Codec<DamageMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stamp").forGetter(DamageMark::stamp),
            Codec.DOUBLE.fieldOf("x").forGetter(DamageMark::x),
            Codec.DOUBLE.fieldOf("y").forGetter(DamageMark::y),
            Codec.DOUBLE.fieldOf("z").forGetter(DamageMark::z),
            Codec.FLOAT.fieldOf("nx").forGetter(DamageMark::nx),
            Codec.FLOAT.fieldOf("ny").forGetter(DamageMark::ny),
            Codec.FLOAT.fieldOf("nz").forGetter(DamageMark::nz),
            Codec.FLOAT.fieldOf("size").forGetter(DamageMark::size),
            Codec.LONG.fieldOf("seed").forGetter(DamageMark::seed),
            BlockState.CODEC.optionalFieldOf("lost").forGetter(DamageMark::lost)).apply(instance, DamageMark::new));

    public DamageMark {
        Objects.requireNonNull(stamp, "stamp");
        Objects.requireNonNull(lost, "lost");
    }

    /** The strike it records, if its stamp is declared and it is whole; none for one that is not. */
    public Optional<Strike> strike() {
        final Optional<DamageStamp> declared = DamageStamps.get(this.stamp);
        if (declared.isEmpty() || !(this.size > 0) || this.nx == 0 && this.ny == 0 && this.nz == 0) {
            return Optional.empty();
        }
        return Optional.of(new Strike(declared.get(), this.x, this.y, this.z, this.nx, this.ny, this.nz, this.size,
                this.seed, this.lost.isPresent()));
    }
}
