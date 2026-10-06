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
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
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
 * The entries of J's Computers' chapter, written as a guide reads: what a computer is here and how to build one first,
 * then what is inside it, the computers, their systems, the network, Operations, storage, autocrafting and the
 * devices. Each entry explains the idea before the part, in running text, and ends with what can go wrong; its
 * sentences lead to the other entries in their own words. The families of items an entry is the page of are read from
 * the mod's registrations, so a part added to a family is the page of its family's entry with nothing more to write.
 */
final class ComputersEntries {

    /** How tall a firmware's picture is drawn: a monitor's glass, as wide as a binder's column. */
    private static final int FIRMWARE_PICTURE = 97;
    private static final String FIRMWARE = "jsc:firmware";

    private ComputersEntries() {
    }

    static void declare(final ModGuide guide) {
        gettingStarted(guide);
        hardware(guide);
        computers(guide);
        systems(guide);
        network(guide);
        operations(guide);
        storage(guide);
        autocrafting(guide);
        peripherals(guide);
    }

    private static void gettingStarted(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("getting_started").titled("Getting started")
                .icon(ComputingModule.PERSONAL_COMPUTER).register();
        guide.page("welcome", section).titled("Welcome to J's Computers").icon(ComputingModule.PERSONAL_COMPUTER)
                .covers(ComputersGuide.MANUAL)
                .shows(ComputingModule.PERSONAL_COMPUTER, ComputingModule.MONITOR, ComputingModule.MAINFRAME,
                        ComputingModule.SERVER_RACK, ComputingModule.CRAFTING_COMPUTER)
                .paragraph("As you might have guessed, the most essential thing in J's Computers, its whole"
                        + " identity, is computers. But what is a computer here? you might ask.")
                .paragraph("If you have played other computer mods, the computers in this one are not that far from"
                        + " what you know: you place one, you turn it on, you use it. And yet they are different, and"
                        + " you will notice how much as you go through this manual. A computer here is built the way"
                        + " a real one is, part by part. It starts its own firmware, it boots an operating system"
                        + " from a disk, and it runs the programs you install on it. A few of them joined together"
                        + " around one big computer make a network, and that network is what stores your items,"
                        + " moves them and crafts for you.")
                .paragraph("To get your first computer you need some hardware. First a case: it holds your parts in"
                        + " place, and it is the block you put in the world (the Personal Computer). Then, to boot"
                        + " one that works at all, you need these parts: a motherboard, a processor (CPU), system"
                        + " memory (RAM), a graphics card (GPU), a power supply (PSU) and a disk for storage."
                        + " [Building your first computer](jsc:first_computer) shows you how to put them together,"
                        + " and what each one does.")
                .paragraph("When everything is in and you turn it on, the first thing you meet is the computer's"
                        + " firmware: on older machines that is the BIOS, on modern ones the UEFI ([](jsc:firmware))."
                        + " From there you can set the more advanced things of the computer, such as the boot order:"
                        + " if you have more than one disk with an operating system on it, the boot order says which"
                        + " one starts first. You might have noticed already that this is not quite what you have"
                        + " seen in other computer mods.")
                .paragraph("Everything in this manual is in the creative tabs for now, one tab for each era of"
                        + " hardware ([The eras](jsc:eras)) and one, J's Computers, for what every era shares. The"
                        + " computers have no recipes yet, on purpose: they will be made from what the industrial"
                        + " chains produce.")
                .subheading("How to read this manual")
                .paragraph("Every entry starts on a page of its own, and the items it talks about stand on the plate"
                        + " under its title: point at one to see what it is, click it to go to its own page. A"
                        + " number in brackets is a link to another entry. And when you are holding an item and want"
                        + " to know what it is, hold the manual key over it (M, unless you changed it): the manual"
                        + " opens right at its page.")
                .register();
        guide.page("first_computer", section).titled("Building your first computer")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .shows(ComputingModule.PERSONAL_COMPUTER, HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150,
                        HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K, ComputingModule.RAM_DDR3_8192,
                        HardwareItems.GPU_VERTEX_GTX_970, ComputingModule.PSU_650G,
                        () -> ComputingModule.disk(StorageTier.SSD, DiskSize.TB_1), ComputingModule.MONITOR,
                        ComputingModule.DOCK_STATION)
                .paragraph("So, you want to build a computer. Good: that is what this mod is about, and it is easier"
                        + " than it sounds.")
                .paragraph("Start with the case. Take a Personal Computer from the creative tab J's Computers -"
                        + " Standard and place it. When you use it, its window opens, and that window is the inside"
                        + " of the case: a slot for each part, the same way a real case has a place for each one."
                        + " Nothing works yet, because a case on its own is just a box.")
                .subheading("The parts, one by one")
                .paragraph("Put the motherboard in first. It is the board every other part sits on, and it decides"
                        + " what the computer can take, so a case takes only a board of its own era"
                        + " ([](jsc:motherboards)).")
                .paragraph("Then the processor, the part that does the computing. It has to fit the board's socket:"
                        + " hover the board and the processor, and both tooltips name their socket"
                        + " ([](jsc:processors)).")
                .paragraph("Now the memory, where the system and the programs run while the computer is on. The"
                        + " board takes some kinds of memory and not others, and its tooltip says which"
                        + " ([](jsc:memory)).")
                .paragraph("A graphics card, so the computer can show a picture at all. Without one the monitor stays"
                        + " dark, unless the processor has graphics built in ([](jsc:graphics_cards)).")
                .paragraph("A disk, where the system will be installed and your files will live ([](jsc:disks))."
                        + " And a power supply, which powers everything else: if the parts draw more than it gives,"
                        + " the computer refuses to start ([](jsc:power_supplies)).")
                .paragraph("If something does not fit, the window does not just ignore it: it tells you why, right"
                        + " there.")
                .subheading("The monitor")
                .paragraph("A computer with no screen is not much use, so place a Monitor right against the case, or"
                        + " join them with Peripheral Cable if you want the monitor somewhere else"
                        + " ([](jsc:peripheral_cables)). Do the same with a Dock Station: that is where the install"
                        + " stick goes.")
                .paragraph("Here is the thing that surprises most players: using the computer block always opens the"
                        + " case. Everything you do with the computer itself, its system and its programs, you do at"
                        + " the monitor.")
                .subheading("Turning it on")
                .paragraph("Turn the computer on from its window and use the monitor. There is no system on the disk"
                        + " yet, so you land in the firmware ([](jsc:firmware)). Put the Frames 10 install stick in"
                        + " the Dock Station, choose it, and follow the installer ([](jsc:installing)). When it is"
                        + " done, the computer starts into its desktop: open This PC, Files, the Command Prompt, the"
                        + " Settings. It is a computer.")
                .ifSomethingGoesWrong("Using the computer opens the case, not the system.",
                        "That is on purpose: the system is at the monitor. Use the monitor.",
                        "The monitor says no computer is in range.",
                        "It is not joined to the computer. Put it right against it, or lay Peripheral Cable all the"
                                + " way.",
                        "The monitor says the computer has no video output.",
                        "Put a graphics card in, or use a processor with graphics built in.",
                        "The window refuses the build.",
                        "Read what it says: a processor that does not fit the socket, memory the board does not"
                                + " take, a part from another era, or more power than the power supply gives.")
                .register();
        guide.page("firmware", section).titled("Firmware: the BIOS and the UEFI")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .shows(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE, HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775,
                        HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .paragraph("Every computer has a small program that lives on the motherboard itself, not on any"
                        + " disk: the firmware. It is the first thing that runs when you turn the computer on. It"
                        + " checks the hardware (that is the list of processor, memory and video you see go by),"
                        + " finds the disks and the drives, and hands the computer over to an operating system. If"
                        + " there is no system to hand it to, it stays on the screen and waits for you.")
                .paragraph("What it looks like depends on the era of the motherboard, the same way it did on real"
                        + " machines:")
                .drawing(FIRMWARE, FIRMWARE_PICTURE, "{\"look\":\"cli_bios\"}",
                        "Vintage boards: the text BIOS, plain text lit on the tube.")
                .drawing(FIRMWARE, FIRMWARE_PICTURE, "{\"look\":\"blue_bios\"}",
                        "Legacy and Transition boards: the blue BIOS, with its keys along the bottom.")
                .drawing(FIRMWARE, FIRMWARE_PICTURE, "{\"look\":\"uefi\"}",
                        "Standard and Advanced boards: the UEFI, a graphical setup.")
                .paragraph("While the computer starts, the firmware tells you two keys. DEL enters the Setup, and F12"
                        + " opens the Boot Menu. The Setup is where the boot order lives: the list of disks in the"
                        + " order the firmware tries them, and the first one with a system on it is the one that"
                        + " starts. The Boot Menu is the same choice for this start only, which is handy when you"
                        + " want to boot an install disc once without changing anything.")
                .paragraph("So, when would you change the boot order? Say you installed Frames XP on one disk and"
                        + " Debian on another. The computer starts whichever disk comes first; put the other one on"
                        + " top, and that is the one that boots. With two systems installed the computer also stops"
                        + " at a boot manager every time it starts, so you can pick"
                        + " ([Two systems on one computer](jsc:two_systems)).")
                .paragraph("The Setup also has a page for the hardware, which lists what the firmware found in the"
                        + " machine, and, on a server with a RAID Controller, one for its drives' array"
                        + " ([](jsc:rack_equipment)).")
                .define("Boot order", "The order the firmware tries the disks in when the computer starts. The"
                        + " first disk with a system on it starts.")
                .ifSomethingGoesWrong("It says there is no bootable device.",
                        "No disk has a system yet and no install medium is in a drive. Put one in and boot from it"
                                + " ([](jsc:installing)).",
                        "The drive is not in the list.",
                        "It is not joined to the computer, or the medium is in the wrong kind of drive: a floppy"
                                + " goes in a Floppy Drive, a CD in a CD Drive, and so on ([](jsc:drives)).")
                .register();
        guide.page("installing", section).titled("Installing a system").icon(ComputingModule.USB_FLASH_DRIVE)
                .coversAll(itemsOf(FormattedMediaItem.class))
                .paragraph("A computer without an operating system is, well, a box that shows its firmware. The"
                        + " system is what you actually use: the desktop, the files, the programs. Installing one is"
                        + " the same as on a real machine: you boot the computer from the system's install medium"
                        + " and follow its installer.")
                .paragraph("Every era has its own kind of medium, and every medium goes in its own kind of drive,"
                        + " joined to the computer like any other device ([](jsc:drives)):")
                .table("The media of each era")
                .property("Vintage", "Floppy disks, in a Floppy Drive")
                .property("Legacy", "CDs, in a CD Drive")
                .property("Transition", "DVDs, in a DVD Drive")
                .property("Standard and Advanced", "USB sticks, in a Dock Station")
                .paragraph("The install media are in each era's creative tab, among the programs, with blank media"
                        + " beside them.")
                .steps("Put the medium in a drive joined to the computer.",
                        "Use the monitor: with no system on the disk, the firmware is waiting ([](jsc:firmware)).",
                        "Choose the drive and boot it. The installer starts, in the look of its own system.",
                        "Follow it. Most installers ask which disk to use, and to confirm.")
                .paragraph("Not every system holds your hand, though. Arch Linux asks you to type the real steps of"
                        + " its guide, and Gentoo compiles itself from source first, just as they do outside the"
                        + " game. If you like that kind of thing, a server setting makes both ask for every single"
                        + " step.")
                .paragraph("A system also needs hardware of its own era or newer: Frames 10 will not go on a Legacy"
                        + " computer, and the installer tells you so ([](jsc:the_systems)).")
                .ifSomethingGoesWrong("The installer refuses the system.",
                        "The hardware is older than the system's first era, or the disk is too small for it.",
                        "The drive does not show in the firmware.",
                        "It is not joined to the computer, or it is not the drive for that medium.")
                .register();
        guide.page("eras", section).titled("The eras").icon(ComputingModule.VINTAGE_PERSONAL_COMPUTER)
                .shows(ComputingModule.VINTAGE_PERSONAL_COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER,
                        ComputingModule.TRANSITION_PERSONAL_COMPUTER, ComputingModule.PERSONAL_COMPUTER,
                        ComputingModule.ADVANCED_PERSONAL_COMPUTER)
                .paragraph("Computers did not always look like they do today, and in J's Computers they do not"
                        + " either. Every computer and every part belongs to an era, a generation of computing, from"
                        + " the beige boxes of the early 1990s to the machines of today.")
                .table("The eras")
                .property("Vintage", "The early 1990s: 16-bit, IA-16, floppy disks")
                .property("Legacy", "Around 2000: 32-bit, x86, CDs")
                .property("Transition", "The late 2000s: 64-bit, x86-64, DVDs")
                .property("Standard", "The 2010s: 64-bit, x86-64, USB sticks")
                .property("Advanced", "Today: 64-bit, x86-64, USB sticks and Blu-ray")
                .paragraph("Why does it matter? First, because parts do not mix across eras: an era's computer"
                        + " takes only a motherboard of its own era, and a board seats only parts of its own era. A"
                        + " Vintage case with a modern graphics card in it is not a thing here, just as it was not"
                        + " in real life.")
                .paragraph("Second, because a newer era is faster at everything: more cores at higher clocks, more"
                        + " memory, faster disks. And third, because of the word size. The network keeps your items"
                        + " as data on its disks, and how much room an item takes depends on the era of the disk: on"
                        + " a Vintage disk an item takes 1 MB, so a 20 MB drive holds 20 items, and from the"
                        + " Transition on an item takes 256 MB, so a 1 TB disk holds 4,096 ([](jsc:items_as_data)).")
                .paragraph("Each era has its own creative tab, J's Computers - Vintage to J's Computers - Advanced,"
                        + " with the same shelves in each: the computers, the Mainframes, the racks, the"
                        + " peripherals, the network, the components and the programs.")
                .define("Instruction set (ISA)", "The language a processor understands. A program built for one"
                        + " runs on it and on every newer one, never on an older one: x86-64 runs x86 and IA-16"
                        + " programs.")
                .register();
    }

    private static void hardware(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("hardware").titled("Inside a computer")
                .icon(HardwareItems.GPU_VERTEX_8800_GT).register();
        guide.page("motherboards", section).titled("Motherboards")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150).coversAll(itemsOf(MotherboardItem.class))
                .paragraph("If the case is the body of a computer, the motherboard is its skeleton: the board every"
                        + " other part sits on, and the one that decides what the computer can take.")
                .paragraph("Hover a board and its tooltip tells you all of it: its form factor (which cases it"
                        + " fits), its socket (which processors), how many memory modules and of which kinds, its"
                        + " expansion slots and their generation (ISA, AGP, PCI-e 3.0 and so on), whether it has"
                        + " sound of its own, and its era.")
                .define("Form factor", "A board's size, which says which cases it fits: Baby-AT and AT in the"
                        + " Vintage, ATX from the Legacy, EATX from the Transition, and MTX for the Mainframes.")
                .define("Socket", "The place the processor sits in. A processor fits a board only when both name the"
                        + " same socket.")
                .paragraph("So when you are picking parts, start from the board: it tells you what to look for in"
                        + " everything else. A board seats parts of its own era only, and a case takes only a board"
                        + " of its own era.")
                .paragraph("One more thing: a part sitting in the window that does nothing usually means the board"
                        + " does not offer that slot. A cheaper board may have fewer memory or expansion slots than"
                        + " the case has room for.")
                .ifSomethingGoesWrong("It does not go into the case.",
                        "The case is of another era, or of a size the board does not fit.",
                        "A part sits in the window but does nothing.",
                        "The board does not offer that slot. Use a board with more.")
                .register();
        guide.page("processors", section).titled("Processors (CPU)")
                .icon(HardwareItems.CPU_INTEGRA_PENTIX_133).coversAll(itemsOf(CpuItem.class))
                .paragraph("The processor, or CPU, is the part that does the actual computing. Everything a computer"
                        + " runs, from the system to a game of Minesweeper, is the processor following instructions,"
                        + " one after the other, very fast.")
                .paragraph("Two numbers tell you how much a processor can do: how many cores it has (how many things"
                        + " it works on at the same time) and its clock, in GHz (how fast each core goes). A newer"
                        + " design also does more with each tick of the clock than an older one at the same speed."
                        + " Hover a processor and its tooltip sums all of that up in one number, its capacity, in"
                        + " items a tick (it/t).")
                .paragraph("Why items a tick? Because in a Mainframe that is exactly what it means: how many items the"
                        + " whole network handles at once. A personal computer with a slow processor is just a slow"
                        + " computer; a Mainframe with a slow processor is a slow network ([](jsc:mainframes)).")
                .paragraph("A processor also has to fit the motherboard. The socket is the shape of the place it sits"
                        + " in, and the board and the processor both name theirs on their tooltips: they have to"
                        + " match. Then there is the instruction set (IA-16, x86, x86-64), the language the"
                        + " processor understands. A program built for one instruction set runs on it and on every"
                        + " newer one, never on an older one, which is why a modern program will not run on a"
                        + " Vintage machine.")
                .paragraph("There are more than 170 processors in the mod, from the Integra and Velocion makers, so"
                        + " the plate on top of this page turns over through them as you read. A processor with"
                        + " graphics built in says so on its tooltip: it lights a monitor without a graphics card.")
                .ifSomethingGoesWrong("It will not go in.",
                        "Its socket is not the board's, or it is of another era.",
                        "The build is refused for mixed processors.",
                        "Processors of two instruction sets never share a board.")
                .register();
        guide.page("memory", section).titled("Memory (RAM)").icon(HardwareItems.RAM_SIMM_4)
                .coversAll(itemsOf(RamItem.class))
                .paragraph("Memory, or RAM, is where a computer keeps what it is working on right now: the system,"
                        + " the programs that are open, the file you are editing. It is fast, and it forgets"
                        + " everything when the computer turns off, which is why the disk exists too.")
                .paragraph("In J's Computers every system and every program needs some memory to run, so a computer"
                        + " with little of it runs fewer things at once. The Task Manager of each system shows you"
                        + " what is using it.")
                .paragraph("Memory comes in generations, from the SIMM modules of the Vintage to DDR5, and a board"
                        + " takes only some of them: its tooltip says which. Modules of a generation it does not take"
                        + " are simply refused.")
                .paragraph("In a Mainframe, memory has one more job: it is the buffer items wait in on their way in or"
                        + " out of the network. Each module's tooltip says how many items it holds that way"
                        + " ([](jsc:mainframes)).")
                .ifSomethingGoesWrong("The module is refused.",
                        "It is of a generation the board does not take.",
                        "A program does not start.",
                        "The computer has no memory left for it. Close something, or add a module.")
                .register();
        guide.page("graphics_cards", section).titled("Graphics cards (GPU)")
                .icon(HardwareItems.GPU_VERTEX_8800_GT).coversAll(itemsOf(GpuItem.class))
                .paragraph("The graphics card is the part that draws: it lights the monitors and draws the windows on"
                        + " them. A computer with no graphics card has nothing to show, unless its processor has"
                        + " graphics built in.")
                .paragraph("A card has a memory of its own, the video memory, and everything that shows a picture"
                        + " uses some of it: each monitor, more for a bigger one, and each window that draws a"
                        + " picture of its own. Run out of it and the monitor tells you so.")
                .define("Video memory (VRAM)", "The graphics card's own memory. Every monitor and every window that"
                        + " draws a picture uses some; a bigger monitor more.")
                .paragraph("In a Mainframe the cards do something else as well: each one gives the Mainframe one more"
                        + " queue to work Operations in, at the same time as the others. A Mainframe with three cards"
                        + " works four queues, its processors' own and one for each card"
                        + " ([](jsc:states_and_queues)).")
                .paragraph("Cards go in the board's expansion slots, and a card in a slot older than itself runs at"
                        + " that slot's speed; its tooltip says so. There are 76 of them, from the VGA-256 to the"
                        + " Envya Vertex RTX 5090.")
                .ifSomethingGoesWrong("The monitor says there is no video output.",
                        "Put a graphics card in, or use a processor with graphics built in.",
                        "The monitor says there is not enough video memory.",
                        "Use a card with more, or fewer or smaller monitors.")
                .register();
        guide.page("power_supplies", section).titled("Power supplies (PSU)").icon(HardwareItems.PSU_200)
                .coversAll(itemsOf(PsuItem.class))
                .paragraph("Every part of a computer draws power, and the power supply is what gives it. Each part's"
                        + " tooltip says how many watts it draws; add them up, and the power supply has to give at"
                        + " least that much.")
                .paragraph("There are supplies from 200 W to 3,000 W, each with its efficiency. If the parts draw"
                        + " more than the supply gives, the computer simply refuses to start: no smoke, no fire, just"
                        + " a refusal. Some supplies scale to what the computer draws and never refuse.")
                .paragraph("So a big graphics card in a computer with a small supply is a classic mistake, here as in"
                        + " real life.")
                .ifSomethingGoesWrong("The computer refuses to start.",
                        "Its parts draw more than its power supply gives. Use a bigger one.")
                .register();
        guide.page("disks", section).titled("Disks").icon(HardwareItems.DISK_TRENCH_20M)
                .coversAll(itemsOf(DiskItem.class))
                .paragraph("A disk is where a computer keeps everything that has to survive turning it off: its"
                        + " system, its programs, your files. And in a network, the disks of the servers are where"
                        + " your items are kept ([](jsc:items_as_data)).")
                .paragraph("There are three kinds, and they differ in speed. A hard disk (HDD) is the slowest and the"
                        + " biggest; an SSD moves data four times as fast; an NVMe drive sixteen times. Before a"
                        + " transfer starts, a hard disk waits 10 ticks, an SSD 3 and NVMe 1.")
                .paragraph("A disk's capacity is counted in items of its era, because that is what the network fills"
                        + " it with: a 20 MB Vintage drive holds 20 items, a 1 TB disk 4,096. Once a system is"
                        + " installed on a disk, its tooltip says which system, its desktop and how many programs.")
                .paragraph("Disks go in a computer's disk slots, or in a server's drive bays at the front of a rack"
                        + " ([](jsc:server_racks)).")
                .ifSomethingGoesWrong("The installer says the disk is too small.",
                        "The system takes more room than the disk has. Use a bigger disk.",
                        "Storage is full.",
                        "Add disks to the servers, or servers to the racks. A newer era's disks hold more.")
                .register();
        guide.page("sound_cards", section).titled("Sound cards").icon(HardwareItems.SOUND_CARD_TONE_BLASTER)
                .coversAll(itemsOf(SoundCardItem.class))
                .paragraph("Yes, the computers make sound: music, the system's sounds, the games. In the older eras,"
                        + " though, a board has no sound of its own, so it needs a sound card: the Artisan Tone"
                        + " Blaster family, from the Vintage to the Transition.")
                .paragraph("A computer takes one sound card, of its board's era. From the Transition on, boards have"
                        + " sound built in, so the card is no longer needed.")
                .paragraph("The sound comes out of the monitor, or out of speakers joined to the computer's sound"
                        + " output ([](jsc:speakers)).")
                .ifSomethingGoesWrong("The build is refused.",
                        "A computer takes one sound card, of its board's era.")
                .register();
        guide.page("expansion_cards", section).titled("Expansion cards").icon(ComputingModule.FURNACE_CARD)
                .coversAll(itemsOf(WorkshopCardItem.class, ClusterInterfaceCardItem.class, NetworkCardItem.class,
                        PhiCoprocessorItem.class))
                .paragraph("Besides graphics and sound, a board's expansion slots take cards that give a computer a"
                        + " job it could not do before.")
                .subheading("Workshop cards")
                .paragraph("The Crafting Table, Furnace, Enchanting and Anvil cards let a Personal Computer's Workshop"
                        + " program craft, smelt, enchant and repair, and let the network change stored items the"
                        + " same way, with UPDATE ([](jsc:operations_catalogue)). Each card does its own thing:"
                        + " smelting needs the Furnace Card, enchanting the Enchanting Card.")
                .subheading("Cluster Interface Cards")
                .paragraph("The Serial Console Card, the Management NIC, the Fabric Host Adapter and the Fabric DPU"
                        + " let a Cluster Management Computer reach the machines in your racks; each newer one"
                        + " reaches more, and installs on several at once ([](jsc:cluster_management_computers)). In"
                        + " any other computer they do nothing.")
                .subheading("Optical Network Card")
                .paragraph("A network card with a fibre port, so a machine can take the fibre of the network's"
                        + " backbone ([](jsc:data_cables)).")
                .subheading("Integra Phi coprocessors")
                .paragraph("Four coprocessor cards that add computing power to the nodes of a supercomputer. They are"
                        + " made for supercomputer nodes and do not go in a server ([](jsc:server_racks)).")
                .ifSomethingGoesWrong("The Workshop cannot do what you ask.",
                        "It needs the card for it: smelting the Furnace Card, enchanting the Enchanting Card.",
                        "A Cluster Interface Card does nothing.",
                        "It only works in a Cluster Management Computer.")
                .register();
    }

    private static void computers(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("computers").titled("The computers")
                .icon(ComputingModule.MAINFRAME).register();
        guide.page("personal_computers", section).titled("Personal Computers")
                .icon(ComputingModule.PERSONAL_COMPUTER).coversAll(blocksOf(PersonalComputerBlock.class))
                .paragraph("The Personal Computer is your own computer, the one you sit at: you install a system on"
                        + " it, run programs, write your own, and from it you reach the network to store and take"
                        + " items, watch it work and ask it to craft.")
                .paragraph("Inside, it takes one motherboard, one processor, up to four memory modules, four graphics"
                        + " cards, two disks and a power supply, though the board may offer fewer. It also has 18"
                        + " storage slots of its own, a local storage that works even when the computer is on no"
                        + " network.")
                .table("What a Personal Computer holds")
                .fixed("Motherboard", "1")
                .fixed("Processors", "1")
                .fixed("Memory", "4")
                .fixed("Graphics cards", "4")
                .fixed("Disks", "2")
                .fixed("Power supply", "1")
                .fixed("Storage slots", "18")
                .paragraph("Every era has its own Personal Computer, and from the Standard on it comes in three cases"
                        + " that differ only in look: the plain one, High Performance and Aesthetic. Pick the one you"
                        + " like; inside they are the same.")
                .paragraph("If you have never built one, [Building your first computer](jsc:first_computer) walks you"
                        + " through it.")
                .ifSomethingGoesWrong("The window refuses the build.",
                        "It says why: no processor or memory, a processor that does not fit the socket, memory the"
                                + " board does not take, or more power than the power supply gives.")
                .register();
        guide.page("mainframes", section).titled("Mainframes").icon(ComputingModule.MAINFRAME)
                .coversAll(blocksOf(MainframeBlock.class))
                .paragraph("Every network has exactly one Mainframe, and it is the network's brain. It keeps the index"
                        + " of everything the network stores, takes every request the other computers send, decides"
                        + " which of its queues works each one, and moves the items. Two Mainframes on one network"
                        + " are a conflict, so give each network its own.")
                .paragraph("It is also big: three blocks wide, two tall and two deep. Place its parts in that shape"
                        + " and it forms. It takes an MTX motherboard of its era, a power supply, up to four"
                        + " processors, eight memory modules, six graphics cards and four disks, and it has 27"
                        + " storage slots.")
                .table("What a Mainframe holds")
                .fixed("Motherboard", "1 MTX")
                .fixed("Processors", "4")
                .fixed("Memory", "8")
                .fixed("Graphics cards", "6")
                .fixed("Disks", "4")
                .fixed("Storage slots", "27")
                .paragraph("Why does its hardware matter so much? Because everything the network does goes through"
                        + " it. Its processors decide how many items it handles at once, its capacity; its memory is"
                        + " the buffer items wait in on their way in or out; and each graphics card gives it one more"
                        + " queue of work done at the same time as the others ([](jsc:states_and_queues)). So when"
                        + " the network feels slow, look at the Mainframe first.")
                .define("Capacity", "How many items the network handles at once, in items a tick. The Mainframe's"
                        + " processors set it.")
                .paragraph("Only the Mainframe runs the Network Manager, the network's control room: every machine on"
                        + " it, the storage, and the Operations being worked.")
                .ifSomethingGoesWrong("The network is slow.",
                        "Faster processors raise the Mainframe's capacity; more graphics cards give it more queues.",
                        "Two Mainframes, and nothing works.",
                        "A network has one Mainframe. Split the cables.")
                .register();
        guide.page("server_racks", section).titled("Server racks and servers").icon(ComputingModule.SERVER_RACK)
                .coversAll(blocksOf(ServerRackBlock.class)).coversAll(itemsOf(ServerItem.class, ServerCaseItem.class))
                .paragraph("Your items have to live somewhere, and in J's Computers they live on the disks of"
                        + " servers, and the servers live in racks.")
                .paragraph("A Server Rack is a cabinet two blocks wide, three tall and two deep, with eight rack units"
                        + " of room. A machine's height is counted in those units: most servers take one, the bigger"
                        + " ones two. The Supercomputer Rack is the same cabinet for supercomputer nodes only, in the"
                        + " Standard and the Advanced.")
                .define("Rack unit (U)", "The height a machine takes in a rack. A rack has eight.")
                .paragraph("A rack seats servers of its own era or older. Each server has drive bays, which show at"
                        + " the front of the rack, room for a gadget or two, and room for processors and cards:")
                .table("Servers")
                .property("Vintage Server", "1 U, 1 drive")
                .property("Legacy Server", "1 U, 2 drives")
                .property("Transition Server", "1 U, 4 drives")
                .property("Server", "1 U, 3 drives")
                .property("Advanced Server", "1 U, 4 drives")
                .property("Storage Server", "2 U, 8 drives")
                .property("Compute Server", "2 U, 1 drive, 4 processors")
                .paragraph("The disks in the servers' bays are what the network fills with your items. Join the rack"
                        + " to the network's backbone cable and it is part of the storage ([](jsc:data_cables)). A"
                        + " supercomputer is every Supercomputer Rack tied together by the high compute cable,"
                        + " meeting the network at one HBW Interface ([](jsc:hbw_interface)).")
                .ifSomethingGoesWrong("A server does not go in.",
                        "It is from a newer era than the rack, or the rack has no rack units left for it.",
                        "Storage is full.",
                        "Add disks to the servers, or servers to the racks.")
                .register();
        guide.page("rack_equipment", section).titled("Rack equipment").icon(ComputingModule.KVM_SWITCH)
                .coversAll(itemsOf(RackGadgetItem.class, RackUnitItem.class))
                .paragraph("Not everything in a rack computes. Some of it serves the rack: the KVM Switch, the Rack"
                        + " UPS and the Cooling Unit take rack units like a server, and a server takes small gadgets"
                        + " in slots of its own.")
                .paragraph("The KVM Switch is the handy one: it lets a single monitor reach several machines in the"
                        + " rack, so you do not need a screen for each.")
                .paragraph("Of the gadgets, the RAID Controller joins a server's drives into one array, which you set"
                        + " in the server's firmware, on the Setup's storage page ([](jsc:firmware)). The Cache Card"
                        + " cuts how long a read waits.")
                .define("RAID", "Drives joined as one. RAID 0, with two drives or more, is a quarter faster but"
                        + " keeps nothing if one fails; RAID 1 keeps every drive a copy; RAID 5, with three or more,"
                        + " survives losing one.")
                .ifSomethingGoesWrong("The RAID Controller does nothing.",
                        "The server has fewer drives than its RAID level needs.")
                .register();
        guide.page("cluster_management_computers", section).titled("Cluster Management Computers")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .coversAll(blocksOf(ClusterManagementComputerBlock.class))
                .paragraph("When you have one rack, you can walk up to each server. When you have ten, you will not"
                        + " want to. The Cluster Management Computer installs systems on the machines in your racks"
                        + " and watches them, all from one desk.")
                .paragraph("Build it as you would a Personal Computer, and give it a Cluster Interface Card: without"
                        + " one it is an ordinary computer ([](jsc:expansion_cards)). Then open the Cluster Manager"
                        + " on it. Each newer card reaches more machines, and installs on several at once.")
                .paragraph("From the Standard on, the Cluster Manager also runs every supercomputer and every"
                        + " datacenter section as one machine ([](jsc:server_router)).")
                .ifSomethingGoesWrong("The Cluster Manager reaches no machine.",
                        "The computer has no Cluster Interface Card, or the card is too old to reach them.")
                .register();
    }

    private static void systems(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("systems").titled("Systems and programs")
                .icon(ComputingModule.FLOPPY_DISK).register();
        guide.page("the_systems", section).titled("Operating systems").icon(ComputingModule.FLOPPY_DISK)
                .paragraph("The operating system is what a computer starts into and what every program runs on. And"
                        + " here they are real systems, or close to it: every one is a parody of a real system, made"
                        + " by a software house of the mod's own world (Midsoft makes Frames, for example), and it"
                        + " behaves like the one it stands for.")
                .paragraph("Each system needs hardware of its era or newer, takes room on the disk it is installed"
                        + " on, and memory while it runs.")
                .table("The systems")
                .property("MC-DOS", "Vintage: a command prompt")
                .property("MC-NET", "Vintage: the whole screen is the network's terminal")
                .property("UNIX System V", "Vintage: a shell, and the CDE desktop")
                .property("Frames 95, Frames XP", "Legacy: a desktop")
                .property("FreeBSD, Debian, Ubuntu, Fedora", "Legacy: a shell until a desktop is added")
                .property("Arch Linux, Gentoo", "Legacy: installed by hand, as they are")
                .property("Frames 7", "Transition: a desktop")
                .property("Frames 10, Frames 11", "Standard and Advanced: a desktop")
                .paragraph("Some of them, Arch and Gentoo above all, want you to install them by hand, step by step,"
                        + " as they do in real life ([](jsc:installing)). And a few you will want to try just for the"
                        + " nostalgia.")
                .ifSomethingGoesWrong("The installer refuses the system.",
                        "The hardware is older than the system's first era, or the disk is too small for it.")
                .register();
        guide.page("desktops", section).titled("Desktops").icon(ComputingModule.MONITOR)
                .paragraph("A desktop is what you see when a system starts with a graphical face: the windows, the"
                        + " taskbar or panel, and the start menu. Each Frames comes with its own. On Linux, FreeBSD"
                        + " and UNIX, a desktop is a package you add: KDE Plasma, GNOME and CDE from the Legacy, and"
                        + " Cinnamon from the Standard. CDE runs on UNIX, FreeBSD and Linux; the others on Linux and"
                        + " FreeBSD.")
                .paragraph("Each desktop looks like its era: KDE in the Transition is the KDE of that time, not of"
                        + " today.")
                .paragraph("Whatever the desktop, you find the same programs under the names its system gives them:"
                        + " Network, This PC, Settings, Files, an editor, the Command Prompt, the System Monitor, a"
                        + " Calculator, the Network Manager and Help. Frames adds the Device Manager, which shows"
                        + " what is on every port of the computer; CDE adds Workstation Info.")
                .ifSomethingGoesWrong("The system starts at a shell, with no desktop.",
                        "On Linux, FreeBSD and UNIX the desktop is a package of its own: install one.")
                .register();
        guide.page("programs", section).titled("Programs").icon(ComputingModule.CD_ROM)
                .paragraph("Every system comes with its programs, and others you install. There are two ways to"
                        + " install one.")
                .paragraph("From media, the way a system is installed: put the program's disc in a joined drive and"
                        + " run its setup from Files. Or from the Mirror, with no media at all: the Mirror is a"
                        + " program on a Standard Mainframe that keeps the network's packages, and with it every"
                        + " computer of the network installs programs by name with its package manager (apt, dnf,"
                        + " pacman, emerge, pkg, or Frames' pckmgr). Players can publish their own packages there,"
                        + " for the others to install. MC-DOS, MC-NET and UNIX System V install from media only.")
                .paragraph("Some programs only run on one kind of computer: the Network Manager on the Mainframe, the"
                        + " Crafting Manager on a Crafting Computer, the Cluster Manager on a Cluster Management"
                        + " Computer, the Workshop on a Personal Computer. And some need the network to run a"
                        + " particular engine, and say so when you open them ([](jsc:engines)).")
                .table("Some programs worth knowing")
                .property("Crafting Manager", "Loads patterns and shows the jobs being worked")
                .property("Pattern Studio", "Writes recipes and burns them at an encoder")
                .property("Workshop", "Crafts, smelts, enchants and repairs with the workshop cards")
                .property("Automation Manager", "Writes and watches the jobs the network runs on its own")
                .property("Storage Insights", "The biggest stocks, and what is running low")
                .property("Craft Planner", "What a craft needs and costs, before it starts")
                .property("Remote Control", "Takes over another machine of the network")
                .paragraph("And for the rest of the time there are editors, the compilers of Σ# and Σ, Soundfoundry"
                        + " for music, Paint, and games: Minesweeper, Solitaire, Snake.")
                .ifSomethingGoesWrong("A program says it needs another engine.",
                        "It is written for an engine the network does not run. Install that engine on the"
                                + " Mainframe.",
                        "The package manager installs nothing.",
                        "The network has no Mirror, or this system installs from media only.")
                .register();
        guide.page("two_systems", section).titled("Two systems on one computer").icon(ComputingModule.CD_ROM)
                .paragraph("Why choose? Install a second system on another disk of the same computer, and you have"
                        + " both.")
                .paragraph("From then on the computer stops at a boot manager each time it starts, so you pick which"
                        + " system to start: the Midsoft Boot Manager on Frames, GRUB on Linux, FreeBSD's own"
                        + " loader. The firmware's boot order says which disk starts first when you do not pick"
                        + " ([](jsc:firmware)).")
                .paragraph("A server setting can turn the boot manager off, if you would rather the computer always"
                        + " went straight to the first disk.")
                .ifSomethingGoesWrong("The computer starts into the wrong system.",
                        "Change the boot order in the firmware, or pick at the boot manager.")
                .register();
        guide.page("help_on_a_computer", section).titled("Help on a computer").icon(ComputersGuide.MANUAL)
                .paragraph("This manual is not only in your hands: every computer can read it too, beside the manual"
                        + " pages of its own commands, the way its own system would.")
                .paragraph("On Frames 95 it is Help Topics, with its Contents, Index and Find; Frames XP has the Help"
                        + " and Support Center, Frames 7 Help and Support, and Frames 10 and 11 Get Help. On KDE it"
                        + " is the Help Center, on GNOME and Cinnamon Help, and on CDE the Help Viewer.")
                .paragraph("At a prompt, type help on MC-DOS and MC-NET, and it takes the whole screen in the sixteen"
                        + " colours; info on a Linux distribution reads every chapter, section and entry as a node;"
                        + " and man on UNIX and FreeBSD pages an entry as a page of section 7, so man graphics-cards"
                        + " opens the entry on graphics cards.")
                .ifSomethingGoesWrong("man says there is no manual entry.",
                        "On Linux the manuals are read with info. Elsewhere, check the topic's name: man"
                                + " graphics-cards, not man graphics.")
                .register();
    }

    private static void network(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("network").titled("The network")
                .icon(ComputingModule.SERVER_RACK).register();
        guide.page("what_a_network_is", section).titled("What a network is here").icon(ComputingModule.MAINFRAME)
                .paragraph("In most storage mods the network is a set of cables that carries channels, and you spend"
                        + " half your time counting them. J's Computers has no channels and no subnetworks at all."
                        + " What it has is much closer to a real computer network.")
                .paragraph("A network is every computer and device joined by data cable to one Mainframe, its brain"
                        + " ([](jsc:mainframes)). The cables come in lines, by the job they do: the access line joins"
                        + " the small computers to a router, the backbone joins the routers, the Mainframe and the"
                        + " racks, the long distance line joins two networks, and the high compute line joins a"
                        + " supercomputer's nodes ([](jsc:data_cables)). You lay the line the job asks for, and you"
                        + " never count what a cable holds.")
                .paragraph("What a cable does have is a speed and a range: how many items a tick an Operation"
                        + " crossing it moves, and how many cables a run can be before a router or a repeater renews"
                        + " it. So a network built with the right cables in the right places is a fast network.")
                .paragraph("And what does the network do for you? It keeps your items as data on its servers' disks"
                        + " ([](jsc:items_as_data)), gives them back when you ask, moves them where you want and"
                        + " crafts what you need ([](jsc:how_autocrafting_works)). Every one of those is an"
                        + " Operation ([](jsc:what_operations_are)).")
                .register();
        guide.page("first_network", section).titled("Your first network").icon(ComputingModule.MAINFRAME)
                .shows(ComputingModule.MAINFRAME, ComputingModule.SERVER_RACK, ComputingModule.SERVER,
                        ComputingModule.STANDARD_ROUTER, () -> ComputingModule.FIBRE_CABLE,
                        () -> ComputingModule.GIGABIT_CABLE, ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Here is the smallest network that does something useful.")
                .steps("Build a Mainframe and give it its parts: an MTX motherboard, a power supply, processors and"
                                + " memory ([](jsc:mainframes)).",
                        "Build a Server Rack and put servers with disks in its bays ([](jsc:server_racks)).",
                        "Join the Mainframe and the rack with the backbone cable of their era: HBW in the Legacy,"
                                + " Fibre Optic in the Standard.",
                        "Place a router, and join your Personal Computer to it with the access cable, Ethernet or"
                                + " Gigabit ([](jsc:routers)). Run the backbone from the router to the Mainframe.",
                        "At the computer, open Network, the Network Interactor. Put items in, and they are stored on"
                                + " the servers' disks; ask for them back, and they come out.")
                .paragraph("That is it. From here everything else is more of the same: more racks for more storage,"
                        + " more computers on the access line, a Crafting Computer for autocrafting.")
                .ifSomethingGoesWrong("A computer is not on the network.",
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
                .paragraph("The data cables are the network itself. They come in lines by the job they do, and each"
                        + " line has one cable for every era it exists in:")
                .table("The lines")
                .property("Access", "Small computers to a router: Thin Coaxial, Ethernet, Cat 5e, Gigabit, Cat 6a")
                .property("Backbone", "Routers, Mainframe and racks: Thick Coaxial, HBW, 10GBASE-CX4, Fibre Optic,"
                        + " OM5 Fibre")
                .property("Long distance", "Two networks, between two Gateway computers")
                .property("High compute", "A supercomputer's nodes to its HBW Interface")
                .paragraph("Every cable has a speed and a range, and both count. A run longer than its range carries"
                        + " nothing, so whatever lies only beyond it is off the network; and an Operation between two"
                        + " machines moves no faster than the slowest cable on the best way between them.")
                .define("Range", "How many cables a run can be before a router or a repeater renews it. A run longer"
                        + " than its range carries nothing.")
                .define("Speed", "The items a tick an Operation crossing a cable moves at most.")
                .paragraph("All the cables of the series share one block: a block holds up to nine wires side by"
                        + " side, each in its own lane, so a data cable, a peripheral cable and a crafting cable can"
                        + " run through the same block without joining. A dye colours a wire, and two wires of"
                        + " different colours never join, which is how you keep two runs apart where they touch.")
                .paragraph("Two rules come from the real thing. From the Standard on, the backbone's fibre runs only"
                        + " straight, so it turns at an optical router ([](jsc:routers)). And the long distance line"
                        + " runs between exactly two ends and holds its block alone.")
                .ifSomethingGoesWrong("Two cables side by side do not join.",
                        "They are of different lines or eras, or dyed in different colours.",
                        "Fibre will not turn a corner.",
                        "From the Standard on, fibre runs only straight. Turn it at an optical router.")
                .register();
        guide.page("routers", section).titled("Routers and repeaters").icon(ComputingModule.STANDARD_ROUTER)
                .coversAll(blocksOf(RouterBlock.class, RepeaterBlock.class))
                .paragraph("A router is where the lines of a network meet. Each era has its own, and a router joins"
                        + " its era's access line to its backbone: your small computers on one side, the Mainframe"
                        + " and the racks on the other. It takes the cables of both lines of its era, and of every"
                        + " earlier era, on any of its six faces, and all of them are one network.")
                .paragraph("Eras matter here: a port takes its own era's cable and every older one, never a newer one,"
                        + " and two eras of one line that touch do not join; they meet at a router of the newer era."
                        + " So an old machine on a new network needs a router between them.")
                .paragraph("The optical routers take only the backbone's fibre, and they are where fibre runs meet,"
                        + " turn and branch.")
                .paragraph("A repeater renews a cable's range: every run starts its range over at a repeater, so a"
                        + " cable goes twice as far with one halfway. The lines pass through it each on its own,"
                        + " never joining one another there.")
                .ifSomethingGoesWrong("An older machine will not join a newer network.",
                        "Put a router of the newer era between them.",
                        "The far end is still off the network.",
                        "A run on one side of the repeater is still longer than its range.")
                .register();
        guide.page("hbw_interface", section).titled("HBW Interface").icon(ComputingModule.HBW_INTERFACE)
                .coversAll(blocksOf(HbwInterfaceBlock.class))
                .paragraph("A supercomputer is not one block: it is every Supercomputer Rack tied together by the"
                        + " high compute cable, InfiniBand, High Compute or OSFP. The HBW Interface is the one point"
                        + " where all of it meets the rest of the network, on the backbone.")
                .paragraph("The Standard has one, and the Advanced has another that takes the OSFP cable.")
                .steps("Join the supercomputer racks to the interface with high compute cable.",
                        "Join the interface to the network's backbone.")
                .ifSomethingGoesWrong("The supercomputer is not seen.",
                        "Its racks are not all on the high compute cable that reaches the interface.")
                .register();
        guide.page("network_gateway", section).titled("Network Gateway").icon(ComputingModule.NETWORK_GATEWAY)
                .coversAll(blocksOf(NetworkGatewayBlock.class))
                .paragraph("Do you play with ComputerCraft too? The Network Gateway is a bridge between its computers"
                        + " and yours. Linked to one of our computers through a peripheral cable on its back, and to"
                        + " a ComputerCraft wired network on its front, it lets a ComputerCraft program ask the"
                        + " network what it holds, and ask for items.")
                .paragraph("What ComputerCraft may do through it is up to you: the Gateway Manager program sets it,"
                        + " down to the highest priority its requests may carry.")
                .ifSomethingGoesWrong("It does nothing.",
                        "It is not linked to one of our computers, or that computer is off.")
                .register();
        guide.page("server_router", section).titled("Server Router").icon(ComputingModule.SERVER_ROUTER)
                .coversAll(blocksOf(ServerRouterBlock.class))
                .paragraph("When your storage outgrows a rack or two, you are building a datacenter, and the Server"
                        + " Router is how you organize one. It switches the network and groups Server Racks into"
                        + " sections, one for each of its faces besides the uplink.")
                .steps("Join its uplink face to the network.",
                        "Join a section of racks to each of its other faces.",
                        "Open it to see each section and how much it can serve.")
                .paragraph("Each section has a budget of what it can serve, and the Cluster Manager runs every"
                        + " section as one machine ([](jsc:cluster_management_computers)).")
                .ifSomethingGoesWrong("It says a section is over budget.",
                        "The section holds more racks than it can serve. Split them over more faces.")
                .register();
    }

    private static void operations(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("operations").titled("Operations")
                .icon(ComputingModule.MAINFRAME).register();
        guide.page("what_operations_are", section).titled("What an Operation is").icon(ComputingModule.MAINFRAME)
                .paragraph("In most storage mods, storage is something that just happens. You put a disk in a drive"
                        + " and the network serves you whatever is on it, like a service that is always there. It"
                        + " works, but it easily makes everything feel like one huge chest that you put things in"
                        + " and take things out of.")
                .paragraph("J's Computers does not work like that. Here the network does not simply serve you: its"
                        + " computers do work to get each thing done, and every piece of that work is an Operation.")
                .paragraph("So, what is an Operation? It is an action the network is carrying out. Operations use a"
                        + " semantic close to SQL, the language databases speak. When you take items out of the"
                        + " storage you are not grabbing them from a server's hard disk yourself: you are sending the"
                        + " network a SELECT, and the network works it. The request goes into a queue, the Mainframe"
                        + " picks it up, finds where the items are, and moves them to you, as fast as its hardware"
                        + " allows.")
                .paragraph("That is why the Mainframe's hardware matters so much ([](jsc:mainframes)): its processors"
                        + " decide how many items it handles at once, and each graphics card gives it one more queue"
                        + " to work in at the same time.")
                .paragraph("There are many other Operations besides SELECT, and"
                        + " [the catalogue](jsc:operations_catalogue) tells you what each one does. You can send them"
                        + " with a click in the Network program, or write them yourself in IQL, the network's"
                        + " language ([](jsc:iql)): SELECT 64 iron_ingot.")
                .paragraph("And you can watch every one of them in the Mainframe's Task Manager, from the moment it is"
                        + " queued until it is done ([](jsc:states_and_queues)).")
                .register();
        guide.page("operations_catalogue", section).titled("The catalogue of Operations")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Here is every Operation the network knows, and what it does. They take their names from"
                        + " the commands of databases, so if you know a little SQL, you already know most of them.")
                .define("SELECT", "Takes items out of storage, to the computer that asked: SELECT 64 iron_ingot.")
                .define("INSERT", "Puts items into storage.")
                .define("MOVE", "Takes items out to a place: an inventory, a machine.")
                .define("DELETE", "Sends items out of the network into an outside inventory. Despite its name, it"
                        + " destroys nothing.")
                .define("DROP", "Destroys what is stored. This one does.")
                .define("CRAFT", "Makes items, from crafting table recipes, machine recipes, or both in many steps"
                        + " ([](jsc:how_autocrafting_works)).")
                .define("UPDATE", "Changes stored items with the workshop cards: smelts, enchants, repairs, combines"
                        + " and names them ([](jsc:expansion_cards)).")
                .define("ANALYZE, REINDEX, VACUUM", "Look after the network's index of what it stores, as a database"
                        + " looks after its own.")
                .warning("DROP destroys the items for good. DELETE only sends them out.")
                .paragraph("You do not need to type any of them: the Network program sends them for you when you"
                        + " click. But you can, in IQL ([](jsc:iql)), and then you can say exactly what you want.")
                .register();
        guide.page("states_and_queues", section).titled("States, priorities and queues")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("An Operation is always in one of eight states, and the Task Manager on the Mainframe shows"
                        + " you which:")
                .table("The states of an Operation")
                .property("PENDING", "Waiting in its queue")
                .property("PROCESSING", "Being worked")
                .property("WAITING", "Paused for something")
                .property("COMPLETED", "Done")
                .property("COMPLETED_PARTIAL", "Done with less")
                .property("FAILED", "Not done, and says why")
                .property("RESOURCE_LOCKED", "What it needs is held")
                .property("DISCARDED", "Dropped")
                .paragraph("Each Operation also has a priority, from low to high, medium unless you say otherwise."
                        + " When there are more Operations ready than queues free, the highest priority goes first,"
                        + " and equal ones keep the order they came in. One that waits too long climbs in priority,"
                        + " so nothing waits forever; and one that waits on something for too long gives up.")
                .paragraph("And the queues? The Mainframe works one queue with its processors, and one more for each"
                        + " graphics card it has, all at the same time. That is why a Mainframe with more graphics"
                        + " cards gets through more Operations at once, and why a faster processor gets each of them"
                        + " done sooner ([](jsc:mainframes)).")
                .ifSomethingGoesWrong("An Operation stays PENDING.",
                        "The Mainframe is busy, off, or has no processor time to spare. A faster Mainframe, or more"
                                + " graphics cards, help.",
                        "It ends RESOURCE_LOCKED.",
                        "What it needed is held by another Operation, or by a LOCK. Wait, or UNLOCK it.",
                        "It ends COMPLETED_PARTIAL.",
                        "There was less than you asked for.")
                .register();
        guide.page("iql", section).titled("IQL, the network's language").icon(ComputingModule.MAINFRAME)
                .paragraph("Clicking in the Network program is fine for taking a stack of iron. But what if you want"
                        + " to know everything the network holds more than a hundred of? Or to craft 64 torches from"
                        + " a script? That is what IQL, the Item Query Language, is for: asking the network for"
                        + " anything, in words, at a command prompt, in a program or in a script.")
                .paragraph("It reads like the language of databases:")
                .table("A few lines of IQL")
                .fixed("Take items", "SELECT 64 iron_ingot")
                .fixed("Craft", "CRAFT 64 torch")
                .fixed("Ask", "QUERY items WHERE qty > 100")
                .fixed("Sort and cut", "QUERY items ORDER BY name LIMIT 2")
                .fixed("Look at the disks", "QUERY disks")
                .paragraph("A request names an Operation, a quantity and an item, and may add FROM, TO, WHERE, IF,"
                        + " ORDER BY, LIMIT and PRIORITY. A question starts with QUERY or SHOW. A script is"
                        + " statements separated by semicolons, with -- comments, and it can ask for values when it"
                        + " is run.")
                .paragraph("On an engine that offers them, you can save what you write often as a view, a procedure"
                        + " or a job, and a job can run on its own, every so often or when a condition is met"
                        + " ([](jsc:engines)). A Σ# program speaks IQL too.")
                .ifSomethingGoesWrong("A line is refused.",
                        "The prompt says where it stopped reading. Fix the word there.",
                        "Views, procedures and jobs are refused.",
                        "They need an engine that offers them.")
                .register();
        guide.page("engines", section).titled("Engines").icon(ComputingModule.MAINFRAME)
                .paragraph("What actually reads IQL and plans the work is the Mainframe's engine, a program installed"
                        + " on it. Every Mainframe comes with the Midsoft IQL Server, and there are others, each"
                        + " speaking its own version of the language and offering its own extras:")
                .table("The engines")
                .property("Midsoft IQL Server", "Every Mainframe: IQL, with views and procedures")
                .property("NextgreIQL", "From the Legacy: shows how it plans, and takes hints")
                .property("Prophet YourIQL", "From the Legacy: you declare a state, and it keeps it")
                .paragraph("Prophet is the curious one: instead of asking for 64 steel ingots, you write KEEP"
                        + " steel_ingot >= 10000, and it works out the Operations that reach that state and keep it"
                        + " there.")
                .paragraph("Programs written for one engine, such as the Nextgre Planner Studio or the Prophet"
                        + " Reactive Console, say so when you open them on a network that runs another.")
                .ifSomethingGoesWrong("A program says it needs another engine.",
                        "Install that engine on the Mainframe.")
                .register();
    }

    private static void storage(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("storage").titled("Storage")
                .icon(HardwareItems.DISK_TRENCH_20M).register();
        guide.page("items_as_data", section).titled("Items as data").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("Remember that the network keeps your items on its disks? This is how. It keeps items,"
                        + " fluids and chemicals as data on the disks of its servers, all counted the same way: a"
                        + " bucket takes as much room as an item.")
                .paragraph("How much room an item takes depends on the era of the disk, because older computers had"
                        + " smaller words: 1 MB in the Vintage, 16 MB in the Legacy, 256 MB from the Transition on."
                        + " That is why a 20 MB Vintage drive holds 20 items and a 1 TB disk holds 4,096.")
                .define("Item size", "The room an item takes on a disk: 1 MB in the Vintage, 16 MB in the Legacy,"
                        + " 256 MB from the Transition on.")
                .paragraph("So more storage means more disks, more servers or newer disks. Disks hold folders, files"
                        + " and programs as well, so the same disks that store your iron can keep your scripts.")
                .paragraph("Items get in and out through the Network program on any computer of the network, or"
                        + " through buses ([](jsc:buses)).")
                .ifSomethingGoesWrong("Storage is full.",
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
                .paragraph("Buses are thin parts you put on the face of a data cable, against a chest or a machine, so"
                        + " the network can reach it without you.")
                .paragraph("The Import Bus brings items from the inventory it faces into the network; the Export Bus"
                        + " sends them out to it. Each has a filter, quantities to keep and to stop at, a mode"
                        + " (always, or only on a redstone signal), a priority and conditions, more of them the newer"
                        + " its era.")
                .paragraph("The External Storage Bus moves nothing by itself: the network uses the inventory it faces"
                        + " as storage of its own, ten times slower than a server. Handy for a chest you already"
                        + " have.")
                .paragraph("A bus's window has three tabs: what it is set to, what it did, and the IQL and Σ# lines"
                        + " that would set it the same way, since programs can set buses too.")
                .ifSomethingGoesWrong("The bus moves nothing.",
                        "Its filter takes nothing, it waits for a redstone signal, or it is not on the network.")
                .register();
        guide.page("tank", section).titled("Tank").icon(ComputingModule.TANK)
                .coversAll(blocksOf(TankBlock.class))
                .paragraph("A plain fluid tank, and the network's way of handling fluids: an Import Bus on a cable"
                        + " against it takes its fluid in, and an Export Bus fills it, the same way they move items.")
                .paragraph("Fill it with a bucket or a pipe. Inside the network a bucket of fluid takes as much room"
                        + " as an item ([](jsc:items_as_data)).")
                .ifSomethingGoesWrong("The fluid does not go in.",
                        "The network's storage has no room left.")
                .register();
    }

    private static void autocrafting(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("autocrafting").titled("Autocrafting")
                .icon(ComputingModule.CRAFTING_CARD_PCIE3).register();
        guide.page("how_autocrafting_works", section).titled("How autocrafting works")
                .icon(() -> ComputingModule.CRAFTING_CABLE)
                .covers(() -> ComputingModule.CRAFTING_CABLE)
                .paragraph("You ask for 512 pistons, and the network works out every step from what it has: how many"
                        + " planks, how much cobblestone, iron and redstone, what it has to make first, on which"
                        + " machines and in which order. Then it runs the crafting table recipes and the machines,"
                        + " and puts the pistons in storage. That is autocrafting.")
                .paragraph("To do it the network has to know the recipes, and a recipe it knows is a pattern. There"
                        + " are three kinds: a crafting table pattern (a 3x3 grid and its result), a machine pattern"
                        + " (what goes into a machine and what comes back), and a many-step pattern, steps of both"
                        + " kinds in order.")
                .define("Pattern", "A recipe the network knows: a crafting table grid, a machine's inputs and"
                        + " outputs, or both in many steps.")
                .paragraph("The parts are a Crafting Computer that runs the crafts ([](jsc:crafting_computers)), the"
                        + " Crafting Cable that joins it to the machines, and on that cable the parts that feed each"
                        + " machine ([](jsc:crafting_interfaces)). Patterns are written in the Pattern Studio and"
                        + " carried to the Crafting Computer on media ([](jsc:pattern_encoders)).")
                .paragraph("Any machine that offers the usual item, fluid, energy and chemical access works, from any"
                        + " mod.")
                .ifSomethingGoesWrong("The plan says something is missing.",
                        "An item has no pattern and none is in storage. Add its pattern or the item.")
                .register();
        guide.page("crafting_computers", section).titled("Crafting Computers and Crafting Cards")
                .icon(ComputingModule.CRAFTING_COMPUTER)
                .coversAll(blocksOf(CraftingComputerBlock.class)).coversAll(itemsOf(CraftingCardItem.class))
                .paragraph("The Crafting Computer is the computer that crafts for the network: it runs crafting table"
                        + " recipes itself, and drives the machines that make the rest.")
                .paragraph("Build it as you would a Personal Computer, then give it a Crafting Card: without one it"
                        + " does not craft at all. The card decides how much it can do: it drives a number of"
                        + " Crafting Interfaces, and keeps that many crafting table patterns in its own memory, so"
                        + " they move with the card.")
                .table("What each era's card drives")
                .fixed("Vintage", "2")
                .fixed("Legacy", "4")
                .fixed("Transition", "5")
                .fixed("Standard", "6")
                .fixed("Advanced", "8")
                .paragraph("Open the Crafting Manager on it to load patterns and watch the jobs"
                        + " ([](jsc:pattern_encoders)).")
                .ifSomethingGoesWrong("It crafts nothing.",
                        "It has no Crafting Card, or it is off.",
                        "A crafting table pattern is gone.",
                        "It lives on the card: it went with the card to another computer.")
                .register();
        guide.page("crafting_interfaces", section).titled("Crafting Interfaces and the machine line")
                .icon(ComputingModule.CRAFTING_INTERFACE_ITEM)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.VINTAGE_CRAFTING_INTERFACE_ITEM,
                        ComputingModule.LEGACY_CRAFTING_INTERFACE_ITEM,
                        ComputingModule.TRANSITION_CRAFTING_INTERFACE_ITEM, ComputingModule.CRAFTING_INTERFACE_ITEM,
                        ComputingModule.ADVANCED_CRAFTING_INTERFACE_ITEM, ComputingModule.CRAFTING_ROUTER_ITEM,
                        ComputingModule.RECEIVING_BUS_ITEM))
                .paragraph("A machine line is how the network runs a machine. It sits on the Crafting Cable, and it"
                        + " has three parts.")
                .paragraph("The Crafting Interface sits on the cable against the machine: it holds that machine's"
                        + " patterns, from 3 to 12 by its era, and feeds it. It feeds the machine directly when it"
                        + " sits against it, or through a crafting cable of its own, dyed apart from the main one.")
                .paragraph("For a machine that takes inputs on more than one face, a Crafting Input Router goes"
                        + " against each input face, on the interface's own cable. And a Crafting Receiving Bus goes"
                        + " against the machine's output: it takes what comes back and credits it to the craft that"
                        + " fed it.")
                .steps("Lay Crafting Cable from the Crafting Computer to the machine.",
                        "Put a Crafting Interface on the cable against the machine.",
                        "For a machine with several input faces, put a Crafting Input Router against each.",
                        "Put a Crafting Receiving Bus against the machine's output.",
                        "Give the machine power, the way its own mod says.")
                .paragraph("An interface works one recipe at a time or several jobs at once, and can be paused and"
                        + " given a limit.")
                .ifSomethingGoesWrong("A machine is never fed.",
                        "Its interface is not on a cable a Crafting Computer drives, the computer is off, or its"
                                + " card drives fewer interfaces than the cable reaches.",
                        "The output never comes back.",
                        "No Crafting Receiving Bus faces the machine's output.")
                .register();
        guide.page("pattern_encoders", section).titled("Patterns and the Pattern Encoder")
                .icon(ComputingModule.PATTERN_ENCODER).coversAll(blocksOf(PatternEncoderBlock.class))
                .paragraph("Patterns are written on a computer, in the Pattern Studio: the grid for a crafting table"
                        + " pattern, or the inputs, the machine and the outputs for a machine pattern. With JEI"
                        + " installed you can carry a recipe straight from JEI into it.")
                .paragraph("But a pattern on one computer is no use to another, so you carry it the old way: on"
                        + " media. The Pattern Encoder is a device joined to the computer by Peripheral Cable that"
                        + " burns patterns onto media: floppies in the Vintage, CDs in the Legacy, and DVDs, CDs or"
                        + " USB sticks from the Standard.")
                .steps("Write the pattern in the Pattern Studio.",
                        "Put a blank medium in a Pattern Encoder joined to the computer, and burn the pattern onto"
                                + " it.",
                        "On the Crafting Computer, open the Crafting Manager and load the pattern from the medium: a"
                                + " crafting table pattern into the card, a machine pattern into the interface of its"
                                + " machine.")
                .ifSomethingGoesWrong("The Pattern Studio sees no encoder.",
                        "It is not joined to that computer.")
                .register();
        guide.page("asking_for_a_craft", section).titled("Asking for a craft").icon(ComputingModule.CRAFTING_COMPUTER)
                .paragraph("You ask for a craft the way you ask for anything else: in the Network program, or in IQL,"
                        + " with CRAFT 512 piston.")
                .paragraph("The network plans it from the bottom up: what it has, what it must make first, in which"
                        + " order and on which machines. Then it runs the steps on as many machines as it has, at the"
                        + " same time wherever they do not depend on each other.")
                .paragraph("Before you start, the Craft Planner shows you the plan: what it needs, what is missing and"
                        + " what it costs. While it runs, the Crafting Manager shows the jobs.")
                .ifSomethingGoesWrong("A many-step craft fails at one step.",
                        "That step ran out of time: its machine has no power, is full, or never gave its output to"
                                + " the Receiving Bus. The craft says which step.")
                .register();
    }

    private static void peripherals(final ModGuide guide) {
        final ModGuide.SectionRef section = guide.section("peripherals").titled("Peripherals")
                .icon(ComputingModule.MONITOR).register();
        guide.page("monitors", section).titled("Monitors").icon(ComputingModule.MONITOR)
                .coversAll(blocksOf(MonitorBlock.class))
                .paragraph("The monitor is the screen of a computer, and everything you do with its programs, you do"
                        + " at it. Use the computer block and you open the case; use the monitor and you sit at the"
                        + " computer.")
                .paragraph("The Vintage has green, amber and paper-white screens, and colour comes after. A monitor"
                        + " takes a video output of the computer's graphics card, placed right against the computer"
                        + " or joined with peripheral cable, and uses part of the card's video memory, a bigger"
                        + " monitor more ([](jsc:graphics_cards)).")
                .paragraph("And when nobody is sitting at it, the monitor still shows what the computer is doing, out"
                        + " in the world.")
                .ifSomethingGoesWrong("The monitor says no computer is in range.",
                        "Lay peripheral cable all the way, within its range.",
                        "The monitor says there is not enough video memory.",
                        "Use a card with more, or fewer or smaller monitors.")
                .register();
        guide.page("drives", section).titled("Drives and the Dock Station").icon(ComputingModule.FLOPPY_DRIVE)
                .coversAll(blocksOf(MediaReaderBlock.class))
                .paragraph("Drives read removable media: the Floppy Drive, the CD Drive, the DVD Drive and the"
                        + " Blu-ray Drive, and the Dock Station, which takes USB sticks. You need them to install"
                        + " systems and programs, and to carry files and patterns between computers.")
                .paragraph("Each medium goes in its own kind of drive, and a drive is joined to the computer like any"
                        + " other device ([](jsc:peripheral_cables)).")
                .ifSomethingGoesWrong("The medium will not go in.",
                        "Each medium goes in its own kind of drive.")
                .register();
        guide.page("peripheral_cables", section).titled("Peripheral cables and hubs")
                .icon(() -> ComputingModule.PERIPHERAL_CABLE)
                .coversAll(() -> List.<ItemLike>of(ComputingModule.VINTAGE_PERIPHERAL_CABLE,
                        ComputingModule.LEGACY_PERIPHERAL_CABLE, ComputingModule.TRANSITION_PERIPHERAL_CABLE,
                        ComputingModule.PERIPHERAL_CABLE, ComputingModule.ADVANCED_PERIPHERAL_CABLE))
                .coversAll(blocksOf(HubBlock.class))
                .paragraph("A peripheral cable joins a computer to its devices: monitors, speakers, drives, printers,"
                        + " hubs, Redstone Interfaces. It is not a data cable, and it carries no network. A device"
                        + " placed right against its computer needs no cable at all.")
                .table("How far each era's cable reaches")
                .fixed("Vintage", "8")
                .fixed("Legacy", "12")
                .fixed("Transition", "14")
                .fixed("Standard", "16")
                .fixed("Advanced", "20")
                .paragraph("Ports go by kind, as on a real computer: a monitor takes a video output of a graphics"
                        + " card, speakers the audio output of the sound card (or of the board, from the Transition"
                        + " on), and every other device one of the board's device ports. A port takes its era's"
                        + " cable and every earlier one.")
                .paragraph("When the board runs out of device ports, a hub takes one and offers more of its own: two"
                        + " on the Vintage switch box, four on the Legacy and Transition hubs, seven on the Standard"
                        + " and Advanced ones. The cable after a hub reaches as far again, and hubs can hang from"
                        + " hubs. Screens and speakers do not pass through a hub, though.")
                .paragraph("The Device Manager of each system shows what is on every port.")
                .ifSomethingGoesWrong("The device is not seen.",
                        "The cable is longer than its range, or of a newer era than the port.",
                        "A monitor joined through a hub stays dark.",
                        "Screens and speakers do not pass through a hub. Join them to the computer.")
                .register();
        guide.page("printers", section).titled("Printers").icon(ComputingModule.PRINTER)
                .coversAll(blocksOf(PrinterBlock.class)).coversAll(itemsOf(PrintedPaperItem.class))
                .paragraph("A printer for each era, and the Printed Paper it turns out. Join one to the computer, use"
                        + " it with paper to fill its tray, and print from a program: pages of text, or a picture,"
                        + " which shows on the sheet in an item frame.")
                .paragraph("Take the sheets from the printer's window.")
                .ifSomethingGoesWrong("Nothing comes out.",
                        "Its tray is empty. Use it with paper.")
                .register();
        guide.page("speakers", section).titled("Speakers").icon(ComputingModule.SPEAKER)
                .coversAll(blocksOf(SpeakerBlock.class, SubwooferBlock.class))
                .paragraph("Speakers carry a computer's sound somewhere other than the monitor. They come from the"
                        + " Legacy on, and two beside a monitor play a stereo recording, a side each.")
                .paragraph("And they sound like their era: Legacy speakers lose the bass and the treble, and"
                        + " Transition ones the bass, until a subwoofer stands against one of them.")
                .ifSomethingGoesWrong("The music has no bass.",
                        "Set the Transition's subwoofer against one of the speakers.")
                .register();
        guide.page("redstone_interfaces", section).titled("Redstone Interfaces")
                .icon(ComputingModule.STANDARD_REDSTONE_INTERFACE).coversAll(blocksOf(RedstoneInterfaceBlock.class))
                .paragraph("A small sensor on a computer's peripheral cable, placed the way an observer is. It lets a"
                        + " program read the redstone signal at its lens, or emit one there: your computers can"
                        + " finally open a door.")
                .paragraph("Name it in its window, so programs find it by that name.")
                .ifSomethingGoesWrong("It reads and emits nothing.",
                        "It has no computer at the other end of its cable.")
                .register();
    }
}
