/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.spacecraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.space.api.SpacecraftSize;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * A satellite or a probe as it is assembled from its components: its size; its bus; its power, solar panels or, far
 * from the Sun, an RTG; its battery; its antenna; its propulsion, cold gas, hydrazine or ion; its onboard computer; and
 * its mission module, which sets what kind of satellite or probe it is. It is whole once it has one of each, and only
 * then can it go up.
 *
 * @param size       how big it is built
 * @param components the components fitted
 * @param mission    the mission module fitted, if one is
 */
public record SpacecraftBuild(SpacecraftSize size, Set<Component> components, Optional<Mission> mission) {

    public static final Codec<SpacecraftBuild> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpacecraftSize.CODEC.fieldOf("size").forGetter(SpacecraftBuild::size),
            Component.CODEC.listOf().<Set<Component>>xmap(SpacecraftBuild::set, List::copyOf)
                    .optionalFieldOf("components", Set.of()).forGetter(SpacecraftBuild::components),
            Mission.CODEC.optionalFieldOf("mission").forGetter(SpacecraftBuild::mission)
    ).apply(instance, SpacecraftBuild::new));

    public SpacecraftBuild {
        components = set(components);
    }

    /** Nothing fitted yet, to be built {@code size}. */
    public static SpacecraftBuild of(final SpacecraftSize size) {
        return new SpacecraftBuild(size, Set.of(), Optional.empty());
    }

    /** The same, {@code component} fitted too, in place of any other of its kind. */
    public SpacecraftBuild with(final Component component) {
        final Set<Component> fitted = EnumSet.noneOf(Component.class);
        fitted.addAll(this.components);
        fitted.removeIf(other -> other.slot() == component.slot());
        fitted.add(component);
        return new SpacecraftBuild(this.size, fitted, this.mission);
    }

    /** The same, its mission module {@code mission}, which sets what kind it is. */
    public SpacecraftBuild with(final Mission fitted) {
        return new SpacecraftBuild(this.size, this.components, Optional.of(fitted));
    }

    /** The kinds of component it still lacks; the mission module's lack is told by {@link #mission()}. */
    public List<Slot> missing() {
        final List<Slot> out = new ArrayList<>();
        for (final Slot slot : Slot.values()) {
            if (this.components.stream().noneMatch(component -> component.slot() == slot)) {
                out.add(slot);
            }
        }
        return List.copyOf(out);
    }

    /** Whether it has one of each component and its mission module, ready to go up. */
    public boolean whole() {
        return missing().isEmpty() && this.mission.isPresent();
    }

    private static Set<Component> set(final Collection<Component> components) {
        final Set<Component> out = EnumSet.noneOf(Component.class);
        out.addAll(components);
        return Collections.unmodifiableSet(out);
    }

    /** The places a component goes in: one component each. */
    public enum Slot {
        BUS, POWER, BATTERY, ANTENNA, PROPULSION, COMPUTER
    }

    /** A component, and the place it goes in. */
    public enum Component implements IStableName {

        BUS("bus", Slot.BUS),
        SOLAR_PANELS("solar_panels", Slot.POWER),
        RTG("rtg", Slot.POWER),
        BATTERY("battery", Slot.BATTERY),
        ANTENNA("antenna", Slot.ANTENNA),
        COLD_GAS("cold_gas", Slot.PROPULSION),
        HYDRAZINE("hydrazine", Slot.PROPULSION),
        ION("ion", Slot.PROPULSION),
        COMPUTER("computer", Slot.COMPUTER);

        public static final Codec<Component> CODEC = StableCodecs.byName(Component.class);

        private final String serializedName;
        private final Slot slot;

        Component(final String serializedName, final Slot slot) {
            this.serializedName = serializedName;
            this.slot = slot;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The place it goes in. */
        public Slot slot() {
            return this.slot;
        }
    }

    /**
     * A mission module: whether it makes a satellite or a probe, and the kind, by its id among the kinds of satellite
     * ({@code SatelliteTypes}) or of probe ({@code ProbeTypes}).
     *
     * @param probe whether it makes a probe
     * @param type  the kind it makes
     */
    public record Mission(boolean probe, ResourceLocation type) {

        public static final Codec<Mission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("probe", false).forGetter(Mission::probe),
                ResourceLocation.CODEC.fieldOf("type").forGetter(Mission::type)
        ).apply(instance, Mission::new));
    }
}
