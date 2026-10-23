/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.material.ChemicalFormula;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MineralFamily;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How the series declares a mineral: what it is mined for, its family and the rocks its veins cross, and what it is
 * made of. A mineral with a formula takes the share of each element from it, by the elements' atomic weights, so
 * chalcopyrite, CuFeS2, holds 35% copper and 35% sulfur without anyone writing either number; a natural alloy with no
 * one formula, electrum, says its parts. The byproducts it gives are its own elements, at their shares, and the trace
 * elements real ones of it hold, each at its real grade counted as the series counts traces ({@link TraceGrade}); the
 * traces take their room from the rest, as impurities in a real mineral do.
 *
 * <pre>{@code
 * MineralBuilder.of(rl("chalcopyrite"), CHALCOPYRITE, MineralFamilies.SULFIDE, Element.COPPER).formula("CuFeS2")
 *         .byproduct(Element.SULFUR).trace(Element.GOLD, 0.000_01).rocks(HostRocks.DEEPSLATE).declare();
 * }</pre>
 */
public final class MineralBuilder {

    private final ResourceLocation id;
    private final TextKey name;
    private final MineralFamily family;
    private final Element main;
    private final Map<Element, Double> parts = new EnumMap<>(Element.class);
    private final List<Element> byproducts = new ArrayList<>();
    private final Map<Element, Double> traces = new LinkedHashMap<>();
    private final Set<HostRock> rocks = new LinkedHashSet<>();
    private @Nullable String formula;
    private int colour = -1;
    private OreLook look = OreLook.PLAIN;

    private MineralBuilder(final ResourceLocation id, final TextKey name, final MineralFamily family,
                           final Element main) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.family = Objects.requireNonNull(family, "family");
        this.main = Objects.requireNonNull(main, "main");
    }

    /** A mineral called {@code id}, named {@code name}, of {@code family}, mined for {@code main}. */
    public static MineralBuilder of(final ResourceLocation id, final TextKey name, final MineralFamily family,
                                    final Element main) {
        return new MineralBuilder(id, name, family, main);
    }

    /** Its formula, which says what it is made of: {@code CuFeS2}. */
    public MineralBuilder formula(final String written) {
        ChemicalFormula.parse(written);
        this.formula = written;
        return this;
    }

    /** A part of it, for a mineral with no one formula: {@code element}, at {@code share} of its mass. */
    public MineralBuilder part(final Element element, final double share) {
        this.parts.put(Objects.requireNonNull(element, "element"), share);
        return this;
    }

    /** One of its own elements that its processing gives besides the main one, at its share of it. */
    public MineralBuilder byproduct(final Element element) {
        this.byproducts.add(Objects.requireNonNull(element, "element"));
        return this;
    }

    /** A trace element real ones of it hold, at {@code realGrade} of its mass, counted as the series counts traces. */
    public MineralBuilder trace(final Element element, final double realGrade) {
        this.traces.put(Objects.requireNonNull(element, "element"), TraceGrade.share(realGrade));
        return this;
    }

    /** Its colour, as {@code 0xRRGGBB}. */
    public MineralBuilder colour(final int rgb) {
        this.colour = rgb;
        return this;
    }

    /** How its ores look. */
    public MineralBuilder ore(final OreLook how) {
        this.look = Objects.requireNonNull(how, "how");
        return this;
    }

    /** The rocks its veins can cross, an ore of it in each. */
    public MineralBuilder rocks(final HostRock... veinsCross) {
        this.rocks.addAll(List.of(veinsCross));
        return this;
    }

    /**
     * Declares the mineral.
     *
     * @throws DeclarationError when it says no formula and no parts, its main element or a byproduct is none of its
     *                          elements, or its traces leave no room for the rest
     */
    public Material declare() {
        double traced = 0.0;
        for (final double share : this.traces.values()) {
            traced += share;
        }
        if (traced >= 1.0) {
            throw DeclarationError.of(this.id.getNamespace(), "the mineral " + this.id,
                    "has traces that leave no room for what it is made of", "give fewer traces");
        }
        final double room = 1.0 - traced;
        final Material.Builder builder = Materials.material(this.id, MaterialKind.MINERAL, this.name).ore(this.look)
                .mineral(this.family, this.rocks.toArray(HostRock[]::new))
                .contains(Elements.of(this.main), share(this.main) * room);
        for (final Element byproduct : this.byproducts) {
            builder.contains(Elements.of(byproduct), share(byproduct) * room);
        }
        this.traces.forEach((element, share) -> builder.contains(Elements.of(element), share));
        if (this.formula != null) {
            builder.with(MaterialProperties.FORMULA, this.formula);
        }
        if (this.colour >= 0) {
            builder.colour(this.colour);
        }
        return builder.declare();
    }

    /* The share of the mineral's mass {@code element} makes up, by its formula or its parts. */
    private double share(final Element element) {
        final double share;
        if (this.formula != null) {
            share = ChemicalFormula.parse(this.formula).massFraction(element.symbol(), Elements::atomicWeight);
        } else if (!this.parts.isEmpty()) {
            share = this.parts.getOrDefault(element, 0.0);
        } else {
            throw DeclarationError.of(this.id.getNamespace(), "the mineral " + this.id,
                    "says neither its formula nor its parts", "give it formula(...) or part(...)");
        }
        if (!(share > 0.0)) {
            throw DeclarationError.of(this.id.getNamespace(), "the mineral " + this.id,
                    "gives " + element.symbol() + ", which it is not made of",
                    "give only the elements of its formula or its parts; a trace with trace(...)");
        }
        return share;
    }
}
