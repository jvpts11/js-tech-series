/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.OrbitKind;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A thing in a world's orbit, as the world keeps it: what it is, the world it goes round and the orbit it is in, whose
 * it is and whether its owner has let it go, the kind it was built as, the moving structure it is if it is one, how
 * many pieces debris is in, and whether a Space Persistor aboard keeps its place loaded.
 *
 * @param kind      what it is
 * @param world     the world it goes round, by its id among the worlds
 * @param orbit     the orbit it is in
 * @param ownership whose it is, and who may use it
 * @param orphaned  whether its owner has let it go: it works no more, and any team may take it
 * @param type      the kind of satellite or of probe it was built as; none for anything else
 * @param structure the moving structure it is, for a rocket, a ship or a station; none for one carried as data
 * @param pieces    how many pieces debris is in; none for anything else
 * @param kept      whether a Space Persistor aboard keeps its place loaded
 */
public record OrbitObject(Kind kind, ResourceLocation world, OrbitKind orbit, Ownership ownership, boolean orphaned,
                          Optional<ResourceLocation> type, Optional<StructureUuid> structure, int pieces,
                          boolean kept) {

    public static final Codec<OrbitObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(OrbitObject::kind),
            ResourceLocation.CODEC.fieldOf("world").forGetter(OrbitObject::world),
            OrbitKind.CODEC.optionalFieldOf("orbit", OrbitKind.LOW).forGetter(OrbitObject::orbit),
            Ownership.CODEC.fieldOf("ownership").forGetter(OrbitObject::ownership),
            Codec.BOOL.optionalFieldOf("orphaned", false).forGetter(OrbitObject::orphaned),
            ResourceLocation.CODEC.optionalFieldOf("type").forGetter(OrbitObject::type),
            StructureUuid.CODEC.optionalFieldOf("structure").forGetter(OrbitObject::structure),
            Codec.INT.optionalFieldOf("pieces", 0).forGetter(OrbitObject::pieces),
            Codec.BOOL.optionalFieldOf("kept", false).forGetter(OrbitObject::kept)
    ).apply(instance, OrbitObject::new));

    /** A rocket in the low orbit of {@code world}, the moving structure {@code structure}, {@code ownership}'s. */
    public static OrbitObject rocket(final ResourceLocation world, final StructureUuid structure,
                                     final Ownership ownership, final boolean kept) {
        return new OrbitObject(Kind.ROCKET, world, OrbitKind.LOW, ownership, false, Optional.empty(),
                Optional.of(structure), 0, kept);
    }

    /** A satellite of the kind {@code type} in {@code orbit} round {@code world}, {@code ownership}'s. */
    public static OrbitObject satellite(final ResourceLocation world, final OrbitKind orbit,
                                        final ResourceLocation type, final Ownership ownership) {
        return new OrbitObject(Kind.SATELLITE, world, orbit, ownership, false, Optional.of(type), Optional.empty(),
                0, false);
    }

    /** The same, let go by its owner, or taken again. */
    public OrbitObject orphaned(final boolean letGo) {
        return new OrbitObject(this.kind, this.world, this.orbit, this.ownership, letGo, this.type, this.structure,
                this.pieces, this.kept);
    }

    /** The same, {@code next}'s now. */
    public OrbitObject owned(final Ownership next) {
        return new OrbitObject(this.kind, this.world, this.orbit, next, this.orphaned, this.type, this.structure,
                this.pieces, this.kept);
    }

    /** The same broken into {@code count} pieces of debris, in its orbit still, working no more. */
    public OrbitObject broken(final int count) {
        return new OrbitObject(Kind.DEBRIS, this.world, this.orbit, this.ownership, true, this.type, Optional.empty(),
                Math.max(1, count), false);
    }

    /** The same, a Space Persistor aboard keeping its place loaded or not. */
    public OrbitObject kept(final boolean keeping) {
        return new OrbitObject(this.kind, this.world, this.orbit, this.ownership, this.orphaned, this.type,
                this.structure, this.pieces, keeping);
    }

    /** Whether it works: not let go by its owner, and not broken. */
    public boolean working() {
        return !this.orphaned && this.kind != Kind.DEBRIS;
    }

    /** What a thing in orbit is. */
    public enum Kind implements IStableName {

        ROCKET("rocket"),
        SHIP("ship"),
        STATION("station"),
        SATELLITE("satellite"),
        PROBE("probe"),
        DEBRIS("debris");

        public static final Codec<Kind> CODEC = StableCodecs.byName(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** Whether its owner may let it go for any team to take, as a satellite or a probe. */
        public boolean mayBeLetGo() {
            return this == SATELLITE || this == PROBE;
        }
    }
}
