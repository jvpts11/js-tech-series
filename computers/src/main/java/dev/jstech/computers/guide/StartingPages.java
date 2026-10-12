/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The first chapter of the Guide to Operations: how the book works, what a computer is here, how to build and start
 * one, the firmware and the Setup, the eras and the systems, and the units the book uses. Every entry explains from
 * zero, starts with the idea, and ends with what can go wrong.
 */
final class StartingPages {

    /*
     * A Standard computer with a build that is valid together: an ATX board and a processor of the same socket, two
     * memory modules of the board's kind, a graphics card, a disk and a power supply that carries their draw. The
     * lamps value is what the server sends a client (bit 0 is the power lamp), so the scene shows it running.
     */
    private static final String COMPUTER_RUNNING = "{Hardware:{Size:15,Items:["
            + "{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},"
            + "{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},"
            + "{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},"
            + "{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},"
            + "{Slot:11,id:\"jsc:disk_ssd_1t\",count:1}]},ManualOn:1b,Lamps:1}";

    private StartingPages() {
    }

    /** Declares the sections and entries of the chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        gettingStarted(guide, chapter);
        startingAComputer(guide, chapter);
        systemsAndEras(guide, chapter);
        howThingsAreCounted(guide, chapter);
    }

    private static void gettingStarted(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("getting_started").titled("Getting started")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        guide.page("welcome", section).titled("Welcome to J's Computers")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .covers(ComputersGuide.MANUAL)
                .shows(ComputingModule.PERSONAL_COMPUTER, ComputingModule.MONITOR, ComputingModule.MAINFRAME,
                        ComputingModule.SERVER_RACK, ComputingModule.CRAFTING_COMPUTER)
                .paragraph("J's Computers is about computers. But what is a computer here? You might expect a box"
                        + " that you place and switch on. It is that, but it is also built part by part, the way a"
                        + " real one is.")
                .paragraph("Computers that are joined by data cable to one Mainframe ([](jsc:mainframes)) make a"
                        + " network ([](jsc:what_a_network_is)). The network keeps your items on its disks, moves"
                        + " them, and crafts what you ask for.")
                .paragraph("When a computer starts, the first thing it runs is the firmware ([](jsc:firmware)): the"
                        + " program that checks the hardware and finds the disks. Then it hands over to an operating"
                        + " system, the system you use day to day.")
                .paragraph("To build a working computer you need parts: a motherboard, a processor (CPU), memory"
                        + " (RAM) and a power supply (PSU). A disk holds the system, and a graphics card (GPU) gives"
                        + " the monitor a picture. Each part has its own entry, and [Building your first"
                        + " computer](jsc:first_computer) puts them together.")
                .paragraph("Every part of this mod comes from the creative tabs: J's Computers has no crafting recipes"
                        + " of its own. J's Computers holds what every era shares, and there is one tab for each era,"
                        + " from J's Computers - Vintage to J's Computers - Advanced ([](jsc:eras)). A modpack can"
                        + " give the mod recipes, with KubeJS or a datapack; then the recipe viewer, JEI or EMI,"
                        + " shows how each thing is made.")
                .subheading("How to read this manual")
                .paragraph("Every entry starts on a page of its own. The row of items under an entry's title, the"
                        + " plate, is clickable: click an item to go to its own page. A number in brackets is a link"
                        + " to another entry.")
                .paragraph("Hold the manual key over an item and the manual opens at the item's page. The manual key"
                        + " is [[M]] unless you changed it.")
                .paragraph("The Search button finds anything in the manual by name. A search that finds nothing says"
                        + " \"Nothing by"
                        + " that name\".")
                .paragraph("The buttons Contents, Index and Glossary, and Back and Forward, move you around the book.")
                .paragraph("This book, the Guide to Operations, is one of seven books. The other six are the Frames"
                        + " Resource Kit, The Linux Desk Reference, Linux by Hand, The Daemon Handbook, Learning IQL"
                        + " and The Book of Sigma. Each book is an item in the J's Computers creative tab, and the"
                        + " library ([](jsc:library)) says what each one covers.")
                .define("Firmware", "The program on the motherboard that starts the computer. It is the first thing"
                        + " that runs when you turn the computer on.")
                .define("Operating system", "The system a computer starts into, and what every program runs on."
                        + " MC-DOS, Frames and the Linux distributions are operating systems.")
                .define("Era", "A generation of hardware, such as Vintage or Standard. Parts of one era are built to"
                        + " fit the boards of that era.")
                .seeAlso("jsc:tour", "jsc:library")
                .register();
        guide.page("tour", section).titled("A tour of J's Computers").icon(ComputingModule.MONITOR)
                .paragraph("This mod has many parts. Before you pick one, it helps to see the whole, from the"
                        + " computers you build to the network they make together.")
                .paragraph("The computers come in kinds. A Personal Computer ([](jsc:personal_computers)) is the one"
                        + " you build first. A Mainframe ([](jsc:mainframes)) directs a network.")
                .paragraph("Others are made for other jobs. A Crafting Computer ([](jsc:crafting_computers)) carries"
                        + " the crafting cards, and servers stand in a server rack ([](jsc:server_racks)). A Cluster"
                        + " Management Computer ([](jsc:cluster_management_computers)) helps you run many machines"
                        + " at once.")
                .paragraph("Computers are built from parts, one by one: a motherboard, a processor, memory, a graphics"
                        + " card, a disk and a power supply. [Building your first computer](jsc:first_computer)"
                        + " shows how they fit.")
                .paragraph("A computer shows its picture on a Monitor. A monitor is joined to a computer by standing"
                        + " right against it, or by a [Peripheral Cable](jsc:peripheral_cables) when it stands"
                        + " further away.")
                .paragraph("Drives read the media that systems are installed from ([](jsc:drives)). The Dock Station"
                        + " ([](jsc:dock_station)) reads USB sticks. Printers print, speakers play sound, and a"
                        + " Redstone Interface ([](jsc:redstone_interfaces)) links a computer to redstone.")
                .paragraph("Computers of one network are joined by data cable ([](jsc:data_cables)). J's Computers"
                        + " has no channels and no subnetworks at all: one network is one Mainframe and its"
                        + " computers. Routers and repeaters ([](jsc:routers)) help the network reach further.")
                .paragraph("[Operations](jsc:what_operations_are) are jobs the network does, such as selecting,"
                        + " moving or crafting items. You send them in IQL ([](jsc:iql)), the network's query"
                        + " language.")
                .paragraph("Items live on the disks as data ([](jsc:items_as_data)). Buses ([](jsc:buses)) move them"
                        + " in and out, and autocrafting ([](jsc:how_autocrafting_works)) crafts what you ask for,"
                        + " from patterns.")
                .paragraph("Each computer's help opens on the book of its own system ([](jsc:help_on_a_computer)).")
                .subheading("Where to start")
                .steps("Build one Personal Computer ([](jsc:first_computer)).",
                        "Turn it on ([](jsc:turning_on)).",
                        "Install a system on it ([](jsc:installing)).",
                        "Read what a network is ([](jsc:what_a_network_is)), then build your first one"
                                + " ([](jsc:first_network)).")
                .seeAlso("jsc:welcome", "jsc:first_computer")
                .register();
        guide.page("first_computer", section).titled("Building your first computer")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .shows(ComputingModule.PERSONAL_COMPUTER, HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150,
                        HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K, ComputingModule.RAM_DDR3_8192,
                        HardwareItems.GPU_VERTEX_GTX_970, ComputingModule.PSU_650G,
                        () -> ComputingModule.disk(StorageTier.SSD, DiskSize.TB_1), ComputingModule.MONITOR,
                        ComputingModule.DOCK_STATION)
                .paragraph("Building a computer is easier than it sounds. You place a case, put the parts in it one"
                        + " by one, and then you add a monitor.")
                .paragraph("Take a Personal Computer from the creative tab J's Computers - Standard and place it."
                        + " Using the computer opens its window, which is the inside of the case, with one place for"
                        + " each part. A case on its own does nothing yet.")
                .subheading("The parts")
                .steps("Put a motherboard in. It is the board the other parts sit on, and the case takes only a"
                                + " board of its own era ([](jsc:motherboards)).",
                        "Put a processor in. Its socket must match the board's socket ([](jsc:processors)).",
                        "Put memory in. The board takes some kinds of memory and not others ([](jsc:memory)).",
                        "Put a graphics card in. Without one the monitor has no picture, unless the processor has"
                                + " graphics built in ([](jsc:graphics_cards)).",
                        "Put a disk in. The system is installed on it, and your files are kept on it"
                                + " ([](jsc:disks)).",
                        "Put a power supply in. It gives the parts their power, rated in watts (W)"
                                + " ([](jsc:power_supplies)).")
                .paragraph("A computer with no disk still switches on, and then it reports that no system can"
                        + " start ([](jsc:no_bootable_device)). A computer with no motherboard or no power supply has"
                        + " no build at all, so it stays off.")
                .paragraph("A part that does not fit does not go into its slot. A processor or a memory module must"
                        + " match the board's socket, era and memory generation. A graphics card must fit the"
                        + " board's bus.")
                .paragraph("A power supply with a fixed wattage refuses the build when the parts draw more watts than"
                        + " it gives. The switch goes on, but the computer does not run.")
                .paragraph("On a Personal Computer, the window only says that it is not a computer yet, and what it"
                        + " still needs. The assembly screen of a server lists each problem in its tooltip.")
                .subheading("The monitor")
                .paragraph("A computer with no screen is not much use. Place a Monitor ([](jsc:monitors)) right"
                        + " against the case, and it is joined to the computer. Lay a"
                        + " [Peripheral Cable](jsc:peripheral_cables) when the monitor has to stand further away.")
                .scene("A Personal Computer with its Monitor, joined by a Peripheral Cable. Drag it to see the back.",
                        120)
                .place(14, 0, 15, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south", COMPUTER_RUNNING)
                .place(16, 0, 15, () -> ComputingModule.MONITOR.get(), "facing=north,button=true,lit=true,on=true",
                        "{Lit:1b,SwitchedOn:1b}")
                .place(15, 0, 15, () -> CoreCables.BLOCK.get(), "", cable(4116))
                .place(15, 0, 14, () -> CoreCables.BLOCK.get(), "", cable(40))
                .place(16, 0, 14, () -> CoreCables.BLOCK.get(), "", cable(2072))
                .balloon(1, GuideBlock.ScenePart.of(14, 0, 15), "The Personal Computer")
                .balloon(2, GuideBlock.ScenePart.of(14, 0, 15).box(12, 9, 15, 14, 12, 16), "Power button",
                        "Press it to turn the computer on.")
                .balloon(3, GuideBlock.ScenePart.of(16, 0, 15), "The Monitor")
                .balloon(4, GuideBlock.ScenePart.of(15, 0, 15), "The Peripheral Cable",
                        "It joins the monitor to the computer when the monitor cannot stand against the case.")
                .paragraph("The Dock Station ([](jsc:dock_station)) reads USB sticks, and that is where a USB install"
                        + " medium goes. Join it to the computer like any other drive.")
                .define("Install medium", "The floppy, CD, DVD or USB stick a system is installed from. The installer"
                        + " is the program on it.")
                .paragraph("The system and its programs show on the monitor, not in the window. The front of the case"
                        + " has the power button ([](jsc:case_front)).")
                .subheading("Turning it on")
                .paragraph("Press the power button on the front of the case ([](jsc:case_front)). The monitor shows"
                        + " the start-up, and if no system is on any disk, the firmware waits for you"
                        + " ([](jsc:no_bootable_device)).")
                .paragraph("Once the computer runs, the monitor's power strip offers the buttons Shut down the PC and"
                        + " Restart the PC.")
                .steps("Put the install medium of Frames 10 in the Dock Station.",
                        "Turn the computer on, and watch the monitor ([](jsc:turning_on)).",
                        "Start the installer from the medium, and follow it ([](jsc:installing)).")
                .ifSomethingGoesWrong("The window's tooltip says \"Not a computer yet\".",
                        "A build needs a motherboard, a processor, memory and a power supply. The tooltip also names"
                                + " a disk: put one in as well, so the computer has a system to start from.",
                        "The switch goes on, but the computer does not run.",
                        "The build is refused. Check the parts against each other: the socket, the era, the memory"
                                + " generation and the power ([](jsc:build_refusals)).",
                        "The monitor says \"No computer found in range over a Peripheral Cable\".",
                        "Put the monitor right against the case, or lay a Peripheral Cable between them"
                                + " ([](jsc:peripheral_cables)).",
                        "The monitor says \"The computer has no video output: install a graphics card\".",
                        "Put a graphics card in the case, or use a processor with graphics built in"
                                + " ([](jsc:graphics_cards)).",
                        "The monitor says \"No signal - the computer is powered off\".",
                        "Press the power button on the front of the case ([](jsc:case_front)).",
                        "The computer beeps, and the monitor says that its parts do not make a computer.",
                        "A part is missing or does not fit. Open the computer: its CHECKS list names what to fix"
                                + " ([](jsc:build_refusals)).",
                        "A processor or a memory module does not go into its slot.",
                        "Check the board: the socket, the era and the memory generation must match"
                                + " ([](jsc:motherboards)).",
                        "The firmware reports that no system can start.",
                        "No system is on a disk yet. Install one ([](jsc:installing)).")
                .seeAlso("jsc:turning_on", "jsc:installing")
                .register();
    }

    private static void startingAComputer(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("starting_a_computer").titled("Starting a computer")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        guide.page("turning_on", section).titled("Turning the computer on").icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Turning a computer on starts a sequence that you can watch on its monitor. It begins"
                        + " with a check of the hardware, and then it looks for a system to start.")
                .paragraph("The check is the power-on self-test (POST). The firmware ([](jsc:firmware)) checks each"
                        + " part it finds, and lists it on the screen, one line after another.")
                .subheading("Turning it on")
                .steps("Press the power button on the front of the case ([](jsc:case_front)).",
                        "Watch the monitor while the self-test runs.",
                        "Press [[DEL]] during the self-test to enter the Setup, or [[F12]] to open the Boot Menu.")
                .paragraph("If you switch the computer off during its self-test, its screen stays up until you press"
                        + " [[Esc]].")
                .table("The keys while the computer starts", "Key", "What it does")
                .row(GuideCell.code("DEL"), GuideCell.words("Enters the Setup, while the self-test runs."))
                .row(GuideCell.code("F12"), GuideCell.words("Opens the Boot Menu, when a disk or medium can start"
                        + " the computer."))
                .row(GuideCell.words("Any key"), GuideCell.words("After a self-test that finds nothing to start,"
                        + " goes to the Setup."))
                .subheading("What the start-up screen shows")
                .paragraph("A new line appears about every 0.15 seconds. These are the lines, in order.")
                .table("The lines of the start-up", "Line", "What it tells you")
                .row(GuideCell.code("Processor :"), GuideCell.words("The processor's name and its cores and clock"
                        + " speed, or \"not detected\". Legacy machines say Main Processor."))
                .row(GuideCell.code("Board     :"), GuideCell.words("The motherboard's name. Legacy machines do not"
                        + " show this line."))
                .row(GuideCell.code("Memory    :"), GuideCell.words("The memory counted so far, which climbs while"
                        + " it is tested, and then OK."))
                .row(GuideCell.code("Video     :"), GuideCell.words("The graphics card's name, or none when there is"
                        + " no card. Legacy machines say Video Adapter."))
                .row(GuideCell.code("Detecting drives"), GuideCell.words("The firmware looks for disks and drives,"
                        + " and lists each one it finds."))
                .row(GuideCell.code("Booting from"), GuideCell.words("The disk the computer starts from, once it has"
                        + " found a system to start."))
                .paragraph("The self-test takes between 2 and 12 seconds. The more memory modules and devices it finds,"
                        + " the longer it takes, and a newer era finishes sooner.")
                .paragraph("After the self-test the system loads, which takes between 2 and 20 seconds. A big system"
                        + " and short memory make it longer. Shutting down takes a third of the boot time, but never"
                        + " less than 1 second or more than 5 seconds.")
                .define("Power-on self-test (POST)", "The check the firmware runs each time the computer starts. It"
                        + " lists each part it finds, one line at a time.")
                .define("Setup", "The firmware's own settings screens. Its pages show the disks and the hardware the"
                        + " firmware found.")
                .define("Boot Menu", "The list the firmware opens with F12 to start a disk or medium, for this start"
                        + " only.")
                .ifSomethingGoesWrong("Nothing starts when you press the power button.",
                        "The computer has no motherboard or no power supply, so it has no build at all. Add them"
                                + " ([](jsc:first_computer)).",
                        "The switch goes on, but the computer does not run.",
                        "The parts do not make a valid build. See what each refusal means in"
                                + " ([](jsc:build_refusals)).",
                        "DEL does nothing.",
                        "DEL works only while the self-test runs. Turn the computer off and on again, and press DEL"
                                + " in time.",
                        "F12 does nothing.",
                        "The Boot Menu opens only when a disk or medium can start the computer"
                                + " ([](jsc:no_bootable_device)).")
                .seeAlso("jsc:firmware", "jsc:setup_utility", "jsc:boot_menu")
                .register();
        guide.page("firmware", section).titled("Firmware: the BIOS and the UEFI")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .paragraph("The firmware lives on the motherboard, not on any disk, so it is there before any system."
                        + " It is the first program that runs when the computer starts: it checks the hardware"
                        + " ([](jsc:turning_on)), finds the disks and drives, and hands the computer to a system. If no"
                        + " system is found, it waits on the screen for you.")
                .paragraph("Its kind depends on the era of the motherboard. Older boards have a BIOS, and newer boards"
                        + " have a UEFI. Both are firmware, and each era has its own look:")
                .table("The firmware of each era", "Era", "Firmware on the screen")
                .row(GuideCell.words("Vintage"), GuideCell.words("Text BIOS, plain text on the screen."))
                .row(GuideCell.words("Legacy and Transition"), GuideCell.words("Blue BIOS."))
                .row(GuideCell.words("Standard and Advanced"), GuideCell.words("UEFI."))
                .paragraph("The boot order is the list of disks in the order the firmware tries them. You change the"
                        + " order on the Boot Order page of the Setup ([](jsc:setup_utility)).")
                .define("Boot order", "The order the firmware tries the disks in when the computer starts. The first"
                        + " disk with a system on it starts.")
                .paragraph("The Boot Menu ([](jsc:boot_menu)) is the same choice for one start only. A disk you pick"
                        + " there is not saved, so the next start follows the boot order again.")
                .paragraph("A disk with two systems on it can show a menu before it starts. See"
                        + " [Two systems on one computer](jsc:two_systems).")
                .paragraph("The Setup also has a Hardware page, which lists what the firmware found in the"
                        + " machine. On a server with a storage controller, it has a Storage page for the array of its"
                        + " drives ([](jsc:raid_arrays)).")
                .define("BIOS", "Basic Input/Output System: the older kind of firmware. The Vintage, Legacy and"
                        + " Transition boards have a BIOS.")
                .define("UEFI", "Unified Extensible Firmware Interface: the newer kind of firmware. The Standard and"
                        + " Advanced boards have a UEFI.")
                .ifSomethingGoesWrong("The screen reports that nothing can start, in words that depend on the era.",
                        "No disk has a system yet, and no install medium is in a drive. Put one in, and start from it"
                                + " ([](jsc:installing)). The exact words depend on the era"
                                + " ([](jsc:no_bootable_device)).",
                        "A disk is not in the list.",
                        "The disk is not connected to the computer, or the medium is in the wrong kind of drive."
                                + " A floppy goes in a Floppy Drive and a CD in a CD Drive ([](jsc:drives)).",
                        "The computer starts from the wrong disk.",
                        "Change the boot order on the Boot Order page of the Setup, or pick the disk in the Boot Menu"
                                + " ([](jsc:setup_utility)).")
                .register();
        guide.page("setup_utility", section).titled("The Setup").icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .paragraph("The Setup is the firmware's own settings screen. You open it with [[DEL]] while the"
                        + " self-test runs ([](jsc:turning_on)). The title of the screen ends with BIOS Setup"
                        + " Utility.")
                .paragraph("The Setup has pages. Press [[Tab]] to move to the next one.")
                .table("The pages of the Setup", "Page", "What it shows")
                .row(GuideCell.code("Boot"), GuideCell.words("The entries you can start from. Enter starts the"
                        + " selected one."))
                .row(GuideCell.code("Boot Order"), GuideCell.words("The disks in the order the firmware tries them."
                        + " Enter on a disk sets it as first boot device."))
                .row(GuideCell.code("Hardware"), GuideCell.words("The parts the firmware found in this machine."))
                .row(GuideCell.code("Storage"), GuideCell.words("The RAID modes. Only a machine with a storage"
                        + " controller has this page."))
                .paragraph("The Hardware page lists Processor, Instruction Set, Cores, Memory, Graphics, Video, Board,"
                        + " Monitors, Hardware Era, Boot Disk and Install Target. The Memory line shows the size in"
                        + " megabytes (MB)"
                        + " and how many of the slots hold a module.")
                .table("The keys in the Setup", "Key", "What it does")
                .row(GuideCell.code("Tab"), GuideCell.words("Goes to the next page."))
                .row(GuideCell.code("Up and Down"), GuideCell.words("Move the selection. On the Storage page they"
                        + " change the RAID mode."))
                .row(GuideCell.code("Enter"), GuideCell.words("Activates the selected row: starts an entry, sets a"
                        + " first boot device, or applies a RAID mode."))
                .row(GuideCell.code("F"), GuideCell.words("Formats the selected disk. The first press only arms it,"
                        + " and the second press formats."))
                .row(GuideCell.code("1 to 9"), GuideCell.words("Selects that row."))
                .row(GuideCell.code("Escape"), GuideCell.words("Leaves the Setup."))
                .warning("Formatting a disk erases everything on it, and the data cannot be brought back.")
                .paragraph("The Storage page shows ARRAY MODE with four options for the RAID array"
                        + " ([](jsc:raid_arrays)). Each one needs drives. Enter applies"
                        + " the mode, and changing it erases the array.")
                .table("The RAID modes on the Storage page", "Option", "Note on the screen")
                .row(GuideCell.code("NONE (independent)"), GuideCell.words("None: each drive works on its own."))
                .row(GuideCell.code("RAID 0"), GuideCell.words("+25% throughput"))
                .row(GuideCell.code("RAID 1"), GuideCell.words("survives to 1 drive"))
                .row(GuideCell.code("RAID 5"), GuideCell.words("survives 1 loss"))
                .paragraph("The Exit row leaves the Setup.")
                .ifSomethingGoesWrong("The Storage page says \"No storage controller fitted.\"",
                        "This machine has no storage controller, so it has no RAID modes ([](jsc:raid_arrays)).")
                .seeAlso("jsc:firmware", "jsc:boot_menu", "jsc:installing")
                .register();
        guide.page("boot_menu", section).titled("The Boot Menu and the boot manager")
                .icon(ComputingModule.FLOPPY_DISK)
                .paragraph("The Boot Menu starts a disk or medium for one start only. Press [[F12]] while the computer"
                        + " starts ([](jsc:turning_on)). You choose with the keys below.")
                .table("The keys in the Boot Menu", "Key", "What it does")
                .row(GuideCell.code("Up and Down"), GuideCell.words("Select an entry in the list."))
                .row(GuideCell.code("Enter"), GuideCell.words("Boot the selected entry."))
                .paragraph("The screen is titled Boot Menu. On a blue BIOS machine it reads"
                        + " \"Boot Menu  (this boot only)\", and its list is headed \"Boot Menu:\".")
                .paragraph("The computer does not save the choice, so the next start follows"
                        + " the boot order ([](jsc:firmware)) again.")
                .subheading("Boot managers")
                .paragraph("The boot manager is a different menu from the Boot Menu. Some systems show their own start"
                        + " menu before they start: GRUB does this on Linux, and the Midsoft Boot Manager on Frames,"
                        + " and FreeBSD has its own loader. The boot manager lets you choose between the systems of a"
                        + " disk ([](jsc:two_systems)).")
                .paragraph("If you do nothing, the boot manager counts down, and then it starts the highlighted"
                        + " entry by"
                        + " itself, after 5 seconds. Press Enter to start the highlighted entry at once.")
                .paragraph("Its help reads: \"Use the Up and Down keys to select which entry is highlighted."
                        + " Press Enter"
                        + " to boot the selected entry.\"")
                .paragraph("On a server, the setting boot.show_boot_menu decides whether a machine stops at its boot"
                        + " manager every time it starts. It is on by default. Turn it off, and the chosen system boots"
                        + " at once. The setting is in the jscomputers-server config file.")
                .define("Boot manager", "The system's own start menu, shown before the system starts. It lets you"
                        + " choose one of the systems on a disk.")
                .ifSomethingGoesWrong("F12 does nothing.",
                        "The Boot Menu opens only when a disk or medium can start the computer"
                                + " ([](jsc:no_bootable_device)).",
                        "The boot manager starts the wrong system.",
                        "Press Up or Down to highlight the system you want, then press Enter"
                                + " before the countdown ends.",
                        "The computer stops at the boot manager every time it starts.",
                        "Turn off boot.show_boot_menu in the jscomputers-server config file.")
                .seeAlso("jsc:two_systems", "jsc:help_on_a_computer", "jsc:firmware")
                .register();
        guide.page("no_bootable_device", section).titled("When nothing will boot")
                .icon(ComputingModule.FLOPPY_DISK)
                .paragraph("Sometimes the firmware finds nothing it can start. Then it says so on the screen and waits"
                        + " for you. The words depend on the era of the motherboard.")
                .table("What the screen says", "Era", "What the screen says")
                .row(GuideCell.words("Vintage"), GuideCell.words("\"Non-system disk or disk error\", then \"Replace"
                        + " and press any key when ready\"."))
                .row(GuideCell.words("Legacy and Transition"), GuideCell.words("\"DISK BOOT FAILURE, INSERT SYSTEM DISK"
                        + " AND PRESS ENTER\"."))
                .row(GuideCell.words("Standard and Advanced"), GuideCell.words("\"No bootable device found\", then"
                        + " \"Press any key to enter Setup\"."))
                .paragraph("When a disk holds a system whose boot loader was deleted, that system's own complaint"
                        + " replaces the firmware's text. The screen then adds \"Put in an installation medium and"
                        + " install over it to repair.\"")
                .paragraph("The screen lists each disk and drive it knows. When there are more than it can show, the"
                        + " last line says how many more there are. An empty list says \"No disk and no drive is"
                        + " attached to this computer.\"")
                .paragraph("The hint reads \"Insert installation media and press Enter, or DEL for Setup\". Press any"
                        + " other key, and the screen says Entering Setup and opens the Setup"
                        + " ([](jsc:setup_utility)).")
                .steps("Put an install medium into a drive that is joined to the computer.",
                        "Press Enter on this screen to start from it, or press DEL to go to the Setup.",
                        "Follow the installer ([](jsc:installing)).")
                .table("Which drive reads which medium", "Medium", "Drive that reads it")
                .row(GuideCell.words("Floppy Disk"), GuideCell.words("Floppy Drive."))
                .row(GuideCell.words("CD-ROM"), GuideCell.words("CD Drive, DVD Drive or Blu-ray Drive."))
                .row(GuideCell.words("DVD-ROM"), GuideCell.words("DVD Drive or Blu-ray Drive."))
                .row(GuideCell.words("USB Flash Drive"), GuideCell.words("Dock Station, or the USB port on the front"
                        + " of a case from the Transition on."))
                .ifSomethingGoesWrong("The screen says that no bootable device was found.",
                        "No disk has a system yet, and no install medium is in a drive. Put one in"
                                + " ([](jsc:installing)).",
                        "A medium is in a drive, but the screen does not list it.",
                        "The medium is in the wrong kind of drive, or the drive is not joined to the computer. See the"
                                + " table above ([](jsc:drives)).",
                        "The screen says \"No disk and no drive is attached to this computer.\"",
                        "Join a drive, or a disk, to the computer ([](jsc:drives)).")
                .seeAlso("jsc:installing", "jsc:drives", "jsc:boot_menu")
                .register();
    }

    private static void systemsAndEras(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("systems_and_eras").titled("Systems and eras")
                .icon(ComputingModule.FLOPPY_DISK).in(chapter).register();
        guide.page("eras", section).titled("The eras").icon(ComputingModule.VINTAGE_PERSONAL_COMPUTER)
                .shows(ComputingModule.VINTAGE_PERSONAL_COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER,
                        ComputingModule.TRANSITION_PERSONAL_COMPUTER, ComputingModule.PERSONAL_COMPUTER,
                        ComputingModule.ADVANCED_PERSONAL_COMPUTER)
                .paragraph("Computers and parts of J's Computers belong to an era, a generation of hardware. The eras"
                        + " run from Vintage to Advanced, and each has its own word size, instruction set, firmware"
                        + " and media.")
                .paragraph("The word size is how much data a processor handles at once. Older computers had smaller"
                        + " words, so the room an item takes on a disk depends on the era ([](jsc:items_as_data)).")
                .paragraph("A disk's room is measured in megabytes (MB). The room per item is how many megabytes one"
                        + " item takes on a disk of that era.")
                .table("The eras", "Era", "Word size", "Instruction set", "Room per item")
                .row(GuideCell.words("Vintage"), GuideCell.words("16-bit"), GuideCell.code("IA-16"),
                        GuideCell.amount(1, "MB"))
                .row(GuideCell.words("Legacy"), GuideCell.words("32-bit"), GuideCell.code("x86"),
                        GuideCell.amount(16, "MB"))
                .row(GuideCell.words("Transition"), GuideCell.words("64-bit"), GuideCell.code("x86-64"),
                        GuideCell.amount(256, "MB"))
                .row(GuideCell.words("Standard"), GuideCell.words("64-bit"), GuideCell.code("x86-64"),
                        GuideCell.amount(256, "MB"))
                .row(GuideCell.words("Advanced"), GuideCell.words("64-bit"), GuideCell.code("x86-64"),
                        GuideCell.amount(256, "MB"))
                .table("The firmware and the media of each era", "Era", "Firmware", "System medium",
                        "Application medium")
                .row(GuideCell.words("Vintage"), GuideCell.words("Text BIOS"),
                        GuideCell.words("Floppy Disk, in a Floppy Drive"),
                        GuideCell.words("Floppy Disk, in a Floppy Drive"))
                .row(GuideCell.words("Legacy"), GuideCell.words("Blue BIOS"),
                        GuideCell.words("CD-ROM, in a CD Drive"), GuideCell.words("CD-ROM, in a CD Drive"))
                .row(GuideCell.words("Transition"), GuideCell.words("Blue BIOS"),
                        GuideCell.words("DVD-ROM, in a DVD Drive"), GuideCell.words("DVD-ROM, in a DVD Drive"))
                .row(GuideCell.words("Standard"), GuideCell.words("UEFI"),
                        GuideCell.words("USB Flash Drive, in a Dock Station or a case's USB port"),
                        GuideCell.words("DVD-ROM for programs, USB Flash Drive for background services"))
                .row(GuideCell.words("Advanced"), GuideCell.words("UEFI"),
                        GuideCell.words("USB Flash Drive, in a Dock Station or a case's USB port"),
                        GuideCell.words("Blu-ray for programs, USB Flash Drive for background services"))
                .paragraph("Parts do not mix freely across eras, so check the era of each part before you put it in."
                        + " A Personal Computer takes only a motherboard of its own era. A processor, a memory module"
                        + " and a sound card must also match the era of the board. A graphics card is checked only for"
                        + " the bus it plugs into.")
                .paragraph("A newer era is faster: its parts have more cores, higher clocks, more memory and faster"
                        + " disks. Its start-up also finishes sooner ([](jsc:turning_on)).")
                .paragraph("Every era has its own creative tab, from J's Computers - Vintage to J's Computers -"
                        + " Advanced.")
                .define("Instruction set (ISA)", "The language a processor understands. A program built for one runs"
                        + " on it and on every newer one, never on an older one: x86-64 runs x86 and IA-16 programs.")
                .ifSomethingGoesWrong("A part does not go into the slot of a board from another era.",
                        "The processor, the memory module or the sound card must have the era of its board"
                                + " ([](jsc:motherboards)).")
                .seeAlso("jsc:items_as_data", "jsc:motherboards")
                .register();
        guide.page("the_systems", section).titled("Operating systems").icon(ComputingModule.FLOPPY_DISK)
                .paragraph("The operating system is what a computer starts into, and what every program runs on. You"
                        + " choose a system when you install it ([](jsc:installing)).")
                .paragraph("Each system needs hardware of its era or newer, takes room on the disk it is installed on,"
                        + " and takes memory while it runs.")
                .table("The systems", "System", "Earliest era", "Room on the disk", "Memory while it runs")
                .row(GuideCell.words("MC-DOS"), GuideCell.words("Vintage"), GuideCell.amount(4, "MB"),
                        GuideCell.amount(1, "MB"))
                .row(GuideCell.words("MC-NET"), GuideCell.words("Vintage"), GuideCell.amount(8, "MB"),
                        GuideCell.amount(2, "MB"))
                .row(GuideCell.words("UNIX System V"), GuideCell.words("Vintage"), GuideCell.amount(10, "MB"),
                        GuideCell.amount(2, "MB"))
                .row(GuideCell.words("Frames 95"), GuideCell.words("Legacy"), GuideCell.amount(48, "MB"),
                        GuideCell.amount(16, "MB"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Legacy"), GuideCell.amount(1536, "MB"),
                        GuideCell.amount(64, "MB"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Transition"), GuideCell.amount(16384, "MB"),
                        GuideCell.amount(384, "MB"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Standard"), GuideCell.amount(16384, "MB"),
                        GuideCell.amount(512, "MB"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Advanced"), GuideCell.amount(20480, "MB"),
                        GuideCell.amount(768, "MB"))
                .row(GuideCell.words("Ubuntu"), GuideCell.words("Legacy"), GuideCell.amount(8192, "MB"),
                        GuideCell.amount(48, "MB"))
                .row(GuideCell.words("Debian"), GuideCell.words("Legacy"), GuideCell.amount(4096, "MB"),
                        GuideCell.amount(24, "MB"))
                .row(GuideCell.words("Fedora"), GuideCell.words("Legacy"), GuideCell.amount(8192, "MB"),
                        GuideCell.amount(48, "MB"))
                .row(GuideCell.words("Arch Linux"), GuideCell.words("Legacy"), GuideCell.amount(2048, "MB"),
                        GuideCell.amount(12, "MB"))
                .row(GuideCell.words("Gentoo"), GuideCell.words("Legacy"), GuideCell.amount(4096, "MB"),
                        GuideCell.amount(12, "MB"))
                .row(GuideCell.words("FreeBSD"), GuideCell.words("Legacy"), GuideCell.amount(2048, "MB"),
                        GuideCell.amount(16, "MB"))
                .paragraph("MC-DOS is a command prompt. UNIX System V is a shell, with no desktop of its own.")
                .paragraph("Each Frames edition runs on its own era and every newer one. Frames 10 needs Standard"
                        + " hardware, and Frames 11 needs Advanced.")
                .paragraph("Arch Linux and Gentoo are installed by hand, step by step. The other systems have a guided"
                        + " installer ([](jsc:installing)).")
                .table("The help book of each system", "System", "Help book")
                .row(GuideCell.words("MC-DOS and the Frames editions"), GuideCell.words("The Frames Resource Kit"))
                .row(GuideCell.words("Ubuntu, Debian and Fedora"), GuideCell.words("The Linux Desk Reference"))
                .row(GuideCell.words("Arch Linux and Gentoo"), GuideCell.words("Linux by Hand"))
                .row(GuideCell.words("FreeBSD and UNIX System V"), GuideCell.words("The Daemon Handbook"))
                .row(GuideCell.words("MC-NET"), GuideCell.words("This Guide to Operations"))
                .paragraph("Each book is in the library ([](jsc:library)). Help on a computer opens on the book of its"
                        + " system first ([](jsc:help_on_a_computer)).")
                .ifSomethingGoesWrong("The installer refuses the system.",
                        "The hardware is older than the system's first era, or the disk is too small for it"
                                + " ([](jsc:installing)).")
                .seeAlso("jsc:installing", "jsc:eras")
                .register();
        guide.page("installing", section).titled("Installing a system").icon(ComputingModule.USB_FLASH_DRIVE)
                .coversAll(ComputersGuide.itemsOf(FormattedMediaItem.class))
                .paragraph("A computer without a system is a box that shows its firmware. The system is what you"
                        + " actually use: its files and its programs. To install one, you start the computer from the"
                        + " system's install medium, and follow its installer.")
                .paragraph("Every era has its own install medium. It goes in a drive that reads it. The drive has to"
                        + " be joined to the computer, like any other device ([](jsc:drives)).")
                .table("The install media of each era")
                .property("Vintage", "Floppy Disk, in a Floppy Drive")
                .property("Legacy", "CD-ROM, in a CD Drive")
                .property("Transition", "DVD-ROM, in a DVD Drive")
                .property("Standard and Advanced", "USB Flash Drive, in a Dock Station or a case's USB port")
                .paragraph("Programs come on media of their own. The Standard uses DVD-ROM for programs, the Advanced"
                        + " uses Blu-ray for them, and both use the USB Flash Drive for background services.")
                .steps("Put the install medium in a drive that is joined to the computer.",
                        "Turn the computer on, and watch the monitor ([](jsc:turning_on)).",
                        "Choose the medium on the Boot page of the Setup ([](jsc:setup_utility)), or in the Boot Menu"
                                + " ([](jsc:boot_menu)), and start it.",
                        "Follow the installer's pages to the end.")
                .paragraph("Not every system holds your hand. Arch Linux and Gentoo boot like a live medium, and you"
                        + " put their system on the disk by hand. Gentoo also builds its programs from source.")
                .paragraph("A server setting can make both ask for every step. In the jscomputers-server config file,"
                        + " install_by_hand.arch_every_step and install_by_hand.gentoo_every_step are off by default."
                        + " With the Arch setting on, Arch asks for the whole guide, plus the hardware clock and a name"
                        + " for the machine. With the Gentoo setting on, Gentoo asks for the whole handbook.")
                .paragraph("A system needs hardware of its era or newer, so Frames 10 will not go on a Legacy computer,"
                        + " and the installer says so ([](jsc:the_systems)).")
                .ifSomethingGoesWrong("The disk would not take the system.",
                        "The disk has no room for the system. Use a bigger disk, or a disk of a newer era, which holds"
                                + " more ([](jsc:items_as_data)).",
                        "The installation could not be written to the disk (no space or no disk).",
                        "The disk is full, or it is not joined to the computer. Check the disk and its room.",
                        "The medium was taken out before the system was written.",
                        "Keep the medium in its drive until the installer has finished.",
                        "There is no installer on that medium.",
                        "The medium is not an install medium for a system. Use the install medium of the system you"
                                + " want.",
                        "No installable system in a linked drive.",
                        "Put the install medium in a drive that is joined to the computer ([](jsc:drives)).",
                        "The installer asks for its install disc in a linked drive.",
                        "Put that system's install medium in a drive joined to the computer ([](jsc:drives)).",
                        "The installer says that the system needs other hardware.",
                        "Install it on a computer that has the hardware the installer names ([](jsc:eras)).")
                .seeAlso("jsc:the_systems", "jsc:drives", "jsc:firmware")
                .register();
    }

    private static void howThingsAreCounted(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("how_things_are_counted").titled("How things are counted")
                .icon(HardwareItems.DISK_TRENCH_20M).in(chapter).register();
        guide.page("items_as_data", section).titled("Items as data").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("The network keeps your items on the disks of its computers, as data. Its disks count"
                        + " items, not raw space. A bucket of fluid takes as much room as one item.")
                .paragraph("How much room an item takes depends on the era of the disk, because older computers had"
                        + " smaller words: 1 MB in the Vintage, 16 MB in the Legacy, and 256 MB from the Transition on."
                        + " So a 20 MB Vintage drive holds 20 items, and a 1 terabyte (TB) disk holds 4,096 items from"
                        + " the Transition on.")
                .define("Item size", "The room an item takes on a disk: 1 MB in the Vintage, 16 MB in the Legacy,"
                        + " 256 MB"
                        + " from the Transition on.")
                .table("Disk sizes in items, from the Transition on", "Disk", "Items")
                .row(GuideCell.words("500 GB"), GuideCell.words("2,000 items"))
                .row(GuideCell.words("1 TB"), GuideCell.words("4,096 items"))
                .row(GuideCell.words("2 TB"), GuideCell.words("8,192 items"))
                .row(GuideCell.words("4 TB"), GuideCell.words("16,384 items"))
                .row(GuideCell.words("8 TB"), GuideCell.words("32,768 items"))
                .paragraph("Above 8 TB, only hard disks are made, up to 24 TB, which holds 98,304 items.")
                .paragraph("Memory works in items too, in its own way. Each memory module keeps a buffer of items for"
                        + " the Mainframe, one item for every 4 MB of the module's size ([](jsc:memory)). A buffer too"
                        + " small for the processor leaves the processor waiting for memory.")
                .paragraph("Disks differ in speed. A solid-state drive (SSD) is 4 times as fast as a hard disk, and an"
                        + " NVMe disk is 16 times as fast ([](jsc:disks)).")
                .seeAlso("jsc:disks", "jsc:units_and_ticks", "jsc:fluids")
                .register();
        guide.page("units_and_ticks", section).titled("Units and ticks")
                .paragraph("Several units appear in this manual and on the screens. This page explains each one, so the"
                        + " numbers mean something to you.")
                .table("The units of this book", "Unit", "What it measures")
                .row(GuideCell.code("item"), GuideCell.words("One unit of everything the network keeps. Disk room is"
                        + " counted in items."))
                .row(GuideCell.code("MB, GB, TB"), GuideCell.words("The real size of a disk or a memory module, in"
                        + " megabytes, gigabytes and terabytes."))
                .row(GuideCell.code("it/t"), GuideCell.words("Items a tick: a processor's tooltip shows it."))
                .row(GuideCell.code("W"), GuideCell.words("Watts: how much power a power supply gives, and how much"
                        + " a part draws."))
                .row(GuideCell.code("s"), GuideCell.words("Seconds. The boot manager counts down in them."))
                .define("Tick", "One of the game's beats. 20 ticks make a second, so a tick is one twentieth of a"
                        + " second.")
                .define("Items a tick (it/t)", "How many items a processor handles in one tick. A faster processor"
                        + " handles more items each tick.")
                .paragraph("The game counts time in ticks, too. The boot manager waits 5 seconds, which is 100 ticks"
                        + " ([](jsc:boot_menu)).")
                .paragraph("Memory is counted in items as well. Each memory module's buffer holds one item for"
                        + " every 4 MB"
                        + " of its size, and the disks count by the era of each disk ([](jsc:items_as_data)).")
                .seeAlso("jsc:items_as_data", "jsc:boot_menu")
                .register();
    }

    /*
     * One Peripheral Cable block. The wire sits in the cable's own lane, number 3, and the number says which faces it
     * crosses, with a second bit per face for a plug where it meets a device.
     */
    private static String cable(final int faces) {
        return "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0," + faces + ",0,0,0,0,0]}";
    }
}
