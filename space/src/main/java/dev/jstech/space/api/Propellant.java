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
import dev.jstech.core.material.MatterState;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * What a rocket burns: the materials of J's Core's catalogue it is made of, each in the state it is carried in and its
 * share of the mass, how far it pushes a rocket for its mass, and whether its two halves light on touching. How long it
 * keeps in a tank follows from its materials: a liquid kept below where it boils at the place, as liquid hydrogen,
 * oxygen and methane are, boils off as the heat leaks in, and a liquid that keeps at the place's warmth, as hydrazine,
 * loses nothing. A propellant all solid, once lit, cannot be stopped.
 *
 * <pre>{@code
 * {"name": "jsspace.propellant.methalox", "vacuum_isp": 380, "components": [
 *     {"material": "jscore:methane", "state": "liquid", "share": 0.217},
 *     {"material": "jscore:oxygen", "state": "liquid", "share": 0.783}]}
 * }</pre>
 *
 * @param name       the key of its name in the language files
 * @param components what it is made of, the shares summing to one
 * @param vacuumIsp  the specific impulse a good engine gets of it in a vacuum, in seconds; each engine says its own
 * @param hypergolic whether its halves light on touching each other, with no igniter
 */
public record Propellant(String name, List<Component> components, double vacuumIsp, boolean hypergolic) {

    /* How far the shares may sum from one, for the rounding of a file's numbers. */
    private static final double SHARES_WITHIN = 0.005;
    /** As a data pack's file writes one. */
    public static final Codec<Propellant> CODEC = RecordCodecBuilder.<Propellant>create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Propellant::name),
            Component.CODEC.listOf(1, 8).fieldOf("components").forGetter(Propellant::components),
            Codec.doubleRange(1, 1e8).fieldOf("vacuum_isp").forGetter(Propellant::vacuumIsp),
            Codec.BOOL.optionalFieldOf("hypergolic", false).forGetter(Propellant::hypergolic)
    ).apply(instance, Propellant::new)).validate(propellant -> Math.abs(propellant.components().stream()
            .mapToDouble(Component::share).sum() - 1) <= SHARES_WITHIN ? DataResult.success(propellant)
            : DataResult.error(() -> "a propellant's shares sum to one"));

    public Propellant {
        Objects.requireNonNull(name, "name");
        components = List.copyOf(components);
    }

    /** Whether it is all solid, a grain that burns once lit until it is spent and cannot be stopped. */
    public boolean solid() {
        return this.components.stream().allMatch(component -> component.state() == MatterState.SOLID);
    }

    /** The share of the mass {@code material} is of it; none when it is not one of its materials. */
    public double share(final ResourceLocation material) {
        return this.components.stream().filter(component -> component.material().equals(material))
                .mapToDouble(Component::share).sum();
    }

    /**
     * One of what a propellant is made of: a material of J's Core's catalogue, the state it is carried in, and its
     * share of the propellant's mass, as an engine burns it.
     *
     * @param material the material's id
     * @param state    the state it is carried in
     * @param share    its share of the mass, from more than nothing to all of it
     */
    public record Component(ResourceLocation material, MatterState state, double share) {

        private static final Codec<MatterState> STATE = Codec.STRING.comapFlatMap(name -> MatterState.byName(name)
                .map(DataResult::success).orElseGet(() -> DataResult.error(() -> "no state of matter is called "
                        + name)), MatterState::serializedName);
        public static final Codec<Component> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Component::material),
                STATE.fieldOf("state").forGetter(Component::state),
                Codec.doubleRange(1e-6, 1).fieldOf("share").forGetter(Component::share)
        ).apply(instance, Component::new));

        public Component {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(state, "state");
        }
    }
}
