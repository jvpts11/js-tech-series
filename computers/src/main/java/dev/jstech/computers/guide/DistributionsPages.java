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
 * The pages of the Linux Desk Reference's chapter on the three distributions that install from an installer here:
 * what a distribution is, how each installer goes through its pages, the first boot, the two package managers, the
 * Mirror and the programs it offers, and the commands a Linux here does not have.
 */
final class DistributionsPages {

    private DistributionsPages() {
    }

    /** Declares the sections and entries of the Fedora, Debian and Ubuntu chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        choosingADistribution(guide, chapter);
        installingLinux(guide, chapter);
        linuxPackages(guide, chapter);
        linuxDifferences(guide, chapter);
    }

    private static void choosingADistribution(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("choosing_a_distribution").titled("Choosing")
                .in(chapter).register();
        guide.page("distributions", section).titled("What a distribution is, and which to choose")
                .paragraph("A Linux system comes in many versions, and each version is called a distribution. A"
                        + " distribution bundles the Linux kernel, the programs that run on it, its own installer and"
                        + " its own package manager. Here you can install three of them: Ubuntu, Debian and Fedora.")
                .paragraph("Each of the three is a Legacy system: it takes Legacy hardware or newer. All three boot"
                        + " the same kernel, named 6.8-jsc, and they differ in the disk they take, the random-access"
                        + " memory (RAM) they hold while they run, and the package manager they use. The numbers"
                        + " below are the game's own, not the exact requirements of a real machine.")
                .table("The three distributions", "Distribution", "Disk it takes", "RAM while it runs",
                        "Package manager")
                .row(GuideCell.words("Ubuntu"), GuideCell.amount(8192, "MB"), GuideCell.amount(48, "MB"),
                        GuideCell.code("apt"))
                .row(GuideCell.words("Debian"), GuideCell.amount(4096, "MB"), GuideCell.amount(24, "MB"),
                        GuideCell.code("apt"))
                .row(GuideCell.words("Fedora"), GuideCell.amount(8192, "MB"), GuideCell.amount(48, "MB"),
                        GuideCell.code("dnf"))
                .paragraph("The disk figure is the room the system takes for itself, before any program or desktop."
                        + " A disk with less free room than that cannot take the system: see"
                        + " [](jsc:installing_linux_keys).")
                .paragraph("A distribution starts at its text console, the screen with a prompt. A desktop, the"
                        + " graphical screen with windows and menus, is a package you add afterwards. The three"
                        + " desktops are in the table below.")
                .table("Desktops you can add", "Desktop", "Command", "Disk to install", "RAM while it runs")
                .row(GuideCell.words("KDE Plasma"), GuideCell.code("kde-plasma"), GuideCell.amount(256, "MB"),
                        GuideCell.amount(224, "MB"))
                .row(GuideCell.words("GNOME"), GuideCell.code("gnome"), GuideCell.amount(192, "MB"),
                        GuideCell.amount(256, "MB"))
                .row(GuideCell.words("Cinnamon"), GuideCell.code("cinnamon"), GuideCell.amount(160, "MB"),
                        GuideCell.amount(160, "MB"))
                .paragraph("KDE Plasma and GNOME need Legacy hardware or newer. Cinnamon needs Standard hardware or"
                        + " newer, so a Legacy computer cannot take it. The installers offer a desktop only when a"
                        + " Mirror answers, as [](jsc:mirror_and_packages) explains.")
                .paragraph("The table gives the numbers to compare. Debian takes the least disk and holds the least"
                        + " memory, while Ubuntu and Fedora take the most. The package manager is the other difference:"
                        + " apt on Ubuntu and Debian, dnf on Fedora.")
                .define("Distribution", "One Linux system, packaged with its own installer, its own package manager"
                        + " and its own sizes.")
                .define("Package manager", "The command that fetches and installs programs for a distribution, from"
                        + " the Mirror.")
                .define("Mirror", "The package server on the network's Mainframe. Installers and package managers"
                        + " fetch from it.")
                .define("Kernel", "The core of the system, the part that runs the programs and talks to the machine."
                        + " Here it is named 6.8-jsc.")
                .ifSomethingGoesWrong("The installer shows no desktop page.",
                        "No Mirror answers, so the computer is not on a network whose Mainframe runs the Mirror."
                                + " Connect it, then install ([](jsc:mirror_and_packages)).",
                        "Cinnamon is missing from the desktop list.",
                        "The computer is a Legacy machine. Choose KDE Plasma or GNOME, or use a Standard computer.")
                .seeAlso("jsc:installing_linux_keys", "jsc:install_ubuntu", "jsc:apt", "jsc:dnf")
                .register();
    }

    private static void installingLinux(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("installing_linux").titled("Installing").in(chapter)
                .register();
        installUbuntu(guide, section);
        installDebian(guide, section);
        installFedora(guide, section);
        installingLinuxKeys(guide, section);
        firstBoot(guide, section);
    }

    private static void installUbuntu(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("install_ubuntu", section).titled("Installing Ubuntu")
                .paragraph("Ubuntu has its own installer. You start it from its install medium, the disc or stick a"
                        + " system is installed from, after you boot the computer from that drive"
                        + " ([](jsc:firmware)). The installer opens in a window titled \"Ubuntu installer\", with a"
                        + " band across the top that holds the page's heading and the words \"[ Help ]\".")
                .define("Install medium", "The disc or stick a system is installed from. The installer is the program"
                        + " on it.")
                .paragraph("The installer goes through its pages in order, and ENTER goes on from each page. The"
                        + " desktop page appears only when a Mirror answers.")
                .table("The pages of the Ubuntu installer", "Page", "Heading on the page", "What you do")
                .row(GuideCell.words("Disk"), GuideCell.words("Guided storage configuration"),
                        GuideCell.words("Choose the disk with UP and DOWN, then press ENTER."))
                .row(GuideCell.words("Name"), GuideCell.words("Profile setup"),
                        GuideCell.words("Type the computer's name, then press ENTER."))
                .row(GuideCell.words("Desktop"), GuideCell.words("Desktop environment"),
                        GuideCell.words("Choose a desktop or none, then press ENTER."))
                .row(GuideCell.words("Copy"), GuideCell.words("Installing system"),
                        GuideCell.words("Wait while the system is copied. The page shows each step."))
                .row(GuideCell.words("Done"), GuideCell.words("Installation complete"),
                        GuideCell.words("Take the install medium out, then press ENTER to reboot."))
                .paragraph("The foot of the screen shows the buttons \"[ Done ]  [ Back ]\" on the pages before the"
                        + " copy, and \"[ Reboot Now ]\" on the copy and done pages. The installers have no key that"
                        + " goes back one page, so check each page before you press ENTER.")
                .steps("Boot the computer from the install medium.",
                        "Choose the disk on the Disk page, then press ENTER.",
                        "Type the computer's name on the Name page, then press ENTER.",
                        "On the Desktop page, choose a desktop or none, then press ENTER.",
                        "Wait until the copy is finished.",
                        "Take the install medium out, then press ENTER to reboot.")
                .ifSomethingGoesWrong("The screen says \"No Mirror answers, so it comes up at its terminal.\"",
                        "The desktop page is skipped, and the system starts without a desktop. Connect the computer to"
                                + " a network whose Mainframe runs the Mirror, then add a desktop after the first boot"
                                + " ([](jsc:apt)).")
                .seeAlso("jsc:installing_linux_keys", "jsc:first_boot")
                .register();
    }

    private static void installDebian(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("install_debian", section).titled("Installing Debian")
                .paragraph("Debian has its own installer, titled \"Debian installer\". You start it from the install"
                        + " medium, as you do for Ubuntu. It is a grey window with a tab on its top edge that carries"
                        + " the page's heading.")
                .paragraph("The line at the bottom shows the keys to use. Read that line with care: some of the keys"
                        + " it names do nothing here.")
                .paragraph("The installer goes through its pages in order, and ENTER goes on from each page. The"
                        + " software page appears only when a Mirror answers.")
                .table("The pages of the Debian installer", "Page", "Heading on the page", "What you do")
                .row(GuideCell.words("Disk"), GuideCell.words("Partition disks"),
                        GuideCell.words("Choose the disk, then press ENTER."))
                .row(GuideCell.words("Name"), GuideCell.words("Configure the network"),
                        GuideCell.words("Type the computer's name, then press ENTER."))
                .row(GuideCell.words("Desktop"), GuideCell.words("Software selection"),
                        GuideCell.words("Choose a desktop or none, then press ENTER."))
                .row(GuideCell.words("Copy"), GuideCell.words("Installing the base system"),
                        GuideCell.words("Wait for the steps: unpacking, the boot entry and the host name."))
                .row(GuideCell.words("Done"), GuideCell.words("Finish the installation"),
                        GuideCell.words("Take the install medium out, then press ENTER to reboot."))
                .paragraph("The name page has the heading \"Configure the network\", which is the installer's own text."
                        + " The field on that page is the computer's name, and the name becomes the host name.")
                .paragraph("The line at the bottom reads \"<Tab> moves; <Space> selects; <Enter> activates buttons\"."
                        + " On Debian, Tab and Space do nothing. Use UP and DOWN to move, and ENTER to choose.")
                .steps("Boot the computer from the install medium.",
                        "Choose the disk, then press ENTER.",
                        "Type the computer's name, then press ENTER.",
                        "Choose a desktop or none, then press ENTER.",
                        "Wait until the copy is finished.",
                        "Take the install medium out, then press ENTER to reboot.")
                .ifSomethingGoesWrong("The software page is missing.",
                        "No Mirror answers. Connect the computer to a network whose Mainframe runs the Mirror, then add"
                                + " a desktop after the first boot with apt install gnome ([](jsc:apt)).",
                        "The line at the bottom says \"<Tab> moves\", but Tab does nothing.",
                        "Use UP and DOWN to move between the choices, and ENTER to choose.")
                .seeAlso("jsc:installing_linux_keys", "jsc:first_boot")
                .register();
    }

    private static void installFedora(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("install_fedora", section).titled("Installing Fedora")
                .paragraph("Fedora has its own installer, titled \"Fedora installer\". It does not go page by page. It"
                        + " shows a hub: a list of every question, each marked as given or as still wanted. You answer"
                        + " the questions one by one, and return to the hub after each.")
                .paragraph("Open a question with its digit, from 1 to 9, or move the cursor with UP and DOWN and press"
                        + " ENTER. When the hub lists nothing as wanted, press B to begin the installation. The hub's"
                        + " foot says so: \"A letter begins the installation once nothing is still wanted.\"")
                .table("The questions of the Fedora installer", "Page", "Heading on the page", "What you do")
                .row(GuideCell.words("Hub"), GuideCell.words("Installation"),
                        GuideCell.words("Lists every question. Open one, answer it, and come back."))
                .row(GuideCell.words("Disk"), GuideCell.words("Installation Destination"),
                        GuideCell.words("Choose the disk, then press ENTER."))
                .row(GuideCell.words("Desktop"), GuideCell.words("Software selection"),
                        GuideCell.words("Choose a desktop or none. Shown only when a Mirror answers."))
                .row(GuideCell.words("Name"), GuideCell.words("Host name"),
                        GuideCell.words("Type the computer's name, then press ENTER."))
                .row(GuideCell.words("Copy"), GuideCell.words("Progress"),
                        GuideCell.words("Wait for the steps, from the environment to the host name."))
                .row(GuideCell.words("Done"), GuideCell.words("Complete!"),
                        GuideCell.words("Take the install medium out, then press ENTER to reboot."))
                .paragraph("The hint at the foot of the hub reads \"Please make a selection from the above ['b' to"
                        + " begin installation, 'q' to quit, 'r' to refresh]:\". The 'q' and 'r' do nothing here, and"
                        + " the 'c' of the other pages is ENTER. To quit, press F3 before anything is written"
                        + " to a disk.")
                .steps("Boot the computer from the Fedora install medium.",
                        "Press 1 to open the first question.",
                        "Answer it, then press ENTER.",
                        "Open every question the hub still lists as wanted, and answer it.",
                        "Press B to begin the installation.")
                .ifSomethingGoesWrong("The hub still lists a question as wanted.",
                        "Open that question with its digit, answer it, and then press B.",
                        "The hint says 'q' to quit, but pressing q does nothing.",
                        "Press F3 to quit. It works only before anything is written to a disk.")
                .seeAlso("jsc:installing_linux_keys", "jsc:dnf")
                .register();
    }

    private static void installingLinuxKeys(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("installing_linux_keys", section).titled("The installers' keys, disks and desktop choice")
                .paragraph("The three installers share one set of keys. The entries above say which key each step"
                        + " needs; this entry lists them all, with what each one does.")
                .table("Keys in the Linux installers", "Key", "What it does")
                .row(GuideCell.code("UP and DOWN"), GuideCell.words("Move the cursor through the disk list, the desktop"
                        + " list and the Fedora hub."))
                .row(GuideCell.code("ENTER"), GuideCell.words("Goes on from the page. On the done page it reboots. On"
                        + " the Fedora hub it opens the question under the cursor."))
                .row(GuideCell.code("1 to 9"), GuideCell.words("On the Fedora hub, opens the question with that"
                        + " number."))
                .row(GuideCell.code("Letters"), GuideCell.words("On the name page, type the computer's name, up to 256"
                        + " letters."))
                .row(GuideCell.code("BACKSPACE"), GuideCell.words("Deletes the last letter of the name."))
                .row(GuideCell.code("E"), GuideCell.words("On the disk page, asks to erase the chosen disk."))
                .row(GuideCell.code("Y"), GuideCell.words("In the erase prompt, erases the disk. ENTER erases too; any"
                        + " other key cancels."))
                .row(GuideCell.code("B"), GuideCell.words("On the Fedora hub, begins the installation."))
                .row(GuideCell.code("F3"), GuideCell.words("Quits the installation, but only before anything is written"
                        + " to a disk."))
                .row(GuideCell.code("ESC"), GuideCell.words("Closes the monitor the installer is shown on. The"
                        + " installation keeps running."))
                .paragraph("The disk page lists the disks of the machine in four columns: \"Disk\", \"Size\", \"Free\""
                        + " and \"Holds\". A disk can take the system only when its free room covers what the system"
                        + " takes. A disk marked for erasing counts as empty, so its whole size counts.")
                .paragraph("To erase a disk, press E on the disk page. The prompt reads \"Erase Disk N?\", where N"
                        + " is the disk's number. It says every file on the disk will be deleted and that"
                        + " \"This cannot be undone.\" Below it, \"Y = erase\" and \"N = cancel\" show the keys.")
                .warning("Erasing deletes every file on the disk, and that cannot be undone.")
                .define("Erase", "Delete every file on a disk, so that the disk is empty before a system goes on it."
                        + " An erase cannot be undone.")
                .paragraph("The name page says \"This name identifies the computer at the prompt and on the network.\""
                        + " The name becomes the host name and the name shown in the prompt.")
                .paragraph("The desktop page appears only when a Mirror answers. Its list holds the desktops this"
                        + " machine can run: a Legacy computer sees KDE Plasma and GNOME, and a Standard computer also"
                        + " sees Cinnamon. Choosing \"None, the terminal only\" leaves the system at its terminal.")
                .paragraph("The copy page shows the progress as \"<n>% complete\" and \"About <n> seconds left\". When"
                        + " the copy ends, the installer says \"INSTALLATION COMPLETE\" and asks you to \"Take the"
                        + " installation medium out before rebooting, or the machine boots the installer again.\"")
                .ifSomethingGoesWrong("The disk page says \"No disk in this machine has room for it.\"",
                        "Add a bigger disk to the machine, or erase a disk you no longer need with E on the disk page.",
                        "The disk page says \"No room here. Erase this disk, or choose another.\"",
                        "Choose a disk with enough free room, or press E to erase the one you have chosen.")
                .seeAlso("jsc:install_ubuntu", "jsc:install_debian", "jsc:install_fedora")
                .register();
    }

    private static void firstBoot(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("first_boot", section).titled("The first boot")
                .paragraph("When the installer has finished and the computer has rebooted, the installed system starts"
                        + " from its own disk. You see a list of lines as the kernel and the services start, then a"
                        + " login line on the text console, the first screen of the machine.")
                .paragraph("The boot begins with \"Loading Linux 6.8-jsc ...\" and \"Loading initial ramdisk ...\"."
                        + " The lines that begin with \"[  OK  ]\" report that a service started. The system is run by"
                        + " systemd, the service manager that starts the services in order.")
                .define("Systemd", "The service manager of these systems. It starts the services at boot, in order.")
                .table("Lines at the start", "Line", "When it shows")
                .row(GuideCell.code("Loading Linux 6.8-jsc ..."), GuideCell.words("Always, first."))
                .row(GuideCell.code("Linux version 6.8-jsc (x86_64)"), GuideCell.words("Always. On a 32-bit machine it"
                        + " reads i686."))
                .row(GuideCell.code("CPU: <processor model>"),
                        GuideCell.words("Always: the central processing unit (CPU) of the machine."))
                .row(GuideCell.code("Memory: <MB> MB available"), GuideCell.words("Always: the memory of the machine."))
                .row(GuideCell.code("sda: <disk name>"), GuideCell.words("One line for each disk, with the letters sda,"
                        + " sdb and on."))
                .row(GuideCell.code("[  OK  ] Started Journal Service."), GuideCell.words("Always."))
                .row(GuideCell.code("[  OK  ] Reached target Local File Systems."), GuideCell.words("Always."))
                .row(GuideCell.code("[  OK  ] Reached target Network is Online."), GuideCell.words("Only when a data"
                        + " cable joins the computer to a Mainframe."))
                .row(GuideCell.code("[  OK  ] Found package mirror on <Mainframe name>."), GuideCell.words("Only when"
                        + " that Mainframe runs the Mirror."))
                .row(GuideCell.code("Starting kde plasma Display Manager..."), GuideCell.words("Only when a desktop is"
                        + " installed. The desktop is named in lower case: kde plasma, gnome or cinnamon."))
                .paragraph("Without a desktop there is no display manager line. The boot ends at the text console: a"
                        + " banner, a login and a shell, and nothing else.")
                .define("Display manager", "The program that shows the graphical login and starts the desktop. Here it"
                        + " starts only when a desktop is installed.")
                .paragraph("The text console prints a greeting, then waits at its login line. Here is the greeting on"
                        + " Ubuntu. Debian and Fedora print the same lines with their own name.")
                .session("jsc:guide/terminal/bash", "Ubuntu")
                .answer("Ubuntu <hostname> tty1", "", "<hostname> login: player", "Password:",
                        "Welcome to Ubuntu (Linux 6.8-jsc x86_64)", "")
                .paragraph("The login is a line, and nothing checks the password you are asked for, because nothing in"
                        + " this world logs in with a password yet. You are always the user player.")
                .define("Login", "The step where the system asks who is using the computer, before it starts a session"
                        + " for that user.")
                .ifSomethingGoesWrong("The system starts at a shell, with no desktop.",
                        "Install a desktop with apt install gnome on Ubuntu or Debian, or dnf install gnome on Fedora,"
                                + " then reboot. Cinnamon needs Standard hardware.",
                        "The boot shows no \"Found package mirror\" line.",
                        "The computer is not on a network whose Mainframe runs the Mirror. Connect its data cable to"
                                + " that Mainframe ([](jsc:data_cables)).")
                .seeAlso("jsc:apt", "jsc:dnf", "jsc:mirror_and_packages")
                .register();
    }

    private static void linuxPackages(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("linux_packages").titled("Packages").in(chapter)
                .register();
        apt(guide, section);
        dnf(guide, section);
        mirrorAndPackages(guide, section);
    }

    private static void apt(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("apt", section).titled("apt, verb by verb")
                .paragraph("apt is the package manager of Ubuntu and Debian. You type apt, a verb such as install, then"
                        + " the name of what it works on, and it fetches from the Mirror. On Fedora the word is not a"
                        + " command: use dnf ([](jsc:dnf)).")
                .paragraph("Words that start with a dash are skipped, so apt install -y vim installs vim.")
                .table("The apt verbs", "Command", "What it does")
                .row(GuideCell.code("apt install <package>"), GuideCell.words("Looks the package up on the Mirror and"
                        + " installs it."))
                .row(GuideCell.code("apt remove <package>"), GuideCell.words("Removes an installed package. purge and"
                        + " erase do the same."))
                .row(GuideCell.code("apt search [term]"), GuideCell.words("Lists the packages on the Mirror whose"
                        + " name or text matches the term. Installed ones are tagged."))
                .row(GuideCell.code("apt list"), GuideCell.words("Lists the installed packages, with their names and"
                        + " descriptions."))
                .row(GuideCell.code("apt update"), GuideCell.words("Reads the package lists from the Mirror."
                        + " It installs nothing."))
                .row(GuideCell.code("apt upgrade"), GuideCell.words("Brings each installed package up to the"
                        + " version the Mirror offers. It adds no new packages. full-upgrade and dist-upgrade do"
                        + " the same."))
                .command(CommandGuidePages.id(Platform.LINUX, "apt"))
                .subheading("Installing a program")
                .steps("Type apt search and a word from the program's name, then press ENTER.",
                        "Find the command name in the first column of the list.",
                        "Type apt install and the command name, then press ENTER.",
                        "Wait until the progress bar of the install is full.")
                .paragraph("An install prints these lines, in this order, before its progress bar.")
                .table("What apt prints for an install", "Line", "What it means")
                .row(GuideCell.code("Reading package lists... Done"), GuideCell.words("The package lists are read."))
                .row(GuideCell.code("Building dependency tree... Done"),
                        GuideCell.words("The packages the program needs are worked out."))
                .row(GuideCell.code("The following NEW packages will be installed:"),
                        GuideCell.words("The program's name follows on the next line."))
                .row(GuideCell.code("Need to get <MB> MB of archives."), GuideCell.words("The size of the download."))
                .row(GuideCell.code("Get:1 <mirror address> stable/main <package> <version> [<MB> MB]"),
                        GuideCell.words("The download from the Mirror."))
                .paragraph("A remove prints the same first two lines. Then it prints \"The following packages will be"
                        + " REMOVED:\" and \"After this operation, <MB> disk space will be freed.\"")
                .ifSomethingGoesWrong("command not found: apt",
                        "This is Fedora, which uses dnf. Type dnf in its place ([](jsc:dnf)).",
                        "E: could not resolve mirror:// - connect this computer to a network whose Mainframe runs the"
                                + " Mirror service",
                        "The computer is not on a network whose Mainframe runs the Mirror. Connect it, as"
                                + " [](jsc:mirror_and_packages) explains.",
                        "E: unable to locate package <name>",
                        "The name is wrong, or the program is not offered on Linux. Find the right name with"
                                + " apt search.",
                        "E: <name>: unmet requirements (hardware or free disk space)",
                        "The window says how much disk is missing. Free up disk space, or use a bigger disk.",
                        "E: cinnamon needs Standard hardware or later",
                        "The computer is a Legacy machine. Use a Standard computer, or install KDE Plasma or GNOME.",
                        "<name> is already the newest version",
                        "Nothing to do. To bring every installed package up to date, use apt upgrade.",
                        "This computer is still setting up <name>.",
                        "A computer runs one install at a time. Wait for the first one to finish.",
                        "<name> is not installed.",
                        "A remove found nothing to remove. Check the installed list with apt list.")
                .seeAlso("jsc:dnf", "jsc:mirror_and_packages", "jsc:what_is_missing")
                .register();
    }

    private static void dnf(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dnf", section).titled("dnf, verb by verb")
                .paragraph("dnf is the package manager of Fedora. It works the way apt does: you type dnf, a verb, then"
                        + " the name of what it works on. On Ubuntu and Debian the word is not a command, and the shell"
                        + " answers \"command not found: dnf\".")
                .paragraph("The verbs are the same as apt's, with one difference. On dnf, update is the upgrade: it"
                        + " brings installed packages up to date. Upgrade does the same job, so either word works.")
                .table("The dnf verbs", "Command", "What it does")
                .row(GuideCell.code("dnf install <package>"), GuideCell.words("Installs the program from the Mirror."))
                .row(GuideCell.code("dnf remove <package>"),
                        GuideCell.words("Removes it. purge and erase do the same."))
                .row(GuideCell.code("dnf search [term]"), GuideCell.words("Lists the matching packages."))
                .row(GuideCell.code("dnf list"), GuideCell.words("Lists the installed packages."))
                .row(GuideCell.code("dnf update"), GuideCell.words("Brings the installed packages up to date."
                        + " On dnf this is the upgrade."))
                .row(GuideCell.code("dnf upgrade"), GuideCell.words("Brings the installed packages up to date, as dnf"
                        + " update does. full-upgrade and dist-upgrade do the same."))
                .command(CommandGuidePages.id(Platform.LINUX, "dnf"))
                .paragraph("An install on Fedora prints its own lines, in this order.")
                .table("What dnf prints for an install", "Line", "What it means")
                .row(GuideCell.code("Last metadata expiration check: <time> ago."), GuideCell.words("When the package"
                        + " lists were last read."))
                .row(GuideCell.code("Dependencies resolved."), GuideCell.words("The packages the program needs are"
                        + " worked out."))
                .row(GuideCell.code("Installing:  <package>  x86_64  <version>  mirror  <MB> MB"), GuideCell.words("The"
                        + " program, its version and its size."))
                .row(GuideCell.code("Downloading Packages:"), GuideCell.words("The download from the Mirror"
                        + " starts, with a line for its speed and progress."))
                .row(GuideCell.code("Running transaction"), GuideCell.words("The program is written to the computer."))
                .row(GuideCell.code("Installed:"), GuideCell.words("Followed by the name of the installed program."))
                .row(GuideCell.code("Complete!"), GuideCell.words("The install has finished."))
                .paragraph("A remove prints \"Removing:  <package>\" in place of the install lines. Every other refusal"
                        + " is the same as apt's, and begins with \"E: \".")
                .ifSomethingGoesWrong("command not found: dnf",
                        "This is Ubuntu or Debian, which use apt. Type apt in its place ([](jsc:apt)).",
                        "E: could not resolve mirror:// - connect this computer to a network whose Mainframe runs the"
                                + " Mirror service",
                        "The computer is not on a network whose Mainframe runs the Mirror. Connect it, as"
                                + " [](jsc:mirror_and_packages) explains.",
                        "E: unable to locate package <name>",
                        "The name is wrong, or the program is not offered on Linux. Find the right name with"
                                + " dnf search.",
                        "<name> is already the newest version",
                        "Nothing to do. dnf update brings every installed package up to date.")
                .seeAlso("jsc:apt", "jsc:mirror_and_packages")
                .register();
    }

    private static void mirrorAndPackages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mirror_and_packages", section).titled("The Mirror and the packages on it")
                .paragraph("The Mirror is the network's package server. It holds the programs and desktops that apt and"
                        + " dnf fetch. It runs as a service on the network's Mainframe ([](jsc:mainframes)). A Linux"
                        + " computer reaches it through a data cable to that Mainframe. The Guide to Operations has its"
                        + " own page on the Mirror ([](jsc:mirror)).")
                .subheading("Putting the Mirror on the Mainframe")
                .steps("Stand at the Mainframe's console.",
                        "Type mirror install, then press ENTER.",
                        "Read the reply: \"Mirror installed on the Mainframe and serving packages\".")
                .paragraph("Type mirror status at any time to see whether the Mirror runs. If you see \"could not"
                        + " resolve mirror://\", check it first.")
                .table("The Mirror's status line", "Status line", "What it means")
                .row(GuideCell.code("Mirror: not installed"),
                        GuideCell.words("The Mirror is not on this Mainframe yet."))
                .row(GuideCell.code("Mirror: serving"), GuideCell.words("The Mirror runs and answers the"
                        + " computers of the network."))
                .row(GuideCell.code("Mirror: installed (Mainframe off)"), GuideCell.words("The Mirror is installed, but"
                        + " the Mainframe is off."))
                .paragraph("A computer reaches the Mirror when a data cable joins it to the network and the"
                        + " Mainframe is serving. Then the boot prints \"Found package mirror on\" and the"
                        + " Mainframe's name.")
                .subheading("What there is to install")
                .paragraph("The table lists the programs that apt and dnf offer on Linux. For each one it gives"
                        + " the disk it takes, the memory it holds while it runs, and the oldest era that can run"
                        + " it. The name in the first column is the one you type after install. Programs that come"
                        + " with every system, such as the Files program and the settings, are not in the table.")
                .table("Programs the Mirror offers to Linux", "Command", "Program", "Disk and RAM", "Minimum era")
                .row(GuideCell.code("kde-plasma"), GuideCell.words("KDE Plasma desktop"),
                        GuideCell.words("256 MB disk, 224 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("gnome"), GuideCell.words("GNOME desktop"),
                        GuideCell.words("192 MB disk, 256 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("cinnamon"), GuideCell.words("Cinnamon desktop"),
                        GuideCell.words("160 MB disk, 160 MB RAM"), GuideCell.words("Standard"))
                .row(GuideCell.code("vim"), GuideCell.words("Vim editor"),
                        GuideCell.words("8 MB disk, 4 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("emacs"), GuideCell.words("Emacs editor"),
                        GuideCell.words("24 MB disk, 12 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("screenfetch"), GuideCell.words("screenfetch, the system's identity with its logo"),
                        GuideCell.words("4 MB disk, 1 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("sgsc"), GuideCell.words("Sigma# compiler"),
                        GuideCell.words("8 MB disk, 16 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("scc"), GuideCell.words("Sigma compiler"),
                        GuideCell.words("4 MB disk, 2 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("sigma"), GuideCell.words("Sigma runtime, a background service"),
                        GuideCell.words("12 MB disk, 24 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("mirror"), GuideCell.words("Mirror service, Mainframe only"),
                        GuideCell.words("64 MB disk, 32 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("mines"), GuideCell.words("Minesweeper"),
                        GuideCell.words("16 MB disk, 1 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("solitaire"), GuideCell.words("Solitaire"),
                        GuideCell.words("16 MB disk, 1 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("snake"), GuideCell.words("Snake"),
                        GuideCell.words("8 MB disk, 1 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("ark"), GuideCell.words("67ark, the archiver"),
                        GuideCell.words("24 MB disk, 16 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("paint"), GuideCell.words("Paint"),
                        GuideCell.words("48 MB disk, 32 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("soundfoundry"), GuideCell.words("Soundfoundry, for music"),
                        GuideCell.words("4 MB disk, 16 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("exceed"), GuideCell.words("Exceed, the sheet of cells"),
                        GuideCell.words("64 MB disk, 64 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("messenger"), GuideCell.words("Midsoft Messenger"),
                        GuideCell.words("48 MB disk, 32 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("knot"), GuideCell.words("Knot, the repository tool"),
                        GuideCell.words("48 MB disk, 32 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("insights"), GuideCell.words("Storage Insights"),
                        GuideCell.words("64 MB disk, 64 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("planner"), GuideCell.words("Craft Planner"),
                        GuideCell.words("64 MB disk, 64 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("autoeng"), GuideCell.words("Automation Engine, a service"),
                        GuideCell.words("32 MB disk, 64 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("msgsvc"), GuideCell.words("Messenger Service, a service"),
                        GuideCell.words("48 MB disk, 8 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("iqlengine"), GuideCell.words("IQL engine, Mainframe only"),
                        GuideCell.words("32 MB disk, 24 MB RAM"), GuideCell.words("Vintage"))
                .row(GuideCell.code("nextgreiql"), GuideCell.words("NextgreIQL engine, Mainframe only"),
                        GuideCell.words("32 MB disk, 48 MB RAM"), GuideCell.words("Legacy"))
                .row(GuideCell.code("youriql"), GuideCell.words("Prophet YourIQL engine, Mainframe only"),
                        GuideCell.words("32 MB disk, 32 MB RAM"), GuideCell.words("Legacy"))
                .paragraph("The numbers are the game's own, not exact requirements. A program installs only on a"
                        + " computer that has the disk and the hardware for it. The install time is the program's size"
                        + " divided by 8 MB a second, multiplied by the era's factor: 1 on Vintage, 2 on Legacy, 3 on"
                        + " Transition, 4 on Standard, 8 on Advanced, 16 on Exa and 32 on Singularity. The time stays"
                        + " between 3 and 90 seconds.")
                .paragraph("Removing a program takes a fifth of the time that installing takes. Programs that players"
                        + " publish to the Mirror install the same way. A player's program written in Sigma# needs the"
                        + " Sigma# runtime first: apt install sigma. Publishing is described in"
                        + " [](jsc:sigma_publish_mirror).")
                .ifSomethingGoesWrong("E: could not resolve mirror:// - connect this computer to a network whose"
                        + " Mainframe runs the Mirror service",
                        "Connect the computer's data cable to the network's Mainframe. If the Mirror is not installed"
                                + " yet, type mirror install at the Mainframe's console.",
                        "the Mirror is full (64 packages)",
                        "The Mirror holds at most 64 packages that players publish, so one must come off before another"
                                + " can go on ([](jsc:sigma_unpublish)).",
                        "this computer cannot store installed programs",
                        "This computer has no store for installed programs. Install the program on a computer"
                                + " with a disk.")
                .seeAlso("jsc:apt", "jsc:dnf", "jsc:mirror", "jsc:mainframes")
                .register();
    }

    private static void linuxDifferences(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("linux_differences")
                .titled("What is different here").in(chapter).register();
        guide.page("what_is_missing", section).titled("What a Linux here does not have, and why")
                .paragraph("Many commands you may know from other Linux systems are not in the shell here. The"
                        + " table lists the common ones and what to do instead. The shell still has reboot, which"
                        + " restarts the computer.")
                .paragraph("Nothing in this world logs in with a password yet, so a step that set one would be setting"
                        + " nothing. Every command runs as the user player, and there is no other user to switch to.")
                .table("Commands you may expect", "Command", "What to do here")
                .row(GuideCell.code("sudo"), GuideCell.words("Not needed: install with apt install or dnf install as"
                        + " player."))
                .row(GuideCell.code("su, passwd, useradd, usermod, adduser"),
                        GuideCell.words("There are no other users. Every command runs as player."))
                .row(GuideCell.code("chmod, chown"), GuideCell.words("The shell keeps no file permissions to change."))
                .row(GuideCell.code("systemctl"), GuideCell.words("No service command. A service starts once"
                        + " its program is installed."))
                .row(GuideCell.code("journalctl"), GuideCell.words("Not in the shell."))
                .row(GuideCell.code("ip, ifconfig"), GuideCell.words("Not in the shell. The shell chapter"
                        + " ([](jsc:shell)) lists the commands that do exist."))
                .row(GuideCell.code("shutdown"),
                        GuideCell.words("Not in the shell. Use reboot to restart the computer."))
                .paragraph("The package commands need no sudo. apt install and dnf install run directly for player"
                        + " ([](jsc:apt), [](jsc:dnf)).")
                .paragraph("When a command is missing, the shell says \"command not found\" with the word you"
                        + " typed, and adds a dim line: \"type 'help' to list commands\".")
                .ifSomethingGoesWrong("command not found: sudo",
                        "Not needed here. Install with apt install or dnf install instead.",
                        "command not found: systemctl",
                        "There is no service command. A service is installed, and the Mirror is put on the Mainframe"
                                + " with mirror install.",
                        "command not found: chmod",
                        "There are no file permissions to change in this world.")
                .seeAlso("jsc:apt", "jsc:dnf")
                .register();
    }
}
