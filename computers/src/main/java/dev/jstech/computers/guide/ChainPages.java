/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import static dev.jstech.computers.guide.ComputersGuide.blocksOf;

import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;

/**
 * Declares the pages of the mod's own chain in the building chapter (computers/docs/design/survival.md): the
 * Electronics Workbench of each era and how it assembles, and the parts it assembles on the way to a computer's.
 */
final class ChainPages {

    private ChainPages() {
    }

    /** Declares the chain's section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("chain").titled("Making the parts").in(chapter).register();
        workbench(guide, section);
        parts(guide, section);
    }

    private static void workbench(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("electronics_workbench", section).titled("The Electronics Workbench")
                .icon(ChainBenches.VINTAGE)
                .coversAll(blocksOf(WorkbenchBlock.class))
                .paragraph("Everything electronic in a computer starts at an Electronics Workbench, one for each era"
                        + " from the Vintage to the Advanced. A bench assembles the parts of its era and of every era"
                        + " before it, and the bench of the next era. It is your own bench, used by hand: nothing"
                        + " reaches its trays through its sides, and no Crafting Interface works it.")
                .define("Electronics Workbench", "The bench of an era where the chain's parts are assembled by"
                        + " hand, from the ingredients laid in its nine trays.")
                .paragraph("Lay each ingredient the part asks for in a tray of its own, in any tray and any order,"
                        + " with at least the count the part takes, and leave nothing else in the trays. The part shows"
                        + " faded beside the trays once the bench knows it, and the groove fills in the colour of the"
                        + " era's tool as it comes.")
                .paragraph("The bench works as a furnace does: each part takes its time, and you may close its window"
                        + " meanwhile. The finished part waits in the part's tray until you take it, and the bench"
                        + " starts the next while the trays still hold enough.")
                .paragraph("It draws energy through the socket on its back and nowhere else, while it works. The"
                        + " power strip on its window has a lamp lit while energy reaches the bench, what it draws, and"
                        + " the energy it holds. Each era's parts are a kind of recipe of their own, which recipe"
                        + " viewers show with the benches that make them.")
                .table("What each bench draws while it works", "Bench", "Draw")
                .row(GuideCell.words("Vintage Electronics Workbench"), GuideCell.words("40 W"))
                .row(GuideCell.words("Legacy Electronics Workbench"), GuideCell.words("60 W"))
                .row(GuideCell.words("Transition Electronics Workbench"), GuideCell.words("300 W"))
                .row(GuideCell.words("Standard Electronics Workbench"), GuideCell.words("80 W"))
                .row(GuideCell.words("Advanced Electronics Workbench"), GuideCell.words("120 W"))
                .ifSomethingGoesWrong("The bench does not start.",
                        "A tray holds something the part does not take, an ingredient is short of its count, the"
                                + " part is of a later era than the bench, or no energy reaches its back.",
                        "The bench stopped with the part half done.",
                        "The part's tray is full: take the finished parts out.",
                        "A hopper puts nothing in the bench.",
                        "It takes nothing through its sides: lay the ingredients in by hand.")
                .seeAlso("jsc:chain_parts")
                .register();
    }

    private static void parts(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("chain_parts", section).titled("The chain's parts")
                .icon(ChainParts.MEMORY_CHIPS.getFirst())
                .coversAll(parts())
                .paragraph("On the way from the ore to a computer's parts, the chain makes parts of its own. You"
                        + " assemble each at the Electronics Workbench of its era or a later one"
                        + " ([](jsc:electronics_workbench)), and find it on the tab of the era it first appears in.")
                .table("The chain's parts", "Parts", "From")
                .row(GuideCell.words("Memory and controller chips, their masks and finished wafers"),
                        GuideCell.words("one of each for every era"))
                .row(GuideCell.words("Mask plate, ceramic package, ceramic and tantalum capacitors, ferrite,"
                        + " inductor, transformer"), GuideCell.words("Vintage"))
                .row(GuideCell.words("Tube screen, FR-2 laminate, aluminium bonding wire, floppy film, platter"
                        + " (Vintage)"), GuideCell.words("Vintage"))
                .row(GuideCell.words("Gallium arsenide laser diode, cold cathode lamp, LCD screen, FR-4 laminate,"
                        + " glass fibre, gold bonding wire, platter, blank disc, thermal paste"),
                        GuideCell.words("Legacy"))
                .row(GuideCell.words("AlGaInP laser diode"), GuideCell.words("Transition"))
                .row(GuideCell.words("Gallium nitride laser diode, LED screen, optical fibre"),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("Self-lit panel"), GuideCell.words("Advanced"))
                .row(GuideCell.words("Etched board"), GuideCell.words("one for every era"))
                .paragraph("The lasers of the CD, DVD and Blu-ray drives come in the eras of their drives, and each"
                        + " screen in the era its kind of monitor first stands on a desk.")
                .seeAlso("jsc:electronics_workbench")
                .register();
    }

    /* The parts on the chain's shelf of every era, the benches left to their own page. */
    private static Supplier<List<ItemLike>> parts() {
        return () -> ChainParts.ERAS.stream()
                .flatMap(era -> ComputingContent.shelf(Shelf.CHAIN, era).items().stream())
                .filter(item -> !(item instanceof BlockItem)).map(item -> (ItemLike) item).toList();
    }
}
