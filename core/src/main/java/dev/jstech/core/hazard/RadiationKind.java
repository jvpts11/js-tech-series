/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of radiation: given off by sources, travelling out from them, weaker with the square of the distance and
 * weakened by every block it crosses, by the block's density; what reaches a body builds up the hazard it doses.
 * Its strengths are in the unit that hazard's rate is shown in, a source's at a block from it.
 *
 * <pre>{@code
 * public static final RadiationKind GLOW = Radiation.declare(new RadiationKind(id("glow"), Texts.GLOW,
 *         MyHazards.GLOW_SICKNESS, 0.0005, 24));
 * GLOW.addAmbient((level, at) -> glowingSky(level, at));
 * }</pre>
 */
public final class RadiationKind {

    private final ResourceLocation id;
    private final TextKey name;
    private final HazardKind dose;
    private final double attenuation;
    private final int reach;
    private final double blockMetres;
    private final List<IAmbientRadiation> ambients = new CopyOnWriteArrayList<>();
    private final List<IBodyRadiation> carried = new CopyOnWriteArrayList<>();

    /**
     * A kind known as {@code id}, called {@code name}, that builds up {@code dose}, weakened in a block by
     * {@code attenuation} for each kilogram a cubic metre the block weighs ({@code e} to the minus their product
     * passes), and counted from sources up to {@code reach} blocks away; a block weakens it as a metre of its material.
     */
    public RadiationKind(final ResourceLocation id, final TextKey name, final HazardKind dose, final double attenuation,
                         final int reach) {
        this(id, name, dose, attenuation, reach, 1);
    }

    /**
     * The same, a block weakening it as {@code blockMetres} of its material does: as a few centimetres, so a block of
     * lead is a lead wall and a container's walls of the same lead as thick shield as much.
     */
    public RadiationKind(final ResourceLocation id, final TextKey name, final HazardKind dose, final double attenuation,
                         final int reach, final double blockMetres) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.dose = Objects.requireNonNull(dose, "dose");
        this.attenuation = attenuation;
        this.reach = reach;
        if (blockMetres <= 0) {
            throw new IllegalArgumentException("a block stands for some of its material, not " + blockMetres + " m");
        }
        this.blockMetres = blockMetres;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What it is called. */
    public TextKey name() {
        return this.name;
    }

    /** The hazard it builds up in a body it reaches. */
    public HazardKind dose() {
        return this.dose;
    }

    /** How far from a source it is counted, in blocks. */
    public int reach() {
        return this.reach;
    }

    /** The share of it that passes through a block weighing {@code density} kilograms a cubic metre. */
    public double transmission(final double density) {
        return Math.exp(-this.attenuation * density);
    }

    /**
     * The share of it that passes through {@code metres} of a material weighing {@code density} kilograms a cubic
     * metre: a container's walls, as thick as they are, measured against what a block of the same stands for.
     */
    public double through(final double density, final double metres) {
        return Math.exp(-this.attenuation * density * metres / this.blockMetres);
    }

    /** How much it is weakened for each kilogram a cubic metre a block it crosses weighs. */
    public double attenuation() {
        return this.attenuation;
    }

    /** Adds radiation of this kind that is everywhere in a place. */
    public void addAmbient(final IAmbientRadiation ambient) {
        this.ambients.add(Objects.requireNonNull(ambient, "ambient"));
    }

    /** Adds radiation of this kind a body carries with it. */
    public void addCarried(final IBodyRadiation radiation) {
        this.carried.add(Objects.requireNonNull(radiation, "radiation"));
    }

    /** What is everywhere in a place. */
    public List<IAmbientRadiation> ambients() {
        return List.copyOf(this.ambients);
    }

    /** What bodies carry. */
    public List<IBodyRadiation> carried() {
        return List.copyOf(this.carried);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
