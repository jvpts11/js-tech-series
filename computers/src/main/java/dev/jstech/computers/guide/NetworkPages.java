/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import static dev.jstech.computers.guide.ComputersGuide.blocksOf;
import static dev.jstech.computers.guide.ComputersGuide.cablesOn;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.RepeaterBlock;
import dev.jstech.computers.block.RouterBlock;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Declares the pages of the network chapter of the Guide to Operations: what a network is, its members and cables,
 * running it (identity, failover, the Network Manager, the Mirror, services), reaching other machines, and MC-NET.
 */
final class NetworkPages {

    /*
     * What a scene's Mainframe tells a renderer it is doing: running, its build valid and joined to a network, which
     * lights its annunciator lamps. The saved parts of a machine are left out since the closed cabinet shows none.
     */
    private static final String MAINFRAME_UP = "{VisualFlags:7}";
    /* The wire lanes of the two data lines a scene draws: the access line top left, the backbone top middle. */
    private static final int ACCESS_LANE = 0;
    private static final int BACKBONE_LANE = 1;
    /* The faces of a cable block as its links count them, and the bit that marks a device plugged in on a face. */
    private static final int NORTH = 1 << 2;
    private static final int SOUTH = 1 << 3;
    private static final int WEST = 1 << 4;
    private static final int EAST = 1 << 5;
    private static final int PLUGGED = 8;

    private NetworkPages() {
    }

    /** Declares the sections and entries of the network chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        networkBasics(guide, chapter);
        networkOperation(guide, chapter);
        networkRemote(guide, chapter);
        mcNetTerminal(guide, chapter);
    }

    private static void networkBasics(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("network_basics").titled("What a network is")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        guide.page("what_a_network_is", section).titled("What a network is here").icon(ComputingModule.MAINFRAME)
                .paragraph("J's Computers has no channels and no subnetworks at all. A network here works much like"
                        + " a real computer network.")
                .paragraph("A network is every computer and device joined by data cable to one Mainframe, its brain"
                        + " ([](jsc:mainframes)). The cables come in lines, by the job they do: the access line joins"
                        + " the small computers to a router, the backbone joins the routers, the Mainframe and the"
                        + " racks, the long distance line joins two networks, gateway to gateway, and the high compute"
                        + " line joins a supercomputer's nodes ([](jsc:data_cables)). You lay the line the job asks"
                        + " for, and you never count what a cable holds.")
                .paragraph("What a cable does have is a speed and a range: how many items per tick an Operation"
                        + " crossing it can move, and how many cables a run can be before a router or a repeater renews"
                        + " it. So a network built with the right cables in the right places is a fast network.")
                .paragraph("And what does the network do for you? It keeps your items as data on its servers' disks"
                        + " ([](jsc:items_as_data)), gives them back when you ask, and crafts what you need"
                        + " ([](jsc:how_autocrafting_works)). Every one of those is an"
                        + " Operation ([](jsc:what_operations_are)).")
                .define("Network", "Every computer and device joined by data cable to one Mainframe, which runs"
                        + " the network.")
                .define("Data cable", "A cable that carries the data of a network, in one of the lines of the"
                        + " data cables.")
                .define("Router", "A block with six faces that joins the access line of an era to its backbone, and"
                        + " extends the network it is part of.")
                .define("Repeater", "A block laid in a run of cable that renews the run's range, and carries each"
                        + " line on.")
                .define("Speed", "The most items per tick that an Operation crossing a cable can move.")
                .define("Range", "How many cables a run can be before a router or a repeater renews it. A run longer"
                        + " than its range carries nothing.")
                .register();
        guide.page("network_members", section).titled("Members, and how the network sees them")
                .icon(ComputingModule.SERVER_RACK)
                .paragraph("Not every block that touches a cable is a member of the network. The network sorts every"
                        + " block it can see into one of three categories, by what it may do with that block.")
                .paragraph("A block of Category A carries the network but is not a member of it, the way a cable is."
                        + " A block of Category B is one the network can read for its state, but never give work to."
                        + " A block of Category C is where the work happens: only these receive work from the"
                        + " Mainframe.")
                .table("The three categories", "Category", "Has an identity", "Gets Operations", "Examples")
                .row(GuideCell.words("A"), GuideCell.words("No"), GuideCell.words("No"),
                        GuideCell.words("Cables, the Server Rack"))
                .row(GuideCell.words("B"), GuideCell.words("Yes"), GuideCell.words("No"),
                        GuideCell.words("none"))
                .row(GuideCell.words("C"), GuideCell.words("Yes"), GuideCell.words("Yes"),
                        GuideCell.words("Mainframe, Server, Subframe, Personal Computer, Crafting Computer"))
                .paragraph("The Server Rack is a passive container. It holds the servers, and the servers are the"
                        + " members: each one has its own network identity and receives work ([](jsc:server_racks)).")
                .paragraph("A member is a block with a network identity, so a block of Category B or C. The network"
                        + " can name it, read it and, for Category C, send it work. You do not need to remember the"
                        + " categories to build a network; the table tells you what each block does.")
                .define("Network category", "The network's rule for a block: Category A carries the network,"
                        + " Category B is read by it, and Category C carries out its Operations.")
                .define("Network identity", "The identity of a network, which tells it apart from every other"
                        + " network. A Mainframe that is wired into a segment adopts the network on it.")
                .ifSomethingGoesWrong("A computer is not on the network.",
                        "The net command says \"not on a network\". Its cable does not reach the Mainframe. Follow the"
                                + " cables from it to the Mainframe, and check that no run is longer than its range"
                                + " ([](jsc:data_cables)).")
                .register();
        guide.page("first_network", section).titled("Your first network").icon(ComputingModule.MAINFRAME)
                .shows(ComputingModule.MAINFRAME, ComputingModule.SERVER_RACK, ComputingModule.SERVER,
                        ComputingModule.STANDARD_ROUTER, () -> ComputingModule.FIBRE_CABLE,
                        () -> ComputingModule.GIGABIT_CABLE, ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Here is the smallest network that does something useful.")
                .scene("A Mainframe, a router, data cables and a Personal Computer: the smallest network. Drag it to"
                        + " see the back.", 120)
                .place(1, 0, 1, ComputingModule.MAINFRAME, "facing=south,optical=true", MAINFRAME_UP)
                .place(3, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, WEST))
                .place(4, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, EAST))
                .place(5, 0, 1, ComputingModule.STANDARD_ROUTER)
                .place(6, 0, 1, CoreCables.BLOCK, "",
                        wire("gigabit_cable", ACCESS_LANE, WEST | SOUTH, WEST | SOUTH))
                .place(6, 0, 2, ComputingModule.PERSONAL_COMPUTER, "facing=south", "")
                .balloon(1, GuideBlock.ScenePart.of(1, 0, 1), "The Mainframe",
                        "It runs the network. Join the backbone cable to any side of it.")
                .balloon(2, GuideBlock.ScenePart.of(3, 0, 1), "The backbone cable",
                        "Run it from the router to the Mainframe.")
                .balloon(3, GuideBlock.ScenePart.of(5, 0, 1), "The router",
                        "Join the access cable and the backbone cable to it.")
                .balloon(4, GuideBlock.ScenePart.of(6, 0, 1), "The access cable",
                        "Run it from the computer to the router.")
                .balloon(5, GuideBlock.ScenePart.of(6, 0, 2), "The Personal Computer",
                        "Plug the access cable into its back.")
                .steps("Build a Mainframe and give it its parts: an MTX motherboard, a power supply, processors,"
                                + " memory, a disk and a graphics card ([](jsc:mainframes)).",
                        "Install a system on the Mainframe, such as MC-NET, as on any computer: a monitor on its"
                                + " graphics card, and the install medium in a drive joined to it ([](jsc:installing))."
                                + " A Mainframe without a system runs nothing.",
                        "Build a Server Rack and put servers with disks in its bays ([](jsc:server_racks)).",
                        "Join the Mainframe and the rack with the backbone cable of their era: HBW in the Legacy,"
                                + " Fibre Optic in the Standard. Fibre goes into the Mainframe only while it holds an"
                                + " Optical Network Card ([](jsc:expansion_cards)), and into the rack only through a"
                                + " Fibre Optic Interface mounted in it ([](jsc:rack_equipment)).",
                        "Place a router, and join your Personal Computer to it with the access cable, Ethernet or"
                                + " Gigabit ([](jsc:routers)). Run the backbone from the router to the Mainframe.",
                        "At the computer, open the Network program ([](jsc:network_interactor)). Put items in, and"
                                + " they are stored on the servers' disks; ask for them back, and they come out.")
                .paragraph("The Mainframe joins every data cable that touches any side of its footprint, so a backbone"
                        + " can meet it on whichever side is nearest. Fibre is the one cable it takes only with the"
                        + " card.")
                .paragraph("That is it. From here everything else is more of the same: more racks for more storage,"
                        + " more computers on the access line, a Crafting Computer for autocrafting.")
                .ifSomethingGoesWrong("A computer is not on the network.",
                        "Its cable is longer than its range, or of another era than the router. Add a repeater, or"
                                + " a router of the newer era.",
                        "The Network program says \"○ Mainframe offline\".",
                        "No Mainframe is on the computer's network. Follow the cables from the computer to the"
                                + " Mainframe ([](jsc:data_cables)).",
                        "Nothing goes into storage, and every Operation stays queued.",
                        "The Mainframe has no system, so it holds every Operation until one is installed"
                                + " ([](jsc:installing)).",
                        "The fibre does not join the Mainframe or the rack.",
                        "Put an Optical Network Card in the Mainframe's card slots ([](jsc:expansion_cards)), and"
                                + " mount a Fibre Optic Interface in the rack ([](jsc:rack_equipment)).")
                .register();
        guide.page("data_cables", section).titled("Data cables").icon(() -> ComputingModule.ETHERNET_CABLE)
                .coversAll(cablesOn(GridKind.DATA, ComputingModule.CRAFTING_CABLE))
                .paragraph("The data cables are the network itself. They come in lines by the job they do, and each"
                        + " line has one cable for every era it exists in:")
                .table("The lines")
                .property("Access", "Small computers to a router")
                .property("Backbone", "Routers, the Mainframe and the racks")
                .property("Long distance", "Two networks, gateway to gateway")
                .property("High compute", "A supercomputer's nodes to its HBW Interface")
                .property("Crafting", "A Crafting Computer to the parts that feed its machines")
                .table("The cable of each line, by era", "Era", "Access", "Backbone", "Long distance",
                        "High compute")
                .row(GuideCell.words("Vintage"), GuideCell.words("Thin Coaxial Cable"),
                        GuideCell.words("Thick Coaxial Cable"), GuideCell.words("Telephone Line"),
                        GuideCell.words("none"))
                .row(GuideCell.words("Legacy"), GuideCell.words("Ethernet Cable"),
                        GuideCell.words("HBW Cable"), GuideCell.words("Leased Line"), GuideCell.words("none"))
                .row(GuideCell.words("Transition"), GuideCell.words("Cat 5e Cable"),
                        GuideCell.words("10GBASE-CX4 Cable"), GuideCell.words("T3 Line"),
                        GuideCell.words("InfiniBand Cable"))
                .row(GuideCell.words("Standard"), GuideCell.words("Gigabit Ethernet Cable"),
                        GuideCell.words("Fibre Optic Cable"), GuideCell.words("VLDC Cable"),
                        GuideCell.words("High Compute Cable"))
                .row(GuideCell.words("Advanced"), GuideCell.words("Cat 6a Cable"),
                        GuideCell.words("OM5 Fibre Cable"), GuideCell.words("Dark Fibre Cable"),
                        GuideCell.words("OSFP Cable"))
                .paragraph("The crafting line has one cable for all eras, the Crafting Cable. It is the only line"
                        + " that is not split by era.")
                .tableFrom(CableFacts.LINE_REACH, "How far a run of each line reaches, in cables", "Line",
                        "Vintage", "Legacy", "Transition", "Standard", "Advanced")
                .tableFrom(CableFacts.LINE_SPEED, "How many items a tick each line moves", "Line", "Vintage",
                        "Legacy", "Transition", "Standard", "Advanced")
                .paragraph("Every cable has a speed and a range, and both count. A run longer than its range carries"
                        + " nothing, so whatever lies only beyond it is off the network. A cable's tooltip says how far"
                        + " one run of it reaches, and what starts a new run.")
                .paragraph("When you lay a run, count it against the range of its cable, and count the repeaters and"
                        + " routers you put in it. Those are the places where the range starts again.")
                .paragraph("Two rules govern two of the lines. From the Standard on, the backbone's fibre runs only"
                        + " straight, so it turns at an optical router ([](jsc:routers)). And the long distance line"
                        + " runs between exactly two ends and holds its block alone.")
                .paragraph("Fibre also goes into a Mainframe or a Cluster Management Computer only while that machine"
                        + " holds an Optical Network Card ([](jsc:expansion_cards)), and into a Server Rack only"
                        + " through a Fibre Optic Interface mounted in it ([](jsc:rack_equipment)).")
                .ifSomethingGoesWrong("Fibre will not turn a corner.",
                        "From the Standard on, fibre runs only straight. Turn it at an optical router.",
                        "A run carries nothing beyond its range.",
                        "The run is longer than its range. Add a repeater or a router inside it, so that the cable"
                                + " after it starts counting again ([](jsc:routers)).")
                .register();
        guide.page("data_cable_colours", section).titled("Colours and lanes of the cables")
                .icon(() -> ComputingModule.GIGABIT_CABLE)
                .paragraph("Every block of cable holds up to nine wires. Each wire sits in one of nine"
                        + " lanes, laid out three across and three up, so one block can carry several lines at once.")
                .paragraph("Each line has its own lane, so a data cable, a peripheral cable and a crafting cable can"
                        + " share one block without joining.")
                .table("The lane of each line", "Line", "Lane")
                .row(GuideCell.words("Access"), GuideCell.words("Top left"))
                .row(GuideCell.words("Backbone"), GuideCell.words("Top middle"))
                .row(GuideCell.words("High compute"), GuideCell.words("Middle"))
                .row(GuideCell.words("Long distance"), GuideCell.words("Middle, alone in its block"))
                .row(GuideCell.words("Crafting"), GuideCell.words("Right"))
                .row(GuideCell.words("Peripheral"), GuideCell.words("Middle left"))
                .paragraph("You can dye a wire. Use a dye on the wire you are looking at, and that wire takes its"
                        + " colour. The sixteen colours of the game are all there.")
                .paragraph("Two wires of one line and era join when their colours agree: the same colour, or either"
                        + " of them with no colour. So an uncoloured wire joins every colour, and two wires of"
                        + " different colours stop at each other. Wires of different lines or eras never join,"
                        + " whatever their colour.")
                .define("Lane", "The place a wire takes inside a cable block. The lanes let several lines share one"
                        + " block.")
                .ifSomethingGoesWrong("Two cables side by side do not join.",
                        "They are of different lines or eras, or they are dyed in colours that do not agree. Check"
                                + " both: dye one to match the other, or leave both without dye.")
                .register();
        guide.page("routers", section).titled("Routers and repeaters").icon(ComputingModule.STANDARD_ROUTER)
                .coversAll(blocksOf(RouterBlock.class, RepeaterBlock.class))
                .paragraph("A router is where the lines of a network meet. Each era has its own, and a router joins"
                        + " its era's access line to its backbone: your small computers on one side, the Mainframe"
                        + " and the racks on the other. It takes the cables of its own era and of every earlier one,"
                        + " on any of its six faces, and all of them become one network. A router extends a network;"
                        + " it never splits one. When your network must reach further, you add a router.")
                .table("The routers", "Router", "Era", "What it takes")
                .row(GuideCell.words("Vintage Router"), GuideCell.words("Vintage"),
                        GuideCell.words("The Vintage cables"))
                .row(GuideCell.words("Personal Router"), GuideCell.words("Legacy"),
                        GuideCell.words("The Legacy cables, and the Vintage ones"))
                .row(GuideCell.words("Transition Router"), GuideCell.words("Transition"),
                        GuideCell.words("The Transition cables, and the older ones"))
                .row(GuideCell.words("Standard Router"), GuideCell.words("Standard"),
                        GuideCell.words("The Standard cables, and the older ones"))
                .row(GuideCell.words("Advanced Router"), GuideCell.words("Advanced"),
                        GuideCell.words("The Advanced cables, and the older ones"))
                .row(GuideCell.words("Optical Router"), GuideCell.words("Standard"),
                        GuideCell.words("The backbone's fibre only"))
                .row(GuideCell.words("Advanced Optical Router"), GuideCell.words("Advanced"),
                        GuideCell.words("The backbone's fibre only"))
                .paragraph("Eras matter here. A port takes its own era's cable and every older one, never a newer"
                        + " one. Two eras of one line that touch do not join: they meet at a router of the newer era."
                        + " So an old machine on a new network needs a router between them.")
                .paragraph("From the Standard on, the backbone's fibre runs only straight. An optical router takes only"
                        + " the backbone's fibre, from the Standard's era up to its own, and it is the block that turns"
                        + " and branches fibre.")
                .paragraph("A repeater renews a cable's range. Lay it in a run of cable, and the cable after it starts"
                        + " counting its range again, so the run can go on past its first range. A repeater takes every"
                        + " line of its era and of every earlier one, except the long distance line. It carries each"
                        + " line on its own and never joins one line to another; that is a router's work.")
                .scene("Routers, a repeater and optical fibre carrying a network far. The fibre runs straight, so the"
                        + " Optical Router turns it. Drag it to see it from the other side.", 120)
                .place(0, 0, 0, ComputingModule.STANDARD_ROUTER)
                .place(1, 0, 0, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, WEST))
                .place(2, 0, 0, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, EAST))
                .place(3, 0, 0, ComputingModule.STANDARD_REPEATER)
                .place(4, 0, 0, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, WEST))
                .place(5, 0, 0, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, EAST))
                .place(6, 0, 0, ComputingModule.OPTICAL_ROUTER)
                .place(6, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, NORTH | SOUTH, NORTH))
                .place(6, 0, 2, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, NORTH | SOUTH, SOUTH))
                .place(6, 0, 3, ComputingModule.STANDARD_ROUTER)
                .view(-35, 25, 1.35F)
                .balloon(1, GuideBlock.ScenePart.of(0, 0, 0), "The Standard Router",
                        "It joins the access line to the backbone.")
                .balloon(2, GuideBlock.ScenePart.of(1, 0, 0), "The Fibre Optic Cable",
                        "It carries the backbone far, but only straight.")
                .balloon(3, GuideBlock.ScenePart.of(3, 0, 0), "The Standard Repeater",
                        "Lay it in a long run to renew the range of the cable.")
                .balloon(4, GuideBlock.ScenePart.of(6, 0, 0), "The Optical Router",
                        "Turn and branch the fibre here.")
                .balloon(5, GuideBlock.ScenePart.of(6, 0, 3), "The router at the far end",
                        "The access cables of the far computers join it.")
                .paragraph("Hubs are not data blocks. A hub belongs to the peripheral line: it takes one of its"
                        + " computer's device ports and offers ports of its own, so one computer can reach more"
                        + " devices. A hub does not carry the network ([](jsc:peripheral_cables)).")
                .table("The hubs", "Hub", "Ports it offers")
                .row(GuideCell.words("Vintage Hub"), GuideCell.words("2 ports"))
                .row(GuideCell.words("Legacy Hub"), GuideCell.words("4 ports"))
                .row(GuideCell.words("Transition Hub"), GuideCell.words("4 ports"))
                .row(GuideCell.words("Standard Hub"), GuideCell.words("7 ports"))
                .row(GuideCell.words("Advanced Hub"), GuideCell.words("7 ports"))
                .paragraph("The Server Router is a different block, with its own page ([](jsc:server_router)).")
                .ifSomethingGoesWrong("An older machine will not join a newer network.",
                        "Put a router of the newer era between them.",
                        "The far end is still off the network.",
                        "A run on one side of the repeater is still longer than its range. Add a repeater inside it.")
                .register();
    }

    private static void networkOperation(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("network_operation").titled("Running a network")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        guide.page("network_identity", section).titled("Network identity, conflicts and orphans")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Every network has an identity, which tells it apart from every other network. You never"
                        + " type that identity; the network shows a short form of it.")
                .paragraph("A Mainframe with no network yet lays down a new network of its own. A Mainframe that is"
                        + " wired into a segment already on a network adopts that network. A segment is the cables"
                        + " joined through a Mainframe, bridged into one.")
                .table("Words on the Mainframe's screen", "Word", "What it means")
                .row(GuideCell.code("ONLINE"), GuideCell.words("The Mainframe is running."))
                .row(GuideCell.code("OFFLINE"), GuideCell.words("The build is not valid, so it cannot run."))
                .row(GuideCell.code("READY"), GuideCell.words("The build is valid, and it is not running yet."))
                .row(GuideCell.code("STANDBY"), GuideCell.words("It is a failover member, waiting ([](jsc:failover))."))
                .row(GuideCell.code("CONFLICT"), GuideCell.words("The network is in conflict, shown in red."))
                .row(GuideCell.code("LINKED"), GuideCell.words("The Mainframe is joined to a network."))
                .row(GuideCell.code("--"), GuideCell.words("The Mainframe has no network."))
                .paragraph("Two running Mainframes on one segment are a conflict. The network collapses until they are"
                        + " pulled apart: both stop being the network's owner, and nothing is deleted. Only a running"
                        + " Mainframe counts as a rival; one that is switched off does not.")
                .paragraph("When a conflict starts, the chat reads \"[J's Computers] NETWORK_CONFLICT: two Mainframes"
                        + " share one network near\", followed by the position of the Mainframe. When the conflict is"
                        + " over, the chat reads \"[J's Computers] Network conflict resolved near\" and the position.")
                .paragraph("Once the second Mainframe is gone from the segment, the one that is left runs the network"
                        + " again, and the conflict clears.")
                .paragraph("When the last Mainframe of a network is broken, the network is orphaned. An orphaned"
                        + " network keeps its storage, so the storage is still there when somebody plugs a new "
                        + "Mainframe"
                        + " in. Switching a Mainframe off does not orphan its network: the data stops passing"
                        + " through it.")
                .define("Orphaned network", "A network with no Mainframe left. Its storage is still there.")
                .define("Failover", "A Mainframe that backs up the owner of its network, and takes over when that"
                        + " owner is gone.")
                .define("Standby", "A failover Mainframe that waits, powered and ready, without running the network.")
                .ifSomethingGoesWrong("The screen says CONFLICT, and both Mainframes stop.",
                        "Two Mainframes share one segment. Remove the cable that joins their segments, so that each"
                                + " network has one Mainframe.")
                .ifSomethingGoesWrong("The network is orphaned after you broke a Mainframe.",
                        "That Mainframe was the last one of its network, so no Mainframe runs it now. Its storage is"
                                + " kept, as described in the section above.")
                .register();
        guide.page("failover", section).titled("Failover").icon(ComputingModule.MAINFRAME)
                .paragraph("A Mainframe can be a failover member. A failover member does not run the network while its"
                        + " owner is there. It waits, powered and ready, and takes over when the owner is gone.")
                .scene("Two Mainframes on one network: one runs it and the other stands by to take over. Drag it to see"
                        + " them from behind.", 110)
                .place(1, 0, 1, ComputingModule.MAINFRAME, "facing=south,optical=true", MAINFRAME_UP)
                .place(3, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, WEST))
                .place(4, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, 0))
                .place(5, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, 0))
                .place(6, 0, 1, CoreCables.BLOCK, "",
                        wire("fibre_cable", BACKBONE_LANE, WEST | EAST, EAST))
                .place(8, 0, 1, ComputingModule.MAINFRAME, "facing=south,optical=true", MAINFRAME_UP)
                .balloon(1, GuideBlock.ScenePart.of(1, 0, 1), "The Mainframe that runs the network",
                        "Its screen reads ONLINE.")
                .balloon(2, GuideBlock.ScenePart.of(4, 0, 1), "The cable of the segment",
                        "Wire both Mainframes to the same segment.")
                .balloon(3, GuideBlock.ScenePart.of(8, 0, 1), "The standby Mainframe",
                        "Turn on its FAILOVER switch. Its screen reads STANDBY.")
                .paragraph("Turning failover on removes the network this Mainframe owned, instead of leaving it"
                        + " orphaned. A machine that is about to"
                        + " back somebody else's network cannot keep one of its own, or the two would be in conflict"
                        + " the moment they were wired together.")
                .paragraph("You make a failover member in three steps, and the screen tells you which role the"
                        + " Mainframe has.")
                .steps("Open the Mainframe's screen and turn on the FAILOVER switch.",
                        "Wire the failover Mainframe to the same segment as its owner.",
                        "Check that the screen reads STANDBY.")
                .table("The roles of the Mainframes", "Role", "What it does")
                .row(GuideCell.words("Owner, with failover off"), GuideCell.words("Runs the network on its segment."))
                .row(GuideCell.code("STANDBY"), GuideCell.words("Powered and synced, but not running the network."))
                .row(GuideCell.words("Active, after a takeover"),
                        GuideCell.words("Took over the network, and runs it now."))
                .paragraph("A standby takes over only when no owner is running on its segment. It then waits"
                        + " 60 ticks, which is 3 seconds of game time, before it decides that its owner is really gone."
                        + " The wait gives the owner time to come back first.")
                .paragraph("When several standbys wait, only the one placed lowest in the world, by its block position,"
                        + " takes over. The others stay on standby.")
                .paragraph("A standby does not hold a conflict. Only a running owner can put a network into conflict."
                        + " The wait is kept when the world reloads, so a takeover in progress does not start its wait"
                        + " again.")
                .ifSomethingGoesWrong("The standby never takes over.",
                        "Another standby with a lower position is waiting first, or the standby is not on the owner's"
                                + " segment. A standby has nothing to back if it is not on that segment.")
                .ifSomethingGoesWrong("The screen reads CONFLICT.",
                        "Two running owners share a segment. A standby does not cause a conflict. Separate the owners"
                                + " ([](jsc:network_identity)).")
                .register();
        guide.page("network_manager", section).titled("The Network Manager").icon(ComputingModule.MONITOR)
                .paragraph("The Network Manager is the control room of the network. It is a desktop program, and only"
                        + " the Mainframe runs it: you open it on the Mainframe, and no other computer can.")
                .paragraph("Its seven tabs show the network from seven sides. Each one is a way of asking the network"
                        + " what it holds, what it is doing, and how well it is doing it.")
                .table("The seven tabs", "Tab", "What it shows")
                .row(GuideCell.words("Devices"), GuideCell.words("Every node the network knows, with its type and"
                        + " whether it is online or offline."))
                .row(GuideCell.words("Processes"), GuideCell.words("The Operations in flight, one line each. A click"
                        + " opens one, with its buttons Close and Cancel."))
                .row(GuideCell.words("Hardware"), GuideCell.words("The orchestration capacity, the parallel queues,"
                        + " the RAM buffer and the network storage, and how many computers of each kind there are."))
                .row(GuideCell.words("Map"), GuideCell.words("The nodes and their lines, drawn as a map. Drag a node"
                        + " to move it."))
                .row(GuideCell.words("Log"), GuideCell.words("Two views along its top. Operations: the Operations"
                        + " logged so far. Events: what happened to every machine on the network, newest first."))
                .row(GuideCell.words("Stats"), GuideCell.words("How many Operations settled in the last hour, and"
                        + " how long they waited and ran."))
                .row(GuideCell.words("Services"), GuideCell.words("The engines and services of the Mainframe, and the"
                        + " steps to replace an engine ([](jsc:mainframe_services))."))
                .paragraph("On the Map, drag a node with the mouse to move it, drag with the middle button to pan, and"
                        + " turn the wheel to zoom. The legend lists the lines in use: access, backbone, long distance"
                        + " and high compute.")
                .paragraph("The Processes tab lists the work the network is doing right now. Each Operation has its"
                        + " own priority and its own record of how much it has moved ([](jsc:what_operations_are)).")
                .subheading("The network's events")
                .paragraph("The Log tab's Events view is the network's event log, apart from its Operations. Each"
                        + " line has the time, a level, the machine and what happened. INFO is something that"
                        + " happened, such as a machine switched on or off. WARN is something to look at, such as a"
                        + " world event expected or under way. ERROR is something that went wrong, such as two"
                        + " Mainframes on one network.")
                .paragraph("Three lists over it filter what it shows: the level, the machine (or the network itself)"
                        + " and how far back, from the last hour to everything it keeps. Print sends what it shows to"
                        + " the computer's printer. Export writes it to a text file on a disk of the Mainframe, one"
                        + " event a line.")
                .paragraph("The network keeps its last seven days, and at most 2,000 events. Older ones make way for"
                        + " new ones.")
                .define("Node", "Any block the network knows: a Mainframe, a server, a Subframe or a computer.")
                .define("Service", "A program installed on a server, or on the Mainframe, that gives the machine its"
                        + " job. You choose the services of each machine.")
                .ifSomethingGoesWrong("The Network Manager is not in your programs.",
                        "It runs only on the Mainframe. Open the Mainframe, not a Personal Computer.")
                .ifSomethingGoesWrong("A node shows as offline in the Devices tab.",
                        "Switch the node on, and check that its cable run reaches the Mainframe"
                                + " ([](jsc:data_cables)).")
                .register();
        guide.page("mirror", section).titled("The Mirror").icon(ComputingModule.MAINFRAME)
                .paragraph("The Mirror is a shelf of packages for the network. A Mainframe that runs the Mirror holds"
                        + " up to 64 packages, and every computer on its network can install from that shelf.")
                .paragraph("The Mirror is a Mainframe service. It needs 64 megabytes (MB) of disk and 32 MB of"
                        + " random-access memory (RAM) on the Mainframe, and it serves packages only while the"
                        + " Mainframe is powered.")
                .steps("Turn the Mainframe on.",
                        "Type mirror install on the Mainframe's terminal.",
                        "Connect the other computers to the network of this Mainframe.",
                        "Install a package from the Mirror with the package manager of your system.")
                .table("The Mirror's commands", "Command", "What it does")
                .row(GuideCell.code("mirror install"), GuideCell.words("Installs the Mirror on this Mainframe."))
                .row(GuideCell.code("mirror status"), GuideCell.words("Shows whether the Mirror is not installed,"
                        + " serving, or installed with the Mainframe off."))
                .paragraph("You publish a package of your own with the sgpack command. Build it first with sgpack "
                        + "build,"
                        + " then publish it with sgpack publish. Publishing a name that is already on the shelf "
                        + "replaces"
                        + " that package, which is how you release a fix. Take a package back with sgpack unpublish.")
                .define("The Mirror", "The shelf of packages that a Mainframe serves to its network.")
                .ifSomethingGoesWrong("could not resolve mirror://",
                        "This computer is not on the network of a Mainframe that runs the Mirror. Connect it to that"
                                + " network, or install the Mirror on the Mainframe with mirror install.",
                        "the network has no running Mainframe to host the Mirror",
                        "Turn the Mainframe on, and try again.",
                        "the Mirror is full (64 packages)",
                        "Take one package off the shelf with sgpack unpublish.",
                        "this is not a package",
                        "Build the package with sgpack build first.")
                .register();
        guide.page("network_services", section).titled("Services on a server bay").icon(ComputingModule.SERVER)
                .paragraph("A server in a rack gets its role from the services installed on it. Its hardware decides"
                        + " how much a server can hold; the services decide what job it does. You choose the services"
                        + " each server runs.")
                .paragraph("A service answers only while its machine is up. Stop the server, and its services stop"
                        + " with it. Each server keeps its own list of services ([](jsc:server_racks)).")
                .table("The services of a server bay", "Service", "What it does", "Era")
                .row(GuideCell.words("Predictive Cache"), GuideCell.words("Keeps the bay's most-wanted items staged in"
                        + " memory, cutting read latency by 15%."), GuideCell.words("Standard"))
                .row(GuideCell.words("Load Balancer"), GuideCell.words("Spreads writes across the bay's drives instead"
                        + " of filling them one after another."), GuideCell.words("Legacy"))
                .row(GuideCell.words("Integrity Monitor"), GuideCell.words("Re-reads the bay after a hot swap, so the"
                        + " network index does not doubt it."), GuideCell.words("Standard"))
                .row(GuideCell.words("Messenger Service"), GuideCell.words("Keeps the network's conversations."),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("KnotHub"), GuideCell.words("Keeps the source this network is still working on,"
                        + " revision by revision."), GuideCell.words("Standard"))
                .row(GuideCell.words("Soundfoundry Server"), GuideCell.words("Streams the music catalogue to every"
                        + " Soundfoundry on the network."), GuideCell.words("Standard"))
                .paragraph("Predictive Cache cuts the read latency of its bay by 15%, and a read never takes less than"
                        + " one tick. A read cache card in the same server adds its own cut on top of it.")
                .define("Predictive Cache", "A service of a server bay that keeps the bay's most-wanted items ready"
                        + " in memory, so you read them 15% sooner.")
                .paragraph("Integrity Monitor matters when you pull a drive out of a running bay. Without it, the index"
                        + " stops trusting that bay until somebody rebuilds the index with the reindex command. With"
                        + " it, the bay is re-read after the swap, and the index is not left in doubt.")
                .define("Integrity Monitor", "A service of a server bay that re-reads the bay after you swap a drive,"
                        + " so the network's index keeps trusting it.")
                .paragraph("Load Balancer changes where your new items land in its bay. Without it, the server fills"
                        + " its disks one after another. With it, each write goes to the emptiest disk first, so no"
                        + " single disk fills up while the others stand empty.")
                .define("Load Balancer", "A service of a server bay that sends each write to the bay's emptiest"
                        + " disk, instead of filling the disks in turn.")
                .ifSomethingGoesWrong("A service does not answer.",
                        "Its server is off. Turn the server on.")
                .ifSomethingGoesWrong("The index says a bay is unconfirmed after a hot swap.",
                        "The bay has no Integrity Monitor. Install it on that bay, or rebuild the index by hand with"
                                + " the reindex command.")
                .register();
        guide.page("mainframe_services", section).titled("Services on the Mainframe").icon(ComputingModule.MAINFRAME)
                .paragraph("The Mainframe carries three kinds of software: an engine that decides the network's work,"
                        + " the Automation Engine that runs saved jobs, and the Mirror ([](jsc:mirror)). A new "
                        + "Mainframe"
                        + " ships with the Midsoft IQL Server installed and running, in the version of its age.")
                .paragraph("The services of a Mainframe are on or off. They serve only while the Mainframe is powered,"
                        + " and they are removed when its disk is formatted.")
                .table("The engines of the Mainframe", "Engine", "What it does")
                .row(GuideCell.words("Midsoft IQL Server"), GuideCell.words("Plans the network's work and answers it in"
                        + " IQL. The ISMS is its studio. ([](jsc:engines))"))
                .row(GuideCell.words("NextgreIQL"), GuideCell.words("Shows how it plans the network's work, and lets "
                        + "you"
                        + " take part in the planning."))
                .row(GuideCell.words("Prophet YourIQL"), GuideCell.words("You tell it which state the network is to "
                        + "keep,"
                        + " and it works out how to get there and stay there."))
                .paragraph("Only one engine plans the network's work at a time. An engine that you install becomes the"
                        + " active one when the network has none. Activating an installed engine makes it the one that"
                        + " plans, and the work the previous engine planned carries on, since an Operation belongs to "
                        + "the"
                        + " network once it is made.")
                .paragraph("Replacing an engine takes a while, and the network has no engine until the replacement is"
                        + " over. The replacement goes through these steps, in order:")
                .steps("Stop taking new plans.",
                        "Stop the old engine.",
                        "Start the new engine.",
                        "Discover the storage and the machines.",
                        "Build the indexes.",
                        "Publish the capabilities.",
                        "Wait until the screen says Ready.")
                .paragraph("The Automation Engine keeps the network's jobs and fires them on time. You write and "
                        + "supervise"
                        + " those jobs in the Automation Manager ([](jsc:automation_engine)).")
                .ifSomethingGoesWrong("The Services tab says No Network Operations Engine is running.",
                        "Install an engine, or start the installed one.")
                .ifSomethingGoesWrong("A replacement will not start.",
                        "Another replacement is under way, the engine is not installed, the Mainframe is off, or the"
                                + " engine is already the running one.")
                .register();
    }

    private static void networkRemote(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("network_remote").titled("Reaching other machines")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        guide.page("remote_access", section).titled("Remote access: ssh and Remote Control")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Being on the same network is what lets you reach another computer. The secure shell"
                        + " (ssh) command opens a shell on another computer of your network, from the terminal you"
                        + " are at.")
                .steps("Type ssh on its own to see the machines you can reach.",
                        "Type ssh and the host name of the machine you want.",
                        "Type exit to leave it. Closing the window leaves it too.")
                .paragraph("A machine answers to its host name, the name its owner gave it, the start of its node id,"
                        + " or the name of its system, as long as that name picks out exactly one machine. You can "
                        + "reach"
                        + " the Mainframe, the Personal Computers of the network and the servers in the racks. You "
                        + "cannot"
                        + " reach the computer you sit at.")
                .paragraph("Access needs no password: being on the same network is what grants it.")
                .table("Messages of ssh", "Message", "What it means")
                .row(GuideCell.words("Connected to <machine>. Type exit to return."),
                        GuideCell.words("The session is open on the other computer."))
                .row(GuideCell.words("<name>: host not found on this network"),
                        GuideCell.words("No machine on your network has that name; type ssh alone to see the names."))
                .row(GuideCell.words("<name> matches <n> machines (<names>) - use the host name or node id"),
                        GuideCell.words("Use the host name or the start of the node id instead."))
                .row(GuideCell.words("connect to host <name>: machine is powered off"),
                        GuideCell.words("The machine is off. Switch it on."))
                .paragraph("Commands you run on another computer use that computer's own budget, and they carry its"
                        + " name. A computer can refuse programs from others: its remote setting decides it.")
                .table("The remote setting", "Command", "What it does")
                .row(GuideCell.code("config remote on"),
                        GuideCell.words("Other computers may run programs here. This is the default."))
                .row(GuideCell.code("config remote off"),
                        GuideCell.words("Other computers cannot run programs here."))
                .paragraph("Remote Control is the point-and-click way into the network's other machines"
                        + " ([](jsc:remote_control)).")
                .define("Remote Control", "The desktop program that takes over another machine of the network and"
                        + " shows it on your screen.")
                .ifSomethingGoesWrong("host not found on this network",
                        "Check the name with ssh alone, or in the Devices tab of the Network Manager. The machine "
                                + "must be"
                                + " on the same network.",
                        "machine is powered off",
                        "Switch the machine on.",
                        "cannot run programs",
                        "The other computer has config remote off. Turn it on there with config remote on.")
                .register();
        guide.page("shared_folders", section).titled("Shared folders").icon(ComputingModule.SERVER)
                .paragraph("A folder on your computer can be shared with the other computers of the network. A shared"
                        + " folder is seen by its path, which starts with two backslashes, then the computer's name,"
                        + " then the share's name: \\\\computer\\name.")
                .paragraph("A share takes its name from the last part of its folder's path, and a drive root such as C:"
                        + " is named c. Two folders cannot share one name, and a computer shares at most 16 folders.")
                .table("The share commands", "Command", "What it does")
                .row(GuideCell.code("config share <folder> [read|write]"),
                        GuideCell.words("Shares the folder. Read only, unless you say write."))
                .row(GuideCell.code("config unshare <name>"), GuideCell.words("Closes that share."))
                .row(GuideCell.code("config"), GuideCell.words("Lists the shares, or none when there are none."))
                .row(GuideCell.code("config netshare <0-1000>"),
                        GuideCell.words("Sets the netshare of the system disk, from 0 to 1000 permille."))
                .paragraph("Another computer's share is reached through its path. The path below a share is followed on"
                        + " the owner's computer, exactly as it would be at that computer's own prompt.")
                .ifSomethingGoesWrong("The share is refused, with a message that it has no such folder.",
                        "The folder does not exist. Check its name, and share a folder that exists.",
                        "Another folder is already shared under that name.",
                        "Two folders cannot share one name. Unshare the other one with config unshare first.",
                        "This computer already shares 16 folders.",
                        "Unshare one of them with config unshare first.",
                        "A write is refused.",
                        "The share was opened for reading only. Ask its owner to share it with write.")
                .register();
    }

    private static void mcNetTerminal(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("mc_net_terminal").titled("MC-NET")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        guide.page("mc_net", section).titled("MC-NET, the network terminal").icon(ComputingModule.MONITOR)
                .paragraph("MC-NET is the operating system of the network terminal. It is a Vintage system, made by"
                        + " Nouvell, and it needs 2 MB of RAM ([](jsc:eras)).")
                .paragraph("You install MC-NET from an install medium only, the way you install MC-DOS and UNIX System"
                        + " V ([](jsc:installing)).")
                .paragraph("With the Interactor installed, the Interactor takes the whole screen: the network's "
                        + "storage,"
                        + " its work and its prompt. Without it, MC-NET is only a prompt, the way a Linux with no "
                        + "desktop"
                        + " is only a prompt. The message on the screen names what is missing.")
                .define("MC-NET", "The Vintage operating system of the network terminal, which shows the Interactor"
                        + " on the whole screen.")
                .define("Operating space", "The full screen a system draws around its prompt, with its own store and"
                        + " work. A system with no operating space is only a prompt.")
                .ifSomethingGoesWrong("The screen is only a prompt.",
                        "The Interactor is missing from MC-NET. Follow the message on the screen, which names what is"
                                + " missing and how to put it back.")
                .seeAlso("jsc:interactor_screen", "jsc:mc_net_commands")
                .register();
        guide.page("interactor_screen", section).titled("The Interactor").icon(ComputingModule.MONITOR)
                .paragraph("The Interactor is the network's full-screen window on MC-NET. On its own it takes the whole"
                        + " terminal. Its headings are the network, the servers, what is held back, the work in "
                        + "flight and"
                        + " what you have starred, and a list sits under them.")
                .paragraph("Arrows move through the list. Tab goes to the next heading, and typing looks for something."
                        + " Along the foot of the screen are ten keys, labelled 1 to 10. Each key is the function key "
                        + "of the"
                        + " same number: press [[F1]] for key 1, up to [[F10]] for key 10.")
                .table("The keys of the Interactor", "Key", "What it does")
                .row(GuideCell.code("F1"), GuideCell.words("Help."))
                .row(GuideCell.code("F2"), GuideCell.words("Get: takes items from the network into your hands. It asks"
                        + " for the quantity first."))
                .row(GuideCell.code("F3"), GuideCell.words("Put: puts what is in your hands into the network."))
                .row(GuideCell.code("F4"), GuideCell.words("Craft: asks for a quantity, then starts a craft of the "
                        + "item."))
                .row(GuideCell.code("F5"), GuideCell.words("Lock: holds the selected item back. It is then listed under"
                        + " held back."))
                .row(GuideCell.code("F6"), GuideCell.words("Free: unlocks the selected item."))
                .row(GuideCell.code("F7"), GuideCell.words("Fav: stars the selected item, so that it is listed under"
                        + " starred."))
                .row(GuideCell.code("F8"), GuideCell.words("Stop: stops the selected operation on the Ops heading. On"
                        + " any other heading it says that only operations can be stopped."))
                .row(GuideCell.code("F9"), GuideCell.words("Find: looks for an item as you type its name."))
                .row(GuideCell.code("F10"), GuideCell.words("Quit: gives the terminal back."))
                .paragraph("The same words work at the prompt, and they do the same things. Each one is shown here with"
                        + " what it does.")
                .table("Words at the prompt", "Words", "What they do")
                .row(GuideCell.code("interac"), GuideCell.words("Opens the whole network on the whole screen."))
                .row(GuideCell.code("interac list stone --sort count"),
                        GuideCell.words("Lists stone, with the largest amount first."))
                .row(GuideCell.code("interac get 42 cobblestone"),
                        GuideCell.words("Takes 42 cobblestone into your hands."))
                .row(GuideCell.code("interac put hand"), GuideCell.words("Puts what is in your hand into the network."))
                .row(GuideCell.code("interac where diamond"), GuideCell.words("Says where diamond is held."))
                .paragraph("The option --to local leaves what is taken in this computer, and not in your hands. The "
                        + "option"
                        + " --sort count orders a listing by how much there is, and --sort name orders it by name.")
                .paragraph("You can name an item the way you would say it: cobblestone, minecraft:cobblestone, or \"oak"
                        + " log\". A name that fits more than one thing is answered with the things it fits, so that "
                        + "your"
                        + " next line can name the one you meant.")
                .paragraph("With words, the Interactor does the one thing you asked and gives the prompt back. An"
                        + " operation that takes time prints its number, and its result appears before the next "
                        + "prompt.")
                .ifSomethingGoesWrong("The name matches nothing on the network.",
                        "Check the spelling, or name the item another way, such as minecraft:cobblestone.",
                        "The network holds nothing.",
                        "Put items in first, with put.",
                        "interac is not offered at the prompt.",
                        "The machine has no network. interac, ssh and mirror are not offered on a machine with no"
                                + " network ([](jsc:network_members)).",
                        "The Stop key says that only operations can be stopped.",
                        "Stop works on the Ops heading only. Select an operation there first.")
                .register();
        guide.page("mc_net_commands", section).titled("The network commands").icon(ComputingModule.MONITOR)
                .paragraph("When you need a command, find its name in this list. These are the words the MC-NET prompt"
                        + " knows. They come in four groups: the network and its"
                        + " data, the files and the text, the machine itself, and its software. Each command has its "
                        + "own"
                        + " page below, made from the command, with its options, its examples and the systems it runs "
                        + "on.")
                .subheading("The network and its data")
                .command(CommandGuidePages.id(Platform.MC_NET, "net"))
                .command(CommandGuidePages.id(Platform.MC_NET, "iql"))
                .command(CommandGuidePages.id(Platform.MC_NET, "interac"))
                .command(CommandGuidePages.id(Platform.MC_NET, "ssh"))
                .command(CommandGuidePages.id(Platform.MC_NET, "cluster"))
                .command(CommandGuidePages.id(Platform.MC_NET, "gateway"))
                .command(CommandGuidePages.id(Platform.MC_NET, "mirror"))
                .subheading("The files and the text")
                .command(CommandGuidePages.id(Platform.MC_NET, "listfiles"))
                .command(CommandGuidePages.id(Platform.MC_NET, "seefile"))
                .command(CommandGuidePages.id(Platform.MC_NET, "read"))
                .command(CommandGuidePages.id(Platform.MC_NET, "write"))
                .command(CommandGuidePages.id(Platform.MC_NET, "copy"))
                .command(CommandGuidePages.id(Platform.MC_NET, "rename"))
                .command(CommandGuidePages.id(Platform.MC_NET, "delete"))
                .command(CommandGuidePages.id(Platform.MC_NET, "findtext"))
                .command(CommandGuidePages.id(Platform.MC_NET, "sortlines"))
                .subheading("The machine itself")
                .command(CommandGuidePages.id(Platform.MC_NET, "status"))
                .command(CommandGuidePages.id(Platform.MC_NET, "devices"))
                .command(CommandGuidePages.id(Platform.MC_NET, "msd"))
                .command(CommandGuidePages.id(Platform.MC_NET, "config"))
                .command(CommandGuidePages.id(Platform.MC_NET, "reboot"))
                .command(CommandGuidePages.id(Platform.MC_NET, "clear"))
                .command(CommandGuidePages.id(Platform.MC_NET, "memory"))
                .command(CommandGuidePages.id(Platform.MC_NET, "tasklist"))
                .command(CommandGuidePages.id(Platform.MC_NET, "end"))
                .command(CommandGuidePages.id(Platform.MC_NET, "findcommand"))
                .command(CommandGuidePages.id(Platform.MC_NET, "worldtime"))
                .command(CommandGuidePages.id(Platform.MC_NET, "format"))
                .command(CommandGuidePages.id(Platform.MC_NET, "run"))
                .command(CommandGuidePages.id(Platform.MC_NET, "help"))
                .command(CommandGuidePages.id(Platform.MC_NET, "echo"))
                .command(CommandGuidePages.id(Platform.MC_NET, "whoami"))
                .command(CommandGuidePages.id(Platform.MC_NET, "exit"))
                .command(CommandGuidePages.id(Platform.MC_NET, "set"))
                .command(CommandGuidePages.id(Platform.MC_NET, "runbackground"))
                .command(CommandGuidePages.id(Platform.MC_NET, "schedule"))
                .command(CommandGuidePages.id(Platform.MC_NET, "showcommands"))
                .command(CommandGuidePages.id(Platform.MC_NET, "listcmd"))
                .subheading("Its software")
                .command(CommandGuidePages.id(Platform.MC_NET, "vim"))
                .command(CommandGuidePages.id(Platform.MC_NET, "emacs"))
                .command(CommandGuidePages.id(Platform.MC_NET, "programs"))
                .command(CommandGuidePages.id(Platform.MC_NET, "install"))
                .command(CommandGuidePages.id(Platform.MC_NET, "uninstall"))
                .command(CommandGuidePages.id(Platform.MC_NET, "store"))
                .command(CommandGuidePages.id(Platform.MC_NET, "iqlengine"))
                .command(CommandGuidePages.id(Platform.MC_NET, "services"))
                .command(CommandGuidePages.id(Platform.MC_NET, "netgetter"))
                .command(CommandGuidePages.id(Platform.MC_NET, "analyze"))
                .command(CommandGuidePages.id(Platform.MC_NET, "reindex"))
                .command(CommandGuidePages.id(Platform.MC_NET, "vacuum"))
                .command(CommandGuidePages.id(Platform.MC_NET, "scc"))
                .command(CommandGuidePages.id(Platform.MC_NET, "sgsc"))
                .command(CommandGuidePages.id(Platform.MC_NET, "sigma"))
                .command(CommandGuidePages.id(Platform.MC_NET, "sgpack"))
                .seeAlso("jsc:interactor_screen", "jsc:remote_control")
                .register();
    }

    /*
     * What a cable block of a scene holds: one wire of the named cable in its lane, the faces it crosses, and the
     * faces where it plugs into a device. A scene runs no server to work these out, so each block says them itself,
     * and a plug always comes with the crossing of its face.
     */
    private static String wire(final String cable, final int lane, final int crossed, final int plugged) {
        final StringBuilder links = new StringBuilder();
        for (int each = 0; each < 9; each++) {
            links.append(each == 0 ? "" : ",").append(each == lane ? crossed | plugged << PLUGGED : 0);
        }
        return "{Wires:[{cable:\"jsc:" + cable + "\"}],Links:[I;" + links + "]}";
    }
}
