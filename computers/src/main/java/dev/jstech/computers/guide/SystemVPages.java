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
 * The pages of UNIX System V: what the system is and how its installer works, the installpkg command for programs
 * on media, the PACE menus, CDE on this system, and the System V details of vi, lp and man.
 */
final class SystemVPages {

    private SystemVPages() {
    }

    /** Declares the sections and entries of UNIX System V, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        systemVStart(guide, chapter);
        systemVSoftware(guide, chapter);
        systemVMenus(guide, chapter);
        systemVDesktop(guide, chapter);
        systemVTools(guide, chapter);
    }

    private static void systemVStart(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("system_v_start").titled("The system")
                .in(chapter).register();
        systemVOverview(guide, section);
        systemVInstaller(guide, section);
    }

    private static void systemVOverview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("system_v_overview", section).titled("What UNIX System V is")
                .paragraph("UNIX System V is the oldest of the UNIX systems you meet at a prompt, and the one that a"
                        + " computer of the first era runs. Its release is 3.2. You work in it the way you work in"
                        + " any UNIX system: you type a command, the system answers, and your files sit in folders.")
                .paragraph("The system was built for a small machine. It takes 10 megabytes (MB) of disk, and it keeps"
                        + " 2 megabytes (MB) of memory for itself while it runs, yet it runs several programs at"
                        + " once. It takes its programs from media and from nowhere else: there is no"
                        + " Mirror to install from, only media ([](jsc:programs)).")
                .table("UNIX System V at a glance")
                .property("Release", "3.2")
                .property("Hardware era", "Vintage, the first era")
                .amount("Space on the disk", 10, "MB")
                .amount("Memory kept for the system while it runs", 2, "MB")
                .property("Install medium", "Floppy disk, read by a Floppy Drive")
                .property("Shell", "sh")
                .property("Package manager", "None: programs come from media")
                .property("Software house", "Bellwether Labs")
                .property("Desktop", "CDE (Common Desktop Environment), installed as a program")
                .define("Install medium", "An item that carries the files of a system or a program. It goes into"
                        + " a drive linked to the computer, and the system reads it from there.")
                .paragraph("Starting the system prints a few lines in order, and the last one says the system is"
                        + " ready.")
                .table("The lines at start", "Line", "What it tells you")
                .row(GuideCell.code("UNIX System V Release 3.2"),
                        GuideCell.words("The system's name and its release."))
                .row(GuideCell.code("Copyright (c) 1984, 1986, 1987 Bellwether Labs"),
                        GuideCell.words("The software house's notice."))
                .row(GuideCell.code("All Rights Reserved"), GuideCell.words("The end of the notice."))
                .row(GuideCell.code("Total real memory     = <n>"),
                        GuideCell.words("The memory the computer has."))
                .row(GuideCell.code("Available memory      = <n>"),
                        GuideCell.words("The memory the system can use, after the kernel keeps its share."))
                .row(GuideCell.code("The system is coming up.  Please wait."),
                        GuideCell.words("The system is starting."))
                .row(GuideCell.code("/dev/root: clean"),
                        GuideCell.words("The disk the system starts from passed its check."))
                .row(GuideCell.code("The system is ready."),
                        GuideCell.words("The prompt is ready for your first command."))
                .paragraph("When you shut the system down, it prints a warning first: \"THE SYSTEM IS BEING SHUT"
                        + " DOWN NOW ! ! !\", then \"Log off now or risk your files being damaged.\"")
                .paragraph("The last line says \"The system is down.\", or \"The system is being restarted.\""
                        + " when the system comes straight back up.")
                .subheading("Folders")
                .paragraph("Your home folder is /usr/player, and the people of the system live under /usr. The"
                        + " installation makes bin, etc, mnt and tmp under the root, and bin, lib and player inside"
                        + " usr. Inside your home it makes Desktop and Documents.")
                .paragraph("Programs go to /usr/bin. When a drive holds a medium, its files appear under /mnt with"
                        + " the letter of the drive, such as /mnt/d.")
                .subheading("Commands")
                .paragraph("Most commands of the shell work here, and [the shell](jsc:shell_prompt) explains them."
                        + " Two commands exist only on this system: installpkg, which installs a program from a"
                        + " medium, and pace, the menu system. Both have their own entries ([](jsc:installpkg),"
                        + " [](jsc:pace)).")
                .define("PACE", "The menu system of UNIX System V, with its menus in frames over one another.")
                .table("Commands that this system lacks", "Command", "It belongs to")
                .row(GuideCell.code("ee"), GuideCell.words("FreeBSD"))
                .row(GuideCell.code("info"), GuideCell.words("Linux"))
                .row(GuideCell.code("lspci, lsusb"), GuideCell.words("Linux"))
                .row(GuideCell.code("pciconf, usbconfig"), GuideCell.words("FreeBSD"))
                .ifSomethingGoesWrong(
                        "You type a command you know from another system, and the prompt does not know it.",
                        "Check the table above: some commands belong to other systems. Use the command this system"
                                + " has.",
                        "The system starts at a prompt, and you expect a desktop.",
                        "A desktop is a program you install. On a Legacy computer or a newer one, install CDE"
                                + " ([](jsc:cde_on_system_v)), then restart the computer. The desktop shows at the"
                                + " next boot.")
                .seeAlso("jsc:installing", "jsc:the_systems", "jsc:shell_prompt")
                .register();
    }

    private static void systemVInstaller(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("system_v_installer", section).titled("The installer")
                .paragraph("You install UNIX System V from its floppy, with the System V installer. The installer"
                        + " asks two things, the disk and the name of the computer, then copies the system onto the"
                        + " disk. It runs in a window with one banner in reverse video the whole way through.")
                .paragraph("You need a computer of the Vintage era or newer, with a Floppy Drive linked to it, and a"
                        + " disk with at least 10 MB of free space. When you finish, the disk holds UNIX System V,"
                        + " and the computer starts it after the reboot.")
                .steps("Put the System V floppy in a Floppy Drive linked to the computer, and start the computer"
                        + " from it ([](jsc:installing)).",
                        "On the settings page, press [[Up]] or [[Down]] to choose the disk.",
                        "Type the name of the computer in the Computer name box.",
                        "Press [[Enter]] to go on. If the disk already holds a system, press [[1]] or [[2]] first.",
                        "Wait for the copy. Closing the monitor does not stop it.",
                        "On the done page, remove the floppy and press [[Enter]] to reboot.")
                .table("The three pages", "Page", "What it shows")
                .row(GuideCell.words("Settings"),
                        GuideCell.words("The question of the disk, the disk list and the Computer name box."))
                .row(GuideCell.words("Copy"),
                        GuideCell.words("Four steps, each marked copying, waiting or with a green check: Base"
                                + " system, Utilities, On-line help and Manual pages."))
                .row(GuideCell.words("Done"),
                        GuideCell.words("The name, the space used and, when you installed beside a system, the"
                                + " boot order."))
                .paragraph("The settings page asks \"Choose the disk to install the UNIX System V on:\" and shows"
                        + " each disk on a line. A line starts with the slot number and the label of the disk, and"
                        + " then gives its size, its free space and what it holds, or \"Nothing\".")
                .define("Disk slot", "The place in the computer where a disk sits. The installer shows the place as"
                        + " a number.")
                .paragraph("A disk that already holds a system asks you first. The question reads \"<disk label>"
                        + " holds <what it holds>. What should happen to it?\"")
                .table("Your choice on a disk that holds a system", "Choice", "What happens")
                .row(GuideCell.code("Erase <disk> and install the UNIX System V"),
                        GuideCell.words("The disk is wiped, and the new system goes on it alone."))
                .row(GuideCell.code("Install the UNIX System V beside <what it holds>"),
                        GuideCell.words("The new system goes on the same disk, next to the old one. This choice is"
                                + " dimmed when the disk has no room for it."))
                .paragraph("While the copy runs, the copy page names the disk you chose. Open the monitor again to"
                        + " see its progress.")
                .paragraph("The done page says \"The UNIX System V is installed on <disk>.\" and then gives the name"
                        + " of the computer and the space used on the disk.")
                .ifSomethingGoesWrong(
                        "The settings page says \"No room here. Erase this disk, or choose another.\"",
                        "The disk has too little free space for the system. Choose another disk from the list, with"
                                + " at least 10 MB free.",
                        "The settings page says \"No disk in this machine has room for it.\"",
                        "Put in a disk with at least 10 MB free, then choose it on the settings page.")
                .seeAlso("jsc:installpkg", "jsc:firmware", "jsc:two_systems")
                .register();
    }

    private static void systemVSoftware(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("system_v_software").titled("Software")
                .in(chapter).register();
        installpkg(guide, section);
    }

    private static void installpkg(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("installpkg", section).titled("Installing a package from the medium")
                .paragraph("Programs on UNIX System V come on their own medium, and you install them with installpkg."
                        + " There is no package manager and no Mirror. The program is on the medium, and the medium"
                        + " must be in a drive linked to the computer ([](jsc:drives)).")
                .paragraph("With nothing said, installpkg takes the program on the medium in the drive, which is how"
                        + " the real system worked: the medium is the question and the answer. With more than one"
                        + " medium in, it lists the programs and asks you to say which one, by name.")
                .steps("Put the program's medium in a drive linked to the computer.",
                        "Type installpkg and press Enter.",
                        "If the system lists several packages, type installpkg and the name of the one you want.",
                        "Wait while the program is set up, then type its command name to run it.")
                .table("What the screen says", "What you see", "What it means")
                .row(GuideCell.code("installpkg: no package medium in a drive"),
                        GuideCell.words("No medium is in a linked drive. Put the program's medium in a drive."))
                .row(GuideCell.code("installpkg: <name>: no such package on the media in the drives"),
                        GuideCell.words("The name is on none of the media in the drives."))
                .row(GuideCell.code("The following packages are on the media in the drives:"),
                        GuideCell.words("More than one medium is in. Type installpkg and the name you want."))
                .row(GuideCell.code("Installing the <name> package."),
                        GuideCell.words("The install has started. The setup line names the program and the medium."))
                .row(GuideCell.code("This computer is still setting up <name>."),
                        GuideCell.words("An earlier setup is still running. Wait for it to finish."))
                .row(GuideCell.code("<name> is already installed."),
                        GuideCell.words("The program is on this computer already."))
                .row(GuideCell.code("<name> needs <era> hardware or newer. This computer is <era>."),
                        GuideCell.words("The computer is older than the program. Use a newer computer."))
                .row(GuideCell.code("<name> runs on <list>, not on <system>."),
                        GuideCell.words("The program is written for another system."))
                .row(GuideCell.code("<name> needs <n> MB free on the disk. There are <m> MB."),
                        GuideCell.words("The disk has too little free space for the program."))
                .row(GuideCell.code("<name> cannot install on this computer's system or hardware."),
                        GuideCell.words("The program does not fit this system or this hardware."))
                .paragraph("The setup line names the medium: the floppy, the compact disc (CD), the digital versatile"
                        + " disc (DVD), the USB drive (Universal Serial Bus) or the Blu-ray disc.")
                .paragraph("A program written for a newer era than your computer is refused, with the line about its"
                        + " hardware. A program written for the Vintage era, on a floppy, has no era rule.")
                .paragraph("Installed programs go to /usr/bin. While a program's medium is in a drive, its files"
                        + " appear under /mnt with the letter of the drive.")
                .command(CommandGuidePages.id(Platform.UNIX, "installpkg"))
                .ifSomethingGoesWrong(
                        "The screen says \"installpkg: no package medium in a drive\".",
                        "Insert the medium the package came on, then try again.",
                        "The screen says \"installpkg: <name>: no such package on the media in the drives\".",
                        "Type installpkg alone to see the list of packages, then type the name exactly as the list"
                                + " shows it.",
                        "The screen says \"This computer is still setting up <name>.\"",
                        "Wait until the first setup finishes, then type the command again.",
                        "The screen says \"<name> needs <era> hardware or newer.\"",
                        "Use a computer of that era or a newer one. The line names the era the program needs.")
                .seeAlso("jsc:programs", "jsc:drives", "jsc:installing")
                .register();
    }

    private static void systemVMenus(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("system_v_menus").titled("Menus")
                .in(chapter).register();
        pace(guide, section);
    }

    private static void pace(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("pace", section).titled("PACE, the menus in frames")
                .paragraph("PACE is the menu system of UNIX System V, and only that system has it. It opens a frame of"
                        + " menus over the screen, and each choice you make opens a frame of its own over the last"
                        + " one. The name stands for the Panelled Access Command Environment of Bellwether Labs'"
                        + " UNIX System V.")
                .define("Frame", "One window of menus in PACE. A new frame opens over the last one, and the keys move"
                        + " between them.")
                .paragraph("Type pace to start it. It takes the whole terminal, and its first frame is the main one,"
                        + " titled PACE. Choose Exit PACE, or press F5 on the last frame, and the terminal comes back.")
                .table("The main frame", "Choice", "What it opens")
                .row(GuideCell.code("Office of player"),
                        GuideCell.words("The Office: the Filecabinet, your home, and the other folders."))
                .row(GuideCell.words("Programs"), GuideCell.words("The programs that are installed. ENTER runs one."))
                .row(GuideCell.words("System Administration"),
                        GuideCell.words("Machine Information, File Systems and Software Installation."))
                .row(GuideCell.words("UNIX System"),
                        GuideCell.words("A shell of its own. Type exit at it to come back to PACE."))
                .row(GuideCell.words("Exit PACE"), GuideCell.words("Leaves PACE and gives the terminal back."))
                .paragraph("Inside a frame, these keys work. The labels on the bottom line of the screen name the"
                        + " function keys.")
                .table("The keys in PACE", "Key", "What it does")
                .row(GuideCell.code("Up, Down, Home, End, Page Up, Page Down"),
                        GuideCell.words("Move in the frame."))
                .row(GuideCell.code("Enter or F2"), GuideCell.words("Opens the item."))
                .row(GuideCell.code("F1"), GuideCell.words("HELP: opens a help frame."))
                .row(GuideCell.code("F3"), GuideCell.words("PREV-FRM: goes to the frame before this one."))
                .row(GuideCell.code("F4"), GuideCell.words("NEXT-FRM: goes to the frame after this one."))
                .row(GuideCell.code("F5 or Escape"),
                        GuideCell.words("CANCEL: closes the frame on top. On the last frame it leaves PACE."))
                .row(GuideCell.code("F6"), GuideCell.words("CMD-MENU: opens the command menu."))
                .row(GuideCell.code("A letter"),
                        GuideCell.words("Moves to the next item that starts with that letter."))
                .paragraph("A mouse click brings a frame to the front and picks its item. A second click on the same"
                        + " item opens it.")
                .subheading("The Office and its folders")
                .paragraph("The Filecabinet opens your home folder, /usr/player. Other Users lists the folders under"
                        + " /usr, except bin, lib and player, and says \"(no other users)\" when there are none."
                        + " Preferences is greyed out, and it says \"Nothing to set on this machine.\"")
                .subheading("Opening a file from PACE")
                .steps("Type pace and press Enter.",
                        "Choose Office of player and press Enter.",
                        "Choose Filecabinet and press Enter.",
                        "Move to a file with the arrow keys, and press Enter. A standard file opens in vi.")
                .subheading("Reading a folder")
                .paragraph("A folder frame is titled with its name and its path, and it lists what is in the folder."
                        + " The vi editor is explained in [](jsc:vi). A Sigma listing is a program in the Sigma"
                        + " language, explained in [](jsc:sigma_two_languages).")
                .table("What ENTER does on each kind of entry", "Detail", "What ENTER does")
                .row(GuideCell.words("Directory"), GuideCell.words("Opens the folder."))
                .row(GuideCell.words("Sigma listing, a file ending in .sg"),
                        GuideCell.words("Opens the file in vi. F6 runs it."))
                .row(GuideCell.words("Sigma# listing, a file ending in .sgs"),
                        GuideCell.words("Opens the file in vi. It does not run."))
                .row(GuideCell.words("Executable, a file in /usr/bin or /bin"),
                        GuideCell.words("Runs the program."))
                .row(GuideCell.words("Standard file"), GuideCell.words("Opens the file in vi."))
                .paragraph("Press F6 to open the command menu, except on a Sigma listing, where F6 runs the file."
                        + " The menu's items are cancel, exit, help, next-frm, prev-frm, refresh, run and"
                        + " unix-system. Refresh reads the folder again.")
                .paragraph("Run opens a command line. Type a UNIX command, press Enter, and read its output. Escape or"
                        + " F5 cancels the line.")
                .paragraph("A command run from PACE shows its output, then says \"Press ENTER to return to PACE.\""
                        + " The UNIX System item has no such line: type exit in its shell to come back.")
                .subheading("System Administration")
                .table("What the items run", "Item", "Command it runs")
                .row(GuideCell.words("Machine Information"), GuideCell.code("uname -a"))
                .row(GuideCell.words("File Systems"), GuideCell.code("df"))
                .row(GuideCell.words("Software Installation"), GuideCell.code("installpkg"))
                .command(CommandGuidePages.id(Platform.UNIX, "pace"))
                .ifSomethingGoesWrong(
                        "Preferences is greyed out.",
                        "Nothing can be set on this system. The frame says \"Nothing to set on this machine.\"",
                        "The Other Users frame says \"(no other users)\".",
                        "Your home folder is /usr/player, and PACE does not list bin, lib or player under /usr, so no"
                                + " other user shows up.",
                        "ENTER on a Sigma file opens it in vi, and it does not run.",
                        "Press F6 on a Sigma listing, a file ending in .sg, to run it.")
                .seeAlso("jsc:installpkg", "jsc:vi", "jsc:cde_on_system_v")
                .register();
    }

    private static void systemVDesktop(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("system_v_desktop").titled("The desktop")
                .in(chapter).register();
        cdeOnSystemV(guide, section);
    }

    private static void cdeOnSystemV(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_on_system_v", section).titled("CDE on System V")
                .paragraph("CDE, the Common Desktop Environment, is the only desktop UNIX System V has. It is a desktop"
                        + " with windows and a panel, and it needs a Legacy computer or a newer one. A Vintage"
                        + " computer runs the system at a prompt only.")
                .paragraph("CDE is a program, so you install it from its own medium, a CD. Once it is installed, the"
                        + " computer starts into it after a restart. The rest of CDE, its panel, its menus and its"
                        + " tools, is explained in [](jsc:cde).")
                .table("CDE on System V at a glance")
                .property("Hardware era needed", "Legacy or newer")
                .amount("Space on the disk", 32, "MB")
                .amount("Memory while it runs", 16, "MB")
                .property("Install medium", "CD, read by a CD Drive")
                .property("Installed with", "installpkg cde")
                .property("Systems that run it", "UNIX System V, FreeBSD and Linux")
                .steps("Put the CDE CD in a CD Drive linked to the computer.",
                        "Type installpkg cde and press Enter.",
                        "Wait for the setup to finish. Its line names the program and the CD.",
                        "Restart the computer. The desktop starts at the next boot.")
                .paragraph("The desktop has a raised panel at the bottom centre of the screen, with four workspaces"
                        + " named One, Two, Three and Four. Its programs include a file manager, which the panel"
                        + " calls Files, a text editor, a terminal, a calculator, a performance meter, the Style"
                        + " Manager and the Help Viewer.")
                .ifSomethingGoesWrong(
                        "The installation says \"CDE needs Legacy hardware or newer. This computer is Vintage.\"",
                        "Use a Legacy computer or a newer one. A Vintage computer runs System V at a prompt only.",
                        "The installation says \"CDE needs 32 MB free on the disk. There are <n> MB.\"",
                        "Free up disk space, or install on a bigger disk.",
                        "The installpkg line says \"no package medium in a drive\".",
                        "Put the CDE CD in a CD Drive linked to the computer, and try again.",
                        "The computer starts at a prompt after CDE is installed.",
                        "The desktop shows only after a restart. Restart the computer.")
                .seeAlso("jsc:cde", "jsc:desktops", "jsc:installpkg")
                .register();
    }

    private static void systemVTools(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("system_v_tools").titled("Tools")
                .in(chapter).register();
        systemVToolsNotes(guide, section);
    }

    private static void systemVToolsNotes(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("system_v_tools_notes", section).titled("vi, lp and man on System V")
                .paragraph("Three commands of this group are explained in full in other chapters, because the UNIX"
                        + " systems share them. This page says only what UNIX System V does on its own.")
                .subheading("vi")
                .paragraph("vi comes with the system, so it is always there, and you never install it. It uses the"
                        + " same engine as [vim](jsc:vim), with the messages of System V. The screen has no colour"
                        + " and no ruler in the corner. Its keys and colon commands are in [](jsc:vi).")
                .table("Messages when a file opens or is written", "Message", "What it means")
                .row(GuideCell.code("\"<name>\" [New file]"),
                        GuideCell.words("The file does not exist yet. Writing it makes the file."))
                .row(GuideCell.code("\"<name>\" <lines> lines, <characters> characters"),
                        GuideCell.words("The file exists. The counts are its size."))
                .subheading("lp")
                .paragraph("lp sends a file to a printer linked to the computer ([](jsc:peripheral_cables)). Its"
                        + " options and messages are on the printing page, [](jsc:printing). Unless you name a"
                        + " printer with -d, lp uses the default one.")
                .subheading("man")
                .paragraph("man shows the manual page of a command. On this system it also reads the wider manuals:"
                        + " man graphics-cards opens the manual's entry on graphics cards, as a page of section 7."
                        + " The command is explained in [](jsc:man_pages), and whatis and apropos are in"
                        + " [](jsc:whatis_and_apropos).")
                .ifSomethingGoesWrong(
                        "vi says \"E37: no write since last change (add ! to override)\".",
                        "Write the file with :w first, or leave with :q! to throw the changes away.",
                        "lp says \"lp: no default destination\".",
                        "Link a printer to the computer. The first printer linked becomes the default.",
                        "man says \"No manual entry for intro\".",
                        "The intro topic belongs to FreeBSD. On UNIX System V, type man and the name of a command.",
                        "man says \"What manual page do you want?\"",
                        "Give the name of a command after man, for example man graphics-cards.")
                .seeAlso("jsc:vi", "jsc:printing", "jsc:man_pages")
                .register();
    }
}
