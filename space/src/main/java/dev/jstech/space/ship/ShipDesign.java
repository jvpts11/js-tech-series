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
import dev.jstech.space.api.ShipPart;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * A starship's design: its name, the sections of its spine from the nose to the tail, and the parts hung on it, each by
 * its id among the ship parts.
 *
 * @param name  its name
 * @param spine its spine's sections, the cockpit first
 * @param hung  the parts hung on it
 */
public record ShipDesign(String name, List<ResourceLocation> spine, List<ResourceLocation> hung) {

    public static final Codec<ShipDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipDesign::name),
            ResourceLocation.CODEC.listOf().fieldOf("spine").forGetter(ShipDesign::spine),
            ResourceLocation.CODEC.listOf().fieldOf("hung").forGetter(ShipDesign::hung)
    ).apply(instance, ShipDesign::new));

    public ShipDesign {
        Objects.requireNonNull(name, "name");
        spine = List.copyOf(spine);
        hung = List.copyOf(hung);
    }

    /** What keeps it down, its parts read from {@code parts}: a part of an id none of them has is unknown. */
    public Set<ShipChecks.Problem> problems(final Map<ResourceLocation, ShipPart> parts) {
        final Optional<List<ShipChecks.Piece>> spinePieces = pieces(this.spine, parts);
        final Optional<List<ShipChecks.Piece>> hungPieces = pieces(this.hung, parts);
        if (spinePieces.isEmpty() || hungPieces.isEmpty()) {
            return EnumSet.of(ShipChecks.Problem.UNKNOWN_PART);
        }
        return ShipChecks.problems(spinePieces.get(), hungPieces.get());
    }

    /* The parts of {@code ids} as the check sees them; none if any is unknown. */
    private static Optional<List<ShipChecks.Piece>> pieces(final List<ResourceLocation> ids,
                                                           final Map<ResourceLocation, ShipPart> parts) {
        final List<ShipChecks.Piece> out = new ArrayList<>();
        for (final ResourceLocation id : ids) {
            final ShipPart part = parts.get(id);
            if (part == null) {
                return Optional.empty();
            }
            out.add(new ShipChecks.Piece(part.kind(), part.place(), part.shipClass(), part.room(), part.airlock(),
                    part.carries()));
        }
        return Optional.of(out);
    }
}
