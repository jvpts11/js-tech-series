/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * How a Multiblock Builder is set: the page its display shows and the row chosen on it, whether the multiblock it is
 * aimed at shows in the world, its display's light, whether it is on, the block chosen for each place that takes
 * several, and where it takes the blocks from.
 *
 * @param page   the page shown
 * @param row    the row chosen on that page, from 0
 * @param show   whether the multiblock it is aimed at shows its ghosts in the world while it is held
 * @param light  whether the display is lit
 * @param on     whether it is on; off, it shows nothing and builds nothing
 * @param chosen the block chosen for the places that take several, keyed by the blocks they take
 * @param source where it takes the blocks from
 */
public record BuilderState(Page page, int row, boolean show, boolean light, boolean on, Map<String, String> chosen,
                           ResourceLocation source) {

    /** How a builder comes: on, on its page to build, taking from the player's inventory. */
    public static final BuilderState DEFAULT = new BuilderState(Page.BUILD, 0, false, false, true, Map.of(),
            BuildSources.INVENTORY);

    public static final Codec<BuilderState> CODEC = RecordCodecBuilder.create(state -> state.group(
            StableCodecs.byName(Page.class).fieldOf("page").forGetter(BuilderState::page),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("row").forGetter(BuilderState::row),
            Codec.BOOL.fieldOf("show").forGetter(BuilderState::show),
            Codec.BOOL.fieldOf("light").forGetter(BuilderState::light),
            Codec.BOOL.fieldOf("on").forGetter(BuilderState::on),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("chosen", Map.of())
                    .forGetter(BuilderState::chosen),
            ResourceLocation.CODEC.optionalFieldOf("source", BuildSources.INVENTORY).forGetter(BuilderState::source)
    ).apply(state, BuilderState::new));

    public static final StreamCodec<ByteBuf, BuilderState> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public BuilderState {
        row = Math.max(0, row);
        chosen = Map.copyOf(chosen);
    }

    /** The same, on {@code to}, its first row chosen. */
    public BuilderState onPage(final Page to) {
        return new BuilderState(to, 0, this.show, this.light, this.on, this.chosen, this.source);
    }

    /** The same, the row {@code by} rows along, never above the first or below the {@code rows}th. */
    public BuilderState rowMoved(final int by, final int rows) {
        return new BuilderState(this.page, Math.max(0, Math.min(Math.max(0, rows - 1), this.row + by)), this.show,
                this.light, this.on, this.chosen, this.source);
    }

    /** The same, its SHOW key pressed. */
    public BuilderState toggleShow() {
        return new BuilderState(this.page, this.row, !this.show, this.light, this.on, this.chosen, this.source);
    }

    /** The same, its LIGHT key pressed. */
    public BuilderState toggleLight() {
        return new BuilderState(this.page, this.row, this.show, !this.light, this.on, this.chosen, this.source);
    }

    /** The same, its POWER key pressed. */
    public BuilderState togglePower() {
        return new BuilderState(this.page, this.row, this.show, this.light, !this.on, this.chosen, this.source);
    }

    /** The same, {@code block} chosen for the places that take the blocks {@code key} names. */
    public BuilderState choosing(final String key, final String block) {
        final Map<String, String> to = new HashMap<>(this.chosen);
        to.put(key, block);
        return new BuilderState(this.page, this.row, this.show, this.light, this.on, to, this.source);
    }

    /** The same, taking the blocks from {@code to}. */
    public BuilderState from(final ResourceLocation to) {
        return new BuilderState(this.page, this.row, this.show, this.light, this.on, this.chosen, to);
    }

    /** A page of the display. */
    public enum Page implements IStableName {
        /** What it will place, what the source has, and whether it is ready. */
        BUILD("build"),
        /** The block chosen for each place that takes several. */
        CHOICES("choices"),
        /** Where it takes the blocks from. */
        SOURCE("source");

        private final String serializedName;

        Page(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The pages in the order of the soft keys under the display; the last soft key puts it away. */
        public static List<Page> order() {
            return List.of(BUILD, CHOICES, SOURCE);
        }
    }
}
