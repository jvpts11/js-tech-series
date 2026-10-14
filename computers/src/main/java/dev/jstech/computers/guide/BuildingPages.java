/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import static dev.jstech.computers.guide.ComputersGuide.itemsOf;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.item.BayDriveItem;
import dev.jstech.computers.item.ClusterInterfaceCardItem;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.NetworkCardItem;
import dev.jstech.computers.item.PhiCoprocessorItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.item.WorkshopCardItem;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Declares the pages of the building chapter: the assembly screen and the front of the case, the parts of a computer
 * one family at a time, and the reasons a build is refused. Each part page says what the part does, what its tooltip
 * tells the player, what decides whether it fits, and what each refusal means.
 */
final class BuildingPages {

    /*
     * The parts of a Standard Personal Computer that make a valid build, as its block entity saves them: the board,
     * the processor, two memory modules, a graphics card, the supply, a hard disk and an M.2 drive, and the DVD drive
     * in the bay.
     */
    private static final String PC_PARTS = "Hardware:{Size:15,Items:["
            + "{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},"
            + "{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},"
            + "{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},"
            + "{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},"
            + "{Slot:11,id:\"jsc:disk_hdd_4t\",count:1},{Slot:12,id:\"jsc:disk_nvme_500g\",count:1},"
            + "{Slot:13,id:\"jsc:internal_dvd_drive\",count:1}]}";
    /** A disc in the bay's drive and a stick in the first USB port. */
    private static final String PC_FRONT = "Front:{Size:4,Items:[{Slot:0,id:\"jsc:dvd_rom\",count:1},"
            + "{Slot:2,id:\"jsc:usb_flash_drive\",count:1}]}";

    private BuildingPages() {
    }

    /** Declares the sections and entries of the building chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        assembly(guide, chapter);
        hardware(guide, chapter);
    }

    private static void assembly(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("assembly").titled("The case").in(chapter).register();
        assemblyScreen(guide, section);
        caseFront(guide, section);
        buildRefusals(guide, section);
    }

    private static void assemblyScreen(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("assembly_screen", section).titled("The assembly screen")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .shows(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150, HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K,
                        ComputingModule.RAM_DDR3_8192, HardwareItems.GPU_VERTEX_GTX_970, ComputingModule.PSU_650G)
                .paragraph("A computer here is a case with parts inside it. Right-click the case to open the assembly"
                        + " screen. It shows the inside of the computer: a slot for each part, and the switches and"
                        + " lamps that run it. Sneak-click the side of the case, not one of its front parts, to take"
                        + " its left side panel off or put it back.")
                .scene("A Personal Computer with its side panel off and every part in its seat. Drag it to turn the"
                        + " case.", 130)
                .place(15, 0, 15, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=east",
                        "{" + PC_PARTS + ",SidePanelOff:1b}")
                .view(-20, 20, 1.2F)
                .balloon(1, GuideBlock.ScenePart.of(15, 0, 15).box(1, 5, 0, 13, 15, 3), "The motherboard",
                        "Every other part sits on it, and it decides which parts fit.")
                .balloon(2, GuideBlock.ScenePart.of(15, 0, 15).box(4, 10, 2, 9, 15, 7), "The processor and its cooler",
                        "The processor sits under the cooler, in the socket its board names.")
                .balloon(3, GuideBlock.ScenePart.of(15, 0, 15).box(9, 9, 1, 13, 15, 4), "The memory",
                        "Two modules stand in their slots.")
                .balloon(4, GuideBlock.ScenePart.of(15, 0, 15).box(1, 7, 1, 11, 10, 5), "The graphics card")
                .balloon(5, GuideBlock.ScenePart.of(15, 0, 15).box(1, 0, 1, 13, 4, 9), "The power supply")
                .balloon(6, GuideBlock.ScenePart.of(15, 0, 15).box(9, 0, 6, 14, 2, 11), "The hard disk")
                .balloon(7, GuideBlock.ScenePart.of(15, 0, 15).box(8, 6, 1, 12, 8, 3), "The M.2 drive",
                        "It sits on the board itself, not in a bay.")
                .balloon(8, GuideBlock.ScenePart.of(15, 0, 15).box(7, 12, 3, 16, 15, 13), "The DVD drive in the bay",
                        "It sits behind the front of the case.")
                .paragraph("A computer runs only when its parts make a build that stands. You read the state of the"
                        + " build in the header of the window, and the window shows each part in its own slot.")
                .paragraph("The POWER button's tooltip names the parts a computer needs. Without a graphics card the"
                        + " monitor stays dark, unless the processor has graphics built in.")
                .define("Build", "The set of parts in the slots of the assembly screen. It is valid when it has no"
                        + " problems.")
                .define("Motherboard", "The board every other part sits on. It decides which parts the computer can"
                        + " take.")
                .define("Processor (CPU)", "The part that does the computing. CPU stands for central processing"
                        + " unit.")
                .define("Memory (RAM)", "Where a computer keeps what it works on right now. RAM stands for"
                        + " random-access memory.")
                .define("Graphics card (GPU)", "The part that draws the picture for the monitors. GPU stands for"
                        + " graphics processing unit.")
                .define("Power supply (PSU)", "The part that gives power to the other parts. PSU stands for power"
                        + " supply unit.")
                .define("Socket", "The place on the board where a processor sits. A processor fits only the socket"
                        + " its board names.")
                .define("RAM buffer", "How many items a computer can hold while it moves them: the total buffer of"
                        + " its memory modules.")
                .subheading("Reading the window")
                .paragraph("The title in the header tells you which computer you are looking at.")
                .table("Title in the header", "Title", "Computer")
                .row(GuideCell.code("PC"), GuideCell.words("A Personal Computer."))
                .row(GuideCell.code("CC"), GuideCell.words("A Crafting Computer."))
                .row(GuideCell.code("CMC"), GuideCell.words("A Cluster Management Computer (CMC)."))
                .row(GuideCell.code("MAINFRAME"), GuideCell.words("A Mainframe."))
                .row(GuideCell.code("SERVER"), GuideCell.words("A server in a Server Rack."))
                .row(GuideCell.code("SUPERCOMPUTER NODE"),
                        GuideCell.words("A supercomputer node in a Server Rack."))
                .paragraph("The word in the top right tells you the state of the computer.")
                .table("The state word", "Word", "Colour", "What it means")
                .row(GuideCell.code("OFFLINE"), GuideCell.words("Red"),
                        GuideCell.words("The build is not valid: there is no build, or it has problems."))
                .row(GuideCell.code("READY"), GuideCell.words("Amber"),
                        GuideCell.words("The build is valid, and the computer is switched off."))
                .row(GuideCell.code("ONLINE"), GuideCell.words("Green"),
                        GuideCell.words("The build is valid, and the computer is switched on and runs."))
                .paragraph("Three lamps show what the computer is doing.")
                .table("The lamps", "Lamp", "Lit when")
                .row(GuideCell.code("PWR"),
                        GuideCell.words("Green while the computer runs. Red while it does not."))
                .row(GuideCell.code("DISK"), GuideCell.words("Amber while the computer works its disk."))
                .row(GuideCell.code("NET"),
                        GuideCell.words("In the accent colour while the computer is linked to a network."))
                .paragraph("A tower has readouts that show its numbers. Hover one to read its tooltip.")
                .table("The readouts of a tower", "Readout", "What it shows")
                .row(GuideCell.code("CAPACITY"),
                        GuideCell.words("The items per tick (it/t) the computer handles, lit while it runs."))
                .row(GuideCell.code("RAM BUFFER"),
                        GuideCell.words("The buffer of its memory, in items (it)."))
                .row(GuideCell.code("NETWORK"),
                        GuideCell.words("LINKED in the accent colour on a network, otherwise a dim \"--\"; the tooltip"
                                + " counts the linked servers."))
                .subheading("The switches and the power button")
                .paragraph("The large button on the left of a tower's front panel is POWER. It switches the computer"
                        + " on and off. A tower has two switches.")
                .table("The switches of a tower", "Switch", "What it does")
                .row(GuideCell.code("AUTO START"),
                        GuideCell.words("Keeps the computer on while its build stands (see below)."))
                .row(GuideCell.code("SIDE PANEL"),
                        GuideCell.words("Lit while the side panel is off, so you can see the parts."))
                .paragraph("Switch AUTO START on while the build is valid, and the computer turns on at once. It stays"
                        + " on as long as the build stands. While AUTO START is on, POWER is locked, and its tooltip"
                        + " says: \"Auto start keeps it on: switch auto start off to switch it off here\".")
                .paragraph("If the build stops standing, the computer goes dark. When the build stands again with AUTO"
                        + " START on, the computer comes back on by itself.")
                .paragraph("If you switch the power on with an invalid build, the switch goes on, but the computer does"
                        + " not run. The board then tests itself and beeps its failure.")
                .paragraph("While the build is not valid, hovering POWER shows three lines: \"Not a computer yet\","
                        + " then \"Needs a board, a CPU, RAM, a disk and a PSU\", then \"Then press to switch on\"."
                        + " The middle line is a general reminder, so it does not name the part that is wrong.")
                .paragraph("The readouts and the POWER button show their tooltip only when the mouse rests on them and"
                        + " the cursor holds nothing.")
                .paragraph("The screen of a Mainframe has AUTO START and FAILOVER. Read about failover in"
                        + " [Failover](jsc:failover) and about the Mainframe in [Mainframes](jsc:mainframes).")
                .subheading("Naming the computer")
                .paragraph("The name field in the header names the computer. It takes up to 32 characters, and it saves"
                        + " each change as you type. An empty field shows a hint that invites you to name"
                        + " the computer.")
                .subheading("To make a computer run")
                .steps("Right-click the case to open the assembly screen.",
                        "Put the parts into their slots, starting with the motherboard.",
                        "Read the word in the header: READY means the build is valid and the computer is off.",
                        "Press POWER, or switch AUTO START on to keep the computer on.")
                .ifSomethingGoesWrong("The POWER button does nothing.",
                        "AUTO START is on, and it locks the button. Switch AUTO START off first.",
                        "The switch is on, but the computer stays off.",
                        "The build is not valid. Hover POWER: its three lines name the parts a computer needs."
                                + " On a rack server, the CHECKS list names each problem"
                                + " ([When the build is refused](jsc:build_refusals)).",
                        "A part sits in the window and does nothing.",
                        "The board does not offer that slot. Use a board with more slots"
                                + " ([](jsc:motherboards)).")
                .seeAlso("jsc:first_computer", "jsc:case_front", "jsc:build_refusals")
                .register();
    }

    private static void caseFront(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("case_front", section).titled("The front of the case").icon(HardwareItems.INTERNAL_CD_DRIVE)
                .coversAll(itemsOf(BayDriveItem.class))
                .paragraph("The front of a Personal Computer, a Crafting Computer or a Cluster Management"
                        + " Computer has parts you can click."
                        + " It has a power button, bays for drives and, from the Transition on, USB ports. Click one of"
                        + " those parts and it does its job. Click anywhere else on the case and the assembly screen"
                        + " opens, or, with a sneak-click, the side panel comes off or goes back on.")
                .scene("The front of a Standard Personal Computer: the power button and its two lamps, a DVD drive"
                        + " with its tray out, and a USB stick in a port. Drag it to look at the case from the side.",
                        130)
                .place(15, 0, 15, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south",
                        "{" + PC_PARTS + "," + PC_FRONT + ",Bay0TrayOpen:1b,Lamps:3}")
                .view(-20, 15, 1.4F)
                .balloon(1, GuideBlock.ScenePart.of(15, 0, 15).box(12, 9, 15, 14, 12, 16), "Power button",
                        "Click it to switch the computer on or off.")
                .balloon(2, GuideBlock.ScenePart.of(15, 0, 15).box(9, 10, 15, 12, 12, 16), "Power and disk lamps",
                        "The first is lit while the computer runs. The second blinks while it works its disk.")
                .balloon(3, GuideBlock.ScenePart.of(15, 0, 15).box(3, 13, 14, 13, 15, 16), "The Internal DVD Drive",
                        "Its eject button opens and closes the tray.")
                .balloon(4, GuideBlock.ScenePart.of(15, 0, 15).box(4, 12, 14, 12, 13, 16), "The tray and its disc",
                        "Click with a disc in your hand to lay it on the open tray. Sneak-click to take it out.")
                .balloon(5, GuideBlock.ScenePart.of(15, 0, 15).box(1, 11, 15, 5, 13, 16), "A USB flash drive",
                        "Sneak-click the port to pull the stick out.")
                .balloon(6, GuideBlock.ScenePart.of(15, 0, 15).box(1, 9, 15, 5, 10, 16), "A free USB port",
                        "Click it with a USB flash drive in your hand to plug the stick in.")
                .paragraph("Look at a standing part, such as an open tray or a stick in a port, and your click goes to"
                        + " that part. The power button, at the top of the front, switches the computer on and off,"
                        + " whatever you hold. It does the same as POWER on the assembly screen.")
                .define("Standing part", "A part that sticks out of the front and takes clicks of its own.")
                .table("The front of each era's case", "Era", "5.25-inch bays", "USB ports on the front")
                .row(GuideCell.words("Vintage"), GuideCell.words("1 bay"), GuideCell.words("0 USB ports"))
                .row(GuideCell.words("Legacy"), GuideCell.words("2 bays"), GuideCell.words("0 USB ports"))
                .row(GuideCell.words("Transition"), GuideCell.words("2 bays"), GuideCell.words("2 USB ports"))
                .row(GuideCell.words("Standard"), GuideCell.words("1 bay"), GuideCell.words("2 USB ports"))
                .row(GuideCell.words("Advanced, Exa and Singularity"), GuideCell.words("0 bays"),
                        GuideCell.words("2 USB ports"))
                .define("Bay", "A slot on the front of the case for a 5.25-inch drive.")
                .subheading("What goes in a bay")
                .paragraph("Four parts go in a bay, one to a bay, and any of them goes in any bay the case has. You put"
                        + " one in on the assembly screen.")
                .table("The parts of a bay", "Part", "Era", "What it takes")
                .row(GuideCell.words("Hot-Swap Drive Bay"), GuideCell.words("Vintage"),
                        GuideCell.words("Disks, from the front"))
                .row(GuideCell.words("Internal CD Drive"), GuideCell.words("Legacy"), GuideCell.words("CDs"))
                .row(GuideCell.words("Internal DVD Drive"), GuideCell.words("Transition"),
                        GuideCell.words("DVDs and CDs"))
                .row(GuideCell.words("Internal Blu-ray Drive"), GuideCell.words("Advanced"),
                        GuideCell.words("Blu-ray discs, DVDs and CDs"))
                .paragraph("An internal drive works like the drive of its kind outside the case"
                        + " ([the drives](jsc:drives)). The eject button on its front opens or closes the tray."
                        + " With the tray open, click with a disc in your hand to lay it on the tray. Sneak-click the"
                        + " open tray to take the disc out again.")
                .paragraph("A drive reads only the discs of its kind. A CD drive reads CDs, a DVD drive reads DVDs and"
                        + " CDs, and a Blu-ray drive reads all three.")
                .define("Tray", "The drawer of an optical drive, which carries the disc.")
                .subheading("The Hot-Swap Drive Bay")
                .paragraph("The Hot-Swap Drive Bay takes a disk from the front. Click with a disk in your hand to slide"
                        + " it in, and click with an empty hand to take it out. It works with the computer on or off."
                        + " It takes a disk of the computer's own era, or of an older one.")
                .define("Hot-swap", "A part you can pull out and push back in while the computer is on.")
                .paragraph("The disk in the bay is one of the computer's own. It gets a drive letter, a system can"
                        + " go on"
                        + " it, and the computer can boot from it. It adds to the computer's capacity and to the power"
                        + " it needs. It takes none of the board's disk slots, so a full board still takes one more"
                        + " disk"
                        + " here.")
                .subheading("USB ports")
                .paragraph("Click a port while you hold a USB flash drive to plug it in. Sneak-click a port to pull its"
                        + " stick out. A stick works as a drive of its own, and a system can be installed from it"
                        + " ([Installing a system](jsc:installing)).")
                .ifSomethingGoesWrong("The disc will not go in, and the game says \"The tray is closed - press"
                        + " the eject button to open it.\"",
                        "Press the eject button on the drive's front to open the tray.",
                        "The drive cannot read that disc.",
                        "The game names the drive, as in \"This CD drive cannot read that disc.\" A CD drive reads"
                                + " CDs, a DVD drive DVDs and CDs, and a Blu-ray drive all three. Use a drive of the"
                                + " right kind.",
                        "The tray will not come out, and the game says \"The drive is reading - wait for it to"
                                + " finish.\"",
                        "The drive is reading a disc. Wait for it to finish.",
                        "The drive already holds a disc.",
                        "Press the eject button to open the tray, then sneak-click the open tray to take the disc out.",
                        "The port already holds a stick.",
                        "Sneak-click the port to pull its stick out.",
                        "The bay already holds a disk.",
                        "Click the bay with an empty hand to take its disk out.",
                        "The hot-swap bay refuses a disk, and the message says this computer does not take that disk.",
                        "The disk is newer than the computer's era. Use a disk of the computer's era or of an"
                                + " older one.")
                .seeAlso("jsc:assembly_screen", "jsc:drives", "jsc:disks")
                .register();
    }

    private static void buildRefusals(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("build_refusals", section).titled("When the build is refused")
                .paragraph("When a build has problems, a rack server's assembly screen can tell you which ones."
                        + " Its power"
                        + " area has a label, CHECKS. Hover it and every problem of the build shows as one line.")
                .paragraph("Towers and the Mainframe do not list the problems. Their POWER tooltip gives only the three"
                        + " generic lines of [The assembly screen](jsc:assembly_screen), so use the table below to find"
                        + " the cause.")
                .paragraph("When nothing is wrong, the list says \"All checks passed\" in green. With no board or power"
                        + " supply in place, it says \"Insert a motherboard and PSU to begin\".")
                .define("Power draw", "The watts the parts ask for in total. The processors, the memory, the disks and"
                        + " the expansion cards add up to it. The motherboard's own power is not counted.")
                .define("Expansion slot", "A slot on the board for an expansion card. The bus standards are ISA, PCI,"
                        + " AGP (Accelerated Graphics Port) and PCI-e (PCI Express), from the oldest to the newest.")
                .define("Bus family", "The family a bus belongs to: ISA, PCI, AGP or PCI-e. A card fits a board only"
                        + " when the bus families match, but for the PCI cards of the Workshop and the PCI Crafting"
                        + " Card, which fit any board but an ISA one. ISA is also the name of the oldest bus.")
                .table("The lines of the CHECKS list", "The line", "What it means", "The fix")
                .row(GuideCell.code("no CPU installed"), GuideCell.words("The build has no processor."),
                        GuideCell.words("Put a processor in the socket the board names."))
                .row(GuideCell.code("no RAM installed"), GuideCell.words("The build has no memory module."),
                        GuideCell.words("Put in a module of a generation the board takes, of its era."))
                .row(GuideCell.code("too many CPUs: <installed> installed, <sockets> sockets"),
                        GuideCell.words("More processors than the board has sockets for."),
                        GuideCell.words("Take processors out, or use a board with more CPU slots."))
                .row(GuideCell.code("CPU socket <a> does not fit board socket <b>"),
                        GuideCell.words("A processor's socket is not the board's socket."),
                        GuideCell.words("Use a processor with the board's socket."))
                .row(GuideCell.code("CPU instruction set <a> does not match the <b> of the other processors"),
                        GuideCell.words("Two processors understand different instruction sets."),
                        GuideCell.words("Use processors of one instruction set."))
                .row(GuideCell.code("too many PCI-e cards: <installed> installed, <slots> PCI-e slots"),
                        GuideCell.words("More expansion cards than the board has expansion slots."),
                        GuideCell.words("Take cards out, or use a board with more PCI-e slots."))
                .row(GuideCell.code("expansion card bus family <a> is not compatible with board bus <b>"),
                        GuideCell.words("A card's bus family is not the board's bus family."),
                        GuideCell.words("Use a card of the board's bus family."))
                .row(GuideCell.code("a sound card of the <a> era does not sit on a board of the <b> era"),
                        GuideCell.words("The sound card is of another era than the board."),
                        GuideCell.words("Use a sound card of the board's era."))
                .row(GuideCell.code("one sound card per machine: <installed> installed"),
                        GuideCell.words("The computer has two sound cards."),
                        GuideCell.words("Take one of them out."))
                .row(GuideCell.code("too many RAM modules: <installed> installed, <slots> slots"),
                        GuideCell.words("More memory modules than the board has slots."),
                        GuideCell.words("Take modules out, or use a board with more RAM slots."))
                .row(GuideCell.code("RAM generation <a> not accepted by board"),
                        GuideCell.words("A module's generation is not on the board's list."),
                        GuideCell.words("Use a module of a generation the board takes."))
                .row(GuideCell.code("too many disks: <installed> installed, <slots> disk slots"),
                        GuideCell.words("More disks in the board's disk slots than it has."),
                        GuideCell.words("Move disks to the Hot-Swap Drive Bay, or use a board with more disk slots."))
                .row(GuideCell.code("PSU insufficient: draw <a>W exceeds <b>W"),
                        GuideCell.words("The power draw is more than the power supply gives."),
                        GuideCell.words("Use a power supply with more watts. On a rack server, the DRAW readout"
                                + " shows the total."))
                .paragraph("The numbers in each line change to match your build.")
                .subheading("Parts that stay out of their slot")
                .paragraph("Some parts never go into a slot. Hold one over the slot, and the window says which rule"
                        + " keeps it out, as the table below lists them.")
                .table("Parts that stay out", "Part", "Why it stays out")
                .row(GuideCell.words("A board of another form factor or era"),
                        GuideCell.words("The case takes only its own form factors, and boards of its own era."))
                .row(GuideCell.words("A processor of another socket or era, while a board is in place"),
                        GuideCell.words("It must match the board's socket and era."))
                .row(GuideCell.words("A memory module of another generation or era, while a board is in place"),
                        GuideCell.words("It must match a generation the board takes, and the board's era."))
                .row(GuideCell.words("A disk of a newer era than the computer"),
                        GuideCell.words("A disk fits the computer's own era or an older one."))
                .row(GuideCell.words("A part other than a drive in a bay"),
                        GuideCell.words("A bay takes only a drive."))
                .row(GuideCell.words("A second sound card"), GuideCell.words("A computer takes one sound card."))
                .row(GuideCell.words("A second Cluster Interface Card in a Cluster Management Computer"),
                        GuideCell.words("A Cluster Management Computer takes only one of them."))
                .row(GuideCell.words("A Workshop card in a computer other than a Personal Computer"),
                        GuideCell.words("Only a Personal Computer takes Workshop cards."))
                .row(GuideCell.words("An Integra Phi coprocessor in a rack server other than a supercomputer node"),
                        GuideCell.words("Only a supercomputer node takes one, and only one per node."))
                .paragraph("A board the case refuses leaves the computer with no build at all. The same happens with no"
                        + " power supply in place.")
                .ifSomethingGoesWrong("A part does not go into its slot.",
                        "Hold it over the slot to read the rule it breaks, and find that rule in the tables above.",
                        "The server says \"Insert a motherboard and PSU to begin\".",
                        "Put in a board the case takes, and a power supply.")
                .register();
    }

    private static void hardware(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("hardware").titled("The parts").in(chapter).register();
        motherboards(guide, section);
        processors(guide, section);
        memory(guide, section);
        graphicsCards(guide, section);
        disks(guide, section);
        powerSupplies(guide, section);
        soundCards(guide, section);
        expansionCards(guide, section);
    }

    private static void motherboards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("motherboards", section).titled("Motherboards")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .coversAll(itemsOf(MotherboardItem.class))
                .paragraph("The motherboard is the board every other part sits on, like the skeleton of a real"
                        + " computer. It decides which processor, memory and cards the computer can take, and which"
                        + " case takes it.")
                .paragraph("Hover a board and its tooltip tells you the rest. It names the board's form factor, its"
                        + " socket, its memory slots and the kinds of memory it takes, its expansion slots and their"
                        + " bus, whether it has sound of its own, whether it drives a screen by itself, and its era.")
                .define("Form factor", "The size and mounting standard of a board. It decides which cases take the"
                        + " board.")
                .table("The lines of the tooltip", "Line", "What it tells you")
                .row(GuideCell.code("<form factor> form factor"),
                        GuideCell.words("The size of the board, which decides which case takes it."))
                .row(GuideCell.code("<n>x <socket>  |  <n> RAM (<kinds>)  |  <n>x <bus>"),
                        GuideCell.words("The processor slots and their socket. The memory slots and the kinds of"
                                + " memory it takes. The expansion slots and their bus."))
                .row(GuideCell.code("On-board audio"),
                        GuideCell.words("Boards from the Transition on have sound of their own."))
                .row(GuideCell.code("Seats parts of its own era only"),
                        GuideCell.words("The board takes parts of its own era only."))
                .row(GuideCell.code("Has a screen output of its own, for its console"),
                        GuideCell.words("A server board drives a monitor by itself, so a server built on it needs no"
                                + " graphics card and keeps its expansion slots for other cards. Every other board"
                                + " says it has none: a monitor on it needs a graphics card, or a processor with"
                                + " graphics."))
                .paragraph("Each computer takes only some boards. The table shows which ones, and the board's era must"
                        + " match the computer's era too.")
                .table("The boards each computer takes", "Computer", "Era", "Boards it takes")
                .row(GuideCell.words("Personal Computer"), GuideCell.words("Vintage"),
                        GuideCell.words("Baby-AT and AT"))
                .row(GuideCell.words("Personal Computer"), GuideCell.words("Legacy"), GuideCell.words("ATX only"))
                .row(GuideCell.words("Personal Computer"), GuideCell.words("Transition and later"),
                        GuideCell.words("ATX and EATX"))
                .row(GuideCell.words("Crafting Computer"), GuideCell.words("Vintage"),
                        GuideCell.words("Baby-AT and AT"))
                .row(GuideCell.words("Crafting Computer"), GuideCell.words("Legacy and later"),
                        GuideCell.words("ATX only"))
                .row(GuideCell.words("Cluster Management Computer"), GuideCell.words("Vintage"),
                        GuideCell.words("Baby-AT and AT"))
                .row(GuideCell.words("Cluster Management Computer"), GuideCell.words("Legacy and later"),
                        GuideCell.words("ATX and EATX"))
                .paragraph("The Mainframe takes only MTX boards. A board of another era is refused at the slot, without"
                        + " a word. A board of another form factor is refused the same way.")
                .paragraph("A cheaper board may have fewer memory slots or expansion slots than the case has room for.")
                .paragraph("The expansion slots run on a bus. The buses, from the oldest to the newest, are ISA, PCI,"
                        + " AGP 2x, AGP 4x, AGP 8x, and PCI-e 1.0, 2.0, 3.0, 4.0, 5.0 and 6.0. A card needs a bus of"
                        + " its board's family ([the bus family](jsc:build_refusals)).")
                .paragraph("A few server boards also take the cards of an older bus: the Vintage server board takes ISA"
                        + " cards beside its PCI ones, and the Legacy server boards take PCI cards beside their PCI-e"
                        + " ones. The board's tooltip names the older bus.")
                .ifSomethingGoesWrong("It does not go into the case.",
                        "The case takes only some form factors, and only boards of its own era. Check the table above.",
                        "A part sits in the window but does nothing.",
                        "The board does not offer that slot. Use a board with more slots of that kind.",
                        "A card does not go into a server.",
                        "The window shows only the expansion slots the board and the server's case wire, and in most"
                                + " eras a storage server has one. If a graphics card sits in it, the card has nowhere"
                                + " to go."
                                + " Build the server on a server board, which has a screen output of its own, and"
                                + " the slot is free for the card.")
                .seeAlso("jsc:eras", "jsc:processors", "jsc:memory")
                .register();
    }

    private static void processors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("processors", section).titled("Processors").icon(HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K)
                .coversAll(itemsOf(CpuItem.class))
                .paragraph("The processor, or CPU, does the actual computing. Every program you run is the processor"
                        + " following its instructions, one after the other, very fast.")
                .paragraph("Two numbers say how much a processor can do: its cores, and its clock speed in GHz. Your"
                        + " processor's cores are the units of work that run at the same time.")
                .define("Core", "One unit of work a processor does at the same time as its other cores.")
                .define("Clock speed", "How fast each core runs, in GHz (gigahertz): billions of beats of the clock a"
                        + " second.")
                .paragraph("Hover a processor and its tooltip shows its capacity, in items a tick (it/t), and its power"
                        + " in watts. On a Mainframe, the capacity of its processors adds up to how many items a tick"
                        + " the whole network handles ([Mainframes](jsc:mainframes)).")
                .paragraph("A processor also has to fit the board. Its socket must be the board's socket, and its era"
                        + " must be the board's era. With no board in place yet, a processor goes into any slot, so you"
                        + " can set up your parts first.")
                .paragraph("Every processor belongs to an instruction set. A program built for one set runs on it"
                        + " and on"
                        + " every newer set, never on an older one. Processors of two sets never share a computer.")
                .table("The instruction sets", "Instruction set", "Word size", "Runs programs built for")
                .row(GuideCell.words("IA-16"), GuideCell.words("16 bits"), GuideCell.words("IA-16"))
                .row(GuideCell.words("x86"), GuideCell.words("32 bits"), GuideCell.words("x86 and IA-16"))
                .row(GuideCell.words("x86-64"), GuideCell.words("64 bits"), GuideCell.words("x86-64, x86 and IA-16"))
                .paragraph("The word size of a processor depends on its era, and so does the size of one item. You"
                        + " will see the same sizes on the disks ([Units and ticks](jsc:units_and_ticks)).")
                .table("Word size and item size by era", "Era", "Word size", "Size of one item")
                .row(GuideCell.words("Vintage"), GuideCell.words("16 bits"), GuideCell.words("1 MB"))
                .row(GuideCell.words("Legacy"), GuideCell.words("32 bits"), GuideCell.words("16 MB"))
                .row(GuideCell.words("Transition"), GuideCell.words("64 bits"), GuideCell.words("256 MB"))
                .row(GuideCell.words("Standard"), GuideCell.words("64 bits"), GuideCell.words("256 MB"))
                .row(GuideCell.words("Advanced"), GuideCell.words("64 bits"), GuideCell.words("256 MB"))
                .row(GuideCell.words("Exa"), GuideCell.words("64 bits"), GuideCell.words("256 MB"))
                .row(GuideCell.words("Singularity"), GuideCell.words("64 bits"), GuideCell.words("256 MB"))
                .paragraph("There are more than 170 processors in the mod, from the Integra and Velocion makers. A"
                        + " processor with graphics built in says so on its tooltip, and it lights a monitor without a"
                        + " graphics card.")
                .ifSomethingGoesWrong("It will not go in.",
                        "Its socket or its era is not the board's. Use a processor with the board's socket and era.",
                        "The build says the CPU instruction set does not match.",
                        "Use processors of one instruction set.",
                        "The build says there are too many CPUs.",
                        "Take processors out, or use a board with more CPU slots.")
                .seeAlso("jsc:motherboards", "jsc:mainframes", "jsc:eras")
                .register();
    }

    private static void memory(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("memory", section).titled("Memory").icon(HardwareItems.RAM_SIMM_4)
                .coversAll(itemsOf(RamItem.class))
                .paragraph("Memory, or RAM, is where a computer keeps what it works on right now: the system, the"
                        + " programs that are open, the file you are editing. RAM is fast, and it forgets everything"
                        + " when the computer turns off, which is why the disk is there too.")
                .paragraph("The Task Manager shows how much memory is in use, on its Performance view.")
                .define("Memory generation", "The standard a memory module follows, such as SIMM, DDR3 or DDR4.")
                .paragraph("Memory comes in generations: SIMM, EDO and SDRAM in the Vintage; SDRAM, DDR and DDR2 in the"
                        + " Legacy; then DDR2, DDR3, DDR4 and DDR5 in the later eras.")
                .paragraph("A board takes only some generations, and its tooltip lists them. A module must also match"
                        + " the board's era. A module of another generation or era is refused at its slot.")
                .paragraph("The Vintage and Legacy modules begin with the words Stratix Layer, as in Stratix Layer"
                        + " SIMM-4. The table gives the rest of each name.")
                .table("The memory modules", "Module", "Era and generation", "RAM buffer", "Power")
                .row(GuideCell.words("SIMM-4"), GuideCell.words("Vintage, SIMM"), GuideCell.words("1 item"),
                        GuideCell.amount(1, "W"))
                .row(GuideCell.words("SIMM-16"), GuideCell.words("Vintage, SIMM"), GuideCell.words("4 items"),
                        GuideCell.amount(2, "W"))
                .row(GuideCell.words("EDO-16"), GuideCell.words("Vintage, EDO"), GuideCell.words("4 items"),
                        GuideCell.amount(2, "W"))
                .row(GuideCell.words("EDO-32"), GuideCell.words("Vintage, EDO"), GuideCell.words("8 items"),
                        GuideCell.amount(3, "W"))
                .row(GuideCell.words("EDO-64"), GuideCell.words("Vintage, EDO"), GuideCell.words("16 items"),
                        GuideCell.amount(4, "W"))
                .row(GuideCell.words("SDRAM-32"), GuideCell.words("Vintage, SDRAM"), GuideCell.words("8 items"),
                        GuideCell.amount(3, "W"))
                .row(GuideCell.words("SDRAM-64"), GuideCell.words("Vintage, SDRAM"), GuideCell.words("16 items"),
                        GuideCell.amount(4, "W"))
                .row(GuideCell.words("SDRAM-128"), GuideCell.words("Legacy, SDRAM"), GuideCell.words("32 items"),
                        GuideCell.amount(5, "W"))
                .row(GuideCell.words("SDRAM-256"), GuideCell.words("Legacy, SDRAM"), GuideCell.words("64 items"),
                        GuideCell.amount(6, "W"))
                .row(GuideCell.words("DDR-256"), GuideCell.words("Legacy, DDR"), GuideCell.words("64 items"),
                        GuideCell.amount(7, "W"))
                .row(GuideCell.words("DDR-512"), GuideCell.words("Legacy, DDR"), GuideCell.words("128 items"),
                        GuideCell.amount(10, "W"))
                .row(GuideCell.words("DDR-1024"), GuideCell.words("Legacy, DDR"), GuideCell.words("256 items"),
                        GuideCell.amount(12, "W"))
                .row(GuideCell.words("DDR2-512"), GuideCell.words("Legacy, DDR2"), GuideCell.words("128 items"),
                        GuideCell.amount(9, "W"))
                .row(GuideCell.words("DDR2-1024"), GuideCell.words("Transition, DDR2"), GuideCell.words("256 items"),
                        GuideCell.amount(10, "W"))
                .row(GuideCell.words("DDR2-2048"), GuideCell.words("Transition, DDR2"), GuideCell.words("512 items"),
                        GuideCell.amount(12, "W"))
                .row(GuideCell.words("DDR2 4096 RDIMM"), GuideCell.words("Transition, DDR2"),
                        GuideCell.words("1,024 items"), GuideCell.amount(15, "W"))
                .row(GuideCell.words("DDR3-1024"), GuideCell.words("Transition, DDR3"), GuideCell.words("256 items"),
                        GuideCell.amount(8, "W"))
                .row(GuideCell.words("DDR3-2048"), GuideCell.words("Transition, DDR3"), GuideCell.words("512 items"),
                        GuideCell.amount(10, "W"))
                .row(GuideCell.words("DDR3-4096"), GuideCell.words("Transition, DDR3"),
                        GuideCell.words("1,024 items"), GuideCell.amount(12, "W"))
                .row(GuideCell.words("DDR3-8192"), GuideCell.words("Standard, DDR3"), GuideCell.words("2,048 items"),
                        GuideCell.amount(15, "W"))
                .row(GuideCell.words("DDR3L-4096"), GuideCell.words("Standard, DDR3"),
                        GuideCell.words("1,024 items"), GuideCell.amount(10, "W"))
                .row(GuideCell.words("DDR3 16384 RDIMM"), GuideCell.words("Standard, DDR3"),
                        GuideCell.words("4,096 items"), GuideCell.amount(18, "W"))
                .row(GuideCell.words("DDR4 8192"), GuideCell.words("Advanced, DDR4"), GuideCell.words("2,048 items"),
                        GuideCell.amount(12, "W"))
                .row(GuideCell.words("DDR4 16384"), GuideCell.words("Advanced, DDR4"), GuideCell.words("4,096 items"),
                        GuideCell.amount(13, "W"))
                .row(GuideCell.words("DDR4 32768"), GuideCell.words("Advanced, DDR4"), GuideCell.words("8,192 items"),
                        GuideCell.amount(15, "W"))
                .row(GuideCell.words("DDR4 65536 RDIMM"), GuideCell.words("Advanced, DDR4"),
                        GuideCell.words("16,384 items"), GuideCell.amount(18, "W"))
                .row(GuideCell.words("DDR5 16384"), GuideCell.words("Advanced, DDR5"), GuideCell.words("4,096 items"),
                        GuideCell.amount(12, "W"))
                .row(GuideCell.words("DDR5 32768"), GuideCell.words("Advanced, DDR5"), GuideCell.words("8,192 items"),
                        GuideCell.amount(14, "W"))
                .row(GuideCell.words("DDR5 65536 RDIMM"), GuideCell.words("Advanced, DDR5"),
                        GuideCell.words("16,384 items"), GuideCell.amount(18, "W"))
                .row(GuideCell.words("DDR5 131072 RDIMM"), GuideCell.words("Advanced, DDR5"),
                        GuideCell.words("32,768 items"), GuideCell.amount(22, "W"))
                .paragraph("In a Mainframe, the RAM buffer has one more job: its items wait in the buffer on their"
                        + " way in"
                        + " or out of the network ([Mainframes](jsc:mainframes)).")
                .ifSomethingGoesWrong("The module is refused.",
                        "It is of a generation the board does not take, or of another era. Use a module the board"
                                + " lists, of its own era.",
                        "The build says the RAM generation is not accepted by the board.",
                        "Use a module of a generation the board lists.",
                        "The build says there is no RAM.",
                        "Put in a module of a kind the board takes.")
                .seeAlso("jsc:processors", "jsc:motherboards", "jsc:mainframes")
                .register();
    }

    private static void graphicsCards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("graphics_cards", section).titled("Graphics cards").icon(HardwareItems.GPU_VERTEX_GTX_970)
                .coversAll(itemsOf(GpuItem.class))
                .paragraph("The graphics card draws the picture: it lights the monitors and draws the windows"
                        + " on them. A"
                        + " computer with no graphics card has nothing to show, unless its processor has graphics built"
                        + " in.")
                .paragraph("A card has a memory of its own, the video memory (VRAM). Every monitor uses some of it, a"
                        + " bigger monitor more, and every window that draws a picture of its own uses some too.")
                .define("Video memory (VRAM)", "The graphics card's own memory. Monitors and the windows that draw"
                        + " pictures use it.")
                .paragraph("A processor with graphics built in has no video memory of its own. It takes a quarter"
                        + " of the"
                        + " computer's RAM for its picture, up to 1,792 MB. A computer with a graphics card of its own"
                        + " does not share its RAM this way.")
                .paragraph("In a Mainframe, each card adds one parallel queue: a line of work the Mainframe runs at the"
                        + " same time as the others. A Mainframe with three cards runs four queues, one from its"
                        + " processors and one for each card ([States, priorities and queues](jsc:states_and_queues)).")
                .paragraph("A card's queue never runs faster than the processors of the Mainframe. A card whose"
                        + " queue runs slower than that is shown in amber.")
                .paragraph("Cards go in the board's expansion slots ([Expansion cards](jsc:expansion_cards)). A card in"
                        + " an older slot runs slower than its tooltip says. The table shows how much of its speed it"
                        + " keeps.")
                .table("A card in an older slot", "Slot compared with the card", "Speed the card keeps")
                .row(GuideCell.words("The same generation or newer"), GuideCell.words("All of it"))
                .row(GuideCell.words("One generation older"), GuideCell.words("Half"))
                .row(GuideCell.words("Two generations older"), GuideCell.words("A quarter"))
                .row(GuideCell.words("Three or more generations older"),
                        GuideCell.words("An eighth, which is the least it keeps"))
                .paragraph("The same share cuts the video memory the computer counts for the card, and the card's"
                        + " queue.")
                .paragraph("There are 76 cards, from the VGA-256 to the Envya Vertex RTX 5090. Hover a card and its"
                        + " tooltip shows you its cores, its video memory and its power.")
                .subheading("Video memory for the monitors")
                .paragraph("A monitor is counted in blocks of its big screen. Its video memory cost is its number of"
                        + " blocks, times the cost of one block in its era.")
                .table("Video memory for one block of a monitor", "Era", "Video memory for one block")
                .row(GuideCell.words("Vintage"), GuideCell.words("64 KB (monochrome) or 256 KB (16 colours)"))
                .row(GuideCell.words("Legacy"), GuideCell.words("4 MB"))
                .row(GuideCell.words("Transition"), GuideCell.words("16 MB"))
                .row(GuideCell.words("Standard"), GuideCell.words("64 MB"))
                .row(GuideCell.words("Advanced"), GuideCell.words("256 MB"))
                .row(GuideCell.words("Exa"), GuideCell.words("1 GB"))
                .row(GuideCell.words("Singularity"), GuideCell.words("4 GB"))
                .paragraph("A window that draws a picture costs a quarter of a colour block of its era, or a whole"
                        + " block"
                        + " while it is maximised. Only graphical programs take that memory, and a minimised"
                        + " window takes"
                        + " none.")
                .ifSomethingGoesWrong("The monitor stays dark while the computer is off.",
                        "Switch the computer on. Monitors light only while the computer runs.",
                        "The monitor stays dark.",
                        "No graphics card is in the computer, and its processor has no graphics. Put in a card, or"
                                + " use a"
                                + " processor with graphics built in.",
                        "The monitor says \"Not enough video memory: this monitor needs <a>, and <b> are free\".",
                        "Use a card with more video memory, or fewer or smaller monitors.",
                        "A card does less than its tooltip says.",
                        "It sits in an older slot. Move it to a newer slot.")
                .seeAlso("jsc:monitors", "jsc:expansion_cards", "jsc:states_and_queues")
                .register();
    }

    private static void disks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("disks", section).titled("Disks").icon(HardwareItems.DISK_TRENCH_20M)
                .coversAll(itemsOf(DiskItem.class))
                .paragraph("A disk keeps what must survive turning the computer off: its system, its programs and your"
                        + " files. The size of a disk is counted in items, as [Items as data](jsc:items_as_data)"
                        + " explains.")
                .paragraph("There are three kinds of disk, and they differ in speed. The hard disk is the slowest, the"
                        + " SSD is four times as fast, and the NVMe is sixteen times as fast.")
                .define("Hard disk (HDD)", "The slowest kind of disk here.")
                .define("Solid-state drive (SSD)", "A disk four times as fast as a hard disk.")
                .define("NVMe", "A fast storage standard, short for Non-Volatile Memory Express. It is the quickest"
                        + " kind of disk here.")
                .define("Latency", "The ticks a disk waits before it starts a transfer. The table gives the defaults.")
                .table("The kinds of disk", "Kind", "Speed", "Wait before a transfer (default)", "Power")
                .row(GuideCell.words("Hard disk (HDD)"), GuideCell.code("x1"), GuideCell.words("10 ticks"),
                        GuideCell.amount(6, "W"))
                .row(GuideCell.words("Solid-state drive (SSD)"), GuideCell.code("x4"), GuideCell.words("3 ticks"),
                        GuideCell.amount(3, "W"))
                .row(GuideCell.words("NVMe"), GuideCell.code("x16"), GuideCell.words("1 tick"),
                        GuideCell.amount(5, "W"))
                .paragraph("A disk's size is counted in items of its era. A"
                        + " 20 MB Vintage drive holds 20 items. A 1 TB disk of a later era holds 4,096 items"
                        + " ([Units and ticks](jsc:units_and_ticks)).")
                .table("Disk sizes", "Size", "Items", "Kinds it comes in")
                .row(GuideCell.words("500 GB"), GuideCell.words("2,000 items"),
                        GuideCell.words("Hard disk, SSD and NVMe"))
                .row(GuideCell.words("1 TB"), GuideCell.words("4,096 items"),
                        GuideCell.words("Hard disk, SSD and NVMe"))
                .row(GuideCell.words("2 TB"), GuideCell.words("8,192 items"),
                        GuideCell.words("Hard disk, SSD and NVMe"))
                .row(GuideCell.words("4 TB"), GuideCell.words("16,384 items"),
                        GuideCell.words("Hard disk, SSD and NVMe"))
                .row(GuideCell.words("8 TB"), GuideCell.words("32,768 items"),
                        GuideCell.words("Hard disk, SSD and NVMe"))
                .row(GuideCell.words("12 TB"), GuideCell.words("49,152 items"), GuideCell.words("Hard disk only"))
                .row(GuideCell.words("16 TB"), GuideCell.words("65,536 items"), GuideCell.words("Hard disk only"))
                .row(GuideCell.words("20 TB"), GuideCell.words("81,920 items"), GuideCell.words("Hard disk only"))
                .row(GuideCell.words("24 TB"), GuideCell.words("98,304 items"), GuideCell.words("Hard disk only"))
                .paragraph("The Vintage disks are Vaultis Trench HDD drives of 20M, 100M and 200M. The Legacy"
                        + " hard disks are Vaultis Link IDE-HDD drives of 4G, 20G and 40G, and the Transition SSD"
                        + " is a Vaultis Link"
                        + " SATA-SSD of 64G.")
                .paragraph("A disk fits a computer of its own era or of a later one. A disk newer than the computer"
                        + " is refused, in a disk slot, in the hot-swap bay and in a rack's drive bay.")
                .paragraph("The board's disk slots count the disks you put in them. A disk in the hot-swap bay"
                        + " takes none"
                        + " of those slots. Each disk also draws power, as the table shows.")
                .ifSomethingGoesWrong("The disk does not go into the disk slot.",
                        "It is newer than the computer. Use a disk of the computer's era or of an older one.",
                        "The build says there are too many disks.",
                        "Move disks to the Hot-Swap Drive Bay, or use a board with more disk slots"
                                + " ([the build refusals](jsc:build_refusals)).")
                .seeAlso("jsc:case_front", "jsc:items_as_data", "jsc:the_systems")
                .register();
    }

    private static void powerSupplies(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("power_supplies", section).titled("Power supplies").icon(HardwareItems.PSU_200)
                .coversAll(itemsOf(PsuItem.class))
                .paragraph("Most parts draw power, and the power supply gives it. The processors, the memory,"
                        + " the graphics cards, the disks and the expansion cards each draw watts. The supply has"
                        + " to give at least their total.")
                .paragraph("There are twelve supplies, from 200 W to 3,000 W, each with its own efficiency.")
                .define("Efficiency", "The share of the power a supply takes in that reaches the parts. The build check"
                        + " does not use it.")
                .table("The power supplies", "Power supply", "Era", "Power", "Efficiency")
                .row(GuideCell.words("MF PowerBasic 200"), GuideCell.words("Vintage"), GuideCell.words("200 W"),
                        GuideCell.words("80%"))
                .row(GuideCell.words("MF PowerBasic 300"), GuideCell.words("Vintage"), GuideCell.words("300 W"),
                        GuideCell.words("80%"))
                .row(GuideCell.words("MF PowerBasic 350"), GuideCell.words("Legacy"), GuideCell.words("350 W"),
                        GuideCell.words("80%"))
                .row(GuideCell.words("MF PowerBasic 500B"), GuideCell.words("Legacy"), GuideCell.words("500 W"),
                        GuideCell.words("80%"))
                .row(GuideCell.words("MF PowerBasic 450B"), GuideCell.words("Transition"), GuideCell.words("450 W"),
                        GuideCell.words("85%"))
                .row(GuideCell.words("MF PowerGold 650G"), GuideCell.words("Standard"), GuideCell.words("650 W"),
                        GuideCell.words("90%"))
                .row(GuideCell.words("MF PowerGold 850G"), GuideCell.words("Standard"), GuideCell.words("850 W"),
                        GuideCell.words("90%"))
                .row(GuideCell.words("MF PowerGold 1000G"), GuideCell.words("Advanced"), GuideCell.words("1,000 W"),
                        GuideCell.words("90%"))
                .row(GuideCell.words("MF PowerPlat 1200P"), GuideCell.words("Advanced"), GuideCell.words("1,200 W"),
                        GuideCell.words("92%"))
                .row(GuideCell.words("MF PowerPlat 1600P"), GuideCell.words("Advanced"), GuideCell.words("1,600 W"),
                        GuideCell.words("92%"))
                .row(GuideCell.words("MF ServerPSU 2000P"), GuideCell.words("Advanced"), GuideCell.words("2,000 W"),
                        GuideCell.words("94%"))
                .row(GuideCell.words("MF ServerPSU 3000P (Redundant)"), GuideCell.words("Advanced"),
                        GuideCell.words("3,000 W"), GuideCell.words("94%"))
                .paragraph("The build adds up the draw of the processors, the memory, the disks and the"
                        + " expansion cards."
                        + " If the sum is more than the supply gives, the build fails with PSU insufficient"
                        + " ([When the build is refused](jsc:build_refusals)).")
                .paragraph("On a rack server, the DRAW readout shows the draw and the supply's watts, and it turns"
                        + " red when"
                        + " the draw is over. A tower has no draw readout, so keep the sum in mind when you choose.")
                .paragraph("A computer whose supply gives less than its parts draw does not start. It stays off, and a"
                        + " server says why.")
                .ifSomethingGoesWrong("The computer does not start, or the build says \"PSU insufficient\".",
                        "Its parts draw more than its supply gives. Use a supply with more watts.")
                .seeAlso("jsc:build_refusals", "jsc:disks", "jsc:processors")
                .register();
    }

    private static void soundCards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sound_cards", section).titled("Sound cards").icon(HardwareItems.SOUND_CARD_TONE_BLASTER)
                .coversAll(itemsOf(SoundCardItem.class))
                .paragraph("Sound comes from the board or from a sound card. A Vintage or Legacy board"
                        + " has no sound of its own, so it needs a sound card for anything more than a beep.")
                .define("PC speaker", "The small beeper of the computer. On a Vintage or Legacy board with no"
                        + " sound card,"
                        + " it is the only sound.")
                .paragraph("The Artisan Tone Blaster family gives you the sound cards. Each one fits one era"
                        + " and one bus.")
                .table("The sound cards", "Sound card", "Era", "Bus")
                .row(GuideCell.words("Artisan Tone Blaster"), GuideCell.words("Vintage"), GuideCell.words("ISA"))
                .row(GuideCell.words("Artisan Tone Blaster 128"), GuideCell.words("Vintage"), GuideCell.words("PCI"))
                .row(GuideCell.words("Artisan Tone Blaster Live"), GuideCell.words("Legacy"),
                        GuideCell.words("AGP 8x"))
                .row(GuideCell.words("Artisan Tone Blaster Audigy"), GuideCell.words("Legacy"),
                        GuideCell.words("PCI-e 1.0"))
                .row(GuideCell.words("Artisan Tone Blaster Hi-Fi"), GuideCell.words("Transition"),
                        GuideCell.words("PCI-e 1.0"))
                .paragraph("A computer takes one sound card, of its board's era, and the card's bus must belong to the"
                        + " board's bus family ([the bus family](jsc:build_refusals)).")
                .paragraph("From the Transition on, a board has sound of its own, so the card is no longer needed. Its"
                        + " tooltip says \"Sound on the board: plays everything\". The earlier boards say \"PC speaker:"
                        + " beeps only\".")
                .paragraph("The sound you hear comes out through the monitors linked to the computer, and through"
                        + " speakers joined"
                        + " to it by peripheral cable ([the speakers](jsc:speakers)). With no monitor"
                        + " and no speaker linked, the sound goes to the PC speaker. Otherwise the sound card plays"
                        + " if there is"
                        + " one, then the board's own sound if it has any, and otherwise the PC speaker.")
                .ifSomethingGoesWrong("The build says a sound card of one era does not sit on a board of another.",
                        "Use a sound card of the board's era.",
                        "The build says \"one sound card per machine\".",
                        "Take one of the two sound cards out.",
                        "A Vintage or Legacy computer only beeps.",
                        "It has no sound card, and no monitor or speaker is linked to carry its sound. Put in a sound"
                                + " card of the board's era, or link a monitor or speakers.")
                .seeAlso("jsc:speakers", "jsc:monitors", "jsc:motherboards")
                .register();
    }

    private static void expansionCards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("expansion_cards", section).titled("Expansion cards").icon(ComputingModule.FURNACE_CARD)
                .coversAll(itemsOf(WorkshopCardItem.class, ClusterInterfaceCardItem.class, NetworkCardItem.class,
                        PhiCoprocessorItem.class))
                .paragraph("Besides graphics and sound, the expansion slots of a board take cards that give a"
                        + " computer a"
                        + " job it could not do before. Each card goes in a slot of its own bus family"
                        + " ([the bus family](jsc:build_refusals)).")
                .subheading("Workshop cards")
                .paragraph("The Crafting Table Card, the Furnace Card, the Enchanting Card and the Anvil Card each"
                        + " give a"
                        + " Personal Computer one job in the Workshop program. Only a Personal Computer takes them, and"
                        + " each one draws 15 W.")
                .paragraph("The Workshop shows \"No fuel\" when smelting has no fuel, and \"No lapis\" when"
                        + " enchanting has"
                        + " no lapis.")
                .subheading("Cluster Interface Cards")
                .paragraph("Four cards let a Cluster Management Computer reach the machines of your racks: the Serial"
                        + " Console Card, the Management NIC, the Fabric Host Adapter and the Fabric DPU. Each newer"
                        + " one"
                        + " reaches more. A Cluster Management Computer takes one of them"
                        + " ([Cluster Management Computers](jsc:cluster_management_computers)).")
                .table("The Cluster Interface Cards", "Card", "Era and bus", "Reaches", "Lanes")
                .row(GuideCell.words("Serial Console Card"), GuideCell.words("Vintage, PCI"),
                        GuideCell.words("Rack servers in datacenters"), GuideCell.words("1 node at a time"))
                .row(GuideCell.words("Management NIC"), GuideCell.words("Legacy, PCI-e 1.0"),
                        GuideCell.words("Datacenters and supercomputer fabrics"), GuideCell.words("2 nodes at a time"))
                .row(GuideCell.words("Fabric Host Adapter"), GuideCell.words("Standard, PCI-e 3.0"),
                        GuideCell.words("Everything, every kind of rack"), GuideCell.words("4 nodes at a time"))
                .row(GuideCell.words("Fabric DPU"), GuideCell.words("Advanced, PCI-e 4.0"),
                        GuideCell.words("Everything, every kind of rack"), GuideCell.words("8 nodes at a time"))
                .define("Reach", "The kinds of machine a card can reach, from the rack servers of a datacenter"
                        + " up to every kind of rack.")
                .define("Lane", "The number of nodes a bulk install writes at once. In an older slot the lanes fall,"
                        + " but never"
                        + " below one.")
                .paragraph("Only a Cluster Management Computer uses these cards.")
                .subheading("Optical Network Card")
                .paragraph("A network card with a fibre port. A Mainframe or a Cluster Management Computer with one"
                        + " shows its fibre port as connected, so it can join the fibre backbone of the network"
                        + " ([Data cables](jsc:data_cables)). A server never takes it: a rack joins the fibre through"
                        + " a Fibre Optic Interface ([](jsc:rack_equipment)). It is a Standard card, on PCI-e 3.0, and"
                        + " it draws 15 W.")
                .subheading("Integra Phi coprocessors")
                .paragraph("Four Integra Phi coprocessors add computing power to the nodes of a supercomputer. Only a"
                        + " supercomputer node takes one, and only one per node. A rack server of another kind"
                        + " refuses it"
                        + " ([Server racks and servers](jsc:server_racks)).")
                .table("The Integra Phi coprocessors", "Coprocessor", "Cores and clock", "Power", "Supercomputer slots")
                .row(GuideCell.words("Integra Phi 5100 Co-processor"), GuideCell.words("60 cores at 1.05 GHz"),
                        GuideCell.words("225 W"), GuideCell.words("1 to 2"))
                .row(GuideCell.words("Integra Phi 7120 Co-processor"), GuideCell.words("61 cores at 1.24 GHz"),
                        GuideCell.words("250 W"), GuideCell.words("1 to 3"))
                .row(GuideCell.words("Integra Phi 7290 Co-processor"), GuideCell.words("72 cores at 1.50 GHz"),
                        GuideCell.words("270 W"), GuideCell.words("1 to 4"))
                .row(GuideCell.words("Integra Phi 9000 Co-processor"), GuideCell.words("96 cores at 1.80 GHz"),
                        GuideCell.words("300 W"), GuideCell.words("1 to 6"))
                .paragraph("The last column is the range of supercomputer slots that the tooltip gives, as in"
                        + " \"Supercomputer slots 1-2\".")
                .subheading("Crafting Cards")
                .paragraph("The Crafting Cards give a Crafting Computer its crafting. There are six, from the"
                        + " Vintage to"
                        + " the Advanced, and autocrafting explains them ([Crafting Cards](jsc:crafting_cards)).")
                .table("The Crafting Cards", "Era", "Bus", "Power")
                .row(GuideCell.words("Vintage"), GuideCell.words("ISA"), GuideCell.amount(10, "W"))
                .row(GuideCell.words("Legacy"), GuideCell.words("PCI"), GuideCell.amount(25, "W"))
                .row(GuideCell.words("Transition"), GuideCell.words("PCI-e 1.0"), GuideCell.amount(75, "W"))
                .row(GuideCell.words("Transition"), GuideCell.words("PCI-e 2.0"), GuideCell.amount(100, "W"))
                .row(GuideCell.words("Standard"), GuideCell.words("PCI-e 3.0"), GuideCell.amount(110, "W"))
                .row(GuideCell.words("Advanced"), GuideCell.words("PCI-e 4.0"), GuideCell.amount(125, "W"))
                .subheading("Storage cards")
                .paragraph("Vaultis makes a RAID controller and a read cache for each era's slot. In a server they"
                        + " join its drives into one volume and answer their reads faster; in another computer they"
                        + " sit in their slot and do nothing ([RAID modes](jsc:raid_arrays)).")
                .ifSomethingGoesWrong("The Workshop shows \"No fuel\" or \"No lapis\".",
                        "Give smelting its fuel, or give enchanting its lapis.",
                        "A Workshop card does not go into a computer.",
                        "Only a Personal Computer takes Workshop cards. Put it in one.",
                        "A second Cluster Interface Card does not go in.",
                        "A Cluster Management Computer takes only one. Take the other one out.",
                        "A Cluster Interface Card does nothing.",
                        "Only a Cluster Management Computer uses it. Put it in one.",
                        "The build says there are too many PCI-e cards.",
                        "Take cards out, or use a board with more PCI-e slots.")
                .seeAlso("jsc:graphics_cards", "jsc:sound_cards", "jsc:build_refusals")
                .register();
    }
}
