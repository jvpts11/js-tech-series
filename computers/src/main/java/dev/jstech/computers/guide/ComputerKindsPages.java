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
import dev.jstech.computers.item.RackGadgetItem;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerCaseItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages about the computers the network runs: the Personal Computer and the Crafting Computer, the Mainframe
 * that runs a network, the racks and servers that hold its storage, the clusters that install and manage many
 * machines, the supercomputers, and the datacenters with the way their load is balanced.
 */
final class ComputerKindsPages {

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
        guide.page("mainframes", section).titled("Mainframes").icon(ComputingModule.MAINFRAME)
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
                .define("Capacity", "The items a tick that the Mainframe's processors can handle, added up.")
                .paragraph("The processors run one queue, and each graphics card runs one more. The queues do not"
                        + " share capacity. A weak card can hold its queue down, and the assembly screen then says"
                        + " \"A card's queue runs at most at the CPU's speed, and no faster than the card\".")
                .paragraph("The assembly screen shows these parts of the Mainframe:")
                .table("The assembly screen", "Part", "What it shows")
                .row(GuideCell.words("CAPACITY (it/t)"),
                        GuideCell.words("The items a tick its processors can handle, added up."))
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
                        + " panel. Its processors, memory and cards are seen only through that opening.")
                .paragraph("AUTO START keeps the Mainframe ready. With it on and a valid build, the Mainframe starts"
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
        guide.page("server_racks", section).titled("Server racks and servers")
                .icon(ComputingModule.SERVER_RACK)
                .coversAll(ComputersGuide.blocksOf(ServerRackBlock.class))
                .coversAll(ComputersGuide.itemsOf(ServerItem.class, ServerCaseItem.class))
                .paragraph("Your items have to live somewhere. In J's Computers they live on the disks of servers,"
                        + " and the servers live in racks.")
                .define("Server", "A computer built to sit in a Server Rack.")
                .paragraph("A Server Rack is a cabinet two blocks wide, three tall and two deep, with eight rack units"
                        + " of room. A machine's height is counted in those units: most servers take one, the bigger"
                        + " ones take two.")
                .define("Server Rack", "A cabinet, two blocks wide, three tall and two deep, that holds machines in"
                        + " rows.")
                .define("Rack unit (U)", "The height a machine takes in a rack, counted in units. Each cabinet has"
                        + " eight.")
                .paragraph("The Supercomputer Rack holds supercomputer nodes only, and comes in the Standard and the"
                        + " Advanced ([](jsc:supercomputers)).")
                .define("Supercomputer", "Many computers that work as one large machine, joined by high compute"
                        + " cable.")
                .paragraph("A rack seats servers of its own era or older. Each server has drive bays, which show at"
                        + " the front of the rack, and gadget bays for small parts. The"
                        + " [server types](jsc:server_types) list how many of each a server has.")
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
                        + " cable, which the rack takes up to its era ([](jsc:data_cables)).")
                .ifSomethingGoesWrong("A server does not go in.",
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
                .paragraph("Each server is a chassis of its own kind. The chassis sets its height, its drive bays and"
                        + " gadget bays, and how many processors and PCI Express slots it has. When you pick a server,"
                        + " these tables tell you what you get. Every server and server case comes from the creative"
                        + " tabs of J's Computers.")
                .table("Heights and bays", "Server", "Height", "Drive bays", "Gadget bays")
                .row(GuideCell.words("Vintage Server"), GuideCell.words("1 U"), GuideCell.words("1"),
                        GuideCell.words("0"))
                .row(GuideCell.words("Legacy Server"), GuideCell.words("1 U"), GuideCell.words("2"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Transition Server"), GuideCell.words("1 U"), GuideCell.words("4"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Server"), GuideCell.words("1 U"), GuideCell.words("3"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Advanced Server"), GuideCell.words("1 U"), GuideCell.words("4"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Storage Server"), GuideCell.words("2 U"), GuideCell.words("8"),
                        GuideCell.words("2"))
                .row(GuideCell.words("Compute Server"), GuideCell.words("2 U"), GuideCell.words("1"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Supercomputer Node"), GuideCell.words("2 U"), GuideCell.words("1"),
                        GuideCell.words("1"))
                .row(GuideCell.words("Advanced Supercomputer Node"), GuideCell.words("2 U"),
                        GuideCell.words("2"), GuideCell.words("1"))
                .table("Processors and slots", "Server", "Processors", "PCI Express slots", "Era")
                .row(GuideCell.words("Vintage Server"), GuideCell.words("1"), GuideCell.words("1"),
                        GuideCell.words("Vintage"))
                .row(GuideCell.words("Legacy Server"), GuideCell.words("2"), GuideCell.words("1"),
                        GuideCell.words("Legacy"))
                .row(GuideCell.words("Transition Server"), GuideCell.words("2"), GuideCell.words("2"),
                        GuideCell.words("Transition"))
                .row(GuideCell.words("Server"), GuideCell.words("2"), GuideCell.words("no limit"),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("Advanced Server"), GuideCell.words("2"), GuideCell.words("no limit"),
                        GuideCell.words("Advanced"))
                .row(GuideCell.words("Storage Server"), GuideCell.words("1"), GuideCell.words("1"),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("Compute Server"), GuideCell.words("4"), GuideCell.words("no limit"),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("Supercomputer Node"), GuideCell.words("2"), GuideCell.words("2"),
                        GuideCell.words("Standard"))
                .row(GuideCell.words("Advanced Supercomputer Node"), GuideCell.words("2"),
                        GuideCell.words("4"), GuideCell.words("Advanced"))
                .paragraph("A server fits a rack of its own era or a newer one. A Vintage Server seats in a Standard"
                        + " Server Rack, but a Standard server never seats in a Vintage rack. Supercomputer Nodes go"
                        + " in a Supercomputer Rack, and the Advanced Supercomputer Node goes in an Advanced"
                        + " Supercomputer Rack. A Vintage Server has no gadget bay, so no gadget fits in it.")
                .paragraph("Each rack unit has five front slots. A machine fills the slots of its rows in order: drive"
                        + " bays first, then gadget bays, and the rest are blocked. The screen says why a slot is"
                        + " blocked, in one of two ways.")
                .ifSomethingGoesWrong("A slot is blocked: \"Blocked - no unit in this row cables these slots\".",
                        "Mount a unit in that row that cables those slots, or pick a chassis that fits the row.",
                        "A slot is blocked: \"Blocked - the chassis does not cable this slot\".",
                        "Use a chassis with more drive or gadget bays.")
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
                .paragraph("A drive bay takes only a disk. A gadget bay takes only a gadget, such as the Cache Card"
                        + " ([](jsc:rack_equipment)).")
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
                        + " far as those drives accept them. Nothing is copied twice.")
                .paragraph("A rebuild takes 20 ticks for each 1,000 items of array capacity. It never takes less than"
                        + " 40 ticks, which is 2 seconds, or more than 3 minutes. While it runs, the rack shows REBUILD"
                        + " and a percentage. It marks an array DEGRADED or FAILED.")
                .ifSomethingGoesWrong("The bay refuses the item.",
                        "A drive bay takes only a disk, and a gadget bay only a gadget.",
                        "The array lost everything.",
                        "A RAID 0 array keeps nothing when a drive is lost. Use RAID 1 or RAID 5 for a copy"
                                + " ([](jsc:raid_arrays)).")
                .seeAlso("jsc:server_types", "jsc:disks")
                .register();
    }

    private static void raidArrays(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("raid_arrays", section).titled("RAID modes")
                .paragraph("A RAID Controller is a gadget for a server's gadget bay. It joins its bay's drives into"
                        + " one array, and until a mode is set, the bay's drives stay separate. You set the array mode"
                        + " in the server's firmware, on the storage page of the Setup ([](jsc:firmware)).")
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
                .paragraph("The Cache Card is a gadget that needs no RAID mode. It cuts the read latency of its bay's"
                        + " drives by 25%: the screen says \"-25% read latency in its bay\".")
                .ifSomethingGoesWrong("The drives in the bay do not join.",
                        "No mode is set, so the bay's drives stay independent. Set a mode on the storage page.",
                        "The firmware says \"No storage controller fitted.\"",
                        "Mount a RAID Controller in this machine's gadget bay.",
                        "The firmware says the mode needs more drives, such as \"needs 3 drives\".",
                        "Add drives to the bay, or choose a mode that needs fewer.")
                .seeAlso("jsc:rack_drive_bays", "jsc:firmware")
                .register();
    }

    private static void rackEquipment(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rack_equipment", section).titled("Rack equipment").icon(ComputingModule.KVM_SWITCH)
                .coversAll(ComputersGuide.itemsOf(RackGadgetItem.class, RackUnitItem.class))
                .paragraph("Not everything in a rack computes. Some units serve the rack: the KVM Switch (keyboard,"
                        + " video, mouse), the Rack UPS (uninterruptible power supply) and the Cooling Unit take rack"
                        + " units, as a server does.")
                .table("Rack units that are not computers", "Unit", "Takes", "What it does")
                .row(GuideCell.words("KVM Switch"), GuideCell.words("1 U"),
                        GuideCell.words("Lets one monitor reach every machine in the rack"))
                .row(GuideCell.words("Rack UPS"), GuideCell.words("1 U"),
                        GuideCell.words("Takes the rack unit, and adds nothing to the heat budget"))
                .row(GuideCell.words("Cooling Unit"), GuideCell.words("1 U"),
                        GuideCell.words("Adds 1,500 W to the heat budget"))
                .paragraph("The KVM Switch lets one monitor reach every machine in the rack, so you do not need a"
                        + " screen for each. A rack with two or more computers needs one to choose between them.")
                .paragraph("The Cooling Unit raises the heat the rack can carry, as explained in"
                        + " ([](jsc:rack_cooling_and_heat)).")
                .paragraph("Gadgets go in the gadget bays of a server ([](jsc:rack_drive_bays)). The RAID Controller"
                        + " joins the drives of its bay into one array, and the Cache Card cuts the time each read from"
                        + " them takes ([](jsc:raid_arrays)).")
                .ifSomethingGoesWrong("A rack with several computers has no way to pick one on the monitor.",
                        "Mount a KVM Switch in the rack. It takes one rack unit.",
                        "The RAID Controller does nothing.",
                        "Its bay has no mode set, or the bay has fewer drives than the mode needs"
                                + " ([](jsc:raid_arrays)).")
                .seeAlso("jsc:server_racks", "jsc:rack_cooling_and_heat")
                .register();
    }

    private static void rackCoolingAndHeat(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rack_cooling_and_heat", section).titled("Cooling and heat")
                .paragraph("When you fill a rack, it can carry only so much heat before its machines slow down. Passive"
                        + " cooling carries 1,000 W. Each Cooling Unit in the rack adds 1,500 W, and every Cooling Unit"
                        + " counts, wherever it sits.")
                .define("Heat budget", "The watts of heat that a rack can carry before its machines slow down.")
                .paragraph("The heat budget is 1,000 W plus 1,500 W for each Cooling Unit. The load is the power draw"
                        + " of every machine whose bay is on: its processors and cards, added up.")
                .paragraph("When the load is over the heat budget, every machine slows down by the same share. The"
                        + " share is 100 times the budget, divided by the load, as a percent, and it never falls below"
                        + " 25%. A hot rack slows down; it does not stop.")
                .table("Share of speed for some loads", "Load", "Heat budget", "Share of speed")
                .row(GuideCell.words("1,000 W"), GuideCell.words("1,000 W"), GuideCell.words("100%"))
                .row(GuideCell.words("2,000 W"), GuideCell.words("1,000 W"), GuideCell.words("50%"))
                .row(GuideCell.words("4,000 W"), GuideCell.words("1,000 W"), GuideCell.words("25%"))
                .row(GuideCell.words("8,000 W"), GuideCell.words("1,000 W"),
                        GuideCell.words("25%, the lowest share"))
                .row(GuideCell.words("5,000 W"), GuideCell.words("2,500 W, with one Cooling Unit"),
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
        guide.page("cluster_management_computers", section).titled("Cluster Management Computers")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .coversAll(ComputersGuide.blocksOf(ClusterManagementComputerBlock.class))
                .paragraph("When you have one rack, you walk up to each server. When you have ten, you will not want"
                        + " to. The Cluster Management Computer installs systems and programs on the machines of your"
                        + " racks, and switches their bays on and off, all from one desk.")
                .define("Cluster", "A group of machines that one computer manages as one set.")
                .paragraph("Build it as you would a Personal Computer ([](jsc:personal_computers)), and give it a"
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
        guide.page("supercomputers", section).titled("Supercomputers")
                .paragraph("A supercomputer is every Supercomputer Rack tied together by a fabric cable, meeting the"
                        + " network at one HBW Interface ([](jsc:hbw_interface)). The Standard and the Advanced have"
                        + " an interface of their own.")
                .paragraph("The fabric cables are the InfiniBand Cable (Transition), the High Compute Cable (Standard)"
                        + " and the OSFP Cable (Advanced).")
                .paragraph("Each Supercomputer Rack holds Supercomputer Nodes, which are full computers. A node adds"
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
                        + " nodes are the rated slots, numbered 1 to 6. A node past the sixth adds nothing.")
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
        guide.page("datacenters", section).titled("Datacenters")
                .paragraph("When you have many Server Racks, you group them into a datacenter. A Server Router does the"
                        + " grouping: it splits the racks into sections.")
                .paragraph("A section is the racks that one output face of a Server Router reaches, without crossing"
                        + " back through the router. The face joining the router to the network is its uplink, and the"
                        + " router detects it. Up to five sections hang off one router, one for each other face.")
                .define("Uplink", "The face of a Server Router that joins it to the network.")
                .define("Datacenter section", "The Server Racks that one output face of a Server Router reaches.")
                .paragraph("The label of a section shows its racks (R) and its servers (S). A section's default"
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
}
