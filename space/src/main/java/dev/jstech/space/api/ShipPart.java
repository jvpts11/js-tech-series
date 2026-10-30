/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A part of a starship, as a data pack's file or a declaration says it: a multiblock of its own, of one class, going in
 * one place on the ship; a fuselage section's length along the spine; how much of the hull's room a module takes;
 * whether it holds an airlock, or, for wings, whether they carry nacelles; and its maker, which gives it its shape and
 * its interior.
 *
 * @param name      its name's key
 * @param kind      what it is
 * @param shipClass the class of ship it is for
 * @param place     where it goes
 * @param length    how long it is along the spine, in blocks; nought for a part that hangs on it
 * @param room      how much of the hull's room it takes: one for a small module, three for a medium one
 * @param airlock   whether it holds an airlock
 * @param carries   for wings, whether they carry nacelles
 * @param maker     its maker's id among the ship makers, if it has one
 */
public record ShipPart(String name, ShipPartKind kind, ShipClass shipClass, ShipPlace place, double length, int room,
                       boolean airlock, boolean carries, Optional<ResourceLocation> maker) {

    /** As a data pack's file writes one. */
    public static final Codec<ShipPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipPart::name),
            StableCodecs.byName(ShipPartKind.class).fieldOf("kind").forGetter(ShipPart::kind),
            StableCodecs.byName(ShipClass.class).fieldOf("class").forGetter(ShipPart::shipClass),
            StableCodecs.byName(ShipPlace.class).fieldOf("place").forGetter(ShipPart::place),
            Codec.doubleRange(0, 64).optionalFieldOf("length", 0.0).forGetter(ShipPart::length),
            Codec.intRange(0, 64).optionalFieldOf("room", 0).forGetter(ShipPart::room),
            Codec.BOOL.optionalFieldOf("airlock", false).forGetter(ShipPart::airlock),
            Codec.BOOL.optionalFieldOf("carries", false).forGetter(ShipPart::carries),
            ResourceLocation.CODEC.optionalFieldOf("maker").forGetter(ShipPart::maker)
    ).apply(instance, ShipPart::new));

    public ShipPart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(shipClass, "shipClass");
        Objects.requireNonNull(place, "place");
        Objects.requireNonNull(maker, "maker");
    }
}
