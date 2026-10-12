/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the Frames Resource Kit's chapter on the five editions of Frames, from Frames 95 to Frames 11: what
 * each edition needs, how it is installed, and what its desktop, its help and its settings look like, with the
 * refusals a player meets. The overview page holds the table of the family, and each edition keeps its own page.
 */
final class FramesEditionsPages {

    private FramesEditionsPages() {
    }

    /** Declares the section and the entries of the editions of Frames, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("editions").titled("The editions")
                .in(chapter).register();
        editionsCompared(guide, section);
        frames95(guide, section);
        framesXp(guide, section);
        frames7(guide, section);
        frames10(guide, section);
        frames11(guide, section);
    }

    private static void editionsCompared(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("editions_compared", section).titled("The editions side by side")
                .paragraph("Frames is one family of five editions. An edition is one version of Frames, with its own"
                        + " installer, its own desktop and its own built-in programs. The editions have an order,"
                        + " from Frames 95, the oldest, to Frames 11, the newest.")
                .paragraph("Each edition has a family rank: its place in the family's order, counted from one. A"
                        + " program can ask for a version by its rank. A program that needs \"Frames XP or newer\""
                        + " runs on XP, 7, 10 and 11, and it refuses Frames 95.")
                .paragraph("Every edition needs a computer of its own era or newer. Frames 95 and XP need Legacy,"
                        + " Frames 7 needs Transition, Frames 10 needs Standard and Frames 11 needs Advanced. Each"
                        + " edition also runs on every computer of a newer era. A newer edition does not go on an"
                        + " older computer, and the installer tells you so ([](jsc:the_systems)).")
                .paragraph("The install medium is the item you put in a drive to install an edition. Frames 95 and XP"
                        + " install from a compact disc (CD), Frames 7 from a digital versatile disc (DVD), and Frames"
                        + " 10 and 11 from a universal serial bus (USB) stick, which goes in the"
                        + " [Dock Station](jsc:dock_station).")
                .table("The five editions", "Edition", "Family rank", "Minimum era", "Install medium")
                .row(GuideCell.words("Frames 95"), GuideCell.words("1"), GuideCell.words("Legacy"),
                        GuideCell.words("CD, in the CD Drive"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("2"), GuideCell.words("Legacy"),
                        GuideCell.words("CD, in the CD Drive"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("3"), GuideCell.words("Transition"),
                        GuideCell.words("DVD, in the DVD Drive"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("4"), GuideCell.words("Standard"),
                        GuideCell.words("USB stick, in the Dock Station"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("5"), GuideCell.words("Advanced"),
                        GuideCell.words("USB stick, in the Dock Station"))
                .table("What each edition takes", "Edition", "Footprint", "Random-access memory (RAM) held",
                        "Items on its era's disk")
                .row(GuideCell.words("Frames 95"), GuideCell.amount(48, "MB"), GuideCell.amount(32, "MB"),
                        GuideCell.words("3 items on a Legacy disk"))
                .row(GuideCell.words("Frames XP"), GuideCell.amount(1536, "MB"), GuideCell.amount(128, "MB"),
                        GuideCell.words("96 items on a Legacy disk"))
                .row(GuideCell.words("Frames 7"), GuideCell.amount(16384, "MB"), GuideCell.amount(1024, "MB"),
                        GuideCell.words("64 items on a Transition disk"))
                .row(GuideCell.words("Frames 10"), GuideCell.amount(16384, "MB"), GuideCell.amount(3072, "MB"),
                        GuideCell.words("64 items on a Standard disk"))
                .row(GuideCell.words("Frames 11"), GuideCell.amount(20480, "MB"), GuideCell.amount(8192, "MB"),
                        GuideCell.words("80 items on an Advanced disk"))
                .paragraph("The footprint is the room a system takes on a disk once it is installed. A disk counts"
                        + " its room in items, and an item is 1 MB on a Vintage disk, 16 MB on a Legacy disk and"
                        + " 256 MB from the Transition on. So Frames 95 takes 3 items of a Legacy disk.")
                .paragraph("The RAM held column is the memory that each system keeps for itself before any program"
                        + " opens.")
                .define("Era", "A generation of hardware, such as Legacy or Standard.")
                .define("Family rank", "An edition's place in its family's order, counted from one.")
                .define("Footprint", "The room a system takes on a disk once it is installed, counted in the disk's"
                        + " items.")
                .ifSomethingGoesWrong(
                        "The installer says \"Frames 10 needs Standard era hardware or newer; this machine is Legacy"
                                + " era.\"",
                        "The computer is older than the edition. Use a computer of the era the message names, or"
                                + " install an older edition ([](jsc:installing)).",
                        "The installer says the target disk has no room for the edition, and gives the megabytes it"
                                + " needs.",
                        "Use a larger disk. A Legacy disk holds Frames 95 in 3 items, and Frames 11 needs 80 items"
                                + " of an Advanced disk.",
                        "The installer says \"No disk is installed to put Frames 11 on.\"",
                        "Install a disk in the computer first ([](jsc:disks)).",
                        "The installer says \"The system on the medium is not known to this machine.\"",
                        "Each install medium belongs to one system. Use the medium made for the edition you want"
                                + " ([](jsc:installing)).")
                .seeAlso("jsc:installing", "jsc:eras", "jsc:midsoft_systems")
                .register();
    }

    private static void frames95(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_95", section).titled("Frames 95")
                .paragraph("Frames 95 is the oldest edition and the first of the family. It needs a Legacy computer or"
                        + " newer, and it installs from a compact disc (CD) in the CD Drive.")
                .paragraph("Its look is the classic one: a grey taskbar with bevelled edges, and a"
                        + " Start list in a band down one side of the screen.")
                .paragraph("Frames 95 is cooperative. Each program keeps the processor until it gives it up, and the"
                        + " system does not take it back. Frames XP and the later editions are preemptive instead:"
                        + " the system takes the processor back when a program's turn is over.")
                .define("Cooperative multitasking", "A way of running programs in which each program keeps the"
                        + " processor until it gives it up.")
                .table("Frames 95 at a glance")
                .property("Minimum era", "Legacy")
                .property("Install medium", "CD, in the CD Drive")
                .amount("Footprint", 48, "MB")
                .amount("RAM held", 32, "MB")
                .property("Version line", "Frames 4.00.950")
                .subheading("Installing Frames 95")
                .steps("Put the Frames 95 CD in the CD Drive, and start the computer from it. If the computer does"
                        + " not start from it, open the Boot Menu ([](jsc:boot_menu)).",
                        "Wait while the installer checks the computer: \"Setup is checking this computer"
                                + " before it starts.\"",
                        "Choose a disk on the page \"Choose a Disk\".",
                        "Give the computer a name on the page \"Computer Name\".",
                        "Wait on the page \"Copying Files\" until the copy is done.",
                        "Wait for \"Setup is finished.\"")
                .paragraph("The installer is a grey wizard, with a picture down one side, that moves through those"
                        + " pages in order.")
                .paragraph("Frames 95 names its help program Help. Its window is called \"Help Topics: Frames Help\""
                        + " and has three tabs: Contents, Index and Find. A topic opens in a window called \"Frames"
                        + " Help\".")
                .paragraph("On this edition the Device Manager is called System, and it sits inside the System"
                        + " dialog. Its Task Manager is a Close Program box.")
                .paragraph("The visual effects page has the heading \"Visual effects\" and one check box: \"Animate"
                        + " windows when minimizing and restoring\".")
                .paragraph("When Frames 95 starts it plays \"Frames 95 chimes\", and when something goes wrong it"
                        + " plays \"Frames 95 error\". It plays no sound when a device connects or disconnects.")
                .paragraph("On a new computer, Frames 95 does not say \"Hi.\": it shows its own Welcome. When it"
                        + " starts,"
                        + " the screen shows a sky with the logo over the horizon, and a bar runs along the foot.")
                .ifSomethingGoesWrong(
                        "The installer says \"Frames 95 needs Legacy era hardware or newer; this machine is Vintage"
                                + " era.\"",
                        "A Vintage computer cannot run Frames 95, so if you have one, use a Legacy computer or newer"
                                + " ([](jsc:eras)).",
                        "A program says \"Automation Manager needs Frames XP or newer. This computer runs Frames 95.\"",
                        "Run that program on a computer with Frames XP or newer ([](jsc:frames_xp)).")
                .seeAlso("jsc:frames_xp", "jsc:the_systems")
                .register();
    }

    private static void framesXp(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_xp", section).titled("Frames XP")
                .paragraph("Frames XP is the second edition. It keeps the Legacy era, so a Legacy computer runs it, but"
                        + " it asks for far more room: its footprint is 1,536 MB, which is 96 items on a Legacy"
                        + " disk.")
                .paragraph("Frames XP is preemptive. The system takes the processor from a program when that program's"
                        + " turn is over, so one program does not hold the processor from the others.")
                .define("Preemptive multitasking", "A way of running programs in which the system takes the processor"
                        + " from a program when its turn is over.")
                .paragraph("Its look is the Luna look: a taskbar along the bottom of the screen and a Start menu in two"
                        + " columns.")
                .table("Frames XP at a glance")
                .property("Minimum era", "Legacy")
                .property("Install medium", "CD, in the CD Drive")
                .amount("Footprint", 1536, "MB")
                .amount("RAM held", 128, "MB")
                .property("Version line", "Frames NT 5.1")
                .subheading("Installing Frames XP")
                .steps("Put the Frames XP CD in the CD Drive, and start the computer from it. If the computer does not"
                        + " start from it, open the Boot Menu ([](jsc:boot_menu)).",
                        "On the page \"The following list shows the disks on this computer.\", choose the disk.",
                        "Wait on the copying page, which says \"Please wait while Setup copies files to the disk.\"",
                        "Give the computer a name on the page \"Computer Name\", where the side panel shows"
                                + " \"Collecting information\".",
                        "Wait while the side panel shows \"Installing Frames\" and then \"Finalizing installation\".",
                        "Wait for \"Setup is finished.\"")
                .paragraph("The copy happens in two stages: the first before the name, and the second after it. The"
                        + " installer changes shape between them, and the second stage shows its steps in a side"
                        + " panel.")
                .paragraph("Frames XP calls its help program Help and Support. Its window is the Help and Support"
                        + " Center, with one pane called \"Technical Reference\".")
                .paragraph("Its Task Manager has four tabs: Applications, Processes, Performance and Networking, with a"
                        + " menu bar and a status bar. Its Device Manager is a console window: the views are in the"
                        + " View menu, and a right-click opens its actions.")
                .paragraph("The visual effects settings are in Performance Options, which you reach from System. The"
                        + " page has the heading \"Visual effects\", four presets, and four checks. The presets are"
                        + " \"Let Frames choose what's best for my computer\", \"Adjust for best appearance\","
                        + " \"Adjust for best performance\" and \"Custom:\".")
                .paragraph("The checks are \"Animate windows when minimizing and maximizing\", \"Fade or slide menus"
                        + " into view\", \"Fade or slide ToolTips into view\" and \"Use drop shadows for icon labels on"
                        + " the desktop\".")
                .paragraph("On a new computer, Frames XP greets you with a balloon notice, a small pop-up from the"
                        + " taskbar area, instead of a window. It plays \"Frames XP chimes\" when it starts and"
                        + " \"Frames XP error\" when something goes wrong.")
                .paragraph("Its splash screen is black, with the logo above the middle and three blocks in a trough.")
                .ifSomethingGoesWrong(
                        "The installer says \"Frames XP needs Legacy era hardware or newer; this machine is Vintage"
                                + " era.\"",
                        "A Vintage computer cannot run Frames XP. Use a Legacy computer or newer ([](jsc:eras)).",
                        "The installer says that the target disk has no room for Frames XP, and gives the megabytes"
                                + " it needs.",
                        "Use a Legacy disk with room for 96 items, which is what Frames XP takes.")
                .seeAlso("jsc:frames_95", "jsc:frames_7")
                .register();
    }

    private static void frames7(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_7", section).titled("Frames 7")
                .paragraph("Frames 7 is the third edition. You need a Transition computer or newer, and it installs"
                        + " from"
                        + " a digital versatile disc (DVD) in the DVD Drive. Its footprint is 16,384 MB, which is 64"
                        + " items on a Transition disk.")
                .paragraph("Its look is the glass look. The taskbar is a superbar, and its icons have no titles. Start"
                        + " is a round orb, and a show-desktop button sits at the right edge of the taskbar.")
                .define("Superbar", "A taskbar made of icons with no titles, as on Frames 7.")
                .paragraph("The Start menu has two columns, and one of them is a glass panel of places. On Frames 7 the"
                        + " program This PC is called Computer, and the Settings program is called Control Panel.")
                .table("Frames 7 at a glance")
                .property("Minimum era", "Transition")
                .property("Install medium", "DVD, in the DVD Drive")
                .amount("Footprint", 16384, "MB")
                .amount("RAM held", 1024, "MB")
                .property("Version line", "Frames NT 6.1")
                .subheading("Installing Frames 7")
                .steps("Put the Frames 7 DVD in the DVD Drive, and start the computer from it. If the computer does not"
                        + " start from it, open the Boot Menu ([](jsc:boot_menu)).",
                        "Choose a disk on the disk page.",
                        "Wait while the installer goes through \"Copying Frames files\", \"Expanding Frames files\","
                                + " \"Installing features\", \"Installing updates\" and \"Completing installation\".",
                        "Type a name for the computer on the page that says \"Choose a name for your computer to"
                                + " distinguish it on the network.\"",
                        "Wait until the installer finishes.")
                .paragraph("Frames 7 calls its help program Help and Support. Its window is called \"Frames Help and"
                        + " Support\" and has a \"Search Help\" box.")
                .paragraph("Its Task Manager has the four tabs of Frames XP, and its Device Manager is the console of"
                        + " Frames XP, in glass.")
                .paragraph("Its Performance Options page is reached from System, as on Frames XP, with the same visual"
                        + " effects and one more check: \"Enable transparent glass\". Under the heading \"Ease of"
                        + " Access: make the computer easier to see\" is a switch, \"Turn off all unnecessary"
                        + " animations (when possible)\".")
                .paragraph("On a new computer, Frames 7 shows its own Welcome, not \"Hi.\". It plays"
                        + " \"Frames 7 chimes\""
                        + " when it starts. Its error sound is Frames XP's, \"Frames XP error\".")
                .paragraph("Its splash screen is black: four coloured lights sweep in and close into a waving flag,"
                        + " with"
                        + " the name of the system under it.")
                .ifSomethingGoesWrong(
                        "The installer says \"Frames 7 needs Transition era hardware or newer; this machine is Legacy"
                                + " era.\"",
                        "A Legacy computer cannot run Frames 7. Frames XP runs on it ([](jsc:frames_xp)).")
                .seeAlso("jsc:frames_xp", "jsc:frames_10")
                .register();
    }

    private static void frames10(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_10", section).titled("Frames 10")
                .paragraph("Frames 10 is the fourth edition. You need a Standard computer or newer, and it installs"
                        + " from"
                        + " a USB stick that goes in the [Dock Station](jsc:dock_station). Its footprint is 16,384 MB,"
                        + " which is 64 items on a Standard disk.")
                .paragraph("Its look is Metro: a dark taskbar with a search box and a task view beside the Start"
                        + " button."
                        + " Start is a list with live tiles, and the Action Center sits at the right edge of the"
                        + " screen.")
                .define("Live tile", "A square in the Start list that can show a changing value.")
                .define("Task view", "The button that shows every open window.")
                .define("Action Center", "The panel at the right edge of the screen that gathers the notices.")
                .paragraph("The file program is called File Explorer. Help is called Get Help, and its window is called"
                        + " Get Help too.")
                .table("Frames 10 at a glance")
                .property("Minimum era", "Standard")
                .property("Install medium", "USB stick, in the Dock Station")
                .amount("Footprint", 16384, "MB")
                .amount("RAM held", 3072, "MB")
                .property("Version line", "Frames NT 10.0")
                .subheading("Installing Frames 10")
                .steps("Put the Frames 10 install stick in the Dock Station ([](jsc:dock_station)).",
                        "Start the computer from the stick. If the computer does not start from it, open the Boot Menu"
                                + " ([](jsc:boot_menu)).",
                        "Choose a disk on the disk page.",
                        "Wait while the installer goes through \"Copying Frames files\", \"Getting files ready for"
                                + " installation\", \"Installing features\","
                                + " \"Installing updates\" and \"Finishing up\".",
                        "Answer the question \"Who's going to use this PC?\".",
                        "Wait until the installer finishes.")
                .paragraph("The installer is a white window with a thin blue border over a dark ground. When you reach"
                        + " the set-up that follows, it fills the screen in blue.")
                .paragraph("Its Task Manager is the modern form, with the pages Processes, Performance, Services,"
                        + " Storage, Network and GPU. The GPU page is described in [Frames 11](jsc:frames_11).")
                .paragraph("Its Device Manager is flat: the modern form that Frames 11 kept.")
                .paragraph("Personalize > Effects holds the visual effects. Its note reads"
                        + " \"How Frames looks and moves."
                        + " A weak graphics card turns both off by itself.\" The page has two switches: \"Transparency"
                        + " effects\" and \"Play animations in Frames\".")
                .paragraph("On a new computer, Frames 10 says \"Hi.\", the first edition of the family to do so."
                        + " It plays"
                        + " \"Frames 10 chimes\" when it starts.")
                .paragraph("Its splash screen is black, with a window of four panes in blue, and a ring of dots chases"
                        + " round below it. It has no error sound of its own: when something goes wrong it plays the"
                        + " Frames 11 error sound, \"Frames 11 error\".")
                .ifSomethingGoesWrong(
                        "The installer says \"Frames 10 needs Standard era hardware or newer; this machine is Legacy"
                                + " era.\"",
                        "A Legacy computer cannot run Frames 10. Frames XP runs on it ([](jsc:frames_xp)).",
                        "The installer says \"Frames 10 needs Standard era hardware or newer; this machine is"
                                + " Transition era.\"",
                        "A Transition computer cannot run Frames 10. Frames 7 runs on it ([](jsc:frames_7)).")
                .seeAlso("jsc:frames_7", "jsc:frames_11", "jsc:dock_station")
                .register();
    }

    private static void frames11(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_11", section).titled("Frames 11")
                .paragraph("Frames 11 is the newest edition, the fifth. It needs an Advanced computer, the newest era"
                        + " with parts, and it needs the most room of the family: 20,480 MB, which is 80 items on an"
                        + " Advanced disk.")
                .paragraph("It installs from a USB stick in the [Dock Station](jsc:dock_station), and it keeps"
                        + " 8,192 MB of RAM for itself.")
                .table("Frames 11 at a glance")
                .property("Minimum era", "Advanced")
                .property("Install medium", "USB stick, in the Dock Station")
                .amount("Footprint", 20480, "MB")
                .amount("RAM held", 8192, "MB")
                .property("Version line", "Frames NT 10.0")
                .subheading("Installing Frames 11")
                .steps("Put the Frames 11 USB stick in the Dock Station, and start the computer from it. If the"
                        + " computer does not start from it, open the Boot Menu ([](jsc:boot_menu)).",
                        "On the page \"Where do you want to install Frames 11?\", choose a disk.",
                        "On the page \"Name this PC\", give the computer a name.",
                        "Wait while the copy shows \"Copying files\", \"Creating the system folders\", \"Adding the"
                                + " built-in programs\" and \"Setting up the boot entry\" in turn, under the caption"
                                + " \"Installing Frames 11\".",
                        "Wait until the installer finishes.")
                .paragraph("The installer's pages are pale cards, with one question to a page.")
                .paragraph("Its look is Modern: a dark taskbar, centred along the bottom of the screen, and a Start"
                        + " menu"
                        + " that floats above it, with a search box and a pinned grid of programs.")
                .paragraph("The file program is called File Explorer, as on Frames 10.")
                .paragraph("Its Device Manager has the flat modern form. The visual effects are under Personalize >"
                        + " Visual effects, with one switch, \"Animation effects\". Its note reads \"Windows open,"
                        + " close and minimize with motion, flyouts slide\".")
                .paragraph("On a new computer, Frames 11 says \"Hi.\", as Frames 10 does. It plays \"Frames 11 chimes\""
                        + " when it starts and \"Frames 11 error\" when something goes wrong. Its splash screen is near"
                        + " black, with the maker's mark in the middle and a ring of dots turning below.")
                .subheading("The Megashell")
                .paragraph("Frames 11 has its own shell, under the name Megashell. Frames 95, XP, 7 and 10 call it"
                        + " Command Prompt, and on Frames 11 its window and its name on the desktop are Megashell.")
                .define("Megashell", "The shell of Frames 11: the program that takes your commands, under its own name"
                        + " on that edition.")
                .paragraph("For the commands you type, see [The Command Prompt](jsc:command_prompt). Help on this"
                        + " edition"
                        + " opens the Get Help window ([](jsc:help_on_a_computer)).")
                .subheading("Edge snapping")
                .paragraph("Edge snapping tiles a window against an edge of the screen. It works on"
                        + " Frames 11 and on the KDE, GNOME and Cinnamon desktops of the Linux systems"
                        + " ([](jsc:unix_desktops)). Frames 95, XP, 7 and 10 do not snap.")
                .define("Edge snapping", "Tiling a window against an edge of the screen when you let go of it there.")
                .define("Work area", "The part of the screen that the taskbar does not cover.")
                .paragraph("Where you let go decides what the window does. A window within 4 pixels of an edge changes"
                        + " its size and place:")
                .table("Edges and what they do", "Where you let go", "What the window does")
                .row(GuideCell.words("Within 4 pixels of the top of the work area"),
                        GuideCell.words("The window maximises, and fills the work area."))
                .row(GuideCell.words("Within 4 pixels of the left edge of the screen"),
                        GuideCell.words("The window fills the left half of the work area."))
                .row(GuideCell.words("Within 4 pixels of the right edge of the screen"),
                        GuideCell.words("The window fills the right half of the work area."))
                .row(GuideCell.words("Anywhere else"), GuideCell.words("The window stays where you let it go."))
                .paragraph("Snapping happens when you let go, not while you drag. Keep the window at the edge, and then"
                        + " release the mouse button.")
                .paragraph("A window that you snap keeps its old place. When you restore it from maximised, it returns"
                        + " to where it was before the snap.")
                .ifSomethingGoesWrong(
                        "A window let go in the middle of the screen does not tile.",
                        "Only the edges in the table snap a window. Drag the window to one of them, and let go.",
                        "A window does not tile on Frames 10.",
                        "Frames 10 has no edge snapping. Move and resize the window by hand.")
                .subheading("The GPU page")
                .paragraph("The GPU page is in Task Manager, and it shows the graphics processing unit (GPU) of the"
                        + " computer: the card that draws the picture for the monitors. Frames 10 has the same page."
                        + " It shows how busy the card is, and which monitors and windows use its video memory.")
                .define("Video memory", "The memory on a graphics card, which the monitors and the graphics windows"
                        + " use.")
                .table("What the GPU page shows", "Part of the page", "What it tells you")
                .row(GuideCell.words("Title and card name"),
                        GuideCell.words("GPU, and the card's name on the right in small text."))
                .row(GuideCell.words("3D graph"), GuideCell.words("How busy the card is, from 0 to 100."))
                .row(GuideCell.words("Memory graph"),
                        GuideCell.words("How full the memory is: \"Dedicated GPU memory usage\" for a card, or"
                                + " \"Shared GPU memory usage\" for integrated graphics."))
                .row(GuideCell.words("Utilization"), GuideCell.words("How busy the card is, as a percent."))
                .row(GuideCell.words("Dedicated memory, or Shared memory"),
                        GuideCell.words("The memory in use, out of the total."))
                .row(GuideCell.words("Cores"),
                        GuideCell.words("The number of cores, and the clock speed in MHz or GHz."))
                .row(GuideCell.words("Slot"),
                        GuideCell.words("The PCI Express (PCIe) slot the card sits in, or \"On the processor\" for"
                                + " integrated graphics."))
                .row(GuideCell.words("Used by"),
                        GuideCell.words("One row for each monitor or graphics window that holds video memory, with"
                                + " its size."))
                .paragraph("The page shows the first graphics card in the computer's build. With no card, it shows the"
                        + " graphics of the processor, if the processor has any, and the memory is then shared with the"
                        + " computer.")
                .paragraph("The utilisation percentage grows with two things. The memory fill adds up to 50 percent,"
                        + " and each open graphics window adds 20 percent. It is 0 when no video memory is in"
                        + " use, and it never goes above 100 percent.")
                .paragraph("A lit monitor holds video memory in blocks, and a graphics window takes a"
                        + " quarter of a monitor block, or a whole block while it fills the screen. A block's size"
                        + " depends on the era of the computer.")
                .table("Video memory by era", "Era", "One monitor block", "Window", "Full-screen window")
                .row(GuideCell.words("Vintage"), GuideCell.amount(256, "KB"), GuideCell.amount(64, "KB"),
                        GuideCell.amount(256, "KB"))
                .row(GuideCell.words("Legacy"), GuideCell.amount(4, "MB"), GuideCell.amount(1, "MB"),
                        GuideCell.amount(4, "MB"))
                .row(GuideCell.words("Transition"), GuideCell.amount(16, "MB"), GuideCell.amount(4, "MB"),
                        GuideCell.amount(16, "MB"))
                .row(GuideCell.words("Standard"), GuideCell.amount(64, "MB"), GuideCell.amount(16, "MB"),
                        GuideCell.amount(64, "MB"))
                .row(GuideCell.words("Advanced"), GuideCell.amount(256, "MB"), GuideCell.amount(64, "MB"),
                        GuideCell.amount(256, "MB"))
                .paragraph("These sizes are estimates. The table gives the 16-colour block; a Vintage monitor"
                        + " that shows only black and white uses 64 KB per block. The page writes an amount in KB"
                        + " below 1 MB, in MB below 1 GB, and in GB above that, with one decimal place.")
                .ifSomethingGoesWrong(
                        "The monitor says \"The computer has no video output: install a graphics card\".",
                        "Install a graphics card in the computer ([](jsc:graphics_cards)).",
                        "The monitor stays dark and says \"Not enough video memory\". It lights when the card has"
                                + " room.",
                        "Turn off a monitor, or use a card with more memory ([](jsc:graphics_cards)).",
                        "A graphics window does not open, and the desktop says \"Not enough video memory\".",
                        "Turn off a monitor or close a graphics program.",
                        "The GPU page says \"This computer has no graphics.\"",
                        "The computer has no graphics card and no graphics on its processor. Install a graphics card"
                                + " ([](jsc:graphics_cards)).")
                .seeAlso("jsc:graphics_cards", "jsc:monitors", "jsc:frames_10")
                .register();
    }
}
