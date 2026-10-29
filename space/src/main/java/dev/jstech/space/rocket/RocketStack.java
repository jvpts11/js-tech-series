/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A rocket's parts as they stand, from the bottom up: each a part of the registry, as many layers tall as it was built
 * where it is built in layers, with the boosters hung at its side, each booster its own parts from the bottom up.
 *
 * @param sections the parts from the bottom up
 */
public record RocketStack(List<Section> sections) {

    public static final Codec<RocketStack> CODEC = Section.CODEC.listOf().xmap(RocketStack::new,
            RocketStack::sections);

    public RocketStack {
        sections = List.copyOf(sections);
    }

    /** A rocket of {@code parts}, from the bottom up, with no boosters. */
    public static RocketStack of(final Part... parts) {
        return new RocketStack(Arrays.stream(parts).map(part -> new Section(part, List.of())).toList());
    }

    /**
     * A part, as tall as it was built.
     *
     * @param id     the part's id in the registry
     * @param layers how many layers tall it was built, for a part built in layers; 1 otherwise
     */
    public record Part(ResourceLocation id, int layers) {

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("part").forGetter(Part::id),
                Codec.intRange(1, 256).optionalFieldOf("layers", 1).forGetter(Part::layers)
        ).apply(instance, Part::new));

        public Part {
            Objects.requireNonNull(id, "id");
        }
    }

    /**
     * A part of the rocket's core and the boosters hung at its side.
     *
     * @param part     the part
     * @param boosters the boosters at its side, each its parts from the bottom up
     */
    public record Section(Part part, List<List<Part>> boosters) {

        public static final Codec<Section> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Part.CODEC.fieldOf("core").forGetter(Section::part),
                Part.CODEC.listOf().listOf().optionalFieldOf("boosters", List.of()).forGetter(Section::boosters)
        ).apply(instance, Section::new));

        public Section {
            Objects.requireNonNull(part, "part");
            boosters = List.copyOf(boosters.stream().map(List::copyOf).toList());
        }
    }
}
