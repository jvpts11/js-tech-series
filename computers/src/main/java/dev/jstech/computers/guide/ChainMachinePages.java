/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.LithoLink;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Declares the pages of the chain's machines in the building chapter (computers/docs/design/survival.md): how a machine
 * is built of its blocks round the Machine Terminal, each group of machines with what each makes, and the Lithography
 * Stations and the Electronics Assembly Lines. The sizes, steps and draws in the tables are read from the machines
 * themselves, so the pages never say other than the game does.
 */
final class ChainMachinePages {

    /* The page every other page of the section leads back to. */
    private static final String BUILDING = "jsc:building_chain_machines";
    private static final List<String> STATIONS = List.of("lithography_vintage", "lithography_legacy",
            "lithography_arf", "lithography_advanced");
    /* The game's ticks a second, over which a draw in watts is spent as joules a tick. */
    private static final int TICKS = 20;

    private ChainMachinePages() {
    }

    /** Declares the machines' section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("chain_machines").titled("The chain's machines").in(chapter)
                .register();
        building(guide, section);
        oreAndMetals(guide, section);
        chemistry(guide, section);
        silicon(guide, section);
        chipFactory(guide, section);
        lithography(guide, section);
        boards(guide, section);
        assembly(guide, section);
    }

    private static void building(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("building_chain_machines", section).titled("Building a chain machine")
                .icon(ChainMachineBlocks.TERMINAL)
                .coversAll(() -> ChainMachineBlocks.shared().stream().map(block -> (ItemLike) block.get()).toList())
                .paragraph("What a bench makes by hand, one part at a time, the chain's machines make in numbers:"
                        + " they crush and smelt the ore, roll and draw the metal, run the chemistry, grow the silicon,"
                        + " make the chips and press the boards and screens. Each machine is a multiblock you build"
                        + " block by block in the shape that is its own.")
                .paragraph("A machine is built of four kinds of block. Its casing, one of five by the work it stands:"
                        + " the Machine Casing for the mechanical machines, the Refractory Casing for heat, the Lined"
                        + " Casing for what corrodes, the Vacuum Casing for vacuum and gases, and the Clean Room Casing"
                        + " for the Lithography Stations. Its working blocks, which make it what it is: a Crusher's"
                        + " rolls, a Wafer Saw's wire web. Its entries, where things go in and come out. And the"
                        + " Machine Terminal, which runs it.")
                .define("Machine Terminal", "The control cabinet you place at a machine's terminal place: once the"
                        + " machine's whole shape stands round it, it becomes that machine's own terminal and runs it.")
                .paragraph("You tell the entries apart by their connector and their band: green takes in, orange gives"
                        + " out, yellow is energy, and the cyan Peripheral Port links a Lithography Station to its"
                        + " computer. The Item Inlet and Item Outlet take and give items, the Liquid Inlet and Liquid"
                        + " Outlet liquids, the Gas Inlet and Gas Outlet gases, and the Power Entry takes energy.")
                .paragraph("Build every block at its place, with the Machine Terminal at the terminal's, facing the way"
                        + " you want the machine's front. A [Structure Projector](jscore:structure_projector) shows you"
                        + " the shape before you build it. The terminal looks for a shape standing whole round it as"
                        + " you place it, as a block beside it changes and as you use it, and tells you when none"
                        + " stands.")
                .paragraph("Formed, the machine is drawn as one: its casing and its working blocks merge into its"
                        + " model, its entries draw nothing of their own, and its beacon turns while it works. It shows"
                        + " what it is working on, with the colours of what goes in and what comes out.")
                .paragraph("Use the terminal to open the machine's window, with its slots, its tanks and the energy it"
                        + " holds. Things go in through its inlets and come out through its outlets, from and to"
                        + " whatever moves them: a hopper, a pipe, the network's buses or a"
                        + " [Crafting Interface](jsc:crafting_interfaces), which runs it for the network's"
                        + " autocrafting.")
                .paragraph("Every machine but the Coke Oven draws its energy at low voltage through its Power Entry,"
                        + " and only while it works. The Coke Oven burns part of its own coal, as a real one does. On a"
                        + " server set to let its computers run free of energy, the machines run free too.")
                .ifSomethingGoesWrong("The terminal says no machine stands round it.",
                        "A block is missing, out of its place, or another stands where the shape wants its own: hold"
                                + " the shape against a Structure Projector's, from the machine's front.",
                        "The machine stands formed but does nothing.",
                        "No energy reaches its Power Entry, what is in its inlets makes nothing it knows, or its"
                                + " outlets are full.",
                        "Breaking one block took the machine apart.",
                        "Every block of the shape holds it together: put the block back and the terminal forms it"
                                + " again. Breaking the terminal gives you back the Machine Terminal.")
                .seeAlso("jsc:ore_and_metal_machines", "jsc:lithography_stations")
                .register();
    }

    private static void oreAndMetals(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> machines = List.of("crusher", "grinder", "induction_furnace", "electrolytic_cell",
                "rolling_mill", "wire_drawer");
        final ModGuide.EntryBuilder page = guide.page("ore_and_metal_machines", section).titled("Ore and metals")
                .icon(ChainMachineBlocks.byId("crusher_roll"))
                .coversAll(working(machines))
                .paragraph("These machines take the ore you dig and make the metal you build with.")
                .paragraph("The Crusher breaks ore, the block or its raw form, into crushed ore between its toothed"
                        + " rolls, and now and then gives the dust of a rarer metal found with it. The Grinder grinds"
                        + " ingots, crystals and glass into dust. The Induction Furnace melts dusts into ingots and"
                        + " alloys, sand and coke into metallurgical silicon, and coke and calcite into calcium"
                        + " carbide.")
                .paragraph("The Electrolytic Cell splits water into hydrogen and oxygen and salt into chlorine, refines"
                        + " copper and makes aluminium from alumina. The Rolling Mill rolls an ingot into plate, sheet"
                        + " and foil, and the Wire Drawer draws it into rod and wire, the fine bonding wire too.")
                .table("Each machine's size, its step and its draw", "Machine", "Size", "Step", "Draw");
        machineRows(page, machines);
        page.seeAlso(BUILDING, "jsc:chemistry_machines").register();
    }

    private static void chemistry(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> machines = List.of("coke_oven", "chemical_reactor", "air_separation_unit");
        final ModGuide.EntryBuilder page = guide.page("chemistry_machines", section).titled("Chemistry and gases")
                .icon(ChainMachineBlocks.byId("chemical_reactor_vessel"))
                .coversAll(working(machines))
                .paragraph("The chemistry the chain asks for happens in these machines, and you feed them the liquids"
                        + " and gases they work with through their inlets.")
                .paragraph("The Coke Oven bakes coal into coke and coal tar, and needs no energy. The Chemical Reactor"
                        + " is the chain's chemistry: the acids, the resins and the plastics, alumina from aluminium"
                        + " ore, photoresist, cultured quartz and ultrapure water among them, through its two liquid"
                        + " inlets and its two gas inlets. The Air Separation Unit separates air into nitrogen, oxygen,"
                        + " argon and krypton, out of its gas outlets.")
                .table("Each machine's size, its step and its draw", "Machine", "Size", "Step", "Draw");
        machineRows(page, machines);
        page.seeAlso(BUILDING, "jsc:silicon_machines").register();
    }

    private static void silicon(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> machines = List.of("siemens_reactor", "crystal_puller", "wafer_saw");
        final ModGuide.EntryBuilder page = guide.page("silicon_machines", section).titled("Silicon")
                .icon(ChainMachineBlocks.byId("crystal_puller_hot_zone"))
                .coversAll(working(machines))
                .paragraph("From the silicon of sand to the wafers every chip is made on, these machines stand up tall"
                        + " where you have the room.")
                .paragraph("The Siemens Reactor turns metallurgical silicon, with hydrogen chloride and hydrogen, into"
                        + " polysilicon under its bell. The Crystal Puller grows a boule from polysilicon and a"
                        + " dopant, and the crystals of gallium arsenide, aluminium gallium indium phosphide, gallium"
                        + " nitride and calcium fluoride, the nitride with the nitrogen of its gas inlet. The Wafer Saw"
                        + " cuts a boule into wafers, polished flat.")
                .table("Each machine's size, its step and its draw", "Machine", "Size", "Step", "Draw");
        machineRows(page, machines);
        page.seeAlso(BUILDING, "jsc:chip_factory_machines").register();
    }

    private static void chipFactory(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> machines = List.of("plasma_etcher", "ion_implanter", "deposition_chamber",
                "packaging_line");
        final ModGuide.EntryBuilder page = guide.page("chip_factory_machines", section).titled("The chip factory")
                .icon(ChainMachineBlocks.byId("ion_implanter_analyzer_magnet"))
                .coversAll(working(machines))
                .paragraph("A chip is made layer by layer on its wafer: a Lithography Station exposes each layer"
                        + " ([](jsc:lithography_stations)), and you take the wafer through these machines for the"
                        + " rest.")
                .paragraph("The Plasma Etcher etches the exposed layer, with a liquid and two gases. The Ion Implanter"
                        + " dopes the layer. The Deposition Chamber lays metal on it, and the magnetic layer of"
                        + " platters and floppy film, the chromium of a mask blank and the gold electrodes of"
                        + " oscillator crystals. The Packaging Line cuts a finished wafer into its dies and packages"
                        + " each: in ceramic in the Vintage, in epoxy with pins later, on solder balls from the"
                        + " Transition.")
                .table("Each machine's size, its step and its draw", "Machine", "Size", "Step", "Draw");
        machineRows(page, machines);
        page.seeAlso(BUILDING, "jsc:lithography_stations").register();
    }

    private static void lithography(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("lithography_stations", section)
                .titled("The Lithography Stations")
                .icon(ChainMachineBlocks.byId("lithography_wafer_stage"))
                .coversAll(working(STATIONS))
                .paragraph("A Lithography Station exposes a wafer's layer through its mask. Each era has its own, the"
                        + " same body lit its era's way: the Vintage's mercury lamp, the Legacy's krypton fluoride"
                        + " laser, the argon fluoride laser with immersion of the ArF Lithography Station, which works"
                        + " both the Transition's recipes and the Standard's, and the Advanced's extreme ultraviolet"
                        + " source with its mirrors. You give a laser its gas through the Gas Inlet, and every station"
                        + " its liquid through the Liquid Inlet.")
                .paragraph("From the Legacy on, a station works only linked to a computer: lay a"
                        + " [peripheral cable](jsc:peripheral_cables) from its Peripheral Port to a computer of the era"
                        + " before the recipe's, or of a later one, switched on and running. The computer runs the"
                        + " exposure as its litho process and holds memory for it while it does, which you see among"
                        + " its processes. A computer two eras newer than the one asked for exposes in half the time.")
                .table("What the recipes of each era ask for", "Recipes of", "Computer", "Memory", "Draw");
        for (final String id : STATIONS) {
            final ChainMachine station = machine(id);
            for (final ChainMachines.Worked worked : ChainMachines.workedBy(station)) {
                final Era era = worked.era();
                page.row(GuideCell.words(english(era)),
                        station.linked() ? GuideCell.words(english(before(era)) + " or later")
                                : GuideCell.words("none"),
                        station.linked() ? GuideCell.amount(LithoLink.memoryMb(era), "MB") : GuideCell.words("none"),
                        GuideCell.amount((long) worked.joulesATick() * TICKS, "W"));
            }
        }
        page.table("Each station's size and its step", "Station", "Size", "Step");
        for (final String id : STATIONS) {
            final ChainMachine station = machine(id);
            page.row(GuideCell.words(station.english()), GuideCell.words(size(station)),
                    GuideCell.amount(station.seconds(), "s"));
        }
        page.ifSomethingGoesWrong("The station stands formed but exposes nothing.",
                        "Its computer is off, of an older era than the recipe asks for, not linked to its Peripheral"
                                + " Port, or short of the memory the exposure holds.")
                .seeAlso(BUILDING, "jsc:chip_factory_machines")
                .register();
    }

    private static void boards(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> machines = List.of("laminator", "board_etcher", "ceramic_kiln", "coil_winder",
                "optical_polisher", "display_line");
        final ModGuide.EntryBuilder page = guide.page("board_machines", section)
                .titled("Boards, parts, glass and screens")
                .icon(ChainMachineBlocks.byId("laminator_hot_platen"))
                .coversAll(working(machines))
                .paragraph("What goes round the chips is made here: you get the boards they sit on, the parts beside"
                        + " them, the glass and the screens.")
                .paragraph("The Laminator presses paper or glass fibre with resin, clad in copper foil, and the Board"
                        + " Etcher etches the clad laminate into a board's traces with its ferric chloride. The Ceramic"
                        + " Kiln fires alumina packages, ceramic capacitors and ferrites, its door shut while it fires."
                        + " The Coil Winder winds wire into coils, motors and magnetic heads.")
                .paragraph("The Optical Polisher polishes quartz into mask plates, glass and calcium fluoride into"
                        + " lenses and polycarbonate into blank discs, and draws glass fibre and optical fibre from its"
                        + " tower. The Display Line makes the screens: tubes with their phosphor, flat screens with"
                        + " indium tin oxide, and LED panels, taking liquid crystal and mercury through its two liquid"
                        + " inlets.")
                .table("Each machine's size, its step and its draw", "Machine", "Size", "Step", "Draw");
        machineRows(page, machines);
        page.seeAlso(BUILDING, "jsc:electronics_assembly_lines").register();
    }

    private static void assembly(final ModGuide guide, final ModGuide.SectionRef section) {
        final List<String> lines = List.of("assembly_vintage", "assembly_legacy", "assembly_transition",
                "assembly_standard", "assembly_advanced");
        final ModGuide.EntryBuilder page = guide.page("electronics_assembly_lines", section)
                .titled("The Electronics Assembly Lines")
                .icon(ChainMachineBlocks.byId("assembly_vintage_placement_head"))
                .coversAll(working(lines))
                .paragraph("An Electronics Assembly Line assembles on its own what you assemble by hand at a bench"
                        + " ([](jsc:electronics_workbench)): the parts of its era and of every era before it, by the"
                        + " same recipes, in the same time and at its era's bench's draw.")
                .paragraph("Each era's line has the machines of its time: axial insertion and wave soldering in the"
                        + " Vintage, DIP insertion and wave soldering in the Legacy, a chip shooter and reflow in the"
                        + " Transition, a gantry and optical inspection in the Standard, two gantries and X-ray"
                        + " inspection in the Advanced. The Feeder Bank and the Wave Solder are the same for the two"
                        + " eras that have them.")
                .paragraph("Lay the ingredients in through its Item Inlet, in any order, and take the parts from its"
                        + " Item Outlet. With a Crafting Interface against its inlet, the network's autocrafting"
                        + " assembles your parts while you do something else.")
                .table("Each line's size, its step and its draw", "Line", "Size", "Step", "Draw");
        machineRows(page, lines);
        page.seeAlso(BUILDING, "jsc:electronics_workbench").register();
    }

    /* A row for each of {@code machines}: its name, its size wide by tall by deep, its step and its draw. */
    private static void machineRows(final ModGuide.EntryBuilder page, final List<String> machines) {
        for (final String id : machines) {
            final ChainMachine machine = machine(id);
            page.row(GuideCell.words(machine.english()), GuideCell.words(size(machine)),
                    machine.seconds() > 0 ? GuideCell.amount(machine.seconds(), "s")
                            : GuideCell.words("the recipe's"),
                    GuideCell.amount(machine.watts(), "W"));
        }
    }

    /* The working blocks of {@code machines}, each once, in the order the machines' shapes name them. */
    private static Supplier<List<ItemLike>> working(final List<String> machines) {
        return () -> {
            final Set<String> shared = new LinkedHashSet<>();
            for (final BlockEntry<? extends Block> block : ChainMachineBlocks.shared()) {
                shared.add(block.getId().getPath());
            }
            final Set<String> blocks = new LinkedHashSet<>();
            for (final String id : machines) {
                for (final String block : machine(id).legend().values()) {
                    if (!shared.contains(block)) {
                        blocks.add(block);
                    }
                }
            }
            return blocks.stream().map(block -> (ItemLike) ChainMachineBlocks.byId(block).get()).toList();
        };
    }

    private static ChainMachine machine(final String id) {
        return ChainMachines.byId(id).orElseThrow(() -> new IllegalArgumentException("no chain machine " + id));
    }

    /* How big a machine stands, wide by tall by deep: "3 × 2 × 2". */
    private static String size(final ChainMachine machine) {
        return machine.layers().getFirst().getFirst().length() + " × " + machine.layers().size() + " × "
                + machine.layers().getFirst().size();
    }

    /* The era before {@code era}, whose computers a station of {@code era} asks for. */
    private static Era before(final Era era) {
        return HardwareEra.SERIES.get(HardwareEra.SERIES.indexOf(era) - 1);
    }

    /* The English name of an era: "Vintage". */
    private static String english(final Era era) {
        final String path = era.path();
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
}
