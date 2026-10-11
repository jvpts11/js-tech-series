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
 * The pages of the Command Prompt chapter of the Frames Resource Kit: the prompt window and its keys, paths and
 * switches, the file, text, machine, task, software and network verbs of the DOS family, and the way to find a
 * command. Each command of the family has its page here, made from the command itself.
 */
final class CommandPromptPages {

    private CommandPromptPages() {
    }

    /** Declares the sections and entries of the Command Prompt, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        promptBasics(guide, chapter);
        promptFiles(guide, chapter);
        promptSystem(guide, chapter);
        promptProgramsAndNetwork(guide, chapter);
    }

    private static void promptBasics(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("prompt_basics").titled("At the prompt").in(chapter)
                .register();
        promptWindow(guide, section);
        pathsAndDrives(guide, section);
        findingACommand(guide, section);
    }

    private static void promptFiles(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("prompt_files").titled("Files and text").in(chapter)
                .register();
        workingWithFiles(guide, section);
        formatADrive(guide, section);
        dosTextTools(guide, section);
    }

    private static void promptSystem(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("prompt_system")
                .titled("The machine and its tasks").in(chapter).register();
        shellVerbs(guide, section);
        machineVerbs(guide, section);
        tasksAndJobs(guide, section);
        dosPrinting(guide, section);
    }

    private static void promptProgramsAndNetwork(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("prompt_programs_and_network")
                .titled("Programs and the network").in(chapter).register();
        softwareVerbs(guide, section);
        networkVerbs(guide, section);
        mainframeVerbs(guide, section);
        editorsAndSigma(guide, section);
    }

    private static void promptWindow(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prompt_window", section).titled("The Command Prompt window and its keys")
                .paragraph("The Command Prompt is the window where you type a command and read what the machine"
                        + " answers. A command is a word that names a program, followed by the words that say what"
                        + " it works on. You press [[Enter]] to run the line.")
                .define("Command", "A program that you call by its name, with the words after the name that say"
                        + " what it works on.")
                .define("Program", "A set of instructions that the computer runs when you call it by name.")
                .paragraph("A monitor opens the screen made for its system. A Frames monitor, or a monitor with no"
                        + " system, opens the Command Prompt screen. MC-DOS, the disk operating system (DOS), opens"
                        + " its own text screen, MC-NET opens"
                        + " the network terminal, and Linux and the other Unix systems open their own terminal.")
                .paragraph("On a Frames desktop, the Command Prompt is one of the desktop's apps, so you can open it"
                        + " from there too.")
                .paragraph("Above the line you type, the window keeps what the machine has answered. That kept"
                        + " output is the scrollback, and the mouse wheel scrolls back through it. On a monitor, the"
                        + " scrollback holds 512 lines.")
                .define("Scrollback", "The output that the window keeps above the prompt, so that you can read back"
                        + " what has scrolled off the top.")
                .subheading("The keys")
                .table("Keys at the prompt", "Key", "What it does")
                .row(GuideCell.code("Enter"), GuideCell.words("Runs the typed line."))
                .row(GuideCell.code("Tab"), GuideCell.words("Fills in the rest of a command's name from the letters"
                        + " you typed. When several names fit, a second Tab in a row lists every candidate"
                        + " above the prompt."))
                .define("Tab completion", "Filling in the rest of a command's name from the letters you have typed,"
                        + " so that you need not type the whole name.")
                .row(GuideCell.code("Up"), GuideCell.words("Shows the line you typed before it, one step back."))
                .row(GuideCell.code("Down"), GuideCell.words("Shows the line you typed after it, one step forward."))
                .row(GuideCell.code("Esc"), GuideCell.words("Closes the window. An editor that is open takes the"
                        + " key first, when the editor uses it."))
                .row(GuideCell.code("Ctrl+C"), GuideCell.words("Copies the text you have picked out. With nothing"
                        + " picked out, it interrupts a tool that is running."))
                .define("Interrupt", "A signal that asks a running tool to stop.")
                .row(GuideCell.code("Ctrl+V"), GuideCell.words("Pastes text: up to 16 lines, each cut to 512"
                        + " characters. Every line but the last runs as it arrives."))
                .row(GuideCell.words("Mouse wheel"), GuideCell.words("Scrolls back through the output."))
                .paragraph("While a tool runs, the window reads only Ctrl+C. Wait for the tool to finish, or press"
                        + " Ctrl+C to interrupt it. The bottom line of the window reminds you of the keys: while you"
                        + " type, it reads \"ENTER run    UP/DOWN history    wheel scroll    ESC close\", and while a"
                        + " tool runs, it reads \"CTRL+C interrupt    wheel scroll    ESC close\".")
                .paragraph("When you have scrolled back, the bottom line shows \"scrolled +\" and a number of lines."
                        + " Once the first word is a command the machine knows, its usage appears dimmed on the"
                        + " right of your line, cut short with two dots if it does not fit.")
                .subheading("Limits and history")
                .paragraph("A line holds at most 256 characters. The window keeps the lines you type as history for"
                        + " this machine, so that the Up and Down keys bring them back. An empty line is never kept,"
                        + " and a line typed twice in a row is kept once. Opening the window starts an empty"
                        + " scrollback, but the line history stays.")
                .ifSomethingGoesWrong("The line is cut off.",
                        "A line holds at most 256 characters. Split it into two lines.",
                        "Typing does nothing while a tool runs.",
                        "While a tool runs, the window reads only Ctrl+C. Wait for the tool to finish, or press"
                                + " Ctrl+C.",
                        "Ctrl+C does not stop a tool.",
                        "The tool must be running in this window. A tool left running in the background does not"
                                + " answer Ctrl+C.",
                        "Esc does not close an editor.",
                        "An editor that uses Esc keeps the key. Leave the editor with its own exit key"
                                + " ([](jsc:editors_overview)), then press Esc.")
                .seeAlso("jsc:help_on_a_computer", "jsc:the_systems")
                .register();
    }

    private static void pathsAndDrives(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("paths_and_drives", section).titled("Paths, drives and the option switches")
                .paragraph("A path is the address of a file or a folder: the folder names, with a backslash between"
                        + " them, starting from a drive letter. C:\\DOS is the folder that MC-DOS searches for its"
                        + " programs by default. The rules for paths are in [the prompt, the drives and the"
                        + " paths](jsc:dos_prompt); this page gives the forms the prompt reads.")
                .define("Folder", "A named place on a disk that holds files, and other places of the same kind.")
                .define("Drive", "A disk that the computer can read, named by a letter. C: is the system disk.")
                .paragraph("Read a path from left to right. The forms below are the ones the prompt understands.")
                .table("Forms of a path", "Form", "What it means")
                .row(GuideCell.code(".."), GuideCell.words("Goes up one folder."))
                .row(GuideCell.code("\\DOS"), GuideCell.words("Starts from the root of the current drive, the top"
                        + " folder, not from the folder you are in."))
                .row(GuideCell.code("D:\\WORK"), GuideCell.words("Starts from the root of drive D:, whichever folder"
                        + " you were in."))
                .row(GuideCell.code("D:"), GuideCell.words("Goes to the root of drive D:, not to the folder you used"
                        + " there before."))
                .row(GuideCell.code("."), GuideCell.words("Is skipped, so it adds nothing to the path."))
                .paragraph("The prompt shows where you are: the current folder, then a greater-than sign. At the top of"
                        + " drive C: it reads C:\\>. After you type cd C:\\DOS, it reads C:\\DOS>.")
                .subheading("Changing drive")
                .paragraph("Type a drive letter and a colon, and nothing else, and the prompt moves to that drive."
                        + " Type D: to move to drive D:. The drives you can use are the ones linked to the computer.")
                .session("jsc:guide/terminal/dos", "MC-DOS")
                .typed("C:\\DOS> ", "cd")
                .answer("C:\\DOS")
                .paragraph("With no words after it, cd prints the folder you are in, such as C:\\DOS.")
                .subheading("Switches")
                .paragraph("A switch changes what the command does. You write it the way that command takes it, such"
                        + " as /y. Each command reads only the switches it declares; the shell itself knows none of"
                        + " them.")
                .define("Switch", "An option word after a command's name, written the way that command takes it.")
                .table("Switches of the DOS family", "Switch", "Command and what it does")
                .row(GuideCell.code("/y"), GuideCell.words("format: confirms that you want the drive erased."))
                .row(GuideCell.code("/PID"), GuideCell.words("taskkill: names the task to stop, by its number."))
                .row(GuideCell.code("/EVERY:"), GuideCell.words("at: runs the job only on the days named after it."))
                .row(GuideCell.code("/DELETE"), GuideCell.words("at: takes one job off the list."))
                .row(GuideCell.code("/I"), GuideCell.words("find: ignores capital letters."))
                .row(GuideCell.code("/V"), GuideCell.words("find: prints the lines that do not hold the text."))
                .row(GuideCell.code("/R"), GuideCell.words("sort: reverses the order of the lines."))
                .row(GuideCell.code("/LOCAL"), GuideCell.words("interac: leaves what it takes in this computer,"
                        + " not in your hands."))
                .row(GuideCell.code("/S:COUNT"), GuideCell.words("interac: orders a listing by how much there is."))
                .subheading("Asking for help")
                .table("Two ways to ask for help", "Form", "Where it works")
                .row(GuideCell.code("--help"), GuideCell.words("After the command's name, on every family:"
                        + " MC-DOS, MC-NET, Frames and the Unix systems."))
                .row(GuideCell.code("/?"), GuideCell.words("After the command's name, on the DOS family only: MC-DOS"
                        + " and the Frames editions."))
                .paragraph("Both forms print the command's page, the same text that help <command> prints. You can"
                        + " read the same page here, in the command pages of this book.")
                .subheading("Quotes and names")
                .paragraph("Words in double quotes stay together as one word. The find command takes its text that"
                        + " way, so find \"oak log\" looks for the whole phrase.")
                .paragraph("On the DOS family, a name between percent signs, such as %USERNAME%, is replaced by its"
                        + " value before the command runs. The built-in names are HOSTNAME, COMPUTERNAME, USER,"
                        + " USERNAME, OS, CD (the prompt) and PATH. You give a name a value with set"
                        + " ([](jsc:shell_verbs)).")
                .paragraph("A star (*) in a name is passed to the command as it is. The command decides what the star"
                        + " matches.")
                .paragraph("If the word you type is not a command, the shell searches for a program of that name in"
                        + " the PATH. The PATH is the list of folders it searches. On MC-DOS it comes from the PATH"
                        + " line of C:\\AUTOEXEC.BAT, and C:\\DOS is the default.")
                .ifSomethingGoesWrong("The system cannot find the path specified.",
                        "Check the folder name with dir. A drive letter alone, such as C:, goes to the root of that"
                                + " drive, not to the folder you were in.",
                        "The system cannot find the file specified.",
                        "The file is not in the folder you are in. Type its full path, such as C:\\DOS\\NAME, or"
                                + " go to its folder first with cd.",
                        "MYFILE.TXT: file not found",
                        "The file name is wrong, or the file is in another folder. Check the name with dir.",
                        "D:\\ The system cannot find the drive specified.",
                        "No drive D: is linked to this computer. Pick a drive the computer has, or link a drive to it.",
                        "D:\\ The device is not ready.",
                        "The drive has no disk in it. Put a disk in the drive, then try again.",
                        "A command typed with /? on MC-NET or a Unix system does not show its help.",
                        "Type --help after the command's name on those systems.")
                .seeAlso("jsc:dos_prompt", "jsc:finding_a_command")
                .register();
    }

    private static void findingACommand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("finding_a_command", section).titled("Finding a command")
                .paragraph("Every command has a page of its own, and the page is the same text that help and the switch"
                        + " /? print. You can list the commands the machine has, or ask for the page of one command"
                        + " you already know. Type the first letters of a name and press [[Tab]] to finish it.")
                .paragraph("Each command has a scope. A command works only on the systems it names, and only on a"
                        + " machine with the parts it needs, such as a disk for the file commands or a network for"
                        + " iql. When a part is missing, the command is not in help, and typing it gives a message.")
                .define("Scope", "The systems and the parts of a machine that a command needs before the machine"
                        + " offers it.")
                .subheading("Listing the commands")
                .table("Ways to see the commands", "Way", "What it shows")
                .row(GuideCell.code("help"), GuideCell.words("The commands this machine has, one line each. MC-DOS"
                        + " and MC-NET show them on the whole screen; Frames prints them."))
                .row(GuideCell.code("help NAME"), GuideCell.words("The page of one command."))
                .row(GuideCell.code("listcmd"), GuideCell.words("Every command and every program on this machine,"
                        + " in one list."))
                .paragraph("listcmd lists everything this machine can run: the commands it offers and every program"
                        + " installed on it, in one list. Its list has a COMMANDS heading with one line per command,"
                        + " then a PROGRAMS heading with each program's name and id. It is off unless the server's"
                        + " settings turn it on.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "listcmd"))
                .paragraph("On Linux and the other Unix systems, man opens the page of a Unix command. Those commands"
                        + " are explained in the Unix books, in [finding help](jsc:man_pages).")
                .ifSomethingGoesWrong("listcmd says command not found.",
                        "The server has listcmd off. Type help instead to list the commands.",
                        "A command is missing from help.",
                        "This machine lacks something the command needs, such as a disk, a network or a port for"
                                + " peripherals. Or the command belongs to a package you have not installed yet.",
                        "The shell says command not found: NAME",
                        "The word is not a command on this machine, and no program of that name is installed. Check"
                                + " the spelling, or type help to list the commands.",
                        "The page says it works once a package is installed.",
                        "Install that package first ([](jsc:software_verbs)). Then the command is on the machine.")
                .seeAlso("jsc:help_on_a_computer", "jsc:the_systems")
                .register();
    }

    private static void workingWithFiles(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("working_with_files", section).titled("Working with files and folders")
                .paragraph("Most of the prompt's work with files is this: you list them, move between folders, copy,"
                        + " rename and delete them. Each verb takes its words in the order the table shows, and each"
                        + " one has a page of its own below.")
                .paragraph("These verbs need a machine with a file system. MC-NET has none, so they are not on its"
                        + " prompt.")
                .define("Extension", "The letters after the last dot in a file name. They tell the system what kind"
                        + " of file it is.")
                .define("Read-only", "A file that you can read, but cannot change or delete.")
                .table("The file verbs", "Verb", "Words after it, and what it does")
                .row(GuideCell.code("dir"), GuideCell.words("Takes an optional folder. Lists the files and folders"
                        + " in it."))
                .row(GuideCell.code("cd (chdir)"), GuideCell.words("Takes an optional folder. Shows the folder you"
                        + " are in, or changes to the folder you name."))
                .row(GuideCell.code("type"), GuideCell.words("A file name. Prints the lines of the file."))
                .row(GuideCell.code("write (save)"), GuideCell.words("A file name, then the text. Creates the file, or"
                        + " overwrites it, with the text."))
                .row(GuideCell.code("del (erase)"), GuideCell.words("A file name. Deletes one file from the system"
                        + " disk."))
                .row(GuideCell.code("copy"), GuideCell.words("A source, then a destination. Copies a file, or a folder"
                        + " tree within one drive."))
                .row(GuideCell.code("move"), GuideCell.words("A source, then a folder. Moves a file or a folder tree"
                        + " into that folder."))
                .row(GuideCell.code("ren (rename)"), GuideCell.words("A file name, then the new name. Renames the file"
                        + " in place; it does not move it."))
                .row(GuideCell.code("mkdir (md)"), GuideCell.words("A folder name. Creates the folder."))
                .row(GuideCell.code("rmdir (rd)"), GuideCell.words("A folder name. Removes the folder, if it is"
                        + " empty."))
                .row(GuideCell.code("tree"), GuideCell.words("No words. Draws the folders under the one you are in."))
                .row(GuideCell.code("run"), GuideCell.words("An .iql file name. Runs the file as IQL statements,"
                        + " the network's query language."))
                .paragraph("dir lists the folder you name, or the folder you are in. Each row shows when the item was"
                        + " last written, its size and its name. A folder shows <DIR> in place of a size, and a"
                        + " read-only file has [RO] after its name. The last line counts the files and the folders"
                        + " and gives the total size in mB, the unit the listing prints. An empty folder says File"
                        + " Not Found.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "dir"))
                .paragraph("cd with no words prints the folder you are in. cd with a folder changes to it. A drive"
                        + " letter alone goes to the root of that drive ([](jsc:paths_and_drives)).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "cd"))
                .paragraph("type prints each line of a file. An empty file prints the dim line \"(empty file)\". A .dat"
                        + " file is refused, because it is the view of a disk's items.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "type"))
                .paragraph("write takes the file name, then everything after it as the text. The extension decides the"
                        + " kind of file. You can write files with the extensions iql, txt, cfg, csv and cmd. You"
                        + " cannot write log and craft files by hand. On success write prints \"wrote\" and the"
                        + " file name.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "write"))
                .paragraph("del deletes one file and prints \"deleted\" and its name. A .dat file cannot be deleted.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "del"))
                .paragraph("copy copies one file, or a whole folder tree within one drive. Between drives it copies"
                        + " files only, and on success it prints \"1 file(s) copied.\" A file on another machine can"
                        + " be copied, but not moved.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "copy"))
                .paragraph("move takes the same words as copy. It moves a file or a folder tree into another folder,"
                        + " and between drives it moves files only. On success it prints \"1 file(s) moved.\"")
                .command(CommandGuidePages.id(Platform.MC_DOS, "move"))
                .paragraph("ren changes a name in place. The new name is everything after the old name. ren does not"
                        + " move the file, so use move for that.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "ren"))
                .paragraph("mkdir creates a folder. Some drives are flat and hold no folders at all, and mkdir refuses"
                        + " them.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "mkdir"))
                .paragraph("rmdir removes a folder, but only an empty one. A folder with files in it is refused.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "rmdir"))
                .paragraph("tree draws the folders under the one you are in. With none, it says \"No subfolders"
                        + " exist\".")
                .command(CommandGuidePages.id(Platform.MC_DOS, "tree"))
                .paragraph("run runs an .iql file, as if you had typed its statements with iql"
                        + " ([](jsc:network_verbs)). Only .iql files run. A result prints in green on success and in"
                        + " red on failure.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "run"))
                .ifSomethingGoesWrong("NAME: .dat files are read-only (use the Network Interactor to access items)",
                        "A .dat file is the view of the disk's items. To move items, use interac"
                                + " ([](jsc:network_verbs)).",
                        "NAME: .log is read-only",
                        "You cannot change log files by hand. For the network's log, see"
                                + " [](jsc:cancelling_and_the_log).",
                        "NAME: .craft files cannot be edited",
                        "Crafts are asked for on the network, not written by hand ([](jsc:asking_for_a_craft)).",
                        "NAME: not enough free space on the disk",
                        "Delete a file you no longer need, or use a larger disk.",
                        "Directories are not supported on this drive.",
                        "This drive is flat and holds no folders. Put the folder on another drive.",
                        "NAME: a file of that name is already there",
                        "Choose another name, or delete the old file first.",
                        "NAME: the destination is read-only",
                        "Copy or move the file into a folder that you can write to.")
                .seeAlso("jsc:paths_and_drives", "jsc:storage")
                .register();
    }

    private static void formatADrive(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("format_a_drive", section).titled("Formatting a drive")
                .paragraph("Formatting erases everything on a drive and prepares the drive again to hold files. Use it"
                        + " on a new disk, or on a disk whose files you no longer need.")
                .define("Format", "To erase every file on a drive and prepare the drive again.")
                .define("Volume", "The disk as the machine sees it, with the files on it.")
                .warning("Formatting erases every file on the drive, and there is no undo. Check the drive letter"
                        + " before you run the line.")
                .steps("Type format, a space, the drive letter and a colon, such as D:.",
                        "Read the warning. It names the drive that will be erased.",
                        "Type the same line with /y at the end, such as format D: /y, to start the format.")
                .paragraph("format asks for /y before it does anything. Without it, format prints the warning in red,"
                        + " then the dim line that shows the line to type. The erasing itself needs the /y.")
                .session("jsc:guide/terminal/dos", "MC-DOS")
                .typed("C:\\> ", "format D:")
                .answer("WARNING: ALL DATA ON DRIVE D: WILL BE LOST!", "Run 'format D: /y' to proceed.")
                .table("The messages of format", "Message", "What it means")
                .row(GuideCell.words("usage: format <drive>: [/y]"), GuideCell.words("You typed format with no drive."))
                .row(GuideCell.words("format: invalid drive: NAME"), GuideCell.words("The first word is not a drive"
                        + " letter."))
                .row(GuideCell.words("format: drive D: drive not ready"), GuideCell.words("No disk is in that drive."))
                .row(GuideCell.words("format: drive D: not found"), GuideCell.words("No drive of that letter is linked"
                        + " to the computer."))
                .row(GuideCell.words("format: cannot format drive C: - the running system lives on it"),
                        GuideCell.words("The system disk cannot be formatted from its own prompt."))
                .row(GuideCell.words("Formatting drive D: ... done"),
                        GuideCell.words("The drive is formatted. The next line says: All data on the volume was"
                                + " erased."))
                .command(CommandGuidePages.id(Platform.MC_DOS, "format"))
                .ifSomethingGoesWrong("format says it cannot format drive C:.",
                        "C: holds the running system. Format a second drive instead ([](jsc:drives)).",
                        "format says the drive is not ready.",
                        "Link a disk to that drive ([](jsc:drives)), then try again.",
                        "The wrong drive was erased.",
                        "Check the drive letter and the warning before you add /y.")
                .seeAlso("jsc:disks", "jsc:drives")
                .register();
    }

    private static void dosTextTools(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_text_tools", section).titled("Text: find, sort, more and the pipe")
                .paragraph("The DOS family has three verbs for text. find prints the lines that hold some text, sort"
                        + " puts lines in order, and more shows a file one screen at a time.")
                .paragraph("find and sort take their text from a file that you name, or from a pipe. more reads only"
                        + " the file that you name.")
                .define("Pipe", "A line of two or more commands joined by a bar symbol, so that the output of the first"
                        + " command becomes the input of the next.")
                .paragraph("The pipe sends the output of one command to the next command as its input. Each bar needs"
                        + " a command on each side of it. A stage hands at most 4,096 lines to the"
                        + " next one.")
                .define("Redirection", "Sending the output of a command to a file, or reading its input from a file,"
                        + " with an arrow symbol.")
                .table("Symbols that join and redirect", "Symbol", "What it does")
                .row(GuideCell.code("|"), GuideCell.words("Sends the output of the left command to the right command."))
                .row(GuideCell.code(">"), GuideCell.words("Writes the output to a file, replacing what the file had."))
                .row(GuideCell.code(">>"), GuideCell.words("Adds the output to the end of a file."))
                .row(GuideCell.code("<"), GuideCell.words("Reads the input from a file."))
                .paragraph("The DOS text tools take a file name or a pipe. The Unix systems have tools of the same"
                        + " names that work separately, with their own rules ([](jsc:text_tools)).")
                .subheading("find")
                .paragraph("find prints the lines that hold the text in double quotes. It is case-sensitive unless you"
                        + " add /I, which ignores capital letters. /V prints the lines that do not hold the text.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "find"))
                .subheading("sort")
                .paragraph("sort puts the lines in order, comparing them as text. /R reverses the order. It reads a"
                        + " pipe"
                        + " or the file that you name.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "sort"))
                .subheading("more")
                .paragraph("more shows a file one screen at a time. It is a pager: a viewer that waits for you between"
                        + " screens. Its keys are these.")
                .define("Pager", "A viewer that shows one screen of a file at a time and waits for you to go on.")
                .table("Keys in more", "Key", "What it does")
                .row(GuideCell.code("Space"), GuideCell.words("Goes on to the next screen."))
                .row(GuideCell.code("Page Down"), GuideCell.words("Goes on to the next screen."))
                .row(GuideCell.code("b"), GuideCell.words("Goes back one screen."))
                .row(GuideCell.code("Page Up"), GuideCell.words("Goes back one screen."))
                .row(GuideCell.words("Up and Down arrows"), GuideCell.words("Move one line."))
                .row(GuideCell.code("/"), GuideCell.words("Looks for the text you type."))
                .row(GuideCell.code("n"), GuideCell.words("Finds the next match of the search."))
                .row(GuideCell.code("q"), GuideCell.words("Gives the terminal back."))
                .command(CommandGuidePages.id(Platform.MC_DOS, "more"))
                .ifSomethingGoesWrong("find prints nothing.",
                        "find is case-sensitive unless you add /I. Check the capital letters too, and keep the"
                                + " text in double quotes.",
                        "sort does not put the numbers in order as numbers.",
                        "sort compares lines as text, so a line \"10\" comes before a line \"9\". The DOS sort has no"
                                + " numeric option.",
                        "The line says syntax error near unexpected token, and names the bar.",
                        "A pipe needs a command on each side of the bar.",
                        "An arrow has no file name after it, and the line stops with a syntax error.",
                        "Type the file name after the arrow, such as > RESULT.TXT.")
                .seeAlso("jsc:pipes_and_variables", "jsc:text_tools")
                .register();
    }

    private static void shellVerbs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("shell_verbs", section).titled("The prompt's own commands")
                .paragraph("A few commands belong to the prompt itself. help lists the commands and explains each one,"
                        + " cls clears the window, echo prints the words you give it, and set gives a name a value."
                        + " Most of them are"
                        + " on every system, and the table shows where each one runs.")
                .table("Where the prompt's own commands run", "Command", "Systems")
                .row(GuideCell.code("help"), GuideCell.words("MC-DOS, MC-NET, the Frames editions, and the Linux and"
                        + " UNIX systems. Not FreeBSD."))
                .row(GuideCell.code("cls"), GuideCell.words("MC-DOS and the Frames editions."))
                .row(GuideCell.code("version"), GuideCell.words("MC-DOS and the Frames editions."))
                .row(GuideCell.code("echo"), GuideCell.words("Every system."))
                .row(GuideCell.code("whoami"), GuideCell.words("Every system."))
                .row(GuideCell.code("exit"), GuideCell.words("Every system."))
                .row(GuideCell.code("set"), GuideCell.words("Every system."))
                .subheading("help")
                .paragraph("help with no words lists the commands this machine can run, one line each, with a summary"
                        + " of each. At the foot it says \"'help <command>' for details\". The list shows only the"
                        + " commands this machine has, not every command of its family.")
                .paragraph("help <command> prints the page of one command. On MC-DOS and MC-NET, help takes the whole"
                        + " screen, with the series' manuals beside the commands. On the Frames editions, help prints."
                        + " help dir opens the page of dir. The page ends with an also line that lists the other"
                        + " names of the command.")
                .table("Keys in the full-screen help", "Key", "What it does")
                .row(GuideCell.code("Tab"), GuideCell.words("Picks a button or a link."))
                .row(GuideCell.code("Enter"), GuideCell.words("Follows the button or link that is picked."))
                .row(GuideCell.code("Alt+C"), GuideCell.words("Shows the contents."))
                .row(GuideCell.code("Alt+N"), GuideCell.words("Goes to the next topic."))
                .row(GuideCell.code("Alt+B"), GuideCell.words("Goes to the topic before."))
                .row(GuideCell.code("Alt+I"), GuideCell.words("Shows the index."))
                .row(GuideCell.code("Alt+S"), GuideCell.words("Finds a word."))
                .row(GuideCell.code("F3"), GuideCell.words("Finds the same word again."))
                .row(GuideCell.code("Esc"), GuideCell.words("Leaves the help."))
                .command(CommandGuidePages.id(Platform.MC_DOS, "help"))
                .subheading("cls, version and echo")
                .paragraph("cls clears the window, and the scrollback goes with it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "cls"))
                .paragraph("version prints the line \"J's Computers Shell v1.0\" in the accent colour.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "version"))
                .paragraph("echo prints the words that you give it. On MC-DOS and on the Frames editions, the text"
                        + " prints as you typed it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "echo"))
                .subheading("whoami and exit")
                .paragraph("whoami prints this computer's name, its type and its node, one row each. A computer with"
                        + " no name shows (unnamed).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "whoami"))
                .paragraph("exit closes a remote session that you opened with ssh ([](jsc:network_verbs)). On the"
                        + " prompt of this computer, exit only says that it is not connected. To leave the window,"
                        + " press [[Esc]].")
                .command(CommandGuidePages.id(Platform.MC_DOS, "exit"))
                .subheading("set and variables")
                .define("Variable", "A name that the machine keeps with a value. You give it a value with set, and"
                        + " read it back between percent signs.")
                .paragraph("set with no words lists every name this machine knows. set NAME=value gives a name a value."
                        + " set NAME= with nothing after the equals sign forgets the name. Names use letters, digits"
                        + " and underscores, and do not start with a digit.")
                .paragraph("A value set here belongs to the machine. It stays set at the monitor, in a session opened"
                        + " from another machine, and after a restart.")
                .steps("Type set BASE=C:\\work to give BASE a value.",
                        "Type set to list the names, and BASE is in the list.",
                        "Type dir %BASE% to read the value back, which the prompt replaces before dir runs.",
                        "Type set BASE= to forget the name again.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "set"))
                .ifSomethingGoesWrong("help does not take the whole screen.",
                        "The full-screen help is on MC-DOS and MC-NET only. On the Frames editions, help prints the"
                                + " list ([](jsc:help_on_a_computer)).",
                        "help <command> says no such command.",
                        "The command is not on this machine. Type help to see the list, or listcmd if the server"
                                + " allows it ([](jsc:finding_a_command)).",
                        "set says a name is not allowed.",
                        "Start the name with a letter, and use only letters, digits and underscores.",
                        "set says usage: set NAME=value",
                        "Put an equals sign between the name and the value: set NAME=value.",
                        "exit says it is not connected.",
                        "exit only closes an ssh session. To leave this window, press [[Esc]].")
                .seeAlso("jsc:help_on_a_computer", "jsc:network_verbs")
                .register();
    }

    private static void machineVerbs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("machine_verbs", section)
                .titled("The machine: status, config, devices, reboot, date and mem")
                .paragraph("These verbs ask the machine about itself. status shows its power, its rate and its link."
                        + " config shows and changes its settings. devices lists the linked devices, reboot restarts"
                        + " the computer, date tells the day and the time, and mem shows the memory.")
                .subheading("status")
                .paragraph("status shows whether the computer is online, its processing rate, its memory buffer and its"
                        + " network link. The first line reads ONLINE in green, or OFFLINE in red."
                        + " Then come three rows.")
                .define("Tick", "The game's beat. A second is 20 of them.")
                .table("The rows of status", "Row", "What it shows")
                .row(GuideCell.code("cpu"), GuideCell.words("The processing rate, in it/t (items a tick)."))
                .row(GuideCell.code("ram"), GuideCell.words("The memory buffer, in it, the unit the mod uses for"
                        + " items."))
                .row(GuideCell.code("network"), GuideCell.words("linked, with the network's id in brackets, or -- when"
                        + " there is no link."))
                .command(CommandGuidePages.id(Platform.MC_DOS, "status"))
                .subheading("config")
                .paragraph("config with no words lists this computer's settings under the heading settings, and tells"
                        + " you how to change one. With a key and a value after it, config key value changes one"
                        + " setting.")
                .table("Settings with their own rules", "Key", "What it does")
                .row(GuideCell.code("name"), GuideCell.words("The computer's name, which whoami shows."))
                .row(GuideCell.code("theme"), GuideCell.words("Applies a preset of accent colour and wallpaper."))
                .row(GuideCell.code("netshare"), GuideCell.words("How much of the disk the network may read,"
                        + " in permille"
                        + " (parts per thousand), from 0 to 1000."))
                .row(GuideCell.code("share FOLDER [read or write]"), GuideCell.words("Opens a folder to the network. A"
                        + " shared folder shows as \\\\<host>\\<name>."))
                .row(GuideCell.code("unshare NAME"), GuideCell.words("Closes a shared folder."))
                .paragraph("Other keys go to the machine's settings store. A machine without one says that it has no"
                        + " settings store. For example, config netshare 500 sets how much of the disk the network"
                        + " may read to half of it.")
                .session("jsc:guide/terminal/dos", "MC-DOS")
                .typed("C:\\> ", "config netshare 500")
                .answer("netshare set")
                .command(CommandGuidePages.id(Platform.MC_DOS, "config"))
                .subheading("devices and reboot")
                .define("Peripheral", "A device that is linked to a port of a computer, such as a monitor, a drive or"
                        + " a printer.")
                .paragraph("devices lists the peripherals linked to this computer. Only a computer with ports for"
                        + " peripherals has this verb.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "devices"))
                .paragraph("reboot restarts the computer at once. reboot --firmware restarts it into the firmware setup"
                        + " instead, the program that runs before the system ([](jsc:firmware)). The prompt says"
                        + " \"The system is going down for reboot NOW!\" and then restarts.")
                .warning("reboot starts at once, with no question first. Save your work before you type it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "reboot"))
                .subheading("date and mem")
                .paragraph("date prints the day and the time by the world's clock. The first day is day 1. A day starts"
                        + " at 06:00 by that clock, so the hours of a day run from six in the morning.")
                .define("World clock", "The one clock that the whole world uses. Jobs, and the times of files, are read"
                        + " from it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "date"))
                .paragraph("mem shows how much memory the computer has, how much is held right now, and how much is"
                        + " promised to what runs. Free is what is left once the promised memory is taken away, so it"
                        + " tells you whether one more program fits.")
                .define("Committed memory", "Memory promised to running programs. It can be more than the memory in"
                        + " use at this moment.")
                .table("The rows of mem", "Row", "What it shows")
                .row(GuideCell.words("Total memory"), GuideCell.words("All the memory the computer has, in MB."))
                .row(GuideCell.words("In use"), GuideCell.words("The memory held at this moment."))
                .row(GuideCell.words("Committed"), GuideCell.words("The memory promised to running programs, in MB."))
                .row(GuideCell.words("Free"), GuideCell.words("Total memory minus committed memory, never below zero,"
                        + " in MB."))
                .command(CommandGuidePages.id(Platform.MC_DOS, "mem"))
                .ifSomethingGoesWrong("config says unknown setting: NAME",
                        "The machine has no setting with that key. Type config alone to list the settings.",
                        "config says netshare needs a number from 0 to 1000",
                        "Give a whole number from 0 to 1000.",
                        "config share refuses: this computer already has all the folders it can share.",
                        "Unshare one folder with config unshare NAME, then share the new one.",
                        "config share says another folder is already shared as NAME",
                        "Choose another name for the share, or unshare the old folder first.",
                        "config unshare says nothing is shared as NAME",
                        "Check the name. config unshare takes the name the share was given.",
                        "devices is not in help.",
                        "This computer has no ports for peripherals, so it has no devices verb. Use a computer with a"
                                + " port, or read [](jsc:peripheral_cables).")
                .seeAlso("jsc:firmware", "jsc:peripheral_cables")
                .register();
    }

    private static void tasksAndJobs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("tasks_and_jobs", section).titled("Tasks, starts in the background and the schedule")
                .paragraph("A task is a program that the computer is running right now. A job is a line the computer"
                        + " keeps to run later, or at an hour of every day. These verbs list and stop the tasks, start"
                        + " commands in the background, and keep the schedule of the jobs.")
                .define("Task", "A program that the computer is running right now.")
                .define("PID", "The process identifier: the number that names one task.")
                .define("Job", "A line the computer keeps to run later or at an hour. It holds memory while it"
                        + " waits.")
                .define("Schedule", "The list of jobs, each with its hour and its days.")
                .subheading("tasklist and taskkill")
                .paragraph("tasklist lists the tasks the computer is running. Its columns are PID, NAME, FILE,"
                        + " STATE and"
                        + " MEM. MEM shows the memory that each task holds of the memory promised to it. When nothing"
                        + " runs, it says \"No tasks are running.\"")
                .paragraph("On the Frames editions, tasklist works from Frames XP on. Frames 95 does not have it.")
                .command(CommandGuidePages.id(Platform.FRAMES, "tasklist"))
                .paragraph("taskkill stops a task. Give it the number with /PID, as in taskkill /PID 12, or write"
                        + " /PID:12. The number is the one that tasklist shows. Like tasklist, it works on Frames XP"
                        + " and later.")
                .command(CommandGuidePages.id(Platform.FRAMES, "taskkill"))
                .subheading("where and start")
                .paragraph("where looks up a command by its name and reports where the machine has it. If it finds"
                        + " nothing, it says INFO: Could not find \"NAME\". With no name, it prints its usage. It works"
                        + " on Frames XP and later.")
                .command(CommandGuidePages.id(Platform.FRAMES, "where"))
                .paragraph("start runs a command and gives you the prompt back at once, while the command keeps"
                        + " running. On a machine that runs one thing at a time, it takes the started commands in turn."
                        + " Each started command holds 1 MB of memory while it runs.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "start"))
                .subheading("at and the schedule")
                .paragraph("at runs a command at an hour of the day, every day or only on the days that you name. The"
                        + " smallest time step a job can use is one hour of the world's clock.")
                .table("Forms of at", "Form", "What it does")
                .row(GuideCell.code("at"), GuideCell.words("Lists the jobs, with their hours and days."))
                .row(GuideCell.code("at 06:00 COMMAND"), GuideCell.words("Runs the command every day at that hour."))
                .row(GuideCell.code("at 06:00 /EVERY:M,W,F COMMAND"),
                        GuideCell.words("Runs the command only on the days"
                        + " named: M, T, W, Th, F, Sa and Su."))
                .row(GuideCell.code("at 2 /DELETE"), GuideCell.words("Takes job 2 off the list."))
                .paragraph("For example, AT 06:00 INTERAC GET 64 COAL /LOCAL brings 64 coal into this computer every"
                        + " morning at six. Only the hour counts: the minutes after the colon are not kept, so write"
                        + " 06:00.")
                .paragraph("When at adds a job, it prints \"Added a new job with ID = \" and the number. Deleting a job"
                        + " prints \"Deleted job\" and its number. A job number that does not exist gets \"The job ID"
                        + " does not exist.\" With no jobs, at says \"There are no entries in the list.\"")
                .command(CommandGuidePages.id(Platform.MC_DOS, "at"))
                .ifSomethingGoesWrong("tasklist says No tasks are running.",
                        "No program is running on the computer right now. Start a program, then run tasklist again.",
                        "taskkill says /PID takes the number of a task.",
                        "Write /PID, then the number that tasklist shows for the task.",
                        "start says Not enough memory to start another task.",
                        "Stop a task with taskkill, or add memory ([](jsc:frames_memory)).",
                        "at says The time is not in the right format.",
                        "Write the hour as 06:00 or 6. Write the days as M, T, W, Th, F, Sa or Su, separated"
                                + " by commas.",
                        "A job did not run at the minute you asked.",
                        "Only the hour is kept. Schedule the job for the hour you need.")
                .seeAlso("jsc:task_manager")
                .register();
    }

    private static void dosPrinting(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dos_printing", section).titled("Printing from the prompt")
                .paragraph("print sends text files to the printer in the background, so the prompt is free while the"
                        + " files print. You need a printer linked to the computer ([](jsc:printers)).")
                .paragraph("The first time you use print, it says \"Resident part of PRINT installed\". Each file that"
                        + " you name joins the queue. The queue lists the file being printed, then the files waiting in"
                        + " the queue."
                        + " When the queue is empty, it says \"PRINT queue is empty\".")
                .command(CommandGuidePages.id(Platform.MC_DOS, "print"))
                .ifSomethingGoesWrong("print says Errors on list device indicate that it may be off-line.",
                        "The printer is not linked, or it is off. Check the printer, then print again"
                                + " ([](jsc:printers)).")
                .seeAlso("jsc:printers")
                .register();
    }

    private static void softwareVerbs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("software_verbs", section).titled("Installing and removing programs")
                .paragraph("A program reaches the computer in two ways: from a medium, such as a disc in a linked drive"
                        + " ([](jsc:installing)), or from the Mirror, the network's package service. programs lists the"
                        + " programs installed here. store lists the ones you can install, install and uninstall change"
                        + " them, and on the Frames editions pckmgr fetches packages from the Mirror.")
                .define("Mirror", "The network's package service, which offers programs that you can install.")
                .define("Package", "A program that the Mirror offers, ready to fetch and set up.")
                .table("Verbs for programs", "Verb", "Words after it, and what it does")
                .row(GuideCell.code("programs (apps)"), GuideCell.words("No words. Lists the programs installed on this"
                        + " computer, each with its id."))
                .row(GuideCell.code("install"),
                        GuideCell.words("A program's id. Installs the program from its medium."))
                .row(GuideCell.code("store (available)"), GuideCell.words("No words. Lists the programs that you can"
                        + " install on this computer."))
                .row(GuideCell.code("uninstall"), GuideCell.words("A program's id. Removes an installed program."))
                .row(GuideCell.code("pckmgr"), GuideCell.words("install, remove, search, list or update, and a name."
                        + " Works from the Mirror, on the Frames editions only."))
                .subheading("Installing from a medium")
                .steps("Type programs to see the programs already installed.",
                        "Type store to see the programs that you can install.",
                        "Put the program's disc in a linked drive, if the program needs one.",
                        "Type install and the program's id.",
                        "Wait for the lines that say Setting up. The last line says Installation complete. Rebooting"
                                + " into the new system.")
                .paragraph("When a program needs a disc that is not in a linked drive, install says that the program"
                        + " needs its install disc in a linked drive.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "programs"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "install"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "store"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "uninstall"))
                .subheading("pckmgr on the Frames editions")
                .paragraph("pckmgr fetches packages from the Mirror on the Frames editions. MC-NET calls the same"
                        + " manager"
                        + " netgetter. MC-DOS has no pckmgr: it installs programs from media only.")
                .table("Verbs of pckmgr", "Verb", "What it does")
                .row(GuideCell.code("install NAME"), GuideCell.words("Fetches and sets up a package."))
                .row(GuideCell.code("remove NAME"), GuideCell.words("Uninstalls a package."))
                .row(GuideCell.code("search [text]"), GuideCell.words("Finds the packages that the Mirror offers. With"
                        + " no text, it lists what is offered."))
                .row(GuideCell.code("list [--available]"), GuideCell.words("Lists the installed packages, or everything"
                        + " on offer with --available."))
                .row(GuideCell.code("update"), GuideCell.words("Brings the installed packages to the current build."))
                .command(CommandGuidePages.id(Platform.FRAMES, "pckmgr"))
                .subheading("screenfetch on the Frames editions")
                .paragraph("screenfetch is a small program for the Frames editions. Once you install it, typing"
                        + " screenfetch at the Command Prompt prints the logo of Frames, with a summary of your"
                        + " machine beside it: the system, its kernel, its uptime, its packages and its shell.")
                .command(CommandGuidePages.id(Platform.FRAMES, "screenfetch"))
                .ifSomethingGoesWrong("no such program: NAME",
                        "Type programs or store to see the ids, and check the spelling.",
                        "NAME needs its install disc in a linked drive",
                        "Link a drive and put the disc in it ([](jsc:drives)).",
                        "could not resolve mirror:// - no package source reachable",
                        "The computer is not on a network with a Mirror. Connect it to one ([](jsc:mirror)).",
                        "No package matches NAME.",
                        "Type search with no text to list the packages the Mirror offers.",
                        "this verb needs a package name",
                        "Type a package name after install or remove.",
                        "The installation could not be written to the disk (no space or no disk).",
                        "Free some space on the disk, or use a larger disk.")
                .seeAlso("jsc:programs", "jsc:mirror")
                .register();
    }

    private static void networkVerbs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("network_verbs", section).titled("Network verbs: net, iql, interac and ssh")
                .paragraph("A network is the computers joined by data cables, with one Mainframe that coordinates them"
                        + " ([](jsc:what_a_network_is)). net describes it, iql asks it questions, interac works"
                        + " with its"
                        + " items, and ssh opens a shell on another computer of it.")
                .define("Mainframe", "The computer that coordinates a network. Each network has one.")
                .table("Verbs of the network", "Verb", "Words after it, and what it does")
                .row(GuideCell.code("net (network)"),
                        GuideCell.words("No words. Summarises the network: its id, whether"
                        + " a Mainframe is there, and how many servers, computers and subframes it has."))
                .row(GuideCell.code("iql (operation, op, sql)"),
                        GuideCell.words("A statement. Runs one IQL statement on"
                        + " the network and prints the answer."))
                .row(GuideCell.code("interac"), GuideCell.words("Optional words. Works with the network's items: finds,"
                        + " fetches, puts back and crafts them."))
                .row(GuideCell.code("ssh"), GuideCell.words("A computer's name. Opens a shell on another computer of"
                        + " the network."))
                .subheading("net")
                .paragraph("net prints the rows network, mainframe, servers, computers, subframes and indexed"
                        + " types. The"
                        + " mainframe row reads present, or MISSING when there is no Mainframe.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "net"))
                .subheading("iql")
                .paragraph("iql runs one IQL statement on the network. Type iql, a space and the statement, such as iql"
                        + " SELECT 64 Cobblestone. A SELECT prints each item it finds as a row with its count, and it"
                        + " shows at most 64 rows. When nothing matches, it says no rows. A change, such as INSERT,"
                        + " prints the engine's words in green.")
                .paragraph("A statement that defines a view, a procedure or a job is answered as in the IQL Server"
                        + " Management Studio ([](jsc:studio_overview)). The grammar of IQL is taught in"
                        + " [](jsc:iql), so this page does not repeat it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "iql"))
                .subheading("interac")
                .paragraph("interac works the network's items. It finds what the network holds, fetches an item into"
                        + " your hands, puts items back and crafts them. Its words include get, put, craft, where,"
                        + " info,"
                        + " list and ops. Alone, interac takes the whole screen, and [[F10]] gives the terminal back.")
                .table("Options of interac", "Option", "What it does")
                .row(GuideCell.code("/LOCAL"), GuideCell.words("Leaves what interac takes in this computer, not in"
                        + " your hands."))
                .row(GuideCell.code("/S:COUNT"), GuideCell.words("Orders a listing by how much there is. --sort name"
                        + " orders it by name."))
                .paragraph("For example, interac get 42 cobblestone takes 42 cobblestone into your hands, through this"
                        + " computer. An item name can be short, such as cobblestone, or full, such as"
                        + " minecraft:cobblestone. When a name fits more than one thing, interac lists them and asks"
                        + " you to say which one.")
                .paragraph("This page covers the commonest verbs. The Operations behind interac's work are described in"
                        + " [](jsc:operations_catalogue).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "interac"))
                .subheading("ssh")
                .paragraph("ssh, the secure shell, opens a shell on another computer of this network, so that you"
                        + " can type"
                        + " commands there. Type ssh and the name of the computer, as the list shows it. The prompt"
                        + " lists the reachable computers, each with its node number, or (offline) when it is down."
                        + " Type exit to come back ([](jsc:shell_verbs)).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "ssh"))
                .ifSomethingGoesWrong("not on a network",
                        "The computer must be on a data network: a data cable to a router, or to the Mainframe"
                                + " ([](jsc:data_cables)).",
                        "iql says syntax: and then the parser's words.",
                        "The message names the word where the parser stopped. Fix that word. The grammar is in"
                                + " [](jsc:iql).",
                        "iql SELECT says no rows.",
                        "Nothing matches the name. Use interac list with part of the name to check its spelling.",
                        "no other computers reachable on this network",
                        "No other computer of this network is up and reachable. Try again when one is.")
                .seeAlso("jsc:what_a_network_is", "jsc:first_network", "jsc:iql")
                .register();
    }

    private static void mainframeVerbs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prompt_mainframe_verbs", section).titled("Looking after the network from the prompt")
                .paragraph("A few verbs work on the network itself, so the prompt offers them only on the computer"
                        + " that keeps that part of the network. Type one anywhere else, and the prompt says it does"
                        + " not know the command.")
                .table("Where each verb is offered", "Verb", "Offered on")
                .row(GuideCell.code("iqlengine"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("services"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("analyze"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("reindex"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("vacuum"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("mirror"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("cluster"), GuideCell.words("A Cluster Management Computer"))
                .row(GuideCell.code("gateway"), GuideCell.words("A computer with ports, for its Network Gateways"))
                .paragraph("The Mainframe's verbs are there only while it is on a network. A Mainframe running MC-DOS"
                        + " or Frames offers them at its Command Prompt.")
                .subheading("The engine and the services")
                .paragraph("iqlengine starts and stops the Midsoft IQL Server, the engine that plans the network's"
                        + " work ([](jsc:midsoft_iql_server)). Type iqlengine start, iqlengine stop or iqlengine"
                        + " status. services lists the network's services, each with its state. The prompt offers"
                        + " both only while the Midsoft IQL Server is installed on the Mainframe.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "iqlengine"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "services"))
                .subheading("The storage index")
                .paragraph("The Mainframe keeps the storage index: its list of which disks hold which items, so the"
                        + " network finds an item without reading every disk. Three verbs look after it.")
                .define("Storage index", "The Mainframe's list of which disks of the network hold which items.")
                .define("Ghost entry", "An item the storage index still lists, though no disk holds it any more.")
                .table("Looking after the index", "Verb", "What it does", "What it says")
                .row(GuideCell.code("analyze"), GuideCell.words("Checks the index against the disks."),
                        GuideCell.code("ANALYZE complete - 12 types reconciled"))
                .row(GuideCell.code("reindex"), GuideCell.words("Reads every disk again and builds the index anew."
                        + " The new index takes over a tick or two later."),
                        GuideCell.code("REINDEX started - rebuilding the catalog from disks"))
                .row(GuideCell.code("vacuum"), GuideCell.words("Clears the ghost entries."),
                        GuideCell.code("VACUUM freed 3 ghost entries"))
                .paragraph("When you pull a drive out of a running bay that has no Integrity Monitor, the index stops"
                        + " trusting that bay until you type reindex ([](jsc:network_services)). Learning IQL explains"
                        + " the same three jobs as statements ([](jsc:maintenance_analyze_vacuum_reindex)).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "analyze"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "reindex"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "vacuum"))
                .subheading("The Mirror")
                .paragraph("mirror install puts the Mirror on the Mainframe: the service that pckmgr fetches packages"
                        + " from ([](jsc:mirror)). mirror status says whether it serves. It answers serving, installed"
                        + " (Mainframe off) or not installed.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "mirror"))
                .subheading("Clusters and gateways")
                .paragraph("cluster does at the prompt what the Cluster Manager does in its window"
                        + " ([](jsc:cluster_management_computers)). It lists the network's supercomputers and"
                        + " datacenters, shows the nodes of one, turns them on or off, and installs a system or a"
                        + " program on them.")
                .paragraph("gateway does what the Gateway Manager does for the Network Gateways on this computer's"
                        + " ports, the bridge to ComputerCraft ([](jsc:network_gateway)). It lists them, shows the"
                        + " status, permissions, computers and log of one, and changes its settings.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "cluster"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "gateway"))
                .ifSomethingGoesWrong(
                        "The prompt says it does not know the command.",
                        "Type it at the prompt of the computer the table names for it.",
                        "iqlengine says the Midsoft IQL Server is already running.",
                        "It is serving already. Type iqlengine status to check it.",
                        "iqlengine says the Midsoft IQL Server is already stopped.",
                        "Type iqlengine start to start it.",
                        "mirror says the Mirror is already installed.",
                        "Type mirror status to see whether it serves.")
                .seeAlso("jsc:mainframes", "jsc:engine_services", "jsc:mirror")
                .register();
    }

    private static void editorsAndSigma(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prompt_editors_and_sigma", section).titled("Editors and Sigma at the prompt")
                .paragraph("Some programs work inside the Command Prompt window instead of opening their own. Two"
                        + " editors take over the window while you change a file, and the tools of Sigma compile and"
                        + " run your programs there. Install each one first ([](jsc:software_verbs)).")
                .subheading("vim and emacs")
                .paragraph("Type vim or emacs, a space and the name of a file. The editor takes the whole window, and"
                        + " gives it back when you leave. The file does not have to exist: it is made when you first"
                        + " save it.")
                .table("Saving and leaving", "Editor", "To save", "To leave")
                .row(GuideCell.code("vim"), GuideCell.words("Press Esc, then type :w and press Enter."),
                        GuideCell.words("Press Esc, then type :q and press Enter. :wq saves and leaves at once."))
                .row(GuideCell.code("emacs"), GuideCell.words("Press Ctrl+X, then Ctrl+S."),
                        GuideCell.words("Press Ctrl+X, then Ctrl+C."))
                .paragraph("vim starts in normal mode, where a letter is a command. Press i to type into the file, and"
                        + " Esc to stop typing. The Linux Desk Reference lists every key of both editors"
                        + " ([](jsc:vim)).")
                .command(CommandGuidePages.id(Platform.MC_DOS, "vim"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "emacs"))
                .subheading("Sigma")
                .paragraph("Sigma and Sigma# are the programming languages of these computers. The Book of Sigma"
                        + " teaches them ([](jsc:sigma_two_languages)). Their tools are verbs of the prompt, once you"
                        + " install their packages.")
                .table("The verbs of Sigma", "Verb", "Comes with the package", "What it does")
                .row(GuideCell.code("scc"), GuideCell.code("scc"),
                        GuideCell.words("Compiles a Sigma file, which ends in .sg, into a listing that ends in .asm."))
                .row(GuideCell.code("sgsc"), GuideCell.code("sgsc"),
                        GuideCell.words("Compiles a Sigma# file, which ends in .sgs, into a listing."))
                .row(GuideCell.code("sigma"), GuideCell.code("sigma"),
                        GuideCell.words("Runs a listing with sigma run, stops one with sigma stop, and lists what"
                                + " runs with sigma ps."))
                .row(GuideCell.code("sgpack"), GuideCell.code("sigma"),
                        GuideCell.words("Starts a package, builds it, and puts it on the Mirror or takes it back."))
                .paragraph("On the Frames editions, pckmgr install fetches these packages from the Mirror. MC-DOS"
                        + " installs them from their media only. The Sigma# compiler and the runtime need a Legacy"
                        + " computer or later, and on Frames they need Frames XP or later, so Frames 95 holds the Sigma"
                        + " compiler alone ([](jsc:sigma_installing)).")
                .steps("Install the packages you need, such as sgsc and sigma.",
                        "Write your program in a file that ends in .sgs, such as HELLO.SGS.",
                        "Type sgsc HELLO.SGS to compile it. The compiler writes the listing beside it.",
                        "Type sigma run and the listing's name to run it.")
                .command(CommandGuidePages.id(Platform.MC_DOS, "scc"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "sgsc"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "sigma"))
                .command(CommandGuidePages.id(Platform.MC_DOS, "sgpack"))
                .ifSomethingGoesWrong(
                        "The prompt says it does not know vim, emacs or a verb of Sigma.",
                        "Install the program or the package that brings it, then type it again.",
                        "pckmgr says it could not resolve mirror://.",
                        "Put the computer on a network whose Mainframe runs the Mirror ([](jsc:mirror)).")
                .seeAlso("jsc:software_verbs", "jsc:sigma_installing", "jsc:sigma_compile")
                .register();
    }
}
