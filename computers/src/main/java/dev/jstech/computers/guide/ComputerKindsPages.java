/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ClusterManagementComputerBlock;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.HbwInterfaceBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerCaseItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;

/**
 * The pages about the computers the network runs: the Personal Computer and the Crafting Computer, the Mainframe
 * that runs a network, the racks and servers that hold its storage, the clusters that install and manage many
 * machines, the supercomputers, and the datacenters with the way their load is balanced.
 */
final class ComputerKindsPages {

    /*
     * The faces of a block as a cable's links count them, one bit for each; the bit of the face where a wire meets a
     * device is set a second time, eight places higher, beside the bit of the face it crosses.
     */
    private static final int DOWN = 1;
    private static final int UP = 2;
    private static final int NORTH = 4;
    private static final int SOUTH = 8;
    private static final int WEST = 16;
    private static final int EAST = 32;
    private static final int PLUG_SHIFT = 8;
    /** How many lanes a cable block keeps links for, and the lanes the backbone and the compute fabric run in. */
    private static final int LANES = 9;
    private static final int BACKBONE_LANE = 1;
    private static final int FABRIC_LANE = 4;
    /** The four horizontal facings in clockwise order, and the way each of them points along the grid. */
    private static final List<String> FACINGS = List.of("north", "east", "south", "west");
    private static final int[] FACING_X = {0, 1, 0, -1};
    private static final int[] FACING_Z = {-1, 0, 1, 0};
    private static final String BACKBONE_CABLE = "jsc:cx4_cable";
    private static final String FABRIC_CABLE = "jsc:hpc_cable";

    /*
     * What a Mainframe cabinet draws from: which hardware slots hold a part (the board, two processors, four memory
     * modules, two graphics cards, the power supply and two disks), no disk carrying a system, and a machine that is
     * running, built right, on a network and with its service panel off so that its parts show.
     */
    private static final String MAINFRAME_DATA = "{VisualHardware:3695079,VisualSystems:0,VisualFlags:15}";
    /** The rack units, from the top row down, of a rack with a server, a KVM switch, a storage server and the rest. */
    private static final int[] MIXED_RACK = {1, 10, 0, 4, 0, 1, 12, 11};
    private static final int[] PLAIN_RACK = {1, 1, 1, 1, 1, 1, 12, 11};
    /** A supercomputer rack: four nodes of two rack units each. */
    private static final int[] NODE_RACK = {6, 0, 6, 0, 6, 0, 6, 0};

    private ComputerKindsPages() {
    }

    /** Declares the sections and entries of the chapter about the computers, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        computers(guide, chapter);
        racks(guide, chapter);
        clusters(guide, chapter);
    }

    private static void computers(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("computers").titled("Computers")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        personalComputers(guide, section);
        craftingComputers(guide, section);
        mainframes(guide, section);
    }

    private static void racks(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("racks").titled("Racks and servers")
                .icon(ComputingModule.SERVER_RACK).in(chapter).register();
        serverRacks(guide, section);
        serverTypes(guide, section);
        rackDriveBays(guide, section);
        raidArrays(guide, section);
        rackEquipment(guide, section);
        rackCoolingAndHeat(guide, section);
    }

    private static void clusters(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("clusters").titled("Clusters and datacenters")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER).in(chapter).register();
        subframes(guide, section);
        clusterManagementComputers(guide, section);
        supercomputers(guide, section);
        hbwInterface(guide, section);
        datacenters(guide, section);
        serverRouter(guide, section);
        loadBalancing(guide, section);
    }

    private static void personalComputers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("personal_computers", section).titled("Personal Computers")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .coversAll(ComputersGuide.blocksOf(PersonalComputerBlock.class))
                .paragraph("The Personal Computer (PC) is your own computer, the one you sit at. You install"
                        + " a system on it and run programs, and from it you reach the network: you store"
                        + " and take items, watch the machines work, and ask the network to craft.")
                .paragraph("Inside, it takes one motherboard, one processor (CPU), four memory modules"
                        + " (random-access memory, or RAM), four graphics cards (graphics processing units, or"
                        + " GPUs), two disks and one power supply.")
                .table("What a Personal Computer holds")
                .fixed("Motherboard", "1")
                .fixed("Processors", "1")
                .fixed("Memory", "4")
                .fixed("Graphics cards", "4")
                .fixed("Disks", "2")
                .fixed("Power supply", "1")
                .fixed("Storage slots", "up to 18")
                .paragraph("Its storage is the space of its own disks, counted in storage slots: up to 18 slots.")
                .paragraph("Every era has its own Personal Computer. From the Standard on, you can choose a plain"
                        + " case, a High Performance case or an Aesthetic case. The Vintage, Legacy and Transition"
                        + " eras have one case each.")
                .paragraph("Some cards serve the Workshop program, which gives you a crafting table, a furnace, an"
                        + " enchanting table and an anvil in one window. Each card works on a Personal Computer from"
                        + " the Legacy on, and each draws 15 W of power.")
                .table("The Workshop cards", "Card", "What it is")
                .row(GuideCell.words("Crafting Table Card"),
                        GuideCell.words("A crafting table, on a card"))
                .row(GuideCell.words("Furnace Card"),
                        GuideCell.words("A furnace that needs no fuel, on a card"))
                .row(GuideCell.words("Enchanting Card"),
                        GuideCell.words("An enchanting table with every bookshelf, on a card"))
                .row(GuideCell.words("Anvil Card"),
                        GuideCell.words("An anvil that never wears, on a card"))
                .paragraph("The Workshop keeps what you put in its window in the computer: the crafting grid, the"
                        + " furnace's input and output, the item to enchant and the items on the anvil. It is lost"
                        + " when the computer breaks.")
                .ifSomethingGoesWrong("The assembly screen says \"Not a computer yet\".",
                        "A computer needs a motherboard, a processor, memory, a disk and a power supply.",
                        "The screen says the processor's socket does not fit the board's socket.",
                        "Choose a processor with the socket the motherboard takes"
                                + " ([](jsc:motherboards)).",
                        "The screen says there are too many PCI Express cards for the board.",
                        "Take a card out, or use a motherboard with more PCI Express slots.",
                        "The screen says the power supply is insufficient.",
                        "Use a power supply that gives more watts than the parts draw"
                                + " ([](jsc:power_supplies)).",
                        "The screen says the board does not take that kind of memory.",
                        "Use memory of a kind the board takes ([](jsc:memory)).")
                .seeAlso("jsc:first_computer", "jsc:firmware")
                .register();
    }

    private static void craftingComputers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("crafting_computers", section).titled("Crafting Computers")
                .icon(ComputingModule.CRAFTING_COMPUTER)
                .coversAll(ComputersGuide.blocksOf(CraftingComputerBlock.class))
                .paragraph("The Crafting Computer crafts for the network. It runs the crafting table recipes itself,"
                        + " and it drives the machines that make the rest through"
                        + " [Crafting Interfaces](jsc:crafting_interfaces).")
                .paragraph("It crafts only with a Crafting Card in it: without one, it does not craft at all."
                        + " The card decides how many Crafting Interfaces it drives, and it keeps the crafting"
                        + " table [patterns](jsc:patterns) in its own memory, so they move with the card"
                        + " ([](jsc:crafting_cards)).")
                .paragraph("Build it as you would a Personal Computer ([](jsc:personal_computers)). It takes"
                        + " the same kinds of part, and it holds these:")
                .table("What a Crafting Computer holds")
                .fixed("Motherboard", "1")
                .fixed("Processors", "1")
                .fixed("Memory", "4")
                .fixed("PCI Express cards", "4")
                .fixed("Disks", "2")
                .fixed("Power supply", "1")
                .fixed("Drive bays", "2")
                .paragraph("The era of the Crafting Card sets how many Crafting Interfaces it drives, and how many"
                        + " patterns its memory keeps:")
                .table("Crafting Cards by era", "Era", "Crafting Interfaces", "Patterns on the card")
                .row(GuideCell.words("Vintage"), GuideCell.words("2"), GuideCell.words("2"))
                .row(GuideCell.words("Legacy"), GuideCell.words("4"), GuideCell.words("4"))
                .row(GuideCell.words("Transition"), GuideCell.words("5"), GuideCell.words("5"))
                .row(GuideCell.words("Standard"), GuideCell.words("6"), GuideCell.words("6"))
                .row(GuideCell.words("Advanced"), GuideCell.words("8"), GuideCell.words("8"))
                .paragraph("Open the Crafting Manager on it to load its patterns and watch the jobs"
                        + " ([](jsc:crafting_manager)).")
                .ifSomethingGoesWrong("It crafts nothing.",
                        "It has no Crafting Card, or it is off. Put a card in it, and switch it on.",
                        "A crafting table pattern is gone.",
                        "It lives on the card. It went with the card to another computer, so put the"
                                + " card back.")
                .seeAlso("jsc:autocrafting")
                .register();
    }

    private static void mainframes(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("mainframes", section).titled("Mainframes")
                .icon(ComputingModule.MAINFRAME)
                .coversAll(ComputersGuide.blocksOf(MainframeBlock.class))
                .paragraph("Every network has exactly one Mainframe, and it is the network's control room. Only the"
                        + " Mainframe runs the Network Manager. Two Mainframes on one network are a conflict, so give"
                        + " each network its own.")
                .define("Mainframe", "The one computer of a network that runs the Network Manager and orchestrates"
                        + " the network's work.")
                .paragraph("A Mainframe is big: three blocks wide, two tall and two deep. Place a Mainframe block in"
                        + " the middle of the ground floor, on the front row. If the whole 3 by 2 by 2 space is clear,"
                        + " the cabinet forms. If not, the item stays in your hand, and particles outline the blocked"
                        + " spaces so you can see what is in the way.")
                .paragraph("The Mainframe takes an MTX motherboard of its own era. There is one Mainframe for each"
                        + " era: the Mainframe (Standard), and the Vintage, Legacy, Transition and Advanced"
                        + " Mainframes.")
                .paragraph("A Mainframe runs the network only with a system on one of its disks, such as MC-NET"
                        + " ([](jsc:mc_net)). Install it as on any computer ([](jsc:installing)); until then the"
                        + " Mainframe holds every Operation it is given. From the Standard on, the backbone is fibre,"
                        + " and the Mainframe takes fibre only while an Optical Network Card sits in one of its card"
                        + " slots ([](jsc:expansion_cards)).")
                .table("What a Mainframe holds")
                .fixed("Motherboard", "1 MTX")
                .fixed("Processors", "up to 4")
                .fixed("Memory", "up to 8")
                .fixed("Graphics cards", "up to 6")
                .fixed("Disks", "up to 4")
                .fixed("Power supply", "1")
                .fixed("Storage slots", "up to 27")
                .paragraph("Its hardware sets how fast the network works. Its processors add up to its capacity, and"
                        + " the Subframes that work for it add to that ([](jsc:subframes)). Its memory sets a RAM"
                        + " buffer, counted in items: a Vintage memory module gives 1 to 16 items, and an Advanced"
                        + " module gives 2,048 to 32,768. The Mainframe works at the lower of its capacity and its RAM"
                        + " buffer, so a small buffer holds back fast processors.")
                .define("Subframe", "A computer that lends its capacity and its queues to the Mainframe of its"
                        + " network.")
                .define("Capacity", "The items per tick that the Mainframe's processors can handle, added up.")
                .paragraph("The processors run one queue, and each graphics card runs one more. The queues do not"
                        + " share capacity. A weak card can hold its queue down, and the assembly screen then says"
                        + " \"A card's queue runs at most at the CPU's speed, and no faster than the card\".")
                .paragraph("The assembly screen shows these parts of the Mainframe:")
                .table("The assembly screen", "Part", "What it shows")
                .row(GuideCell.words("CAPACITY (it/t)"),
                        GuideCell.words("The items per tick its processors can handle, added up."))
                .row(GuideCell.words("QUEUES"),
                        GuideCell.words("The queues of its processors and graphics cards."))
                .row(GuideCell.words("RAM BUFFER (it)"),
                        GuideCell.words("The items its memory can buffer, added up over the modules."))
                .row(GuideCell.words("AUTO START"),
                        GuideCell.words("Turns the Mainframe on by itself when its build stands."))
                .row(GuideCell.words("FAILOVER"),
                        GuideCell.words("Switches the Mainframe between owner and standby."))
                .row(GuideCell.words("PWR, DISK and NET"),
                        GuideCell.words("Three lamps: power, disk and network."))
                .row(GuideCell.words("STANDBY"),
                        GuideCell.words("This Mainframe waits as a standby."))
                .row(GuideCell.words("CONFLICT"),
                        GuideCell.words("Two owners share one segment of the network."))
                .paragraph("Its power button sits on the sill, the ledge at the bottom of the front, in the middle. A"
                        + " click on it switches the Mainframe on or off, whatever you hold. A click anywhere else on"
                        + " the cabinet opens its assembly screen. Sneak-click the cabinet to open or close the service"
                        + " panel. Its processors, memory and cards are seen only through that opening.");
        mainframeScene(page);
        page.paragraph("AUTO START keeps the Mainframe ready. With it on and a valid build, the Mainframe starts"
                        + " at once, as a cold start that runs its self-test. If a change breaks the build, the"
                        + " Mainframe turns off. When the build stands again, it turns on by itself.")
                .paragraph("With the [FAILOVER](jsc:failover) switch off, the Mainframe is an owner: it takes the"
                        + " network segment it stands on, or lays down a network of its own. With FAILOVER on, it is a"
                        + " standby: when an owner is on its network, it attaches to that network and does not run it.")
                .define("Owner", "The Mainframe that runs its network, with FAILOVER off.")
                .define("Standby", "A Mainframe that waits on a network an owner runs, ready to take it over.")
                .paragraph("A standby takes over a network that has no owner. After 3 seconds, which is 60 ticks, one"
                        + " standby runs the network itself. The countdown is saved with the world, so reloading does"
                        + " not restart it. If two owners sit on one segment of a network, the network goes into"
                        + " conflict, and the screen shows CONFLICT.")
                .paragraph("If you break a Mainframe, it drops the Mainframe of its own era, and the items in its"
                        + " inventory spill out. If it was the last Mainframe of its network, the network is orphaned,"
                        + " not deleted, so its storage survives ([](jsc:network_identity)).")
                .ifSomethingGoesWrong("The Mainframe does not form.",
                        "Clear the whole 3 by 2 by 2 space, then place it again. The blocked spaces show particles.",
                        "Every Operation stays queued.",
                        "The Mainframe has no system. Install one on it ([](jsc:installing)).",
                        "The fibre does not join the Mainframe.",
                        "Put an Optical Network Card in one of its card slots ([](jsc:expansion_cards)).",
                        "The screen shows CONFLICT.",
                        "Two owners share one segment. Take one of them off the segment, or switch FAILOVER on for"
                                + " one.",
                        "The network is slow.",
                        "Faster processors raise the capacity, and more graphics cards give more queues. Check the"
                                + " memory too: a small buffer holds the capacity back.",
                        "The chat reports a network conflict: two Mainframes share one network.",
                        "Keep one Mainframe on each network, and separate the cables between them.")
                .seeAlso("jsc:states_and_queues", "jsc:failover")
                .register();
    }

    private static void serverRacks(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("server_racks", section).titled("Server racks and servers")
                .icon(ComputingModule.SERVER_RACK)
                .coversAll(ComputersGuide.blocksOf(ServerRackBlock.class))
                .coversAll(ComputersGuide.itemsOf(ServerItem.class, ServerCaseItem.class))
                .paragraph("Your items have to live somewhere. In J's Computers they live on the disks of servers,"
                        + " and the servers live in racks.")
                .define("Server", "A computer built to sit in a Server Rack.")
                .paragraph("A Server Rack gives eight rack units of room. A machine's height is counted in those"
                        + " units: most servers take one, the bigger ones take two.")
                .define("Server Rack", "A cabinet, two blocks wide, three tall and two deep, that holds machines in"
                        + " rows.")
                .define("Rack unit (U)", "The height a machine takes in a rack, counted in units. Each cabinet has"
                        + " eight.");
        serverRackScene(page);
        page.paragraph("The Supercomputer Rack holds supercomputer nodes only, and comes in the Standard and the"
                        + " Advanced ([](jsc:supercomputers)).")
                .define("Supercomputer", "Many computers that work as one large machine, joined by high compute"
                        + " cable.")
                .paragraph("A rack seats servers of its own era or older. Each server has drive bays, which show at"
                        + " the front of the rack. The [server types](jsc:server_types) list how many a server has.")
                .paragraph("To mount a server by hand, hold it and click the front of the cabinet. It goes into the"
                        + " row under the crosshair, counted from the top. Aim elsewhere, and it takes the first row"
                        + " that fits, from the top down. A rack unit mounts the same way.")
                .paragraph("Click the cabinet with an empty hand to open the rack screen. On a Supercomputer Rack,"
                        + " sneak-click to take the livery panel off.")
                .paragraph("Each rack unit has a power switch for its bay, and a new rack switches every bay on. The"
                        + " switch of a bay is the power button of the machine in it. The machine still runs its"
                        + " self-test and stops at its boot menu.")
                .paragraph("The disks in the bays are the storage. A server item carries no disks of its own: its"
                        + " drives are the disks you put in its bays. Join the rack's back port to the backbone"
                        + " cable, which the rack takes up to its era ([](jsc:data_cables)). From the Standard on, the"
                        + " backbone is fibre, and the rack takes it only through a Fibre Optic Interface mounted in"
                        + " one of its rows ([](jsc:rack_equipment)).")
                .ifSomethingGoesWrong("The fibre does not join the rack.",
                        "Mount a Fibre Optic Interface in the rack: the Standard one for the Fibre Optic Cable, the"
                                + " Advanced one for the OM5 Fibre Cable too ([](jsc:rack_equipment)).",
                        "A server does not go in.",
                        "It is from a newer era than the rack, or the rows it needs are taken or too small for it."
                                + " Use a rack of its era or a newer one, or free those rows.",
                        "The message names another kind of rack, such as \"This chassis belongs in a Server"
                                + " Rack\".",
                        "Put it in the rack the message names.",
                        "The message says \"This chassis is of a later era than this rack\".",
                        "Put it in a rack of its own era or a later one.")
                .seeAlso("jsc:rack_drive_bays", "jsc:data_cables")
                .register();
    }

    private static void serverTypes(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("server_types", section).titled("Server types")
                .paragraph("Each server is a chassis of its own kind. The chassis sets its height, its drive bays, and"
                        + " how many processors and expansion slots it has. When you pick a server, this table tells"
                        + " you what you get. Every server and server case comes from the creative tabs of J's"
                        + " Computers.")
                .paragraph("Every era has three: a 1U server, a 2U Storage Server with the most drive bays and a 2U"
                        + " Compute Server with the most processors. A newer era's has more of what it is built for."
                        + " A server still runs only as many processors as its board has sockets.")
                .tableFrom(RackFacts.SERVERS, "Servers", "Server", "Height", "Drive bays", "Processors",
                        "Expansion slots", "Era")
                .paragraph("A server fits a rack of its own era or a newer one. A Vintage Server seats in a Standard"
                        + " Server Rack, but a Standard server never seats in a Vintage rack. Supercomputer Nodes go"
                        + " in a Supercomputer Rack, and the Advanced Supercomputer Node goes in an Advanced"
                        + " Supercomputer Rack.")
                .paragraph("A server's expansion slots take its cards, the storage cards among them: a RAID controller"
                        + " that joins its drives into one volume and a read cache that answers them faster"
                        + " ([](jsc:raid_arrays)).")
                .paragraph("Each rack unit has five front slots. A machine fills the slots of its rows in order with"
                        + " its drive bays, and the rest are blocked. When a slot is blocked, the screen says why,"
                        + " with one of two fixed messages.")
                .ifSomethingGoesWrong("A slot is blocked: \"Blocked - no unit in this row cables these slots\".",
                        "Mount a unit in that row that cables those slots, or pick a chassis that fits the row.",
                        "A slot is blocked: \"Blocked - the chassis does not cable this slot\".",
                        "Use a chassis with more drive bays.")
                .seeAlso("jsc:server_racks", "jsc:rack_drive_bays")
                .register();
    }

    private static void rackDriveBays(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rack_drive_bays", section).titled("Drive bays and hot-swap")
                .paragraph("A drive bay holds one disk. You can pull the disk out of its bay and put it back while"
                        + " the machine is on, which is called hot-swap.")
                .define("Hot-swap", "Taking a part out of a machine, or putting one in, while the machine stays on.")
                .paragraph("The bays belong to the rack, not to the server. A drive stays in its bay when the server is"
                        + " pulled out, and the next server mounted over those rows inherits its drives.")
                .paragraph("A drive bay takes only a disk of the rack's era or an older one, as a computer's disk slots"
                        + " do ([](jsc:disks)).")
                .paragraph("Drives can join into one volume, called RAID. Depending on the mode, a RAID array keeps"
                        + " working when a drive is lost ([](jsc:raid_arrays)).")
                .define("RAID", "A way of joining several drives into one volume, with or without a copy of the data.")
                .paragraph("What happens when you pull a drive depends on the bay it is in.")
                .table("Pulling a drive", "Bay", "What happens")
                .row(GuideCell.words("An independent drive"),
                        GuideCell.words("It leaves with the items on it."))
                .row(GuideCell.words("A RAID 0 array"),
                        GuideCell.words("The whole array loses its items."))
                .row(GuideCell.words("A RAID 1 or RAID 5 array"),
                        GuideCell.words("The array keeps working while a replacement drive rebuilds it."))
                .paragraph("In a RAID 1 or RAID 5 array, a pulled drive's contents move to the drives that stay, as"
                        + " far as those drives have room for them.")
                .paragraph("A rebuild takes 20 ticks for each 1,000 items of array capacity. It never takes less than"
                        + " 40 ticks, which is 2 seconds, or more than 3 minutes. While it runs, the rack shows REBUILD"
                        + " and a percentage. The rack also marks an array DEGRADED or FAILED.")
                .ifSomethingGoesWrong("The bay refuses the item.",
                        "A drive bay takes only a disk of the rack's era or an older one. Hold the item over the bay"
                                + " to read which rule keeps it out.",
                        "The array lost everything.",
                        "A RAID 0 array keeps no copy, so it loses everything when a drive is lost. Use RAID 1 or"
                                + " RAID 5 for a copy"
                                + " ([](jsc:raid_arrays)).")
                .seeAlso("jsc:server_types", "jsc:disks")
                .register();
    }

    private static void raidArrays(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("raid_arrays", section).titled("RAID modes").icon(ComputingModule.VAULTIS_ARRAY_PCIE3)
                .coversAll(ComputersGuide.hardwareOf(StorageCardSpec.class))
                .paragraph("A RAID controller is an expansion card: the Vaultis Array of each era, in a server's"
                        + " expansion slot. It joins the server's drives into one array, and until a mode is set, the"
                        + " drives stay separate. You set the array mode in the server's firmware, on the storage page"
                        + " of the Setup ([](jsc:firmware)). The card keeps the mode, so it goes with the card.")
                .tableFrom(RackFacts.STORAGE_CARDS, "Storage cards", "Card", "What it is", "Slot", "Era",
                        "Read latency cut")
                .table("Array modes", "Mode", "Drives needed", "Survives a lost drive", "Throughput")
                .row(GuideCell.words("NONE"), GuideCell.words("1"), GuideCell.words("No"),
                        GuideCell.words("100%"))
                .row(GuideCell.words("RAID 0"), GuideCell.words("2"), GuideCell.words("No"),
                        GuideCell.words("125%"))
                .row(GuideCell.words("RAID 1"), GuideCell.words("2"),
                        GuideCell.words("Yes, down to one drive"), GuideCell.words("100%"))
                .row(GuideCell.words("RAID 5"), GuideCell.words("3"),
                        GuideCell.words("Yes, one drive"), GuideCell.words("100%"))
                .paragraph("The usable space depends on the mode. RAID 0 uses the space of all its drives. RAID 1 gives"
                        + " the space of its smallest drive. RAID 5 gives the space of its drives but one, at the size"
                        + " of the smallest. RAID 0 never rebuilds, since it keeps no copy. The screen shows \"+25%"
                        + " throughput\" for RAID 0, and \"survives to 1 drive\" for RAID 1.")
                .steps("Open the Setup of the server and go to its storage page.",
                        "Press Up or Down until the mode you want shows under ARRAY MODE.",
                        "Press [[Enter]] to apply it.")
                .paragraph("The read cache, the Vaultis Cache of each era, needs no mode. In a server's expansion slot"
                        + " it cuts the read latency of the server's drives, by more in each newer era, as the table"
                        + " shows. Outside a server, a storage card sits in its slot and does nothing.")
                .ifSomethingGoesWrong("The server's drives do not join.",
                        "No mode is set, so the drives stay independent. Set a mode on the storage page.",
                        "The firmware says \"No storage controller fitted.\"",
                        "Seat a Vaultis Array card in one of this machine's expansion slots.",
                        "The firmware says the mode needs more drives, such as \"needs 3 drives\".",
                        "Add drives to the server's bays, or choose a mode that needs fewer.")
                .seeAlso("jsc:rack_drive_bays", "jsc:firmware")
                .register();
    }

    private static void rackEquipment(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rack_equipment", section).titled("Rack equipment").icon(ComputingModule.KVM_SWITCH)
                .coversAll(ComputersGuide.itemsOf(RackUnitItem.class))
                .paragraph("Not everything in a rack computes. Some units serve the rack: the KVM Switch (keyboard,"
                        + " video, mouse), the Rack UPS (uninterruptible power supply), the Cooling Unit and the Fibre"
                        + " Optic Interface take rack units, as a server does.")
                .table("Rack units that are not computers", "Unit", "Takes", "What it does")
                .row(GuideCell.words("KVM Switch"), GuideCell.words("1 U"),
                        GuideCell.words("Lets one monitor reach every machine in the rack"))
                .row(GuideCell.words("Rack UPS"), GuideCell.words("1 U"),
                        GuideCell.words("Takes the rack unit, and adds nothing to the heat budget"))
                .row(GuideCell.words("Cooling Unit"), GuideCell.words("1 U"),
                        GuideCell.words("Adds to the heat budget, more in each newer era"))
                .row(GuideCell.words("Fibre Optic Interface"), GuideCell.words("1 U"),
                        GuideCell.words("Lets the rack's back take the Fibre Optic Cable"))
                .row(GuideCell.words("Advanced Fibre Optic Interface"), GuideCell.words("1 U"),
                        GuideCell.words("Lets the rack's back take the Fibre Optic Cable and the OM5 Fibre Cable"))
                .paragraph("The KVM Switch, the Rack UPS and the Cooling Unit come in every era, from the Vintage to"
                        + " the Advanced. A rack seats the units of its own era and of the earlier ones, as it does"
                        + " servers: a Vintage KVM Switch goes in a Standard rack, a Standard one never in a Vintage"
                        + " rack.")
                .paragraph("The KVM Switch lets one monitor reach every machine in the rack, so you do not need a"
                        + " screen for each. A rack with two or more computers needs one to choose between them.")
                .paragraph("The Cooling Unit raises the heat the rack can carry, a newer era's more than an older"
                        + " one's, as explained in ([](jsc:rack_cooling_and_heat)).")
                .paragraph("From the Standard on, the backbone is fibre, and a rack takes it only through a Fibre Optic"
                        + " Interface. The Standard one takes the Fibre Optic Cable; the Advanced one, which only an"
                        + " Advanced rack seats, takes the OM5 Fibre Cable as well. One in any row of the rack is"
                        + " enough, and the servers in it need no network card ([](jsc:data_cables)).")
                .paragraph("A server's storage cards go in its expansion slots, not in the rack: the RAID controller"
                        + " joins the server's drives into one array, and the read cache cuts the time each read from"
                        + " them takes ([](jsc:raid_arrays)).")
                .ifSomethingGoesWrong("A rack with several computers has no way to pick one on the monitor.",
                        "Mount a KVM Switch in the rack. It takes one rack unit.",
                        "A unit does not go in, and the rack says it belongs in a newer rack.",
                        "The unit is of a newer era than the rack. Use the rack's own era's unit or an older one.",
                        "The OM5 Fibre Cable does not join the rack.",
                        "The rack holds the Standard Fibre Optic Interface, which takes only the Fibre Optic Cable."
                                + " Mount the Advanced one, in an Advanced rack.",
                        "The RAID controller does nothing.",
                        "Its server has no mode set, or fewer drives than the mode needs ([](jsc:raid_arrays)).")
                .seeAlso("jsc:server_racks", "jsc:rack_cooling_and_heat")
                .register();
    }

    private static void rackCoolingAndHeat(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rack_cooling_and_heat", section).titled("Cooling and heat")
                .paragraph("When you fill a rack, it can carry only so much heat before its machines slow down. Every"
                        + " Cooling Unit in the rack counts toward that limit, wherever it sits.")
                .define("Heat budget", "The watts of heat that a rack can carry before its machines slow down.")
                .paragraph("The heat budget is 1,000 W plus what each Cooling Unit adds, by its era. The load is the"
                        + " power draw of every machine whose bay is on: its processors and cards, added up.")
                .tableFrom(RackFacts.COOLING_UNITS, "Cooling Units", "Unit", "Era", "Adds to the heat budget")
                .paragraph("When the load is over the heat budget, every machine slows down by the same share. The"
                        + " share is 100 times the budget, divided by the load, as a percent, and it never falls below"
                        + " 25%. A hot rack slows down; it does not stop.")
                .table("Share of speed for some loads", "Load", "Heat budget", "Share of speed")
                .row(GuideCell.words("1,000 W"), GuideCell.words("1,000 W"), GuideCell.words("100%"))
                .row(GuideCell.words("2,000 W"), GuideCell.words("1,000 W"), GuideCell.words("50%"))
                .row(GuideCell.words("4,000 W"), GuideCell.words("1,000 W"), GuideCell.words("25%"))
                .row(GuideCell.words("8,000 W"), GuideCell.words("1,000 W"),
                        GuideCell.words("25%, the lowest share"))
                .row(GuideCell.words("5,000 W"), GuideCell.words("2,500 W, with one Standard Cooling Unit"),
                        GuideCell.words("50%"))
                .paragraph("The rack screen shows THROTTLED and the share while the rack is over its budget. The"
                        + " slowdown cuts the capacity of each server, including the bonus from its graphics cards.")
                .ifSomethingGoesWrong("The rack shows THROTTLED.",
                        "Switch off the bays of the machines you do not need, or add a Cooling Unit"
                                + " ([](jsc:rack_equipment)).")
                .seeAlso("jsc:rack_equipment", "jsc:server_racks")
                .register();
    }

    private static void subframes(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("subframes", section).titled("Subframes")
                .paragraph("A Subframe is a computer that helps the Mainframe of its network carry the work. When your"
                        + " Mainframe is alone, it does all the work itself, with its own capacity and queues.")
                .paragraph("A Subframe lends a share of its capacity. By default that share is 60%, set by the Subframe"
                        + " share in the mod's config. It lends its queues too, which are its graphics cards, so a"
                        + " Subframe without graphics cards lends capacity only.")
                .paragraph("A Subframe works only for a Mainframe that runs the same engine as it does"
                        + " ([](jsc:engines)). A Subframe without a Mainframe lends nothing.")
                .ifSomethingGoesWrong("The screen says \"No Subframes on this network.\"",
                        "The network has no Subframe, so the Mainframe does all the work by itself.",
                        "The screen says a Subframe will take no work until it runs the same engine too.",
                        "Make it run the same engine as its Mainframe ([](jsc:engines)).")
                .seeAlso("jsc:states_and_queues", "jsc:engines")
                .register();
    }

    private static void clusterManagementComputers(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("cluster_management_computers", section)
                .titled("Cluster Management Computers")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .coversAll(ComputersGuide.blocksOf(ClusterManagementComputerBlock.class))
                .paragraph("When you have one rack, you walk up to each server. When you have ten, you will not want"
                        + " to. The Cluster Management Computer installs systems and programs on the machines of your"
                        + " racks, and switches their bays on and off, all from one desk.")
                .define("Cluster", "A group of machines that one computer manages as one set.");
        clusterScene(page);
        page.paragraph("Build it as you would a Personal Computer ([](jsc:personal_computers)), and give it a"
                        + " Cluster Interface Card. Without one, it is an ordinary computer"
                        + " ([](jsc:expansion_cards)). With one, it is the manager of its cluster: each card reaches a"
                        + " different set of machines, and installs on a set number of them at once.")
                .define("Datacenter", "The Server Racks that one Server Router runs, grouped into sections behind its"
                        + " faces.")
                .table("Cluster Interface Cards", "Card", "Era", "Reaches", "Nodes at once")
                .row(GuideCell.words("Serial Console Card"), GuideCell.words("Vintage"),
                        GuideCell.words("Server Racks"), GuideCell.words("1"))
                .row(GuideCell.words("Management NIC"), GuideCell.words("Legacy"),
                        GuideCell.words("Server Racks and Supercomputer Racks"), GuideCell.words("2"))
                .row(GuideCell.words("Fabric Host Adapter"), GuideCell.words("Standard"),
                        GuideCell.words("Every rack type"), GuideCell.words("4"))
                .row(GuideCell.words("Fabric DPU"), GuideCell.words("Advanced"),
                        GuideCell.words("Every rack type"), GuideCell.words("8"))
                .paragraph("The card decides what the manager reaches, not the era of the computer. The Serial Console"
                        + " Card of the Vintage era already reaches Server Racks, and every card reaches them.")
                .paragraph("A job installs on several nodes at once, as the card allows. Each node takes its own"
                        + " install time: 280 ticks at 2 GHz, which is 14 seconds. A faster processor takes less time,"
                        + " but never under 60 ticks, which is 3 seconds, or over 1,200 ticks, which is 60 seconds.")
                .paragraph("ALL ON and ALL OFF switch every bay of the cluster at once. Each node also has its own bay,"
                        + " which reads \"Bay off\" when it is switched off.")
                .paragraph("A system or a program comes from a disc in a reader that is linked to the manager by cable"
                        + " ([](jsc:drives)). The manager looks for a system disc and a program disc in those linked"
                        + " readers.")
                .table("Messages of the Cluster Manager", "Message", "What it means", "Fix")
                .row(GuideCell.words("bay off"), GuideCell.words("The node's bay is switched off."),
                        GuideCell.words("Switch its bay on, or use ALL ON."))
                .row(GuideCell.words("already installed"),
                        GuideCell.words("The node already has that system or program."),
                        GuideCell.words("Nothing to do."))
                .row(GuideCell.words("no system"),
                        GuideCell.words("A program needs a system on the node first."),
                        GuideCell.words("Install a system on the node first ([](jsc:installing))."))
                .row(GuideCell.words("too old for the program"),
                        GuideCell.words("The node's era is older than the program allows."),
                        GuideCell.words("Use a node of a newer era."))
                .row(GuideCell.words("does not meet the program's requirements"),
                        GuideCell.words("The node lacks what the program asks for."),
                        GuideCell.words("Use a node that meets them."))
                .row(GuideCell.words("that system installs by hand from its own shell"),
                        GuideCell.words("The manager cannot install this system."),
                        GuideCell.words("Install it on that node by hand ([](jsc:installing))."))
                .row(GuideCell.words("that disc carries nothing to install"),
                        GuideCell.words("The disc holds no system or program."),
                        GuideCell.words("Put in a disc that holds one."))
                .row(GuideCell.words("no system disc in a linked reader"),
                        GuideCell.words("No linked reader holds a system disc."),
                        GuideCell.words("Put a system disc in a reader linked by cable."))
                .row(GuideCell.words("no program disc in a linked reader"),
                        GuideCell.words("No linked reader holds a program disc."),
                        GuideCell.words("Put a program disc in a reader linked by cable."))
                .row(GuideCell.words("a job is already running"),
                        GuideCell.words("Only one job runs at a time."),
                        GuideCell.words("Wait for it to finish, or cancel it."))
                .row(GuideCell.words("this card does not reach that cluster"),
                        GuideCell.words("The card's reach stops short of that cluster."),
                        GuideCell.words("Use a card from the table above that reaches it."))
                .row(GuideCell.words("no cluster interface card"),
                        GuideCell.words("The computer has no card."),
                        GuideCell.words("Put a Cluster Interface Card in the computer."))
                .row(GuideCell.words("changed while writing"),
                        GuideCell.words("The node changed during the job."),
                        GuideCell.words("Run the job again."))
                .ifSomethingGoesWrong("The Cluster Manager reaches no machine.",
                        "The computer has no Cluster Interface Card, or the card does not reach the machines."
                                + " The table above says what each card reaches.",
                        "The manager says \"A Cluster Interface Card is required.\"",
                        "Put a Cluster Interface Card in the computer.")
                .seeAlso("jsc:expansion_cards", "jsc:datacenters")
                .register();
    }

    private static void supercomputers(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("supercomputers", section).titled("Supercomputers")
                .paragraph("A supercomputer is every Supercomputer Rack tied together by a fabric cable, meeting the"
                        + " network at one HBW Interface ([](jsc:hbw_interface)). The Standard and the Advanced have"
                        + " an interface of their own.")
                .paragraph("The fabric cables are the InfiniBand Cable (Transition), the High Compute Cable (Standard)"
                        + " and the OSFP Cable (Advanced).");
        supercomputerScene(page);
        page.paragraph("Each Supercomputer Rack holds Supercomputer Nodes, which are full computers. A node adds"
                        + " crafts only when it holds a Phi that fits its slot, is assembled, and has its bay on."
                        + " The Integra Phi co-processors come in four models.")
                .define("Co-processor", "A card that adds extra processing to a node, such as the Integra Phi series.")
                .table("Integra Phi co-processors", "Co-processor", "Slots it fits", "Cores", "Power draw")
                .row(GuideCell.words("Integra Phi 5100"), GuideCell.words("1 to 2"),
                        GuideCell.words("60 cores at 1.05 GHz"), GuideCell.words("225 W"))
                .row(GuideCell.words("Integra Phi 7120"), GuideCell.words("1 to 3"),
                        GuideCell.words("61 cores at 1.24 GHz"), GuideCell.words("250 W"))
                .row(GuideCell.words("Integra Phi 7290"), GuideCell.words("1 to 4"),
                        GuideCell.words("72 cores at 1.5 GHz"), GuideCell.words("270 W"))
                .row(GuideCell.words("Integra Phi 9000"), GuideCell.words("1 to 6"),
                        GuideCell.words("96 cores at 1.8 GHz"), GuideCell.words("300 W"))
                .paragraph("A Phi's tooltip gives its slots as \"Supercomputer slots 1-2\" for the Integra Phi 5100."
                        + " The nodes are counted across the racks on the run, from the top down, and the first six"
                        + " nodes are the rated slots, numbered 1 to 6.")
                .paragraph("Each slot gives parallel crafts: slot 1 gives 8 crafts, slot 2 gives 16 crafts, doubling"
                        + " up to slot 6 with 256 crafts. Together the six slots give up to 504 parallel crafts. Each"
                        + " slot shows its state:")
                .table("What a slot shows", "Slot shows", "What it means")
                .row(GuideCell.words("NO PHI CARD"), GuideCell.words("The node holds no Phi."))
                .row(GuideCell.words("RATING LOW"),
                        GuideCell.words("The Phi is rated only for the slots before this one."))
                .row(GuideCell.words("BAY OFF"), GuideCell.words("The node's bay is switched off."))
                .row(GuideCell.words("NO NODE"), GuideCell.words("No node sits in that slot."))
                .paragraph("The supercomputer is ONLINE when its interface is on a network and at least one slot runs"
                        + " crafts. A slot runs crafts when its node holds a Phi that fits, it is assembled, and its"
                        + " bay is on. An operation can hold several slots, as many as are free, and the Mainframe"
                        + " shows the crafts in use out of the total.")
                .ifSomethingGoesWrong("The supercomputer is not ONLINE.",
                        "Check the slots: each needs a node with a Phi that fits, is assembled, and has its bay on.",
                        "A slot says RATING LOW.",
                        "Use a Phi whose rated slots reach that far.",
                        "A node past six adds nothing.",
                        "Only the first six nodes count, in rack order from the top. Put the ones you want first.")
                .seeAlso("jsc:server_racks", "jsc:hbw_interface")
                .register();
    }

    private static void hbwInterface(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("hbw_interface", section).titled("HBW Interface").icon(ComputingModule.HBW_INTERFACE)
                .coversAll(ComputersGuide.blocksOf(HbwInterfaceBlock.class))
                .paragraph("A supercomputer is not one block. It is every Supercomputer Rack tied together by a fabric"
                        + " cable, and you meet it at the HBW Interface, the one point where it meets the rest of the"
                        + " network through the backbone.")
                .paragraph("The Standard has one, and the Advanced has another that takes the OSFP cable.")
                .paragraph("The interface looks along the high compute cable from it, through up to 256 cable steps,"
                        + " and counts each Supercomputer Rack on that run. Racks on any other run do not count.")
                .steps("Join the supercomputer racks to the interface with high compute cable.",
                        "Join the interface to the network's backbone.")
                .ifSomethingGoesWrong("The supercomputer is not seen.",
                        "Its racks are not all on the high compute cable that reaches the interface. Join every rack to"
                                + " that same high compute run.")
                .seeAlso("jsc:data_cables", "jsc:supercomputers")
                .register();
    }

    private static void datacenters(final ModGuide guide, final ModGuide.SectionRef section) {
        final ModGuide.EntryBuilder page = guide.page("datacenters", section).titled("Datacenters")
                .paragraph("When you have many Server Racks, you group them into a datacenter. A Server Router does the"
                        + " grouping: it splits the racks into sections.")
                .paragraph("A section is the racks that one output face of a Server Router reaches, without crossing"
                        + " back through the router. The face joining the router to the network is its uplink, and the"
                        + " router detects it. Up to five sections hang off one router, one for each other face.")
                .define("Uplink", "The face of a Server Router that joins it to the network.")
                .define("Datacenter section", "The Server Racks that one output face of a Server Router reaches.");
        datacenterScene(page);
        page.paragraph("The label of a section shows its racks (R) and its servers (S). A section's default"
                        + " name is the router's name and its face.")
                .paragraph("A Server Router has one budget for all its sections together. At its tier, T3, it manages 8"
                        + " Server Racks in all, counted section by section. Racks past the eighth are counted as"
                        + " unmanaged, and the router shows its rack count in red when it goes over.")
                .paragraph("More faces do not add budget. To manage more racks, use a second Server Router, which has a"
                        + " budget of its own.")
                .ifSomethingGoesWrong("The rack count shows in red.",
                        "The router manages 8 racks at most. Use a second Server Router for the others.",
                        "A section has racks that are unmanaged.",
                        "Take racks off that router, or put some on a second Server Router"
                                + " ([](jsc:server_router)).")
                .seeAlso("jsc:server_router", "jsc:load_balancing")
                .register();
    }

    private static void serverRouter(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("server_router", section).titled("Server Router").icon(ComputingModule.SERVER_ROUTER)
                .coversAll(ComputersGuide.blocksOf(ServerRouterBlock.class))
                .paragraph("The Server Router is how you organise a datacenter. It groups Server Racks into sections,"
                        + " one for each of its faces besides the uplink ([](jsc:datacenters)). Its faces take any data"
                        + " cable, of any era ([](jsc:data_cables)).")
                .steps("Join its uplink face to the network.",
                        "Join a section of racks to each of its other faces.",
                        "Open it to see each section and its rack count.")
                .paragraph("A Server Router manages up to 8 Server Racks in all, across sections. Racks past that are"
                        + " counted as unmanaged, and the router shows its rack count in red.")
                .paragraph("The Cluster Manager runs each section as one machine, as far as its card reaches"
                        + " ([](jsc:cluster_management_computers)).")
                .ifSomethingGoesWrong("The rack count shows in red.",
                        "More than 8 racks sit on the router. Move some to a second Server Router, which has its own"
                                + " budget.")
                .seeAlso("jsc:datacenters", "jsc:load_balancing")
                .register();
    }

    private static void loadBalancing(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("load_balancing", section).titled("Load balancing")
                .paragraph("Load balancing decides where a new item goes when several servers could take it. Each"
                        + " section of a Server Router has its own balancing mode, and the button on the router moves a"
                        + " section to the next mode.")
                .define("Load balancing", "Sharing new items among the servers, so that no single server fills first.")
                .table("Balancing modes", "Mode", "Screen label", "What a new item does")
                .row(GuideCell.words("Round robin"), GuideCell.words("ROUND-ROBIN"),
                        GuideCell.words("The next server in turn. The start rotates for each face."))
                .row(GuideCell.words("Least loaded"), GuideCell.words("LEAST-LOADED"),
                        GuideCell.words("Sorts the servers by free space, most free first, and fills in that order."))
                .row(GuideCell.words("Manual"), GuideCell.words("MANUAL"),
                        GuideCell.words("Fills the servers in the order of the list."))
                .paragraph("Every section starts in ROUND-ROBIN, and you see each section's mode on the router. The"
                        + " router keeps its place per face, so in ROUND-ROBIN each new write takes the next server"
                        + " in turn.")
                .paragraph("A server can also run the Load Balancer service: a program that spreads writes over the"
                        + " bay's drives, instead of filling them one after another ([](jsc:programs)).")
                .ifSomethingGoesWrong("All the items go to one server.",
                        "If the section is in MANUAL, it fills the first server in the list. Press the button until"
                                + " it reads ROUND-ROBIN or LEAST-LOADED.",
                        "The bay's drives fill one after another.",
                        "Run the Load Balancer service on that server.")
                .seeAlso("jsc:server_router", "jsc:programs")
                .register();
    }

    private static void mainframeScene(final ModGuide.EntryBuilder entry) {
        entry.scene("A Standard Mainframe, running, with its service panel off so that its parts show. Drag it to"
                + " turn it.", 130);
        entry.place(16, 0, 16, () -> ComputingModule.MAINFRAME.get(), "facing=south,optical=false", MAINFRAME_DATA);
        /* The cabinet is drawn whole by its origin; its other eleven blocks only hold the places. */
        for (int up = 0; up < 2; up++) {
            for (int back = 0; back < 2; back++) {
                for (int across = -1; across <= 1; across++) {
                    if (up == 0 && back == 0 && across == 0) {
                        continue;
                    }
                    entry.place(16 + across, up, 16 - back, () -> ComputingModule.MAINFRAME_PART.get(),
                            "facing=south,core=" + (across == 0) + ",era=3,optical=false", "");
                }
            }
        }
        entry.balloon(1, GuideBlock.ScenePart.of(15, 1, 16).box(4, 6, 5, 16, 8, 11), "Processors",
                "Their capacity adds up to the speed of the network.");
        entry.balloon(2, GuideBlock.ScenePart.of(15, 1, 16).box(4, 1, 5, 15, 5, 11), "Memory",
                "Its size sets the RAM buffer.");
        entry.balloon(3, GuideBlock.ScenePart.of(15, 0, 16).box(4, 11, 5, 12, 16, 11), "Graphics cards",
                "Each card runs one more queue.");
        entry.balloon(4, GuideBlock.ScenePart.of(15, 0, 16).box(4, 6, 5, 11, 11, 11), "Power supply");
        entry.balloon(5, GuideBlock.ScenePart.of(17, 0, 16).box(0, 7, 4, 12, 14, 11), "Disks");
        entry.balloon(6, GuideBlock.ScenePart.of(16, 0, 16).box(7, 3, 14, 9, 6, 16), "Power button",
                "Click it to switch the Mainframe on or off.");
        entry.balloon(7, GuideBlock.ScenePart.of(17, 1, 16).box(0, 9, 14, 11, 11, 16), "Status lamps",
                "They show that it runs, that it is on a network, and when it has a fault.");
    }

    private static void serverRackScene(final ModGuide.EntryBuilder entry) {
        entry.scene("A Server Rack with its rack units filled: a server, a KVM Switch, a Storage Server, another"
                + " server, a Cooling Unit and a Rack UPS. Drag it to turn it.", 130);
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 16, 16, "south", false, rackData(false, MIXED_RACK));
        entry.balloon(1, GuideBlock.ScenePart.of(15, 2, 16).box(6, 9, 14, 16, 14, 16), "Server",
                "Click the front of the rack with a server in your hand to mount it.");
        entry.balloon(2, GuideBlock.ScenePart.of(15, 2, 16).box(6, 4, 14, 16, 8, 16), "KVM Switch",
                "It lets one monitor reach every machine in the rack.");
        entry.balloon(3, GuideBlock.ScenePart.of(15, 1, 16).box(6, 4, 14, 16, 13, 16), "Storage Server",
                "It takes two rack units, and its ten drive bays hold the disks.");
        entry.balloon(4, GuideBlock.ScenePart.of(15, 0, 16).box(6, 8, 14, 16, 13, 16), "Cooling Unit",
                "This Standard one adds 1,500 W to the heat the rack can carry.");
        entry.balloon(5, GuideBlock.ScenePart.of(15, 0, 16).box(6, 3, 14, 16, 7, 16), "Rack UPS");
    }

    private static void supercomputerScene(final ModGuide.EntryBuilder entry) {
        entry.scene("A supercomputer: two Supercomputer Racks of nodes, joined by High Compute Cable to the HBW"
                + " Interface, which meets the network. Drag it to see the cables behind the racks.", 130);
        final String nodes = rackData(true, NODE_RACK);
        rackAt(entry, () -> ComputingModule.SUPERCOMPUTER_RACK.get(), 14, 16, "south", true, nodes);
        rackAt(entry, () -> ComputingModule.SUPERCOMPUTER_RACK.get(), 11, 16, "south", true, nodes);
        /*
         * The fabric runs along the top of the racks' backs, where the first view can see it past the cabinets, and
         * drops to the interface.
         */
        cableAt(entry, 10, 2, 14, FABRIC_CABLE, FABRIC_LANE, EAST | plugged(SOUTH));
        cableAt(entry, 11, 2, 14, FABRIC_CABLE, FABRIC_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 12, 2, 14, FABRIC_CABLE, FABRIC_LANE, WEST | EAST);
        cableAt(entry, 13, 2, 14, FABRIC_CABLE, FABRIC_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 14, 2, 14, FABRIC_CABLE, FABRIC_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 15, 2, 14, FABRIC_CABLE, FABRIC_LANE, WEST | DOWN);
        cableAt(entry, 15, 1, 14, FABRIC_CABLE, FABRIC_LANE, UP | DOWN);
        cableAt(entry, 15, 0, 14, FABRIC_CABLE, FABRIC_LANE, UP | plugged(EAST));
        entry.place(16, 0, 14, () -> ComputingModule.HBW_INTERFACE.get(), "", "");
        cableAt(entry, 17, 0, 14, BACKBONE_CABLE, BACKBONE_LANE, plugged(WEST) | plugged(EAST));
        entry.place(18, 0, 14, () -> ComputingModule.STANDARD_ROUTER.get(), "", "");
        entry.view(-35, 25, 1.35F);
        entry.balloon(1, GuideBlock.ScenePart.of(13, 2, 16).box(6, 4, 13, 16, 14, 16), "Supercomputer Node",
                "A full computer of two rack units, with its own co-processor.");
        entry.balloon(2, GuideBlock.ScenePart.of(14, 1, 16), "Supercomputer Rack");
        entry.balloon(3, GuideBlock.ScenePart.of(12, 2, 14), "High Compute Cable",
                "It ties every rack of the supercomputer to the interface.");
        entry.balloon(4, GuideBlock.ScenePart.of(16, 0, 14), "HBW Interface",
                "The one point where the supercomputer meets the network.");
        entry.balloon(5, GuideBlock.ScenePart.of(17, 0, 14), "Backbone cable");
        entry.balloon(6, GuideBlock.ScenePart.of(18, 0, 14), "A router of the network");
    }

    private static void datacenterScene(final ModGuide.EntryBuilder entry) {
        entry.scene("A datacenter: a Server Router with a section of racks behind each of three faces, and its"
                + " uplink to the network on the fourth. Drag it to look at each section.", 130);
        entry.place(16, 0, 16, () -> ComputingModule.SERVER_ROUTER.get(), "facing=north", "");
        /* The uplink is the face opposite the router's facing, here the south one. */
        cableAt(entry, 16, 0, 17, BACKBONE_CABLE, BACKBONE_LANE, plugged(NORTH) | plugged(SOUTH));
        entry.place(16, 0, 18, () -> ComputingModule.STANDARD_ROUTER.get(), "", "");
        /* The west section. */
        cableAt(entry, 15, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, plugged(EAST) | WEST);
        cableAt(entry, 14, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST);
        cableAt(entry, 13, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, EAST | plugged(WEST));
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 11, 16, "west", false, rackData(false, PLAIN_RACK));
        /* The east section. */
        cableAt(entry, 17, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, plugged(WEST) | EAST);
        cableAt(entry, 18, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST);
        cableAt(entry, 19, 0, 16, BACKBONE_CABLE, BACKBONE_LANE, WEST | plugged(EAST));
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 21, 16, "east", false, rackData(false, MIXED_RACK));
        /* The north section. */
        cableAt(entry, 16, 0, 15, BACKBONE_CABLE, BACKBONE_LANE, plugged(SOUTH) | NORTH);
        cableAt(entry, 16, 0, 14, BACKBONE_CABLE, BACKBONE_LANE, SOUTH | plugged(NORTH));
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 16, 12, "north", false, rackData(false, PLAIN_RACK));
        entry.view(-35, 25, 1.35F);
        entry.balloon(1, GuideBlock.ScenePart.of(16, 0, 16), "Server Router",
                "Each of its other faces leads to a section of racks.");
        entry.balloon(2, GuideBlock.ScenePart.of(16, 0, 17), "Uplink",
                "The face that joins the router to the network.");
        entry.balloon(3, GuideBlock.ScenePart.of(11, 1, 16), "A section of racks");
        entry.balloon(4, GuideBlock.ScenePart.of(21, 1, 16), "A section of racks");
        entry.balloon(5, GuideBlock.ScenePart.of(16, 1, 12), "A section of racks");
        entry.balloon(6, GuideBlock.ScenePart.of(18, 0, 16), "Data cable");
        entry.balloon(7, GuideBlock.ScenePart.of(16, 0, 18), "The network");
    }

    private static void clusterScene(final ModGuide.EntryBuilder entry) {
        entry.scene("A Cluster Management Computer, cabled to the two Server Racks it manages. Drag it to follow the"
                + " cable behind the racks.", 130);
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 16, 16, "south", false, rackData(false, MIXED_RACK));
        rackAt(entry, () -> ComputingModule.SERVER_RACK.get(), 13, 16, "south", false, rackData(false, PLAIN_RACK));
        entry.place(18, 0, 15, () -> ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get(),
                "facing=south,optical=false", "");
        /*
         * The cable runs along the top of the racks' backs, where the first view can see it past the cabinets, and
         * drops to the computer.
         */
        cableAt(entry, 12, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, EAST | plugged(SOUTH));
        cableAt(entry, 13, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 14, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST);
        cableAt(entry, 15, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 16, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST | plugged(SOUTH));
        cableAt(entry, 17, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | EAST);
        cableAt(entry, 18, 2, 14, BACKBONE_CABLE, BACKBONE_LANE, WEST | DOWN);
        cableAt(entry, 18, 1, 14, BACKBONE_CABLE, BACKBONE_LANE, UP | DOWN);
        cableAt(entry, 18, 0, 14, BACKBONE_CABLE, BACKBONE_LANE, UP | plugged(SOUTH));
        entry.balloon(1, GuideBlock.ScenePart.of(18, 0, 15), "Cluster Management Computer",
                "It installs systems and programs on the machines of the racks, and switches their bays.");
        entry.balloon(2, GuideBlock.ScenePart.of(16, 1, 16), "A Server Rack it manages");
        entry.balloon(3, GuideBlock.ScenePart.of(13, 1, 16), "A Server Rack it manages");
        entry.balloon(4, GuideBlock.ScenePart.of(14, 2, 14), "Backbone cable",
                "It joins the computer to the back of every rack.");
    }

    /**
     * A rack and the eleven blocks of its cabinet that only hold the places, standing where its origin stands. The
     * cabinet is two blocks across, to the right of the origin, two deep, behind it, and three tall.
     */
    private static void rackAt(final ModGuide.EntryBuilder entry, final Supplier<? extends Block> cabinet,
                               final int x, final int z, final String facing, final boolean compute,
                               final String data) {
        final int turn = FACINGS.indexOf(facing);
        final int rightX = FACING_X[(turn + 1) % FACINGS.size()];
        final int rightZ = FACING_Z[(turn + 1) % FACINGS.size()];
        final int backX = FACING_X[(turn + 2) % FACINGS.size()];
        final int backZ = FACING_Z[(turn + 2) % FACINGS.size()];
        entry.place(x, 0, z, cabinet, "facing=" + facing, data);
        for (int up = 0; up < 3; up++) {
            for (int across = 0; across < 2; across++) {
                for (int deep = 0; deep < 2; deep++) {
                    if (up == 0 && across == 0 && deep == 0) {
                        continue;
                    }
                    final String state = "facing=" + facing + ",top=" + (up == 2) + ",front=" + (up < 2 && deep == 0)
                            + ",compute=" + compute;
                    entry.place(x + rightX * across + backX * deep, up, z + rightZ * across + backZ * deep,
                            () -> ComputingModule.SERVER_RACK_PART.get(), state, "");
                }
            }
        }
    }

    /** What a rack draws from: the unit in each row from the top, the bays all on, and the livery panel on or off. */
    private static String rackData(final boolean panelOff, final int... units) {
        final StringBuilder bytes = new StringBuilder();
        for (final int unit : units) {
            if (bytes.length() > 0) {
                bytes.append(',');
            }
            bytes.append(unit).append('b');
        }
        return "{Units:[B;" + bytes + "],DisplayEra:3,BayPowerOff:0,ServicePanelOff:" + (panelOff ? "1b" : "0b") + "}";
    }

    private static void cableAt(final ModGuide.EntryBuilder entry, final int x, final int y, final int z,
                                final String cable, final int lane, final int links) {
        final StringBuilder lanes = new StringBuilder();
        for (int i = 0; i < LANES; i++) {
            if (i > 0) {
                lanes.append(',');
            }
            lanes.append(i == lane ? links : 0);
        }
        entry.place(x, y, z, () -> CoreCables.BLOCK.get(), "",
                "{Wires:[{cable:\"" + cable + "\"}],Links:[I;" + lanes + "]}");
    }

    /** A face a wire crosses and meets a device on, which a cable's links write as two bits. */
    private static int plugged(final int face) {
        return face | face << PLUG_SHIFT;
    }
}
