/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Quince's chapter of The Daemon Handbook: the six systems and the processors each runs on, installing them, the
 * desktop, the memory of the classic systems and of the later ones, their crash pages, and their programs and keg.
 */
final class QuincePages {

    private QuincePages() {
    }

    /** Declares the sections and entries of Quince's systems, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef systems = guide.section("quince_systems_section").titled("The systems")
                .in(chapter).register();
        quinceSystems(guide, systems);
        quinceInstalling(guide, systems);
        final ModGuide.SectionRef desktop = guide.section("quince_desktop_section").titled("The desktop")
                .in(chapter).register();
        quinceDesktop(guide, desktop);
        quinceMemory(guide, desktop);
        quinceCrashes(guide, desktop);
        final ModGuide.SectionRef software = guide.section("quince_software").titled("Programs").in(chapter)
                .register();
        quincePrograms(guide, software);
        quinceKeg(guide, software);
    }

    private static void quinceSystems(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_systems", section).titled("Quince's systems")
                .paragraph("Quince makes its own computers and its own systems for them. Each system runs on the"
                        + " processors it was built for, and on no other: you cannot install a system for the PowerPC"
                        + " on an Integra processor. Quince's computers are described in [](jsc:quince_computers).")
                .define("Quince System", "Quince's first system, for the Russet 128K and its 68k processor.")
                .define("Quince OS 9", "The last classic system, for the PowerPC of the PowerQ G3 and G4.")
                .define("Quince OS X", "The system that came after, for the PowerPC and for Integra processors.")
                .define("Quince OS X Server", "Quince OS X for servers, with Server Admin to run their services.")
                .define("quinceOS", "Quince's newest system, for the Q1, Q2 and Q3, Quince's own chips.")
                .table("The systems", "System", "First era", "Processors", "Disk", "Memory")
                .row(GuideCell.words("Quince System"), GuideCell.words("Vintage"), GuideCell.words("68k"),
                        GuideCell.words("1 MB"), GuideCell.words("1 MB"))
                .row(GuideCell.words("Quince OS 9"), GuideCell.words("Legacy"), GuideCell.words("PowerPC"),
                        GuideCell.words("400 MB"), GuideCell.words("32 MB"))
                .row(GuideCell.words("Quince OS X"), GuideCell.words("Legacy"), GuideCell.words("PowerPC, x86-64"),
                        GuideCell.words("2,048 MB"), GuideCell.words("128 MB"))
                .row(GuideCell.words("Quince OS X Server"), GuideCell.words("Transition"),
                        GuideCell.words("x86-64, ARM64"), GuideCell.words("4,096 MB"), GuideCell.words("256 MB"))
                .row(GuideCell.words("quinceOS"), GuideCell.words("Advanced"), GuideCell.words("ARM64"),
                        GuideCell.words("16,384 MB"), GuideCell.words("4,096 MB"))
                .paragraph("The Quince System and Quince OS 9 are the classic systems. They run on the kernel"
                        + " quince_classic, which lets one program run until it gives way to the next, and they have no"
                        + " command line at all: everything is done with the mouse. Quince OS X, its Server and"
                        + " quinceOS run on the kernel wallace, a UNIX underneath, with a Terminal and the zsh"
                        + " shell. That UNIX is why Quince's chapter is in this handbook.")
                .define("Classic system", "The Quince System or Quince OS 9: no command line, and memory given out"
                        + " in fixed blocks.")
                .paragraph("Quince OS X carries Champollion, which runs a program built for the PowerPC on an x86-64"
                        + " processor at half its speed. Quince OS X Server and quinceOS carry Champollion 2, which"
                        + " runs x86-64 programs on Quince's chips at four fifths of their speed"
                        + " ([](jsc:processors)).")
                .table("How each formats its disk", "System", "File system")
                .row(GuideCell.words("Quince System"), GuideCell.words("HFS"))
                .row(GuideCell.words("Quince OS 9"), GuideCell.words("HFS+"))
                .row(GuideCell.words("Quince OS X and its Server"), GuideCell.words("HFS+ (Journaled)"))
                .row(GuideCell.words("quinceOS"), GuideCell.words("APFS"))
                .ifSomethingGoesWrong("The installer does not offer the system.",
                        "The computer's processor is not one the system was built for. Install it on a Quince"
                                + " computer with the right processor.")
                .seeAlso("jsc:the_systems", "jsc:quince_installing", "jsc:quince_desktop")
                .register();
    }

    private static void quinceInstalling(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_installing", section).titled("Installing a Quince system")
                .paragraph("Put the system's install medium in a drive linked to the computer, then start the"
                        + " installer from the computer's firmware, as for any system ([](jsc:installing)). Quince's"
                        + " installers look like Quince's desktop: a window on the grey or blue ground.")
                .subheading("The Quince System, from three floppies")
                .paragraph("The Quince System comes on three floppy disks: Install 1, System Tools and Utilities."
                        + " Each floppy's tooltip says which of the three it is. Put the first in a Floppy Drive and"
                        + " press Install. When the installer has copied what is on it, it asks for the next one:"
                        + " take the first out, put the next in, and the copy goes on by itself.")
                .define("Easy Install", "The Quince System's installer page, which installs the whole system onto"
                        + " the disk it shows.")
                .subheading("The others, from one disc")
                .paragraph("Quince OS 9, Quince OS X and its Server come on one CD (compact disc). Their installer"
                        + " goes through its pages in order: a welcome, what to read first, the license, the disk to"
                        + " install on and the kind of installation. Press Continue on each, Agree on the license,"
                        + " and Install once you have chosen the disk.")
                .subheading("quinceOS and its recovery")
                .paragraph("On a computer with one of Quince's chips, press Options on the start-up screen. It opens"
                        + " quinceOS's recovery, which offers three things: Reinstall quinceOS puts a fresh copy of"
                        + " the system on the disk, Disk Utility checks the startup disk with First Aid, and Startup"
                        + " Disk chooses the disk the computer starts from.")
                .define("Recovery", "The utilities quinceOS opens from the start-up screen's Options.")
                .ifSomethingGoesWrong("The installer keeps asking for the next floppy.",
                        "The floppy in the drive is not the one asked for. Put in the floppy the page names.",
                        "The copy stops with the medium taken out.",
                        "Leave each medium in its drive until the installer asks for the next or the copy is done.")
                .seeAlso("jsc:quince_systems", "jsc:installing")
                .register();
    }

    private static void quinceDesktop(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_desktop", section).titled("The menu bar, the Seeker and the Dock")
                .paragraph("Every Quince system starts at its desktop. Across the top is the menu bar: the fruit's"
                        + " menu at its left, then File, Edit and View. The fruit's menu opens About This Computer."
                        + " On the classic systems it also holds a few programs, and the Special menu holds Empty"
                        + " Trash, Restart and Shut Down. From Quince OS X the fruit's menu holds System Preferences,"
                        + " Force Quit, Restart and Shut Down, and the Go and Window menus join the bar.")
                .define("Menu bar", "The bar across the top of a Quince desktop, with the fruit's menu at its"
                        + " left.")
                .paragraph("The disks and the Trash lie on the desktop. Double-click a disk to open it in the Seeker,"
                        + " Quince's file browser. Drag a file to the Trash to throw it away; emptying the Trash"
                        + " deletes it for good.")
                .define("Seeker", "Quince's file browser, where the disks, folders and files are opened.")
                .paragraph("From Quince OS X there is also the Dock along the bottom of the screen. It holds the"
                        + " programs you start most and every program that is open; click one to start it or bring"
                        + " it to the front.")
                .define("Dock", "The row of programs along the bottom of the screen, from Quince OS X.")
                .table("What each program is called", "Program", "Classic systems", "From Quince OS X")
                .row(GuideCell.words("Files"), GuideCell.words("Seeker"), GuideCell.words("Seeker"))
                .row(GuideCell.words("Editor"), GuideCell.words("TextPad"), GuideCell.words("TextPad"))
                .row(GuideCell.words("Settings"), GuideCell.words("Control Panels"),
                        GuideCell.words("System Preferences"))
                .row(GuideCell.words("Help"), GuideCell.words("Help Center"), GuideCell.words("Help Viewer"))
                .row(GuideCell.words("Command Prompt"), GuideCell.words("None"), GuideCell.words("Terminal"))
                .row(GuideCell.words("System Monitor"), GuideCell.words("None"),
                        GuideCell.words("Activity Monitor"))
                .seeAlso("jsc:quince_systems", "jsc:desktops")
                .register();
    }

    private static void quinceMemory(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_memory", section).titled("Memory on Quince's systems")
                .subheading("The classic systems: a block for each program")
                .paragraph("On the Quince System and Quince OS 9, each program you open gets a block of memory of"
                        + " its own, which it keeps while it runs. Two sizes set the block: the preferred size and the"
                        + " minimum size. The program opens in its preferred size while that much is free, in what"
                        + " is left above its minimum otherwise, and not at all below its minimum.")
                .define("Preferred size", "The block a program opens in while that much memory is free.")
                .define("Minimum size", "The smallest block a program opens in on a classic system.")
                .paragraph("With a program's window in front, choose Get Info from the File menu to change its two"
                        + " sizes. The suggested size beside them is what the program's maker suggests. About This"
                        + " Computer, in the fruit's menu, shows each open program's block as a bar, the part in use"
                        + " darker.")
                .define("Get Info", "The window that shows what a file or program is, and sets a program's memory"
                        + " on a classic system.")
                .subheading("Quince OS X and quinceOS: Force Quit")
                .paragraph("The later systems share the memory as the other systems do. When it runs out, they do"
                        + " not end anything themselves: every program pauses, and Force Quit Applications asks"
                        + " which to quit. Choose one or more and press Force Quit. Once there is room, the programs"
                        + " go on.")
                .define("Force Quit Applications", "The window that asks which programs to quit when the memory"
                        + " runs out.")
                .ifSomethingGoesWrong("A classic system says there is not enough memory to open a program.",
                        "Quit a program to free its block, or lower the program's minimum size in Get Info.",
                        "Every program on Quince OS X stops moving.",
                        "The memory ran out: choose what to quit in Force Quit Applications.")
                .seeAlso("jsc:quince_systems", "jsc:quince_crashes")
                .register();
    }

    private static void quinceCrashes(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_crashes", section).titled("When a Quince system crashes")
                .paragraph("A Quince system that crashes stops on its page and stays there until you restart the"
                        + " computer. Press Restart on the bomb, or any key; the computer then starts over from its"
                        + " self-test. What was running is lost.")
                .paragraph("The classic systems show the bomb: a box with a bomb and the words \"Sorry, a system error"
                        + " occurred.\", with the error's number below. Quince OS 9 also names the program that was"
                        + " running. The later systems show a grey page that asks you to restart, in English, French,"
                        + " German and Spanish.")
                .define("Bomb", "The box the classic systems crash onto, with the error's number.")
                .table("The bomb's numbers", "Number", "What went wrong")
                .row(GuideCell.code("25"), GuideCell.words("The memory ran out."))
                .row(GuideCell.code("02"), GuideCell.words("A part was taken out from under a running program."))
                .row(GuideCell.code("01"), GuideCell.words("Anything else."))
                .seeAlso("jsc:quince_memory")
                .register();
    }

    private static void quincePrograms(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_programs", section).titled("Quince's programs")
                .paragraph("Beside the programs every desktop has, Quince's systems come with programs of their"
                        + " own. You start each from the Dock, or from the Applications folder in the Seeker.")
                .define("qTunes", "The music player, from Quince OS 9. It plays the computer's songs with the same"
                        + " player every music program on the computer shares.")
                .define("Expedition", "The web browser, from Quince OS 9. The world has no web yet, so it shows a"
                        + " page that says so.")
                .define("Preview", "The picture viewer, from Quince OS X. Open a picture in the Seeker to see it.")
                .define("Quince Store", "The store, from Quince OS X. From the Standard era, it lists the programs"
                        + " the network's Mirror serves, and GET installs one, free.")
                .define("Server Admin", "On Quince OS X Server: the Mainframe's services, and buttons that stop and"
                        + " start the Mirror and the Automation Engine.")
                .paragraph("The classic systems take their programs from media only. The later ones take them from"
                        + " media too, and from the Standard era from the network's Mirror, with the Quince Store or"
                        + " with keg at the Terminal ([](jsc:quince_keg)).")
                .seeAlso("jsc:quince_systems", "jsc:programs")
                .register();
    }

    private static void quinceKeg(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("quince_keg", section).titled("keg, at the Terminal")
                .paragraph("With keg you install programs from the network's Mirror at the Terminal of Quince OS X,"
                        + " its Server and quinceOS. It came with the Standard era: on a computer of an older era, the"
                        + " Terminal has no keg command, and programs come from media.")
                .define("keg", "Quince's package manager, which installs the Mirror's programs at the Terminal.")
                .command(CommandGuidePages.id(Platform.QUINCE, "keg"))
                .seeAlso("jsc:quince_programs", "jsc:mirror")
                .register();
    }
}
