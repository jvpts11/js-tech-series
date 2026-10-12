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
 * Declares the pages of the programs of Frames: the everyday programs every edition bundles, the tools that look
 * after the machine, and Help and Virtual Studio.
 */
final class FramesProgramsPages {

    private FramesProgramsPages() {
    }

    /** Declares the sections and entries of the programs of Frames, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        everydayPrograms(guide, chapter);
        machineTools(guide, chapter);
        helpAndStudio(guide, chapter);
    }

    private static void everydayPrograms(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("frames_everyday").titled("Everyday programs")
                .in(chapter).register();
        guide.page("frames_program_list", section).titled("The programs of Frames")
                .paragraph("Every Frames edition comes with the same core set of programs, so the tools you need"
                        + " are on each one. They are This PC, Settings, Files, the Editor, the Command Prompt,"
                        + " System Monitor, Calculator, the Device Manager, Help, and the Network and the Network"
                        + " Manager.")
                .paragraph("Most of what you need is built in, but not all of it: Virtual Studio is"
                        + " installable.")
                .paragraph("Some editions give the same program a name of their own. The Files window is called"
                        + " File Explorer on Frames 10 and 11, and the This PC window is called Computer on Frames 7."
                        + " The table shows how the names of Files, This PC and Help change from edition to edition.")
                .table("Program names by edition", "Edition", "Files", "This PC", "Help")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Files"), GuideCell.words("This PC"),
                        GuideCell.words("Help"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Files"), GuideCell.words("This PC"),
                        GuideCell.words("Help and Support"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Files"), GuideCell.words("Computer"),
                        GuideCell.words("Help and Support"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("File Explorer"), GuideCell.words("This PC"),
                        GuideCell.words("Get Help"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("File Explorer"), GuideCell.words("This PC"),
                        GuideCell.words("Get Help"))
                .table("Program names by edition, continued", "Edition", "Device Manager", "Command Prompt")
                .row(GuideCell.words("Frames 95"), GuideCell.words("System, in System Properties"),
                        GuideCell.words("Command Prompt"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Device Manager"),
                        GuideCell.words("Command Prompt"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Device Manager"),
                        GuideCell.words("Command Prompt"))
                .row(GuideCell.words("Frames 10"), GuideCell.words("Device Manager"),
                        GuideCell.words("Command Prompt"))
                .row(GuideCell.words("Frames 11"), GuideCell.words("Device Manager"),
                        GuideCell.words("Megashell, in the launcher"))
                .paragraph("On Frames 7 the Control Panel is the name of Settings. On Frames 11 the launcher calls the"
                        + " prompt Megashell, which is the Command Prompt ([](jsc:command_prompt)) under another"
                        + " name.")
                .define("Program", "A set of instructions the machine runs for you.")
                .define("Built-in program", "A program that comes with the edition, with no install step.")
                .define("Installable program", "A program that is not on the machine at first: you install it"
                        + " from a medium or from the Mirror.")
                .ifSomethingGoesWrong("The Task Manager is not on the desktop.",
                        "It is not a bundled desktop program. Right-click the taskbar and choose Task Manager"
                                + " ([](jsc:task_manager)).",
                        "Virtual Studio is not in the list.",
                        "It is an installable program: install it first ([](jsc:programs)).")
                .seeAlso("jsc:programs", "jsc:this_pc")
                .register();
        guide.page("this_pc", section).titled("This PC")
                .paragraph("This PC is the window that shows the machine itself: its name, its disks, the drives"
                        + " linked to it, the hardware seated in it and the programs installed on it. Open it when"
                        + " you want to know what the computer holds. On Frames 7 the same window is called"
                        + " Computer.")
                .subheading("The card at the top")
                .paragraph("The top of the window is a card for the machine. It shows the machine's name, its kind,"
                        + " its era, its system and whether it is on a network. A machine with no system says"
                        + " \"No system installed\".")
                .subheading("Renaming the machine")
                .paragraph("You can give the machine a name of your own. The name can be 32 characters long at most,"
                        + " and the machine trims the spaces at both ends before it keeps the name.")
                .steps("Press Rename on the card.", "Type the new name in the field.",
                        "Press [[Enter]] to save the name.")
                .paragraph("A disk or medium has a name too. Select its row and press Rename: the name is cut to"
                        + " 32 characters, and a blank name is ignored.")
                .subheading("Devices and drives")
                .paragraph("Under the card, Devices and drives lists each disk with a bar that shows how full it is,"
                        + " and the system it holds. Each linked drive names what is in it. For an installer, the"
                        + " row says what it would install.")
                .table("Buttons on a row", "Button", "What it does")
                .row(GuideCell.code("Open"),
                        GuideCell.words("Opens the disk or medium in the Files window ([](jsc:file_explorer))."))
                .row(GuideCell.code("Install"),
                        GuideCell.words("Starts an install from an installer medium ([](jsc:installing))."))
                .row(GuideCell.code("Eject"), GuideCell.words("Takes the medium out of the drive ([](jsc:drives))."))
                .paragraph("Install appears only on an installer. Eject appears only on a drive that holds a"
                        + " medium.")
                .paragraph("Further down, the window lists the hardware seated in the machine. Installed programs"
                        + " follows, as a grid of the programs on the machine.")
                .define("Device", "A piece of hardware attached to the machine, such as a drive or a monitor.")
                .define("Volume", "The name a disk or medium carries. This PC and the Files tree show it.")
                .ifSomethingGoesWrong("The window says \"No disks installed and no drives linked\".",
                        "Install a disk in the computer, or link a drive to it ([](jsc:drives)).",
                        "The window says \"No system installed\".",
                        "Install a system first ([](jsc:installing)).",
                        "The name does not change after you press Enter.",
                        "The field was blank once its spaces were trimmed, and a blank name is ignored. Type a name.",
                        "You cannot type more than 32 characters.",
                        "The field stops at 32 characters. Use a shorter name.")
                .seeAlso("jsc:drives", "jsc:installing")
                .register();
        guide.page("file_explorer", section).titled("Files and the File Explorer")
                .paragraph("The Files window is where you look at the files on your disks, and open, copy, move,"
                        + " rename and delete them. On Frames 10 the same window is called File Explorer. On the"
                        + " other editions it is called Files.")
                .paragraph("The tree on the left groups the places: Quick access, Storage, Network and Shares. Quick"
                        + " access holds the shortcuts. Shares are the network folders you can reach from this"
                        + " window. The Recycle Bin is a place too: a file dropped on it is deleted.")
                .subheading("The bar above the list")
                .paragraph("Frames 7 shows a command bar above the list. Frames 10 shows a ribbon with the tabs File,"
                        + " Home and View. The Layout group on the ribbon chooses between Details and Large icons."
                        + " The other editions show no bar. Press [[Esc]] to close an open ribbon.")
                .subheading("Keys")
                .table("Keys in the Files window", "Key", "What it does")
                .row(GuideCell.code("Backspace"), GuideCell.words("Goes up one folder."))
                .row(GuideCell.code("Enter"), GuideCell.words("Opens the selected row."))
                .row(GuideCell.code("Up, Down"), GuideCell.words("Moves the selection."))
                .row(GuideCell.code("Delete"), GuideCell.words("Deletes the selected row, or every row you swept."))
                .row(GuideCell.code("F2"), GuideCell.words("Renames the selected row."))
                .row(GuideCell.code("Ctrl+C"), GuideCell.words("Copies the selected row."))
                .row(GuideCell.code("Ctrl+X"), GuideCell.words("Cuts the selected row, unless it is on a read-only"
                        + " medium."))
                .row(GuideCell.code("Ctrl+V"), GuideCell.words("Pastes what you copied or cut."))
                .row(GuideCell.code("Ctrl+A"), GuideCell.words("Selects every file and folder in the listing."))
                .row(GuideCell.code("Esc"), GuideCell.words("Closes the ribbon, if one is open."))
                .subheading("Moving a file")
                .steps("Click the file to select it.", "Press [[Ctrl]]+[[X]] to cut it, or [[Ctrl]]+[[C]] to copy it.",
                        "Open the folder you want, and press [[Ctrl]]+[[V]].")
                .paragraph("Right-click a row to see its menu: Open, Run, Open with, Choose another program, Cut,"
                        + " Copy, Paste, Rename, Delete, New, Eject, Refresh, Extract here and Compress to, with the"
                        + " name of the archive.")
                .subheading("Deleting and read-only places")
                .paragraph("On the system disk, Delete moves the file into the Recycle Bin. On a medium or a network"
                        + " share, Delete removes the file for good, once you say yes. A read-only medium refuses"
                        + " Cut and Delete, and the bar says \"read-only medium\".")
                .paragraph("A .dat file is different: it is a read-only list of the items stored on a disk. You can"
                        + " read it, but no one can change it by hand. The window says that to change those items"
                        + " you use the network interactor ([](jsc:interactor_screen)).")
                .define("Sweep", "Dragging a box over several rows to select all of them at once.")
                .define(".dat file", "A read-only file that lists the items stored on a disk. You can read it, but"
                        + " you cannot change it by hand.")
                .ifSomethingGoesWrong("A file on a medium is gone, and there is no Recycle Bin to bring it back.",
                        "Delete on a medium is permanent. Copy the file to the system disk first if you may need it.",
                        "The window says \"This file is part of the installer and cannot be changed, copied or"
                                + " deleted.\"",
                        "Run the setup program to install it, as the message says.",
                        "The window says \"This file is impossible to modify, create, delete or change manually,"
                                + " use the network interactor for it.\"",
                        "Read the file. To change its items, use the network interactor"
                                + " ([](jsc:interactor_screen)).",
                        "Cut and Delete are refused on a read-only medium.",
                        "Copy the file with [[Ctrl]]+[[C]] and paste it onto a disk you can change.",
                        "A dialog titled \"Could not do that\" opens.",
                        "Read the message in the dialog: it says what was refused.")
                .seeAlso("jsc:drives", "jsc:interactor_screen")
                .register();
        guide.page("editor", section).titled("The Editor")
                .paragraph("The Editor writes and changes text files. Each file you open gets a tab of its own, and"
                        + " you can have up to 8 files open at once. It shows line numbers, so you can find your"
                        + " place, and the line and column of the cursor in the status bar.")
                .paragraph("Long lines can wrap onto the next row of the screen. You can turn wrap off, so that each"
                        + " line stays on one row.")
                .subheading("Keys")
                .table("Keys in the Editor", "Key", "What it does")
                .row(GuideCell.code("Ctrl+S"), GuideCell.words("Saves the file. A file with no name asks for one"
                        + " first."))
                .row(GuideCell.code("Ctrl+O"), GuideCell.words("Opens a file through the system's file window."))
                .row(GuideCell.code("Ctrl+P"), GuideCell.words("Prints the file."))
                .row(GuideCell.code("Ctrl+F"), GuideCell.words("Opens the find bar."))
                .row(GuideCell.code("Ctrl+G"), GuideCell.words("Opens the find bar in Go to line mode."))
                .row(GuideCell.code("Ctrl+N"), GuideCell.words("Opens a new tab with a new file."))
                .row(GuideCell.code("Ctrl+W"), GuideCell.words("Closes the current tab."))
                .row(GuideCell.code("Ctrl+Tab"), GuideCell.words("Switches to the next tab."))
                .row(GuideCell.code("Enter"), GuideCell.words("While the find bar has focus, runs its action."))
                .row(GuideCell.code("Esc"), GuideCell.words("While the find bar is open, closes it and returns to"
                        + " the text."))
                .subheading("The find bar")
                .paragraph("Press [[Ctrl]]+[[F]] to open the find bar, which offers Replace, Next and"
                        + " Replace all. Press [[Ctrl]]+[[G]] to use the same bar to"
                        + " go to a line: the bar then offers Go to and Go.")
                .subheading("Saving and closing")
                .steps("Press [[Ctrl]]+[[S]] to save the file under its name.",
                        "If the file has no name, the Editor says \"Save As first\". Use Save As to give it a name,"
                                + " then save.",
                        "To close a tab with changes you have not saved, press [[Ctrl]]+[[W]] and read the message.",
                        "Save with [[Ctrl]]+[[S]], or press [[Ctrl]]+[[W]] again to discard the changes.")
                .define("Tab", "One open file in the Editor. The Editor keeps up to 8 tabs at once.")
                .define("Wrap", "The way a long line continues on the next row of the screen. You can turn it off.")
                .define("Find bar", "The strip where you type what to find. It also does the Go to line action.")
                .ifSomethingGoesWrong("The Editor says \"Save As first\".",
                        "The file has no name yet, so [[Ctrl]]+[[S]] cannot save it. Give it a name first.",
                        "The Editor says \"Cannot save a .dat file\".",
                        "A .dat file is read-only: you can read it, but the Editor cannot save it.",
                        "The Editor reports that the file is too long to save, with its length and the limit in"
                                + " characters.",
                        "Shorten the text. A file holds up to 32,767 characters.",
                        "The Editor reports that the path is too long to save to, with its length and the limit in"
                                + " characters.",
                        "Use a shorter name or fewer folders. A path holds up to 160 characters.",
                        "The Editor says \"Only 8 files at once\".",
                        "Close a tab with [[Ctrl]]+[[W]] first.",
                        "The Editor warns about unsaved changes when you close a tab, and asks you to close"
                                + " again to discard them.",
                        "Save with [[Ctrl]]+[[S]], or close the tab again to discard the changes.",
                        "The Editor says a file \"is too large to open here\".",
                        "The Editor leaves the file as it is, so nothing is lost.")
                .seeAlso("jsc:file_explorer")
                .register();
    }

    private static void machineTools(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("frames_machine_tools").titled("Looking after the machine")
                .in(chapter).register();
        guide.page("device_manager", section).titled("The Device Manager")
                .paragraph("The Device Manager lists the hardware of the machine and the port each piece is plugged"
                        + " into. It lets you disable a device, which stops the computer from using it, and enable"
                        + " it again. It is on Frames only.")
                .paragraph("Its look follows the edition. On Frames 95 it sits in System Properties, under the name"
                        + " System. On Frames XP it is a console window, with the views in its View menu. On Frames 11"
                        + " the views are a switch in its menu row.")
                .table("Views", "View", "What the list groups by")
                .row(GuideCell.words("Devices by type"), GuideCell.words("What each device is."))
                .row(GuideCell.words("Devices by connection"), GuideCell.words("How each device is connected."))
                .row(GuideCell.words("Devices by port"), GuideCell.words("The port each device is plugged into."))
                .table("Menus of the window", "Menu", "Entries")
                .row(GuideCell.words("File"), GuideCell.words("Print... (does nothing), Exit"))
                .row(GuideCell.words("Action"), GuideCell.words("Enable or Disable, Update Driver... (does nothing),"
                        + " Uninstall (does nothing), Properties"))
                .row(GuideCell.words("View"), GuideCell.words("The three views, and Show hidden devices (does"
                        + " nothing)"))
                .row(GuideCell.words("Help"), GuideCell.words("About Device Manager"))
                .subheading("Disabling a device")
                .steps("Select the device in the list.",
                        "Open the Action menu and choose Disable. A device that is already disabled offers Enable.",
                        "Look at the list: a disabled device is drawn crossed out.")
                .paragraph("A device you disable stays plugged in, and you can enable it again at any time. Only a"
                        + " device on one of this computer's own linked ports can be disabled.")
                .subheading("What a disabled device stops doing")
                .table("When a device is disabled", "Device", "What stops")
                .row(GuideCell.words("Monitor"), GuideCell.words("It is not driven, and its video memory is not"
                        + " counted."))
                .row(GuideCell.words("Speaker"), GuideCell.words("It is silent."))
                .row(GuideCell.words("Any Redstone Interface"), GuideCell.words("It does nothing while its link is"
                        + " disabled."))
                .row(GuideCell.words("Network Gateway"), GuideCell.words("It is not active while its owner is"
                        + " disabled."))
                .row(GuideCell.words("Dock Station"), GuideCell.words("It does not start a desktop."))
                .define("Port", "A socket on the machine, such as a card slot or a link port, that a device is"
                        + " plugged into.")
                .define("Hub", "A device that adds ports of its own. A device plugged into a hub is listed under"
                        + " it.")
                .paragraph("A hub is a device too, and disabling a hub disables every device under it. If you"
                        + " unlink a device, its disabled setting is removed, so a device you link again comes"
                        + " back enabled.")
                .ifSomethingGoesWrong("The device you want to disable is missing from the list.",
                        "Only a device on one of this computer's linked ports can be disabled. Link it first"
                                + " ([](jsc:peripheral_cables)).",
                        "Every device under a hub is disabled.",
                        "Enable the hub again. A device disabled by itself stays disabled, so enable it too.",
                        "Update Driver, Uninstall, Show hidden devices and Print do nothing when you choose them.",
                        "Use Enable and Disable, which are the actions that work.")
                .seeAlso("jsc:disable_device", "jsc:peripheral_cables")
                .register();
        guide.page("task_manager", section).titled("The Task Manager")
                .paragraph("The Task Manager shows what the machine is running and what each thing uses: the windows"
                        + " open, the processes, the processor (CPU) and the memory, and the disks. You use it to end"
                        + " a program that will not close.")
                .paragraph("Open it from the taskbar ([](jsc:taskbar)): right-click the taskbar and choose Task"
                        + " Manager. It is kept off the desktop and off the Start menu.")
                .subheading("Its look on each edition")
                .table("The Task Manager on each edition", "Edition", "Look", "Pages")
                .row(GuideCell.words("Frames 95"), GuideCell.words("A Close Program box, with two warnings"),
                        GuideCell.words("None: the box has no pages"))
                .row(GuideCell.words("Frames XP and Frames 7"), GuideCell.words("A window with a menu bar and a status"
                        + " bar"), GuideCell.words("Applications, Processes, Performance and Networking"))
                .row(GuideCell.words("Frames 10 and Frames 11"), GuideCell.words("A window with graphs"),
                        GuideCell.words("Processes, Performance, Services, Storage, Network, and the graphics"
                                + " processor (GPU)"))
                .subheading("Ending a program or a process")
                .steps("Click a row that is a window or a process.",
                        "Press End Task. In the process list the button reads End Process.")
                .paragraph("End Task is unavailable when the selected row is neither a window nor a process. Ending a"
                        + " window closes that program. Ending a process stops the script or service it names.")
                .subheading("The process list")
                .table("Columns of the process list", "Column", "What it shows")
                .row(GuideCell.words("Image Name or Process Name"), GuideCell.words("The name of the program or the"
                        + " process."))
                .row(GuideCell.words("Mem Usage or Memory"), GuideCell.words("How much memory the row uses."))
                .row(GuideCell.words("CPU"), GuideCell.words("How much of the processor the row uses."))
                .row(GuideCell.words("Kind"), GuideCell.words("What sort of row it is: system, desktop, service,"
                        + " program, script or graphics."))
                .subheading("Performance and networking")
                .paragraph("The Performance page shows CPU Usage and Memory Usage as figures and as histories. The"
                        + " graphics part shows video memory, with its dedicated and shared parts. A computer with no"
                        + " graphics says \"This computer has no graphics.\"")
                .paragraph("The Networking page says whether the machine is Connected or Not connected, and shows the"
                        + " link activity over the last minute.")
                .define("Process", "A running script or service on the machine.")
                .paragraph("The Task Manager does not show the Operations of a network. You watch those in the Network"
                        + " Manager ([](jsc:network_manager)). The eight states an Operation can be in are on their"
                        + " own page ([](jsc:operation_states)).")
                .ifSomethingGoesWrong("End Task is unavailable.",
                        "Click a row that is a window or a process before you press it.",
                        "The Close Program box says \"Ending a program may lose unsaved work.\"",
                        "Save your work in that program first, then end it.",
                        "The Close Program box says \"Ending the system restarts the machine.\"",
                        "Choose Cancel, unless you want the machine to restart.",
                        "You look for a network job here and do not find it.",
                        "Open the Network Manager ([](jsc:network_manager)).")
                .seeAlso("jsc:taskbar", "jsc:network_manager")
                .register();
        guide.page("system_monitor", section).titled("The System Monitor")
                .paragraph("The System Monitor is a dashboard of this machine at a glance: its processor, its memory,"
                        + " its graphics, and a bar for every disk. It only reads the machine and shows it, so you"
                        + " cannot change anything in it.")
                .paragraph("Open it before you start a big program, to see whether the machine has memory to spare."
                        + " The memory part shows the random-access memory (RAM) that programs hold, and how much is"
                        + " free.")
                .subheading("What you read")
                .paragraph("The processor load is how busy the processor is, in percent. Memory"
                        + " shows the memory held, out of the total in MB, and the free memory below it.")
                .paragraph("Graphics shows \"no GPU\" when the machine has no graphics processor (GPU). Otherwise it"
                        + " shows the video memory held, out of the total, and the load of the card in percent.")
                .paragraph("Storage draws a bar for each disk, and marks the system disk with the word (system)."
                        + " It also counts"
                        + " the programs installed on the machine.")
                .define("Load", "How busy a part of the machine is, in percent.")
                .define("Held", "Memory that programs have taken for themselves. Free memory is what is left.")
                .ifSomethingGoesWrong("A bar shows a figure a moment late.",
                        "The window reads the machine again about every 40 frames of the screen. Wait a moment, then"
                                + " look again.")
                .seeAlso("jsc:task_manager", "jsc:this_pc")
                .register();
    }

    private static void helpAndStudio(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("frames_help_and_studio").titled("Help and Virtual Studio")
                .in(chapter).register();
        guide.page("frames_help", section).titled("Help and Support, and Get Help")
                .paragraph("Help is the window that reads the manuals of the series and the machine's own manual pages."
                        + " Open it when you want to know what a command does or how a part works. Every desktop"
                        + " ships it, under a name that depends on the edition.")
                .paragraph("The contents hold the manuals of the series, with the Technical Reference first, as books"
                        + " of chapters, sections and entries. The commands of the machine are there too: each"
                        + " command's page is the one the machine prints for it at a terminal.")
                .table("Help on each edition", "Edition", "Window title", "Layout")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Help Topics: Frames Help"),
                        GuideCell.words("Tabs for Contents, Index and Find"))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Help and Support Center"),
                        GuideCell.words("A Technical Reference pane, with Search, Home, Index, Favorites and History"))
                .row(GuideCell.words("Frames 7"), GuideCell.words("Frames Help and Support"),
                        GuideCell.words("Search Help and Browse Help"))
                .row(GuideCell.words("Frames 10 and Frames 11"), GuideCell.words("Get Help"),
                        GuideCell.words("A search box that says \"Search the manuals and commands\""))
                .subheading("Searching")
                .steps("Type the word you want. The search field opens as you type.",
                        "Press [[Enter]] to run the search.",
                        "Read the result line: it gives the number of results and the word you searched"
                                + " for.")
                .paragraph("Links on a page lead to another page, and Back returns you to the page before.")
                .subheading("Keys")
                .table("Keys in Help", "Key", "What it does")
                .row(GuideCell.code("Alt+Left"), GuideCell.words("Goes back to the page before."))
                .row(GuideCell.code("Alt+Right"), GuideCell.words("Goes forward again."))
                .row(GuideCell.code("Backspace"), GuideCell.words("Goes back, or deletes a letter while you type a"
                        + " search."))
                .row(GuideCell.code("Page Up, Page Down"), GuideCell.words("Scrolls the page."))
                .row(GuideCell.code("Enter"), GuideCell.words("Runs the search. On Frames 95 it"
                        + " displays the selected topic."))
                .row(GuideCell.code("Esc"), GuideCell.words("Stops typing the search."))
                .define("Topic", "One page of help. On Frames 95 each topic opens in its own window.")
                .paragraph("The help is read at this machine, so the page for a command shows only the commands the"
                        + " machine has.")
                .ifSomethingGoesWrong("The result line reads \"Nothing in the manuals or the commands answers to\","
                        + " followed by your word.",
                        "Try a shorter word, or the name of the command.",
                        "A command you expected is missing from the results.",
                        "Look for the command on a machine that has it.")
                .seeAlso("jsc:help_on_a_computer")
                .register();
        guide.page("virtual_studio", section).titled("Virtual Studio")
                .paragraph("Virtual Studio is the workshop for Sigma# programs, in one window. You open a solution or a"
                        + " folder, write the sources, build them, and start the program. The language itself is in"
                        + " the Book of Sigma ([](jsc:book_of_sigma)).")
                .paragraph("It is an installable program, for Frames XP or newer. Installing it needs 512 MB of free"
                        + " disk space, and it holds 256 MB of memory while it is open.")
                .subheading("Opening your work")
                .table("The Start Window cards", "Card", "What it opens")
                .row(GuideCell.words("Open a project or solution"), GuideCell.words("A .sln file on this machine."))
                .row(GuideCell.words("Open a local folder"), GuideCell.words("Any folder of programs."))
                .row(GuideCell.words("Open a file"), GuideCell.words("One .sgs or .sg file, with no project."))
                .paragraph("Virtual Studio opens .sln, .sgproj, .sgsproj, .sg, .sgs and .asm files.")
                .subheading("Building and starting")
                .steps("Press [[Ctrl]]+[[Shift]]+[[B]] to build the solution.",
                        "Read the result: it says Build succeeded, or Build failed with the number of errors.",
                        "Press [[F5]] to build the startup project and run it at the terminal.")
                .table("Keys in Virtual Studio", "Key", "What it does")
                .row(GuideCell.code("Ctrl+Shift+N"), GuideCell.words("Opens the New Project wizard."))
                .row(GuideCell.code("Ctrl+Shift+B"), GuideCell.words("Builds the solution."))
                .row(GuideCell.code("Ctrl+S"), GuideCell.words("Saves the file."))
                .row(GuideCell.code("Ctrl+F"), GuideCell.words("Finds text."))
                .row(GuideCell.code("Ctrl+G"), GuideCell.words("Goes to a line."))
                .row(GuideCell.code("F5"), GuideCell.words("Builds the startup project and runs it."))
                .row(GuideCell.code("Ctrl+W"), GuideCell.words("Closes the current tab."))
                .row(GuideCell.code("Ctrl+."), GuideCell.words("Implements an interface."))
                .row(GuideCell.code("Ctrl+`"), GuideCell.words("Opens the terminal, ready for typing."))
                .row(GuideCell.code("Ctrl+Space"), GuideCell.words("Shows the completions, if they are turned on."))
                .paragraph("The list of what can follow a name says what each call costs the program, before you"
                        + " write the line. The Book of Sigma explains the editors ([](jsc:sigma_editors)).")
                .define("Solution", "A folder that groups projects. It has a file that ends in .sln.")
                .define("Project", "One program's source files, and the project file that says what to build.")
                .define("Template", "A starting point for a new project, offered by the New Project wizard.")
                .define("Build", "Turning the sources into a program. Start builds the startup project, then runs it.")
                .ifSomethingGoesWrong("Virtual Studio cannot be installed on Frames 95.",
                        "It needs Frames XP or newer ([](jsc:frames_xp)).",
                        "The disk has less than 512 MB of free space when you install it.",
                        "Free up 512 MB of disk space, then install it.",
                        "The window says \"Low on memory\".",
                        "This computer is running out of RAM for programs. Close other programs, or add memory"
                                + " ([](jsc:frames_memory)).",
                        "The result says Build failed.",
                        "Fix the errors the build reports, then press [[Ctrl]]+[[Shift]]+[[B]] again.")
                .seeAlso("jsc:book_of_sigma", "jsc:sigma_editors")
                .register();
    }
}
