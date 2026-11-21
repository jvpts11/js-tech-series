/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A place of a world a ship may come down onto, as its console's destination page offers it: the dock it last left,
 * a landing pad its pilot may use, numbered in the order the page lists them, its pilot's bed, or where its pilot
 * last took off from there.
 *
 * @param kind      what the place is
 * @param number    which of its kind it is, from one, for a pad; nought for the others
 * @param dimension the world it is in
 * @param at        where it stands
 */
public record ShipDestination(Kind kind, int number, ResourceKey<Level> dimension, BlockPos at) {

    public static final Codec<ShipDestination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Kind.class).fieldOf("kind").forGetter(ShipDestination::kind),
            Codec.INT.optionalFieldOf("number", 0).forGetter(ShipDestination::number),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ShipDestination::dimension),
            BlockPos.CODEC.fieldOf("at").forGetter(ShipDestination::at)
    ).apply(instance, ShipDestination::new));
    public static final StreamCodec<ByteBuf, ShipDestination> STREAM_CODEC = StreamCodec.composite(
            StableCodecs.byId(Kind.class, Kind.DOCK), ShipDestination::kind,
            ByteBufCodecs.VAR_INT, ShipDestination::number,
            ResourceKey.streamCodec(Registries.DIMENSION), ShipDestination::dimension,
            BlockPos.STREAM_CODEC, ShipDestination::at,
            ShipDestination::new);

    public ShipDestination {
        at = at.immutable();
    }

    /** What a place a ship may come down onto is, its column's word and its name on the console's page. */
    @TextHolder
    public enum Kind implements IStableId, IStableName {
        /** The dock the ship last left. */
        DOCK(0, "dock", TextKey.of("jsspace.ship_dest.kind.dock", "DOCK"),
                TextKey.of("jsspace.ship_dest.name.dock", "SMALL SHIPYARD")),
        /** A landing pad its pilot may use. */
        PAD(1, "pad", TextKey.of("jsspace.ship_dest.kind.pad", "PAD"),
                TextKey.of("jsspace.ship_dest.name.pad", "SHIP PAD %s")),
        /** Its pilot's bed. */
        BED(2, "bed", TextKey.of("jsspace.ship_dest.kind.bed", "BED"),
                TextKey.of("jsspace.ship_dest.name.bed", "YOUR BED")),
        /** Where its pilot last took off from in that world. */
        LIFT_OFF(3, "lift_off", TextKey.of("jsspace.ship_dest.kind.lift_off", "LIFT"),
                TextKey.of("jsspace.ship_dest.name.lift_off", "LAST LIFT-OFF"));

        private final int id;
        private final String serializedName;
        private final TextKey word;
        private final TextKey title;

        Kind(final int id, final String serializedName, final TextKey word, final TextKey title) {
            this.id = id;
            this.serializedName = serializedName;
            this.word = word;
            this.title = title;
        }

        @Override
        public int id() {
            return this.id;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** Its column's word on the page. */
        public TextKey word() {
            return this.word;
        }

        /** What the page calls a place of its kind, a pad with its number. */
        public TextKey title() {
            return this.title;
        }
    }
}
