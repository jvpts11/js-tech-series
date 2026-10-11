/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the MC-DOS chapter of the Frames Resource Kit: the prompt, the drives and the paths, installing the
 * system, the files it keeps on its disk, the MC-DOS Shell, Midsoft Diagnostics and the full-screen help.
 */
final class McDosPages {

    private McDosPages() {
    }

    /** Declares the sections and entries of MC-DOS, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        basicsSection(guide, chapter);
        programsSection(guide, chapter);
    }

    private static void basicsSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("dos_basics").titled("Working in MC-DOS")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        prompt(guide, section);
        install(guide, section);
        systemFiles(guide, section);
    }

    private static void programsSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("dos_programs").titled("MC-DOS's programs")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        dosShell(guide, section);
        msd(guide, section);
        help(guide, section);
    }

    private static void prompt(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_prompt", section).titled("The prompt, the drives and the paths")
                .paragraph("MC-DOS waits for you at the prompt, the line where you type a command and press [[Enter]]."
                        + " The prompt shows where you are: the folder you are in, then a greater-than sign. At the top"
                        + " of the system disk it reads C:\\>, and in a folder called SYSTEM it reads C:\\SYSTEM>."
                        + " A command works on the folder the prompt shows, unless you name another place.")
                .paragraph("Every disk and drive has a drive letter: a letter and a colon, such as C:. Type the letter"
                        + " and the colon to change drive, for example D:. You then stand at the top folder of that"
                        + " drive. MC-DOS keeps no current folder for each drive, so a drive does not remember where"
                        + " you were.")
                .table("The drive letters", "Letter", "Which drive it is")
                .row(GuideCell.code("C:"), GuideCell.words("The system disk, or the first data disk when there is"
                        + " no system disk."))
                .row(GuideCell.words("D: and up"), GuideCell.words("The data disks in the disk slots, in slot order;"
                        + " the first one is D: when there is a system disk."))
                .row(GuideCell.words("After the disks"), GuideCell.words("The readers, the Dock Station bays and the"
                        + " case-front drives, each keeping its letter."))
                .row(GuideCell.code("A: and B:"),
                        GuideCell.words("Never. The machine has no drive A: and no drive B:."))
                .paragraph("Removable drives are the ones you join to the computer ([](jsc:drives)). Data disks are the"
                        + " disks in its slots ([](jsc:disks)).")
                .table("Typed at the prompt", "Typed", "What it means")
                .row(GuideCell.code("D:"), GuideCell.words("Changes to drive D: and its top folder."))
                .row(GuideCell.code("D:DIR"), GuideCell.words("The folder DIR at the top of drive D:."))
                .row(GuideCell.code("\\"), GuideCell.words("The top folder of the current drive."))
                .row(GuideCell.code("\\DIR"), GuideCell.words("The folder DIR at the top of the current drive."))
                .row(GuideCell.code(".."), GuideCell.words("The folder above; at the top folder it stays put, with"
                        + " no error."))
                .paragraph("A path is the full address of a file or a folder: its drive, its folders and its name."
                        + " A path can join the folders with a backslash or a slash, so C:\\DOS\\README.TXT and"
                        + " C:/DOS/README.TXT name the same file.")
                .paragraph("When you type only a word, MC-DOS takes it for a built-in command first, and then for a"
                        + " program. It looks for the program in the folder you are in, then in the folders of the"
                        + " search path, the list kept in the PATH setting.")
                .paragraph("To run a program that is in another folder, type its full path, such as C:\\DOS\\HELLO.")
                .paragraph("The commands for folders are dir, cd, mkdir and rmdir. dir lists a folder, cd shows the"
                        + " current folder or changes it, mkdir makes a folder and rmdir removes an empty one."
                        + " [Working with files and folders](jsc:working_with_files) explains them all.")
                .paragraph("Type a command's name, a space and /? to read its page. Typing help dir opens the same page"
                        + " ([](jsc:dos_help)).")
                .define("Prompt", "The line where MC-DOS waits for a command. It shows the current folder and a"
                        + " greater-than sign.")
                .define("Drive letter", "The name a disk or a drive has on MC-DOS: a letter and a colon, such as C:.")
                .define("Path", "The full address of a file or a folder: its drive, its folders and its name.")
                .define("Search path", "The list of folders MC-DOS searches for a program when you type only the"
                        + " program's name.")
                .ifSomethingGoesWrong(
                        "The prompt says \"command not found: HELLO\".",
                        "No command or program of that name is in the folder you are in or on the search path."
                                + " Check the spelling, or type the full path, such as C:\\DOS\\HELLO."
                                + " [Finding a command](jsc:finding_a_command) explains the rest.",
                        "The prompt says E:\\ The system cannot find the drive specified.",
                        "The machine has no drive with that letter. Type one of the letters in the table above.",
                        "The prompt says \"D:\\ The device is not ready.\"",
                        "The drive has no disk in it. Put a disk or a medium in the drive ([](jsc:drives)).",
                        "The prompt says no system disk or OS installed.",
                        "The computer has no system disk to start from. Put a disk in a disk slot, or install MC-DOS on"
                                + " one ([](jsc:dos_install)).",
                        "The prompt says The system cannot find the path specified.",
                        "The folder does not exist. List the folder with dir, then type the name again.",
                        "The prompt says The directory is not empty.",
                        "Delete the files in the folder first, then remove the folder with rmdir.")
                .seeAlso("jsc:working_with_files", "jsc:paths_and_drives")
                .register();
    }

    private static void install(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_install", section).titled("Installing MC-DOS")
                .paragraph("MC-DOS is the Vintage system that works at the prompt ([](jsc:the_systems)). You install it"
                        + " from a medium only: a floppy disk, put in a Floppy Drive. The installer copies the"
                        + " system to a disk you choose, and then the computer starts from that disk.")
                .table("What you need")
                .property("Install medium", "The MC-DOS floppy disk, from the creative tabs.")
                .property("Drive", "A Floppy Drive joined to the computer ([](jsc:drives)).")
                .property("Room on the disk", "4 megabytes (MB) free on the disk you install on.")
                .property("Random-access memory (RAM)", "1 megabyte (MB) while MC-DOS runs.")
                .paragraph("The firmware is the program on the motherboard that starts the computer"
                        + " ([](jsc:firmware)).")
                .steps("Put the MC-DOS floppy disk in the Floppy Drive.",
                        "Start the computer with the floppy in its drive, as the installing page explains"
                                + " ([](jsc:installing)).",
                        "Read the page \"Welcome to Setup.\" and press [[Enter]].",
                        "Move the cursor to a disk with the arrow keys. Type a name in \"Computer name:\" if you want"
                                + " one, then press [[Enter]].",
                        "Wait while the bar fills. The page says \"Setup is copying MC-DOS to the disk.\"",
                        "Read \"MC-DOS Setup is complete.\" and press [[Enter]] to restart.")
                .table("The four pages of Setup", "Page", "Heading on the screen", "Keys at the foot")
                .row(GuideCell.words("Welcome"), GuideCell.words("Welcome to Setup."),
                        GuideCell.code("ENTER=Continue  F3=Exit"))
                .row(GuideCell.words("Settings"), GuideCell.words("Setup will use the following settings:"),
                        GuideCell.code("ENTER=Continue  F3=Exit"))
                .row(GuideCell.words("Copy"), GuideCell.words("Setup is copying MC-DOS to the disk."),
                        GuideCell.words("none"))
                .row(GuideCell.words("Done"), GuideCell.words("MC-DOS Setup is complete."),
                        GuideCell.code("ENTER=Restart"))
                .paragraph("The settings page lists the disks of the computer. Continue stays off until you pick a disk"
                        + " that has at least 4 megabytes (MB) free. Setup copies the system onto the disk you picked."
                        + " When the copy is done, the computer restarts from that disk.")
                .paragraph("As the computer starts, the screen prints a line for each drive, such as Drive C:, and then"
                        + " the version line, MC-DOS Version 1.0. You are then at the prompt ([](jsc:dos_prompt)).")
                .define("Firmware", "The program on the motherboard that starts the computer. It runs before any"
                        + " system does.")
                .ifSomethingGoesWrong(
                        "Nothing starts from the floppy.",
                        "The Floppy Drive is not joined to the computer, or the firmware does not find it. Join the"
                                + " drive, then check the firmware ([](jsc:firmware)).",
                        "The screen says \"No room here. Erase this disk, or choose another.\"",
                        "The disk you picked has less than 4 megabytes (MB) free. Pick another disk that has room"
                                + " ([](jsc:disks)).",
                        "Continue does nothing on the settings page.",
                        "No disk is picked yet, or the picked disk has no room. Pick a disk with room first.",
                        "Pressing [[F3]] does nothing.",
                        "The copy has begun, and [[F3]] works only before it. Wait for the last page.")
                .seeAlso("jsc:dos_system_files", "jsc:frames_which_machine")
                .register();
    }

    private static void systemFiles(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_system_files", section).titled("The system's own files")
                .paragraph("When MC-DOS is installed, the system disk, C:, shows you some files that were never copied"
                        + " onto it. The system builds them from what is installed each time the disk is read, so they"
                        + " are not stored on the disk. They sit at the top of C: and in its DOS folder.")
                .table("The files of the system disk", "File or folder", "What it is")
                .row(GuideCell.code("DOS\\"), GuideCell.words("The folder of the system's own files."))
                .row(GuideCell.code("AUTOEXEC.BAT"), GuideCell.words("The startup file, with the PATH line."))
                .row(GuideCell.code("CONFIG.SYS"), GuideCell.words("The configuration file, whose lines have no effect"
                        + " on this system."))
                .row(GuideCell.code("COMMAND.COM"), GuideCell.words("The command interpreter: the program that reads"
                        + " what you type and runs it."))
                .row(GuideCell.code("MCDOS.SYS"), GuideCell.words("The system itself."))
                .row(GuideCell.code("DOS\\HIMEM.SYS"), GuideCell.words("The memory driver of the system."))
                .row(GuideCell.code("DOS\\README.TXT"),
                        GuideCell.words("The notes of the system, which tell you to type HELP for the commands."))
                .row(GuideCell.code("PROGRAM\\"), GuideCell.words("The folder of an installed program, named with up to"
                        + " eight capital letters."))
                .row(GuideCell.code("PROGRAM\\PROGRAM.EXE"), GuideCell.words("The program itself."))
                .row(GuideCell.code("PROGRAM\\README.TXT"), GuideCell.words("The notes of that program."))
                .paragraph("AUTOEXEC.BAT holds three lines: @ECHO OFF, PROMPT $P$G and a PATH line. The PATH line lists"
                        + " the DOS folder first, then the folder of each installed program in the order they were"
                        + " installed. When you type only a program's name, MC-DOS searches the folders on that line."
                        + " If AUTOEXEC.BAT or its PATH line is missing, it searches the DOS folder only.")
                .paragraph("The PROMPT line does not set your prompt. The prompt always shows the folder you are in and"
                        + " a greater-than sign ([](jsc:dos_prompt)).")
                .paragraph("CONFIG.SYS holds the lines DEVICE=C:\\DOS\\HIMEM.SYS, DOS=HIGH, FILES=30 and BUFFERS=20."
                        + " The file is there for you to read, but its lines have no effect on this system.")
                .ifSomethingGoesWrong(
                        "You type the name of a program you installed, and the prompt says command not found.",
                        "Type the full path of the program's .EXE file: C:\\ followed by its folder and its file name."
                                + " Programs typed by name are found in the folder you are in and in the folders of"
                                + " the PATH line.",
                        "You expect the PROMPT line to change the prompt, and it does not.",
                        "The prompt always shows the folder and a greater-than sign. No line of AUTOEXEC.BAT changes"
                                + " it ([](jsc:dos_prompt)).")
                .seeAlso("jsc:dos_prompt", "jsc:dos_shell")
                .register();
    }

    private static void dosShell(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_shell", section).titled("DOSSHELL and its keys")
                .paragraph("The MC-DOS Shell is a menu program for the files of the system. You see the folders and the"
                        + " files, copy, move or delete them, and start programs with the keys, without typing each"
                        + " command yourself. Every action of its File menu runs one of MC-DOS's own commands.")
                .paragraph("Type dosshell at the prompt to open it. It runs on MC-DOS only, takes the whole screen and"
                        + " opens on the top folder of C:.")
                .steps("Type dosshell at the prompt and press [[Enter]].",
                        "Press [[Tab]] to move from one area to the next, and the arrow keys to move within one.",
                        "Press [[Enter]] on a program to start it as a task.",
                        "Hold [[Alt]] and press [[Tab]] to pick the next task, and let go of [[Alt]] to switch to it.",
                        "Press [[F3]] to leave the shell, once no task is listed.")
                .table("The areas of the screen", "Area", "What it shows")
                .row(GuideCell.words("Directory Tree"), GuideCell.words("The folders of the disk you are on."))
                .row(GuideCell.words("Drives"), GuideCell.words("The drives the machine has, shown only when it has"
                        + " some."))
                .row(GuideCell.words("Main"), GuideCell.words("The files of the folder you picked."))
                .row(GuideCell.words("Active Task List"), GuideCell.words("The tasks you started, shown while the Task"
                        + " Swapper is on."))
                .table("The views", "View", "What it shows")
                .row(GuideCell.words("Program/File Lists"), GuideCell.words("The tree and the files, over the programs"
                        + " and the tasks, the way the shell opens."))
                .row(GuideCell.words("Single File List"), GuideCell.words("The tree and the list of files, down to the"
                        + " bottom of the screen."))
                .row(GuideCell.words("Program List"), GuideCell.words("The programs and the tasks only."))
                .paragraph("Dual File Lists and All Files answer \"Not available on this machine.\" Select Across"
                        + " Directories does the same.")
                .paragraph("The program list has a group called Main. It holds Command Prompt, which starts a command"
                        + " prompt as a task, and Sigma when Sigma is installed. It holds the groups [Programs] and"
                        + " [Disk Utilities]. [Programs] lists the programs you installed, except the Command Prompt.")
                .table("Keys in the shell", "Key", "What it does")
                .row(GuideCell.code("Tab"), GuideCell.words("Moves to the next area, and Shift with Tab goes back."))
                .row(GuideCell.code("Arrows"), GuideCell.words("Move within an area."))
                .row(GuideCell.code("Page Up"), GuideCell.words("Moves a page up the list."))
                .row(GuideCell.code("Page Down"), GuideCell.words("Moves a page down the list."))
                .row(GuideCell.code("Home"), GuideCell.words("Goes to the first item."))
                .row(GuideCell.code("End"), GuideCell.words("Goes to the last item."))
                .row(GuideCell.code("Enter"), GuideCell.words("Opens the item picked, or starts a program as a task."))
                .row(GuideCell.code("F10"), GuideCell.words("Opens the File menu."))
                .row(GuideCell.code("Alt+F"), GuideCell.words("Opens the File menu."))
                .row(GuideCell.code("Alt+O"), GuideCell.words("Opens the Options menu."))
                .row(GuideCell.code("Alt+V"), GuideCell.words("Opens the View menu."))
                .row(GuideCell.code("Alt+T"), GuideCell.words("Opens the Tree menu."))
                .row(GuideCell.code("Alt+H"), GuideCell.words("Opens the Help menu."))
                .row(GuideCell.code("F3"),
                        GuideCell.words("Leaves the shell, which is refused while a task is listed."))
                .row(GuideCell.code("Alt+F4"), GuideCell.words("Leaves the shell, as F3 does."))
                .row(GuideCell.code("F5"), GuideCell.words("Reads the disk again."))
                .row(GuideCell.code("Shift+F5"), GuideCell.words("Draws the screen again."))
                .row(GuideCell.code("F7"), GuideCell.words("Moves the selected file."))
                .row(GuideCell.code("F8"), GuideCell.words("Copies the selected file."))
                .row(GuideCell.code("F9"), GuideCell.words("Shows the contents of the selected file, which Escape"
                        + " closes."))
                .row(GuideCell.code("Shift+F9"), GuideCell.words("Opens a Command Prompt as a task."))
                .row(GuideCell.code("Del"), GuideCell.words("Deletes the selected file, or the selected empty folder in"
                        + " the tree."))
                .row(GuideCell.code("Alt+Tab"),
                        GuideCell.words("Switches to the next task, and Shift+Alt+Tab goes back."))
                .row(GuideCell.code("Escape"), GuideCell.words("Leaves a group of the program list, or closes a menu."))
                .row(GuideCell.code("+"), GuideCell.words("In the tree: shows the folders in the selected folder."))
                .row(GuideCell.code("*"), GuideCell.words("In the tree: shows every folder under the selected folder."))
                .row(GuideCell.code("-"), GuideCell.words("In the tree: puts away the folders under the selected"
                        + " folder."))
                .table("The File menu", "Item", "Key", "What it does")
                .row(GuideCell.words("Open"), GuideCell.code("Enter"), GuideCell.words("Starts a program, or opens the"
                        + " folder picked."))
                .row(GuideCell.words("View File Contents"), GuideCell.code("F9"), GuideCell.words("Shows the file, as"
                        + " the TYPE command does."))
                .row(GuideCell.words("Move..., from and to names"), GuideCell.code("F7"), GuideCell.words("Moves a"
                        + " file, as MOVE does."))
                .row(GuideCell.words("Copy..., from and to names"), GuideCell.code("F8"), GuideCell.words("Copies a"
                        + " file, as COPY does."))
                .row(GuideCell.words("Delete..., with a question when Confirmation is on"), GuideCell.code("Del"),
                        GuideCell.words("Deletes the file, as DEL does, or the folder, as RMDIR does."))
                .row(GuideCell.words("Rename..., with a new name"), GuideCell.words("none"), GuideCell.words("Gives the"
                        + " file another name, as REN does."))
                .row(GuideCell.words("Create Directory..., with the parent and a name"), GuideCell.words("none"),
                        GuideCell.words("Makes a folder, as MKDIR does."))
                .row(GuideCell.words("Exit"), GuideCell.code("F3"), GuideCell.words("Leaves the shell for the prompt."))
                .paragraph("The Options menu holds Confirmation, which asks before deleting, and Enable Task"
                        + " Swapper. The"
                        + " Task Swapper keeps each program you start as a task, so that you can switch between"
                        + " them. Show Information shows the selected file, its folder and its disk.")
                .paragraph("Disk Utilities holds Format. Its dialog has a field that starts with the text \"a:\"."
                        + " Replace the text with the drive you mean and add /y, as in D: /y. Without /y, Format prints"
                        + " its warning and erases nothing.")
                .paragraph("Print sends the selected file to a printer ([](jsc:printers)). It is greyed out when no"
                        + " printer is linked.")
                .paragraph("The bottom line shows the keys: F10=Actions, Shift+F9=Command Prompt and, while the"
                        + " Task Swapper is on, Alt+Tab=Next Task.")
                .define("MC-DOS Shell", "The menu program of MC-DOS for its files and programs, opened with the command"
                        + " dosshell.")
                .define("task", "A program or a Command Prompt that runs from the shell and is kept in the Active Task"
                        + " List.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "dosshell"))
                .ifSomethingGoesWrong(
                        "Enter on a file does nothing.",
                        "Only the files that end in .EXE, .COM and .BAT start from the shell. Press [[F9]] to read a"
                                + " text file.",
                        "The screen says You cannot quit MC-DOS Shell with programs in the Active Task List; quit"
                                + " those programs first.",
                        "Finish each task, then press [[F3]]. The tasks are the programs listed in the Active Task"
                                + " List.",
                        "The screen says No files match file specifier.",
                        "The pattern in File Display Options shows no file. Change it, for example to *.* to see every"
                                + " file.",
                        "The screen says Drive not ready.",
                        "The drive has no disk. Put a disk in it, or pick another drive.",
                        "The screen says Not available on this machine.",
                        "The shell does not offer that item. Use the commands at the prompt for the same job.")
                .seeAlso("jsc:dos_msd", "jsc:dos_help")
                .register();
    }

    private static void msd(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_msd", section).titled("MSD and its keys")
                .paragraph("Midsoft Diagnostics (MSD) reads the hardware of the computer and shows what it is made of."
                        + " Type msd at the prompt to open it. The program takes the whole screen, and it works on"
                        + " MC-DOS and on [MC-NET](jsc:mc_net).")
                .steps("Type msd at the prompt and press [[Enter]].",
                        "Move the pick with the arrow keys. Press [[Enter]] on LPT Ports or COM Ports to open a list.",
                        "In the list, pick a port with the arrows, then press [[E]] to enable its device or [[D]] to"
                                + " disable it.",
                        "Press [[Enter]] or [[Esc]] to put the list away. Press [[F3]] on the main screen to give the"
                                + " terminal back.")
                .table("The buttons of the main screen", "Button", "What it shows")
                .row(GuideCell.words("Computer..."), GuideCell.words("The name of the first processor."))
                .row(GuideCell.words("Memory..."),
                        GuideCell.words("The conventional memory, 640K, then the memory above 1 MB, counted in K."))
                .row(GuideCell.words("Video..."), GuideCell.words("The name of the first video card, or (none)."))
                .row(GuideCell.words("Network..."),
                        GuideCell.words("The cable of the first network adapter, or (none)."))
                .row(GuideCell.words("OS Version..."), GuideCell.words("The name of the system."))
                .row(GuideCell.words("Disk Drives..."),
                        GuideCell.words("The drive letters the machine sees, such as C: D:."))
                .row(GuideCell.words("LPT Ports..."),
                        GuideCell.words("The number of parallel ports. Zero when there are none. Opens a list."))
                .row(GuideCell.words("COM Ports..."),
                        GuideCell.words("The number of serial ports. Opens a list."))
                .paragraph("Only LPT Ports and COM Ports open a list. The other buttons show what they found, and Enter"
                        + " does nothing on them.")
                .table("The columns of a ports list", "Column", "What it shows")
                .row(GuideCell.words("Port"), GuideCell.words("The name of the port, such as COM1 or LPT1."))
                .row(GuideCell.words("Attached"), GuideCell.words("The device on the port, or (none) when nothing is"
                        + " attached."))
                .row(GuideCell.words("Status"),
                        GuideCell.words("On, or Off (disabled) for a device you have disabled."))
                .table("The keys", "Screen", "Key", "What it does")
                .row(GuideCell.words("Main screen"), GuideCell.code("Arrows"),
                        GuideCell.words("Move the pick between the buttons."))
                .row(GuideCell.words("Main screen"), GuideCell.code("Enter"),
                        GuideCell.words("Opens the button picked."))
                .row(GuideCell.words("Main screen"), GuideCell.code("L"), GuideCell.words("Opens the LPT ports."))
                .row(GuideCell.words("Main screen"), GuideCell.code("C"), GuideCell.words("Opens the COM ports."))
                .row(GuideCell.words("Main screen"), GuideCell.code("Esc or F3"),
                        GuideCell.words("Leaves the program."))
                .row(GuideCell.words("Ports list"), GuideCell.code("Up and Down"), GuideCell.words("Pick a port."))
                .row(GuideCell.words("Ports list"), GuideCell.code("E"),
                        GuideCell.words("Enables the device of the port picked."))
                .row(GuideCell.words("Ports list"), GuideCell.code("D"),
                        GuideCell.words("Disables the device of the port picked, which keeps its port while the"
                                + " computer neither reads nor writes it."))
                .row(GuideCell.words("Ports list"), GuideCell.code("Enter or Esc"),
                        GuideCell.words("Puts the list away."))
                .paragraph("A mouse click works too: on a button, on a port, and on the Enable and Disable"
                        + " buttons under the list.")
                .define("Midsoft Diagnostics (MSD)", "The program that reads the hardware of the computer and reports"
                        + " what it finds.")
                .define("LPT port", "A parallel port, where a printer is usually plugged in.")
                .define("COM port", "A serial port, where a modem or a mouse of the era could be plugged in.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "msd"))
                .ifSomethingGoesWrong(
                        "The screen says msd: there is no machine to read.",
                        "msd has no computer to read. Type msd at the prompt of a computer.",
                        "A port says (none) under Attached.",
                        "Nothing is plugged into that port, so there is no device to enable or disable.",
                        "A port says Off (disabled) under Status.",
                        "You disabled its device. Pick the port and press [[E]] to enable the device again.")
                .seeAlso("jsc:printers", "jsc:dos_help")
                .register();
    }

    private static void help(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_help", section).titled("The help, full screen")
                .paragraph("Type help at the prompt to open the help of MC-DOS. It takes the whole screen, in"
                        + " place of the prompt, and it shows the topics of the manual. Its topics come from the"
                        + " Frames Resource Kit, the book you are reading.")
                .paragraph("Type help and a command's name, such as help dir, to open the page of that command."
                        + " The same page opens when you type the command's name, a space and /?.")
                .paragraph("The help screen has four parts, from the top down, as the table shows.")
                .table("The parts of the help screen", "Part", "What it shows")
                .row(GuideCell.words("Top line"), GuideCell.words("The menus File and Search."))
                .row(GuideCell.words("Second line"), GuideCell.words("Four buttons: Next, Back, Contents and Index."))
                .row(GuideCell.words("Middle"), GuideCell.words("The topic, with its links."))
                .row(GuideCell.words("Bottom line"), GuideCell.words("The keys, a message or the Find prompt."))
                .steps("Type help at the prompt and press [[Enter]].",
                        "Press [[Tab]] until a button or a link has the focus, then press [[Enter]] to follow it.",
                        "Press [[Alt]]+[[N]] for the next topic, [[Alt]]+[[B]] for the one before,"
                                + " [[Alt]]+[[C]] for the Contents and [[Alt]]+[[I]] for the Index.",
                        "Press [[Alt]]+[[S]] to open the Search menu, pick Find... and press [[Enter]].",
                        "Type the words to find, then press [[Enter]].",
                        "Press [[Esc]] to leave the help, or press [[Alt]]+[[F4]].")
                .table("The keys of the help", "Key", "What it does")
                .row(GuideCell.code("Tab"), GuideCell.words("Picks the next button or link, and Shift with Tab picks"
                        + " the previous one."))
                .row(GuideCell.code("Enter"), GuideCell.words("Follows the link or button picked."))
                .row(GuideCell.code("Up and Down"), GuideCell.words("Scroll one line."))
                .row(GuideCell.code("Page Up and Page Down"), GuideCell.words("Scroll one page."))
                .row(GuideCell.code("Home and End"), GuideCell.words("Go to the top and the bottom of the topic."))
                .row(GuideCell.code("F10 or Alt+F"), GuideCell.words("Opens the File menu, with Exit."))
                .row(GuideCell.code("Alt+S"), GuideCell.words("Opens the Search menu, with Find... and Repeat Last"
                        + " Find."))
                .row(GuideCell.code("Alt+C"), GuideCell.words("Shows the Contents."))
                .row(GuideCell.code("Alt+N"), GuideCell.words("Shows the next topic."))
                .row(GuideCell.code("Alt+B"), GuideCell.words("Shows the topic before."))
                .row(GuideCell.code("Alt+I"), GuideCell.words("Shows the Index."))
                .row(GuideCell.code("F3"), GuideCell.words("Repeats the last find."))
                .row(GuideCell.code("Esc"), GuideCell.words("Closes an open menu or the Find prompt, or leaves the help"
                        + " when nothing is open."))
                .row(GuideCell.code("Alt+F4"), GuideCell.words("Leaves the help."))
                .paragraph("The bottom line shows only Alt+C=Contents, Alt+N=Next and Alt+B=Back. Alt+I and Alt+S work"
                        + " too.")
                .paragraph("Find first looks for a command with exactly that name. Then it looks for the next entry of"
                        + " the manual you are reading whose title or text has every word you typed. It goes round from"
                        + " the topic on the screen. Repeat Last Find, or [[F3]], looks for the same words again.")
                .define("Topic", "One page of the help: the page of a command, or an entry of a manual.")
                .define("Link", "A word in a page that you pick with Tab and follow with Enter.")
                .ifSomethingGoesWrong(
                        "The bottom line says Match not found.",
                        "No command and no entry has every word you typed. Try fewer words, or the name of a command.",
                        "Esc does not leave the help.",
                        "Esc first closes an open menu or the Find prompt. Press it again, or press [[Alt]]+[[F4]].")
                .seeAlso("jsc:help_on_a_computer", "jsc:dos_prompt")
                .register();
    }
}
