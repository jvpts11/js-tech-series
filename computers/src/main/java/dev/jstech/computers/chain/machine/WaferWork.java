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
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.tier.Era;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Where a wafer stands on its way to chips (computers/docs/design/survival.md, "How a chip is made"), kept on a wafer
 * between its steps and on a finished wafer: the chip its mask makes of it, the layer it is on of the layers its era's
 * chips have, and the machine it goes to next, which its item says too ({@link #item}). Each layer goes through the
 * Lithography Station, the Plasma Etcher, the Ion Implanter and the Deposition Chamber in turn; the finished wafer,
 * through the Packaging Line.
 *
 * @param chip   the item it is cut into: a chip, a laser diode or an LED
 * @param layer  the layer it is on, from 1
 * @param layers the layers it has in all
 * @param next   the machine it goes to next
 */
public record WaferWork(ResourceLocation chip, int layer, int layers, Step next) {

    public static final Codec<WaferWork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("chip").forGetter(WaferWork::chip),
            Codec.INT.fieldOf("layer").forGetter(WaferWork::layer),
            Codec.INT.fieldOf("layers").forGetter(WaferWork::layers),
            StableCodecs.byName(Step.class).fieldOf("next").forGetter(WaferWork::next)).apply(instance,
            WaferWork::new));
    public static final StreamCodec<ByteBuf, WaferWork> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** The same wafer gone on to {@code step}, on the same layer. */
    public WaferWork to(final Step step) {
        return new WaferWork(chip, layer, layers, step);
    }

    /**
     * The item a silicon wafer of {@code era} is while it goes to {@code next}, a wafer of light of that era too:
     * exposed on its way to the etcher, etched to the implanter, doped to the deposition chamber, metallised back to
     * the lithography station, and finished to the packaging line.
     */
    public static Item item(final Era era, final Step next) {
        final int at = ChainParts.ERAS.indexOf(era);
        return (switch (next) {
            case LITHOGRAPHY -> ChainChips.METALLISED_WAFERS;
            case ETCH -> ChainChips.EXPOSED_WAFERS;
            case IMPLANT -> ChainChips.ETCHED_WAFERS;
            case DEPOSITION -> ChainChips.DOPED_WAFERS;
            case PACKAGING -> ChainParts.FINISHED_WAFERS;
        }).get(at).get();
    }

    /** Whether this was its last layer. */
    public boolean lastLayer() {
        return layer >= layers;
    }

    /** The machine a wafer goes to next. */
    public enum Step implements IStableName {
        LITHOGRAPHY("lithography"),
        ETCH("etch"),
        IMPLANT("implant"),
        DEPOSITION("deposition"),
        PACKAGING("packaging");

        private final String serializedName;

        Step(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
