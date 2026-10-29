/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The kit of blocks a rocket part is built of, and where each goes: its layers from the bottom, each a square of rows
 * from north to south and letters from west to east, a letter for each kind of block of the kit and a space for a place
 * left empty, as a real part is hollow where it is. A part built in layers, a tank, repeats one of its layers as tall
 * as it is built; it may have a layer of its own for when it is only one layer tall, and a foot it stands on when it
 * sits on an engine, as a first stage's tank stands on its skirt.
 *
 * <pre>{@code
 * {"layers": [["TYF", "TOT", "TTT"], ["TYT", "T T", "TTT"], ["VYT", "TOT", "TTT"]], "stretch": 1,
 *  "alone": ["VYF", "TOT", "TTT"], "foot": ["KKK", "K K", "KKK"],
 *  "key": {"T": "jsspace:tank_wall", "O": "jsspace:tank_dome", "F": "jsspace:fill_port", "V": "jsspace:vent_valve",
 *          "Y": "jsspace:cable_raceway", "K": "jsspace:tank_skirt"}}
 * }</pre>
 *
 * @param layers  its layers from the bottom, each its rows from north to south
 * @param key     the block each letter stands for
 * @param stretch the layer repeated as the part is built taller, for a part built in layers
 * @param alone   the one layer it is when built a single layer tall
 * @param foot    the layer it stands on when it sits on an engine
 */
public record RocketKit(List<List<String>> layers, Map<String, ResourceLocation> key, Optional<Integer> stretch,
                        Optional<List<String>> alone, Optional<List<String>> foot) {

    private static final Codec<List<String>> LAYER = Codec.STRING.listOf(1, 16);
    private static final Codec<String> LETTER = Codec.STRING.validate(letter -> letter.length() == 1
            && !" ".equals(letter) ? DataResult.success(letter) : DataResult.error(() -> "a kit's key is one letter"));
    /** As a data pack's file writes one. */
    public static final Codec<RocketKit> CODEC = RecordCodecBuilder.<RocketKit>create(instance -> instance.group(
            LAYER.listOf(1, 64).fieldOf("layers").forGetter(RocketKit::layers),
            Codec.unboundedMap(LETTER, ResourceLocation.CODEC).fieldOf("key").forGetter(RocketKit::key),
            Codec.intRange(0, 63).optionalFieldOf("stretch").forGetter(RocketKit::stretch),
            LAYER.optionalFieldOf("alone").forGetter(RocketKit::alone),
            LAYER.optionalFieldOf("foot").forGetter(RocketKit::foot)
    ).apply(instance, RocketKit::new)).validate(RocketKit::checked);

    public RocketKit {
        layers = List.copyOf(layers.stream().map(List::copyOf).toList());
        key = Map.copyOf(key);
        Objects.requireNonNull(stretch, "stretch");
        alone = alone.map(List::copyOf);
        foot = foot.map(List::copyOf);
    }

    /** A kit of fixed layers, built as it is drawn. */
    public static RocketKit of(final List<List<String>> layers, final Map<String, ResourceLocation> key) {
        return new RocketKit(layers, key, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** How wide it is, in blocks. */
    public int width() {
        return this.layers.getFirst().size();
    }

    /** The fewest layers it may be built of. */
    public int least() {
        if (this.stretch.isEmpty()) {
            return this.layers.size();
        }
        return this.alone.isPresent() ? 1 : this.layers.size() - 1;
    }

    /**
     * Its layers as built {@code count} layers tall, sitting on an engine or not, from the bottom; none for a height it
     * cannot be built at. A part not built in layers is always its own height.
     */
    public Optional<List<List<String>>> built(final int count, final boolean onEngine) {
        if (this.stretch.isEmpty()) {
            return Optional.of(this.layers);
        }
        final List<List<String>> out = new ArrayList<>();
        int body = count;
        if (onEngine && this.foot.isPresent() && count > 1) {
            out.add(this.foot.get());
            body--;
        }
        if (body == 1 && this.alone.isPresent()) {
            out.add(this.alone.get());
            return Optional.of(List.copyOf(out));
        }
        final int fixed = this.layers.size() - 1;
        if (body < fixed) {
            return Optional.empty();
        }
        final int at = this.stretch.get();
        for (int k = 0; k < this.layers.size(); k++) {
            final int times = k == at ? body - fixed : 1;
            for (int t = 0; t < times; t++) {
                out.add(this.layers.get(k));
            }
        }
        return Optional.of(List.copyOf(out));
    }

    /** How many of each block it takes, built {@code count} layers tall, sitting on an engine or not. */
    public Map<ResourceLocation, Integer> counts(final int count, final boolean onEngine) {
        final Map<ResourceLocation, Integer> counted = new LinkedHashMap<>();
        built(count, onEngine).ifPresent(built -> {
            for (final List<String> layer : built) {
                for (final String row : layer) {
                    for (int x = 0; x < row.length(); x++) {
                        final ResourceLocation block = this.key.get(String.valueOf(row.charAt(x)));
                        if (block != null) {
                            counted.merge(block, 1, Integer::sum);
                        }
                    }
                }
            }
        });
        return counted;
    }

    /* Every layer square and as wide as the first, every letter in the key. */
    private static DataResult<RocketKit> checked(final RocketKit kit) {
        final int width = kit.width();
        final List<List<String>> all = new ArrayList<>(kit.layers());
        kit.alone().ifPresent(all::add);
        kit.foot().ifPresent(all::add);
        for (final List<String> layer : all) {
            if (layer.size() != width) {
                return DataResult.error(() -> "a kit's layers are all " + width + " rows");
            }
            for (final String row : layer) {
                if (row.length() != width) {
                    return DataResult.error(() -> "a kit's rows are all " + width + " letters");
                }
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    if (letter != ' ' && !kit.key().containsKey(String.valueOf(letter))) {
                        return DataResult.error(() -> "the letter " + letter + " is in no kit's key");
                    }
                }
            }
        }
        if (kit.stretch().isPresent() && kit.stretch().get() >= kit.layers().size()) {
            return DataResult.error(() -> "a kit stretches one of its own layers");
        }
        return DataResult.success(kit);
    }
}
