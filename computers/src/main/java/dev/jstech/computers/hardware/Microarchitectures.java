/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.List;

/**
 * The chip designs this mod's processors and graphics cards are built on, each with the work one of its cores does
 * per gigahertz.
 *
 * <p>The names follow one rule: a maker's product brand gets the parody the mod sells it under, and a design's own
 * codename stays the real one. So Intel's Core design reads as Centro, the line the mod sells it in, and AMD's Zen as
 * Way, while Haswell, K10 and Kepler keep their names. A graphics design sold under a brand and never given a public
 * design name of its own is left unnamed, and its card names its chip instead.
 *
 * <p>The processor efficiencies count one core of the P6 design (the Pentium Pro, II and III) at a gigahertz as 1.0.
 * A graphics design's efficiency is on the same scale as a processor's capacity, so that a card's power reads in the
 * same items per tick: the fastest card of an era does a little more than the fastest processor of it. What a card
 * lists as its cores changes over the years (pipelines on the early ones, shaders on the later), which is why those
 * efficiencies swing so far between generations while the power they give climbs steadily.
 */
public final class Microarchitectures {

    /** A part that says nothing of its design is counted as the P6 is. */
    public static final Microarchitecture UNSPECIFIED = new Microarchitecture("unspecified", "", 1000);

    // Processors, oldest first.

    public static final Microarchitecture I486 = new Microarchitecture("i486", "486", 450);
    public static final Microarchitecture P5 = new Microarchitecture("p5", "P5", 600);
    public static final Microarchitecture K5 = new Microarchitecture("k5", "K5", 600);
    public static final Microarchitecture K6 = new Microarchitecture("k6", "K6", 750);
    public static final Microarchitecture P6 = new Microarchitecture("p6", "P6", 1000);
    public static final Microarchitecture K7 = new Microarchitecture("k7", "K7", 1100);
    public static final Microarchitecture NETBURST = new Microarchitecture("netburst", "NetBurst", 700);
    public static final Microarchitecture K8 = new Microarchitecture("k8", "K8", 1300);
    /** Intel's Core design, named after the line the mod sells it in. */
    public static final Microarchitecture CENTRO = new Microarchitecture("centro", "Centro", 1600);
    public static final Microarchitecture K10 = new Microarchitecture("k10", "K10", 1500);
    public static final Microarchitecture NEHALEM = new Microarchitecture("nehalem", "Nehalem", 2000);
    public static final Microarchitecture WESTMERE = new Microarchitecture("westmere", "Westmere", 2000);
    public static final Microarchitecture SANDY_BRIDGE = new Microarchitecture("sandy_bridge", "Sandy Bridge", 2400);
    public static final Microarchitecture IVY_BRIDGE = new Microarchitecture("ivy_bridge", "Ivy Bridge", 2400);
    public static final Microarchitecture HASWELL = new Microarchitecture("haswell", "Haswell", 2700);
    public static final Microarchitecture BULLDOZER = new Microarchitecture("bulldozer", "Bulldozer", 1350);
    public static final Microarchitecture PILEDRIVER = new Microarchitecture("piledriver", "Piledriver", 1350);
    public static final Microarchitecture SKYLAKE = new Microarchitecture("skylake", "Skylake", 2900);
    public static final Microarchitecture KABY_LAKE = new Microarchitecture("kaby_lake", "Kaby Lake", 2900);
    public static final Microarchitecture COFFEE_LAKE = new Microarchitecture("coffee_lake", "Coffee Lake", 2900);
    public static final Microarchitecture COMET_LAKE = new Microarchitecture("comet_lake", "Comet Lake", 2900);
    /** AMD's Zen and Zen+, named Way as the Awayken line is. */
    public static final Microarchitecture WAY_1 = new Microarchitecture("way_1", "Way 1", 2700);
    public static final Microarchitecture WAY_1_PLUS = new Microarchitecture("way_1_plus", "Way 1+", 2700);
    public static final Microarchitecture WAY_2 = new Microarchitecture("way_2", "Way 2", 3100);
    public static final Microarchitecture WAY_3 = new Microarchitecture("way_3", "Way 3", 3500);
    public static final Microarchitecture WAY_4 = new Microarchitecture("way_4", "Way 4", 3800);
    public static final Microarchitecture WAY_5 = new Microarchitecture("way_5", "Way 5", 4100);
    /** The performance cores of the hybrid chips; their efficiency cores are the two designs after. */
    public static final Microarchitecture ALDER_LAKE = new Microarchitecture("alder_lake", "Alder Lake", 3600);
    public static final Microarchitecture RAPTOR_LAKE = new Microarchitecture("raptor_lake", "Raptor Lake", 3600);
    public static final Microarchitecture ARROW_LAKE = new Microarchitecture("arrow_lake", "Arrow Lake", 3900);
    public static final Microarchitecture GRACEMONT = new Microarchitecture("gracemont", "Gracemont", 2300);
    public static final Microarchitecture SKYMONT = new Microarchitecture("skymont", "Skymont", 2900);
    public static final Microarchitecture SKYLAKE_SP = new Microarchitecture("skylake_sp", "Skylake-SP", 2800);
    public static final Microarchitecture CASCADE_LAKE = new Microarchitecture("cascade_lake", "Cascade Lake", 2800);
    public static final Microarchitecture ICE_LAKE_SP = new Microarchitecture("ice_lake_sp", "Ice Lake-SP", 3100);
    public static final Microarchitecture COOPER_LAKE = new Microarchitecture("cooper_lake", "Cooper Lake", 3100);
    public static final Microarchitecture SAPPHIRE_RAPIDS =
            new Microarchitecture("sapphire_rapids", "Sapphire Rapids", 3400);
    public static final Microarchitecture EMERALD_RAPIDS =
            new Microarchitecture("emerald_rapids", "Emerald Rapids", 3400);

    // Graphics cards, oldest first.

    public static final Microarchitecture VGA = new Microarchitecture("vga", "VGA", 10_000);
    /** Rendition's design, sold under a brand and never named otherwise; its card names the chip. */
    public static final Microarchitecture RENDITION = new Microarchitecture("rendition", "", 60_000);
    public static final Microarchitecture NV3 = new Microarchitecture("nv3", "NV3", 20_000);
    /**
     * ATI's design of the Rage line, sold under the brand and never named otherwise; left unnamed. A little behind
     * the NV3 of the same year, as the Rage Pro was behind the RIVA 128.
     */
    public static final Microarchitecture RAGE = new Microarchitecture("rage", "", 40_000);
    /** 3dfx's design, sold under the Voodoo brand; left unnamed. */
    public static final Microarchitecture THREEDFX = new Microarchitecture("threedfx", "", 30_000);
    /**
     * The RIVA TNT's and TNT2's: the TNT lands between the RIVA 128 and the Voodoo2 of its year, and the TNT2 M64
     * that opens the next era under the Celsius cards above it.
     */
    public static final Microarchitecture FAHRENHEIT = new Microarchitecture("fahrenheit", "Fahrenheit", 36_000);
    public static final Microarchitecture CELSIUS = new Microarchitecture("celsius", "Celsius", 25_000);
    /** The first Radeon's; its one-pipeline card sits with the TNT2 M64 at the floor of its era. */
    public static final Microarchitecture R100 = new Microarchitecture("r100", "R100", 50_000);
    public static final Microarchitecture R200 = new Microarchitecture("r200", "R200", 25_000);
    public static final Microarchitecture KELVIN = new Microarchitecture("kelvin", "Kelvin", 30_000);
    public static final Microarchitecture R300 = new Microarchitecture("r300", "R300", 25_000);
    /** The GeForce FX's, whose entry card was slower per pipeline than the cards before it. */
    public static final Microarchitecture RANKINE = new Microarchitecture("rankine", "Rankine", 13_000);
    /** The GeForce 6's: its 6800 Ultra and the R400's X800 XT top their era a little above its fastest processor. */
    public static final Microarchitecture CURIE = new Microarchitecture("curie", "Curie", 23_500);
    public static final Microarchitecture R400 = new Microarchitecture("r400", "R400", 19_000);
    /** The Radeon X1000's; its X1950 XTX sits a little under the Vertex 7800 GTX it chased. */
    public static final Microarchitecture R500 = new Microarchitecture("r500", "R500", 22_000);
    /**
     * The first unified Radeons', from the HD 2000 to the HD 4000: their shaders worked in groups of five, as the
     * TeraScale 2 ones did, so a card is counted a group at a time.
     */
    public static final Microarchitecture TERASCALE = new Microarchitecture("terascale", "TeraScale", 6500, 5);
    /** Counted at the clock its shaders run at, which is twice the rest of the chip's. */
    public static final Microarchitecture TESLA = new Microarchitecture("tesla", "Tesla", 2400);
    /** Counted at the clock its shaders run at, which is twice the rest of the chip's. */
    public static final Microarchitecture FERMI = new Microarchitecture("fermi", "Fermi", 1800);
    /** Its shaders worked in groups of five, so a card is counted a group at a time. */
    public static final Microarchitecture TERASCALE_2 = new Microarchitecture("terascale_2", "TeraScale 2", 5000, 5);
    /*
     * The Radeons from the HD 7000 to the RX 400: their shaders did about as much a clock through the generations of
     * the design, so one entry covers them, and each card names its chip.
     */
    public static final Microarchitecture GCN = new Microarchitecture("gcn", "GCN", 1000);
    public static final Microarchitecture KEPLER = new Microarchitecture("kepler", "Kepler", 1000);
    /** Each of its shaders did about a third more a clock than a Kepler one. */
    public static final Microarchitecture MAXWELL = new Microarchitecture("maxwell", "Maxwell", 1350);
    /** Maxwell's shaders on a finer process, doing as much a clock at a much higher clock. */
    public static final Microarchitecture PASCAL = new Microarchitecture("pascal", "Pascal", 1350);

    private static final List<Microarchitecture> ALL = List.of(UNSPECIFIED,
            I486, P5, K5, K6, P6, K7, NETBURST, K8, CENTRO, K10, NEHALEM, WESTMERE, SANDY_BRIDGE, IVY_BRIDGE, HASWELL,
            BULLDOZER, PILEDRIVER, SKYLAKE, KABY_LAKE, COFFEE_LAKE, COMET_LAKE, WAY_1, WAY_1_PLUS, WAY_2, WAY_3, WAY_4,
            WAY_5, ALDER_LAKE, RAPTOR_LAKE, ARROW_LAKE, GRACEMONT, SKYMONT, SKYLAKE_SP, CASCADE_LAKE, ICE_LAKE_SP,
            COOPER_LAKE, SAPPHIRE_RAPIDS, EMERALD_RAPIDS,
            VGA, RENDITION, NV3, RAGE, THREEDFX, FAHRENHEIT, CELSIUS, R100, R200, KELVIN, R300, RANKINE, CURIE, R400,
            R500, TESLA, TERASCALE, FERMI, TERASCALE_2, GCN, KEPLER, MAXWELL, PASCAL);

    private Microarchitectures() {
    }

    /** Every design in the table. */
    public static List<Microarchitecture> all() {
        return ALL;
    }
}
