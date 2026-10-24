/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.material.Material;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * The marks of what a pipe carries, which any mod declares: a fluid has a mark when its tag holds it, and matter of a
 * line of a state (a gas, a slurry) when its material says so. A mark is of one of two kinds. A pipe has to be
 * {@linkplain Kind#MADE_FOR made for} a mark to carry what has it at all, as only a pipe made for corrosives carries
 * chlorine; and a mark may {@linkplain Kind#WEARS wear} the pipes that carry it, as hydrogen embrittles steel and a
 * slurry abrades a pipe, each pipe saying how long it stands it ({@code CableType.Builder.wornBy}).
 *
 * <pre>{@code
 * public static final TagKey<Fluid> CORROSIVE_GASES = PipeMarks.madeFor(FluidTags.create(rl("corrosive_gases")),
 *         EATS_METAL::contains);
 * public static final TagKey<Fluid> HYDROGEN = PipeMarks.wears(FluidTags.create(rl("hydrogen")),
 *         material -> material == hydrogen);
 * }</pre>
 */
public final class PipeMarks {

    private static final Map<TagKey<Fluid>, Mark> MARKS = new LinkedHashMap<>();
    /* What the lines read each tick: the marks as they stand, never copied on reading. */
    private static volatile List<Mark> declared = List.of();

    private PipeMarks() {
    }

    /**
     * Declares {@code tag} a mark a pipe has to be made for, which matter has when {@code materials} says its material
     * does; the tag, to keep.
     *
     * @throws DeclarationError when the tag is a mark already
     */
    public static TagKey<Fluid> madeFor(final TagKey<Fluid> tag, final Predicate<Material> materials) {
        return declare(new Mark(tag, Kind.MADE_FOR, materials));
    }

    /**
     * Declares {@code tag} a mark that wears the pipes declared worn by it, which matter has when {@code materials}
     * says its material does; the tag, to keep.
     *
     * @throws DeclarationError when the tag is a mark already
     */
    public static TagKey<Fluid> wears(final TagKey<Fluid> tag, final Predicate<Material> materials) {
        return declare(new Mark(tag, Kind.WEARS, materials));
    }

    /** Every mark declared, in the order they were. */
    public static List<Mark> all() {
        return declared;
    }

    /** The mark of {@code tag}, or none when it is not a mark. */
    public static Optional<Mark> of(final TagKey<Fluid> tag) {
        synchronized (MARKS) {
            return Optional.ofNullable(MARKS.get(tag));
        }
    }

    private static TagKey<Fluid> declare(final Mark mark) {
        synchronized (MARKS) {
            if (MARKS.containsKey(mark.tag())) {
                throw DeclarationError.of(mark.tag().location().getNamespace(), "the pipe mark "
                        + mark.tag().location(), "is declared twice", "declare each mark once, where its mod keeps its "
                        + "tags");
            }
            MARKS.put(mark.tag(), mark);
            declared = List.copyOf(new ArrayList<>(MARKS.values()));
        }
        return mark.tag();
    }

    /** What a mark asks of a pipe. */
    public enum Kind {
        /** A pipe carries what has it only when it is made for it. */
        MADE_FOR,
        /** Any pipe carries what has it, and one declared worn by it wears through in its time. */
        WEARS
    }

    /**
     * A mark.
     *
     * @param tag       the tag of the fluids that have it
     * @param kind      what it asks of a pipe
     * @param materials which materials' matter has it
     */
    public record Mark(TagKey<Fluid> tag, Kind kind, Predicate<Material> materials) {

        public Mark {
            Objects.requireNonNull(tag, "tag");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(materials, "materials");
        }

        /** Whether {@code fluid} has it. */
        public boolean on(final Fluid fluid) {
            return fluid.defaultFluidState().is(this.tag);
        }

        /** Whether matter of {@code material} has it. */
        public boolean on(final Material material) {
            return this.materials.test(material);
        }
    }
}
