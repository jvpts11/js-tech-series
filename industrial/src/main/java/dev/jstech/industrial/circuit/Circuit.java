/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.circuit;

import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The ten circuits, one for each industrial tier: a tier is the circuit a player can make, as the real history of
 * electronics went, from cams and gears to quantum chips, and then beyond it. The first three are made by hand; from
 * the transistor on, each is made by the machines of its time.
 *
 * <p>A circuit is known by its item tag, {@code jsindustrial:circuits/<name>}, which its items go in as they come with
 * the machines that make them; making any item of it moves the player, and their team, to its tier
 * (industrial/docs/design/tiers-and-circuits.md).
 */
@TextHolder
public enum Circuit {
    MECHANICAL(IndustrialTier.T0, TextKey.of("jsindustrial.circuit.mechanical", "Mechanical"),
            TextKey.of("jsindustrial.circuit.mechanical.what", "Logic of cams and gears, with no electricity."),
            TextKey.of("jsindustrial.circuit.mechanical.made", "By hand.")),
    ELECTROMECHANICAL(IndustrialTier.T1, TextKey.of("jsindustrial.circuit.electromechanical", "Electromechanical"),
            TextKey.of("jsindustrial.circuit.electromechanical.what", "Relays."),
            TextKey.of("jsindustrial.circuit.electromechanical.made", "By hand.")),
    VACUUM_TUBE(IndustrialTier.T2, TextKey.of("jsindustrial.circuit.vacuum_tube", "Vacuum tube"),
            TextKey.of("jsindustrial.circuit.vacuum_tube.what",
                    "Thermionic valves on a printed board: a tungsten filament, glass and a Kovar seal."),
            TextKey.of("jsindustrial.circuit.vacuum_tube.made", "By hand, with the soldering iron.")),
    TRANSISTOR(IndustrialTier.T3, TextKey.of("jsindustrial.circuit.transistor", "Transistor"),
            TextKey.of("jsindustrial.circuit.transistor.what",
                    "Discrete silicon transistors, grown in the Czochralski furnace, on a printed board."),
            TextKey.of("jsindustrial.circuit.transistor.made", "In the Precision Assembler.")),
    INTEGRATED(IndustrialTier.T4, TextKey.of("jsindustrial.circuit.integrated", "Integrated"),
            TextKey.of("jsindustrial.circuit.integrated.what",
                    "The first made by lithography, with the ultraviolet light of mercury lamps."),
            TextKey.of("jsindustrial.circuit.integrated.made", "In the Clean Room.")),
    NANOSCALE(IndustrialTier.T5, TextKey.of("jsindustrial.circuit.nanoscale", "Nanoscale"),
            TextKey.of("jsindustrial.circuit.nanoscale.what",
                    "Deep ultraviolet lithography, then extreme ultraviolet."),
            TextKey.of("jsindustrial.circuit.nanoscale.made", "In the Clean Room.")),
    QUANTUM(IndustrialTier.T6, TextKey.of("jsindustrial.circuit.quantum", "Quantum"),
            TextKey.of("jsindustrial.circuit.quantum.what", "Quantum states laid layer by layer."),
            TextKey.of("jsindustrial.circuit.quantum.made", "In the Clean Room, beside a microgravity chamber.")),
    SINGULARITY(IndustrialTier.T7, TextKey.of("jsindustrial.circuit.singularity", "Singularity"),
            TextKey.of("jsindustrial.circuit.singularity.what", "Built with controlled antimatter microexplosions."),
            TextKey.of("jsindustrial.circuit.singularity.made", "In the Clean Room, beside an antimatter chamber.")),
    TRANSCENDENT(IndustrialTier.T8, TextKey.of("jsindustrial.circuit.transcendent", "Transcendent"),
            TextKey.of("jsindustrial.circuit.transcendent.what", "Circuits made across a wormhole."),
            TextKey.of("jsindustrial.circuit.transcendent.made", "In the Clean Room, beside an open Wormhole Gate.")),
    OMNIVERSAL(IndustrialTier.T9, TextKey.of("jsindustrial.circuit.omniversal", "Omniversal"),
            TextKey.of("jsindustrial.circuit.omniversal.what", "Materialised directly."),
            TextKey.of("jsindustrial.circuit.omniversal.made", "In the Creation Forge, with no Clean Room."));

    private final IndustrialTier tier;
    private final TextKey name;
    private final TextKey what;
    private final TextKey made;
    private final TagKey<Item> tag;

    /* The last tier whose circuit is made by hand: the vacuum tube's, soldered at a bench. */
    private static final IndustrialTier LAST_BY_HAND = IndustrialTier.T2;

    Circuit(final IndustrialTier tier, final TextKey name, final TextKey what, final TextKey made) {
        this.tier = tier;
        this.name = name;
        this.what = what;
        this.made = made;
        this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
                "circuits/" + name().toLowerCase(Locale.ROOT)));
    }

    /** Has making each circuit move a player to its tier, once, from the mod's constructor. */
    public static void declare() {
        for (final Circuit circuit : values()) {
            ProgressionTriggers.whenMade(circuit.tag, SeriesAxes.INDUSTRIAL_TIER, circuit.tier);
        }
    }

    /** The circuit of {@code tier}: there is one for each. */
    public static Circuit of(final IndustrialTier tier) {
        for (final Circuit circuit : values()) {
            if (circuit.tier == tier) {
                return circuit;
            }
        }
        throw new IllegalArgumentException("no circuit of the tier " + tier);
    }

    /** The tier this circuit defines. */
    public IndustrialTier tier() {
        return this.tier;
    }

    /** Its name: "Transistor". */
    public TextKey nameKey() {
        return this.name;
    }

    /** What it is: "Discrete silicon transistors ... on a printed board." */
    public TextKey whatKey() {
        return this.what;
    }

    /** How it is made: "In the Precision Assembler." */
    public TextKey madeKey() {
        return this.made;
    }

    /** The item tag of its items, {@code jsindustrial:circuits/<name>}. */
    public TagKey<Item> tag() {
        return this.tag;
    }

    /** Whether it can always be made by hand: the first three, up to the vacuum tube. */
    public boolean byHand() {
        return this.tier.isAtMost(LAST_BY_HAND);
    }
}
