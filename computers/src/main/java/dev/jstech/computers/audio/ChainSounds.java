/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.content.SoundBuilder;
import java.util.Map;
import java.util.Optional;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;

/**
 * The sounds the chain's machines make as they work (computers/docs/design/survival.md), each a running loop heard
 * at the machine while it works: the Crusher's rolls on the ore, the Grinder's drum, the hum of the Induction Furnace's
 * coil, the bubbling of the Electrolytic Cell over its current's hum, and so on down the chain to the datacenter's
 * cooling plant. The Lithography Stations of every era sound alike, and so do the Electronics Assembly Lines.
 */
public final class ChainSounds {

    /* The families of one machine to an era, by the name their loop goes by. */
    private static final String LITHOGRAPHY = "lithography";
    private static final String ASSEMBLY = "assembly_line";
    /* How far a machine of the chain is heard, in blocks: a plant's machines are big and loud. */
    private static final int RANGE = 20;
    /* The folder of the mod's sounds the loops are in. */
    private static final String FOLDER = "machine/";
    /* The machines' loops, by the machine's id; a family of one machine to an era goes by its family's name. */
    private static final Map<String, SoundKey> WORK = Map.ofEntries(
            work(loop("crusher").subtitle("Crusher rumbles")),
            work(loop("grinder").subtitle("Grinder drum turns")),
            work(loop("induction_furnace").subtitle("Induction furnace hums")),
            work(loop("electrolytic_cell").subtitle("Electrolytic cell bubbles")),
            work(loop("rolling_mill").subtitle("Rolling mill rolls")),
            work(loop("wire_drawer").subtitle("Wire drawer pulls")),
            work(loop("coke_oven").subtitle("Coke oven roars")),
            work(loop("chemical_reactor").subtitle("Reactor agitator stirs")),
            work(loop("air_separation_unit").subtitle("Air compressor runs")),
            work(loop("siemens_reactor").subtitle("Siemens reactor hums")),
            work(loop("crystal_puller").subtitle("Crystal puller pumps")),
            work(loop("wafer_saw").subtitle("Wafer saw cuts")),
            work(loop("plasma_etcher").subtitle("Plasma etcher buzzes")),
            work(loop("ion_implanter").subtitle("Ion implanter hums")),
            work(loop("deposition_chamber").subtitle("Deposition chamber hums")),
            work(loop("packaging_line").subtitle("Packaging line works")),
            work(loop("laminator").subtitle("Laminator presses")),
            work(loop("board_etcher").subtitle("Board etcher sprays")),
            work(loop("ceramic_kiln").subtitle("Kiln burns")),
            work(loop("coil_winder").subtitle("Coil winder spins")),
            work(loop("optical_polisher").subtitle("Polisher grinds")),
            work(loop("display_line").subtitle("Display line gantry moves")),
            work(loop(LITHOGRAPHY).subtitle("Lithography station runs")),
            work(loop(ASSEMBLY).subtitle("Placement head works")),
            work(loop("preinstaller").subtitle("Preinstaller writes")),
            work(loop("large_scale_printer").subtitle("Printing press runs")),
            work(loop("cooling_tower").subtitle("Cooling tower fan roars")),
            work(loop("chiller").subtitle("Chiller compressor runs")));

    private ChainSounds() {
    }

    /** The loop the chain's machine {@code id} runs while it works; none for one that works silent. */
    public static Optional<SoundKey> work(final String id) {
        if (id.startsWith(LITHOGRAPHY + "_")) {
            return Optional.of(WORK.get(LITHOGRAPHY));
        }
        if (id.startsWith("assembly_")) {
            return Optional.of(WORK.get(ASSEMBLY));
        }
        return Optional.ofNullable(WORK.get(id));
    }

    /** Loads the declarations, so they reach the registries with the rest of the mod's content. */
    public static void init() {
        // Nothing to do: loading the class is what declares the sounds.
    }

    /* The running loop of the chain's machine {@code id}, heard over a plant. */
    private static SoundBuilder loop(final String id) {
        return CONTENT.sound(FOLDER + id).loop().range(RANGE);
    }

    /* The loop declared, under the machine's id. */
    private static Map.Entry<String, SoundKey> work(final SoundBuilder loop) {
        final SoundKey key = loop.register();
        return Map.entry(key.id().getPath().substring(FOLDER.length()), key);
    }
}
