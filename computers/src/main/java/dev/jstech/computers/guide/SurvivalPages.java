/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.villager.ComputerVillagers;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;
import java.util.List;

/**
 * The section of the Guide to Operations about what the world holds of the computers before a player builds any
 * (computers/docs/design/survival.md, "Abandoned buildings" and "Villagers"): the abandoned buildings and their
 * hardware boxes, and the two villagers who trade in computers, with their job sites.
 */
final class SurvivalPages {

    private SurvivalPages() {
    }

    /** Declares the section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("found_in_the_world").titled("Found in the world")
                .in(chapter).register();
        buildings(guide, section);
        villagers(guide, section);
    }

    private static void buildings(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("abandoned_buildings", section).titled("Abandoned buildings")
                .paragraph("Twenty kinds of building lie abandoned across the Overworld, and you find each of one"
                        + " era: the Vintage near the world's spawn, the Advanced the farther you go. A house with an"
                        + " old computer in its attic, a dump of broken machines, a shop, a school, a library, a bank"
                        + " with a Mainframe in its vault, a factory of the chain's machines, a datacenter, a"
                        + " supercomputing centre.")
                .paragraph("About one in five still works: its machines whole, their systems installed, ready to"
                        + " come up as soon as you give them energy. The rest are ruins, their computers open with half"
                        + " their parts in their seats, and you find their loot in chests and hardware boxes.")
                .paragraph("Every building holds a way to a graphical system in its main chest: a system with its"
                        + " own desktop, or in the Vintage UNIX System V and CDE for it.")
                .define("Hardware box", "A carton found in the buildings, printed with its era's machine: a"
                        + " computer's parts, or a graphics card. It opens once and stays open.")
                .register();
    }

    private static void villagers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("computer_villagers", section).titled("The Programmer and the IT Technician")
                .icon(ComputerVillagers.PROGRAMMERS_DESK)
                .coversAll(() -> List.of(ComputerVillagers.PROGRAMMERS_DESK.get(),
                        ComputerVillagers.REPAIR_BENCH.get()))
                .paragraph("A villager who takes a Programmer's Desk becomes a Programmer, who sells you systems"
                        + " and programs on the media of their era and buys your paper, books and blank media. One who"
                        + " takes a Repair Bench becomes an IT Technician, who sells you parts (boards with their"
                        + " firmware), network cables in packs of eight and monitors, and buys your copper, tin and"
                        + " the parts of older eras.")
                .paragraph("A villager's level is the era of what it sells: Novice the Vintage, Apprentice the"
                        + " Legacy, Journeyman the Transition, Expert the Standard, Master the Advanced. It keeps the"
                        + " trades of the levels below, and a system can be bought three times before it restocks.")
                .paragraph("Every kind of village may have a computer shop with both job sites and a Vintage demo"
                        + " computer running MC-DOS. Each job site is two things of the trade on four planks: the"
                        + " floppy disk and the paper for the desk, the solder and the iron ingot for the bench.")
                .register();
    }
}
