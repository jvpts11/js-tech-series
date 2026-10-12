/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the Midsoft systems chapter: which edition of Frames fits which machine, how each edition is
 * installed, how two systems share one computer, the boot list that picks between them, and how memory decides
 * what a window may do.
 */
final class MidsoftSystemsPages {

    private MidsoftSystemsPages() {
    }

    /** Declares the section and entries of the Midsoft systems, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        installsSection(guide, chapter);
    }

    private static void installsSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("midsoft_installs")
                .titled("Installing Midsoft's systems").icon(ComputingModule.CD_ROM).in(chapter).register();
        framesWhichMachine(guide, section);
        framesSetup95Xp(guide, section);
        framesSetup7To11(guide, section);
        twoSystems(guide, section);
        midsoftBootManager(guide, section);
        framesMemory(guide, section);
    }

    private static void framesWhichMachine(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_which_machine", section).titled("Which Frames goes on which machine")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Midsoft made five editions of Frames. Each edition is built for one era of computer,"
                        + " the generation a machine belongs to ([](jsc:eras)). A system runs on the era it was"
                        + " made for and on every newer era, but never on an older machine.")
                .paragraph("Choose the edition by the machine you have. The editions also have a rank, from 1 for"
                        + " Frames 95 to 5 for Frames 11. Programs use the rank when they ask for a Frames edition,"
                        + " such as XP or newer. The installer does not use it.")
                .paragraph("The install medium is the compact disc (CD), digital versatile disc (DVD) or Universal"
                        + " Serial Bus (USB) stick that a system is installed from. You read a medium in a drive"
                        + " joined to the computer: a CD goes in a CD Drive, a DVD in a DVD Drive, and a USB stick"
                        + " in a Dock Station ([](jsc:dock_station)).")
                .table("The five editions and their machines", "Edition", "Machine era", "Install medium")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Legacy"), GuideCell.words("CD"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Legacy"), GuideCell.words("CD"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Transition"), GuideCell.words("DVD"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Standard"), GuideCell.words("USB stick"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Advanced"), GuideCell.words("USB stick"))
                .paragraph("Own RAM is the random-access memory (RAM) that the running system keeps for itself"
                        + " before any program opens. Footprint is the space the system takes on its disk. Both"
                        + " are counted in megabytes (MB), and the table below gives them for each edition"
                        + " ([](jsc:frames_memory)).")
                .table("Own RAM and footprint of each edition", "Edition", "Own RAM (MB)", "Footprint (MB)")
                .row(GuideCell.words("Frames 95"), GuideCell.amount(32, "MB"), GuideCell.amount(48, "MB"))
                .row(GuideCell.words("Frames XP"), GuideCell.amount(128, "MB"), GuideCell.amount(1536, "MB"))
                .row(GuideCell.words("Frames 7"), GuideCell.amount(1024, "MB"), GuideCell.amount(16384, "MB"))
                .row(GuideCell.words("Frames 10"), GuideCell.amount(3072, "MB"), GuideCell.amount(16384, "MB"))
                .row(GuideCell.words("Frames 11"), GuideCell.amount(8192, "MB"), GuideCell.amount(20480, "MB"))
                .paragraph("A disk measures its room in items, and an item takes 1 MB on a Vintage disk, 16 MB on"
                        + " a Legacy disk and 256 MB on a disk of the Transition era or newer. So the same system"
                        + " takes fewer items on a newer disk. The footprint counts whole items, rounded up"
                        + " ([](jsc:disks)).")
                .table("Items each edition takes on a disk", "Edition", "On a Legacy disk",
                        "On a Transition, Standard or Advanced disk")
                .row(GuideCell.words("Frames 95"), GuideCell.words("3 items"), GuideCell.words("1 item"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("96 items"), GuideCell.words("6 items"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("1,024 items"), GuideCell.words("64 items"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("1,024 items"), GuideCell.words("64 items"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("1,280 items"), GuideCell.words("80 items"))
                .paragraph("A Legacy disk of 4 gigabytes (GB), such as the Vaultis Link IDE-HDD 4G, holds 4,096 MB."
                        + " That is room for Frames XP, which needs 1,536 MB, but not for Frames 7, which needs"
                        + " 16,384 MB. The disks you can buy are listed in [Disks](jsc:disks).")
                .paragraph("The era also sets the firmware, the program that starts the machine. Frames 95, XP and 7"
                        + " start in the blue basic input/output system (BIOS), and Frames 10 and 11 in the"
                        + " Unified Extensible Firmware Interface (UEFI) ([](jsc:firmware)).")
                .paragraph("The memory of a machine comes from its memory modules. The smallest module of the Legacy"
                        + " era is the Stratix Layer SDRAM-128, with 128 MB. A Legacy machine with that module runs"
                        + " Frames XP without tight RAM, which the memory page explains"
                        + " ([](jsc:frames_memory)).")
                .define("Edition", "One version of Frames, with its own installer and its own desktop.")
                .define("Install medium", "The disc or stick a system is installed from, such as a CD or a USB"
                        + " stick.")
                .define("Firmware", "The program on the motherboard that starts the computer and tests its hardware"
                        + " before it hands over to a system.")
                .define("Own RAM", "The memory the running system keeps for itself before any program opens, in"
                        + " megabytes.")
                .define("Footprint", "The space a system takes on its disk, in megabytes. The disk counts it in"
                        + " items.")
                .ifSomethingGoesWrong("The screen says: \"Frames 10 needs Standard era hardware or newer; this"
                                + " machine is Legacy era.\"",
                        "Choose the edition for this machine's era, or use a newer machine ([](jsc:eras)).",
                        "The screen says: \"No disk is installed to put Frames 7 on.\"",
                        "Install a disk in the machine first, then start the installer again ([](jsc:disks)).",
                        "The screen says: \"The target disk has no room for\", then the system's name and, in brackets,"
                                + " the megabytes it needs.",
                        "Choose a bigger disk, or another disk with room ([](jsc:disks)).")
                .seeAlso("jsc:frames_setup_95_xp", "jsc:frames_setup_7_10_11", "jsc:eras")
                .register();
    }

    private static void framesSetup95Xp(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_setup_95_xp", section).titled("Setting up Frames 95 and XP")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Frames 95 and Frames XP both come on a CD, so you need a CD Drive joined to the computer"
                        + " ([](jsc:drives)). The disc holds the setup program, SETUP.EXE, which copies the system"
                        + " onto a disk. Frames 95 and XP each have their own setup pages, and the keys below"
                        + " matter on the Frames XP disk page.")
                .subheading("Frames 95")
                .paragraph("The firmware offers the disc as \"Frames 95 installer\", and choosing it starts the setup"
                        + " ([](jsc:firmware)).")
                .steps("Put the Frames 95 CD in a CD Drive that is joined to the computer.",
                        "Turn the computer on, and choose \"Frames 95 installer\" in the firmware.",
                        "Read the check on the first page, then press [[Enter]] to continue, or [[F3]] to exit.",
                        "Choose the disk for the system, then go on to the next page.",
                        "Type the computer's name, then go on.",
                        "Wait while the files are copied.",
                        "When the last page says \"Setup is finished.\", the copy is done.",
                        "You can then take the CD out of the drive.")
                .table("The pages of Frames 95", "Page", "What it shows")
                .row(GuideCell.words("Welcome"), GuideCell.words("The check: Generation, Processor, Memory, the"
                        + " disk and the installation medium, each with its answer."))
                .row(GuideCell.words("Disk"), GuideCell.words("\"Choose a Disk\", with the disks of the machine."))
                .row(GuideCell.words("Name"), GuideCell.words("\"Computer Name\": the name is cut to 256 letters."))
                .row(GuideCell.words("Copy"), GuideCell.words("\"Copying Files\"."))
                .row(GuideCell.words("Done"), GuideCell.words("\"Setup is finished.\""))
                .subheading("Frames XP")
                .paragraph("Frames XP comes on the same CD and starts the same way: choose \"Frames XP installer\" in"
                        + " the firmware. Its pages are different, and its disk page lists the keys you need.")
                .table("The keys on the Frames XP disk page", "Key", "What it does")
                .row(GuideCell.code("ENTER"), GuideCell.words("Installs the system on the chosen disk."))
                .row(GuideCell.code("E"), GuideCell.words("Erases the chosen disk, after you confirm it twice."))
                .row(GuideCell.code("F3"), GuideCell.words("Quits the setup."))
                .table("The pages of Frames XP", "Page", "What it shows")
                .row(GuideCell.words("Disk"), GuideCell.words("\"The following list shows the disks on this"
                        + " computer.\""))
                .row(GuideCell.words("Name"), GuideCell.words("\"Computer Name\"."))
                .row(GuideCell.words("Copy"), GuideCell.words("\"Please wait while Setup copies files to the disk.\""))
                .row(GuideCell.words("Install"), GuideCell.words("\"Installing Frames\", with the steps Installing"
                        + " Frames and Finalizing installation."))
                .row(GuideCell.words("Done"), GuideCell.words("\"Setup is finished.\", and the key ENTER restarts"
                        + " the computer."))
                .subheading("Choosing the disk")
                .paragraph("Both setups use the same rules for the disk. The setup proposes the first disk with room"
                        + " for the system. A disk has room when its free space is at least the system's footprint.")
                .paragraph("If the chosen disk is too small, the setup says \"No room here. Erase this disk, or choose"
                        + " another.\" Erasing is asked twice: the first answer only asks, and the second erases the"
                        + " disk. The question reads \"Erase Disk\" with the disk's number. The erase happens before"
                        + " the copy, and choosing another disk before the copy starts cancels it.")
                .define("Setup", "The installer program that copies a system onto a disk.")
                .ifSomethingGoesWrong("The screen says: \"No disk in this machine has room for it.\"",
                        "Erase a disk you no longer need, or add a bigger disk ([](jsc:disks)).",
                        "The screen says: \"No room here. Erase this disk, or choose another.\"",
                        "Choose another disk from the list, or erase this one if its files are not needed.",
                        "The firmware does not offer the installer, and says: \"No installation medium is in a drive"
                                + " linked to this machine.\"",
                        "Put the CD in a CD Drive that is joined to the computer ([](jsc:peripheral_cables)).",
                        "The copy stops and says: \"The medium was taken out before the system was written.\"",
                        "Put the CD back in the drive and start the setup again. Leave the CD in until the copy is"
                                + " done.")
                .seeAlso("jsc:frames_setup_7_10_11", "jsc:frames_95", "jsc:frames_xp", "jsc:installing")
                .register();
    }

    private static void framesSetup7To11(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_setup_7_10_11", section).titled("Setting up Frames 7, 10 and 11")
                .icon(ComputingModule.DVD_ROM)
                .paragraph("Frames 7 comes on a DVD, which you read in a DVD Drive. Frames 10 and Frames 11 come on a"
                        + " USB stick, which you plug into a Dock Station joined to the computer"
                        + " ([](jsc:dock_station)).")
                .paragraph("Each of the three has its own setup window, in the look of its era. They ask for the same"
                        + " things, a disk, a name and a copy, but in a different order and with different words.")
                .subheading("Frames 7")
                .paragraph("On Frames 7, the setup is a glass window titled \"Install"
                        + " Frames\". Its welcome page has one large \"Install now\" button.")
                .steps("Put the Frames 7 DVD in a DVD Drive that is joined to the computer.",
                        "Turn the computer on, and choose \"Frames 7 installer\" in the firmware.",
                        "Follow the pages in the table below, in the order the table gives them.")
                .table("The pages of Frames 7", "Page", "What it shows")
                .row(GuideCell.words("Welcome"), GuideCell.words("One large \"Install now\" button."))
                .row(GuideCell.words("Disk"), GuideCell.words("\"Where do you want to install Frames?\""))
                .row(GuideCell.words("Copy"), GuideCell.words("\"Installing Frames...\", with the steps Copying Frames"
                        + " files, Expanding Frames files, Installing features, Installing updates and Completing"
                        + " installation."))
                .row(GuideCell.words("Name"), GuideCell.words("\"Choose a name for your computer to distinguish it on"
                        + " the network.\""))
                .row(GuideCell.words("Done"), GuideCell.words("\"Setup is finished.\""))
                .subheading("Frames 10")
                .paragraph("Frames 10 has its own look, titled \"Frames Setup\". Its name page takes the whole screen.")
                .table("The pages of Frames 10", "Page", "What it shows")
                .row(GuideCell.words("Welcome"), GuideCell.words("One large \"Install now\" button."))
                .row(GuideCell.words("Disk"), GuideCell.words("\"Where do you want to install Frames?\""))
                .row(GuideCell.words("Copy"), GuideCell.words("\"Installing Frames\", with the steps Copying Frames"
                        + " files, Getting files ready for installation, Installing features, Installing updates and"
                        + " Finishing up."))
                .row(GuideCell.words("Name"), GuideCell.words("\"Who's going to use this PC?\" The answer becomes the"
                        + " computer's name."))
                .row(GuideCell.words("Done"), GuideCell.words("\"Setup is finished.\""))
                .subheading("Frames 11")
                .paragraph("Frames 11 uses cards, in a window titled \"Frames 11 Setup\". Each card has its own"
                        + " buttons: Next and Back, and Erase disk on the disk page. The welcome card asks nothing:"
                        + " it has one button, Install now.")
                .table("The pages of Frames 11", "Page", "What it shows")
                .row(GuideCell.words("Welcome"), GuideCell.words("One large \"Install now\" button, which asks"
                        + " nothing."))
                .row(GuideCell.words("Disk"), GuideCell.words("\"Where do you want to install Frames 11?\""))
                .row(GuideCell.words("Name"), GuideCell.words("\"Name this PC\"."))
                .row(GuideCell.words("Copy"), GuideCell.words("\"Installing Frames 11\", with the steps Copying files,"
                        + " Creating the system folders, Adding the built-in programs and Setting up the boot"
                        + " entry."))
                .row(GuideCell.words("Done"), GuideCell.words("\"Setup is finished.\" The button on this card is"
                        + " Restart."))
                .subheading("The USB stick")
                .paragraph("The USB stick holds the system for Frames 10 and 11. It does not go into the computer: it"
                        + " goes into the USB port of a Dock Station, and the Dock Station must be joined to the"
                        + " computer. The stick's tooltip says \"Plug into a linked Dock Station\", and a hint says to"
                        + " run the setup program or to install from This PC.")
                .steps("Plug the stick into the USB port of a Dock Station that is joined to the computer.",
                        "Turn the computer on, and choose \"Frames 10 installer\" or \"Frames 11 installer\" in the"
                                + " firmware.",
                        "Follow the pages of your edition, as the tables above show them.")
                .ifSomethingGoesWrong("The firmware does not offer the installer, and says: \"No installation medium is"
                                + " in a drive linked to this machine.\"",
                        "The stick is not in a Dock Station, or the Dock Station is not joined to the computer. Plug"
                                + " the stick in, and join the Dock Station ([](jsc:peripheral_cables)).",
                        "The disk page says: \"No room here. Erase this disk, or choose another.\"",
                        "Choose another disk with room, or erase this disk as described in the Frames 95 section"
                                + " ([](jsc:frames_setup_95_xp)).",
                        "The copy stops and says: \"The medium was taken out before the system was written.\"",
                        "Put the stick back in the Dock Station, start the setup again, and leave the stick in until"
                                + " the copy ends.")
                .seeAlso("jsc:frames_setup_95_xp", "jsc:dock_station", "jsc:drives")
                .register();
    }

    private static void twoSystems(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("two_systems", section).titled("Two systems on one computer").icon(ComputingModule.CD_ROM)
                .paragraph("Why choose? Install a second system on another disk of the same computer, and you have"
                        + " both.")
                .paragraph("Unless the server's boot menu is turned off, each start of a computer with two systems"
                        + " stops at a boot manager, so you pick which system to start: the Midsoft Boot"
                        + " Manager on Frames, GRUB on Linux, FreeBSD's own loader."
                        + " With one system only, a Frames machine goes straight through"
                        + " ([](jsc:midsoft_boot_manager)). The boot order in the firmware sets which disk the firmware"
                        + " tries first ([](jsc:firmware)).")
                .paragraph("The installer proposes a default disk for the new system: the first disk with no system on"
                        + " it. When every disk has a system, it proposes the first disk. A disk holds up to 16"
                        + " systems, as long as it has room for each one.")
                .paragraph("The new system becomes the one the computer boots by default. Installing a system that"
                        + " the disk already carries does not add a second copy: the setup rewrites the system's boot"
                        + " loader, which is how a damaged system is repaired.")
                .paragraph("A server setting can turn the boot menu off, so the computer boots its default system at"
                        + " once, with no list. The setting is boot.show_boot_menu.")
                .define("Boot manager", "The program that lists the systems on a computer, so you can pick one to"
                        + " start.")
                .define("Midsoft Boot Manager", "The list of systems that a Frames machine shows when it starts, so"
                        + " you can pick one.")
                .define("Boot order", "The order the firmware tries the disks in when the computer starts. The first"
                        + " disk with a system on it starts.")
                .ifSomethingGoesWrong("The computer starts into the wrong system.",
                        "Pick the system you want at the Midsoft Boot Manager, or change the boot order in the"
                                + " firmware ([](jsc:firmware)).",
                        "The screen says \"The target disk has no room for\", then the name of the second system"
                                + " and the megabytes it needs.",
                        "Install it on another disk with room, or on a bigger disk ([](jsc:disks)).",
                        "The computer starts at once, with no list, although it has two systems.",
                        "The server's boot menu setting is off. Turn it on, with the setting boot.show_boot_menu.")
                .seeAlso("jsc:midsoft_boot_manager", "jsc:installing", "jsc:firmware")
                .register();
    }

    private static void midsoftBootManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("midsoft_boot_manager", section).titled("The Midsoft Boot Manager")
                .icon(ComputingModule.CD_ROM)
                .paragraph("The Midsoft Boot Manager is the list that a Frames machine shows when it starts. It shows"
                        + " up only when there is a choice, which means two or more entries. With one system, the"
                        + " machine starts that system at once.")
                .paragraph("The list names each system with its plain name. The other systems also show the number of"
                        + " their disk in brackets. The system the computer is running is the default: if you press"
                        + " nothing, the countdown ends and that system starts.")
                .table("The keys in the list", "Key", "What it does")
                .row(GuideCell.code("Up"), GuideCell.words("Moves the cursor up the list."))
                .row(GuideCell.code("Down"), GuideCell.words("Moves the cursor down the list."))
                .row(GuideCell.code("Enter"), GuideCell.words("Starts the system under the cursor."))
                .row(GuideCell.code("Esc"), GuideCell.words("Leaves the monitor, and the list keeps its place for"
                        + " the next opening."))
                .paragraph("The countdown lasts 5 seconds, which is 100 ticks ([](jsc:units_and_ticks)). The first key"
                        + " you press stops the countdown, and the list then waits for you. Any key counts, not only"
                        + " the arrows and Enter.")
                .define("Tick", "One beat of the game: 20 ticks make a second.")
                .paragraph("On Frames 10 and 11 the list ends with \"Change firmware settings\", which opens the"
                        + " firmware ([](jsc:firmware)). On Frames 95, XP and 7 the list has no such entry. The number"
                        + " keys do not choose from this list; the arrows do, and the list offers no restart.")
                .ifSomethingGoesWrong("The screen says \"kickmgr is missing\", and the computer will not start that"
                                + " system.",
                        "The manager's file is gone from the system's folder, Frames/kickmgr.sys. Install the system"
                                + " again on the same disk to repair it ([](jsc:two_systems)).",
                        "The countdown starts the system you did not want.",
                        "Press a key early in the countdown to stop it, then choose with the arrows and Enter.",
                        "The list does not appear, although two systems are installed.",
                        "The server's boot menu setting is off. Turn it on, with the setting boot.show_boot_menu.")
                .seeAlso("jsc:two_systems", "jsc:firmware", "jsc:units_and_ticks")
                .register();
    }

    private static void framesMemory(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_memory", section).titled("Memory and the Frames editions").icon(ComputingModule.MONITOR)
                .paragraph("Setup does not refuse a machine for having little memory. Its check page shows your"
                        + " Memory line, and a machine with little memory can still take a Frames install. It then"
                        + " runs tight and boots slowly, as the section on tight RAM explains below.")
                .paragraph("The system keeps its own RAM first, before anything else: 32 MB for Frames 95, 128 MB for"
                        + " XP, 1 GB for 7, 3 GB for 10 and 8 GB for Frames 11 ([](jsc:frames_which_machine)). Every"
                        + " window you open takes memory too: its program's own RAM.")
                .paragraph("What a program holds grows with the era of the machine it runs on, because software grew"
                        + " with the machines it was written for. The figure a program gives is what it holds on a"
                        + " Legacy machine. It holds a sixteenth of that on a Vintage machine, 8 times as much on a"
                        + " Transition machine, 32 times on a Standard machine and 128 times on an Advanced machine. A"
                        + " program that comes with the system holds 16 MB on a Legacy machine. A typical machine of"
                        + " each era, with two memory sticks of the middle size, holds its system and about eight"
                        + " programs.")
                .paragraph("Free memory is the machine's total minus everything held. When a program needs more than"
                        + " is free, every Frames edition crashes: it does not refuse the program. This is true for a"
                        + " window, for a program started at the prompt and for a service. The newer editions warn"
                        + " first, as the table shows.")
                .table("What happens as the memory fills", "Edition", "Kernel", "What happens")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Cooperative"), GuideCell.words("The system"
                        + " crashes, with no warning."))
                .row(GuideCell.words("Frames XP, 7, 10 and 11"), GuideCell.words("Preemptive"),
                        GuideCell.words("A balloon warns once the memory passes nine tenths. Then the system"
                                + " crashes on the program that does not fit."))
                .subheading("The warning")
                .paragraph("On Frames XP, 7, 10 and 11, the window that takes the memory past nine tenths opens with"
                        + " a balloon that says \"Low on memory\". Its text asks you to close some programs, and names"
                        + " the three that hold the most, with what each holds.")
                .subheading("The crash")
                .paragraph("Each edition goes down on its own page. Frames 95 shows \"A fatal exception has"
                        + " occurred.\". XP and 7 show a stop error named MEMORY_MANAGEMENT. Frames 10 and 11 show a"
                        + " sad face, a code you can scan, and the stop code MEMORY_MANAGEMENT.")
                .paragraph("The page is on every monitor that shows the computer, and keys and clicks do nothing"
                        + " while it stands. The programs the computer ran stop. The page stays for 4.2 seconds on"
                        + " Frames 95, 6 seconds on XP, 8 seconds on 7 and 10 seconds on 10 and 11. Then the computer"
                        + " starts over from its self-test, into an empty session.")
                .subheading("The red page")
                .paragraph("A part taken out of a running computer brings the system down too: a stick of memory, an"
                        + " expansion card, or the disk the system runs from. Frames shows the same page in red, and"
                        + " Frames 10 and 11 name the part. If the computer still has what it needs to run, it starts"
                        + " over after the page. If it does not, the monitor goes dark. Taking out the processor, the"
                        + " board or the power supply switches the computer off at once, with no page.")
                .paragraph("Graphics programs also need video memory. When a window needs"
                        + " more than is free, the balloon says \"Not enough video memory\" and asks you to turn off a"
                        + " monitor or close a graphics program.")
                .subheading("Tight RAM and the boot time")
                .paragraph("A machine is tight for a system when it holds less than twice the system's own RAM. A"
                        + " tight boot takes one and a half times as long. You can see the limit for each edition in"
                        + " the table.")
                .table("Memory below which each edition is tight", "Edition", "Tight below")
                .row(GuideCell.words("Frames 95"), GuideCell.amount(64, "MB"))
                .row(GuideCell.words("Frames XP"), GuideCell.amount(256, "MB"))
                .row(GuideCell.words("Frames 7"), GuideCell.amount(2048, "MB"))
                .row(GuideCell.words("Frames 10"), GuideCell.amount(6144, "MB"))
                .row(GuideCell.words("Frames 11"), GuideCell.amount(16384, "MB"))
                .paragraph("A boot takes about one second, plus the system's footprint in megabytes divided by (160"
                        + " times the disk's speed times the machine's era factor). The disk's speed is 1 for a hard"
                        + " disk, 4 for a solid-state drive (SSD) and 16 for an NVMe drive. The era factor is 2 for"
                        + " Legacy, 3 for Transition, 4 for Standard and 8 for Advanced. The result is at least 2"
                        + " seconds and at most 20 seconds.")
                .paragraph("With enough RAM, on a hard disk, Frames XP on a Legacy machine boots in about 6 seconds,"
                        + " and Frames 11 on an Advanced machine in about 17 seconds. Frames 7 on a Transition machine"
                        + " and"
                        + " Frames 10 on a Standard machine reach the 20-second limit.")
                .define("Kernel", "The part of the system that runs the programs and switches between them.")
                .define("Cooperative kernel", "A kernel in which each program must give the processor back by"
                        + " itself, because nothing forces it to.")
                .define("Preemptive kernel", "A kernel that switches programs by force, so that one program cannot"
                        + " hold the processor.")
                .define("Balloon", "The small notice that appears in the notification area of the desktop.")
                .define("Tight RAM", "The state of a machine that holds less than twice its system's own RAM.")
                .ifSomethingGoesWrong("The computer reboots after a blue crash page.",
                        "A program needed more memory than was free. Close a program before you open another one,"
                                + " or add RAM ([](jsc:memory)).",
                        "A balloon says \"Low on memory\".",
                        "The memory is past nine tenths. Close a program, or add RAM to the computer"
                                + " ([](jsc:memory)).",
                        "The crash page is red.",
                        "A part was taken out while the computer ran. Put it back, and switch the computer on again"
                                + " if its monitor went dark.",
                        "A balloon says \"Not enough video memory\".",
                        "Turn off a monitor, or close a graphics program.",
                        "The computer takes a long time to start.",
                        "The machine is tight on RAM. Add RAM until it holds at least twice the system's own RAM,"
                                + " using the table above.")
                .seeAlso("jsc:memory", "jsc:frames_which_machine")
                .register();
    }
}
