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
 * Pages of the programs every system has: how programs are installed, the desktop programs, the programs
 * that talk over the network, and where to find help on a computer.
 */
final class EverydayProgramsPages {

    private EverydayProgramsPages() {
    }

    /** Declares the sections and entries of the programs every system has, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        getPrograms(guide, chapter);
        desktopApps(guide, chapter);
        networkTools(guide, chapter);
        socialAndMedia(guide, chapter);
        gettingHelp(guide, chapter);
    }

    private static void getPrograms(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("get_programs").titled("Getting programs")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        guide.page("programs", section).titled("Programs").icon(ComputingModule.CD_ROM)
                .paragraph("A program is software that does one job for you, such as drawing a picture,"
                        + " playing a game or keeping a list of files. Every system comes with some programs,"
                        + " and you can install others. Each program has a name, runs on some kinds of system,"
                        + " and needs some disk space and memory.")
                .define("Program", "Software that a system runs for you, in a window or at a prompt.")
                .paragraph("There are two ways to install a program. You can install it from install media:"
                        + " the disc or stick the program ships on, such as a CD (compact disc), a DVD (digital"
                        + " versatile disc) or a USB (universal serial bus) stick. Or you can install it from"
                        + " [the Mirror](jsc:mirror), by name, with no media at all.")
                .paragraph("From media, you put the program's install medium in a linked drive of the"
                        + " computer, and then you start its setup.")
                .steps("Put the program's install medium in a linked drive of the computer.",
                        "At a prompt, type install and the program's name, then press [[Enter]].")
                .paragraph("On a Frames desktop you can also open This PC, where each linked drive shows its"
                        + " program with an Install button beside it. The computer answers with a line that"
                        + " starts with Setting up, then the program's name, then from and the medium, such as"
                        + " the floppy or the CD.")
                .define("Desktop", "The graphical face of a system: its windows, icons, menus and the programs you"
                        + " start from it.")
                .table("Install media by era", "Era of the program", "Medium", "Drive you use")
                .row(GuideCell.words("Vintage"), GuideCell.words("Floppy"), GuideCell.words("Floppy Drive"))
                .row(GuideCell.words("Legacy"), GuideCell.words("CD"), GuideCell.words("CD Drive"))
                .row(GuideCell.words("Transition"), GuideCell.words("DVD"), GuideCell.words("DVD Drive"))
                .row(GuideCell.words("Standard"),
                        GuideCell.words("USB stick for a service that runs in the background, DVD for the rest"),
                        GuideCell.words("Dock Station for the stick, DVD Drive for the disc"))
                .row(GuideCell.words("Advanced"),
                        GuideCell.words("USB stick for a service that runs in the background, Blu-ray for the"
                                + " rest"),
                        GuideCell.words("Dock Station for the stick, Blu-ray Drive for the disc"))
                .paragraph("The systems come on the same media, by their era: a Vintage system on a floppy, a"
                        + " Legacy system on a CD, a Transition system on a DVD, and a Standard or Advanced"
                        + " system on a USB stick. Read more about the drives on their own page"
                        + " ([](jsc:drives)).")
                .paragraph("From the Mirror you install by name, and the computer needs a network whose"
                        + " Mainframe runs the Mirror. The Mirror is the package service of that Mainframe: it"
                        + " keeps the network's packages, and each computer installs from it with its package"
                        + " manager.")
                .define("Package", "A program as the network's repository keeps it, ready to install by name.")
                .define("Package manager", "The program that fetches a package and records what is installed"
                        + " on the computer, such as apt, dnf, pacman, emerge, pkg or pckmgr.")
                .table("Package managers by system", "System", "Package manager")
                .row(GuideCell.words("Ubuntu and Debian"), GuideCell.code("apt"))
                .row(GuideCell.words("Fedora"), GuideCell.code("dnf"))
                .row(GuideCell.words("Arch Linux"), GuideCell.code("pacman"))
                .row(GuideCell.words("Gentoo"), GuideCell.code("emerge"))
                .row(GuideCell.words("FreeBSD"), GuideCell.code("pkg"))
                .row(GuideCell.words("Frames 95 to 11"), GuideCell.code("pckmgr"))
                .row(GuideCell.words("MC-DOS, MC-NET and UNIX System V"),
                        GuideCell.words("None: these systems install programs from media only."))
                .paragraph("Gentoo builds each program from its source code. When the computer has room for the"
                        + " build, the install runs as a script in front of the terminal, and the answer comes"
                        + " later.")
                .paragraph("The store command lists every program that is not preinstalled, one line each,"
                        + " with the install command beside it. You can also type available. The list does not"
                        + " depend on your computer: a program in the list may still refuse to install here,"
                        + " and the install says why. When nothing is left, store answers \"nothing else to"
                        + " install\".")
                .paragraph("The update command brings the installed programs up to their newest version. It"
                        + " never installs a new program, and it answers \"All packages are up to date.\" when"
                        + " there is nothing to do.")
                .paragraph("On Frames, the pckmgr command also has the verbs remove, search and list. The list"
                        + " verb with --available shows every program the Mirror offers.")
                .paragraph("Some programs are services that run on a Mainframe. When you install one there,"
                        + " the service switches on, and the Mainframe keeps a record of its version. Some"
                        + " programs also need an IQL engine on the network"
                        + " ([](jsc:engines)).")
                .paragraph("You can publish your own packages to the Mirror, for other players to install, with"
                        + " the Sigma package tool ([](jsc:sigma_publish_mirror)).")
                .paragraph("The programs of the other chapters have their own pages: the Crafting Manager"
                        + " ([](jsc:crafting_manager)), the Pattern Studio ([](jsc:pattern_studio)) and the"
                        + " Workshop ([](jsc:workshop)).")
                .paragraph("The desktop programs follow on the next pages: the Network window, This PC, Files,"
                        + " the Editor, Settings, the System Monitor, the Task Manager, a Calculator, Paint,"
                        + " Exceed, and the games. The books of the series, one for each family of systems, go"
                        + " deeper into install and use ([](jsc:library)).")
                .ifSomethingGoesWrong(
                        "The install says that the program needs its install disc in a linked drive.",
                        "Put the disc of that program in a linked drive of the same computer, then install"
                                + " again.",
                        "The install says that the program needs a later era of hardware.",
                        "The computer's hardware is older than the program allows. Install it on a newer"
                                + " computer.",
                        "The install says that the program only installs on one kind of computer, such as the"
                                + " Mainframe.",
                        "Install it on the kind of computer the message names.",
                        "The install says unmet requirements (hardware or free disk space).",
                        "The computer lacks a processor or a graphics card the program needs, or it has too"
                                + " little free disk space. Give it what is missing, then install again.",
                        "The install says could not resolve mirror://.",
                        "Connect the computer to a network whose Mainframe runs the Mirror ([](jsc:mirror)).",
                        "The install says this system installs programs from install media, not a package"
                                + " manager.",
                        "Put the program's install medium in a linked drive, and install from it.",
                        "The install says unable to locate package, followed by a name.",
                        "The Mirror does not offer that name for your system. Check the name in the store list,"
                                + " then install again.",
                        "The install says this computer is still setting up a program.",
                        "Wait for that setup to end before you start another install.",
                        "The install says the installation could not be written to the disk.",
                        "There is no space on the disk, or there is no disk. Free some space, or put a disk in"
                                + " the computer, then install again.",
                        "The install says a program is already the newest version.",
                        "Nothing needs doing: the program is installed and up to date.")
                .seeAlso("jsc:installing", "jsc:the_systems", "jsc:drives", "jsc:mirror")
                .register();
    }

    private static void desktopApps(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("desktop_apps").titled("On the desktop")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        guide.page("this_pc_and_desktop_tools", section).titled("This PC, Files, the Editor and Settings")
                .icon(ComputingModule.MONITOR)
                .paragraph("Some programs come with every desktop, under the names its system gives them. This"
                        + " page covers four of them: This PC, which shows the machine and its drives; Files,"
                        + " which holds your files; the Editor, which writes text; and Settings, which changes"
                        + " the way the desktop and the machine behave. The Network window has its own page"
                        + " ([](jsc:network_interactor)).")
                .subheading("This PC")
                .paragraph("This PC is the page of the machine itself. A card shows the computer's name, its"
                        + " kind, its era, its system and its network. Below it are the devices and drives:"
                        + " each disk with a bar that shows how full it is, and each linked drive with what is"
                        + " in it. For an installer in a linked drive, This PC names the program it would"
                        + " install.")
                .table("Parts of This PC", "Part", "What it does")
                .row(GuideCell.words("Card"),
                        GuideCell.words("The computer's name, kind, era, system and network."))
                .row(GuideCell.words("Devices and drives"),
                        GuideCell.words("Each disk, and each linked drive with its Install, Open and Eject"
                                + " buttons."))
                .row(GuideCell.words("Hardware"), GuideCell.words("The parts fitted to the computer."))
                .row(GuideCell.words("Programs"), GuideCell.words("The programs installed, as a grid."))
                .paragraph("This PC is on Frames, Linux and FreeBSD. UNIX has"
                        + " [Workstation Info](jsc:cde_workstation_info) in its place.")
                .subheading("Files")
                .paragraph("Files is the program for your files. Its toolbar"
                        + " has back, forward and up buttons. An address trail shows the folder you are in, and"
                        + " each part of it is clickable. A search box sits beside it.")
                .paragraph("The drive tree on the left holds the quick-access shortcuts and every volume, each"
                        + " with its letter. A removable disk has an eject control. The list in the middle shows"
                        + " the files of the folder, with an icon for each type, and you sort it by name, type"
                        + " or size.")
                .table("Parts of Files", "Part", "What it does")
                .row(GuideCell.words("Toolbar"), GuideCell.words("Back, forward and up."))
                .row(GuideCell.words("Address trail"),
                        GuideCell.words("The folder you are in, each part clickable."))
                .row(GuideCell.words("Drive tree"),
                        GuideCell.words("Shortcuts and every volume, with its letter."))
                .row(GuideCell.words("List"),
                        GuideCell.words("The files of the folder, sortable by name, type or size."))
                .row(GuideCell.words("Status bar"), GuideCell.words("Along the foot of the window."))
                .paragraph("Double-click a folder to open it. Double-click an installer to run its setup, or a"
                        + " text file to open it in the Editor. The right-click menu offers cut, copy, paste,"
                        + " rename, delete, new file, new folder and properties, and it greys out what the"
                        + " volume forbids. An archive opens with 67ark ([](jsc:archiver)).")
                .subheading("The Editor")
                .paragraph("The Editor edits plain text. It is not an integrated development environment (IDE)."
                        + " It has no syntax highlighting, no completion and no compiler, so it only edits the"
                        + " text.")
                .table("Parts of the Editor", "Part", "What it does")
                .row(GuideCell.words("Line numbers"), GuideCell.words("Beside each line of the text."))
                .row(GuideCell.words("Find and replace"), GuideCell.words("Finds text, and changes it."))
                .row(GuideCell.words("Go to line"), GuideCell.words("Jumps to the line you name."))
                .row(GuideCell.words("Wrap"),
                        GuideCell.words("Breaks long lines to the width of the window. You can turn it off."))
                .row(GuideCell.words("Bar at the foot"), GuideCell.words("The line and the column you are on."))
                .paragraph("You can have several files open at once. Open and Save As use the system's own file"
                        + " window. The Editor is the default for text files, so a .txt file opens there.")
                .subheading("Settings")
                .paragraph("Settings is the control panel. A list on the left holds its pages, and the page you"
                        + " pick fills the pane on the right. A change takes effect as soon as you make it.")
                .paragraph("The eight pages are Personalize, System, Network, Storage, Display, Programs, Sound"
                        + " and Users. The Programs page holds the line Uninstall a program.")
                .steps("Open Files from the desktop.", "Double-click the folder that holds the text file.",
                        "Double-click the text file. It opens in the Editor.")
                .ifSomethingGoesWrong("The Editor says a file is too large to open here.",
                        "Split the text into smaller files, and open each one.",
                        "An item in the right-click menu is greyed out.",
                        "The volume does not allow that change. Choose another action, or another volume.")
                .seeAlso("jsc:programs", "jsc:archiver", "jsc:help_on_a_computer")
                .register();
        guide.page("system_monitor_and_task_manager", section)
                .titled("The System Monitor and the Task Manager").icon(ComputingModule.MONITOR)
                .paragraph("The System Monitor and the Task Manager show what the machine is doing. The first"
                        + " only reads: it shows the load on the processor, the memory, the graphics and the"
                        + " storage. The second lists what is running, and it lets you end a program.")
                .subheading("The System Monitor")
                .paragraph("The System Monitor is on the desktop systems. It reads the machine as it works, and"
                        + " it changes as the load changes.")
                .table("Parts of the System Monitor", "Part", "What it shows")
                .row(GuideCell.words("Processor"), GuideCell.words("The load on the processor."))
                .row(GuideCell.words("Memory"),
                        GuideCell.words("The megabytes held and the total, then the megabytes that are free."))
                .row(GuideCell.words("Graphics"),
                        GuideCell.words("The video memory in use and the load on the card. Without a card it"
                                + " says no GPU."))
                .row(GuideCell.words("Storage"),
                        GuideCell.words("How many programs are installed, and each disk, with the system's disk"
                                + " marked (system)."))
                .subheading("The Task Manager")
                .paragraph("Right-click the panel and open the Task Manager from its menu. It is not on the"
                        + " Start menu or the desktop. Each desktop has its own manager, so what you see depends"
                        + " on your system.")
                .table("The Task Manager by desktop", "Desktop", "Its manager")
                .row(GuideCell.words("Frames 95"), GuideCell.words("A Close Program box."))
                .row(GuideCell.words("Frames XP"), GuideCell.words("Four tabs, with a menu and a status bar."))
                .row(GuideCell.words("Frames 11"), GuideCell.words("A rail at the side and a wide table."))
                .row(GuideCell.words("Linux desktops"), GuideCell.words("Each desktop's own system monitor."))
                .paragraph("On Frames XP the tabs are Applications, Processes, Performance and Networking. The"
                        + " Performance tab shows the use of the processor and the memory, as numbers and as a"
                        + " history, and the graphics card when there is one. The Networking tab shows whether"
                        + " the machine is Connected or Not connected, and the link activity over the last"
                        + " minute.")
                .table("Buttons of the Task Manager", "Button", "What it does")
                .row(GuideCell.code("End Task"),
                        GuideCell.words("Ends the selected window or program."))
                .row(GuideCell.code("End Process"), GuideCell.words("Ends the selected program."))
                .row(GuideCell.code("Switch To"), GuideCell.words("Brings the selected window to the front."))
                .paragraph("Next to the buttons the window warns: \"Ending a program may lose unsaved work.\""
                        + " and \"Ending the system restarts the machine.\" A system, a desktop or a service"
                        + " cannot be ended from here.")
                .steps("Right-click the panel, and open the Task Manager.",
                        "Select the window or the program in the list.",
                        "If the program still lets you, save your work in it first.",
                        "Press End Task for a window, or End Process for a program.")
                .ifSomethingGoesWrong("End Task stays greyed out.",
                        "Select a window or a program's row first.",
                        "The row you want to close is the system, a desktop or a service.",
                        "The machine itself cannot be ended from here. Close the program that owns that row"
                                + " instead.")
                .seeAlso("jsc:programs", "jsc:help_on_a_computer")
                .register();
        guide.page("calculator_paint_exceed", section).titled("Calculator, Paint and Exceed")
                .icon(ComputingModule.MONITOR)
                .paragraph("Three programs of the desktop work with numbers and pictures: a Calculator for"
                        + " expressions, Paint for pictures, and Exceed for a sheet of cells that can ask the"
                        + " network what it holds.")
                .subheading("The Calculator")
                .paragraph("The Calculator works out the expression you type, with brackets, powers, roots,"
                        + " logarithms and the trigonometric functions. Type with the keys or the keyboard, and"
                        + " press = or [[Enter]] to get the answer. The expression holds at most 256 characters.")
                .table("Calculator keys", "Key", "What it does")
                .row(GuideCell.code("DEG"),
                        GuideCell.words("Switches sin, cos and tan between degrees and radians."))
                .row(GuideCell.code("C"), GuideCell.words("Clears the expression."))
                .row(GuideCell.code("<-"), GuideCell.words("Deletes one character."))
                .row(GuideCell.code("="), GuideCell.words("Works out the answer. Enter does the same."))
                .row(GuideCell.code("sin, cos, tan"), GuideCell.words("The trigonometric functions."))
                .row(GuideCell.code("^"), GuideCell.words("Raises a number to a power."))
                .row(GuideCell.code("sqrt"), GuideCell.words("The square root."))
                .row(GuideCell.code("ln"), GuideCell.words("The natural logarithm."))
                .row(GuideCell.code("log"), GuideCell.words("The logarithm."))
                .row(GuideCell.code("!"), GuideCell.words("The factorial of the number before it."))
                .row(GuideCell.code("pi, e"), GuideCell.words("The constants pi and e."))
                .ifSomethingGoesWrong("The display shows Error.",
                        "Check the brackets, the signs and the function names, then type the expression again.")
                .subheading("Paint")
                .paragraph("Paint draws a picture on a canvas of up to 128 by 128 pixels, on one layer, in a"
                        + " palette of 256 colours. A saved picture is a .pix file of about a kilobyte.")
                .table("Paint tools", "Tool", "What it does")
                .row(GuideCell.words("Pencil"), GuideCell.words("Draws with the chosen colour."))
                .row(GuideCell.words("Eraser"), GuideCell.words("Erases part of the picture."))
                .row(GuideCell.words("Fill"), GuideCell.words("Fills an area with the chosen colour."))
                .row(GuideCell.words("Dropper"), GuideCell.words("Takes a colour from the picture."))
                .row(GuideCell.words("Line, Rectangle, Ellipse"), GuideCell.words("Draws a shape of that kind."))
                .paragraph("The buttons along the top are New, Open, Save, Undo, the zoom, Wallpaper and Print."
                        + " Undo goes back through up to 24 steps. Wallpaper makes the picture the desktop"
                        + " background. Print sends the picture to a printer, one picture to a page"
                        + " ([](jsc:printers)).")
                .ifSomethingGoesWrong("Paint says \"Picture too large to save\".",
                        "Make the picture smaller, then save it again.",
                        "Paint says \"That picture is too large to open here\".",
                        "Open a smaller picture.",
                        "Paint refuses a file and says it is not a picture.",
                        "Open a file that ends in .pix.")
                .subheading("Exceed")
                .paragraph("Exceed is a sheet of cells, as a spreadsheet is. It has 26 columns, from A to Z, and"
                        + " 99 rows. Each cell has a name made of its column letter and its row number, such as"
                        + " B7. Its files are plain comma-separated text (.csv).")
                .define("Cell", "One box of a sheet, named by its column letter and its row number.")
                .define("Formula", "The text of a cell that starts with an equals sign, and works out a value"
                        + " from other cells.")
                .define("Live cell", "A cell whose number comes from the network, and so shows in its own"
                        + " colour.")
                .paragraph("A formula uses the four arithmetic signs, brackets, the power sign, the names of"
                        + " other cells, and the functions below. A live cell is one that uses QTY, FREE or"
                        + " SERVERS. Exceed asks the network again after a wait of 3 seconds, so typing does not"
                        + " send a request for every key.")
                .table("Exceed functions", "Function", "What it gives")
                .row(GuideCell.code("SUM"), GuideCell.words("The total of a range of cells."))
                .row(GuideCell.code("MIN"), GuideCell.words("The smallest value of a range."))
                .row(GuideCell.code("MAX"), GuideCell.words("The largest value of a range."))
                .row(GuideCell.code("AVG"), GuideCell.words("The average of a range."))
                .row(GuideCell.code("COUNT"), GuideCell.words("How many values a range holds."))
                .row(GuideCell.code("QTY(\"item_id\")"),
                        GuideCell.words("How many of an item the network holds, given by the item's id in quotes."))
                .row(GuideCell.code("FREE()"), GuideCell.words("How much room the network has left."))
                .row(GuideCell.code("SERVERS()"), GuideCell.words("How many servers are on the network."))
                .table("Errors in a cell", "Shown", "What it means")
                .row(GuideCell.code("#ERR"),
                        GuideCell.words("The formula cannot be worked out: bad syntax, a bad reference or an"
                                + " unknown function."))
                .row(GuideCell.code("#LOOP"),
                        GuideCell.words("The cell refers to itself, directly or through other cells."))
                .row(GuideCell.code("#NONE"),
                        GuideCell.words("The cell asks the network for something the machine has not reported"
                                + " yet."))
                .paragraph("The buttons of Exceed are Open, Save, Save As and Refresh.")
                .ifSomethingGoesWrong("Exceed says \"Sheet too large to save\".",
                        "Remove the cells you do not need, then save again.",
                        "Exceed asks whether to open another sheet and lose your changes.",
                        "Answer no, and save the sheet first.",
                        "A cell shows #ERR.",
                        "Check the formula's brackets, the function names and the cell names.",
                        "A cell shows #LOOP.",
                        "Remove the reference that leads back to the cell.",
                        "A cell shows #NONE.",
                        "Check the item's id, or wait for the network to report.")
                .seeAlso("jsc:programs", "jsc:help_on_a_computer")
                .register();
        guide.page("archiver", section).titled("67ark").icon(ComputingModule.CD_ROM)
                .paragraph("67ark packs many files into one archive that weighs less, and it takes them back"
                        + " out again. That is how a small disk is made to stretch: the archive takes less room"
                        + " than the files it holds.")
                .define("Archive", "One file that holds other files, packed to weigh less. It ends in .ark.")
                .define("Pack", "To put files into an archive, which stores them in less room.")
                .define("Extract", "To copy files back out of an archive.")
                .paragraph("The window does one of two jobs, and it says which one you are doing. Either an archive is"
                        + " open, and its contents are listed, or you are putting an archive together from files"
                        + " you chose on the disk.")
                .table("Columns of the archive list", "Column", "What it shows")
                .row(GuideCell.words("Name"), GuideCell.words("The name of the file inside the archive."))
                .row(GuideCell.words("Size"),
                        GuideCell.words("The size of the file, in bytes, kilobytes or megabytes."))
                .row(GuideCell.words("Saved"),
                        GuideCell.words("The percentage by which the archive is smaller than the files."))
                .steps("Open an archive, or start a new one with New.",
                        "Press Add, and choose the files you want to pack.",
                        "Press Pack. The server does the packing, so the saving on disk is real.",
                        "Press Extract for the files you chose, Extract all for every file, or Take out into a"
                                + " folder.")
                .paragraph("After packing, the window asks whether to delete the original files or keep them.")
                .ifSomethingGoesWrong("67ark says that file is not an archive.",
                        "Open only a file that ends in .ark.",
                        "67ark says an archive holds at most 512 files.",
                        "Pack the files into two archives.")
                .seeAlso("jsc:programs", "jsc:this_pc_and_desktop_tools")
                .register();
        guide.page("games", section).titled("Minesweeper, Solitaire and Snake").icon(ComputingModule.MONITOR)
                .paragraph("Three games run on the desktop systems. Each one is installed like any other program"
                        + " ([](jsc:programs)), and needs 1 megabyte (MB) of memory.")
                .subheading("Minesweeper")
                .paragraph("Reveal a cell to see how many mines touch it. A number shows that count, in its own"
                        + " colour. An empty cell opens the cells around it. You flag a cell you think holds a"
                        + " mine.")
                .define("Flag", "A mark you place on a cell that you think holds a mine.")
                .table("Minesweeper levels", "Level", "Button", "Grid", "Mines")
                .row(GuideCell.words("Beginner"), GuideCell.code("Beg"), GuideCell.words("9 by 9"),
                        GuideCell.words("10"))
                .row(GuideCell.words("Intermediate"), GuideCell.code("Int"), GuideCell.words("16 by 16"),
                        GuideCell.words("40"))
                .row(GuideCell.words("Expert"), GuideCell.code("Exp"), GuideCell.words("16 by 30"),
                        GuideCell.words("99"))
                .paragraph("The game starts on Beginner.")
                .subheading("Solitaire")
                .paragraph("Solitaire is Klondike. You draw one card at a time from the stock, the face-down"
                        + " pile, into the waste, which holds the cards you drew. The seven columns of cards are"
                        + " the tableau, and the four piles, one for each suit, are the foundations, built from"
                        + " ace up to king.")
                .define("Tableau", "The seven columns of cards in Solitaire, where the cards are laid out in a"
                        + " row.")
                .define("Foundation", "One of the four piles in Solitaire, where a suit is built from ace up to"
                        + " king.")
                .table("Points in Solitaire", "Move", "Points")
                .row(GuideCell.words("Waste to tableau"), GuideCell.words("5 points"))
                .row(GuideCell.words("Turn over a card"), GuideCell.words("5 points"))
                .row(GuideCell.words("To a foundation"), GuideCell.words("10 points"))
                .row(GuideCell.words("From a foundation back to the tableau"), GuideCell.words("minus 15 points"))
                .row(GuideCell.words("Waste back to the stock"), GuideCell.words("minus 100 points"))
                .paragraph("The window shows the score and the moves, and it says \"You win\" when the game is"
                        + " done. The New button starts another game.")
                .subheading("Snake")
                .paragraph("Snake runs on a grid of 20 by 12 cells. You eat the food to grow, and each food is"
                        + " worth 10 points. The snake starts at a length of 4. Press an arrow key to start.")
                .paragraph("The speed sets how many ticks pass between two steps. A tick is the game's beat, and"
                        + " 20 ticks make a second ([](jsc:units_and_ticks)).")
                .table("Speeds of Snake", "Speed", "A step every")
                .row(GuideCell.words("Slow"), GuideCell.words("6 ticks"))
                .row(GuideCell.words("Normal"), GuideCell.words("4 ticks"))
                .row(GuideCell.words("Fast"), GuideCell.words("2 ticks"))
                .paragraph("With walls on, running into a wall ends the game. With walls off, the grid wraps"
                        + " round, and only running into yourself ends the game. The window shows the score, the"
                        + " best score and the length, and it says \"Game over\" when you lose.")
                .seeAlso("jsc:programs", "jsc:units_and_ticks")
                .register();
    }

    private static void networkTools(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("network_tools").titled("Programs for the network")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        guide.page("network_interactor", section).titled("The Network window")
                .icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("The Network window reaches the network's storage and its work from this computer."
                        + " Its desktop icon is called Network, and its window is titled Network Interactor. It"
                        + " comes with every desktop, and it does what the MC-NET terminal does"
                        + " ([](jsc:mc_net)).")
                .paragraph("From it you ask the network for items, take items from local storage into your"
                        + " inventory, send items from local storage back to the network, and ask the network"
                        + " to craft items. The items you ask for come into the local storage of this computer"
                        + " first.")
                .define("Local storage", "The disk storage of this computer, as opposed to the storage the"
                        + " network holds.")
                .define("Request", "To ask the network to put items into the local storage of this computer.")
                .define("Favourite", "An item you star, so that it stays in its own tab.")
                .subheading("The window")
                .table("Parts of the Network window", "Part", "What it does")
                .row(GuideCell.words("Toolbar"),
                        GuideCell.words("The search field, and the buttons Sort, Mod and Category."))
                .row(GuideCell.words("Tabs"), GuideCell.words("Five tabs, and the Favourites tab at the end."))
                .row(GuideCell.words("Grid"),
                        GuideCell.words("The items of the tab, in a well of cells with a caption over it."))
                .row(GuideCell.words("Details panel"),
                        GuideCell.words("The item you chose, its actions and its details."))
                .row(GuideCell.words("Inventory band"),
                        GuideCell.words("The slots of your inventory, at the foot of the window."))
                .row(GuideCell.words("Grips"),
                        GuideCell.words("Two handles that reshape the window: one adds columns to the grid, and"
                                + " the other shows from 1 to 4 rows of your inventory."))
                .row(GuideCell.words("Status bar"),
                        GuideCell.words("The Mainframe's state, the counts and a storage gauge."))
                .paragraph("Drag a grip to change the size. The window remembers its size.")
                .subheading("The tabs")
                .table("Tabs of the Network window", "Tab", "What it lists", "When it is empty")
                .row(GuideCell.words("Status"), GuideCell.words("The network card."),
                        GuideCell.words("Never empty."))
                .row(GuideCell.words("Local"), GuideCell.words("Items on this computer's own disks."),
                        GuideCell.words("Nothing on this computer's disks."))
                .row(GuideCell.words("Network"), GuideCell.words("Every item the network holds."),
                        GuideCell.words("Nothing on the network."))
                .row(GuideCell.words("Crafting"),
                        GuideCell.words("Items the network can craft from its patterns."),
                        GuideCell.words("No patterns on the network."))
                .row(GuideCell.words("Operations"),
                        GuideCell.words("The network's Operations, running ones first."),
                        GuideCell.words("No operations on the network."))
                .row(GuideCell.words("Favourites"), GuideCell.words("The items you have starred."),
                        GuideCell.words("Nothing starred yet."))
                .paragraph("Press [[F]] on an item, or its star in the details, to star it. The status card on the"
                        + " Status tab shows the network's state. Its header says This network, and then Mainframe"
                        + " online or Mainframe offline. Below it, a STORAGE gauge shows the disk space in use and"
                        + " the items held. Then come the counts of item types, servers, craftable items and"
                        + " favourites, and the count of Operations running.")
                .subheading("Requests and the inventory")
                .paragraph("Press [[Enter]] on an item, or click it, to open its dialog. On the Network tab, Request"
                        + " asks the network to put the items into the local storage of this computer. On the"
                        + " Local tab, To Inventory takes items out of local storage into your inventory, as far"
                        + " as your inventory has room. To Network sends items from local storage to the network,"
                        + " and anything the network does not take comes back to local storage.")
                .paragraph("Adv opens an advanced panel, where you choose the servers to pull from and where the"
                        + " items go. Fluids and chemicals cannot go into your inventory.")
                .paragraph("The quantity buttons move the amount by 1, 10, 100 or 1,000, up or down, and Max takes"
                        + " the most allowed. You can also type digits, and [[Backspace]] deletes the last one."
                        + " With"
                        + " several items chosen, each item is asked for the quantity, or what the network holds"
                        + " of it if that is less.")
                .paragraph("The Prio button sets the [priority](jsc:priorities) of the request. It cycles through"
                        + " LOW, MED-, MED, MED+ and HIGH, and it starts at the default, MEDIUM.")
                .subheading("Searching, filtering and sorting")
                .paragraph("Press [[/]] on the grid to move to the search field. Type part of a name, and the list"
                        + " shows the items whose names match. [[Esc]] leaves the field. The Sort button cycles"
                        + " A-Z, MOST and LEAST: alphabetical, largest total first, or smallest total first. The"
                        + " Mod and Category buttons open menus that narrow the list to one mod, or one category"
                        + " of items.")
                .paragraph("The search and the filters apply to the tab you are on, not to the Status or the"
                        + " Operations tab.")
                .table("Keys on the grid", "Key", "What it does")
                .row(GuideCell.code("Arrow keys"), GuideCell.words("Move the dotted cell."))
                .row(GuideCell.code("Shift"), GuideCell.words("With an arrow, extends the selection."))
                .row(GuideCell.code("Enter"), GuideCell.words("Opens the dialog for the selected items."))
                .row(GuideCell.code("C"),
                        GuideCell.words("Opens the Craft dialog for the cell's item, if it can be crafted."))
                .row(GuideCell.code("F"), GuideCell.words("Stars or unstars the selection. If all are starred,"
                        + " all are unstarred."))
                .row(GuideCell.code("Ctrl+A"), GuideCell.words("Selects every item of the tab."))
                .row(GuideCell.code("/"), GuideCell.words("Moves to the search field."))
                .subheading("Crafting")
                .paragraph("The Crafting tab lists the items the network can craft from its"
                        + " [patterns](jsc:pattern_encoders). A pattern is the recipe the network knows, and a"
                        + " [Crafting Computer](jsc:crafting_computers) loads them.")
                .paragraph("Press [[C]], or [[Enter]] on a craftable item, to open the Craft dialog. It shows a"
                        + " plan: the recipe it uses, the machines and stages, and what each input costs in stock."
                        + " Craft makes the whole amount, Partial makes what can be made now, and [[Enter]] makes"
                        + " the whole amount too.")
                .paragraph("When an item can be made in more than one way, up to three recipe cards sit side by"
                        + " side. [[Left]] and [[Right]] change the card, and the computer remembers the choice for"
                        + " each item. The estimate, marked EST, shows a time, or EST -- while the plan is still"
                        + " being worked out.")
                .subheading("Updating an item")
                .paragraph("The dialog of a single item also has an Update... button. It opens a window with"
                        + " three tabs: Enchant, Smelt and Repair. A tab is greyed out when this computer has no"
                        + " card for it, or when the item does not take it. The cards are the personal-use cards"
                        + " of the [Workshop](jsc:workshop). The window closes once the network has taken the"
                        + " update.")
                .subheading("Operations")
                .paragraph("The Operations tab lists the network's [Operations](jsc:what_operations_are). The"
                        + " running ones come first, then the recent log, which keeps the last 32 Operations. A"
                        + " row shows its state in short form:")
                .table("States in the Operations tab", "Short", "Full name")
                .row(GuideCell.code("done"), GuideCell.words("Completed"))
                .row(GuideCell.code("part"), GuideCell.words("Completed (partial)"))
                .row(GuideCell.code("fail"), GuideCell.words("Failed"))
                .row(GuideCell.code("run"), GuideCell.words("Processing"))
                .row(GuideCell.code("wait"), GuideCell.words("Waiting"))
                .row(GuideCell.code("lock"), GuideCell.words("Resource locked"))
                .row(GuideCell.code("pend"), GuideCell.words("Pending"))
                .row(GuideCell.code("drop"), GuideCell.words("Discarded"))
                .paragraph("Open a row to see how much of the Operation has moved, and its parts: one"
                        + " sub-operation for each machine that works on it, or its sources. With a Supercomputer,"
                        + " the caption also shows how many parallel crafts run, out of the number the"
                        + " Supercomputer has.")
                .ifSomethingGoesWrong("The status card says Mainframe offline.",
                        "This window cannot reach the network. Place the computer on a network whose Mainframe is"
                                + " running.",
                        "The Network tab says Nothing on the network.",
                        "The network's disks hold nothing yet. Put items on the network first.",
                        "The Crafting tab says No patterns on the network.",
                        "No Crafting Computer holds a pattern yet. Load .craft files on a Crafting Computer.",
                        "The Crafting tab lists fewer items than you expect.",
                        "The tab shows only what the patterns allow. Load more patterns on a Crafting"
                                + " Computer.",
                        "The Craft dialog shows EST --.",
                        "The plan is still being prepared, or no route exists yet. Wait a moment, then look"
                                + " again.")
                .seeAlso("jsc:mc_net", "jsc:what_a_network_is", "jsc:states_and_queues", "jsc:crafting_computers")
                .register();
        guide.page("remote_control", section).titled("Remote Control").icon(ComputingModule.MONITOR)
                .paragraph("Remote Control takes over another machine of the network, and puts its screen on this"
                        + " monitor. It is the point-and-click twin of [ssh](jsc:remote_access), the secure shell."
                        + " It matters most for a server in a rack, which has no screen of its own"
                        + " ([](jsc:server_racks)).")
                .paragraph("The window lists the machines on this network under the heading Machines on this"
                        + " network. Each row shows the machine's kind and its system, and whether it is up or"
                        + " off. Select a machine, and press Take over. Taking over puts the machine's session on"
                        + " this monitor: its power-on self-test, its firmware, its terminal, or its full desktop"
                        + " ([](jsc:monitors)).")
                .table("Parts of Remote Control", "Part", "What it shows")
                .row(GuideCell.words("Heading"), GuideCell.words("Machines on this network."))
                .row(GuideCell.words("Rows"),
                        GuideCell.words("The machine's kind and its system, and whether it is up or off."))
                .row(GuideCell.words("Button"),
                        GuideCell.words("Take over, or Select a machine when no row is chosen."))
                .ifSomethingGoesWrong("A machine is listed as off.",
                        "Switch the machine on before you can take it over.",
                        "The list says No other machine is reachable.",
                        "Check that this computer is on the network whose machines you want to reach.")
                .seeAlso("jsc:mainframes", "jsc:remote_access", "jsc:monitors")
                .register();
        guide.page("storage_insights", section).titled("Storage Insights").icon(ComputingModule.CD_ROM)
                .paragraph("Storage Insights shows where the network's storage went. It is a dashboard of the"
                        + " network's contents: the totals, the biggest types of item, the types that are running"
                        + " low, and how full each server is. You open it when you want to know what the network"
                        + " is short of.")
                .table("Tiles of Storage Insights", "Tile", "What it counts")
                .row(GuideCell.words("TYPES"), GuideCell.words("The number of item types the network holds."))
                .row(GuideCell.words("TOTAL"), GuideCell.words("The total of the network's contents."))
                .row(GuideCell.words("LOW"), GuideCell.words("The number of item types running low."))
                .table("Sections of Storage Insights", "Section", "What it shows")
                .row(GuideCell.words("TOP ITEMS"), GuideCell.words("The biggest types, as a bar chart."))
                .row(GuideCell.words("LOW STOCK"),
                        GuideCell.words("The types under the threshold, or all stocked when none are."))
                .row(GuideCell.words("BY SERVER"), GuideCell.words("How full each server is."))
                .define("Threshold", "The count under which an item type is listed as running low. It starts at"
                        + " 64 items.")
                .paragraph("Use the control labelled Threshold to change the count. Click a type to see its"
                        + " detail view. The detail view has a Back button, the total of the type, the places it"
                        + " is STORED IN, what it is USED TO MAKE, and the [buses](jsc:buses) that filter it. A"
                        + " type no bus filters is marked as not on any bus filter.")
                .paragraph("The search field, which says search item..., narrows the list. The dashboard asks the"
                        + " network again on its own, every so often, so the numbers keep up with the network.")
                .ifSomethingGoesWrong("The list says no match.",
                        "No item type fits the search. Try a shorter word.",
                        "The dashboard shows no numbers.",
                        "The network holds no items yet. Put items on the network, and the numbers will follow.")
                .seeAlso("jsc:buses", "jsc:items_as_data", "jsc:storage")
                .register();
    }

    private static void socialAndMedia(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("social_and_media").titled("Talking and making")
                .icon(ComputingModule.USB_FLASH_DRIVE).in(chapter).register();
        guide.page("messenger", section).titled("Messenger").icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("Messenger lets the people on a network talk to each other. Two programs make it work."
                        + " On the desktop, the program is called Midsoft Messenger, and it is the window you talk"
                        + " in. The Messenger Service keeps the conversations, and it runs on a server in a rack.")
                .define("Midsoft Messenger", "The desktop window of Messenger, where you read and type your"
                        + " messages.")
                .define("Messenger Service", "The service on a rack server that keeps a network's conversations"
                        + " for every Midsoft Messenger on it.")
                .paragraph("The Messenger Service installs only on a server in a rack of the network"
                        + " ([](jsc:server_racks)). A Personal Computer or a Mainframe cannot host it.")
                .subheading("The window")
                .table("Parts of Messenger", "Part", "What it does")
                .row(GuideCell.words("Roster"), GuideCell.words("The people on the network, under On the network;"
                        + " Everybody is the one room they all read."))
                .row(GuideCell.words("Conversation"), GuideCell.words("What has been said, on the right."))
                .row(GuideCell.words("Compose line"), GuideCell.words("Where you type a message."))
                .row(GuideCell.words("Send"), GuideCell.words("Sends the message."))
                .row(GuideCell.words("Nudge"), GuideCell.words("Sends a nudge to the others."))
                .paragraph("Press [[Enter]] to send a message. A message holds at most 200 characters. The window asks"
                        + " the server for news every 2 seconds. When you nudge, the other computers show a line"
                        + " that says a nudge was sent, with the name of the sender.")
                .subheading("Limits and cost")
                .paragraph("The service keeps the last 500 messages. When there are more, the oldest fall off the"
                        + " end. Its history lives on the server, not on either computer, so a conversation is"
                        + " there when you sit at any computer of the network. It goes with the server if the"
                        + " server is taken away.")
                .paragraph("The service costs 8 MB of memory before anyone connects, and 2 MB more for each person"
                        + " who is connected. Each message takes its own text and a little overhead. The bar at the"
                        + " foot of the roster shows what the service weighs.")
                .ifSomethingGoesWrong("The window says No Messenger Service on this network.",
                        "Install the Messenger Service on a server in a rack of this network, and let it run.")
                .seeAlso("jsc:server_racks", "jsc:server_router", "jsc:mainframes")
                .register();
        guide.page("soundfoundry", section).titled("Soundfoundry").icon(ComputingModule.CD_ROM)
                .paragraph("Soundfoundry plays songs on the computer, from a file or from the network. It is on the"
                        + " Frames, Linux and FreeBSD desktops, but not on UNIX. It needs a Legacy computer or newer.")
                .paragraph("Soundfoundry needs a sound card. Without one, the song line says NO SOUND CARD"
                        + " INSTALLED, and the volume, balance and playlist buttons turn grey. Install a sound card"
                        + " in the computer ([](jsc:sound_cards)), and choose where the sound goes"
                        + " ([](jsc:speakers)).")
                .subheading("The player")
                .table("Output button", "Label", "Plays the sound through")
                .row(GuideCell.code("OUT -"), GuideCell.words("Nothing."))
                .row(GuideCell.code("OUT MON"), GuideCell.words("The monitor."))
                .row(GuideCell.code("OUT SPK"), GuideCell.words("The speakers."))
                .row(GuideCell.code("OUT MON+SPK"), GuideCell.words("The monitor and the speakers."))
                .paragraph("The window also has SHUFFLE and REPEAT buttons, and a PL button that shows the playlist."
                        + " ADD, REM and SEL work on the playlist. NET opens the network share window. The volume"
                        + " and the balance are sliders, and the song line shows MONO or STEREO, and its bit rate"
                        + " and sample rate.")
                .paragraph("The menu holds the ways to fill the playlist: open a file or a folder, add to the"
                        + " playlist, remove or keep the selected songs, sort the list, and save or open a"
                        + " playlist. A playlist holds at most 500 songs, and its name is at most 32 characters.")
                .define("Playlist", "The list of songs queued to play, in order.")
                .subheading("Songs from the network")
                .paragraph("The network tabs are SEARCH, DOWNLOADS and SHARED. A search finds every word you type"
                        + " in a song's title, artist, album or file name, in any order and any case. A query holds"
                        + " at most 64 characters. Songs from the network arrive at the speed of the slowest cable"
                        + " on the way, and several downloads at once share that speed.")
                .paragraph("A song plays from one of three places: a file on this computer's disk, a song of the"
                        + " library that this computer already has on its own disk, or a stream from the"
                        + " Soundfoundry Server the computer picked. A downloaded or imported song is kept in the"
                        + " music folder of the computer.")
                .define("Stream", "A song sent while it plays, instead of kept on the computer first.")
                .define("Share", "A song sent to the server's library, so that the others on the network can play"
                        + " it.")
                .subheading("Soundfoundry Server")
                .paragraph("The Soundfoundry Server runs on a server in a rack. It streams the music catalogue to"
                        + " every Soundfoundry on the network ([](jsc:server_racks)). Its library is the songs in"
                        + " its own music folder, and each computer's shared songs sit in a folder named after that"
                        + " computer.")
                .define("Soundfoundry Server", "The service on a rack server that streams the network's music to"
                        + " every Soundfoundry you play it on.")
                .paragraph("The server streams only while it has the memory to hold the stream open. So a computer"
                        + " asks to listen before it plays, and asks again every little while.")
                .ifSomethingGoesWrong("The song line says NO SOUND CARD INSTALLED.",
                        "Install a sound card in the computer.",
                        "A network song does not play.",
                        "No Soundfoundry Server is on the network, or the server has no memory to spare for the"
                                + " stream. Install a Soundfoundry Server on a server in a rack of the network.")
                .seeAlso("jsc:sound_cards", "jsc:speakers")
                .register();
        guide.page("knot_and_knothub", section).titled("Knot and KnotHub").icon(ComputingModule.CD_ROM)
                .paragraph("Knot keeps the history of a file that the players of a network are still working on."
                        + " You send the file to the network's repository, and Knot keeps it as a revision: one saved"
                        + " state of the file, numbered r1, r2 and so on.")
                .define("Revision", "One saved state of a file in Knot, numbered r1, r2 and so on.")
                .define("Push", "To send the file's current state to the repository as a new revision.")
                .define("Pull", "To fetch the network's copy of a file.")
                .paragraph("Knot holds the source that is still being worked on, before it becomes a finished"
                        + " package ([](jsc:programs)). Push reads the file from this computer's own disk, not from"
                        + " text typed in the window, so the network keeps what the machine really holds. The"
                        + " history runs along the top of the window, and the changes sit underneath.")
                .paragraph("A file keeps at most 64 revisions, and the oldest go first. A repository keeps at most"
                        + " 64 files. Each revision is kept whole, not as a difference from the last one, so the"
                        + " computer that holds the repository needs the disk space for all of them.")
                .paragraph("KnotHub keeps the repository for the network, and it runs on a server in a rack"
                        + " of the network ([](jsc:server_racks)). The"
                        + " [Book of Sigma](jsc:knot) explains Knot for programs.")
                .ifSomethingGoesWrong("The window says No KnotHub on this network.",
                        "Install KnotHub on a server in a rack of this network, and let it run.",
                        "Push answers No change in r, followed by a number.",
                        "The file has not changed since its last revision. Change the file on your disk, then push"
                                + " again.")
                .seeAlso("jsc:knot", "jsc:server_racks", "jsc:programs")
                .register();
    }

    private static void gettingHelp(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("getting_help").titled("Getting help")
                .icon(ComputersGuide.MANUAL).in(chapter).register();
        guide.page("help_on_a_computer", section).titled("Help on a computer").icon(ComputersGuide.MANUAL)
                .paragraph("Every desktop has a Help program, under a name of its own. It ships with the desktop,"
                        + " so you do not install it. It reads the manuals of the series, and beside them the"
                        + " manual pages of the machine's own commands. This manual is not only in your hands:"
                        + " every computer can read it too.")
                .table("Help on each desktop", "Desktop", "Its Help is called")
                .row(GuideCell.words("Frames 95"), GuideCell.words("Help Topics"))
                .row(GuideCell.words("Frames XP and 7"), GuideCell.words("Help and Support"))
                .row(GuideCell.words("Frames 10 and 11"), GuideCell.words("Get Help"))
                .row(GuideCell.words("KDE"), GuideCell.words("Help Center"))
                .row(GuideCell.words("GNOME and Cinnamon"), GuideCell.words("Help"))
                .row(GuideCell.words("CDE"), GuideCell.words("Help Viewer"))
                .paragraph("At a prompt, several commands give you help. Each one works on a different set of"
                        + " systems.")
                .paragraph("The help command lists the commands, or shows how one of them is used. Type help alone"
                        + " to list them, or help and a command's name to see its use. It works on every system"
                        + " except FreeBSD. On MC-DOS and MC-NET it takes the whole screen. On the other systems it"
                        + " prints its text ([](jsc:help_command)).")
                .paragraph("The man command opens the manual page of a command, on Linux, UNIX System V and"
                        + " FreeBSD. A topic such as graphics-cards opens as a page of section 7"
                        + " ([](jsc:man_pages)). The whatis and apropos commands search the manual"
                        + " ([](jsc:whatis_and_apropos)).")
                .paragraph("On Linux the info command reads the manuals as a tree of nodes. Its keys are these:")
                .table("Keys of info", "Key", "What it does")
                .row(GuideCell.code("n"), GuideCell.words("Goes to the next node."))
                .row(GuideCell.code("p"), GuideCell.words("Goes to the previous node."))
                .row(GuideCell.code("u"), GuideCell.words("Goes up to the node above."))
                .row(GuideCell.code("l"), GuideCell.words("Goes back to the node you were at."))
                .row(GuideCell.code("Tab"), GuideCell.words("Picks a link."))
                .row(GuideCell.code("Enter"), GuideCell.words("Follows the picked link."))
                .row(GuideCell.code("m"), GuideCell.words("Goes to a menu item, by its name."))
                .row(GuideCell.code("s"), GuideCell.words("Searches the manuals."))
                .row(GuideCell.code("h"), GuideCell.words("Shows the keys."))
                .row(GuideCell.code("q"), GuideCell.words("Leaves info."))
                .paragraph("With no name, info opens the directory of every manual.")
                .ifSomethingGoesWrong("man says No manual entry for a name.",
                        "Check the name. For the entry on graphics cards, type man graphics-cards, not man"
                                + " graphics.",
                        "help does not work on FreeBSD.",
                        "Use man on FreeBSD instead ([](jsc:man_pages)).",
                        "info does not work on your system.",
                        "info is for Linux only. Use man instead ([](jsc:man_pages)).")
                .seeAlso("jsc:library", "jsc:finding_help_overview", "jsc:help_command", "jsc:info_keys")
                .register();
    }
}
