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
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A starship's design: its name, the sections of its spine from the nose to the tail, and the parts hung on it, each by
 * its id among the ship parts; which sections are flipped left for right, their doors and fittings on the other side;
 * and the finish it is painted in.
 *
 * @param name    its name
 * @param spine   its spine's sections, the cockpit first
 * @param hung    the parts hung on it
 * @param flipped the places along the spine of the sections flipped left for right
 * @param finish  its finish
 */
public record ShipDesign(String name, List<ResourceLocation> spine, List<ResourceLocation> hung,
                         List<Integer> flipped, ResourceLocation finish) {

    /** The longest name a design takes, and the most parts it may have along its spine and hung on it. */
    public static final int LONGEST_NAME = 24;
    public static final int MOST_SECTIONS = 16;
    public static final int MOST_HUNG = 16;
    public static final Codec<ShipDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ShipDesign::name),
            ResourceLocation.CODEC.listOf().fieldOf("spine").forGetter(ShipDesign::spine),
            ResourceLocation.CODEC.listOf().fieldOf("hung").forGetter(ShipDesign::hung),
            Codec.INT.listOf().optionalFieldOf("flipped", List.of()).forGetter(ShipDesign::flipped),
            ResourceLocation.CODEC.optionalFieldOf("finish", SmallShipFinishes.DEFAULT).forGetter(ShipDesign::finish)
    ).apply(instance, ShipDesign::new));
    public static final StreamCodec<ByteBuf, ShipDesign> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public ShipDesign {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(finish, "finish");
        spine = List.copyOf(spine);
        hung = List.copyOf(hung);
        final int sections = spine.size();
        flipped = flipped.stream().filter(place -> place >= 0 && place < sections).distinct().sorted().toList();
    }

    /** A design flipping nothing, painted in the default finish. */
    public ShipDesign(final String name, final List<ResourceLocation> spine, final List<ResourceLocation> hung) {
        this(name, spine, hung, List.of(), SmallShipFinishes.DEFAULT);
    }

    /** Whether the section at {@code place} along the spine is flipped left for right. */
    public boolean flips(final int place) {
        return this.flipped.contains(place);
    }

    /** The same design, its spine {@code sections}, flipping those of {@code flips}. */
    public ShipDesign withSpine(final List<ResourceLocation> sections, final List<Integer> flips) {
        return new ShipDesign(this.name, sections, this.hung, flips, this.finish);
    }

    /** The same design, the parts {@code parts} hung on it. */
    public ShipDesign withHung(final List<ResourceLocation> parts) {
        return new ShipDesign(this.name, this.spine, parts, this.flipped, this.finish);
    }

    /** The same design, painted in {@code painted}. */
    public ShipDesign painted(final ResourceLocation painted) {
        return new ShipDesign(this.name, this.spine, this.hung, this.flipped, painted);
    }

    /** The same design, called {@code called}. */
    public ShipDesign named(final String called) {
        return new ShipDesign(called, this.spine, this.hung, this.flipped, this.finish);
    }

    /** Whether it keeps within the limits a shipyard holds a design to: its name and its parts. */
    public boolean bounded() {
        return this.name.length() <= LONGEST_NAME && this.spine.size() <= MOST_SECTIONS
                && this.hung.size() <= MOST_HUNG;
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
