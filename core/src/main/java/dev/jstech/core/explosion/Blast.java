/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import dev.jstech.core.material.Material;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A blast about to go off: say more, then {@link #detonate()}. It is decided by its energy, with no radius of its own;
 * the medium where it goes off, its casing and whether it sets fires shape the rest.
 *
 * <pre>{@code
 * Explosions.blast(level, centre, MyMod.id("demolition_charge"), 20e6)
 *         .by(player)
 *         .casing(steel, 0.5)
 *         .detonate();
 * }</pre>
 */
public final class Blast {

    private final ServerLevel level;
    private final Vec3 centre;
    private final ResourceLocation explosive;
    private final double joules;
    @Nullable
    private Entity source;
    private Optional<Casing> casing = Optional.empty();
    private boolean incendiary;
    private Optional<BlastMedium> medium = Optional.empty();
    private BlastConsequence most = BlastConsequence.FULL;

    Blast(final ServerLevel level, final Vec3 centre, final ResourceLocation explosive, final double joules) {
        if (!(joules >= 0) || Double.isInfinite(joules)) {
            throw new IllegalArgumentException("a blast of " + joules + " joules");
        }
        this.level = Objects.requireNonNull(level, "level");
        this.centre = Objects.requireNonNull(centre, "centre");
        this.explosive = Objects.requireNonNull(explosive, "explosive");
        this.joules = joules;
    }

    /**
     * It is set off by {@code entity}: the thing that went off (a primed charge, a missile) or whoever set it off; the
     * one behind it is blamed for what it does, and the land it may harm is theirs to harm.
     */
    public Blast by(@Nullable final Entity entity) {
        this.source = entity;
        return this;
    }

    /** It has a casing of {@code kilograms} of {@code material}, flung out as fragments of it. */
    public Blast casing(final Material material, final double kilograms) {
        if (!(kilograms > 0) || Double.isInfinite(kilograms)) {
            throw new IllegalArgumentException("a casing of " + kilograms + " kilograms");
        }
        this.casing = Optional.of(new Casing(material, kilograms));
        return this;
    }

    /** It sets fire to what its fireball reaches, where there is air to burn. */
    public Blast incendiary() {
        this.incendiary = true;
        return this;
    }

    /** It goes out through {@code through}, whatever is where it goes off: a mod's own place with its own air. */
    public Blast through(final BlastMedium through) {
        this.medium = Optional.of(through);
        return this;
    }

    /** It does no more than {@code consequence}, as a mod's own setting may say; the server's setting still holds. */
    public Blast atMost(final BlastConsequence consequence) {
        this.most = consequence;
        return this;
    }

    /** Sets it off; nothing when another mod or a protection stopped it before it began. */
    public Optional<Detonation> detonate() {
        return Explosions.detonate(this);
    }

    ServerLevel level() {
        return this.level;
    }

    Vec3 centre() {
        return this.centre;
    }

    ResourceLocation explosive() {
        return this.explosive;
    }

    double joules() {
        return this.joules;
    }

    @Nullable
    Entity source() {
        return this.source;
    }

    Optional<Casing> casing() {
        return this.casing;
    }

    boolean isIncendiary() {
        return this.incendiary;
    }

    Optional<BlastMedium> medium() {
        return this.medium;
    }

    BlastConsequence most() {
        return this.most;
    }

    /**
     * A blast's casing.
     *
     * @param material  what it is made of, whose density sets how far its fragments fly
     * @param kilograms how much of it there is
     */
    public record Casing(Material material, double kilograms) {

        public Casing {
            Objects.requireNonNull(material, "material");
        }
    }
}
