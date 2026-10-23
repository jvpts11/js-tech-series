/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_JS_TECH_TAB;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_TAB;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.GENERATOR;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SECOND_GENERATOR;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SET_UP_NEXT;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SET_UP_PLAN;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.industrial.IndustrialModule;
import net.minecraft.world.item.Items;

/**
 * J's Industrial in the manuals: its chapter, written once and shown both in the series' Technical Reference and in
 * the mod's own Plant Drawings, a folder of blueprints in which every entry is a drawing of one sheet or more. Its
 * sections follow the pages of the mod's design, each numbering its drawings by the letters of its trade, as a real
 * set does; a section shows once it has its first drawing. The machines are shown as their blocks, which the reader
 * turns, with numbered balloons, and set up in plans seen from above. Every number is the machines' own.
 */
public final class IndustrialGuide {

    /** The mod's own manual, and the style it is drawn in. */
    public static final String PLANT_DRAWINGS = "jsindustrial:plant_drawings";
    public static final String DRAWINGS_STYLE = "jsindustrial:drawings";

    public static final ItemEntry<ManualItem> MANUAL = IndustrialModule.CONTENT.item("plant_drawings",
            properties -> new ManualItem(properties, PLANT_DRAWINGS)).named("Plant Drawings")
            .tab(IndustrialModule.MANUALS).register();

    /* A machine's model: how tall it is drawn, turned so its front faces the reader, and its front and its side. */
    private static final int MODEL = 70;
    private static final String FACING_THE_READER = "facing=south";
    private static final GuideBlock.ScenePart FRONT = GuideBlock.ScenePart.of(0, 0, 0).box(3, 3, 15, 13, 13, 16);
    private static final GuideBlock.ScenePart SIDE = GuideBlock.ScenePart.of(0, 0, 0).box(15, 3, 3, 16, 13, 13);

    private IndustrialGuide() {
    }

    /** Declares the chapter, the drawings and their style, before the data generation writes the mod's files. */
    public static void declare() {
        final ModGuide guide = IndustrialModule.CONTENT.guide();
        // Held over an item, the manual key fills a bar in the black and yellow of a machine's guard.
        guide.style("drawings", CoreGuide.drawings("jsindustrial:guide/drawings", "JI")
                .withHoldBar(GuideStyle.HoldBar.HAZARD));
        guide.manual("plant_drawings").titled("Plant Drawings").cover("J's Industrial")
                .edition("Set A - Sheets 1 to %s").style(DRAWINGS_STYLE).chapters("jsindustrial").priority(50)
                .icon("jsindustrial:gui/guide/cover_mark")
                .about("New to machines? Start with JI-GN-001, then JI-GS-001.")
                .register();
        guide.chapter().titled("J's Industrial").order(40).tab("#FFF0B23A")
                .about("Machines that turn ores and metals into the parts everything else is made from, and the"
                        + " energy they run on.")
                .register();
        // The sections in the order of the mod's design, each lettered for its trade; an empty one is not shown.
        final ModGuide.SectionRef reading = guide.section("reading").titled("Reading the drawings").code("GN")
                .icon(MANUAL).register();
        final ModGuide.SectionRef starting = guide.section("getting_started").titled("Getting Started")
                .code("GS").icon(IndustrialModule.COAL_GENERATOR).register();
        guide.section("the_world").titled("The World").code("GE").icon(() -> Items.IRON_ORE).register();
        final ModGuide.SectionRef metallurgy = guide.section("metallurgy").titled("Metallurgy").code("MT")
                .icon(() -> Items.IRON_INGOT).register();
        final ModGuide.SectionRef power = guide.section("power").titled("Power").code("EL")
                .icon(() -> SharedEnergy.INSULATED_COPPER_CABLE).register();
        final ModGuide.SectionRef machines = guide.section("machines").titled("Machines").code("MC")
                .icon(IndustrialModule.COMPRESSOR).register();
        final ModGuide.SectionRef fluids = guide.section("fluids_and_gases").titled("Fluids and Gases")
                .code("PI").icon(IndustrialModule.TANK).register();
        guide.section("chemistry").titled("Chemistry").code("CH").register();
        guide.section("conveyors").titled("Conveyors").code("CV").register();
        guide.section("clean_room").titled("Clean Room").code("CR").register();
        guide.section("science").titled("Science").code("SC").register();
        guide.section("nuclear").titled("Nuclear").code("NU").register();
        guide.section("transcendence").titled("Transcendence").code("TR").register();
        guide.section("the_aleph").titled("The Aleph").code("AL").register();
        guide.section("tools_and_armour").titled("Tools and Armour").code("TA").register();
        ReadingPages.declare(guide, reading);
        coalGenerator(guide, starting);
        macerator(guide, starting);
        electricFurnace(guide, starting);
        compressor(guide, machines);
        power(guide, power);
        materials(guide, metallurgy);
        tank(guide, fluids);
    }

    private static void tank(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("tank", section).titled("Tank").icon(IndustrialModule.TANK).covers(IndustrialModule.TANK)
                .whatItIs("A plain tank that holds up to 16 buckets of one fluid.")
                .whatItIsFor("Keeping a fluid, and handing it to pipes, cables and whatever takes fluids from a"
                        + " block.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Fill it with a bucket or a pipe, and empty it the same way.",
                        "Break it and its item keeps the fluid: place it again and the fluid is back.")
                .whatCanGoWrong("The fluid does not go in.",
                        "It already holds another fluid, or it is full.")
                .register();
    }

    private static void coalGenerator(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("coal_generator", section).titled("Coal Generator").icon(IndustrialModule.COAL_GENERATOR)
                .covers(IndustrialModule.COAL_GENERATOR)
                .scene("The Coal Generator", MODEL)
                .place(0, 0, 0, IndustrialModule.COAL_GENERATOR, FACING_THE_READER, "")
                .balloon(1, FRONT, "The front: open it to put fuel in.")
                .balloon(2, SIDE, "Energy goes out on every side.")
                .note(SET_UP_NEXT)
                .nextColumn()
                .whatItIs("A block that burns fuel to make energy: 20 FE a tick while the fuel burns.")
                .define("FE", "Forge Energy, the energy most machine mods count in. A generator makes it, a cable"
                        + " carries it, a machine spends it.")
                .define("Tick", "The game's beat: 20 ticks make a second.")
                .whatItIsFor("Powering the other machines, at the start, before anything better exists.")
                .howToGetIt(FROM_THE_TAB)
                .nextPage()
                .plan(SET_UP_PLAN)
                .planPart(IndustrialModule.COAL_GENERATOR, GENERATOR)
                .planPart(IndustrialModule.MACERATOR, "Macerator")
                .planOptional("any machine touching it (optional)")
                .howToUseIt("Place the generator, and a machine against it.",
                        "Put fuel in its slot: coal, charcoal, planks, logs.",
                        "One coal burns 1,600 ticks and makes 32,000 FE.",
                        "It holds 16,000 FE and gives up to 1,000 FE twice a second.")
                .nextColumn()
                .whatCanGoWrong("It works but nothing gets energy.",
                        "No machine or cable touches it, or its Sides tab closes that face. Put a machine against a"
                                + " face, or lay Insulated Copper Cable.",
                        "It stopped and its energy is full.",
                        "Nothing takes its energy. While it is full it stops burning, and goes on when there is"
                                + " room.",
                        "A machine works in bursts.",
                        "One generator makes 20 FE a tick; a Macerator spends 40. Put a second generator against"
                                + " it.")
                .register();
    }

    private static void compressor(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("compressor", section).titled("Compressor").icon(IndustrialModule.COMPRESSOR)
                .covers(IndustrialModule.COMPRESSOR)
                .scene("The Compressor", MODEL)
                .place(0, 0, 0, IndustrialModule.COMPRESSOR, FACING_THE_READER, "")
                .balloon(1, FRONT, "The front: the side you open it from.")
                .balloon(2, SIDE, "Power and items go in on any side.")
                .note(SET_UP_NEXT)
                .nextColumn()
                .whatItIs("A machine that presses a metal ingot flat into a plate.")
                .whatItIsFor("Making iron and copper plates from ingots.")
                .howToGetIt(FROM_THE_TAB)
                .subheading("What it makes")
                .recipes(IndustrialModule.COMPRESSING.id().toString())
                .nextPage()
                .plan(SET_UP_PLAN)
                .planPart(IndustrialModule.COAL_GENERATOR, GENERATOR)
                .planPart(IndustrialModule.COMPRESSOR, "Compressor")
                .planOptional(SECOND_GENERATOR)
                .howToUseIt("Place the Compressor.",
                        "Place a Coal Generator so it touches the Compressor, on any side.",
                        "Put coal in the generator: it makes power while the coal burns.",
                        "Open the Compressor and put ingots in the left box.",
                        "Every 6 seconds one plate appears in the right box. Take them out.")
                .nextColumn()
                .whatCanGoWrong("Nothing happens.",
                        "No power: the generator needs coal and must touch the Compressor.",
                        "It stops and starts.",
                        "One generator makes 20 FE (units of power) a tick (a twentieth of a second); the"
                                + " Compressor needs 30. Add a second generator.",
                        "It stopped with ingots left.",
                        "The right box is full. Take the plates out.",
                        "An item just sits there.",
                        "Only iron and copper ingots become plates.")
                .register();
    }

    private static void macerator(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("macerator", section).titled("Macerator").icon(IndustrialModule.MACERATOR)
                .covers(IndustrialModule.MACERATOR)
                .scene("The Macerator", MODEL)
                .place(0, 0, 0, IndustrialModule.MACERATOR, FACING_THE_READER, "")
                .balloon(1, FRONT, "The front: the side you open it from.")
                .balloon(2, SIDE, "Power and items go in on any side.")
                .note(SET_UP_NEXT)
                .nextColumn()
                .whatItIs("A machine that grinds ores and metals into dust.")
                .whatItIsFor("Getting more from each ore: one ore or raw ore becomes two dusts, and each dust smelts"
                        + " into an ingot.")
                .howToGetIt(FROM_THE_TAB)
                .nextPage()
                .plan(SET_UP_PLAN)
                .planPart(IndustrialModule.COAL_GENERATOR, GENERATOR)
                .planPart(IndustrialModule.MACERATOR, "Macerator")
                .planOptional(SECOND_GENERATOR)
                .howToUseIt("Place the Macerator, with a Coal Generator touching it.",
                        "Put coal in the generator.",
                        "Put raw iron or iron ore in the Macerator's left box.",
                        "Every 10 seconds two iron dusts appear in the right box. Smelt them into ingots.")
                .nextColumn()
                .whatCanGoWrong("It works in bursts.",
                        "It spends 40 FE a tick and one generator makes 20. Add a second generator.",
                        "It stopped with ore left.",
                        "The right box is full. Take the dust out.",
                        "An item just sits there.",
                        "Only iron ores, raw iron and iron ingots are ground today.")
                .nextPage()
                .subheading("What it makes")
                .recipes(IndustrialModule.MACERATING.id().toString())
                .note("Each iron dust smelts into an iron ingot, in a furnace or the Electric Furnace.")
                .seeAlso("jsindustrial:dust_ingots_plates", "jsindustrial:electric_furnace")
                .register();
    }

    private static void electricFurnace(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("electric_furnace", section).titled("Electric Furnace").icon(IndustrialModule.ELECTRIC_FURNACE)
                .covers(IndustrialModule.ELECTRIC_FURNACE)
                .scene("The Electric Furnace", MODEL)
                .place(0, 0, 0, IndustrialModule.ELECTRIC_FURNACE, FACING_THE_READER, "")
                .balloon(1, FRONT, "The front: the side you open it from.")
                .balloon(2, SIDE, "Power and items go in on any side.")
                .note(SET_UP_NEXT)
                .nextColumn()
                .whatItIs("A furnace that runs on energy instead of fuel.")
                .whatItIsFor("Smelting anything a furnace smelts, by the same recipes, in 8 seconds an item, a little"
                        + " faster than a furnace, for 30 FE a tick.")
                .howToGetIt(FROM_THE_TAB)
                .nextPage()
                .plan(SET_UP_PLAN)
                .planPart(IndustrialModule.COAL_GENERATOR, GENERATOR)
                .planPart(IndustrialModule.ELECTRIC_FURNACE, "Furnace")
                .planOptional(SECOND_GENERATOR)
                .howToUseIt("Place the Electric Furnace, with a Coal Generator touching it.",
                        "Put coal in the generator.",
                        "Put what to smelt in the furnace's left box; take the result from the right one.")
                .nextColumn()
                .whatCanGoWrong("It stops and starts.",
                        "It spends 30 FE a tick and one generator makes 20. Add a second generator.",
                        "An item just sits there.",
                        "A furnace does not smelt it either.")
                .register();
    }

    private static void power(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("making_and_moving_power", section).titled("Power: making it and moving it")
                .icon(IndustrialModule.COAL_GENERATOR)
                .covers(() -> SharedEnergy.INSULATED_COPPER_CABLE)
                .whatItIs("How energy goes from a generator to the machines: through the faces that touch, or along"
                        + " Insulated Copper Cable.")
                .whatItIsFor("Running machines that cannot stand against a generator, and running many from a few"
                        + " generators.")
                .howToGetIt(FROM_THE_JS_TECH_TAB)
                .define("Buffer", "The energy a machine can store, like a small battery inside it. It keeps a machine"
                        + " working for a while after its power stops.")
                .nextColumn()
                .howToUseIt("Place the generator and the machines.",
                        "Lay Insulated Copper Cable from the generator to each machine, like a line of blocks.",
                        "Add generators until the machines stop working in bursts.")
                .subheading("What each machine spends")
                .paragraph("Coal Generator: makes 20 FE a tick. Macerator: 40. Compressor: 30. Electric Furnace: 30.")
                .nextPage()
                .paragraph("The Insulated Copper Cable is the series' basic cable, J's Core's: it carries up to 500 FE"
                        + " a tick and loses a tenth over a hundred blocks. It is laid in the shared cable block of the"
                        + " series, in a lane of its own, so it runs beside the cables of other mods without joining"
                        + " them.")
                .nextColumn()
                .whatCanGoWrong("A machine on the cable gets nothing.",
                        "The cable does not reach it, or the generator has no fuel.",
                        "The machines work in bursts.",
                        "Together they spend more than the generators make, or more than the cable carries. Add a"
                                + " generator, or a second cable.")
                .seeAlso("jsindustrial:single_line_diagrams")
                .register();
    }

    private static void materials(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dust_ingots_plates", section).titled("Materials: dust, ingots, plates").icon(() -> Items.IRON_INGOT)
                .whatItIs("What the machines make: iron dust, ground from ore; iron and copper plates, pressed from"
                        + " ingots.")
                .whatItIsFor("They are the parts later machines and recipes are made from. They belong to J's Core,"
                        + " so every mod of the series uses the same ones.")
                .howToGetIt("Make them with the machines, or take them from the J's Tech tab.")
                .nextColumn()
                .howToUseIt("Grind raw iron in the Macerator: two dusts.",
                        "Smelt each dust in a furnace or the Electric Furnace: an ingot.",
                        "Press the ingots in the Compressor: plates.")
                .subheading("What smelts into an iron ingot")
                .recipes("minecraft:smelting", () -> Items.IRON_INGOT)
                .whatCanGoWrong("Dust will not go into the Compressor.",
                        "Dust is smelted, not pressed. Smelt it into an ingot first.")
                .register();
    }
}
