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
 * Declares the pages of the desktop chapter of the Frames Resource Kit: the Start menus, the taskbar, the windows
 * and the notices, then the Settings, Personalize, effects, sounds and shut-down of each Frames edition.
 */
final class FramesDesktopPages {

    private FramesDesktopPages() {
    }

    /** Declares the sections and entries of the desktop of Frames, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        workingSection(guide, chapter);
        settingsSection(guide, chapter);
    }

    private static void workingSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("desktop_working").titled("Working on the desktop")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        startMenus(guide, section);
        taskbar(guide, section);
        frameWindows(guide, section);
        notices(guide, section);
    }

    private static void settingsSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("desktop_settings").titled("Settings and looks")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        frameSettings(guide, section);
        personalize(guide, section);
        effects(guide, section);
        systemSounds(guide, section);
        shuttingDown(guide, section);
    }

    private static void startMenus(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("start_menus", section).titled("The Start menu of each edition")
                .icon(ComputingModule.MONITOR)
                .paragraph("The Start menu is where you find the programs of"
                        + " your Frames. Click the Start button on the taskbar, and the menu opens above it."
                        + " The general idea is on the page [Desktops](jsc:desktops).")
                .paragraph("Each Frames edition draws its Start menu in its own style. Every one of them lists"
                        + " your programs, and each has a power control at its foot.")
                .table("The Start menus", "Edition", "Start button", "What the menu holds")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Start, at the left corner of the taskbar"),
                        GuideCell.words("One vertical list of programs, under a side band that carries the"
                                + " name of the system."))
                .row(GuideCell.words("Frames XP"), GuideCell.words("A green pill labelled \"start\""),
                        GuideCell.words("Two columns: programs on the left, places on the right."))
                .row(GuideCell.words("Frames 7"), GuideCell.words("A round glass button at the left end"),
                        GuideCell.words("Two columns in a frame of glass: programs on the white side, places"
                                + " on the glass side."))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Start, beside the search box"),
                        GuideCell.words("A rail, a list of programs and a set of tiles."))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Centred with the program icons"),
                        GuideCell.words("A floating panel: a search box, a grid of pinned programs and a"
                                + " power button."))
                .subheading("Frames 95 and Frames XP")
                .paragraph("The menu of Frames 95 is a single vertical list, one row for each program. Below"
                        + " the programs, a row labelled \"Shut Down\" opens the"
                        + " [power controls](jsc:shutting_down).")
                .paragraph("Frames XP splits its menu into two columns. The left column lists your programs,"
                        + " and the first two are in bold, because they are pinned. The right column holds the"
                        + " places: This PC, Files, Settings and Network. The header shows your own name, and"
                        + " the footer holds \"Log Off\" and \"Turn Off Computer\".")
                .subheading("Frames 7")
                .paragraph("The menu of Frames 7 is drawn in glass. Its left column is white and lists your"
                        + " programs, with the first two in bold. A search box at the foot of that column"
                        + " filters the list as you type.")
                .paragraph("The right column is the glass itself. At its head is the picture of your account,"
                        + " and under it are the places in white: Files, Computer, Network, Settings and Device"
                        + " Manager. Frames 7 calls This PC \"Computer\". \"Shut down\" sits at the foot of the"
                        + " column.")
                .paragraph("The menu opens over the corner of the screen, level with the top of the bar. The"
                        + " round button stays in the bar beneath it.")
                .subheading("Frames 10")
                .paragraph("The menu of Frames 10 has three parts. The narrow rail at the left holds the menu's"
                        + " own button at the top. At the foot of the rail are your account, your files,"
                        + " Settings and the power button.")
                .paragraph("Beside the rail is the list. It shows the programs you used most in this session,"
                        + " up to 4 of them, under the heading \"Most used\". Then it lists every program from A"
                        + " to Z under its letter. When you type in the search box, the list keeps only what"
                        + " matches.")
                .paragraph("The tiles at the right of the menu are squares, each standing for one program. The"
                        + " tile sizes are these:")
                .table("Tile sizes", "Size", "Cells", "Menu text")
                .row(GuideCell.words("Small"), GuideCell.words("1 by 1 cell"), GuideCell.words("Resize: Small"))
                .row(GuideCell.words("Medium"), GuideCell.words("2 by 2 cells"), GuideCell.words("Resize: Medium"))
                .row(GuideCell.words("Wide"), GuideCell.words("4 by 2 cells"), GuideCell.words("Resize: Wide"))
                .paragraph("A Frames 10 machine keeps up to 24 tiles. A new machine starts with File Explorer,"
                        + " Calculator, System Monitor, Settings, Editor, Command Prompt, Paint, Minesweeper and"
                        + " Network Manager on its tiles.")
                .paragraph("Two tiles are live. The Network Manager tile reads \"Connected to a network\" or"
                        + " \"No network\". The System Monitor tile shows the processor in use, marked \"CPU\""
                        + " (central processing unit),"
                        + " and the memory in use, marked \"Memory\". A live tile turns over every 7 seconds,"
                        + " between its picture and its reading.")
                .paragraph("You arrange the tiles with the mouse. A right click on a tile unpins it or changes"
                        + " its size. A right click on a program in the list pins it, with \"Pin to Start\"."
                        + " Drag a tile onto another tile to take its place.")
                .subheading("Frames 11")
                .paragraph("Frames 11 uses a compact floating panel. It has a search box that reads \"Type here"
                        + " to search\", a grid of pinned programs under the heading \"Pinned\", and a footer."
                        + " The account is at the left of the footer, and the power button at the right. The"
                        + " grid is 5 columns wide.")
                .subheading("Using the Start menu")
                .paragraph("Whatever the edition, the search box starts empty each time you open the menu. The"
                        + " search keeps the programs whose name contains the text you typed, and capital letters"
                        + " do not matter. It takes up to 24 characters.")
                .paragraph("A left click starts the program you point at. A right click opens that program's own"
                        + " menu.")
                .steps("Click the Start button in the taskbar.",
                        "Find the program in the list, or type part of its name.",
                        "Click the program with the left button.")
                .ifSomethingGoesWrong("The menu says \"No results\" while you type.",
                        "Check the spelling, or clear the search box and look through the list.",
                        "You want a 25th tile on a Frames 10 menu.",
                        "A machine keeps up to 24 tiles. Right-click a tile, choose \"Unpin from Start\", then"
                                + " pin the new program.")
                .define("desktop", "The face a system shows after it starts: the wallpaper, the windows, the"
                        + " taskbar and the Start menu.")
                .define("Start menu", "The menu that opens from the Start button, with your programs, the places"
                        + " of the system and the power controls.")
                .define("taskbar", "The bar along the bottom of the screen, with the Start button, a button for"
                        + " each open program and the status area.")
                .define("window", "A box on the desktop that holds one open program.")
                .define("tile", "A square on the Frames 10 Start menu that stands for one program, and starts that"
                        + " program when you click it.")
                .define("pin", "Keeping a program on a tile, a square in the grid or a button on the taskbar, so"
                        + " that it stays there when the program is closed.")
                .seeAlso("jsc:taskbar", "jsc:frames_windows", "jsc:editions_compared")
                .register();
    }

    private static void taskbar(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("taskbar", section).titled("The taskbar and its menu")
                .icon(ComputingModule.MONITOR)
                .paragraph("The taskbar is the bar along the bottom of the screen. It holds the Start button, a"
                        + " button for every open program, and the status area at the right. Every Frames"
                        + " edition has a taskbar that is 24 pixels tall, but each one looks different.")
                .table("The taskbar of each edition", "Edition", "What the bar looks like")
                .row(GuideCell.words("Frames 95"),
                        GuideCell.words("A grey bevelled bar. Start is at the left corner, each open program has"
                                + " a named button, and the status area is sunken at the right."))
                .row(GuideCell.words("Frames XP"),
                        GuideCell.words("A blue gradient bar, with the green start pill and a quick launch strip"
                                + " beside it."))
                .row(GuideCell.words("Frames 7"),
                        GuideCell.words("A superbar of icons without titles. Each program is a glass button with"
                                + " its icon alone. A program with several windows shows the edge of a second"
                                + " pane behind its own."))
                .row(GuideCell.words("Frames 10"),
                        GuideCell.words("A dark bar: Start, the search box, the Task View button, one square for"
                                + " each program, then the status area and the Action Center button."))
                .row(GuideCell.words("Frames 11"),
                        GuideCell.words("A flat dark band. Start and the program icons sit together, centred."
                                + " Each icon has a small mark that shows what its program is doing. The bar shows"
                                + " no window titles."))
                .subheading("Clicking a program's button")
                .paragraph("What happens depends on the state of the program's windows.")
                .table("A click on a program's button", "You click", "What happens")
                .row(GuideCell.words("A program that is not open"), GuideCell.words("It starts."))
                .row(GuideCell.words("Its one window, in front"),
                        GuideCell.words("The window minimises, and goes down to the taskbar."))
                .row(GuideCell.words("Its one window, minimised"), GuideCell.words("The window comes back."))
                .row(GuideCell.words("Its one window, behind others"),
                        GuideCell.words("The window comes to the front."))
                .row(GuideCell.words("Several windows"),
                        GuideCell.words("A popup lists the windows of that program, so you can pick one."))
                .subheading("The menu of an open program")
                .paragraph("Click a program's button with the right button to open its menu. The items you see"
                        + " depend on its windows.")
                .table("The menu of an open program", "Item", "When it appears")
                .row(GuideCell.words("Restore all"), GuideCell.words("The program has several windows."))
                .row(GuideCell.words("Restore"), GuideCell.words("Its one window is minimised."))
                .row(GuideCell.words("Bring to front"), GuideCell.words("Its one window is not minimised."))
                .row(GuideCell.words("Minimize all"), GuideCell.words("The program has several windows."))
                .row(GuideCell.words("Minimize"), GuideCell.words("The program has one window."))
                .row(GuideCell.words("Minimize others"), GuideCell.words("The program is open."))
                .row(GuideCell.words("Maximize"), GuideCell.words("The program has one window."))
                .row(GuideCell.words("Close all windows"), GuideCell.words("The program has several windows."))
                .row(GuideCell.words("Close"), GuideCell.words("The program has one window."))
                .row(GuideCell.words("Pin to taskbar, or Unpin from taskbar"),
                        GuideCell.words("Every Frames edition. It keeps the program's button on the bar, even when"
                                + " the program is closed."))
                .paragraph("A closed program has a shorter menu: Open, and Unpin from taskbar if it is pinned.")
                .subheading("The menu of the empty bar")
                .paragraph("Right-click the empty part of the bar to open its menu. A line separates Show the"
                        + " Desktop from Task Manager.")
                .table("The menu of the empty bar", "Row", "What it does")
                .row(GuideCell.words("Cascade Windows"), GuideCell.words("Arranges the windows in a cascade."))
                .row(GuideCell.words("Show the Desktop"), GuideCell.words("Minimises every window."))
                .row(GuideCell.words("Task Manager"), GuideCell.words("Opens the Task Manager."))
                .subheading("The status area")
                .paragraph("At the right of the bar, the status area shows the network, the sound, the memory and"
                        + " the clock, and on some editions the day. The memory is random-access memory (RAM).")
                .table("The status area", "Shows", "What it reads")
                .row(GuideCell.words("The network"), GuideCell.words("\"Network connected\" or \"No network\"."))
                .row(GuideCell.words("The memory"), GuideCell.words("\"RAM\" and the amount in use."))
                .row(GuideCell.words("The clock"),
                        GuideCell.words("The time: 24-hour by default, or 12-hour if you choose it on Personalize."))
                .row(GuideCell.words("The day"),
                        GuideCell.words("On Frames 7 and Frames 10: the current in-game day, under the time,"
                                + " marked \"Day\"."))
                .subheading("The Task Manager")
                .paragraph("The [Task Manager](jsc:task_manager) lists the programs that are running. It also"
                        + " shows the processor, the memory, the network and the disks, and it ends a program"
                        + " that stops answering. Its pages are Applications, Processes, Performance, Networking,"
                        + " Services and Storage.")
                .paragraph("Frames 95 gives its Task Manager the shape of a close box. Frames XP and Frames 7 use"
                        + " tabs, and Frames 10 and Frames 11 use a modern shape with graphs.")
                .warning("Ending a program may lose unsaved work.")
                .ifSomethingGoesWrong("A window has vanished from the screen.",
                        "It is minimised. Click its button on the taskbar to bring it back.",
                        "A program has several windows, and you cannot tell which one you want.",
                        "Click the program's button, then choose the window in the popup.")
                .define("Task View", "A view on Frames 10 that dims the desktop and shows every open window as a"
                        + " card.")
                .define("Action Center", "The dark pane of Frames 10 that lists the notices and holds the quick"
                        + " actions.")
                .seeAlso("jsc:task_manager", "jsc:personalize", "jsc:frames_windows")
                .register();
    }

    private static void frameWindows(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_windows", section).titled("Windows: move, resize, minimise, maximise, snap and Task View")
                .icon(ComputingModule.MONITOR)
                .paragraph("A window is the box that holds one open program. Its title bar runs along the top, with"
                        + " the program's name and three boxes. The corner at the bottom right is the resize grip.")
                .table("The boxes of a window", "Box", "What it does")
                .row(GuideCell.words("Minimise box"),
                        GuideCell.words("The window goes down to its button on the taskbar. Click that button to"
                                + " bring the window back."))
                .row(GuideCell.words("Maximise or restore box"),
                        GuideCell.words("A maximised window fills the work area. The same box returns the window to"
                                + " its old size."))
                .row(GuideCell.words("Close box"),
                        GuideCell.words("The window closes. The program may ask you first."))
                .subheading("Moving and resizing")
                .steps("Drag the title bar to move the window.",
                        "Drag the resize grip to change the size of the window.")
                .paragraph("The window stays on the screen: its title bar cannot be dragged off the desktop. A"
                        + " program sets the smallest size its windows can take, so the window stops shrinking at"
                        + " that size.")
                .subheading("Snap, on Frames 11")
                .paragraph("On Frames 11, you can snap a window by letting it go at an edge of the screen. Let go"
                        + " with the mouse pointer at the top edge of the work area, and the window maximises. Let"
                        + " go at the left edge, and it takes the left half of the work area. Let go at the right"
                        + " edge, and it takes the right half.")
                .paragraph("Windows do not snap on Frames 95, Frames XP, Frames 7 or Frames 10.")
                .subheading("Task View, on Frames 10")
                .paragraph("On Frames 10, the Task View button on the taskbar shows every open window at once. The"
                        + " desktop dims, and each window becomes a card: its program's icon and its title, over a"
                        + " live picture of the window.")
                .paragraph("A click on a card brings that window to the front. A click anywhere else, or on the"
                        + " Task View button again, puts the view away.")
                .subheading("Showing the desktop")
                .paragraph("Frames 7 has a narrow corner at the far right of the taskbar. Click it to show the"
                        + " desktop. Every edition has the same row in the taskbar's menu, Show the Desktop, and"
                        + " both minimise every window.")
                .paragraph("Cascade Windows, in the same menu, arranges the windows in a cascade.")
                .ifSomethingGoesWrong("A window will not snap to the top edge of the screen.",
                        "Snap works on Frames 11 only. On the other editions, use the maximise box.",
                        "You look for the Task View button, and it is not on the taskbar.",
                        "Only Frames 10 has the Task View button. Frames 95, Frames XP, Frames 7 and Frames 11 have"
                                + " none.")
                .define("title bar", "The strip along the top of a window, with its name and its three boxes.")
                .define("resize grip", "The small corner at the bottom right of a window, which you drag to change"
                        + " its size.")
                .define("snap", "To let a window go at a screen edge, so that it fills half or all of the work"
                        + " area.")
                .define("work area", "The desktop minus the taskbar: the part of the screen a maximised window"
                        + " fills.")
                .seeAlso("jsc:taskbar", "jsc:frames_11")
                .register();
    }

    private static void notices(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("notices", section).titled("Notices and the Action Center")
                .icon(ComputingModule.MONITOR)
                .paragraph("A notice is a message that the desktop or a program sends you. Frames has three kinds of"
                        + " notice, and each one asks something different of you. A dialog waits for your answer."
                        + " A balloon says one thing and goes away by itself. A toast is the Frames 10 form of the"
                        + " balloon.")
                .table("The kinds of notice", "Kind", "What it does", "Where it appears")
                .row(GuideCell.words("Dialog"),
                        GuideCell.words("Waits until you answer it: an error, a question, or the Open with chooser."),
                        GuideCell.words("In the middle of the screen, over everything else."))
                .row(GuideCell.words("Balloon"),
                        GuideCell.words("Says one thing, then goes away by itself after 9 seconds."),
                        GuideCell.words("Over the status area of the taskbar, on Frames 95, Frames XP, Frames 7 and"
                                + " Frames 11."))
                .row(GuideCell.words("Toast"), GuideCell.words("The Frames 10 form of the balloon."),
                        GuideCell.words("In a corner of the screen, on Frames 10 only."))
                .subheading("Dialogs")
                .paragraph("An error dialog has the title \"Error\". A question offers the buttons \"OK\", \"Yes\" and"
                        + " \"No\", and you choose one of them. A click beside a dialog makes the computer beep, and"
                        + " the dialog stays on the screen. The Escape key closes a dialog.")
                .subheading("Balloons")
                .paragraph("A balloon plays the notify sound when it appears, unless Quiet hours is on. These are"
                        + " some of the balloons you may see:")
                .table("Some balloons", "Title", "What it tells you")
                .row(GuideCell.words("Low on memory"),
                        GuideCell.words("The memory has passed nine tenths. It names the three programs that hold"
                                + " the most, and what each holds. A program that does not fit after that brings the"
                                + " system down ([](jsc:frames_memory))."))
                .row(GuideCell.words("Not enough video memory"),
                        GuideCell.words("A program needs more video memory than the graphics card has free."))
                .row(GuideCell.words("Cannot open"),
                        GuideCell.words("No program on this computer opens the item you chose."))
                .subheading("The history of notices")
                .paragraph("Each notice goes into a list, with the newest first. The list keeps up to 12 notices. It"
                        + " lasts while the desktop is open, and the list clears when the desktop closes.")
                .subheading("The Action Center, on Frames 10")
                .paragraph("The Action Center is a dark pane down the right edge of the screen. Open it with the"
                        + " Action Center button at the far right of the taskbar, and close it with the same button."
                        + " Opening it marks the notices as read.")
                .paragraph("The list is headed \"Notifications\", with \"Clear all\" beside it, and each notice opens"
                        + " its program when you click it. If there are no notices, the pane says \"No new"
                        + " notifications\". Four quick actions sit along its foot.")
                .table("The quick actions", "Action", "What it does")
                .row(GuideCell.words("All settings"),
                        GuideCell.words("Closes the pane, and opens Settings on its first page, Personalize."))
                .row(GuideCell.words("Network"),
                        GuideCell.words("Opens the Network Manager. If it cannot open, Settings opens instead."))
                .row(GuideCell.words("Quiet hours"),
                        GuideCell.words("Turns Quiet hours on or off. The button is lit while it is on."))
                .row(GuideCell.words("Sound"), GuideCell.words("Opens Settings on the Sound page."))
                .subheading("Quiet hours")
                .paragraph("Quiet hours keeps notices from popping up. While it is on, a notice goes to the list only:"
                        + " no balloon appears, and no notify sound plays. The switch does not mute dialogs, so an"
                        + " error or a question still appears.")
                .ifSomethingGoesWrong("No notice appears on the screen.",
                        "On Frames 10, Quiet hours may be on. Open the Action Center: a lit Quiet hours button means"
                                + " it is on. Click it to turn it off.",
                        "The list of notices is empty after you open the desktop again.",
                        "The list lasts only while the desktop is open, so it clears when the desktop closes.")
                .define("dialog", "A box in the middle of the screen that waits until you answer it.")
                .define("balloon", "A small box over the status area of the taskbar, which goes away by itself after"
                        + " 9 seconds.")
                .define("toast", "The Frames 10 form of the balloon, shown in a corner of the screen.")
                .define("Quiet hours", "A switch that keeps notices from popping up. The notices still go to the"
                        + " list.")
                .seeAlso("jsc:taskbar", "jsc:system_sounds", "jsc:frames_settings")
                .register();
    }

    private static void frameSettings(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("frames_settings", section).titled("Settings and the Control Panel")
                .icon(ComputingModule.MONITOR)
                .paragraph("Settings is where you change how your Frames works: its look, its sound, its network and"
                        + " its disks. It has a left column that lists its pages, and the page you choose appears at"
                        + " the right. On Frames 95, Frames XP, Frames 10 and Frames 11 the program is called"
                        + " Settings. On Frames 7 the same program is called Control Panel.")
                .paragraph("Frames 7 and Frames 10 open Settings on a home page. On Frames 7 the home page is a list"
                        + " of categories, and on Frames 10 it is a grid of tiles. The other editions open on their"
                        + " first page.")
                .table("The pages of Settings", "Page", "What you do there")
                .row(GuideCell.words("Personalize"),
                        GuideCell.words("Choose the wallpaper and the clock, and on some editions the accent, the"
                                + " theme and the light or dark look."))
                .row(GuideCell.words("System"),
                        GuideCell.words("Name the computer, read its hardware, and restart into the firmware."))
                .row(GuideCell.words("Network"),
                        GuideCell.words("See the link of the computer, share folders, and allow or refuse remote"
                                + " programs."))
                .row(GuideCell.words("Storage"), GuideCell.words("See each disk and its size. The system disk is"
                        + " marked."))
                .row(GuideCell.words("Display"),
                        GuideCell.words("Set the brightness, and the scale of the screen in percent."))
                .row(GuideCell.words("Programs"), GuideCell.words("See the installed programs, and uninstall them."))
                .row(GuideCell.words("Sound"),
                        GuideCell.words("Set the volume, mute the sound, choose the output, and test the sound."))
                .row(GuideCell.words("Users"),
                        GuideCell.words("Not built yet. The page says \"Coming in a future update\"."))
                .paragraph("The page [Personalize](jsc:personalize) is explained on its own page, and so is the"
                        + " Effects page, reached from several of these pages.")
                .subheading("The pages of each edition")
                .paragraph("Frames 95 lists its eight pages at the left, with no home page. Its"
                        + " [effects page](jsc:effects) is reached from Display.")
                .paragraph("Frames XP lists the same eight pages. Its Effects page, called Performance Options, is"
                        + " reached from System.")
                .paragraph("Frames 10 opens on a home page: the heading \"Frames Settings\", and eight tiles in two"
                        + " rows of four. The tiles are System, Display, Sound, Network, Personalize, Storage,"
                        + " Programs and Users.")
                .paragraph("Frames 11 lists the same eight pages, with no home page. Its Effects page, called Visual"
                        + " effects, is reached from Personalize.")
                .subheading("The categories of Frames 7")
                .paragraph("The Control Panel of Frames 7 has seven categories. Each one is a link, with a line of"
                        + " text under its name.")
                .table("The categories of the Control Panel", "Category", "Its line of text", "What it opens")
                .row(GuideCell.words("System and Security"), GuideCell.words("Look after the disks"),
                        GuideCell.words("System"))
                .row(GuideCell.words("User Accounts"), GuideCell.words("The accounts on this computer"),
                        GuideCell.words("Users, which is not built yet"))
                .row(GuideCell.words("Network and Internet"), GuideCell.words("Share folders, allow remote programs"),
                        GuideCell.words("Network"))
                .row(GuideCell.words("Appearance and Personalization"), GuideCell.words("Change the theme"),
                        GuideCell.words("Personalize"))
                .row(GuideCell.words("Hardware and Sound"), GuideCell.words("Adjust the display"),
                        GuideCell.words("Sound, through its title; Display, through its line of text"))
                .row(GuideCell.words("Programs"), GuideCell.words("Uninstall a program"), GuideCell.words("Programs"))
                .row(GuideCell.words("Ease of Access"), GuideCell.words("Optimize visual display"),
                        GuideCell.words("The Effects page, Performance Options"))
                .subheading("Opening a page")
                .steps("Open Settings from the Start menu. On Frames 7, open Control Panel.",
                        "Click a page in the list at the left, or a category or tile on the home page.",
                        "Change the setting you want on that page.")
                .subheading("The System page")
                .paragraph("The System page has a box for the name of the computer. It shows \"(unnamed)\" while the"
                        + " name is empty. Below it are the facts of the hardware: the processor and its speed in"
                        + " megahertz (MHz), the architecture, the memory in megabytes (MB), the graphics memory,"
                        + " the system and the"
                        + " platform.")
                .paragraph("On Frames 7, the page also shows the [experience rating](jsc:effects), which is a set of"
                        + " scores for the parts. The button \"Restart to firmware\" restarts the computer into its"
                        + " [firmware](jsc:firmware) setup.")
                .ifSomethingGoesWrong("A page says \"Coming in a future update\".",
                        "The Users page is not built yet, so the game shows that note.",
                        "The button \"Restart to firmware\" is missing.",
                        "The button shows only when a monitor is linked to the computer. Link a monitor, then open"
                                + " the System page again.",
                        "The Sound page says \"No speakers linked\".",
                        "Link speakers to the computer's sound output, then look at the page again.",
                        "The Sound page says \"No sound hardware\".",
                        "The computer has no sound card or board sound. See [Sound cards](jsc:sound_cards).",
                        "The Test button on the Sound page is grey.",
                        "The computer cannot play a sound at the moment, so Test is off.")
                .define("home page", "The first page of Settings on Frames 7 and Frames 10, which lists the"
                        + " categories or the tiles.")
                .define("experience rating", "Five scores that rate the processor, the memory, the graphics, the"
                        + " gaming and the disk of a computer.")
                .define("accent", "The highlight colour of the desktop, used for selections and buttons.")
                .define("theme", "A named preset that sets the accent and the wallpaper together.")
                .seeAlso("jsc:personalize", "jsc:effects", "jsc:system_sounds", "jsc:shared_folders")
                .register();
    }

    private static void personalize(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("personalize", section).titled("Personalising: themes, wallpapers and colours")
                .icon(ComputingModule.MONITOR)
                .paragraph("Personalize is the page where you change how the desktop looks. You choose a wallpaper"
                        + " and the clock format. On the richer editions you also choose an accent and a theme."
                        + " Frames 10 and Frames 11 add a light or dark look, and Frames 11 lets you align the"
                        + " taskbar.")
                .paragraph("The [Effects](jsc:effects) entry sits at the top of this page. It leads to the effects of"
                        + " your edition.")
                .table("What Personalize offers, by edition", "Edition", "The rows of this page")
                .row(GuideCell.words("Frames 95"), GuideCell.words("The wallpaper and the clock."))
                .row(GuideCell.words("Frames XP"), GuideCell.words("The wallpaper, the accent, the themes and the"
                        + " clock."))
                .row(GuideCell.words("Frames 7"), GuideCell.words("The wallpaper, the accent, the themes and the"
                        + " clock."))
                .row(GuideCell.words("Frames 10"),
                        GuideCell.words("The wallpaper, the accent, the themes, the light or dark look and the"
                                + " clock."))
                .row(GuideCell.words("Frames 11"),
                        GuideCell.words("The wallpaper, the accent, the themes, the light or dark look, the taskbar"
                                + " alignment and the clock."))
                .subheading("Wallpapers")
                .paragraph("The grid shows the desktop's own wallpaper first, and then every wallpaper that you can"
                        + " choose. Each edition starts with its own.")
                .table("The wallpapers of the Frames editions", "Wallpaper", "What it shows")
                .row(GuideCell.words("Frames 95"), GuideCell.words("A classic teal background."))
                .row(GuideCell.words("Frames XP"), GuideCell.words("A green hill under a blue sky."))
                .row(GuideCell.words("Frames 7"), GuideCell.words("A night blue, banded with falling light."))
                .row(GuideCell.words("Frames 10"), GuideCell.words("A dark room, with a window of four panes lit."))
                .row(GuideCell.words("Frames 11"),
                        GuideCell.words("The blue bloom, with a dark picture for the dark look."))
                .row(GuideCell.words("Other desktops"),
                        GuideCell.words("The backdrops of KDE, GNOME, Cinnamon and FreeBSD."))
                .paragraph("The backdrop of the Common Desktop Environment (CDE) cannot be chosen on a Frames.")
                .subheading("Accents and themes")
                .paragraph("The accent is the highlight colour of your desktop. You choose one of six accents: blue,"
                        + " green, orange, violet, pink or amber. A theme sets the accent and the wallpaper for you."
                        + " Frames has three themes:")
                .table("The themes", "Theme", "What it sets")
                .row(GuideCell.words("system"), GuideCell.words("No change to the accent; the desktop keeps its own"
                        + " wallpaper."))
                .row(GuideCell.words("ocean"),
                        GuideCell.words("The green accent, and the Frames XP wallpaper, the green hill."))
                .row(GuideCell.words("slate"),
                        GuideCell.words("The violet accent, and the Frames 11 wallpaper, the blue bloom."))
                .paragraph("The theme you chose appears as the highlighted button.")
                .subheading("Light and dark")
                .paragraph("Frames 10 and Frames 11 have a Light and a Dark look. The other editions have no such"
                        + " row: Frames 7 and Frames XP have no dark look.")
                .subheading("Taskbar alignment, on Frames 11")
                .paragraph("Frames 11 offers Center and Left for the taskbar. Center puts the Start button and the"
                        + " program icons together in the middle, and it is the default. Left moves them to the left"
                        + " edge.")
                .subheading("The clock")
                .paragraph("The clock shows 24-hour time by default. Choose 12-hour to change it on any edition.")
                .steps("Open Settings, and open the Personalize page.",
                        "Click a wallpaper in the grid.")
                .ifSomethingGoesWrong("The wallpaper you want is not in the grid.",
                        "Only the wallpapers the game offers appear there.",
                        "Frames 95 has no accent or theme row.",
                        "The classic look of Frames 95 has no accent or theme rows. Change the wallpaper and the clock"
                                + " instead.",
                        "Frames 7 or Frames XP has no light or dark look.",
                        "Use Frames 10 or Frames 11 for a light or dark look.")
                .seeAlso("jsc:frames_settings", "jsc:effects", "jsc:editions_compared")
                .register();
    }

    private static void effects(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("effects", section).titled("Visual effects and the basic look")
                .icon(ComputingModule.MONITOR)
                .paragraph("Effects are the animations and visual extras of a Frames desktop: the glass of Frames 7,"
                        + " the fades, the shadows and the motion when a window opens. You can switch them on or off."
                        + " Each edition has its own Effects page, reached from another page of Settings.")
                .table("Where the Effects page is", "Edition", "Reached from", "Its title")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Display"), GuideCell.words("Effects"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("System"), GuideCell.words("Performance Options"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("System"), GuideCell.words("Performance Options"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Personalize"),
                        GuideCell.words("Personalize > Effects"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Personalize"),
                        GuideCell.words("Personalize > Visual effects"))
                .subheading("What each Effects page offers")
                .table("The switches of each Effects page", "Edition", "The switches")
                .row(GuideCell.words("Frames 95"),
                        GuideCell.words("One box: \"Animate windows when minimizing and restoring\". The footer has"
                                + " OK, Cancel and Apply."))
                .row(GuideCell.words("Frames XP"),
                        GuideCell.words("Four presets, from \"Let Frames choose what's best for my computer\" to"
                                + " \"Custom:\". Four boxes: animating windows, fading or sliding menus, fading or"
                                + " sliding ToolTips, and drop shadows for icon labels; the footer has"
                                + " OK, Cancel and Apply."))
                .row(GuideCell.words("Frames 7"),
                        GuideCell.words("The same presets, plus \"Enable transparent glass\", and boxes for the"
                                + " animations, the menus, the ToolTips and the icon shadows. Under Ease of Access, a"
                                + " switch: \"Turn off all unnecessary animations (when possible)\"."))
                .row(GuideCell.words("Frames 10"),
                        GuideCell.words("Two switches: \"Transparency effects\" and \"Play animations in Frames\"."
                                + " There are no buttons: a change takes effect at once."))
                .row(GuideCell.words("Frames 11"),
                        GuideCell.words("One switch: \"Animation effects\". There are no buttons: a change takes"
                                + " effect at once."))
                .paragraph("On Frames 10, with Play animations in Frames off, windows open, close and minimise at"
                        + " once, and the live tiles stop turning.")
                .subheading("The basic look of Frames 7")
                .paragraph("Frames 7 draws its glass only when the graphics can run it. Your computer checks its"
                        + " graphics first. If they are too weak, Frames 7 falls back to the Basic look: the same"
                        + " frames and buttons, but opaque, with no glass. The check applies only to a computer"
                        + " built of parts.")
                .paragraph("The check uses the graphics score of the computer. The score comes from the best"
                        + " graphics card, or from the integrated graphics when there is no card. Video memory (VRAM),"
                        + " the memory of the graphics card, sets the highest score a card can reach:")
                .table("The highest score by video memory", "Video memory of the card", "Highest score")
                .row(GuideCell.words("Under 128 MB"), GuideCell.words("2.9"))
                .row(GuideCell.words("128 MB to 255 MB"), GuideCell.words("4.9"))
                .row(GuideCell.words("256 MB to 511 MB"), GuideCell.words("5.9"))
                .row(GuideCell.words("512 MB to 1,023 MB"), GuideCell.words("6.9"))
                .row(GuideCell.words("1,024 MB or more"), GuideCell.words("7.9"))
                .paragraph("The Basic look applies when the score is under 3.0. A card with under 128 MB of video"
                        + " memory therefore always gets the Basic look on Frames 7, and so does a computer with"
                        + " integrated graphics and no card.")
                .paragraph("Frames 10 and Frames 11 check the graphics too. They keep their look, and only their"
                        + " effects go off.")
                .paragraph("When the Basic look is on, every effect is off. The Effects page then shows this note:"
                        + " \"The graphics are too weak for these effects, so all are off. A stronger card brings"
                        + " back what is chosen here.\" Your choices stay saved.")
                .paragraph("When Frames 7 falls back to the Basic look, a balloon titled \"Frames 7 Basic\" appears,"
                        + " once.")
                .subheading("The experience rating")
                .paragraph("The System page of Frames 7 shows an experience rating. It has five scores: the"
                        + " processor, the memory, the graphics, the gaming and the disk. The lowest of the five is"
                        + " the base score.")
                .ifSomethingGoesWrong("The glass is opaque, and the effects are gone on Frames 7.",
                        "The graphics are too weak for the glass, so Frames 7 uses the Basic look. Install a stronger"
                                + " graphics card, with more video memory, and check the System page.",
                        "Animations stop on Frames 10.",
                        "Play animations in Frames is off. Switch it on, on the Effects page.",
                        "Animations stop on Frames 7.",
                        "The Ease of Access switch Turn off all unnecessary animations is on. Switch it off.")
                .define("Basic look", "The opaque look that Frames 7 falls back to, when its graphics cannot run the"
                        + " glass.")
                .seeAlso("jsc:personalize", "jsc:graphics_cards", "jsc:frames_settings")
                .register();
    }

    private static void systemSounds(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("system_sounds", section).titled("System sounds")
                .icon(ComputingModule.MONITOR)
                .paragraph("Frames plays short sounds for the events of the machine: startup, shutdown, an error, a"
                        + " notice, a beep, and a device that you plug in or take out. Each edition has its own"
                        + " chimes, so a Frames 95 startup sounds different from a Frames 11 startup. Each sound"
                        + " has a subtitle: the caption that names it on the screen, such as \"Frames 95 chimes\".")
                .table("The startup chime and the error sound", "Edition", "Startup and shutdown", "Error")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Frames 95 chimes"),
                        GuideCell.words("Frames 95 error"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Frames XP chimes"),
                        GuideCell.words("Frames XP error"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Frames 7 chimes"),
                        GuideCell.words("Frames XP error, borrowed"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Frames 10 chimes"),
                        GuideCell.words("Frames 11 error, borrowed"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Frames 11 chimes"),
                        GuideCell.words("Frames 11 error"))
                .paragraph("Frames 7 has no error sound of its own, so it uses the one of Frames XP. Frames 10 uses"
                        + " the one of Frames 11. A shutdown plays the startup chime of the same edition again.")
                .subheading("Sounds that the editions share")
                .table("Sounds shared by the editions", "Sound", "When you hear it")
                .row(GuideCell.words("Notice"),
                        GuideCell.words("\"Frames chimes a notice\", when a balloon appears, unless Quiet"
                                + " hours is on."))
                .row(GuideCell.words("Beep"),
                        GuideCell.words("\"Frames dings\", when you click beside a dialog."))
                .row(GuideCell.words("Device connects"),
                        GuideCell.words("When you plug a device in. Frames 95 has no such sound."))
                .row(GuideCell.words("Device disconnects"),
                        GuideCell.words("When you take a device out. Frames 95 has no such sound."))
                .subheading("Where the sound goes")
                .paragraph("The machine plays the sound through its monitor, its speakers, or both. The Output setting"
                        + " on the Sound page of [Settings](jsc:frames_settings) chooses: Monitor, Speakers or Both.")
                .paragraph("The same Sound page holds the Volume, which changes in steps of 10 percent from 0 to 100"
                        + " percent, and the Mute switch, which shows On or Off. On Frames 10, Quiet hours also stops"
                        + " the notice sound, as the [notices](jsc:notices) entry explains.")
                .ifSomethingGoesWrong("You hear no sound at all.",
                        "Open the Sound page of Settings. Mute must be Off, and Output must reach a speaker or the"
                                + " monitor.",
                        "The Sound page says \"No speakers linked\".",
                        "Link speakers to the computer's sound output.",
                        "The Sound page says \"No sound hardware\".",
                        "The computer has no sound card or board sound. See [Sound cards](jsc:sound_cards).",
                        "The Test button is grey.",
                        "The computer cannot play a sound at the moment, so Test is off.")
                .define("chime", "A short tune that the machine plays at startup and at shutdown.")
                .define("subtitle", "The caption that shows the name of a sound, such as the name of its chime.")
                .seeAlso("jsc:frames_settings", "jsc:speakers", "jsc:sound_cards")
                .register();
    }

    private static void shuttingDown(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("shutting_down", section).titled("Signing out, shutting down and restarting")
                .icon(ComputingModule.MONITOR)
                .paragraph("When you have finished, you can log off, shut the machine down, or restart it. Each"
                        + " edition has its own control for this, and most of them open the same Power dialog.")
                .table("The power control of each edition", "Edition", "Where it is")
                .row(GuideCell.words("Frames 95"), GuideCell.words("The row Shut Down, under the programs in Start."))
                .row(GuideCell.words("Frames XP"),
                        GuideCell.words("Log Off and Turn Off Computer, in the footer of Start."))
                .row(GuideCell.words("Frames 7"), GuideCell.words("The row Shut down, at the foot of Start."))
                .row(GuideCell.words("Frames 10"),
                        GuideCell.words("The power button, at the foot of the rail in Start."))
                .row(GuideCell.words("Frames 11"), GuideCell.words("The power button, in the footer of Start."))
                .subheading("The Power dialog")
                .paragraph("The Power dialog is a short list with the title \"Power\". It has three rows, in this"
                        + " order, and each row shows a short hint under its name.")
                .table("The rows of the Power dialog", "Row", "What it does")
                .row(GuideCell.words("Shut down"), GuideCell.words("The machine powers off."))
                .row(GuideCell.words("Restart"),
                        GuideCell.words("The machine powers off and starts again. If there is no goodbye message to"
                                + " show, the power-on self-test (POST) screen opens at once."))
                .row(GuideCell.words("Log off"), GuideCell.words("The screen closes, and the machine keeps running."))
                .paragraph("A click outside the list closes the dialog without doing anything. The Escape key closes"
                        + " it too.")
                .subheading("Shutting down from the desktop")
                .steps("Open the Start menu.",
                        "Click the power control of your edition, as the table above shows.",
                        "Click Shut down, Restart or Log off in the Power dialog.")
                .subheading("Logging off on Frames XP")
                .paragraph("On Frames XP, the footer's Log Off closes the desktop at once, without the Power dialog."
                        + " The machine keeps running.")
                .subheading("What the computer does")
                .paragraph("The desktop closes first. Then the machine acts on your choice: a shutdown turns it off, a"
                        + " restart starts it again, and a log off leaves it running. To open the firmware setup, use"
                        + " the button Restart to firmware on the System page of [Settings](jsc:frames_settings).")
                .ifSomethingGoesWrong("You chose Shut down by accident.",
                        "The dialog asks nothing after a row is chosen, so the choice goes at once. Click outside the"
                                + " list, or press [[Esc]], before you choose.",
                        "The computer restarts and shows the power-on self-test (POST) again.",
                        "That is a restart, and it is expected. The check runs again at every start.",
                        "The computer still runs after Log off.",
                        "That is expected: Log off closes only the screen. To shut the machine down, open the desktop"
                                + " again and use the Power dialog.")
                .define("Power dialog", "The short list of Shut down, Restart and Log off, with the title Power.")
                .seeAlso("jsc:firmware", "jsc:frames_settings")
                .register();
    }
}
