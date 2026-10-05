/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import static dev.jstech.computers.guide.ComputersGuide.blocksOf;
import static dev.jstech.computers.guide.ComputersGuide.itemsOf;
import static dev.jstech.computers.guide.ComputersGuideTexts.FROM_THE_SHARED_TAB;
import static dev.jstech.computers.guide.ComputersGuideTexts.FROM_THE_STANDARD_TAB;
import static dev.jstech.computers.guide.ComputersGuideTexts.FROM_THE_TABS;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.ClusterManagementComputerBlock;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.HbwInterfaceBlock;
import dev.jstech.computers.block.HubBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.block.RepeaterBlock;
import dev.jstech.computers.block.RouterBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.block.SubwooferBlock;
import dev.jstech.computers.block.TankBlock;
import dev.jstech.computers.item.ClusterInterfaceCardItem;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.NetworkCardItem;
import dev.jstech.computers.item.PhiCoprocessorItem;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RackGadgetItem;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.item.ServerCaseItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.item.WorkshopCardItem;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.core.guide.ModGuide;
import java.util.List;
import net.minecraft.world.level.ItemLike;

/**
 * The entries of J's Computers' chapter, by section: the computers, their hardware, the data network, Operations,
 * storage, autocrafting, the operating systems and the peripherals. Every entry follows the spine every entry of the
 * series follows, and the families of items it covers are read from the mod's registrations, so a part added to a
 * family is the page of its family's entry with nothing more to write.
 */
final class ComputersEntries {

    private ComputersEntries() {
    }

    static void declare(final ModGuide guide) {
        computers(guide);
        hardware(guide);
        network(guide);
        operations(guide);
        storage(guide);
        autocrafting(guide);
        systems(guide);
        peripherals(guide);
    }

    private static void computers(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("computers").titled("Computers and eras")
                .icon(ComputingModule.PERSONAL_COMPUTER).register();
        guide.page("first_computer", section).titled("Your first computer").icon(ComputingModule.PERSONAL_COMPUTER)
                .covers(ComputersGuide.MANUAL)
                .whatItIs("The steps from an empty room to a computer at its desktop, for a player who has never"
                        + " used J's Computers.")
                .whatItIsFor("In J's Computers you build real computers: a case, a motherboard, a processor, memory,"
                        + " a disk, a graphics card, a monitor. You install a system on them from a disc and use"
                        + " their programs.")
                .howToGetIt("Everything is in the creative tab J's Computers - Standard: the computer, the parts, the"
                        + " monitor, the cable, the Dock Station and the install media.")
                .howToUseIt("Place a Personal Computer and use it: its window is the inside of the case.",
                        "Put in a Standard motherboard, then a processor that fits its socket, memory, a graphics"
                                + " card, a disk and a power supply.",
                        "Place a Monitor against the computer, or join it with Peripheral Cable. Do the same with a"
                                + " Dock Station.",
                        "Turn the computer on and use the monitor: with no system yet, it shows the firmware.",
                        "Put the Frames 10 install stick in the Dock Station and choose to install from it.",
                        "Follow the installer. The computer starts into its desktop.")
                .whatCanGoWrong("Using the computer opens the case, not the system.",
                        "That is how it is: the system is at the monitor. Use the monitor.",
                        "The monitor says no computer is in range.",
                        "It is not joined to the computer. Put it against it, or lay Peripheral Cable all the way.")
                .register();
        guide.page("personal_computers", section).titled("Personal Computers")
                .icon(ComputingModule.PERSONAL_COMPUTER).coversAll(blocksOf(PersonalComputerBlock.class))
                .whatItIs("Your own computer: a case you fill with parts, which runs a system and its programs.")
                .whatItIsFor("Using programs, writing your own, and reaching the network: from a Personal Computer"
                        + " you store and take items, watch the network and craft.")
                .table("What a Personal Computer holds")
                .fixed("Motherboard", "1")
                .fixed("Processors", "1")
                .fixed("Memory", "4")
                .fixed("Graphics cards", "4")
                .fixed("Disks", "2")
                .fixed("Power supply", "1")
                .fixed("Storage slots", "18")
                .howToGetIt(FROM_THE_TABS)
                .paragraph("From the Standard on it comes in three cases that differ only in look: the plain one,"
                        + " High Performance and Aesthetic.")
                .howToUseIt("Put in a motherboard of the case's era first: a case takes only its own era's boards.",
                        "Add a processor, memory, a power supply, a disk and a graphics card.",
                        "Join a monitor and turn it on.")
                .whatCanGoWrong("The window refuses the build.",
                        "It says why: no processor or memory, a processor that does not fit the socket, memory the"
                                + " board does not take, or more power than the power supply gives.",
                        "A part sits in the window but does nothing.",
                        "The board does not offer that slot. Use a board with more.")
                .register();
        guide.page("crafting_computers", section).titled("Crafting Computers")
                .icon(ComputingModule.CRAFTING_COMPUTER).coversAll(blocksOf(CraftingComputerBlock.class))
                .whatItIs("The computer that crafts for the network: it runs crafting table recipes itself and"
                        + " drives the machines that make the rest.")
                .whatItIsFor("Making what you ask the network for, from what it holds, step by step.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Build it as a Personal Computer: processor, memory, disks, power supply.",
                        "Put in a Crafting Card: without one it does not craft.",
                        "Lay Crafting Cable from it to the Crafting Interfaces of your machines.",
                        "Open the Crafting Manager on it to load patterns and watch the jobs.")
                .whatCanGoWrong("It crafts nothing.",
                        "It has no Crafting Card, or it is off.")
                .seeAlso("jsc:how_autocrafting_works", "jsc:crafting_cards")
                .register();
        guide.page("cluster_management_computers", section).titled("Cluster Management Computers")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .coversAll(blocksOf(ClusterManagementComputerBlock.class))
                .whatItIs("A computer that installs systems on the machines in your racks and watches them.")
                .whatItIsFor("Looking after many racked machines from one desk, instead of one at a time.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Build it as a Personal Computer.",
                        "Put in a Cluster Interface Card: without one it is an ordinary computer.",
                        "Open the Cluster Manager to reach the racked machines.")
                .whatCanGoWrong("The Cluster Manager reaches no machine.",
                        "The computer has no Cluster Interface Card, or the card is too old to reach them.")
                .seeAlso("jsc:cluster_interface_cards")
                .register();
        guide.page("mainframes", section).titled("Mainframes").icon(ComputingModule.MAINFRAME)
                .coversAll(blocksOf(MainframeBlock.class))
                .whatItIs("The network's brain: a large computer, three blocks wide, two tall and two deep, that keeps"
                        + " the index of everything stored, takes every request and moves the items.")
                .whatItIsFor("Every network has exactly one. How fast the whole network works comes from its"
                        + " hardware.")
                .table("What a Mainframe holds")
                .fixed("Motherboard", "1 MTX")
                .fixed("Processors", "4")
                .fixed("Memory", "8")
                .fixed("Graphics cards", "6")
                .fixed("Disks", "4")
                .fixed("Storage slots", "27")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Place its parts in the shape of 3 by 2 by 2: it forms when the shape is whole.",
                        "Give it an MTX motherboard of its era, a power supply, processors and memory.",
                        "Add graphics cards: each gives it one more queue of work done at the same time.",
                        "Join it to the network's backbone cable.")
                .define("Capacity", "How many items the network handles at once, in items a tick. The Mainframe's"
                        + " processors set it.")
                .whatCanGoWrong("The network is slow.",
                        "Look at the Mainframe first: faster processors raise its capacity, more graphics cards"
                                + " give it more queues.",
                        "Two Mainframes, and nothing works.",
                        "A network has one Mainframe. Split the cables.")
                .register();
        guide.page("server_racks", section).titled("Server Racks").icon(ComputingModule.SERVER_RACK)
                .coversAll(blocksOf(ServerRackBlock.class))
                .whatItIs("A cabinet two blocks wide, three tall and two deep, with eight rack units for servers"
                        + " and the equipment that serves them. The Supercomputer Rack is the same cabinet for"
                        + " supercomputer nodes only.")
                .whatItIsFor("Holding the servers whose disks keep the network's items.")
                .howToGetIt(FROM_THE_TABS)
                .paragraph("The Supercomputer Racks are in the Standard and the Advanced.")
                .howToUseIt("Place its parts in its shape: it forms when the shape is whole.",
                        "Put servers in its bays: a rack seats servers of its own era or older.",
                        "Join it to the network's backbone cable.")
                .whatCanGoWrong("A server does not go in.",
                        "It is from a newer era than the rack, or the rack has no rack units left for it.")
                .register();
        guide.page("servers", section).titled("Servers and nodes").icon(ComputingModule.SERVER)
                .coversAll(itemsOf(ServerItem.class, ServerCaseItem.class))
                .whatItIs("The machines a rack holds: servers with drive bays, a case to build one in, and the nodes"
                        + " of a supercomputer.")
                .whatItIsFor("A server's disks are where the network keeps your items. A Storage Server holds more"
                        + " drives; a Compute Server more processors.")
                .table("Servers")
                .property("Vintage Server", "1 U, 1 drive")
                .property("Legacy Server", "1 U, 2 drives")
                .property("Transition Server", "1 U, 4 drives")
                .property("Server", "1 U, 3 drives")
                .property("Advanced Server", "1 U, 4 drives")
                .property("Storage Server", "2 U, 8 drives")
                .property("Compute Server", "2 U, 1 drive, 4 processors")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put a server in a rack's bay.",
                        "Put disks in the server's drive bays at the front of the rack.",
                        "Give it processors and the cards it needs.")
                .define("Rack unit (U)", "The height a machine takes in a rack. A rack has eight.")
                .whatCanGoWrong("Storage is full.",
                        "Add disks to the servers, or servers to the racks. A newer era's disks hold more.")
                .register();
        guide.page("rack_equipment", section).titled("Rack equipment").icon(ComputingModule.KVM_SWITCH)
                .coversAll(itemsOf(RackGadgetItem.class, RackUnitItem.class))
                .whatItIs("What serves a rack rather than computing: the KVM Switch, the Rack UPS and the Cooling Unit,"
                        + " which take rack units, and the RAID Controller and Cache Card a server takes.")
                .whatItIsFor("A KVM Switch lets one monitor reach several machines; a RAID Controller joins a"
                        + " server's drives; a Cache Card cuts how long a read waits.")
                .define("RAID", "Drives joined as one. RAID 0, with two drives or more, is a quarter faster but"
                        + " keeps nothing if one fails; RAID 1 keeps every drive a copy; RAID 5, with three or more,"
                        + " survives losing one.")
                .howToGetIt(FROM_THE_SHARED_TAB)
                .howToUseIt("Put a rack unit in a rack's bay, as a server.",
                        "Put a RAID Controller or a Cache Card in a server's gadget slot.")
                .whatCanGoWrong("The RAID Controller does nothing.",
                        "The server has fewer drives than its RAID level needs.")
                .register();
    }

    private static void hardware(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("hardware").titled("Hardware")
                .icon(HardwareItems.GPU_VERTEX_8800_GT).register();
        guide.page("motherboards", section).titled("Motherboards")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150).coversAll(itemsOf(MotherboardItem.class))
                .whatItIs("The board every other part of a computer sits on.")
                .whatItIsFor("It decides what a computer can take: which processors (its socket), how much memory"
                        + " and of which kinds, how many cards, and whether it has sound of its own.")
                .define("Form factor", "A board's size, which says which cases it fits: Baby-AT and AT in the"
                        + " Vintage, ATX from the Legacy, EATX from the Transition, and MTX for the Mainframes.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Read its tooltip: form factor, socket, memory, slots and era.",
                        "Put it in a case of its era first, then the parts it takes.")
                .whatCanGoWrong("It does not go into the case.",
                        "The case is of another era, or of a size the board does not fit.")
                .register();
        guide.page("processors", section).titled("Processors (CPU)")
                .icon(HardwareItems.CPU_INTEGRA_PENTIX_133).coversAll(itemsOf(CpuItem.class))
                .whatItIs("The part that computes, from the Integra and Velocion makers, in every era.")
                .whatItIsFor("In a Mainframe its capacity is how many items the network handles at once.")
                .table("What a processor's tooltip says")
                .property("Cores and clock", "How many things it does at once, and how fast each")
                .property("Capacity (it/t)", "Items a tick: cores times clock times design")
                .property("Socket", "The board socket it fits")
                .property("Watts", "The power it draws")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Pick one whose socket matches the board's.",
                        "Put it in the board's processor slot.")
                .define("Instruction set", "What a processor runs. A program built for one runs on it and every"
                        + " newer one, never on an older one.")
                .whatCanGoWrong("It will not go in.",
                        "Its socket is not the board's, or it is of another era.",
                        "The build is refused for mixed processors.",
                        "Processors of two instruction sets never share a board.")
                .register();
        guide.page("memory", section).titled("Memory (RAM)").icon(HardwareItems.RAM_SIMM_4)
                .coversAll(itemsOf(RamItem.class))
                .whatItIs("The memory a computer runs its system and programs in, from SIMM modules in the Vintage"
                        + " to DDR5.")
                .whatItIsFor("Every program needs some. In a Mainframe it is also the buffer items wait in on their"
                        + " way in or out of the network.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Read the board's tooltip for the kinds of memory it takes.",
                        "Put modules of one of those kinds in its memory slots.")
                .whatCanGoWrong("The module is refused.",
                        "It is of a generation the board does not take.")
                .register();
        guide.page("graphics_cards", section).titled("Graphics cards (GPU)")
                .icon(HardwareItems.GPU_VERTEX_8800_GT).coversAll(itemsOf(GpuItem.class))
                .whatItIs("The card that lights the monitors and draws the windows, from the VGA-256 to the Envya"
                        + " Vertex RTX 5090.")
                .whatItIsFor("A computer needs one to show a picture, unless its processor has graphics built in. In"
                        + " a Mainframe each card is one more queue of Operations.")
                .define("Video memory", "The card's own memory. Every monitor and every window that draws a picture"
                        + " uses some; a bigger monitor more.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put it in an expansion slot of the board.",
                        "Join monitors to the computer: each takes one of the card's video outputs.")
                .whatCanGoWrong("The monitor says there is no video output.",
                        "Put a graphics card in, or use a processor with graphics built in.",
                        "The monitor says there is not enough video memory.",
                        "Use a card with more, or fewer or smaller monitors.")
                .register();
        guide.page("power_supplies", section).titled("Power supplies (PSU)").icon(HardwareItems.PSU_200)
                .coversAll(itemsOf(PsuItem.class))
                .whatItIs("The part that powers the others, from 200 W to 3,000 W.")
                .whatItIsFor("Every part draws power; the supply must give at least what they draw together.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Add up what the parts draw: each tooltip says its watts.",
                        "Put in a supply that gives more.")
                .whatCanGoWrong("The computer refuses to start.",
                        "Its parts draw more than its power supply gives. Use a bigger one.")
                .register();
        guide.page("disks", section).titled("Disks").icon(HardwareItems.DISK_TRENCH_20M)
                .coversAll(itemsOf(DiskItem.class))
                .whatItIs("Where a computer keeps its system, its files and programs, and where the network keeps"
                        + " your items: hard disks, SSDs and NVMe drives.")
                .whatItIsFor("A disk's capacity is counted in items of its era: a 20 MB Vintage drive holds 20, a 1 TB"
                        + " disk 4,096.")
                .table("How fast each kind is")
                .property("HDD", "The speed of a hard disk; waits 10 ticks before a transfer")
                .property("SSD", "Four times as fast; waits 3 ticks")
                .property("NVMe", "Sixteen times as fast; waits 1 tick")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put it in a computer's disk slot, or in a server's drive bay.",
                        "Install a system on it, or let the network fill it with items.")
                .whatCanGoWrong("The installer says the disk is too small.",
                        "The system takes more room than the disk has. Use a bigger disk.")
                .register();
        guide.page("sound_cards", section).titled("Sound cards").icon(HardwareItems.SOUND_CARD_TONE_BLASTER)
                .coversAll(itemsOf(SoundCardItem.class))
                .whatItIs("The Artisan Tone Blaster family, from the Vintage to the Transition: the card that gives a"
                        + " computer its sound.")
                .whatItIsFor("Music and sounds out of the monitors and speakers. From the Transition on, boards have"
                        + " sound of their own.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put one in an expansion slot of a board of its era.",
                        "Join speakers to the computer to hear it beside the monitor.")
                .whatCanGoWrong("The build is refused.",
                        "A computer takes one sound card, of its board's era.")
                .register();
        guide.page("workshop_cards", section).titled("Workshop cards").icon(ComputingModule.FURNACE_CARD)
                .coversAll(itemsOf(WorkshopCardItem.class))
                .whatItIs("The Crafting Table, Furnace, Enchanting and Anvil cards.")
                .whatItIsFor("They let a Personal Computer's Workshop program craft, smelt, enchant and repair, and"
                        + " let the network change stored items the same way.")
                .howToGetIt(FROM_THE_SHARED_TAB)
                .howToUseIt("Put the card in a Personal Computer.",
                        "Open the Workshop program.")
                .whatCanGoWrong("The Workshop cannot do what you ask.",
                        "It needs the card for it: smelting the Furnace Card, enchanting the Enchanting Card.")
                .register();
        guide.page("cluster_interface_cards", section).titled("Cluster Interface Cards")
                .icon(ComputingModule.SERIAL_CONSOLE_CARD).coversAll(itemsOf(ClusterInterfaceCardItem.class))
                .whatItIs("The Serial Console Card, the Management NIC, the Fabric Host Adapter and the Fabric DPU.")
                .whatItIsFor("They let a Cluster Management Computer reach racked machines; each newer one reaches"
                        + " more, and installs on several at once.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put the card in a Cluster Management Computer.")
                .whatCanGoWrong("The card does nothing in another computer.",
                        "It only works in a Cluster Management Computer.")
                .register();
        guide.page("optical_network_card", section).titled("Optical Network Card")
                .icon(ComputingModule.OPTICAL_NETWORK_CARD).coversAll(itemsOf(NetworkCardItem.class))
                .whatItIs("A network card with a fibre port.")
                .whatItIsFor("It lets a machine take the fibre of the network's backbone.")
                .howToGetIt(FROM_THE_STANDARD_TAB)
                .howToUseIt("Put it in an expansion slot of the machine.",
                        "Lay fibre straight into it.")
                .whatCanGoWrong("The fibre will not join.",
                        "Fibre runs only straight. Turn it at an optical router.")
                .register();
        guide.page("phi_coprocessors", section).titled("Integra Phi coprocessors")
                .icon(ComputingModule.PHI_5100).coversAll(itemsOf(PhiCoprocessorItem.class))
                .whatItIs("Four coprocessor cards for supercomputer nodes.")
                .whatItIsFor("Adding computing power to the nodes of a supercomputer.")
                .howToGetIt(FROM_THE_STANDARD_TAB)
                .howToUseIt("Put one in a Supercomputer Node's card slot.")
                .whatCanGoWrong("It will not go into a server.",
                        "It is made for supercomputer nodes.")
                .register();
    }

    private static void network(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("network").titled("The data network")
                .icon(ComputingModule.SERVER_RACK).register();
        guide.page("first_network", section).titled("Your first network").icon(ComputingModule.MAINFRAME)
                .whatItIs("The steps from a few computers to a network that keeps your items.")
                .whatItIsFor("A network stores items as data on its servers' disks, gives them back when asked,"
                        + " moves them and crafts.")
                .howToGetIt("Everything is in the creative tabs of J's Computers.")
                .howToUseIt("Build a Mainframe and give it its parts.",
                        "Build a Server Rack and put servers with disks in its bays.",
                        "Join the Mainframe and the racks with the backbone cable of their era.",
                        "Join the small computers with the access cable, through a router.",
                        "At a computer on the network, open Network and put items in.")
                .whatCanGoWrong("A computer is not on the network.",
                        "Its cable is longer than its range, or of another era than the router. Add a repeater, or"
                                + " a router of the newer era.")
                .register();
        guide.page("data_cables", section).titled("Data cables").icon(() -> ComputingModule.ETHERNET_CABLE)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.THIN_COAX_CABLE, ComputingModule.ETHERNET_CABLE,
                        ComputingModule.CAT5E_CABLE, ComputingModule.GIGABIT_CABLE, ComputingModule.CAT6A_CABLE,
                        ComputingModule.THICK_COAX_CABLE, ComputingModule.HBW_CABLE, ComputingModule.CX4_CABLE,
                        ComputingModule.FIBRE_CABLE, ComputingModule.OM5_CABLE, ComputingModule.TELEPHONE_LINE,
                        ComputingModule.LEASED_LINE, ComputingModule.T3_LINE, ComputingModule.VLDC_CABLE,
                        ComputingModule.DARK_FIBRE_CABLE, ComputingModule.INFINIBAND_CABLE,
                        ComputingModule.HPC_CABLE, ComputingModule.OSFP_CABLE))
                .whatItIs("The cables of the network, by the job they do: access, backbone, long distance and high"
                        + " compute, each with one cable per era.")
                .whatItIsFor("You lay the line the job asks for: the access line joins small computers to a router,"
                        + " the backbone joins the routers, the Mainframe and the racks.")
                .table("The lines")
                .property("Access", "Small computers to a router: Ethernet, Gigabit")
                .property("Backbone", "Routers, Mainframe, racks: HBW, Fibre Optic")
                .property("Long distance", "Two networks, between two Gateway computers")
                .property("High compute", "A supercomputer's nodes to its HBW Interface")
                .define("Range", "How many cables a run can be before a router or a repeater renews it. A run longer"
                        + " than its range carries nothing.")
                .define("Speed", "The items a tick an Operation crossing a cable moves at most.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Place a cable like a block, from one device to the next.",
                        "Dye a cable to keep two runs apart where they touch: two colours never join.")
                .whatCanGoWrong("Two cables side by side do not join.",
                        "They are of different lines or eras, or dyed in different colours.",
                        "Fibre will not turn a corner.",
                        "From the Standard on, fibre runs only straight. Turn it at an optical router.")
                .register();
        guide.page("routers", section).titled("Routers").icon(ComputingModule.STANDARD_ROUTER)
                .coversAll(blocksOf(RouterBlock.class))
                .whatItIs("The device where a network's lines meet: one per era, and the optical routers for fibre.")
                .whatItIsFor("A router joins its era's access line to its backbone, and takes the cables of every"
                        + " earlier era on any face, all as one network.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Place the router where the access cables of your computers meet.",
                        "Run the backbone from it to the Mainframe.")
                .whatCanGoWrong("An older machine will not join a newer network.",
                        "Two eras of one line meet only at a router of the newer era. Put one between them.")
                .register();
        guide.page("repeaters", section).titled("Repeaters").icon(ComputingModule.STANDARD_REPEATER)
                .coversAll(blocksOf(RepeaterBlock.class))
                .whatItIs("A device that renews a cable's range, one per era.")
                .whatItIsFor("Every run starts its range over at a repeater, so a cable goes twice as far with one"
                        + " halfway. The lines pass through it each on its own, never joining.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put the repeater in the run before the cable reaches its range.")
                .whatCanGoWrong("The far end is still off the network.",
                        "A run on one side of the repeater is still longer than its range.")
                .register();
        guide.page("hbw_interface", section).titled("HBW Interface").icon(ComputingModule.HBW_INTERFACE)
                .coversAll(blocksOf(HbwInterfaceBlock.class))
                .whatItIs("The one point where a supercomputer meets the network: the Standard's, and the"
                        + " Advanced's that takes the OSFP cable.")
                .whatItIsFor("A supercomputer is every Supercomputer Rack tied together by the high compute cable;"
                        + " the HBW Interface joins all of it to the backbone.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join the supercomputer racks to it with high compute cable.",
                        "Join it to the network's backbone.")
                .whatCanGoWrong("The supercomputer is not seen.",
                        "Its racks are not all on the high compute cable that reaches the interface.")
                .register();
        guide.page("network_gateway", section).titled("Network Gateway").icon(ComputingModule.NETWORK_GATEWAY)
                .coversAll(blocksOf(NetworkGatewayBlock.class))
                .whatItIs("A device, linked to a computer through its peripheral cable, that is a bridge to"
                        + " ComputerCraft's computers.")
                .whatItIsFor("A ComputerCraft program can ask the network what it holds and ask for items through it;"
                        + " the Gateway Manager program sets what it may do.")
                .howToGetIt(FROM_THE_SHARED_TAB)
                .howToUseIt("Join it to a computer with peripheral cable on its back socket.",
                        "Join its front to a ComputerCraft wired network.",
                        "Set what ComputerCraft may do in the Gateway Manager.")
                .whatCanGoWrong("It does nothing.",
                        "It is not linked to one of our computers, or that computer is off.")
                .register();
        guide.page("server_router", section).titled("Server Router").icon(ComputingModule.SERVER_ROUTER)
                .coversAll(blocksOf(ServerRouterBlock.class))
                .whatItIs("A device that switches the network and groups Server Racks into datacenter sections, one"
                        + " for each of its faces.")
                .whatItIsFor("Building a datacenter: many racks in sections, run together.")
                .howToGetIt(FROM_THE_SHARED_TAB)
                .howToUseIt("Join its uplink face to the network.",
                        "Join a section of racks to each of its other faces.",
                        "Open it to see each section and its budget.")
                .whatCanGoWrong("It says it is over budget.",
                        "A section holds more racks than it can serve. Split them over more faces.")
                .register();
    }

    private static void operations(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("operations").titled("Operations")
                .icon(ComputingModule.MAINFRAME).register();
        guide.page("what_operations_are", section).titled("Operations").icon(ComputingModule.MAINFRAME)
                .whatItIs("Everything the network does for you is an Operation: a request it queues, works over some"
                        + " ticks, and finishes. They are named as the commands of a database.")
                .whatItIsFor("SELECT takes items out, INSERT puts them in, MOVE sends them somewhere, CRAFT makes"
                        + " them, UPDATE changes them with the workshop cards.")
                .table("The states of an Operation")
                .property("PENDING", "Waiting in its queue")
                .property("PROCESSING", "Being worked")
                .property("WAITING", "Paused for something")
                .property("COMPLETED", "Done")
                .property("COMPLETED_PARTIAL", "Done with less")
                .property("FAILED", "Not done, and says why")
                .property("RESOURCE_LOCKED", "What it needs is held")
                .property("DISCARDED", "Dropped")
                .howToGetIt("Every computer on the network sends them: from the Network program, a command prompt"
                        + " or a program.")
                .howToUseIt("Ask for something in the Network program.",
                        "Watch it in the Mainframe's Task Manager until it is done.")
                .whatCanGoWrong("An Operation stays PENDING.",
                        "The Mainframe is busy or off. A faster Mainframe, or more graphics cards, help.",
                        "It ends COMPLETED_PARTIAL.",
                        "There was less than you asked for.")
                .register();
        guide.page("iql", section).titled("IQL, the network's language").icon(ComputingModule.MAINFRAME)
                .whatItIs("IQL, the Item Query Language, is how you ask the network for anything in words, at a"
                        + " command prompt, in a program or in a script.")
                .whatItIsFor("Asking for exactly what you want: SELECT 64 iron_ingot, CRAFT 64 torch, QUERY items"
                        + " WHERE qty > 100.")
                .howToGetIt("Every system's command prompt speaks it on a computer of the network.")
                .howToUseIt("Open a command prompt on a computer of the network.",
                        "Type a request and press Enter.",
                        "Save what you write often as a view, a procedure or a job, on an engine that offers"
                                + " them.")
                .whatCanGoWrong("A line is refused.",
                        "The prompt says where it stopped reading. Fix the word there.")
                .register();
    }

    private static void storage(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("storage").titled("Storage")
                .icon(HardwareItems.DISK_TRENCH_20M).register();
        guide.page("items_as_data", section).titled("Items as data").icon(HardwareItems.DISK_TRENCH_20M)
                .whatItIs("The network keeps items, fluids and chemicals as data on the disks of its servers, all"
                        + " counted the same way: a bucket takes as much room as an item.")
                .whatItIsFor("Keeping far more than chests hold, and finding anything by name.")
                .define("Item size", "The room an item takes on a disk: 1 MB in the Vintage, 16 MB in the Legacy,"
                        + " 256 MB from the Transition on.")
                .howToGetIt("Build a network with servers and disks.")
                .howToUseIt("Put items in through the Network program, or an Import Bus.",
                        "Take them out the same way, or with an Export Bus.")
                .whatCanGoWrong("Storage is full.",
                        "Add disks or servers. A newer era's disks hold more.")
                .register();
        guide.page("buses", section).titled("Buses").icon(ComputingModule.IMPORT_BUS_ITEM)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.VINTAGE_IMPORT_BUS_ITEM,
                        ComputingModule.LEGACY_IMPORT_BUS_ITEM, ComputingModule.TRANSITION_IMPORT_BUS_ITEM,
                        ComputingModule.IMPORT_BUS_ITEM, ComputingModule.ADVANCED_IMPORT_BUS_ITEM,
                        ComputingModule.VINTAGE_EXPORT_BUS_ITEM, ComputingModule.LEGACY_EXPORT_BUS_ITEM,
                        ComputingModule.TRANSITION_EXPORT_BUS_ITEM, ComputingModule.EXPORT_BUS_ITEM,
                        ComputingModule.ADVANCED_EXPORT_BUS_ITEM, ComputingModule.VINTAGE_EXTERNAL_STORAGE_BUS_ITEM,
                        ComputingModule.LEGACY_EXTERNAL_STORAGE_BUS_ITEM,
                        ComputingModule.TRANSITION_EXTERNAL_STORAGE_BUS_ITEM,
                        ComputingModule.EXTERNAL_STORAGE_BUS_ITEM, ComputingModule.ADVANCED_EXTERNAL_STORAGE_BUS_ITEM))
                .whatItIs("Thin parts on a data cable's face: the Import Bus, the Export Bus and the External Storage"
                        + " Bus.")
                .whatItIsFor("The Import Bus brings items from the inventory it faces into the network; the Export"
                        + " Bus sends them out to it; the External Storage Bus lets the network use it as storage,"
                        + " ten times slower than a server.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Use the bus on a data cable beside the chest or machine.",
                        "Open it to set its filter, what to keep, its mode and its priority.")
                .whatCanGoWrong("The bus moves nothing.",
                        "Its filter takes nothing, it waits for a redstone signal, or it is not on the network.")
                .register();
        guide.page("tank", section).titled("Tank").icon(ComputingModule.TANK)
                .coversAll(blocksOf(TankBlock.class))
                .whatItIs("A plain fluid tank.")
                .whatItIsFor("Moving fluid in and out of the network: an Import Bus takes its fluid in, an Export Bus"
                        + " fills it, as they move items.")
                .howToGetIt(FROM_THE_SHARED_TAB)
                .howToUseIt("Fill it with a bucket or a pipe.",
                        "Put an Import Bus on a cable against it.")
                .whatCanGoWrong("The fluid does not go in.",
                        "The network's storage has no room left.")
                .register();
    }

    private static void autocrafting(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("autocrafting").titled("Autocrafting")
                .icon(ComputingModule.CRAFTING_CARD_PCIE3).register();
        guide.page("how_autocrafting_works", section).titled("How autocrafting works")
                .icon(() -> ComputingModule.CRAFTING_CABLE)
                .covers(() -> ComputingModule.CRAFTING_CABLE)
                .whatItIs("The network making things for you: you ask for 512 pistons and it works out every step"
                        + " from what it has, runs the crafting table and the machines, and stores the pistons.")
                .whatItIsFor("Never crafting the same thing by hand twice.")
                .define("Pattern", "A recipe the network knows: a crafting table grid, a machine's inputs and"
                        + " outputs, or both in many steps.")
                .howToGetIt("Build a Crafting Computer, lay Crafting Cable and give the network patterns.")
                .howToUseIt("Write a pattern in the Pattern Studio and burn it at a Pattern Encoder.",
                        "Load it on the Crafting Computer in the Crafting Manager.",
                        "Lay Crafting Cable to a Crafting Interface on each machine.",
                        "Ask: CRAFT 512 piston. The Craft Planner shows the plan first.")
                .whatCanGoWrong("The plan says something is missing.",
                        "An item has no pattern and none is in storage. Add its pattern or the item.",
                        "A machine is never fed.",
                        "Its interface is not on a cable a Crafting Computer drives, or the computer is off.")
                .register();
        guide.page("crafting_cards", section).titled("Crafting Cards").icon(ComputingModule.CRAFTING_CARD_PCIE3)
                .coversAll(itemsOf(CraftingCardItem.class))
                .whatItIs("The card that makes a Crafting Computer craft.")
                .whatItIsFor("It drives a number of Crafting Interfaces and keeps that many crafting table patterns"
                        + " in its own memory, so they move with the card.")
                .table("What each era's card drives")
                .fixed("Vintage", "2")
                .fixed("Legacy", "4")
                .fixed("Transition", "5")
                .fixed("Standard", "6")
                .fixed("Advanced", "8")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put it in a Crafting Computer.")
                .whatCanGoWrong("A crafting table pattern is gone.",
                        "It lives on the card: it went with the card to another computer.")
                .register();
        guide.page("crafting_interfaces", section).titled("Crafting Interfaces")
                .icon(ComputingModule.CRAFTING_INTERFACE_ITEM)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.VINTAGE_CRAFTING_INTERFACE_ITEM,
                        ComputingModule.LEGACY_CRAFTING_INTERFACE_ITEM,
                        ComputingModule.TRANSITION_CRAFTING_INTERFACE_ITEM, ComputingModule.CRAFTING_INTERFACE_ITEM,
                        ComputingModule.ADVANCED_CRAFTING_INTERFACE_ITEM, ComputingModule.CRAFTING_ROUTER_ITEM,
                        ComputingModule.RECEIVING_BUS_ITEM))
                .whatItIs("The parts on the Crafting Cable that run a machine: the Crafting Interface, the Crafting"
                        + " Input Router and the Crafting Receiving Bus.")
                .whatItIsFor("The interface holds a machine's patterns and feeds it; a router feeds one input face of"
                        + " a machine with several; the receiving bus takes the output back.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Put a Crafting Interface on the cable against the machine.",
                        "For a machine with several input faces, put a Crafting Input Router on each.",
                        "Put a Crafting Receiving Bus against the machine's output.",
                        "Give the machine power, the way its own mod says.")
                .whatCanGoWrong("The output never comes back.",
                        "No Crafting Receiving Bus faces the machine's output.")
                .register();
        guide.page("pattern_encoders", section).titled("Pattern Encoders").icon(ComputingModule.PATTERN_ENCODER)
                .coversAll(blocksOf(PatternEncoderBlock.class))
                .whatItIs("A device joined to a computer that burns patterns onto media.")
                .whatItIsFor("Carrying a pattern from the Pattern Studio, where you write it, to the Crafting"
                        + " Computer that uses it.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join it to the computer with peripheral cable.",
                        "Put a blank medium in it: a floppy in the Vintage, a CD in the Legacy.",
                        "Burn the pattern from the Pattern Studio.")
                .whatCanGoWrong("The Pattern Studio sees no encoder.",
                        "It is not joined to that computer.")
                .register();
    }

    private static void systems(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("systems").titled("Operating systems")
                .icon(ComputingModule.FLOPPY_DISK).register();
        guide.page("installing", section).titled("Installing a system").icon(ComputingModule.USB_FLASH_DRIVE)
                .coversAll(itemsOf(FormattedMediaItem.class))
                .whatItIs("Putting an operating system on a computer's disk from its install medium: a floppy, a CD, a"
                        + " DVD, a USB stick or a Blu-ray.")
                .whatItIsFor("A computer does nothing useful until it has a system.")
                .table("The media of each era")
                .property("Vintage", "Floppy disks, in a Floppy Drive")
                .property("Legacy", "CDs, in a CD Drive")
                .property("Transition", "DVDs, in a DVD Drive")
                .property("Standard and Advanced", "USB sticks, in a Dock Station")
                .howToGetIt("The install media are in each era's tab, among the programs; blank media beside them.")
                .howToUseIt("Put the medium in a drive joined to the computer.",
                        "Use the monitor: with no system, it shows the firmware.",
                        "Choose the drive and boot it.",
                        "Follow the installer.")
                .whatCanGoWrong("The installer refuses the system.",
                        "The hardware is older than the system's first era, or the disk is too small.",
                        "The drive does not show in the firmware.",
                        "It is not joined to the computer, or it is not the drive for that medium.")
                .register();
        guide.page("the_systems", section).titled("The systems").icon(ComputingModule.FLOPPY_DISK)
                .whatItIs("Every system is a parody of a real one, made by a software house of the mod's world, and"
                        + " behaves like the one it stands for.")
                .whatItIsFor("Each era has its systems: MC-DOS and UNIX in the Vintage, Frames 95 and Linux in the"
                        + " Legacy, Frames 7, 10 and 11 after them.")
                .table("Some of the systems")
                .property("MC-DOS", "Vintage, a command prompt")
                .property("MC-NET", "Vintage, the network's terminal")
                .property("Frames 95", "Legacy, a desktop")
                .property("Debian, Ubuntu, Fedora", "Legacy, a shell until a desktop is added")
                .property("Frames 10", "Standard, a desktop")
                .howToGetIt("Install media in each era's tab.")
                .howToUseIt("Install a second system on another disk to have two.",
                        "Choose which to start at the boot manager.")
                .whatCanGoWrong("The computer starts into the wrong system.",
                        "Change the boot order in the firmware, or pick at the boot manager.")
                .register();
        guide.page("programs", section).titled("Programs").icon(ComputingModule.CD_ROM)
                .whatItIs("What runs on a system: every system comes with its programs, and others are installed.")
                .whatItIsFor("Network, This PC, Files, the Command Prompt and the Settings on every desktop; the"
                        + " Network Manager on the Mainframe; the Crafting Manager on a Crafting Computer.")
                .howToGetIt("Some come with the system. Others install from their own media, or from the Mirror with"
                        + " the system's package manager.")
                .howToUseIt("Put the program's disc in a joined drive and run its setup from Files.",
                        "Or, with a Mirror on the Mainframe, install it by name with the package manager.")
                .whatCanGoWrong("A program says it needs another engine.",
                        "It is written for an engine the network does not run. Install that engine on the"
                                + " Mainframe.")
                .register();
    }

    private static void peripherals(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("peripherals").titled("Peripherals")
                .icon(ComputingModule.MONITOR).register();
        guide.page("monitors", section).titled("Monitors").icon(ComputingModule.MONITOR)
                .coversAll(blocksOf(MonitorBlock.class))
                .whatItIs("The screen of a computer: green, amber and paper-white screens in the Vintage, colour"
                        + " after.")
                .whatItIsFor("Everything you do with a computer's programs, you do at its monitor.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Place it against the computer, or join it with peripheral cable.",
                        "Use it to sit at the computer.")
                .whatCanGoWrong("The monitor says no computer is in range.",
                        "Lay peripheral cable all the way, within its range.")
                .register();
        guide.page("drives", section).titled("Drives and the Dock Station").icon(ComputingModule.FLOPPY_DRIVE)
                .coversAll(blocksOf(MediaReaderBlock.class))
                .whatItIs("The Floppy, CD, DVD and Blu-ray drives, and the Dock Station that takes USB sticks.")
                .whatItIsFor("Reading install media, and moving files on removable media.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join it to the computer.",
                        "Put the medium in it.")
                .whatCanGoWrong("The medium will not go in.",
                        "Each medium goes in its own kind of drive.")
                .register();
        guide.page("peripheral_cables", section).titled("Peripheral cables")
                .icon(() -> ComputingModule.PERIPHERAL_CABLE)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.VINTAGE_PERIPHERAL_CABLE,
                        ComputingModule.LEGACY_PERIPHERAL_CABLE, ComputingModule.TRANSITION_PERIPHERAL_CABLE,
                        ComputingModule.PERIPHERAL_CABLE, ComputingModule.ADVANCED_PERIPHERAL_CABLE))
                .whatItIs("The cable that joins a computer to its devices: monitors, speakers, drives, printers,"
                        + " hubs. It carries no network.")
                .whatItIsFor("Placing a device away from its computer.")
                .table("How far each era's cable reaches")
                .fixed("Vintage", "8")
                .fixed("Legacy", "12")
                .fixed("Transition", "14")
                .fixed("Standard", "16")
                .fixed("Advanced", "20")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Lay it from the device's back to the computer.",
                        "A device placed against its computer needs no cable.")
                .whatCanGoWrong("The device is not seen.",
                        "The cable is longer than its range, or of a newer era than the port.")
                .register();
        guide.page("hubs", section).titled("Hubs").icon(ComputingModule.STANDARD_HUB)
                .coversAll(blocksOf(HubBlock.class))
                .whatItIs("A device that takes one of the computer's device ports and offers more.")
                .whatItIsFor("Joining more devices than the board has ports for: two on the Vintage switch box, four"
                        + " on the Legacy and Transition hubs, seven on the Standard and Advanced ones.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join the hub to the computer, then the devices to the hub.")
                .whatCanGoWrong("A monitor joined through a hub stays dark.",
                        "Screens and speakers do not pass through a hub. Join them to the computer.")
                .register();
        guide.page("printers", section).titled("Printers").icon(ComputingModule.PRINTER)
                .coversAll(blocksOf(PrinterBlock.class)).coversAll(itemsOf(PrintedPaperItem.class))
                .whatItIs("A printer for each era, and the Printed Paper it turns out.")
                .whatItIsFor("Printing what a program sends it: pages of text, or a picture that shows on the sheet"
                        + " in an item frame.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join it to the computer.",
                        "Use it with paper to fill its tray.",
                        "Print from a program; take the sheets from its window.")
                .whatCanGoWrong("Nothing comes out.",
                        "Its tray is empty. Use it with paper.")
                .register();
        guide.page("speakers", section).titled("Speakers").icon(ComputingModule.SPEAKER)
                .coversAll(blocksOf(SpeakerBlock.class, SubwooferBlock.class))
                .whatItIs("Speakers from the Legacy on, and the Transition's subwoofer.")
                .whatItIsFor("Carrying a computer's sound somewhere other than the monitor. Two beside a monitor play"
                        + " a stereo recording a side each.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Join the speaker to the computer's sound output.",
                        "In the Transition, set the subwoofer against one of the speakers for the bass.")
                .whatCanGoWrong("The music has no bass.",
                        "Legacy speakers lose the bass and the treble, and Transition ones the bass until a"
                                + " subwoofer stands against them.")
                .register();
        guide.page("redstone_interfaces", section).titled("Redstone Interfaces")
                .icon(ComputingModule.STANDARD_REDSTONE_INTERFACE).coversAll(blocksOf(RedstoneInterfaceBlock.class))
                .whatItIs("A small sensor on a computer's peripheral cable, placed as an observer is.")
                .whatItIsFor("Letting a program read the redstone signal at its lens, or emit one there.")
                .howToGetIt(FROM_THE_TABS)
                .howToUseIt("Place it facing what it should read or power.",
                        "Join it to the computer.",
                        "Name it in its window, so programs find it by that name.")
                .whatCanGoWrong("It reads and emits nothing.",
                        "It has no computer at the other end of its cable.")
                .register();
    }
}
