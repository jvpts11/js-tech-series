/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Milestone;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A discovery: a piece of research a team makes, known in part while its research goes, its share the progress made,
 * and whole once it is done, when it opens what asks for it. It costs research and data ({@link ResearchCost}), and it
 * starts only once the team knows what it comes after: other discoveries, other knowledge, milestones.
 *
 * <pre>{@code
 * public static final Discovery THE_LASER = Discovery.builder(rl("laser"), MyTexts.LASER)
 *         .when(MyTexts.LASER_WHEN).year(1960).field(MyTexts.LIGHT)
 *         .costs(24_000).needs(MyResearch.SPECTRAL, 8)
 *         .after(STIMULATED_EMISSION)
 *         .declare();
 * }</pre>
 *
 * A recipe asks for it by its id, as for any knowledge (the {@code KnowledgeGate}).
 */
public final class Discovery {

    private final ResourceLocation id;
    private final TextKey name;
    private final @Nullable TextKey when;
    private final OptionalInt year;
    private final @Nullable TextKey field;
    private final List<ResourceLocation> after;
    private final Knowledge knowledge;

    private Discovery(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.when = builder.when;
        this.year = builder.year;
        this.field = builder.field;
        this.after = List.copyOf(builder.after);
        this.knowledge = new Knowledge(builder.id, builder.name, true);
    }

    /** Starts declaring the discovery {@code id}, named {@code name}. */
    public static Builder builder(final ResourceLocation id, final TextKey name) {
        return new Builder(id, name);
    }

    /** Its id, which its knowledge, its cost's file and the recipes that ask for it share. */
    public ResourceLocation id() {
        return this.id;
    }

    /** Its name as a player reads it. */
    public TextKey name() {
        return this.name;
    }

    /** When and by whom it was made, as a player reads it: "1960, Maiman"; none when it has not been made. */
    public @Nullable TextKey when() {
        return this.when;
    }

    /** The year it was made in the real world, which the tree is laid out by; none when it has not been made yet. */
    public OptionalInt year() {
        return this.year;
    }

    /** The field of science it belongs to, which the tree groups it by; none for one in no field. */
    public @Nullable TextKey field() {
        return this.field;
    }

    /** The knowledge it comes after, by id: discoveries, other knowledge, milestones. */
    public List<ResourceLocation> after() {
        return this.after;
    }

    /** The knowledge it is, held by a team in part while it is researched. */
    public Knowledge knowledge() {
        return this.knowledge;
    }

    @Override
    public String toString() {
        return "Discovery[" + this.id + "]";
    }

    /** Declares a discovery: what it is called, when it was made, what it costs and what it comes after. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey name;
        private final List<ResourceLocation> after = new ArrayList<>();
        private @Nullable TextKey when;
        private OptionalInt year = OptionalInt.empty();
        private @Nullable TextKey field;
        private @Nullable ResearchCost cost;

        private Builder(final ResourceLocation id, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** When and by whom it was made: "1938, Hahn and Meitner". */
        public Builder when(final TextKey text) {
            this.when = Objects.requireNonNull(text, "text");
            return this;
        }

        /** The year it was made in the real world. */
        public Builder year(final int made) {
            this.year = OptionalInt.of(made);
            return this;
        }

        /** The field of science it belongs to, its name as a player reads it. */
        public Builder field(final TextKey name) {
            this.field = Objects.requireNonNull(name, "name");
            return this;
        }

        /** Costs {@code points} of research, as it is while no data pack says otherwise. */
        public Builder costs(final long points) {
            this.cost = this.cost == null ? ResearchCost.of(points) : new ResearchCost(points, this.cost.data());
            return this;
        }

        /** Asks for {@code count} data of {@code kind}, each covering an equal share of the way. */
        public Builder needs(final DataKind kind, final int count) {
            if (this.cost == null) {
                throw new IllegalStateException("say what " + this.id + " costs before the data it needs");
            }
            this.cost = this.cost.with(kind, count);
            return this;
        }

        /** Comes after {@code discovery}: it starts only once the team has made it. */
        public Builder after(final Discovery discovery) {
            this.after.add(discovery.id());
            return this;
        }

        /** Comes after {@code knowledge}, as an element known. */
        public Builder after(final Knowledge knowledge) {
            this.after.add(knowledge.id());
            return this;
        }

        /** Comes after {@code milestone}, once the team has passed it. */
        public Builder after(final Milestone milestone) {
            this.after.add(milestone.knowledge().id());
            return this;
        }

        /**
         * Declares it, from a mod's constructor or the class that holds it, with its knowledge and its cost.
         *
         * @return the discovery, to keep in a constant
         */
        public Discovery declare() {
            if (this.cost == null) {
                throw new IllegalStateException("say what " + this.id + " costs, with costs(points)");
            }
            return Discoveries.declare(new Discovery(this), this.cost);
        }
    }
}
