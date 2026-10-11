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
 * The FreeBSD chapter of The Daemon Handbook: installing the system with its own installer, its boot, its services,
 * its two ways to a program (pkg and the ports tree), its devices and its help without the help command.
 */
final class FreeBsdPages {

    private FreeBsdPages() {
    }

    /** Declares the sections and entries of FreeBSD, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        freebsdSystem(guide, chapter);
        freebsdSoftware(guide, chapter);
        freebsdTools(guide, chapter);
    }

    private static void freebsdSystem(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("freebsd_system").titled("The system").in(chapter)
                .register();
        freebsdOverview(guide, section);
        bsdinstall(guide, section);
        loader(guide, section);
        rcServices(guide, section);
    }

    private static void freebsdSoftware(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("freebsd_software").titled("Software").in(chapter)
                .register();
        pkg(guide, section);
        portsTree(guide, section);
        portsBuild(guide, section);
    }

    private static void freebsdTools(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("freebsd_tools").titled("Tools").in(chapter).register();
        freebsdEditors(guide, section);
        deviceListing(guide, section);
        freebsdHelp(guide, section);
    }

    private static void freebsdOverview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("freebsd_overview", section).titled("FreeBSD on this computer")
                .paragraph("FreeBSD is a UNIX-like system, and the Daemon Handbook is about it and about UNIX System V."
                        + " On your computer it works the way the other systems do: you install it from a medium,"
                        + " you start it, and you work at its prompt. It starts at a text"
                        + " prompt, and a desktop is an extra package you may add later.")
                .define("Terminal system", "A system that starts at a text prompt. A desktop is an optional package.")
                .paragraph("FreeBSD needs Legacy hardware or newer, the first hardware era it runs on. A computer of"
                        + " the Vintage era cannot install it. The installed system takes 2,048 MB of disk space, and"
                        + " 16 MB of memory while it runs.")
                .define("Hardware era", "The generation of a computer's hardware, from Vintage to Singularity."
                        + " Legacy is the first one FreeBSD runs on.")
                .table("FreeBSD at a glance")
                .property("Minimum hardware era", "Legacy")
                .amount("Disk space of the installed system", 2048, "MB")
                .amount("Memory while it runs", 16, "MB")
                .property("Kernel", "FreeBSD 14.1-RELEASE GENERIC")
                .property("Shell", "sh")
                .property("Package manager", "pkg")
                .property("Console", "ttyv0")
                .subheading("What you see when it starts")
                .paragraph("When a prompt opens, FreeBSD greets you with the lines below. The first line names the"
                        + " architecture: IA-32, the 32-bit Intel architecture, on a 32-bit computer, and vel64 on a"
                        + " 64-bit one.")
                .session("jsc:guide/terminal/sh", "FreeBSD")
                .answer("FreeBSD/vel64 (<hostname>) (ttyv0)", "", "login: <your name>",
                        "FreeBSD 14.1-RELEASE (GENERIC)", "", "Welcome to FreeBSD, <your name>.",
                        "  Find a command by what it does:", "    apropos <word>",
                        "  Learn the system:", "    man intro",
                        "  See every command here:", "    Tab, twice", "",
                        "Tip: pkg info lists every package installed here.")
                .paragraph("The greeting names three ways to find your way. The last section of this chapter explains"
                        + " each one ([](jsc:freebsd_help)).")
                .subheading("Desktops on FreeBSD")
                .paragraph("You add a desktop later, or choose one during the installation. A desktop is offered only"
                        + " when the network's Mirror answers and your computer's hardware is new enough for it.")
                .define("Mirror", "The package server of the network, run by a Mainframe that runs the Mirror service.")
                .table("Desktops on FreeBSD", "Desktop", "Command", "Needs at least")
                .row(GuideCell.words("KDE Plasma"), GuideCell.code("kde-plasma"),
                        GuideCell.words("Legacy era, 256 MB of disk, 224 MB of memory"))
                .row(GuideCell.words("GNOME"), GuideCell.code("gnome"),
                        GuideCell.words("Legacy era, 192 MB of disk, 256 MB of memory"))
                .row(GuideCell.words("CDE"), GuideCell.code("cde"),
                        GuideCell.words("Legacy era, 32 MB of disk, 16 MB of memory"))
                .row(GuideCell.words("Cinnamon"), GuideCell.code("cinnamon"),
                        GuideCell.words("Standard era, 160 MB of disk, 160 MB of memory"))
                .paragraph("The desktops of every system are described in [Desktops](jsc:desktops).")
                .ifSomethingGoesWrong("The installer says \"FreeBSD needs Legacy era hardware or newer; this machine"
                                + " is Vintage era.\"",
                        "Install FreeBSD on a Legacy computer, or on a newer one.",
                        "The installer says \"No disk is installed to put FreeBSD on.\"",
                        "Put a disk in one of the computer's slots.",
                        "The installer says \"No disk in this machine has room for it.\"",
                        "Use a bigger disk, or erase a disk on the Partitioning page ([](jsc:bsdinstall)).")
                .seeAlso("jsc:the_systems", "jsc:installing", "jsc:bsdinstall")
                .register();
    }

    private static void bsdinstall(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("bsdinstall", section).titled("Installing FreeBSD, page by page")
                .paragraph("bsdinstall is FreeBSD's own installer. It asks its questions on pages, always in the same"
                        + " order, and then it copies the system onto the disk you choose. This page goes through"
                        + " each page, so you know what it asks before you answer.")
                .define("bsdinstall", "FreeBSD's own installer, which asks its questions on pages in one fixed order.")
                .paragraph("You need a computer with a disk and an install medium: the FreeBSD CD (compact"
                        + " disc), put in a CD Drive. A Mirror on the network is optional. With one, the installer"
                        + " also offers desktops and fetches packages from it.")
                .define("Ports Collection", "The ports tree, the folder of recipes that builds programs from source.")
                .subheading("The pages in order")
                .table("The pages of bsdinstall", "Page", "What you do")
                .row(GuideCell.words("Welcome"), GuideCell.words("Press Install to begin."))
                .row(GuideCell.words("Set Hostname"), GuideCell.words("Keep the name the field shows, or type"
                        + " another one."))
                .row(GuideCell.words("Distribution Select"), GuideCell.words("Tick ports to install the Ports"
                        + " Collection. It is ticked by default."))
                .row(GuideCell.words("Partitioning"), GuideCell.words("Choose the disk and how it is used."))
                .row(GuideCell.words("Mirror Selection"), GuideCell.words("Choose the Mirror to fetch from, or none"
                        + " to install from the CD only."))
                .row(GuideCell.words("Archive Extraction"), GuideCell.words("Wait while the system is copied."))
                .row(GuideCell.words("System Configuration"), GuideCell.words("Tick the services to start at boot."
                        + " cron is ticked by default, and sshd is not."))
                .row(GuideCell.words("Desktop"), GuideCell.words("Only when a Mirror answers: choose a desktop, or"
                        + " None."))
                .row(GuideCell.words("Complete"), GuideCell.words("Remove the CD and reboot."))
                .paragraph("The Welcome page has the button Install, and every other page has OK. bsdinstall has no"
                        + " Back button: each page offers only Cancel and its own button, so to change an earlier"
                        + " answer, press Cancel and start the installer again.")
                .subheading("Choosing the disk")
                .paragraph("The Partitioning page lists one row per disk. The disks are named ada0, ada1 and so on,"
                        + " in the order of the drives. Each row offers a way to use that disk. Auto (UFS) puts the"
                        + " UNIX File System (UFS) on the whole disk.")
                .table("The choices for each disk", "Choice", "When you see it")
                .row(GuideCell.words("Auto (UFS): use the whole of the disk"), GuideCell.words("The disk is empty."))
                .row(GuideCell.words("Auto (UFS): use the whole of the disk, erasing what is on it"),
                        GuideCell.words("The disk holds a system or files. Its files are erased."))
                .row(GuideCell.words("Beside: share the disk with its system"), GuideCell.words("The disk holds a"
                        + " system and has room left beside it."))
                .define("UFS", "The disk filesystem that bsdinstall lays down on the whole disk.")
                .paragraph("Before the installer erases a disk, it asks \"Erase ada0?\" and names what it erases. The"
                        + " dialog says \"This cannot be undone.\" Press Y to erase, or N to cancel.")
                .warning("Erasing a disk deletes every file on it, and this cannot be undone.")
                .subheading("The services and the Mirror")
                .paragraph("The Mirror Selection page asks where pkg and ports fetch from. The row mirror://mainframe"
                        + " appears only when a Mirror answers ([](jsc:mirror)). Otherwise you install from the CD.")
                .paragraph("When the copy is done, your answers become settings. Ticking cron turns cron on, and"
                        + " ticking sshd turns remote on. Both are the settings you can change later"
                        + " ([](jsc:rc_services)). With the ports box ticked and a Mirror in use, the whole ports"
                        + " tree is laid onto the disk now. Without a Mirror, the installer leaves an"
                        + " empty /usr/ports folder for portsnap to fill.")
                .paragraph("At the end, the computer restarts into the new system once you take the CD out.")
                .define("Distribution set", "A compressed bundle of system files, such as base.txz or kernel.txz.")
                .ifSomethingGoesWrong("The Desktop page has no desktop in its list.",
                        "No Mirror answered, or this computer's era is older than the desktop's minimum. Connect to a"
                                + " network with a Mirror, or use a newer computer.",
                        "The Mirror Selection page has no mirror:// row.",
                        "No Mainframe on this network runs the Mirror service. A Mainframe has to run it"
                                + " ([](jsc:mirror)).",
                        "You want to change an answer, and there is no Back button.",
                        "Press Cancel, then start the installer again.")
                .seeAlso("jsc:disks", "jsc:installing", "jsc:rc_services", "jsc:mirror")
                .register();
    }

    private static void loader(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("loader", section).titled("The loader and the boot lines")
                .paragraph("After the firmware, the computer runs the loader. The loader is the program at the start of"
                        + " the disk that loads the kernel, which is the core of the system. It is the boot manager of"
                        + " FreeBSD, and it can show a short menu for five seconds before it starts the system.")
                .define("Loader", "The program at the start of the disk that loads the kernel.")
                .define("Kernel", "The core of the system, and the first thing the boot lines name.")
                .define("Boot lines", "The text the system prints on its way up, when it starts, and on its way down,"
                        + " when it stops.")
                .paragraph("The loader does not choose between systems. It starts FreeBSD from the system disk and"
                        + " lists no other system. If you want another system on the same computer, use the"
                        + " firmware's boot order or its Boot Menu ([](jsc:firmware), [](jsc:boot_menu)).")
                .subheading("The loader's menu")
                .table("The entries of the loader's menu", "Entry", "What it does")
                .row(GuideCell.words("Boot"), GuideCell.words("Starts the running system. It is the default: Enter"
                        + " starts it."))
                .row(GuideCell.words("Reboot"), GuideCell.words("Starts the machine over."))
                .row(GuideCell.words("Firmware settings"), GuideCell.words("Opens the firmware's settings, shown"
                        + " only on Standard and later hardware, which uses UEFI (the Unified Extensible Firmware"
                        + " Interface)."))
                .paragraph("The menu counts down from 5 seconds. Its top line says \"Welcome to FreeBSD\", and the line"
                        + " under it says \"Autoboot in 5 seconds. [Space] to pause\".")
                .paragraph("Any key stops the count, although the line names the space bar. Once the count stops, press"
                        + " [[Enter]] to start the default entry, or a number from 1 to 9 to start that entry at once."
                        + " Keys beyond the entries do nothing.")
                .paragraph("The server setting boot.show_boot_menu turns the menu on or off. It is on by default. When"
                        + " it is off, the computer starts the system at once.")
                .subheading("The boot lines on the way up")
                .paragraph("On its way up, FreeBSD prints a fixed set of lines. Each line appears only when its"
                        + " condition holds, as the last column says.")
                .table("The boot lines on the way up", "Line", "When it appears")
                .row(GuideCell.code("Loading kernel..., then Booting..."), GuideCell.words("At the start."))
                .row(GuideCell.code("FreeBSD 14.1-RELEASE GENERIC <architecture>"), GuideCell.words("Always."))
                .row(GuideCell.code("CPU: <processor name>"), GuideCell.words("Always: the processor of the computer."))
                .row(GuideCell.code("real memory  = <n> MB"), GuideCell.words("Always: the memory the computer has."))
                .row(GuideCell.code("ada0: <<disk name>>"),
                        GuideCell.words("One line for each disk, then ada1 and so on."))
                .row(GuideCell.code("Trying to mount root from ufs:/dev/ada0p2 [rw]..."),
                        GuideCell.words("Always: the system's own disk."))
                .row(GuideCell.code("Setting hostname: <hostname>."), GuideCell.words("Always."))
                .row(GuideCell.code("Starting Network: lo0 em0."), GuideCell.words("Only when a data cable reaches the"
                        + " computer."))
                .row(GuideCell.code("pkg: repository Mirror found on <mirror host>."),
                        GuideCell.words("Only when a Mirror answers."))
                .row(GuideCell.code("Starting devd."), GuideCell.words("Always."))
                .row(GuideCell.code("Starting syslogd."), GuideCell.words("Always."))
                .row(GuideCell.code("Starting cron."), GuideCell.words("Only when cron is on."))
                .row(GuideCell.code("Starting sshd."), GuideCell.words("Only when remote is allowed."))
                .row(GuideCell.code("Starting <desktop> display manager."),
                        GuideCell.words("Only when a desktop is installed."))
                .subheading("The boot lines on the way down")
                .paragraph("When you stop or restart the computer, FreeBSD prints its way down. It stops each service"
                        + " that is running, and it syncs the disks. The last line says \"Rebooting...\" when the"
                        + " computer restarts, and \"The operating system has halted.\" when it stops.")
                .subheading("When the system's file is missing")
                .paragraph("The loader starts the file boot/kernel, in the folder boot on the system disk. Without that"
                        + " file, FreeBSD cannot start. To repair it, install over it from a medium.")
                .ifSomethingGoesWrong("The computer waits at the loader's menu, and nothing starts.",
                        "Press Enter to start Boot, or wait 5 seconds for it to start on its own.",
                        "The computer starts a system other than FreeBSD.",
                        "The loader starts only FreeBSD. Change the firmware's boot order, or use its Boot Menu"
                                + " ([](jsc:firmware)).",
                        "The loader's menu does not appear.",
                        "The server setting boot.show_boot_menu is off. Turn it on to show the menu again.",
                        "The system's file boot/kernel is missing from the system disk.",
                        "Boot an installation medium and install over it to repair.")
                .seeAlso("jsc:firmware", "jsc:boot_menu", "jsc:bsdinstall")
                .register();
    }

    private static void rcServices(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("rc_services", section).titled("Services at boot: cron and sshd")
                .paragraph("A service is a program that runs in the background, without a window of its own. FreeBSD"
                        + " asks about two of them, at the installer and at each boot: cron, which runs scheduled jobs,"
                        + " and sshd, which lets other computers reach this one.")
                .define("Service", "A program that runs in the background, such as cron for scheduled jobs or sshd for"
                        + " remote login.")
                .define("rc", "The startup scripts of FreeBSD. They read the settings to decide which services start.")
                .table("The two services", "Service", "Setting", "What it does")
                .row(GuideCell.code("cron"), GuideCell.code("cron"), GuideCell.words("Runs scheduled jobs. On by"
                        + " default."))
                .row(GuideCell.code("sshd"), GuideCell.code("remote"), GuideCell.words("Lets other computers reach"
                        + " this one."))
                .paragraph("The sshd service and the remote setting are one setting. Turning remote on starts sshd at"
                        + " boot, and the sshd box of the installer sets the same setting. The box is not ticked by"
                        + " default, so a computer installed by bsdinstall starts with remote off unless you tick it.")
                .paragraph("A computer that was not installed by bsdinstall starts with remote on.")
                .subheading("Changing the services with config")
                .paragraph("You change both settings with the config command. The values are on and off, and true and"
                        + " false also work.")
                .steps("Type config to list the settings.", "Type config cron on to turn cron on.",
                        "Type config cron off to turn cron off.",
                        "Type config remote on to let other computers reach this one.",
                        "Type config remote off to refuse the remote calls of other computers.")
                .paragraph("A successful change answers with the name of the setting and the word set, such as"
                        + " cron set. Typing config alone lists both settings, one line for each.")
                .table("The lines of config", "Line", "What it means")
                .row(GuideCell.code("remote  on (other computers may run programs here)"),
                        GuideCell.words("The remote setting is on."))
                .row(GuideCell.code("cron  on"), GuideCell.words("The cron setting, shown as on or as off."))
                .paragraph("Without a value, config cron answers"
                        + " \"usage: config <key> <value>  (or 'config' to list)\"."
                        + " A value that is not on or off is refused with unknown setting: cron. The key is known, so"
                        + " the value is the part that is wrong.")
                .subheading("The rc.conf file")
                .paragraph("The rc scripts read /etc/rc.conf at boot. On FreeBSD that file is generated from your"
                        + " settings, not kept on the disk, so change the settings with config. It holds these lines:")
                .listing("rc.conf", "hostname=\"<hostname>\"", "ifconfig_em0=\"DHCP\"", "cron_enable=\"YES\"",
                        "sshd_enable=\"NO\"", "dumpdev=\"AUTO\"")
                .paragraph("The ifconfig_em0 line appears only when the computer is networked. The cron and sshd lines"
                        + " read YES or NO from the two settings.")
                .subheading("What a service change does")
                .paragraph("With cron off, scheduled jobs wait instead of running. A job you put in the background with"
                        + " & keeps running. With remote off, the remote calls of other computers to this one are"
                        + " refused.")
                .ifSomethingGoesWrong("sshd did not start after the installation.",
                        "The sshd box was left unticked. Type config remote on.",
                        "Scheduled jobs do not run.",
                        "The cron setting is off. Type config cron on.",
                        "Other computers cannot run programs here.",
                        "The remote setting is off. Type config remote on.",
                        "The computer answers unknown setting: cron.",
                        "The value was not on or off. Use config cron on or config cron off.")
                .seeAlso("jsc:processes_and_jobs", "jsc:system_commands", "jsc:bsdinstall")
                .register();
    }

    private static void pkg(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("pkg", section).titled("pkg, verb by verb")
                .paragraph("pkg is the package manager of FreeBSD. A package is a program built by someone else and"
                        + " fetched ready to install, so you do not build it yourself. The packages come from the"
                        + " network's Mirror, and you type pkg, a verb, and the name it works on.")
                .define("Community package", "A package another player published to the Mirror.")
                .define("Outdated", "An installed package whose version differs from the one the Mirror serves now.")
                .table("The verbs of pkg", "Command", "What it does")
                .row(GuideCell.code("pkg install <package>"), GuideCell.words("Fetches a package from the Mirror and"
                        + " installs it."))
                .row(GuideCell.code("pkg delete <package>"), GuideCell.words("Removes an installed package."))
                .row(GuideCell.code("pkg search [term]"), GuideCell.words("Lists the packages that match the term, one"
                        + " row each."))
                .row(GuideCell.code("pkg info"), GuideCell.words("Lists every installed package, with its"
                        + " description."))
                .row(GuideCell.code("pkg update"), GuideCell.words("Refreshes the Mirror's list of packages"
                        + " without changing any installed package."))
                .row(GuideCell.code("pkg upgrade"), GuideCell.words("Installs newer versions of the packages you have"
                        + " installed."))
                .paragraph("Three shorter words also work: add means install, remove means delete, and query means"
                        + " info.")
                .paragraph("pkg search marks a package [installed] when it is on your computer, and [community] when"
                        + " another player published it. pkg upgrade installs the newer version of each Outdated"
                        + " package.")
                .command(CommandGuidePages.id(Platform.FREEBSD, "pkg"))
                .subheading("Installing a package")
                .steps("Type pkg install and the name of the package.",
                        "Read the lines that name the packages and the download size.",
                        "Wait while the package is set up. The setup runs as a job you can watch.")
                .listing("pkg install", "Updating Mirror repository catalogue...",
                        "Mirror repository is up to date.",
                        "The following 1 package(s) will be affected (of 0 checked):",
                        "New packages to be INSTALLED:", "        <name>: <version>",
                        "Number of packages to be installed: 1", "<n> MiB to be downloaded.",
                        "[1/1] Fetching <name>-<version>.pkg from mirror://mainframe")
                .paragraph("A delete lists the packages it removes, then says how much space it frees, such as"
                        + " \"The operation will free <n> MiB.\"")
                .subheading("Messages of pkg")
                .table("Messages of pkg", "Message", "What it means")
                .row(GuideCell.code("pkg: could not resolve mirror://"), GuideCell.words("This computer is not on a"
                        + " network whose Mainframe runs the Mirror service."))
                .row(GuideCell.code("pkg: unable to locate package <name>"), GuideCell.words("The Mirror does not offer"
                        + " a package with that name."))
                .row(GuideCell.code("pkg: unmet requirements (hardware or free disk space)"),
                        GuideCell.words("The computer lacks the hardware or the free disk space the package needs."))
                .row(GuideCell.code("pkg: <name> is not installed."), GuideCell.words("You tried to delete a package"
                        + " that is not installed."))
                .row(GuideCell.code("pkg: <name> comes with every system."), GuideCell.words("The program is already"
                        + " there, so there is nothing to install."))
                .row(GuideCell.code("pkg: <name> is already installed."), GuideCell.words("The package is on the"
                        + " computer already."))
                .row(GuideCell.code("pkg: <name> needs <era> hardware or later"),
                        GuideCell.words("The computer is of an older era than the package needs."))
                .row(GuideCell.code("pkg: <name> only installs on the Mainframe"),
                        GuideCell.words("The package runs only on that kind of computer: the Mainframe, a server in a"
                        + " rack, a Cluster Management Computer or a Personal Computer."))
                .row(GuideCell.code("pkg: this computer cannot store installed programs"),
                        GuideCell.words("The computer has no disk to keep programs on."))
                .row(GuideCell.code("no packages installed"), GuideCell.words("pkg info has nothing to list."))
                .row(GuideCell.code("no packages match '<term>'"),
                        GuideCell.words("pkg search found no package with that word."))
                .ifSomethingGoesWrong("pkg says it could not resolve mirror://.",
                        "Connect this computer to a network whose Mainframe runs the Mirror service.",
                        "pkg says it cannot locate the package by its name.",
                        "Type pkg search and a word to find the real name, then use that name.",
                        "pkg says the requirements are unmet.",
                        "Free some disk space, or use a computer with better hardware.",
                        "pkg says the package is not installed when you delete it.",
                        "Type pkg info to find the exact name.",
                        "pkg says the program comes with every system.",
                        "Nothing to do: it is already on the computer.")
                .seeAlso("jsc:programs", "jsc:software_commands", "jsc:ports_tree")
                .register();
    }

    private static void portsTree(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("ports_tree", section).titled("The ports tree: portsnap")
                .paragraph("FreeBSD also builds programs from source, on your own computer. The recipes for them sit in"
                        + " the ports tree, a folder on the system disk. You fetch the tree with portsnap, and the"
                        + " Mirror serves it as one copy.")
                .define("Ports tree", "The folder of recipes, one per program, that builds programs on this computer.")
                .define("Port", "The recipe for one program in the ports tree.")
                .define("Snapshot", "The copy of the ports tree that the Mirror serves, fetched as one unit.")
                .paragraph("The tree is real files. Each port has a folder at /usr/ports/<category>/<name>/ with three"
                        + " files. Makefile holds the recipe, distinfo holds the checksum and size of its source, and"
                        + " pkg-descr holds the one-line description.")
                .subheading("The words of portsnap")
                .table("The words of portsnap", "Word", "What it does")
                .row(GuideCell.code("fetch"), GuideCell.words("Brings the snapshot from the Mirror without laying out"
                        + " a tree."))
                .row(GuideCell.code("extract"), GuideCell.words("Lays out the tree afresh from the snapshot, replacing"
                        + " any tree there."))
                .row(GuideCell.code("update"), GuideCell.words("Brings a tree that is laid out already up to date."))
                .row(GuideCell.code("auto"), GuideCell.words("Fetches, then extracts or updates, whichever the"
                        + " computer needs."))
                .command(CommandGuidePages.id(Platform.FREEBSD, "portsnap"))
                .steps("The first time, type portsnap fetch extract.", "Afterwards, type portsnap fetch update.")
                .paragraph("The words combine in the order you type them. While it runs, portsnap prints its progress,"
                        + " and its last line gives the number of ports and the disk they take.")
                .warning("portsnap extract replaces the whole ports tree, and the build folders inside it go too.")
                .table("Where some programs sit in the tree", "Program", "Category")
                .row(GuideCell.words("screenfetch"), GuideCell.code("sysutils"))
                .row(GuideCell.words("emacs"), GuideCell.code("editors"))
                .row(GuideCell.words("minesweeper, solitaire, snake"), GuideCell.code("games"))
                .row(GuideCell.words("scc, sgsc, sigma"), GuideCell.code("lang"))
                .row(GuideCell.words("knot, knothub"), GuideCell.code("devel"))
                .row(GuideCell.words("kde_plasma"), GuideCell.code("x11"))
                .row(GuideCell.words("gnome, cinnamon"), GuideCell.code("x11"))
                .row(GuideCell.words("cde"), GuideCell.code("x11"))
                .subheading("Messages of portsnap")
                .table("Messages of portsnap", "Message", "What it means")
                .row(GuideCell.code("portsnap: this computer has no system disk"),
                        GuideCell.words("The computer has no disk to hold the tree."))
                .row(GuideCell.code("Looking up the Mirror for the ports tree... none found."),
                        GuideCell.words("fetch found no Mirror, so no snapshot comes; it then prints No mirrors"
                                + " remaining, giving up."))
                .row(GuideCell.code("No snapshot available.  Try running portsnap fetch"),
                        GuideCell.words("extract and update need a snapshot first."))
                .row(GuideCell.code("/usr/ports was not created by portsnap."),
                        GuideCell.words("update works only on a tree that portsnap laid out,"
                                + " so run portsnap extract once."))
                .row(GuideCell.code("portsnap: No space left on device"), GuideCell.words("The disk is full."))
                .ifSomethingGoesWrong("portsnap says none found for the Mirror.",
                        "Connect the network to a Mainframe that runs the Mirror service.",
                        "portsnap says there is no snapshot available.",
                        "Type portsnap fetch first, then extract or update.",
                        "portsnap says /usr/ports was not created by portsnap.",
                        "Type portsnap extract once. Afterwards, update works.",
                        "portsnap says there is no space left on the device.",
                        "Free some disk space.")
                .seeAlso("jsc:ports_build", "jsc:pkg", "jsc:disks")
                .register();
    }

    private static void portsBuild(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("ports_build", section).titled("Building ports: make, and the 10% lighter build")
                .paragraph("You build a port in its own folder, with the make command, which reads the port's recipe."
                        + " The build compiles the program's source on this computer. It takes a while, and the result"
                        + " is made for this computer.")
                .define("Build", "Compiling the program's source on this computer.")
                .define("Distfile", "The source archive that a build fetches from the Mirror.")
                .paragraph("The usage line of make is: usage: make [build] [install | reinstall | deinstall] [clean].")
                .steps("Type cd and the port's folder, such as /usr/ports/sysutils/screenfetch.",
                        "Type make install clean to build, install and clean the port.",
                        "Type make deinstall to take the program off the computer again.")
                .command(CommandGuidePages.id(Platform.FREEBSD, "make"))
                .table("The targets of make", "Target", "What it does")
                .row(GuideCell.code("build"), GuideCell.words("Builds the port on this computer, which is"
                        + " what make does when you give no target."))
                .row(GuideCell.code("install"),
                        GuideCell.words("Builds the port if it is not built, then installs it."))
                .row(GuideCell.code("reinstall"),
                        GuideCell.words("Builds and installs the port over the one installed."))
                .row(GuideCell.code("deinstall"), GuideCell.words("Takes the installed port off the computer; with"
                        + " clean, it cleans first."))
                .row(GuideCell.code("clean"), GuideCell.words("Deletes the work folder that the build left."))
                .paragraph("You combine targets in one line, such as make install clean. A build takes between 5"
                        + " seconds and 30 minutes. It runs one job per processor core, and a faster processor shortens"
                        + " it.")
                .subheading("The 10% lighter build")
                .paragraph("A program you build on this computer needs 10% less than the package of the same program."
                        + " When a build ends, it prints Built for this machine:, then a line such as 10% less memory,"
                        + " disk and processor than the package.")
                .table("What a build here saves", "What", "Saved by a build here")
                .row(GuideCell.words("Processor speed to run"), GuideCell.words("10% less"))
                .row(GuideCell.words("Disk space to install"), GuideCell.words("10% less"))
                .row(GuideCell.words("Memory while it runs"), GuideCell.words("10% less"))
                .row(GuideCell.words("Video memory"), GuideCell.words("Not reduced"))
                .subheading("Messages of make")
                .table("Messages of make", "Message", "What it means")
                .row(GuideCell.code("make: no target to make."), GuideCell.words("The folder is not a port's folder."
                        + " make then stops in that folder."))
                .row(GuideCell.code("make: don't know how to make <target>. Stop"),
                        GuideCell.words("make does not know that target."))
                .row(GuideCell.code("===>  <pkg> is already installed"), GuideCell.words("The port is on the computer"
                        + " already; the message suggests make deinstall and make install, or make reinstall."))
                .row(GuideCell.code("===>  <pkg> is marked as broken: <reason>."), GuideCell.words("A gate of the"
                        + " install blocks the port, such as its era or the kind of computer."))
                .row(GuideCell.code("===>  <pkg> cannot install: a Mainframe's services come prebuilt; use pkg install"
                        + " <name>."),
                        GuideCell.words("The port is a Mainframe service, so install it with pkg instead."))
                .row(GuideCell.code("===>  <pkg> cannot install: this computer cannot store installed programs."),
                        GuideCell.words("The computer has no disk to keep programs on."))
                .row(GuideCell.code("===>  <pkg> cannot install: this machine is short of the processor or the free"
                        + " disk space it needs."), GuideCell.words("The computer is too small for the port."))
                .row(GuideCell.code("fetch: mirror://mainframe/distfiles/<distfile>: No address record"),
                        GuideCell.words("The build needs its source, and no Mirror answers."))
                .ifSomethingGoesWrong("make says there is no target to make.",
                        "Go into the port's folder first, such as /usr/ports/sysutils/screenfetch.",
                        "make says the program is already installed.",
                        "Type make reinstall, or make deinstall and then make install.",
                        "fetch says there is no address record for the Mirror.",
                        "The Mirror cannot be reached. Connect the network to a Mirror, then try again.",
                        "make says the computer is short of the processor or the free disk space.",
                        "Use a computer with a faster processor, or free some disk space.")
                .seeAlso("jsc:ports_tree", "jsc:pkg", "jsc:processors")
                .register();
    }

    private static void freebsdEditors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("freebsd_editors", section).titled("Editors on FreeBSD")
                .paragraph("FreeBSD comes with two editors for text files: ee, the small one, and vi, the"
                        + " editor of UNIX."
                        + " Both are already on the system, so you never install them.")
                .table("The two editors", "Editor", "What it is on FreeBSD")
                .row(GuideCell.code("ee <file>"), GuideCell.words("FreeBSD's own editor. It shows its shortcuts on the"
                        + " top rows of the screen."))
                .row(GuideCell.code("vi <file>"), GuideCell.words("The vi of FreeBSD is nvi. It has no colour and no"
                        + " ruler."))
                .paragraph("Each editor has its own pages with its keys and its steps: [ee](jsc:ee) and [vi](jsc:vi),"
                        + " and both are in [Editors at the prompt](jsc:editors_overview). This page only says what is"
                        + " particular to FreeBSD.")
                .subheading("The messages of each editor")
                .paragraph("ee shows \"<name>\" [New file] when the file you named is new. After a save, it shows"
                        + " \"<name>\" with the number of lines and the number of characters.")
                .paragraph("vi shows <name>: new file: line 1 for a new file, and <name>: unmodified: line 1 for a file"
                        + " that exists. After a write, it shows the number of lines and characters.")
                .paragraph("To leave ee, press [[Esc]] to open its menu, then press [[Enter]] to choose leave"
                        + " editor. The screen shows the same tip: ESC-Enter: exit. In vi, press [[Esc]] to return"
                        + " to normal mode, then type :wq to write and quit.")
                .ifSomethingGoesWrong("ee says Unknown command at its command prompt.",
                        "The command prompt takes write, quit and exit. Type one of them.",
                        "vi says E37: no write since last change (add ! to override).",
                        "Type :w to write first, or :q! to leave without writing.",
                        "vi says E492: not an editor command.",
                        "Type a command from the vi page ([](jsc:vi)).",
                        "Your typing does not appear as text in vi.",
                        "Press i to start inserting text, and Esc to stop inserting.")
                .seeAlso("jsc:ee", "jsc:vi", "jsc:editors_overview")
                .register();
    }

    private static void deviceListing(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("device_listing", section).titled("pciconf and usbconfig: what is on the board")
                .paragraph("The board of your computer has two buses that carry its devices: the Peripheral Component"
                        + " Interconnect (PCI) bus, and the Universal Serial Bus (USB). The PCI bus holds the built-in"
                        + " parts and the expansion cards. The USB bus holds what you plug in. pciconf lists the PCI"
                        + " devices, and usbconfig lists the USB devices. Both commands are explained with the other"
                        + " hardware lists ([](jsc:devices_commands)); this page shows what FreeBSD prints.")
                .define("Driver name", "The name FreeBSD gives a device. The number counts the devices of that kind.")
                .define("ugen", "The name FreeBSD gives a USB device, such as ugen0.2.")
                .paragraph("Type pciconf -lv with both letters together, to list the devices on the PCI bus with their"
                        + " details. Type usbconfig with no words, to list the USB devices.")
                .table("The line of pciconf -l", "Part", "What it tells you")
                .row(GuideCell.code("<driver name>@pci0:<bus>:<device>:<function>:"),
                        GuideCell.words("The device's driver name, then its slot as bus, device and function."))
                .row(GuideCell.code("class=0x<six hex digits>"), GuideCell.words("The kind of device, as a code."))
                .row(GuideCell.code("device   = '<name>'"), GuideCell.words("With -lv: the device's name."))
                .row(GuideCell.code("class    = <class name>"),
                        GuideCell.words("With -lv: the kind of device in words."))
                .subheading("The devices of the board")
                .table("The devices pciconf lists", "Device", "Driver name")
                .row(GuideCell.words("Host bridge, in slot 00:00.0"), GuideCell.code("hostb0"))
                .row(GuideCell.words("Serial and parallel ports, or a USB controller, in slot 00:14.0"),
                        GuideCell.words("uart0 for serial and parallel, ehci0 for USB 2, xhci0 for USB 3"))
                .row(GuideCell.words("Audio: the board's own audio, in slot 00:1b.0"),
                        GuideCell.code("hdac0, hdac1"))
                .row(GuideCell.words("Network: the first one, in slot 00:19.0"), GuideCell.code("em0, em1"))
                .row(GuideCell.words("Video"), GuideCell.code("vgapci0"))
                .row(GuideCell.words("Processing cards"), GuideCell.code("none<n>"))
                .subheading("usbconfig")
                .paragraph("usbconfig takes no options. Its first line names the controller, USB 2 controller or"
                        + " USB 3 controller, with its number of ports.")
                .paragraph("Each following line is a device, numbered as ugen0.2, ugen0.3 and so on, in the order"
                        + " the ports run. A hub is listed before the devices on it.")
                .paragraph("A device the computer disabled shows cfg=255. Such a device is not in use.")
                .ifSomethingGoesWrong("pciconf says usage: pciconf -l [-v] after you typed -l -v.",
                        "Type the two letters together: pciconf -lv.",
                        "usbconfig says invalid option.",
                        "usbconfig takes no options. Type usbconfig alone.",
                        "A device shows cfg=255.",
                        "The computer disabled the device, so it is not in use.",
                        "pciconf says no hardware to read.",
                        "Run the command on the computer whose parts you want to list.")
                .seeAlso("jsc:devices_commands", "jsc:system_commands")
                .register();
    }

    private static void freebsdHelp(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("freebsd_help", section).titled("Finding your way without help")
                .paragraph("FreeBSD has no help command. Typing help gets a line that points to two other ways. You use"
                        + " those ways, and the Tab key, to find any command.")
                .define("Candidate", "A command name that Tab could fill in.")
                .paragraph("At the prompt, type help and press [[Enter]]. FreeBSD answers with one line:")
                .session("jsc:guide/terminal/sh", "FreeBSD")
                .answer("help: not found. Try: apropos <word>, or man intro")
                .subheading("apropos, man and whatis")
                .table("The helpers of FreeBSD", "Command", "What it does")
                .row(GuideCell.code("apropos <word>"),
                        GuideCell.words("Searches the name and the summary of every command here, and every topic,"
                        + " for the word."))
                .row(GuideCell.code("man <command>"),
                        GuideCell.words("Opens the manual page of a command. A manual entry such as graphics-cards"
                        + " opens the same way."))
                .row(GuideCell.code("man intro"), GuideCell.words("Opens the one topic FreeBSD has: the introduction to"
                        + " the commands."))
                .row(GuideCell.code("whatis <command>"), GuideCell.words("Shows the one line that a manual page opens"
                        + " with."))
                .steps("Type apropos and a word for what you want to do, such as apropos network.",
                        "Read the lines it prints. Each line is a command name and its one line.",
                        "Type man and the command name to read its manual page.")
                .paragraph("The introduction is short, so read it first. It says that a command is a small program that"
                        + " does one thing, and that a shell runs the one you type at its prompt.")
                .subheading("Tab")
                .paragraph("Press [[Tab]] once to fill in a command name, when only one name fits. Press it twice in a"
                        + " row, with nothing left to fill, to list every candidate. The candidates show on the line"
                        + " above the prompt, with two spaces between them.")
                .table("Messages of the helpers", "Message", "What it means")
                .row(GuideCell.code("help: not found. Try: apropos <word>, or man intro"),
                        GuideCell.words("FreeBSD has no help command. Use one of the two ways it names."))
                .row(GuideCell.code("<text>: nothing appropriate."), GuideCell.words("No command or topic matches the"
                        + " word you searched."))
                .row(GuideCell.code("No manual entry for <word>"), GuideCell.words("There is no manual page with that"
                        + " name. Check the spelling."))
                .ifSomethingGoesWrong("The computer says help: not found.",
                        "Type apropos and a word for what you want to do, or type man intro.",
                        "apropos says nothing appropriate.",
                        "Try a shorter word, or a different one.",
                        "man says No manual entry for graphics.",
                        "Type the whole name, such as man graphics-cards.")
                .seeAlso("jsc:help_on_a_computer", "jsc:finding_help_overview", "jsc:man_pages",
                        "jsc:whatis_and_apropos", "jsc:pipes_and_variables")
                .register();
    }
}
