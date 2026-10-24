/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_TAB;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.industrial.IndustrialFluids;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.industrial.research.ResearchLink;

/**
 * The drawings of the research: the Computational Research Centre, its blocks and the parts that go in its places,
 * the data its discoveries spend, the groups the Research Link Cable joins, and the hot water its coolant comes out
 * as. Every number is the machine's own.
 */
final class ResearchPages {

    private ResearchPages() {
    }

    /** Declares the drawings, the research in {@code science} and the hot water in {@code fluids}. */
    static void declare(final ModGuide guide, final ModGuide.SectionRef science, final ModGuide.SectionRef fluids) {
        centre(guide, science);
        data(guide, science);
        groups(guide, science);
        hotWater(guide, fluids);
    }

    private static void centre(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("computational_research_centre", section).titled("Computational Research Centre")
                .icon(CrcContent.TERMINAL)
                .covers(CrcContent.TERMINAL).covers(CrcContent.CASING).covers(CrcContent.GATE).covers(CrcContent.BUS)
                .covers(CrcContent.POWER_ENTRY).covers(CrcContent.LINK_SOCKET).covers(CrcContent.COOLANT_INLET)
                .covers(CrcContent.COOLANT_OUTLET).covers(CrcContent.HEAT_PORT)
                .covers(CrcContent.RESEARCH_PROCESSOR).covers(CrcContent.INTEGRATED_RESEARCH_PROCESSOR)
                .covers(CrcContent.NANOSCALE_RESEARCH_PROCESSOR).covers(CrcContent.HEAT_SINK)
                .covers(CrcContent.LIQUID_COOLER).covers(CrcContent.HEAT_PIPE_COOLER)
                .whatItIs("A research computer of 1959: three grey cabinets 9 blocks deep, 3 wide and 3 high, built"
                        + " of its own blocks. It works your team's discoveries.")
                .whatItIsFor("Making discoveries, which open the recipes that need them. A discovery costs research"
                        + " points, which its processors compute, and the data it asks for.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Build it of CRC Casing, the CRC Terminal in the middle of the front, at your eyes.",
                        "Along the middle of its middle layer, behind the Terminal: Bus, Gate, Bus, Gate, Bus, Gate,"
                                + " Bus.",
                        "In the six places of the rear's two lower rows: one CRC Power Entry, and if you want"
                                + " them a Link Socket, a Coolant Inlet with its Outlet and a Heat Port; casing in"
                                + " the rest.",
                        "Bring 11 kV to the Power Entry.",
                        "Open the Terminal: fill its 27 places, put data in its data places, and pick a discovery"
                                + " on its Research tab.")
                .nextPage()
                .subheading("Its parts")
                .paragraph("Research Processor: 1 point a second for 5 kW. Integrated Research Processor: 4 points"
                        + " for 20 kW. Nanoscale Research Processor: 16 points for 80 kW. All the power a processor"
                        + " draws becomes heat.")
                .paragraph("Heat Sink: sheds 5 kW to the air. Liquid Cooler: up to 15 kW into the water flowing from"
                        + " the Coolant Inlet to the Outlet. Heat Pipe Cooler: up to 25 kW out through the Heat Port.")
                .nextColumn()
                .whatCanGoWrong("It does not form.",
                        "A block is wrong, or the entries are not as it takes them: one Power Entry, at most one of"
                                + " each other, the Inlet with its Outlet. Jade on the Terminal says what is wrong.",
                        "It does not compute.",
                        "No discovery is chosen, its data is short, your team lacks what it comes after, or no"
                                + " power reaches the Power Entry.",
                        "A processor is gone.",
                        "It burnt: past 100 °C the hungriest one burns. Add coolers; SHED turns red while it sheds"
                                + " less heat than it makes.")
                .seeAlso("jsindustrial:research_data", "jsindustrial:research_groups")
                .register();
    }

    private static void data(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("research_data", section).titled("Research data").icon(IndustrialResearch.CRYSTAL_DATA)
                .covers(IndustrialResearch.SAMPLE_DATA).covers(IndustrialResearch.CRYSTAL_DATA)
                .covers(IndustrialResearch.SPECTRAL_DATA).covers(IndustrialResearch.PARTICLE_DATA)
                .whatItIs("The records the instruments leave: Sample Data, what a material is made of; Crystal"
                        + " Data, the structure of a solid; Spectral Data, the lines of light of each element;"
                        + " Particle Data, what an accelerator's runs show.")
                .whatItIsFor("Discoveries ask for them. A discovery that asks for 10 data spends one each tenth of"
                        + " the way.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Put them in the data places of a Computational Research Centre, each kind in its own.",
                        "The CRCs of your team in one group spend the data of all of them.")
                .whatCanGoWrong("It stopped part of the way.",
                        "Its data ran short. The card on its Research tab says how many more, and what gives them.")
                .seeAlso("jsindustrial:computational_research_centre")
                .register();
    }

    private static void groups(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("research_groups", section).titled("Research groups").icon(() -> ResearchLink.CABLE)
                .covers(() -> ResearchLink.CABLE)
                .whatItIs("The Research Link Cable joins Computational Research Centres into a group: every CRC on"
                        + " one run of it works the same discovery.")
                .whatItIsFor("Researching faster. Each team counts its own CRCs: the strongest whole, each other at"
                        + " nine tenths.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Give each CRC a CRC Link Socket on its rear.",
                        "Lay the cable from socket to socket.",
                        "Choose the discovery on any of them: the whole group works it, then its queue.",
                        "Dye a run to keep two groups apart: runs of two colours never join.")
                .whatCanGoWrong("A CRC is not in the group.",
                        "It has no Link Socket, the cable does not reach it, or its run is another colour. Its Group"
                                + " tab shows the CRCs it is joined to.",
                        "The cable was cut.",
                        "Nothing is lost: the progress is your team's. Join them again and they go on.")
                .seeAlso("jsindustrial:computational_research_centre")
                .register();
    }

    private static void hotWater(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("hot_water", section).titled("Hot water")
                .icon(IndustrialFluids.HOT_WATER.bucket().orElseThrow())
                .covers(IndustrialFluids.HOT_WATER.bucket().orElseThrow())
                .whatItIs("Water warmed to 60 °C: what a cooling loop gives back, such as the Liquid Coolers of a"
                        + " Computational Research Centre.")
                .whatItIsFor("Carrying a machine's heat away: cold water goes in, takes the heat and leaves hot.")
                .howToGetIt("From a CRC's Coolant Outlet while its Liquid Coolers work, or from the creative menu,"
                        + " J's Industrial tab.")
                .nextColumn()
                .howToUseIt("Pipe it away from the Coolant Outlet, into a Tank or wherever it is wanted.",
                        "Keep cold water coming into the Coolant Inlet.")
                .whatCanGoWrong("The CRC heats though its Liquid Coolers are in place.",
                        "Its Coolant Outlet is full, or its Inlet is dry. Pipe the hot water away and bring cold"
                                + " water in.")
                .seeAlso("jsindustrial:computational_research_centre", "jsindustrial:tank")
                .register();
    }
}
