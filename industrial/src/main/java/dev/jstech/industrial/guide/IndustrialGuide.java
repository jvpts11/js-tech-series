/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import static dev.jstech.industrial.guide.IndustrialGuideTexts.ANY_SIDE;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_TAB;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.GENERATOR;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SECOND_GENERATOR;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SET_UP_NEXT;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.SET_UP_PLAN;
import static dev.jstech.industrial.guide.IndustrialGuideTexts.THE_FRONT;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.industrial.IndustrialModule;
import net.minecraft.world.item.Items;

/**
 * J's Industrial in the manuals: its chapter, written once and shown both in the series' Technical Reference and in
 * the mod's own Plant Drawings, a folder of blueprints in which every entry is a drawing of one sheet or more. The
 * machines are drawn from three sides, traced from their own blocks, with numbered balloons, and set up in plans seen
 * from above. Every number is the machines' own.
 */
public final class IndustrialGuide {

    /** The mod's own manual, and the style it is drawn in. */
    public static final String PLANT_DRAWINGS = "jsindustrial:plant_drawings";
    public static final String DRAWINGS_STYLE = "jsindustrial:drawings";

    public static final ItemEntry<ManualItem> MANUAL = IndustrialModule.CONTENT.item("plant_drawings",
            properties -> new ManualItem(properties, PLANT_DRAWINGS)).named("Plant Drawings")
            .tab(IndustrialModule.MANUALS).register();


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
                .about("New to machines? Start with JI-001, then JI-101.")
                .register();
        guide.chapter().titled("J's Industrial").order(30).tab("#FFF0B23A")
                .about("Machines that turn ores and metals into the parts everything else is made from, and the"
                        + " energy they run on.")
                .register();
        final ModGuide.SectionRef reading = guide.section("reading").titled("Reading the drawings")
                .icon(MANUAL).register();
        final ModGuide.SectionRef machines = guide.section("machines").titled("Machines")
                .icon(IndustrialModule.MACERATOR).register();
        final ModGuide.SectionRef power = guide.section("power").titled("Power")
                .icon(IndustrialModule.COAL_GENERATOR).register();
        final ModGuide.SectionRef materials = guide.section("materials").titled("Materials")
                .icon(() -> Items.IRON_INGOT).register();
        final ModGuide.SectionRef storage = guide.section("storage").titled("Storage")
                .icon(IndustrialModule.TANK).register();
        reading(guide, reading);
        coalGenerator(guide, machines);
        compressor(guide, machines);
        macerator(guide, machines);
        electricFurnace(guide, machines);
        power(guide, power);
        materials(guide, materials);
        tank(guide, storage);
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

    private static void reading(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("reading_drawings", section).titled("How to read these drawings").icon(MANUAL).covers(MANUAL)
                .whatItIs("A drawing shows one machine on one sheet or more, the way engineers draw a plant: from"
                        + " three sides, with numbers that point at what matters.")
                .whatItIsFor("Reading every other drawing of this set.")
                .howToGetIt("You have it: these drawings come in the Plant Drawings folder, from the creative menu,"
                        + " J's Industrial tab.")
                .subheading("The views")
                .paragraph("Each machine is drawn from above, from the front and from the side, traced in line from"
                        + " the block itself.")
                .nextColumn()
                .subheading("The dimension")
                .paragraph("The line with arrows under the front view shows how wide the drawing is: one block.")
                .subheading("The balloons")
                .paragraph("A number in a circle points at a part; the legend under the views says what it is.")
                .subheading("The title block")
                .paragraph("The box in the corner names the set and the drawing, its number, which sheet of how many,"
                        + " and its revision.")
                .nextPage()
                .subheading("The zones")
                .paragraph("The numbers along the top and the letters down the side cut the sheet into zones, so a"
                        + " place on it can be named: B4.")
                .howToUseIt("Start at the drawing list: each row is a drawing and how many sheets it has.",
                        "Click a drawing's number or its title to open it.",
                        "Turn the sheets with the arrows, the arrow keys or the mouse wheel.")
                .nextColumn()
                .whatCanGoWrong("A drawing seems to stop halfway.",
                        "It goes on, on its next sheet: the title block says which sheet of how many.")
                .seeAlso("jsindustrial:coal_generator")
                .register();
    }

    private static void coalGenerator(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("coal_generator", section).titled("Coal Generator").icon(IndustrialModule.COAL_GENERATOR)
                .covers(IndustrialModule.COAL_GENERATOR)
                .views(IndustrialModule.COAL_GENERATOR)
                .callout(1, GuideBlock.View.FRONT, 8, 8, "The front: open it to put fuel in.")
                .callout(2, GuideBlock.View.SIDE, 8, 8, "Energy goes out on every side.")
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
                        "It holds 16,000 FE and gives up to 1,000 FE a tick.")
                .nextColumn()
                .whatCanGoWrong("The flame is lit but nothing gets energy.",
                        "No machine or cable touches it. Put a machine against a face, or lay Energy Cable.",
                        "It burns fuel but its bar stays full.",
                        "Nothing takes its energy. While it is full, the fuel burning goes on and its energy is"
                                + " lost.",
                        "A machine works in bursts.",
                        "One generator makes 20 FE a tick; a Macerator spends 40. Put a second generator against"
                                + " it.")
                .register();
    }

    private static void compressor(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("compressor", section).titled("Compressor").icon(IndustrialModule.COMPRESSOR)
                .covers(IndustrialModule.COMPRESSOR)
                .views(IndustrialModule.COMPRESSOR)
                .callout(1, GuideBlock.View.FRONT, 10, 7, THE_FRONT)
                .callout(2, GuideBlock.View.SIDE, 12, 5, ANY_SIDE)
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
                .views(IndustrialModule.MACERATOR)
                .callout(1, GuideBlock.View.FRONT, 8, 8, THE_FRONT)
                .callout(2, GuideBlock.View.SIDE, 8, 8, ANY_SIDE)
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
                .views(IndustrialModule.ELECTRIC_FURNACE)
                .callout(1, GuideBlock.View.FRONT, 8, 8, THE_FRONT)
                .callout(2, GuideBlock.View.SIDE, 8, 8, ANY_SIDE)
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
                .covers(() -> IndustrialModule.ENERGY_CABLE)
                .whatItIs("How energy goes from a generator to the machines: through the faces that touch, or along"
                        + " Energy Cable.")
                .whatItIsFor("Running machines that cannot stand against a generator, and running many from a few"
                        + " generators.")
                .howToGetIt(FROM_THE_TAB)
                .define("Buffer", "The energy a machine can store, like a small battery inside it. It keeps a machine"
                        + " working for a while after its power stops.")
                .nextColumn()
                .howToUseIt("Place the generator and the machines.",
                        "Lay Energy Cable from the generator to each machine, like a line of blocks.",
                        "Add generators until the machines stop working in bursts.")
                .subheading("What each machine spends")
                .paragraph("Coal Generator: makes 20 FE a tick. Macerator: 40. Compressor: 30. Electric Furnace: 30.")
                .nextPage()
                .paragraph("The Energy Cable carries any amount of energy, any distance, and loses none. It is laid"
                        + " in the shared cable block of the series, in a lane of its own, so it runs beside the"
                        + " cables of other mods without joining them.")
                .nextColumn()
                .whatCanGoWrong("A machine on the cable gets nothing.",
                        "The cable does not reach it, or the generator has no fuel.",
                        "The machines work in bursts.",
                        "Together they spend more than the generators make. Add a generator.")
                .register();
    }

    private static void materials(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dust_ingots_plates", section).titled("Materials: dust, ingots, plates").icon(() -> Items.IRON_INGOT)
                .whatItIs("What the machines make: iron dust, ground from ore; iron and copper plates, pressed from"
                        + " ingots.")
                .whatItIsFor("They are the parts later machines and recipes are made from. They belong to J's Core,"
                        + " so every mod of the series uses the same ones.")
                .howToGetIt("Make them with the machines, or take them from the J's Industrial tab.")
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
