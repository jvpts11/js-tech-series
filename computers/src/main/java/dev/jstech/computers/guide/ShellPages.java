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
 * The pages of the shell chapter, shared by the three books of the Unix family: the prompt, the tree of folders,
 * the syntax of a line, and the commands that Linux, FreeBSD and UNIX System V share or hold alone.
 */
final class ShellPages {

    private ShellPages() {
    }

    /** Declares the sections and entries of the shell, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        basics(guide, chapter);
        commands(guide, chapter);
        software(guide, chapter);
    }

    private static void basics(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("shell_basics").titled("The shell").in(chapter)
                .register();
        shellPrompt(guide, section);
        fileTree(guide, section);
        pipesAndVariables(guide, section);
    }

    private static void commands(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("shell_commands").titled("Commands").in(chapter)
                .register();
        fileCommands(guide, section);
        textTools(guide, section);
        systemCommands(guide, section);
        processesAndJobs(guide, section);
        devicesCommands(guide, section);
        printing(guide, section);
        networkCommands(guide, section);
    }

    private static void software(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("shell_software").titled("Software").in(chapter)
                .register();
        softwareCommands(guide, section);
        mainframeCommands(guide, section);
        sigmaCommands(guide, section);
    }

    private static void shellPrompt(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("shell_prompt", section).titled("What a shell is, and its prompt")
                .paragraph("A shell is the program that reads the line you type, runs the command named in that line,"
                        + " and prints what the command answers. You type into the terminal, the text screen of a"
                        + " computer, at the prompt: the short line of text that waits for you.")
                .paragraph("Each computer has one prompt, and you type into it from the computer's own screen."
                        + " Linux, FreeBSD and UNIX System V share one set of commands and one way of reading a"
                        + " line. A command that one of these systems lacks is not in its list, so it cannot run"
                        + " there.")
                .paragraph("You log in as player on all three systems, and the prompt shows that name first. The"
                        + " part after the at sign is the computer's own name, which you change with the config"
                        + " command ([](jsc:system_commands)).")
                .subheading("The prompt on each system")
                .table("Prompt by system", "System", "Shell", "Prompt")
                .row(GuideCell.words("Ubuntu, Debian and Fedora"), GuideCell.words("bash"),
                        GuideCell.code("player@<name>:<folder>$"))
                .row(GuideCell.words("Gentoo"), GuideCell.words("bash"),
                        GuideCell.code("player@<name>:<folder>$"))
                .row(GuideCell.words("Arch Linux"), GuideCell.words("zsh"),
                        GuideCell.code("player@<name> <folder> %"))
                .row(GuideCell.words("FreeBSD"), GuideCell.words("sh"), GuideCell.code("root@<name>:<folder> #"))
                .row(GuideCell.words("UNIX System V"), GuideCell.words("sh"),
                        GuideCell.code("player@<name>:<folder> $"))
                .paragraph("The folder part shows where you are. The sign ~ stands for your home folder, the folder"
                        + " your files live in: /home/player on Linux and FreeBSD, and /usr/player on UNIX System"
                        + " V.")
                .paragraph("On FreeBSD the prompt shows root and ends in #, because a fresh FreeBSD machine opens"
                        + " as root. These systems have no sudo and no su command, so you do not change accounts"
                        + " with a command.")
                .define("Shell", "The program that reads the lines you type and runs the commands in them.")
                .define("Prompt", "The text before the cursor, which names who you are, on which computer and in"
                        + " which folder.")
                .define("Root", "The all-powerful account of a system. On FreeBSD the prompt names that account and"
                        + " ends in the sign #.")
                .define("Terminal", "The text screen of a computer, with a keyboard, where you type commands and read"
                        + " the answers.")
                .define("Command", "A program you call by its name, with the words after the name that say what it"
                        + " works on.")
                .ifSomethingGoesWrong("I cannot find the command I need.",
                        "Type help on Linux or UNIX System V to list the commands; FreeBSD has no help command."
                                + " The apropos command with one word finds commands by what they do"
                                + " ([](jsc:whatis_and_apropos)).",
                        "The shell says the name is not found, and suggests apropos or man intro.",
                        "apropos with one word finds the command you want. man intro reads a manual entry on FreeBSD"
                                + " only.",
                        "A command the manual lists says it is not found.",
                        "The command belongs to a package you have not installed. The which command says whether it"
                                + " is here and what put it there ([](jsc:software_commands)).")
                .seeAlso("jsc:file_tree", "jsc:help_command")
                .register();
    }

    private static void fileTree(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("file_tree", section).titled("The tree of folders and paths")
                .paragraph("Unix shows all of a computer's storage as one tree of folders. The tree starts at the"
                        + " top folder, written /, and every file sits somewhere inside it.")
                .paragraph("Under the hood the storage keeps the DOS (Disk Operating System) model of drive"
                        + " letters, and the shell translates it. The system disk is C: and it is the top folder."
                        + " Another disk appears as a folder inside /media on Linux and FreeBSD, or inside /mnt on"
                        + " UNIX System V.")
                .table("Folders you use", "Folder", "Linux and FreeBSD", "UNIX System V")
                .row(GuideCell.words("Your home folder"), GuideCell.code("/home/player"),
                        GuideCell.code("/usr/player"))
                .row(GuideCell.words("Your Desktop, whose files the desktop shows as icons"),
                        GuideCell.code("/home/player/Desktop"), GuideCell.code("/usr/player/Desktop"))
                .row(GuideCell.words("Your Documents"), GuideCell.code("/home/player/Documents"),
                        GuideCell.code("/usr/player/Documents"))
                .row(GuideCell.words("The other drives, one folder each"), GuideCell.code("/media"),
                        GuideCell.code("/mnt"))
                .paragraph("You name a folder or file with a path. The first character tells the shell where to start.")
                .table("How a path starts", "What you type", "What it means")
                .row(GuideCell.code("/x/y"), GuideCell.words("An absolute path: it starts at the top folder and"
                        + " names the whole address."))
                .row(GuideCell.code("x/y"), GuideCell.words("A relative path: it starts at the folder you are in."))
                .row(GuideCell.code("~"), GuideCell.words("Your home folder. The form ~/x is a folder inside it."))
                .row(GuideCell.code(".."), GuideCell.words("The folder above the one you are in."))
                .row(GuideCell.code("/media/d/x"), GuideCell.words("The file x on drive D:, on Linux and FreeBSD."
                        + " On UNIX System V the same file is /mnt/d/x."))
                .paragraph("Commands that name a disk use its device name. The system disk is sda1, drive D: is sdb,"
                        + " drive E: is sdc, and each drive after that goes on by one letter.")
                .define("Path", "The address of a file or folder: its folder names, with a slash between them.")
                .ifSomethingGoesWrong("I cannot find my file.",
                        "Run pwd to see the folder you are in. A path that starts with a slash begins at the top"
                                + " folder, not at your folder.",
                        "The df command shows the device with \"(no medium)\" after it.",
                        "Put a disk in that drive, then run df again ([](jsc:file_commands)).")
                .seeAlso("jsc:shell_prompt", "jsc:file_commands")
                .register();
    }

    private static void pipesAndVariables(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("pipes_and_variables", section)
                .titled("Pipes, redirection, background jobs, variables, wildcards and Tab")
                .paragraph("A few small signs change what a command reads and where its answer goes. They work on"
                        + " Linux, FreeBSD and UNIX System V, and this entry says where the three differ.")
                .subheading("Pipes and redirection")
                .paragraph("A pipe, written with a bar, takes what the command on its left prints and gives it to the"
                        + " command on its right. Two commands joined this way work as one.")
                .paragraph("The shell reads a bar or an ampersand only as a word of its own. Written as ls|grep x, the"
                        + " whole thing is one word, and no pipe is made.")
                .table("Operators", "Operator", "What it does")
                .row(GuideCell.code("|"), GuideCell.words("Sends the answer of the left command to the right one."))
                .row(GuideCell.code(">"), GuideCell.words("Writes the answer to a file, and replaces what the file"
                        + " held."))
                .row(GuideCell.code(">>"), GuideCell.words("Adds the answer to the end of a file."))
                .row(GuideCell.code("<"), GuideCell.words("Reads a file as the input of the command."))
                .row(GuideCell.code("&"), GuideCell.words("Runs the command in the background, as the last word of"
                        + " the line."))
                .paragraph("The file name may stick to the arrow, as in >notes.txt, or follow it as its own word, as"
                        + " in > notes.txt. An arrow with no file name is a syntax error. A quoted operator is plain"
                        + " text, so a quoted bar is a plain character.")
                .paragraph("A pipe carries at most 4,096 lines from one command to the next.")
                .paragraph("Some commands take the whole terminal, so they cannot be piped or redirected: less, vim,"
                        + " emacs, vi and ee, info on Linux, and man when it opens a manual entry.")
                .define("Pipe", "A connection that gives what one command prints to the next command as its input.")
                .define("Redirection", "Sending a command's answer to a file with the arrows > and >>, or reading"
                        + " its input from a file with <.")
                .subheading("Background jobs")
                .paragraph("Put an ampersand as the last word of a line, and the command runs in the background. You"
                        + " get the prompt back at once, and you can go on typing while the command works.")
                .define("Job", "A command you started with the ampersand and left to run in the background. It has a"
                        + " number, which you use to list it or stop it.")
                .paragraph("Each background job takes 1 MB of the machine's memory. When no room is left, the shell"
                        + " refuses the new job.")
                .subheading("Variables")
                .paragraph("A variable is a name that stands for a text value. You give it a value with NAME=value on"
                        + " a line of its own, and you use it with a dollar sign: $NAME, or ${NAME} when letters"
                        + " follow the name directly.")
                .define("Variable", "A name that stands for a text value, such as USER for the login name.")
                .paragraph("A name holds only letters, digits and underscores, and it does not start with a digit."
                        + " The set command gives a value on every machine. The export command makes a value visible"
                        + " to the programs the shell runs, and unset forgets a name.")
                .table("Names the machine answers", "Name", "Value")
                .row(GuideCell.code("HOSTNAME"), GuideCell.words("The computer's name."))
                .row(GuideCell.code("USER"), GuideCell.words("The login name, which is always player."))
                .row(GuideCell.code("OS"), GuideCell.words("The system: Linux, FreeBSD or UNIX."))
                .row(GuideCell.code("PWD"), GuideCell.words("The folder you are in."))
                .row(GuideCell.code("PATH"), GuideCell.words("Set by default only on UNIX System V; on Linux and"
                        + " FreeBSD it is empty until you set it."))
                .paragraph("A variable belongs to the computer, not to one window, so a name you set stays set for"
                        + " the whole computer.")
                .subheading("Wildcards")
                .paragraph("A star stands for any text in a file name. An unquoted word with a star opens into the"
                        + " names of the matching files in its folder, before the command runs.")
                .define("Wildcard", "A symbol, such as the star, that stands for other text in names. The shell"
                        + " replaces it with the matching names.")
                .paragraph("Put the word in quotes, as in \"notes*\", and the shell keeps the star as you wrote it."
                        + " On MC-DOS and the Frames systems the command gets the star itself.")
                .subheading("Tab")
                .paragraph("Press Tab to complete a word you have started. On the first word, Tab completes a command"
                        + " name. After a command such as mkfs.ext4 or mount, it completes a device name. Tab does not"
                        + " complete file or folder names.")
                .paragraph("Press Tab twice in a row, and the shell prints every possible completion above the"
                        + " prompt.")
                .subheading("What is not here")
                .paragraph("Some shell features of other systems are missing from these three. The table says what"
                        + " each one does here.")
                .table("Missing here", "Feature", "What happens")
                .row(GuideCell.code(";"), GuideCell.words("The semicolon, && and || are plain words that the"
                        + " command receives as text."))
                .row(GuideCell.code("2>"), GuideCell.words("There is no way to send errors to a file; only the"
                        + " arrows above exist."))
                .row(GuideCell.code("$(command)"),
                        GuideCell.words("A command cannot take the place of its own answer."))
                .row(GuideCell.code("alias"), GuideCell.words("There is no alias command, and no ! history form, on"
                        + " any of these systems."))
                .row(GuideCell.code("find"), GuideCell.words("Not on Unix, where find is a DOS command; grep"
                        + " searches the lines of a file instead."))
                .row(GuideCell.code("more"), GuideCell.words("Not on Unix, where the command for reading by the"
                        + " page is less."))
                .ifSomethingGoesWrong("The shell says syntax error near unexpected token, followed by a bar or an"
                        + " arrow.",
                        "A pipe needs a command on each side, and an arrow needs a file name. Put a space around the"
                                + " bar.",
                        "The shell says the command takes the whole terminal, so it cannot be piped or redirected.",
                        "Run the command alone. To read a long answer, pipe the command into grep or head to keep only"
                                + " the lines you need, or write the answer to a file and read it with less.",
                        "The shell says the machine has no memory left for another job.",
                        "Stop a finished or unwanted job with kill %<id> ([](jsc:processes_and_jobs)).",
                        "ls | wc -l prints 1.",
                        "The ls command prints every name on one line unless you give -l. Use ls -l, which prints one"
                                + " line for each name.",
                        "The shell says a name is letters, digits and underscores, and does not start with a digit.",
                        "Choose a variable name with only letters, digits and underscores, and do not start it with a"
                                + " digit.")
                .seeAlso("jsc:shell_prompt", "jsc:processes_and_jobs")
                .register();
    }

    private static void fileCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("file_commands", section).titled("Working with files and folders")
                .paragraph("Most of your work on a Unix system is with files and folders: you list them, move between"
                        + " them, copy, rename and delete them. These commands need a disk, the system disk or one in"
                        + " a drive.")
                .paragraph("Here is a short session. It shows how the shell answers when a command gets no file"
                        + " name.")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .typed("player@ubuntu:~$ ", "cat")
                .answer("cat: usage: cat <file>")
                .paragraph("Folder names end with a slash in the ls listing, so you can tell files and folders apart"
                        + " at a glance. The commands below are the commonest first.")
                .steps("Type write notes.txt followed by the text you want in the file.",
                        "Type cat notes.txt to read the file back.",
                        "Type ls to see the file in the folder.")
                .command(CommandGuidePages.id(Platform.LINUX, "ls"))
                .command(CommandGuidePages.id(Platform.LINUX, "cd"))
                .command(CommandGuidePages.id(Platform.LINUX, "pwd"))
                .command(CommandGuidePages.id(Platform.LINUX, "cat"))
                .command(CommandGuidePages.id(Platform.LINUX, "mkdir"))
                .command(CommandGuidePages.id(Platform.LINUX, "touch"))
                .command(CommandGuidePages.id(Platform.LINUX, "write"))
                .command(CommandGuidePages.id(Platform.LINUX, "cp"))
                .command(CommandGuidePages.id(Platform.LINUX, "mv"))
                .command(CommandGuidePages.id(Platform.LINUX, "rm"))
                .command(CommandGuidePages.id(Platform.LINUX, "rmdir"))
                .command(CommandGuidePages.id(Platform.LINUX, "run"))
                .command(CommandGuidePages.id(Platform.LINUX, "du"))
                .command(CommandGuidePages.id(Platform.LINUX, "df"))
                .paragraph("The df command lists every disk the computer has, with its device name. The mkfs.ext4"
                        + " command makes a new, empty file system on one device.")
                .command(CommandGuidePages.id(Platform.LINUX, "mkfs.ext4"))
                .warning("mkfs.ext4 erases the whole device. Everything on it is lost, and the erase cannot be"
                        + " undone.")
                .ifSomethingGoesWrong("The shell says usage: cat <file>, because you gave no file name.",
                        "Give the name of one file after cat.",
                        "The mkfs.ext4 command says it cannot open a device, and that there is no such device.",
                        "Run df and choose one of the devices it lists.",
                        "The df command says there are no filesystems mounted.",
                        "No disk is in the system or in a drive. Put one in, then run df again.")
                .seeAlso("jsc:file_tree", "jsc:text_tools")
                .register();
    }

    private static void textTools(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("text_tools", section).titled("Reading and sorting text")
                .paragraph("Text tools read lines of a file, or the lines that a pipe brings in. They count the lines,"
                        + " pick some out, put them in order, or show them one screen at a time.")
                .paragraph("A line is the text between two line breaks. Most of these tools count or choose by line.")
                .define("Pager", "A program that shows one screen of text at a time, so you can read a long file.")
                .command(CommandGuidePages.id(Platform.LINUX, "grep"))
                .command(CommandGuidePages.id(Platform.LINUX, "wc"))
                .command(CommandGuidePages.id(Platform.LINUX, "head"))
                .command(CommandGuidePages.id(Platform.LINUX, "tail"))
                .command(CommandGuidePages.id(Platform.LINUX, "sort"))
                .subheading("Keys in less")
                .paragraph("less shows a file one page at a time. These keys move you through it, and each one"
                        + " works while less holds the whole terminal.")
                .table("Keys in less", "Key", "What it does")
                .row(GuideCell.code("Space"), GuideCell.words("Shows the next page."))
                .row(GuideCell.code("b"), GuideCell.words("Shows the page before."))
                .row(GuideCell.code("Arrow keys"), GuideCell.words("Move one line at a time."))
                .row(GuideCell.code("/"), GuideCell.words("Searches the text for what you type next."))
                .row(GuideCell.code("n"), GuideCell.words("Goes to the next match of the search."))
                .row(GuideCell.code("q"), GuideCell.words("Leaves less and gives you back the prompt."))
                .command(CommandGuidePages.id(Platform.LINUX, "less"))
                .ifSomethingGoesWrong("grep shows no lines at all.",
                        "grep matches the exact text you type, so check the spelling. Add -i to ignore upper and"
                                + " lower case.",
                        "less says it cannot be piped.",
                        "Type less alone, with a file name. To read only some lines, first pick them with grep, then"
                                + " read the result in a file with less.")
                .seeAlso("jsc:file_commands", "jsc:pipes_and_variables")
                .register();
    }

    private static void systemCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("system_commands", section).titled("Machine and system information")
                .paragraph("These commands tell you about the computer you use: its system, its name, its power and"
                        + " its settings. Some work on every machine, and the others only on the Unix systems, as"
                        + " each block says.")
                .paragraph("The config command shows and changes the settings of the computer. With no argument it"
                        + " lists them. With a key and a value, as in config name <name>, it changes one setting.")
                .table("Two settings of config", "Setting", "What it takes")
                .row(GuideCell.code("name"), GuideCell.words("The computer's name, which the prompt shows after the at"
                        + " sign."))
                .row(GuideCell.code("netshare"), GuideCell.words("A number from 0 to 1000, in thousandths (permille)."))
                .paragraph("The config command has more settings, such as wallpaper and theme.")
                .command(CommandGuidePages.id(Platform.LINUX, "uname"))
                .command(CommandGuidePages.id(Platform.LINUX, "hostname"))
                .command(CommandGuidePages.id(Platform.LINUX, "date"))
                .command(CommandGuidePages.id(Platform.LINUX, "status"))
                .command(CommandGuidePages.id(Platform.LINUX, "config"))
                .command(CommandGuidePages.id(Platform.LINUX, "reboot"))
                .paragraph("The reboot command can open the firmware instead of the system: its --firmware option"
                        + " enters the setup of the [firmware](jsc:firmware).")
                .command(CommandGuidePages.id(Platform.LINUX, "devices"))
                .command(CommandGuidePages.id(Platform.LINUX, "which"))
                .command(CommandGuidePages.id(Platform.LINUX, "whoami"))
                .command(CommandGuidePages.id(Platform.LINUX, "echo"))
                .command(CommandGuidePages.id(Platform.LINUX, "clear"))
                .command(CommandGuidePages.id(Platform.LINUX, "set"))
                .command(CommandGuidePages.id(Platform.LINUX, "export"))
                .command(CommandGuidePages.id(Platform.LINUX, "unset"))
                .paragraph("You will not find a version command on these systems. It belongs to MC-DOS and the"
                        + " Frames systems, where it shows the shell's version.")
                .ifSomethingGoesWrong("The config command says unknown setting, followed by the name you typed.",
                        "Run config with no argument to see the setting names.",
                        "The config command says netshare needs a number from 0 to 1000.",
                        "Give a whole number from 0 to 1000.",
                        "whoami shows no login name.",
                        "Type echo \"$USER\", which prints player.")
                .seeAlso("jsc:devices_commands", "jsc:shell_prompt")
                .register();
    }

    private static void processesAndJobs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("processes_and_jobs", section).titled("Processes, jobs and scheduling")
                .paragraph("A process is a program that is running now. Each process has a number, its process ID"
                        + " (PID), and the ps command lists them with it. It lists the system's own processes first."
                        + " Number 1 is the first process, which starts every other one: systemd on most Linux"
                        + " distributions, and init on Gentoo, FreeBSD and UNIX. Each service the system runs comes"
                        + " next, and the programs you start are numbered from 1000.")
                .define("Process", "A program that is running. Every running program has a number, which ps lists"
                        + " as its PID.")
                .paragraph("The kill command stops a process by its number. The process stops where it stands, and any"
                        + " work it had not finished stays not done. A signal can come before the number, such as -9"
                        + " or -KILL, and kill -l lists the signals. A service you stop stays stopped until the"
                        + " computer starts again. Stopping process 1 takes the whole system down: the kernel panics,"
                        + " prints why on the screen, and the computer starts over by itself.")
                .paragraph("A job is a background command you started with an ampersand. You list your jobs with"
                        + " jobs, and you stop one with kill %1, where 1 is its number in that list.")
                .command(CommandGuidePages.id(Platform.LINUX, "ps"))
                .command(CommandGuidePages.id(Platform.LINUX, "kill"))
                .paragraph("When the computer runs out of memory, the kernel, the core of the system, ends the"
                        + " process that holds the most, then the next, until everything left fits. The dmesg command"
                        + " prints what the kernel has logged since the computer started: what it found as it came up,"
                        + " and a line for each process it ended. On a desktop, a window the kernel ends closes with"
                        + " a balloon, \"Out of memory\", that names it. A program ended in front of the terminal"
                        + " leaves the word Killed. Only when nothing is left to end does the kernel panic, and then"
                        + " the computer starts over by itself.")
                .command(CommandGuidePages.id(Platform.LINUX, "dmesg"))
                .command(CommandGuidePages.id(Platform.LINUX, "jobs"))
                .paragraph("The crontab command sets a line that the machine runs by itself, at an hour you choose,"
                        + " every day or on named days. Such a line is a schedule.")
                .define("Schedule", "A line that the machine runs by itself, at a set hour, every day or on named"
                        + " days.")
                .table("Arguments of crontab", "Argument", "What it does")
                .row(GuideCell.code("-l"), GuideCell.words("Lists the lines the machine runs, and when."))
                .row(GuideCell.code("-r <id>"), GuideCell.words("Removes the line with that number."))
                .row(GuideCell.code("06:00"), GuideCell.words("The hour the line runs. The form 6 also works."))
                .row(GuideCell.code("M, T, W, Th, F, Sa, Su"), GuideCell.words("The days, separated by commas. Leave"
                        + " them out to run every day."))
                .command(CommandGuidePages.id(Platform.LINUX, "crontab"))
                .ifSomethingGoesWrong("The kill command says kill: not a process id.",
                        "Give the number of the process from the ps list, not its name.",
                        "The kill command says there is no such job.",
                        "The jobs command shows the current job numbers. Use one of them after the percent sign.",
                        "The crontab command says the word you gave is not an hour of the day.",
                        "Write the hour as \"06:00\" or as \"6\".")
                .seeAlso("jsc:pipes_and_variables", "jsc:system_commands")
                .register();
    }

    private static void devicesCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("devices_commands", section).titled("Hardware lists: lspci, lsusb, pciconf and usbconfig")
                .paragraph("A computer has parts on its Peripheral Component Interconnect (PCI) bus, the bus that"
                        + " expansion cards and built-in parts sit on. It has devices on its Universal Serial Bus"
                        + " (USB), the bus for plugged-in devices.")
                .define("PCI", "Peripheral Component Interconnect: the bus on the board that expansion cards and"
                        + " built-in parts sit on.")
                .define("USB", "Universal Serial Bus: the bus for plugged-in devices.")
                .paragraph("Each place on a bus is a slot, and a slot is written as bus:device:function. The commands"
                        + " below list what sits in those slots. Linux has lspci and lsusb, and FreeBSD has pciconf"
                        + " and usbconfig.")
                .command(CommandGuidePages.id(Platform.LINUX, "lspci"))
                .command(CommandGuidePages.id(Platform.LINUX, "lsusb"))
                .command(CommandGuidePages.id(Platform.FREEBSD, "pciconf"))
                .command(CommandGuidePages.id(Platform.FREEBSD, "usbconfig"))
                .paragraph("The devices command is a different list. It shows the peripherals linked to your computer,"
                        + " and it is on the system commands page ([](jsc:system_commands)).")
                .ifSomethingGoesWrong("The command says no hardware to read.",
                        "Run it on the computer whose parts you want to list.",
                        "The shell says lspci is not found on FreeBSD.",
                        "Use pciconf on FreeBSD. The lspci command is a Linux command.")
                .seeAlso("jsc:system_commands", "jsc:file_commands")
                .register();
    }

    private static void printing(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("printing", section).titled("Printing from the terminal")
                .paragraph("You print from the terminal with two commands. The lp command sends files to a printer, and"
                        + " the lpstat command shows the print requests and the printers.")
                .define("Destination", "The printer that a print job goes to.")
                .define("Copies", "How many prints of each file one job makes.")
                .paragraph("Name the destination with -d, or leave it out to use the default one. The number of copies"
                        + " comes with -n.")
                .command(CommandGuidePages.id(Platform.LINUX, "lp"))
                .command(CommandGuidePages.id(Platform.LINUX, "lpstat"))
                .ifSomethingGoesWrong("The lp command says there is no default destination.",
                        "Name a printer with -d, and check its name with lpstat.",
                        "The lp command says the destination is non-existent.",
                        "Type the name exactly as lpstat shows it.",
                        "The lp command says there are no files to print.",
                        "Give at least one file name after the command.")
                .seeAlso("jsc:shell_commands", "jsc:pipes_and_variables")
                .register();
    }

    private static void networkCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("network_commands", section).titled("The network from the prompt")
                .paragraph("These commands use the data network of your computer, the cables and the storage that join"
                        + " the computers ([](jsc:what_a_network_is)). Without a network, the net command says the"
                        + " computer is not on a network.")
                .paragraph("The network answers queries in IQL, its own query language ([](jsc:iql)). The iql command"
                        + " runs one IQL statement, and prints the answer, up to 64 rows.")
                .command(CommandGuidePages.id(Platform.LINUX, "net"))
                .command(CommandGuidePages.id(Platform.LINUX, "iql"))
                .paragraph("The interac command works the network's items. Alone, it takes the whole terminal and shows"
                        + " the network in three tabs: Network, Servers and Locked. With words, it does one thing and"
                        + " gives the prompt back.")
                .table("Options of interac", "Option", "What it does")
                .row(GuideCell.code("--to"), GuideCell.words("Leaves what you take in this computer, not in your"
                        + " hands."))
                .row(GuideCell.code("--sort name"),
                        GuideCell.words("Sorts the lines by name instead of by amount, which is the default."))
                .command(CommandGuidePages.id(Platform.LINUX, "interac"))
                .paragraph("The ssh command opens a shell on another computer of the network, and the exit"
                        + " command closes that remote shell and returns you to this computer. Typed with no"
                        + " name, ssh lists the computers it can reach.")
                .command(CommandGuidePages.id(Platform.LINUX, "ssh"))
                .command(CommandGuidePages.id(Platform.LINUX, "exit"))
                .ifSomethingGoesWrong("The net command says not on a network.",
                        "Connect the computer to a data cable ([](jsc:data_cables)).",
                        "The iql command says syntax, followed by a message.",
                        "The message says what is wrong with the statement. Check it against the IQL pages"
                                + " ([](jsc:iql)).",
                        "The ssh command says no other computers are reachable on this network.",
                        "Turn on the other computer, and check that it is on the same network.")
                .seeAlso("jsc:file_commands", "jsc:devices_commands")
                .register();
    }

    private static void softwareCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("software_commands", section).titled("Installing and removing software")
                .paragraph("There are two ways to add software to a Unix system, and this entry says which is which."
                        + " The store installs a program from an install disc in a drive joined to the computer."
                        + " A package manager installs and removes packages from the Mirror.")
                .paragraph("A package manager works only on the system that uses it. Removing software on these"
                        + " systems is the package manager's job: there is no uninstall command here, because"
                        + " uninstall is a DOS command. If you are not sure how a command got here, type which, and"
                        + " it says whether the system or a package put it there.")
                .table("Package managers", "System", "Manager", "Its verbs")
                .row(GuideCell.words("Ubuntu and Debian"), GuideCell.code("apt"),
                        GuideCell.words("install, remove, search, list, update and upgrade"))
                .row(GuideCell.words("Fedora"), GuideCell.code("dnf"), GuideCell.words("The same verbs as apt"))
                .row(GuideCell.words("Arch Linux"), GuideCell.code("pacman"),
                        GuideCell.words("-S to install, -R to remove, -Ss to search, -Q to list, -Syu to upgrade"))
                .row(GuideCell.words("Gentoo"), GuideCell.code("emerge"),
                        GuideCell.words("A name to install, --unmerge to remove, --search, --sync and --update @world"))
                .row(GuideCell.words("FreeBSD"), GuideCell.code("pkg"),
                        GuideCell.words("install, delete, search, info, update and upgrade"))
                .row(GuideCell.words("UNIX System V"), GuideCell.code("none"),
                        GuideCell.words("No package manager on this system"))
                .paragraph("For the verbs of apt and dnf, read the Linux Desk Reference ([](jsc:apt)). For pkg,"
                        + " read the Daemon Handbook ([](jsc:pkg)). A package install or removal needs the Mirror,"
                        + " the network service that keeps the packages ([](jsc:mirror)).")
                .subheading("Installing from the store")
                .steps("Type store to list the programs you can install on this computer.",
                        "Put the install disc of the program in a drive joined to the computer.",
                        "Type install and the program's name, such as screenfetch.",
                        "Type programs to see the installed programs, and check that yours is there.")
                .command(CommandGuidePages.id(Platform.LINUX, "install"))
                .command(CommandGuidePages.id(Platform.LINUX, "store"))
                .command(CommandGuidePages.id(Platform.LINUX, "programs"))
                .paragraph("The screenfetch program prints a short summary of the system, with labels such as OS,"
                        + " Kernel, Uptime, Packages and Shell. You install it first, like any program of the store.")
                .define("screenfetch", "A program that prints your system's logo beside a summary of the machine:"
                        + " its system, kernel, uptime, packages and shell.")
                .command(CommandGuidePages.id(Platform.LINUX, "screenfetch"))
                .ifSomethingGoesWrong("The install command says no such program.",
                        "The store command lists the programs you can install.",
                        "The install command says the program needs its install disc in a linked drive.",
                        "Put the disc in a drive joined to the computer, then run install again.",
                        "A package command says it could not resolve mirror.",
                        "Connect the computer to a network whose [Mainframe](jsc:mainframes) runs the Mirror.",
                        "The shell says screenfetch: not found.",
                        "Run install screenfetch first, then type screenfetch again.")
                .seeAlso("jsc:apt", "jsc:dnf", "jsc:arch_pacman_in_system", "jsc:pkg")
                .register();
    }

    private static void mainframeCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mainframe_commands", section).titled("Looking after the network from the prompt")
                .paragraph("A few commands work on the network itself, so the prompt offers them only on the computer"
                        + " that keeps that part of the network. Type one anywhere else, and the prompt says it does"
                        + " not know the command.")
                .table("Where each command is offered", "Command", "Offered on")
                .row(GuideCell.code("iqlengine"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("services"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("analyze"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("reindex"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("vacuum"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("mirror"), GuideCell.words("The network's Mainframe"))
                .row(GuideCell.code("cluster"), GuideCell.words("A Cluster Management Computer"))
                .row(GuideCell.code("gateway"), GuideCell.words("A computer with ports, for its Network Gateways"))
                .paragraph("The Mainframe is the computer that coordinates a network ([](jsc:mainframes)). Its"
                        + " commands are there only while it is on a network.")
                .subheading("The engine and the services")
                .paragraph("iqlengine starts and stops the Midsoft IQL Server, the engine that plans the network's"
                        + " work ([](jsc:midsoft_iql_server)). Type iqlengine start, iqlengine stop or iqlengine"
                        + " status. services lists the network's services, each with its state. The prompt offers"
                        + " both only while the Midsoft IQL Server is installed on the Mainframe.")
                .command(CommandGuidePages.id(Platform.LINUX, "iqlengine"))
                .command(CommandGuidePages.id(Platform.LINUX, "services"))
                .subheading("The storage index")
                .paragraph("The Mainframe keeps the storage index: its list of which disks hold which items, so the"
                        + " network finds an item without reading every disk. Three commands look after it.")
                .define("Storage index", "The Mainframe's list of which disks of the network hold which items.")
                .define("Ghost entry", "An item the storage index still lists, though no disk holds it any more.")
                .table("Looking after the index", "Command", "What it does", "What it says")
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
                .command(CommandGuidePages.id(Platform.LINUX, "analyze"))
                .command(CommandGuidePages.id(Platform.LINUX, "reindex"))
                .command(CommandGuidePages.id(Platform.LINUX, "vacuum"))
                .subheading("The Mirror")
                .paragraph("mirror install puts the Mirror on the Mainframe: the service your package manager fetches"
                        + " packages from ([](jsc:mirror)). mirror status says whether it serves. It answers serving,"
                        + " installed (Mainframe off) or not installed.")
                .command(CommandGuidePages.id(Platform.LINUX, "mirror"))
                .subheading("Clusters and gateways")
                .paragraph("cluster does at the prompt what the Cluster Manager does in its window"
                        + " ([](jsc:cluster_management_computers)). It lists the network's supercomputers and"
                        + " datacenters, shows the nodes of one, turns them on or off, and installs a system or a"
                        + " program on them.")
                .paragraph("gateway does what the Gateway Manager does for the Network Gateways on this computer's"
                        + " ports, the bridge to ComputerCraft ([](jsc:network_gateway)). It lists them, shows the"
                        + " status, permissions, computers and log of one, and changes its settings.")
                .command(CommandGuidePages.id(Platform.LINUX, "cluster"))
                .command(CommandGuidePages.id(Platform.LINUX, "gateway"))
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

    private static void sigmaCommands(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_at_the_prompt", section).titled("Sigma at the prompt")
                .paragraph("Sigma and Sigma# are the programming languages of these computers. Their tools are"
                        + " commands at the prompt, once you install their packages. The Book of Sigma teaches the"
                        + " languages themselves ([](jsc:sigma_two_languages)).")
                .table("The commands of Sigma", "Command", "Comes with the package", "What it does")
                .row(GuideCell.code("scc"), GuideCell.code("scc"),
                        GuideCell.words("Compiles a Sigma file, which ends in .sg, into a listing that ends in .asm."))
                .row(GuideCell.code("sgsc"), GuideCell.code("sgsc"),
                        GuideCell.words("Compiles a Sigma# file, which ends in .sgs, into a listing."))
                .row(GuideCell.code("sigma"), GuideCell.code("sigma"),
                        GuideCell.words("Runs a listing with sigma run, stops one with sigma stop, and lists what"
                                + " runs with sigma ps."))
                .row(GuideCell.code("sgpack"), GuideCell.code("sigma"),
                        GuideCell.words("Starts a package, builds it, and puts it on the Mirror or takes it back."))
                .paragraph("You install a package by its name, with your system's package manager"
                        + " ([](jsc:software_commands)). The packages come from the Mirror, so your computer must be"
                        + " on a network whose Mainframe runs it.")
                .steps("Install the packages you need, such as sgsc and sigma.",
                        "Write your program in a file that ends in .sgs, such as hello.sgs.",
                        "Type sgsc hello.sgs to compile it. The compiler writes hello.asm beside it.",
                        "Type sigma run hello.asm to run the listing.")
                .command(CommandGuidePages.id(Platform.LINUX, "scc"))
                .command(CommandGuidePages.id(Platform.LINUX, "sgsc"))
                .command(CommandGuidePages.id(Platform.LINUX, "sigma"))
                .command(CommandGuidePages.id(Platform.LINUX, "sgpack"))
                .ifSomethingGoesWrong(
                        "The prompt says it does not know sgsc, scc, sigma or sgpack.",
                        "Install the package that brings the command, as the table shows, then type it again.",
                        "The package manager says it could not resolve mirror://.",
                        "Put the computer on a network whose Mainframe runs the Mirror ([](jsc:mirror)).")
                .seeAlso("jsc:sigma_installing", "jsc:sigma_compile", "jsc:sigma_run")
                .register();
    }
}
