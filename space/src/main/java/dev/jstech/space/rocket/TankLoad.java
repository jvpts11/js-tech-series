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
import dev.jstech.space.api.Propellant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * What a tank of a rocket holds: the propellant it was filled with and the kilograms of each of its materials left. The
 * materials keep apart, as a real tank's fuel and oxidiser do, and boil off each at its own pace, so a tank long left
 * may hold more of one than its engine burns with the other: an engine burns them in its propellant's shares, and
 * what is left over of one is carried and never burnt.
 *
 * @param propellant the propellant's id
 * @param kilograms  the kilograms of each material, by its id
 */
public record TankLoad(ResourceLocation propellant, Map<ResourceLocation, Double> kilograms) {

    public static final Codec<TankLoad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("propellant").forGetter(TankLoad::propellant),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).fieldOf("kilograms")
                    .forGetter(TankLoad::kilograms)
    ).apply(instance, TankLoad::new));

    public TankLoad {
        Objects.requireNonNull(propellant, "propellant");
        kilograms = Map.copyOf(kilograms);
    }

    /** A tank filled with {@code kilograms} of {@code propellant}, each material by its share. */
    public static TankLoad filled(final ResourceLocation id, final Propellant propellant, final double kilograms) {
        final Map<ResourceLocation, Double> each = new LinkedHashMap<>();
        for (final Propellant.Component component : propellant.components()) {
            each.merge(component.material(), Math.max(0, kilograms) * component.share(), Double::sum);
        }
        return new TankLoad(id, each);
    }

    /** All it holds, in kilograms. */
    public double total() {
        double sum = 0;
        for (final double held : this.kilograms.values()) {
            sum += held;
        }
        return sum;
    }

    /**
     * How many kilograms of {@code burnt} an engine can burn of it: as much as its scarcest material allows in the
     * propellant's shares; none when it holds another propellant, as an engine does not light on the wrong one.
     */
    public double usable(final ResourceLocation id, final Propellant burnt) {
        if (!this.propellant.equals(id)) {
            return 0;
        }
        double most = Double.POSITIVE_INFINITY;
        for (final Propellant.Component component : burnt.components()) {
            most = Math.min(most, this.kilograms.getOrDefault(component.material(), 0.0) / component.share());
        }
        return Double.isFinite(most) ? most : 0;
    }

    /** The same tank, holding {@code share} of all it holds, each material alike, as a holed tank leaks. */
    public TankLoad scaled(final double share) {
        final Map<ResourceLocation, Double> each = new LinkedHashMap<>();
        this.kilograms.forEach((material, held) -> each.put(material, held * Math.clamp(share, 0, 1)));
        return new TankLoad(this.propellant, each);
    }

    /** The same tank, holding {@code kilograms} of {@code material}. */
    public TankLoad with(final ResourceLocation material, final double kilograms) {
        final Map<ResourceLocation, Double> each = new LinkedHashMap<>(this.kilograms);
        each.put(material, Math.max(0, kilograms));
        return new TankLoad(this.propellant, each);
    }
}
