/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.guide;

import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.text.Text;
import dev.jstech.industrial.IndustrialModule;
import java.util.function.Supplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The general drawings of the Plant Drawings, the sheets every other is read by: how a drawing is read, and the
 * symbols of each kind of diagram the sections are drawn in, each with a small example the reader can also see as its
 * blocks, or, for the ground, in its own colours.
 */
final class ReadingPages {

    /** How tall the examples are, the sections of the ground, and the scenes of their blocks. */
    private static final int EXAMPLE = 100;
    private static final int SECTION = 110;
    private static final int SCENE = 60;
    /** How near the scenes of blocks are first seen: near enough to tell each machine. */
    private static final float SCENE_ZOOM = 1.6F;
    /** How tall a legend of six symbols is, and one of seven. */
    private static final int LEGEND = 118;
    private static final int LONG_LEGEND = 130;
    /* The faces a wire or a pipe crosses in a cable block, a bit each, and the plug where it meets a device. */
    private static final int NORTH = 1 << 2;
    private static final int SOUTH = 1 << 3;
    private static final int WEST = 1 << 4;
    private static final int EAST = 1 << 5;
    private static final int PLUG = 8;

    private ReadingPages() {
    }

    /** Declares the general drawings in their section, in the order they are read. */
    static void declare(final ModGuide guide, final ModGuide.SectionRef section) {
        reading(guide, section);
        singleLine(guide, section);
        pid(guide, section);
        geology(guide, section);
    }

    private static void reading(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("reading_drawings", section).titled("How to read these drawings").icon(IndustrialGuide.MANUAL)
                .covers(IndustrialGuide.MANUAL)
                .whatItIs("A drawing shows one thing of the plant on one sheet or more, the way engineers draw a"
                        + " plant: a machine as its block, which you turn, with numbers that point at what matters,"
                        + " and the plant's systems in the diagrams of their trades.")
                .whatItIsFor("Reading every other drawing of this set.")
                .howToGetIt("You have it: these drawings come in the Plant Drawings folder, from the creative menu,"
                        + " J's Industrial tab.")
                .subheading("The model")
                .paragraph("Each machine is shown as its block. Drag it to turn it, and turn the mouse wheel to bring"
                        + " it closer. A multiblock has buttons that add or take away a layer.")
                .subheading("The balloons")
                .paragraph("A number in a circle points at a part; the legend under the model says what it is.")
                .nextColumn()
                .subheading("The title block")
                .paragraph("The box in the corner names the set and the drawing, its number, which sheet of how many,"
                        + " and its revision.")
                .subheading("The drawing number")
                .paragraph("JI, the letters of the drawing's section, and its place there: JI-GS-002 is the second"
                        + " drawing of Getting Started.")
                .nextPage()
                .subheading("The switch")
                .paragraph("A drawing that can be shown two ways has a switch over it, DIAGRAM and 3D or LINES and"
                        + " COLOURS. Click a word to see it that way.")
                .subheading("The zones")
                .paragraph("The numbers along the top and the letters down the side cut the sheet into zones, so a"
                        + " place on it can be named: B4.")
                .howToUseIt("Start at the drawing list: its drawings stand under the titles of their sections.",
                        "Click a drawing's number or its title to open it.",
                        "Turn the sheets with the arrows, the arrow keys or the mouse wheel.")
                .nextColumn()
                .whatCanGoWrong("A drawing seems to stop halfway.",
                        "It goes on, on its next sheet: the title block says which sheet of how many.")
                .seeAlso("jsindustrial:single_line_diagrams", "jsindustrial:coal_generator")
                .register();
    }

    private static void singleLine(final ModGuide guide, final ModGuide.SectionRef section) {
        final String symbols = PlantDrawing.drawn()
                .legend("generator", "breaker", "transformer", "bus", "line", "motor").json();
        final PlantDrawing plant = PlantDrawing.drawn()
                .symbol("generator", 75, 12, Text.literal("COAL GEN 1"), Text.literal("20 FE/t")).line(75, 19, 75, 28)
                .symbol("breaker", 75, 31, Text.literal("CB-1"), Text.EMPTY).line(75, 34, 75, 42)
                .bus(5, 42, 146, Text.literal("BUS A"));
        int load = 0;
        for (final String[] machine : new String[][] {{"MACERATOR", "40 FE/t"}, {"COMPRESSOR", "30 FE/t"},
                {"FURNACE", "30 FE/t"}}) {
            final int x = 27 + 48 * load++;
            plant.line(x, 45, x, 54).symbol("breaker", x, 57, Text.EMPTY, Text.EMPTY).line(x, 60, x, 68)
                    .symbolOver("motor", x, 76, Text.literal(machine[0]), Text.literal(machine[1]));
        }
        guide.page("single_line_diagrams", section).titled("Reading single-line diagrams")
                .icon(IndustrialModule.COAL_GENERATOR)
                .drawing(PlantDrawing.SINGLE_LINE, LEGEND, symbols, "The symbols of a single-line diagram")
                .nextColumn()
                .modes("Diagram", "3D")
                .drawing(PlantDrawing.SINGLE_LINE, EXAMPLE, plant.json(), "A small plant")
                .scene("The same plant, as its blocks", SCENE)
                .place(0, 0, 2, IndustrialModule.COAL_GENERATOR, "facing=south", "")
                .place(1, 0, 2, cable(), "", copperCable(WEST | EAST | WEST << PLUG))
                .place(2, 0, 2, cable(), "", copperCable(WEST | EAST))
                .place(3, 0, 0, cable(), "", copperCable(SOUTH | EAST | EAST << PLUG))
                .place(3, 0, 1, cable(), "", copperCable(NORTH | SOUTH))
                .place(3, 0, 2, cable(), "", copperCable(NORTH | SOUTH | WEST | EAST | EAST << PLUG))
                .place(3, 0, 3, cable(), "", copperCable(NORTH | SOUTH))
                .place(3, 0, 4, cable(), "", copperCable(NORTH | EAST | EAST << PLUG))
                .place(4, 0, 0, IndustrialModule.MACERATOR, "facing=south", "")
                .place(4, 0, 2, IndustrialModule.COMPRESSOR, "facing=south", "")
                .place(4, 0, 4, IndustrialModule.ELECTRIC_FURNACE, "facing=south", "")
                .view(-35, 30, SCENE_ZOOM)
                .balloon(1, GuideBlock.ScenePart.of(0, 0, 2), "COAL GEN 1")
                .balloon(2, GuideBlock.ScenePart.of(4, 0, 0), "MACERATOR")
                .balloon(3, GuideBlock.ScenePart.of(4, 0, 2), "COMPRESSOR")
                .balloon(4, GuideBlock.ScenePart.of(4, 0, 4), "FURNACE")
                .nextPage()
                .whatItIs("The drawing of an electric grid: one line stands for every wire of a circuit, and each"
                        + " part on it is a symbol.")
                .whatItIsFor("Seeing how energy goes from the generators to the machines, and where it can be cut.")
                .howToGetIt("These drawings are in Power, and wherever the plant moves energy.")
                .howToUseIt("Follow the line from a generator down to each machine.",
                        "A breaker is where the circuit can be opened: what comes after it stops.",
                        "Click 3D over the example to see the same plant as its blocks.")
                .nextColumn()
                .whatCanGoWrong("A line seems to end in the air.",
                        "It goes on to a symbol on another drawing, which its tag names.")
                .seeAlso("jsindustrial:making_and_moving_power")
                .register();
    }

    private static void pid(final ModGuide guide, final ModGuide.SectionRef section) {
        final String symbols = PlantDrawing.drawn()
                .legend("vessel", "pump", "valve", "check_valve", "instrument", "pipe", "signal").json();
        final String water = PlantDrawing.drawn()
                .vessel(6, 14, 30, 50, Text.literal("TK-101")).line(36, 56, 52, 56)
                .symbol("valve", 58, 56, Text.literal("V-1"), Text.EMPTY).line(64, 56, 80, 56)
                .symbol("pump", 87, 56, Text.literal("P-101"), Text.EMPTY).line(93, 56, 106, 56)
                .symbol("check_valve", 112, 56, Text.EMPTY, Text.EMPTY).flow(118, 56, 146, 56)
                .line(100, 38, 100, 56).symbol("instrument", 100, 30, Text.literal("PI"), Text.literal("101"))
                .symbol("label", 132, 52, PlantDrawingTexts.TO_BOILER.text(), Text.EMPTY)
                .json();
        guide.page("pids", section).titled("Reading P&IDs").icon(IndustrialModule.TANK)
                .drawing(PlantDrawing.PID, LONG_LEGEND, symbols, "The symbols of a P&ID")
                .nextColumn()
                .modes("Diagram", "3D")
                .drawing(PlantDrawing.PID, EXAMPLE, water, "Water to a boiler")
                .scene("The same pipes, as their blocks; the valve and the pump are to come", SCENE)
                .place(0, 0, 0, IndustrialModule.TANK)
                .place(1, 0, 0, cable(), "", copperPipe(WEST | EAST | WEST << PLUG))
                .place(2, 0, 0, standIn())
                .place(3, 0, 0, cable(), "", copperPipe(WEST | EAST))
                .place(4, 0, 0, standIn())
                .place(5, 0, 0, cable(), "", copperPipe(WEST | EAST))
                .view(-35, 30, SCENE_ZOOM)
                .balloon(1, GuideBlock.ScenePart.of(0, 0, 0), "TK-101")
                .balloon(2, GuideBlock.ScenePart.of(2, 0, 0), "V-1")
                .balloon(3, GuideBlock.ScenePart.of(4, 0, 0), "P-101")
                .balloon(4, GuideBlock.ScenePart.of(5, 0, 0), "The pipe")
                .nextPage()
                .whatItIs("A piping and instrumentation diagram, the drawing of a chemical plant: the vessels, the"
                        + " pumps and valves on the pipes between them, and the instruments that read them.")
                .whatItIsFor("Seeing where a fluid goes, what moves it, what can stop it and what measures it.")
                .howToGetIt("These drawings are in Fluids and Gases and in Chemistry.")
                .howToUseIt("Read it along the arrows, from the vessel on.",
                        "A bubble is an instrument: its letters say what it measures, PI a pressure, and its number"
                                + " which one it is.",
                        "Click 3D over the example to see the same pipes as their blocks.")
                .nextColumn()
                .whatCanGoWrong("A block of the drawing is not in the game yet.",
                        "It comes with its part of the mod; until then the 3D view shows a plain block in its place.")
                .seeAlso("jsindustrial:tank")
                .register();
    }

    private static void geology(final ModGuide guide, final ModGuide.SectionRef section) {
        final String rocks = PlantDrawing.drawn().rocks("minecraft:dirt", "soil", "minecraft:sandstone", "sandstone",
                "minecraft:stone", "stone", "minecraft:granite", "granite", "minecraft:deepslate", "deepslate",
                "minecraft:iron_ore", "ore").json();
        guide.page("geological_sections", section).titled("Reading geological sections").icon(() -> Items.IRON_ORE)
                .drawing(PlantDrawing.GEOLOGY, LEGEND, rocks, "The rocks, in their hatching and their colours")
                .nextColumn()
                .modes("Lines", "Colours")
                .drawing(PlantDrawing.GEOLOGY, SECTION, section(false), "Section A-A")
                .drawing(PlantDrawing.GEOLOGY, SECTION, section(true), "Section A-A, in the rocks' colours")
                .nextPage()
                .whatItIs("The ground as if cut through with a knife: its layers of rock from the surface down, the"
                        + " veins of ore in them, the faults where the ground broke and slid, and the boreholes that"
                        + " prove it.")
                .whatItIsFor("Knowing where a vein runs before digging for it.")
                .howToGetIt("These drawings are in The World, where the ores and their veins are.")
                .howToUseIt("Each rock is drawn in its hatching; the legend names it.",
                        "Where a fault crosses the layers, the same layer stands higher on one side than the other,"
                                + " and so does the vein.",
                        "Click COLOURS over the section to see each rock as its block.")
                .nextColumn()
                .whatCanGoWrong("A vein stops at a line.",
                        "That line is a fault: the vein goes on, higher or lower, on its other side.")
                .seeAlso("jsindustrial:reading_drawings")
                .register();
    }

    /* Section A-A: five layers, a fault that lowered its east side, a vein of iron in the stone, two boreholes. */
    private static String section(final boolean colours) {
        final PlantDrawing cut = PlantDrawing.drawn();
        for (final int[] column : new int[][] {{0, 74, 18}, {74, 148, 27}}) {
            cut.column(column[0], column[1], column[2])
                    .layer("minecraft:dirt", "soil", 7).layer("minecraft:sandstone", "sandstone", 18)
                    .layer("minecraft:stone", "stone", 22).layer("minecraft:granite", "granite", 20)
                    .layer("minecraft:deepslate", "deepslate", 106 - column[2] - 67);
        }
        cut.vein("minecraft:iron_ore", 24, 52, 66, 57).vein("minecraft:iron_ore", 80, 61, 132, 66)
                .fault(68, 106, 80, 18)
                .borehole(44, 18, 66, Text.literal("BH-1")).borehole(112, 27, 60, Text.literal("BH-2"));
        if (colours) {
            cut.inColours();
        }
        return cut.json();
    }

    /* The cable block of the series, which holds every wire and pipe; what it holds is in its data. */
    private static Supplier<Block> cable() {
        return () -> CoreCables.BLOCK.get();
    }

    /* A block that stands for a part of the plant the mod does not have yet. */
    private static Supplier<Block> standIn() {
        return () -> Blocks.IRON_BLOCK;
    }

    /* One Insulated Copper Cable in its lane of the cable block, crossing and plugged into the faces given. */
    private static String copperCable(final int faces) {
        return "{Wires:[{cable:\"jscore:insulated_copper_cable\"}],Links:[I;0,0,0,0,0,0,0," + faces + ",0]}";
    }

    /* One Copper Pipe, which fills its cable block alone, crossing and plugged into the faces given. */
    private static String copperPipe(final int faces) {
        return "{Wires:[{cable:\"jscore:copper_pipe\"}],Links:[I;0,0,0,0," + faces + ",0,0,0,0]}";
    }
}
