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
 * The pages of the shared UNIX chapter on desktops: which desktops a Linux, FreeBSD or UNIX system can show, how each
 * one is installed, its look and its program names, and CDE part by part. Every page is true for each system its
 * desktop runs on, and says which system it is about where that differs.
 */
final class UnixDesktopsPages {

    private UnixDesktopsPages() {
    }

    /** Declares the sections and entries of the desktops chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        desktopEnvironments(guide, chapter);
        cdeDesktop(guide, chapter);
    }

    private static void desktopEnvironments(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("desktop_environments").titled("Desktops")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        desktops(guide, section);
        installADesktop(guide, section);
        kdePlasma(guide, section);
        gnome(guide, section);
        cinnamon(guide, section);
        desktopProgramNames(guide, section);
    }

    private static void cdeDesktop(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("cde_desktop").titled("CDE")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        cde(guide, section);
        cdeFrontPanel(guide, section);
        cdeStyleManager(guide, section);
        cdeApplicationManager(guide, section);
        cdeWorkstationInfo(guide, section);
        cdeTrashCan(guide, section);
    }

    private static void desktops(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("desktops", section).titled("Desktops").icon(ComputingModule.MONITOR)
                .paragraph("A desktop is what a system shows when it starts with a graphical face: its windows, a"
                        + " panel or taskbar, and a menu that lists its programs. Without one, the system stops at"
                        + " its terminal, a prompt where you type commands. Each Frames edition brings its own"
                        + " desktop. Linux and FreeBSD do not, so you install one as a package. On UNIX, the one"
                        + " desktop is the Common Desktop Environment (CDE).")
                .define("Package", "A piece of software that the system's package manager installs, updates and"
                        + " removes, by its name.")
                .define("Desktop environment", "The graphical shell a system starts into: its windows, its panel"
                        + " and the menu that lists the programs it shows.")
                .paragraph("Four desktops are built in. Each has its own look, and its own names for the"
                        + " programs. The eras each one needs are explained at [](jsc:eras).")
                .table("The built-in desktops", "Desktop", "Runs on", "From the era", "Disk to install",
                        "Memory while it runs")
                .row(GuideCell.words("KDE Plasma"), GuideCell.words("Linux, FreeBSD"),
                        GuideCell.words("Legacy"), GuideCell.amount(256, "MB"), GuideCell.amount(224, "MB"))
                .row(GuideCell.words("GNOME"), GuideCell.words("Linux, FreeBSD"),
                        GuideCell.words("Legacy"), GuideCell.amount(192, "MB"), GuideCell.amount(256, "MB"))
                .row(GuideCell.words("Cinnamon"), GuideCell.words("Linux, FreeBSD"),
                        GuideCell.words("Standard"), GuideCell.amount(160, "MB"), GuideCell.amount(160, "MB"))
                .row(GuideCell.words("CDE"), GuideCell.words("UNIX, FreeBSD, Linux"),
                        GuideCell.words("Legacy"), GuideCell.amount(32, "MB"), GuideCell.amount(16, "MB"))
                .paragraph("The disk and memory figures are the mod's own estimates of what each desktop takes. A"
                        + " Vintage computer has no graphical desktop at all: it lives at its terminal.")
                .paragraph("Each desktop wears the look of its era. On Legacy hardware KDE shows KDE 2 and GNOME"
                        + " shows GNOME 1. On Transition they show KDE 4 and GNOME 2. From the Standard on, KDE"
                        + " Plasma and GNOME have their own look. Cinnamon and CDE have one look each, on every"
                        + " era they run on.")
                .paragraph("A desktop shows as launchers only the programs it bundles, and only those your computer"
                        + " may run. KDE Plasma, GNOME and Cinnamon bundle the same ten: Network, This PC, Settings,"
                        + " Files, the Editor, the Command Prompt, the System Monitor, the Calculator, the Network"
                        + " Manager and Help. The Network Manager shows only on a Mainframe ([](jsc:mainframes)).")
                .paragraph("CDE bundles its own set. It has no This PC, so no CDE launcher shows it. Its answer to"
                        + " the question is Workstation Info ([](jsc:cde_workstation_info)).")
                .paragraph("A desktop you install starts at the next boot, not at the moment you install it. The"
                        + " computer checks what it has installed during its self-test, so restart it after an"
                        + " install ([](jsc:firmware)).")
                .ifSomethingGoesWrong("The system starts at a shell, with no desktop.",
                        "No desktop package is installed. Install one, as [](jsc:install_a_desktop) shows.",
                        "You installed a desktop, and the screen still shows the shell.",
                        "The desktop starts at the next boot. Restart the computer.",
                        "The computer is a Vintage one, and the screen shows only the terminal.",
                        "Every desktop on this page needs Legacy hardware or later, and Cinnamon needs Standard."
                                + " Use a newer computer.")
                .seeAlso("jsc:install_a_desktop", "jsc:desktop_program_names", "jsc:the_systems", "jsc:cde")
                .register();
    }

    private static void installADesktop(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("install_a_desktop", section).titled("Installing a desktop").icon(ComputingModule.MONITOR)
                .paragraph("On Linux and FreeBSD you have two ways to get a desktop. You can choose one while the"
                        + " system installs, or add one afterwards from the terminal. Both ways read the Mirror,"
                        + " the network's package shelf ([](jsc:mirror)).")
                .paragraph("A Mirror answers when your computer reaches a Mainframe that runs it, through a data"
                        + " cable. The Mirror is a service, so it stops answering when the cable is cut.")
                .subheading("At install time")
                .paragraph("The installer of Ubuntu, Debian, Fedora and FreeBSD has a Desktop page. Gentoo, Arch"
                        + " Linux, UNIX System V and MC-DOS have none. The Desktop page appears only when a Mirror"
                        + " answers.")
                .steps("Connect the computer to a network whose Mainframe runs the Mirror.",
                        "Start the installer of Ubuntu, Debian, Fedora or FreeBSD.",
                        "On the Desktop page, pick a desktop, or the terminal only.",
                        "Let the installer finish. A desktop you chose is installed as a step of its own.",
                        "Restart the computer, so the desktop starts.")
                .subheading("After the install")
                .paragraph("Once the system runs, you add a desktop from its terminal. Use the install form of the"
                        + " system's package manager, and give the desktop's name ([](jsc:software_commands)).")
                .table("The install forms", "Package manager", "Systems", "Form")
                .row(GuideCell.words("apt"), GuideCell.words("Debian, Ubuntu"),
                        GuideCell.code("apt install <name>"))
                .row(GuideCell.words("dnf"), GuideCell.words("Fedora"), GuideCell.code("dnf install <name>"))
                .row(GuideCell.words("pacman"), GuideCell.words("Arch Linux"), GuideCell.code("pacman -S <name>"))
                .row(GuideCell.words("emerge"), GuideCell.words("Gentoo"), GuideCell.code("emerge <name>"))
                .row(GuideCell.words("pkg"), GuideCell.words("FreeBSD"), GuideCell.code("pkg install <name>"))
                .paragraph("The name is the desktop's command name: kde-plasma, gnome, cinnamon or cde. The id works"
                        + " too, as kde_plasma. Capitals do not matter.")
                .paragraph("Gentoo builds a desktop from source in the terminal, and the terminal waits until the build"
                        + " is done.")
                .paragraph("Which desktops the installer offers depends on the system and on your computer's era.")
                .paragraph("On a Legacy computer it offers KDE Plasma, GNOME and CDE, where the system runs them."
                        + " A Standard computer is also offered Cinnamon. A Vintage computer is offered none.")
                .paragraph("UNIX System V installs its programs from its install media, and has no package manager."
                        + " A desktop is not added from the Mirror on UNIX.")
                .ifSomethingGoesWrong(
                        "The Desktop page does not appear.",
                        "No Mirror answers, or the installer offers no desktop. Connect to a network whose Mainframe"
                                + " runs the Mirror, or add a desktop after the install.",
                        "could not resolve mirror:// - connect this computer to a network whose Mainframe runs the"
                                + " Mirror service",
                        "Connect the computer to a network whose Mainframe runs the Mirror, then try again.",
                        "The package manager says \"unable to locate package\" and the name.",
                        "The name is not a package this system is offered. Check the name: kde-plasma, gnome,"
                                + " cinnamon or cde.",
                        "cinnamon needs Standard hardware or later",
                        "The computer is older than the desktop. Use a computer of that era or a newer one.",
                        "unmet requirements (hardware or free disk space)",
                        "The disk has less free room than the desktop takes, or the hardware is short. Free some"
                                + " disk space, then try again.",
                        "This computer cannot store installed programs.",
                        "The computer has no disk to keep programs on. Give it one.",
                        "The message \"This computer is still setting up\" and a desktop's name.",
                        "A previous install is still running. Wait until it finishes.",
                        "Setup cancelled. Nothing was installed.",
                        "The setup stopped, so the desktop was not added. Install it again, as the steps above show.",
                        "The package manager says \"<name> is already the newest version\".",
                        "The desktop is installed already. Restart the computer, because it starts at the next boot.")
                .seeAlso("jsc:desktops", "jsc:the_systems", "jsc:installing", "jsc:mirror")
                .register();
    }

    private static void kdePlasma(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("kde_plasma", section).titled("KDE Plasma").icon(ComputingModule.MONITOR)
                .paragraph("KDE Plasma is the desktop of the KDE project. It runs on Linux and FreeBSD, on Legacy"
                        + " hardware or later, and its package is named kde-plasma. Its house is the KDE Guild, a"
                        + " software house of the mod's world.")
                .paragraph("KDE changes its face with the era. On Legacy hardware you see KDE 2. On Transition you"
                        + " see KDE 4, with its Kickoff 4 menu. From the Standard on you see KDE Plasma.")
                .define("Kickoff", "KDE's start menu: a launcher you open from the panel, with a list of programs"
                        + " beside its places.")
                .paragraph("The panel sits along the bottom of the screen. On KDE Plasma, Kickoff is a two-pane"
                        + " launcher: a column of places beside the list of programs. Type in it, and the search"
                        + " looks through every program. KDE 4 shows the tabs Favorites, Applications, Computer,"
                        + " Recently Used and Leave.")
                .paragraph("KDE gives seven programs their own names, as the table shows. The rest keep the names"
                        + " they have on every desktop ([](jsc:desktop_program_names)).")
                .table("KDE's own names", "Program", "Name on KDE Plasma")
                .row(GuideCell.words("Files"), GuideCell.words("Dolphin"))
                .row(GuideCell.words("Editor"), GuideCell.words("Kate"))
                .row(GuideCell.words("Command Prompt"), GuideCell.words("Konsole"))
                .row(GuideCell.words("Calculator"), GuideCell.words("KCalc"))
                .row(GuideCell.words("This PC"), GuideCell.words("Info Center"))
                .row(GuideCell.words("Settings"), GuideCell.words("System Settings"))
                .row(GuideCell.words("Help"), GuideCell.words("Help Center"))
                .paragraph("To install it, use the form for your system in [](jsc:install_a_desktop). On Debian and"
                        + " Ubuntu, that is apt install kde-plasma. The boot lines call it kde plasma, as in"
                        + " \"Starting kde plasma Display Manager\".")
                .define("Display manager", "The boot step that starts a graphical desktop. The boot lines name the"
                        + " desktop it starts.")
                .ifSomethingGoesWrong(
                        "The package manager says \"kde-plasma needs Legacy hardware or later\".",
                        "The computer is a Vintage one. Use a Legacy computer or a newer one.",
                        "The package manager says \"unmet requirements (hardware or free disk space)\".",
                        "Plasma takes 256 MB of disk. Free some disk space, then try again.")
                .seeAlso("jsc:install_a_desktop", "jsc:desktop_program_names", "jsc:eras")
                .register();
    }

    private static void gnome(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gnome", section).titled("GNOME").icon(ComputingModule.MONITOR)
                .paragraph("GNOME is a desktop that runs on Linux and FreeBSD, from Legacy hardware on. Its package"
                        + " is named gnome. Its house is the GNOME Trust, a software house of the mod's world.")
                .paragraph("GNOME changes its face with the era too. On Legacy hardware you see GNOME 1: a thick"
                        + " frame in relief, a centred title in muted purple, warm greys and chunky bevelled studs."
                        + " On Transition you see GNOME 2. From the Standard on you see GNOME itself.")
                .define("Activities overview", "GNOME's full-screen view of the desktop, with a search field, the"
                        + " workspaces and a grid of every program.")
                .paragraph("GNOME has no start menu as such. Its panel is a bar along the top of the screen, with"
                        + " the Activities overview. Open it, and the overview covers the desktop with a search"
                        + " field, the workspace strip and a grid of every program.")
                .paragraph("On Transition, GNOME 2 has its own menu, with sections named Places, System, Preferences,"
                        + " Accessories, Games, Internet and System Tools.")
                .paragraph("GNOME gives its programs its own names, as the table shows. This PC is called About.")
                .table("GNOME's own names", "Program", "Name on GNOME")
                .row(GuideCell.words("Files"), GuideCell.words("Files"))
                .row(GuideCell.words("Editor"), GuideCell.words("Text Editor"))
                .row(GuideCell.words("Command Prompt"), GuideCell.words("Terminal"))
                .row(GuideCell.words("Calculator"), GuideCell.words("Calculator"))
                .row(GuideCell.words("System Monitor"), GuideCell.words("System Monitor"))
                .row(GuideCell.words("Settings"), GuideCell.words("Settings"))
                .row(GuideCell.words("This PC"), GuideCell.words("About"))
                .row(GuideCell.words("Help"), GuideCell.words("Help"))
                .paragraph("To install it, use the form for your system in [](jsc:install_a_desktop). On Fedora,"
                        + " that is dnf install gnome. The boot lines show it as \"Starting gnome Display"
                        + " Manager\".")
                .ifSomethingGoesWrong(
                        "The package manager says \"gnome needs Legacy hardware or later\".",
                        "The computer is a Vintage one. Use a Legacy computer or a newer one.",
                        "The package manager says \"gnome is already the newest version\".",
                        "It is installed already. A desktop that is installed starts at the next boot, so restart"
                                + " the computer.")
                .seeAlso("jsc:install_a_desktop", "jsc:desktop_program_names", "jsc:eras")
                .register();
    }

    private static void cinnamon(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cinnamon", section).titled("Cinnamon").icon(ComputingModule.MONITOR)
                .paragraph("Cinnamon is a desktop from Spearmint Linux, a software house of the mod's world. It runs"
                        + " on Linux and FreeBSD, and it needs Standard hardware or later. It is a later desktop, so"
                        + " no older computer can run it.")
                .paragraph("Cinnamon has one look, on every computer it runs on, so you never meet an older face."
                        + " Its package is named cinnamon.")
                .paragraph("Its menu has three parts: a left rail of favourites, then the categories, then the list of"
                        + " programs in the chosen category. The categories are System, Accessories, Office and"
                        + " Preferences.")
                .table("The categories", "Category", "What it holds")
                .row(GuideCell.words("System"), GuideCell.words("Files, This PC, the System Monitor, the Command"
                        + " Prompt, the Network, Help and the other system tools"))
                .row(GuideCell.words("Accessories"), GuideCell.words("Everything else: the Calculator, the editors,"
                        + " the games"))
                .row(GuideCell.words("Office"), GuideCell.words("Exceed, the Workshop, the Crafting Manager, the"
                        + " Pattern Studio, the Craft Planner, the Storage Insights and the Automation Manager"))
                .row(GuideCell.words("Preferences"), GuideCell.words("Settings"))
                .paragraph("Cinnamon gives its programs its own names: Nemo for Files, xed for the Editor, Terminal for"
                        + " the Command Prompt, System Settings for Settings and System Info for This PC. The full"
                        + " list is in [](jsc:desktop_program_names).")
                .paragraph("To install it, use the form for your system in [](jsc:install_a_desktop). The boot lines"
                        + " show it as \"Starting cinnamon Display Manager\".")
                .ifSomethingGoesWrong(
                        "The package manager says \"cinnamon needs Standard hardware or later\".",
                        "The computer is older than Standard. Use a Standard computer or a newer one.",
                        "The installer does not offer Cinnamon on a Legacy computer.",
                        "On a Legacy computer, choose another desktop, because Cinnamon is offered only on Standard"
                                + " hardware or later.")
                .seeAlso("jsc:install_a_desktop", "jsc:desktop_program_names", "jsc:eras")
                .register();
    }

    private static void desktopProgramNames(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("desktop_program_names", section).titled("Program names under each desktop")
                .icon(ComputingModule.MONITOR)
                .paragraph("Each desktop calls its programs by its own names. The program is the same one: the"
                        + " Editor on KDE Plasma is Kate, and it is the same text editor under any desktop. Find a"
                        + " program in the table under the name your desktop shows.")
                .table("The names under each desktop", "Program", "KDE Plasma", "GNOME", "Cinnamon", "CDE")
                .row(GuideCell.words("Network"), GuideCell.words("Network"), GuideCell.words("Network"),
                        GuideCell.words("Network"), GuideCell.words("Network"))
                .row(GuideCell.words("This PC"), GuideCell.words("Info Center"), GuideCell.words("About"),
                        GuideCell.words("System Info"), GuideCell.words("not bundled"))
                .row(GuideCell.words("Settings"), GuideCell.words("System Settings"), GuideCell.words("Settings"),
                        GuideCell.words("System Settings"), GuideCell.words("Style Manager"))
                .row(GuideCell.words("Files"), GuideCell.words("Dolphin"), GuideCell.words("Files"),
                        GuideCell.words("Nemo"), GuideCell.words("File Manager"))
                .row(GuideCell.words("Editor"), GuideCell.words("Kate"), GuideCell.words("Text Editor"),
                        GuideCell.words("xed"), GuideCell.words("Text Editor"))
                .row(GuideCell.words("Command Prompt"), GuideCell.words("Konsole"), GuideCell.words("Terminal"),
                        GuideCell.words("Terminal"), GuideCell.words("Terminal"))
                .row(GuideCell.words("System Monitor"), GuideCell.words("System Monitor"),
                        GuideCell.words("System Monitor"), GuideCell.words("System Monitor"),
                        GuideCell.words("Performance Meter"))
                .row(GuideCell.words("Calculator"), GuideCell.words("KCalc"), GuideCell.words("Calculator"),
                        GuideCell.words("Calculator"), GuideCell.words("Calculator"))
                .row(GuideCell.words("Network Manager"), GuideCell.words("Network Manager"),
                        GuideCell.words("Network Manager"), GuideCell.words("Network Manager"),
                        GuideCell.words("Network Manager"))
                .row(GuideCell.words("Help"), GuideCell.words("Help Center"), GuideCell.words("Help"),
                        GuideCell.words("Help"), GuideCell.words("Help Viewer"))
                .row(GuideCell.words("Workstation Info"), GuideCell.words("not bundled"),
                        GuideCell.words("not bundled"), GuideCell.words("not bundled"),
                        GuideCell.words("Workstation Info"))
                .paragraph("A product name, such as Dolphin, Kate or Nemo, reads the same in every language. A plain"
                        + " name, such as Text Editor or System Settings, is translated with the rest of the"
                        + " screen.")
                .paragraph("A program keeps its own window whatever its launcher is called. So a renamed launcher"
                        + " still opens the same program.")
                .paragraph("The Network Manager is bundled on every desktop, but it shows only on a Mainframe. On a"
                        + " computer that is not a Mainframe, you do not see it in any launcher.")
                .ifSomethingGoesWrong(
                        "You look for This PC on CDE, and it is not there.",
                        "CDE does not bundle This PC. Use Workstation Info, which answers the same question.",
                        "You look for the Network Manager, and it is not in the menu.",
                        "It shows only on a Mainframe. Look for it on a Mainframe.")
                .seeAlso("jsc:desktops", "jsc:cde_workstation_info", "jsc:help_on_a_computer")
                .register();
    }

    private static void cde(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde", section).titled("CDE").icon(ComputingModule.MONITOR)
                .paragraph("CDE, the Common Desktop Environment, is the desktop of the Unix workstations, and the only"
                        + " desktop UNIX has. It runs on UNIX, FreeBSD and Linux. It is light: it needs Legacy"
                        + " hardware or later, 32 MB of disk and 16 MB of memory while it runs.")
                .paragraph("CDE has no login and no logout. There is nobody to log in, so you never see a login."
                        + " It starts straight away, with \"Starting the desktop\" on its splash screen.")
                .paragraph("Where the other desktops have a start menu, a taskbar and an Action Center, CDE has one"
                        + " strip: the Front Panel, along the bottom centre of the screen. It has no start menu, no"
                        + " taskbar and no Action Center. It lists no open windows, because CDE never did: a window"
                        + " you put away becomes an icon on its workspace.")
                .define("Front Panel", "CDE's strip of controls at the bottom of the screen, with the workspace switch"
                        + " in its middle.")
                .paragraph("The Front Panel holds its controls from left to right: Clock, Calendar, File Manager, Text"
                        + " Editor, Style Manager, Applications, Trash Can and Help Viewer. Most of them open"
                        + " something, as [](jsc:cde_front_panel) lists.")
                .paragraph("A control with a small arrow at its head raises a subpanel: a tray that slides up out of"
                        + " the Front Panel. A subpanel stays up until you press its arrow again, and only one is up"
                        + " at a time.")
                .define("Subpanel", "A tray that slides up out of a Front Panel control, and stays up until its arrow"
                        + " is pressed again.")
                .subheading("Workspaces")
                .paragraph("CDE has four workspaces, named One, Two, Three and Four. Each is a separate screen of the"
                        + " same desktop, and each can wear its own backdrop ([](jsc:cde_style_manager)).")
                .define("Workspace", "One of four separate screens of the same desktop. A window lives on one, or on"
                        + " all of them.")
                .subheading("Windows")
                .paragraph("Each window has a title bar in the Motif style. The button at its left opens the window"
                        + " menu: Restore, Minimize, Maximize, Lower, Occupy Workspace..., Occupy All Workspaces and"
                        + " Close. A double click on that button closes the window with no menu.")
                .define("Motif", "The window style of CDE: a title bar, with a menu button at its left.")
                .paragraph("To install CDE, use the form for your system in [](jsc:install_a_desktop). On Debian"
                        + " and Ubuntu, that is apt install cde.")
                .ifSomethingGoesWrong(
                        "You look for a Start menu, and there is none.",
                        "CDE has no Start menu. Open Applications on the Front Panel to find your programs.",
                        "The package manager says \"cde needs Legacy hardware or later\".",
                        "The computer is a Vintage one. Use a Legacy computer or a newer one.")
                .seeAlso("jsc:cde_front_panel", "jsc:cde_style_manager", "jsc:cde_application_manager",
                        "jsc:cde_workstation_info", "jsc:cde_trash_can", "jsc:install_a_desktop")
                .register();
    }

    private static void cdeFrontPanel(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_front_panel", section).titled("The Front Panel and EXIT").icon(ComputingModule.MONITOR)
                .paragraph("Each control of the Front Panel shows its name when you rest the pointer on it, such as"
                        + " \"File Manager\" or \"Trash Can\".")
                .table("The controls", "Control", "What it does")
                .row(GuideCell.words("Clock"), GuideCell.words("Shows the time, with its hands"))
                .row(GuideCell.words("Calendar"), GuideCell.words("Shows the date, with the day on its page"))
                .row(GuideCell.words("File Manager"), GuideCell.words("Opens Files, with a subpanel of Home, Desktop"
                        + " and each medium in a drive"))
                .row(GuideCell.words("Text Editor"), GuideCell.words("Opens a subpanel headed Personal Applications:"
                        + " Text Editor, Command Prompt and Calculator"))
                .row(GuideCell.words("Style Manager"), GuideCell.words("Opens the Style Manager"))
                .row(GuideCell.words("Applications"), GuideCell.words("Opens a subpanel headed Applications: the"
                        + " Application Manager, System Monitor and Workstation Info"))
                .row(GuideCell.words("Trash Can"), GuideCell.words("Opens the Trash, and its picture shows whether it"
                        + " is empty or full"))
                .row(GuideCell.words("Help Viewer"), GuideCell.words("Opens the Help Viewer"))
                .paragraph("The EXIT button sits at the right end of the workspace switch, beside the four workspaces."
                        + " It is not at the far right of the panel. Press it to open the Exit dialog, which has three"
                        + " buttons: Shut Down, Restart and Cancel.")
                .paragraph("The dialog says how many programs are open. The count is spelled out, so it reads, for"
                        + " example, \"Three programs are still open\", followed by \"on this workstation.\" The last"
                        + " line warns that what is not saved will be lost.")
                .table("The Exit dialog", "Words on the dialog", "When it shows them")
                .row(GuideCell.words("No program is open"), GuideCell.words("No program runs"))
                .row(GuideCell.words("One program is still open"), GuideCell.words("One program runs"))
                .row(GuideCell.words("Two or more programs are still open"), GuideCell.words("Two or more programs"
                        + " run, with the number spelled out"))
                .ifSomethingGoesWrong(
                        "The dialog says a program is still open.",
                        "Close that program first, so that nothing is lost, then press EXIT.")
                .seeAlso("jsc:cde", "jsc:cde_trash_can")
                .register();
    }

    private static void cdeStyleManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_style_manager", section).titled("The Style Manager").icon(ComputingModule.MONITOR)
                .paragraph("The Style Manager is the settings window of CDE. Press its control on the Front Panel to"
                        + " open it. Its window is called Style Manager - Color, Style Manager - Backdrop or Style"
                        + " Manager - Audio, depending on the page.")
                .paragraph("It has three pages in a strip, and a click on one opens it. Color is the palette the"
                        + " whole desktop is drawn from. Backdrop is the pattern each workspace wears. Audio sets the"
                        + " system's volume and where its sound goes.")
                .paragraph("Everything else the machine keeps about itself is still set at its prompt, with the"
                        + " config command.")
                .subheading("Color")
                .paragraph("Color has eight palettes, under the names they have: Default, Alpine, Broica, Charcoal,"
                        + " Desert, Neptune, SeaFoam and Urchin. The whole desktop changes to a palette as soon as"
                        + " you pick it, so you see it before you keep it.")
                .steps("Open the Color page.",
                        "Pick a palette. The desktop changes to show it.",
                        "Press OK to keep it with the machine.")
                .paragraph("OK keeps the palette with the machine. Cancel, or closing the page any other way, puts back"
                        + " the palette the desktop had when the page opened. The buttons along the foot are Apply,"
                        + " OK and Cancel.")
                .subheading("Backdrop")
                .paragraph("The backdrop is the pattern behind the windows. Each workspace wears its own, so the four"
                        + " workspaces can look different. The six patterns are Hatch, Pinstripe, Tiles, Weave, Dots"
                        + " and Plain.")
                .steps("Open the Backdrop page.",
                        "Pick a pattern. Only the preview changes.",
                        "Press Apply. The pattern goes on the workspace named on the page.")
                .paragraph("The page stays open after Apply, so you can change the next workspace.")
                .subheading("Audio")
                .paragraph("Audio has a volume scale with its value over the slider, and a Mute switch. It has two"
                        + " outputs, each a toggle, and both may be on. One output always stays on: the last one"
                        + " cannot be turned off.")
                .paragraph("Nothing is sent to the machine while you work the page, and only OK sends your choice to"
                        + " it.")
                .ifSomethingGoesWrong(
                        "A colour change does not stay.",
                        "You left the Color page with Cancel, or by closing it. Open the page again and press OK.",
                        "A backdrop change does not stay.",
                        "Press Apply for that workspace.")
                .seeAlso("jsc:cde", "jsc:cde_front_panel")
                .register();
    }

    private static void cdeApplicationManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_application_manager", section).titled("The Application Manager").icon(ComputingModule.MONITOR)
                .paragraph("The Application Manager is CDE's program list. It groups the programs in folders, so you"
                        + " find a program by its group. Open it from the Applications control on the Front Panel, or"
                        + " from the first row of the Applications subpanel.")
                .table("The groups", "Group", "What it holds")
                .row(GuideCell.words("Desktop_Apps"), GuideCell.words("Files, Editor, Command Prompt, Calculator and"
                        + " Settings"))
                .row(GuideCell.words("Desktop_Tools"), GuideCell.words("Every program named in no other group, such"
                        + " as one someone else wrote."))
                .row(GuideCell.words("Network"), GuideCell.words("Network, Network Manager, ISMS, NextGre Studio,"
                        + " Prophet Console, Gateway Manager, Remote Control, Cluster Manager, Crafting Manager,"
                        + " Storage Insights, Craft Planner, Automation Manager and Pattern Studio"))
                .row(GuideCell.words("Games"), GuideCell.words("Minesweeper"))
                .paragraph("A group is drawn only when it holds a program. Its status line says how many programs it"
                        + " holds, such as \"Games, 1 program\". A double click on a group opens it in its own window,"
                        + " and a double click on a program starts it.")
                .paragraph("The window reads the desktop each time it draws, so a program you install while the window"
                        + " is open appears at once, without a refresh.")
                .ifSomethingGoesWrong(
                        "A group you expect is not drawn.",
                        "A group with no program in it is not drawn. Install a program for it.",
                        "A program is not in the group you expect.",
                        "A program named in no other group lands in Desktop_Tools. Look there.")
                .seeAlso("jsc:cde", "jsc:cde_front_panel")
                .register();
    }

    private static void cdeWorkstationInfo(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_workstation_info", section).titled("Workstation Info").icon(ComputingModule.MONITOR)
                .paragraph("Workstation Info tells you about the workstation you sit at: who is using it, the system it"
                        + " runs and the hardware under it. It is the CDE answer to This PC, and only CDE bundles it."
                        + " Open it from the Applications subpanel, or from the Applications control.")
                .table("What it shows", "Group", "Rows")
                .row(GuideCell.words("Workstation"), GuideCell.words("User Name, Host Name and Network, which reads"
                        + " \"Not on a network\" when there is none"))
                .row(GuideCell.words("System"), GuideCell.words("Operating System, Architecture and Window System"))
                .row(GuideCell.words("Hardware"), GuideCell.words("Processor, with its clock, Physical Memory, Memory"
                        + " in Use with a meter, Video Memory and System Disk"))
                .paragraph("Each figure is read when the window opens, and again every few seconds while the window"
                        + " stays open. So Memory in Use moves as the workstation works.")
                .paragraph("The Devices... button opens the Devices dialog. It shows one row for each port of the"
                        + " workstation: Video, then Audio, then the other ports by name. Each row shows what is"
                        + " plugged in, or \"free\". Select a device to Disable or Enable it. A disabled device is"
                        + " shown crossed out, in a quieter ink.")
                .ifSomethingGoesWrong(
                        "Network reads \"Not on a network\".",
                        "This workstation is not on a network. Connect it to one, as [](jsc:first_network) shows.")
                .seeAlso("jsc:desktop_program_names", "jsc:cde")
                .register();
    }

    private static void cdeTrashCan(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cde_trash_can", section).titled("The Trash Can").icon(ComputingModule.MONITOR)
                .paragraph("On CDE the trash is the Trash Can, a control on the Front Panel. Other desktops keep"
                        + " it as an icon on the wallpaper. Its picture shows whether it is empty or full, so you"
                        + " see at a glance whether it holds anything. Click it to open the Trash window.")
                .define("Put Back", "CDE's word for restoring an item to where it was. Other desktops call this"
                        + " Restore.")
                .define("Shred", "CDE's word for deleting an item for good. Other desktops call this Delete.")
                .paragraph("The Trash window has three menus: File, Selected and View. Put Back restores the selected"
                        + " items to where they were. Shred deletes them for good. The window also has Select All,"
                        + " Deselect All, By Name and By Size.")
                .paragraph("CDE has no Empty Trash button.")
                .paragraph("Before a delete, CDE asks Confirm File Delete. For one item it asks, for example, \"Delete"
                        + " the item in the Home for good?\" Its properties show the name, the original location and"
                        + " the size. The original location is Home or Desktop.")
                .ifSomethingGoesWrong(
                        "You shredded an item you wanted.",
                        "Shred deletes for good, and the item cannot be put back. Check the selection before you press"
                                + " Shred.",
                        "There is no Empty Trash button.",
                        "Select everything in the Trash window, then press Shred.")
                .seeAlso("jsc:cde_front_panel", "jsc:desktops")
                .register();
    }
}
