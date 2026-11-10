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
 * The OpenBSD chapter of The Daemon Handbook: the system secure by default, its installer of questions and answers,
 * its boot, its daemons, its packages, patches and ports, and pf, doas and the kernel debugger.
 */
final class OpenBsdPages {

    private OpenBsdPages() {
    }

    /** Declares the sections and entries of OpenBSD, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef system = guide.section("openbsd_system").titled("The system").in(chapter)
                .register();
        overview(guide, system);
        installer(guide, system);
        daemons(guide, system);
        final ModGuide.SectionRef software = guide.section("openbsd_software").titled("Software").in(chapter)
                .register();
        packages(guide, software);
        patches(guide, software);
        final ModGuide.SectionRef tools = guide.section("openbsd_tools").titled("Tools").in(chapter).register();
        defence(guide, tools);
        debugger(guide, tools);
    }

    private static void overview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_overview", section).titled("OpenBSD on this computer")
                .paragraph("OpenBSD is a UNIX-like system made by the Blowfish Project, and it is secure by default:"
                        + " it starts at its terminal with only what it needs switched on, and everything else waits"
                        + " for you to switch it on. Its mark is Puffy, the blowfish.")
                .define("Secure by default", "A system that comes up with as little running as it can, so nothing"
                        + " runs that you did not ask for.")
                .paragraph("OpenBSD needs Legacy hardware or newer. It takes 1,024 MB of disk space, less when you"
                        + " leave sets out at the installer, and 12 MB of memory while it runs.")
                .table("OpenBSD at a glance")
                .property("Minimum hardware era", "Legacy")
                .amount("Disk space of the installed system", 1024, "MB")
                .amount("Memory while it runs", 12, "MB")
                .property("Kernel", "OpenBSD 7.5 GENERIC#79")
                .property("Shell", "ksh")
                .property("Package manager", "pkg_add")
                .property("Console", "ttyC0")
                .subheading("What you see when it starts")
                .paragraph("Its prompt is the computer's name and a hash, because you are root. The greeting names"
                        + " the ways to find your way: apropos and the afterboot page are there only when the man"
                        + " set is installed.")
                .session("jsc:guide/terminal/sh", "OpenBSD")
                .answer("OpenBSD/IA-32 (<hostname>.my.domain) (ttyC0)", "", "login: player",
                        "OpenBSD 7.5 (GENERIC) #79", "",
                        "Welcome to OpenBSD: The proactively secure Unix-like operating system.",
                        "  Find a command by what it does:", "    apropos <word>",
                        "  Learn the system:", "    man afterboot", "",
                        "Tip: rcctl ls on lists every service switched on here.")
                .subheading("A desktop on OpenBSD")
                .paragraph("OpenBSD takes the same desktops FreeBSD takes. A desktop installed still comes up at the"
                        + " terminal: its display manager, xenodm, starts it only once you type rcctl enable xenodm"
                        + " ([](jsc:openbsd_daemons)). Every desktop hangs the same picture of the sea.")
                .ifSomethingGoesWrong("A desktop is installed and the computer still starts at the terminal.",
                        "xenodm is off, which is how OpenBSD ships. Type rcctl enable xenodm, and rcctl start xenodm"
                                + " to bring it up now.",
                        "The prompt says ksh: man: not found.",
                        "The man set was left out at the installer. Install the system again with it.")
                .seeAlso("jsc:the_systems", "jsc:openbsd_installer", "jsc:openbsd_daemons")
                .register();
    }

    private static void installer(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_installer", section).titled("Installing OpenBSD, question by question")
                .paragraph("OpenBSD's installer asks its questions at the terminal, one under the other, and you"
                        + " answer each one by typing and pressing Enter. The answer in square brackets is the one"
                        + " Enter takes when you type nothing.")
                .paragraph("Put the OpenBSD medium in a drive joined to the computer and start the computer from it."
                        + " The first question is (I)nstall, (U)pgrade, (A)utoinstall or (S)hell?: type i.")
                .table("The questions it asks", "Question", "What your answer does")
                .row(GuideCell.code("System hostname?"), GuideCell.words("Names the computer."))
                .row(GuideCell.code("Start sshd(8) by default?"), GuideCell.words("Lets other computers reach this"
                        + " one, or not."))
                .row(GuideCell.code("Do you expect to run the X Window System?"), GuideCell.words("Says whether a"
                        + " desktop will run here at all."))
                .row(GuideCell.code("...started by xenodm(1)?"), GuideCell.words("Starts the desktop with the"
                        + " computer. No, the default, keeps it at the terminal."))
                .row(GuideCell.code("Which disk is the root disk?"), GuideCell.words("Chooses the disk the system"
                        + " goes on: wd0 for an IDE disk, sd0 for a SATA one."))
                .row(GuideCell.code("Use (W)hole disk or (E)dit the MBR?"), GuideCell.words("w takes the whole"
                        + " disk; e puts OpenBSD in the free room beside what the disk holds."))
                .row(GuideCell.code("Location of sets?"), GuideCell.words("The medium, or http to fetch them from"
                        + " the network's Mirror."))
                .row(GuideCell.code("Set name(s)?"), GuideCell.words("Which sets to install; done starts the copy."))
                .paragraph("The network, the password and the time zone are answered for you: the computer has no"
                        + " account to protect yet and the world keeps one clock.")
                .subheading("The sets")
                .paragraph("The system comes in five sets. Type -man* to leave the manual out, and the name of a set"
                        + " to put it back; all puts every set back.")
                .table("The sets", "Set", "What it holds", "Left out")
                .row(GuideCell.code("bsd"), GuideCell.words("The kernel."), GuideCell.words("Always installed."))
                .row(GuideCell.code("bsd.rd"), GuideCell.words("The kernel that repairs a system."),
                        GuideCell.words("Saves its room."))
                .row(GuideCell.code("base75.tgz"), GuideCell.words("The base system."),
                        GuideCell.words("Always installed."))
                .row(GuideCell.code("comp75.tgz"), GuideCell.words("The compiler and its tools."),
                        GuideCell.words("Saves its room, and make goes with it."))
                .row(GuideCell.code("man75.tgz"), GuideCell.words("The manual's pages."),
                        GuideCell.words("Saves its room, and man and apropos go with it."))
                .paragraph("When the copy ends, answer Exit to (S)hell, (H)alt or (R)eboot? with r, and take the medium"
                        + " out.")
                .ifSomethingGoesWrong("The installer says the disk is too small.",
                        "OpenBSD needs 1,024 MB. Choose a bigger disk, or answer w to take the whole disk.",
                        "The installer says it is not a valid choice.",
                        "Type the disk's name as the question lists it, such as wd0.")
                .seeAlso("jsc:installing", "jsc:openbsd_overview")
                .register();
    }

    private static void daemons(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_daemons", section).titled("The boot, and the daemons rcctl switches")
                .paragraph("OpenBSD starts through its boot prompt, boot>. It waits five seconds without counting, and"
                        + " then starts the system; press any key to stop the wait, and Enter or boot to start. reboot"
                        + " starts the computer over.")
                .define("Daemon", "A program that runs in the background, without a window of its own.")
                .paragraph("rcctl is how you switch the daemons. enable and disable say whether a daemon starts with"
                        + " the system; start and stop act on it now. A service you install comes switched off, so"
                        + " you enable it before it runs.")
                .table("The daemons", "Daemon", "What it does")
                .row(GuideCell.code("cron"), GuideCell.words("Runs scheduled jobs. On by default."))
                .row(GuideCell.code("sshd"), GuideCell.words("Lets other computers reach this one."))
                .row(GuideCell.code("pf, pflogd, syslogd"), GuideCell.words("The firewall, its log and the system"
                        + " log. They always run."))
                .row(GuideCell.code("xenodm"), GuideCell.words("Starts the desktop, once one is installed. Off by"
                        + " default."))
                .steps("Type rcctl ls on to see what is switched on.", "Type rcctl enable xenodm to start the desktop"
                        + " with the computer from now on.", "Type rcctl start xenodm to bring it up now.")
                .paragraph("cron and sshd are the computer's own switches, so stopping one switches it off for good"
                        + " and starting one switches it on. A daemon switched off starts only with rcctl -f start.")
                .command(CommandGuidePages.id(Platform.OPENBSD, "rcctl"))
                .ifSomethingGoesWrong("rcctl says the service does not exist.",
                        "Type rcctl ls all to see the daemons the computer has. xenodm is there only with a desktop.",
                        "rcctl says to use -f to force start.",
                        "The daemon is switched off. Type rcctl enable and its name first.")
                .seeAlso("jsc:openbsd_overview", "jsc:processes_and_jobs")
                .register();
    }

    private static void packages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_packages", section).titled("Packages and the ports tree")
                .paragraph("OpenBSD's package manager is three commands. pkg_add fetches a package from the network's"
                        + " Mirror and installs it, pkg_info lists what is installed, and pkg_delete takes a package"
                        + " off.")
                .table("The three tools", "Command", "What it does")
                .row(GuideCell.code("pkg_add <package>"), GuideCell.words("Installs a package from the Mirror."))
                .row(GuideCell.code("pkg_add -u"), GuideCell.words("Brings every installed package up to the"
                        + " Mirror's version."))
                .row(GuideCell.code("pkg_info"), GuideCell.words("Lists the installed packages."))
                .row(GuideCell.code("pkg_info -Q <term>"), GuideCell.words("Searches the Mirror."))
                .row(GuideCell.code("pkg_delete <package>"), GuideCell.words("Takes a package off."))
                .row(GuideCell.code("pkg_add -un"), GuideCell.words("Lists what pkg_add -u would bring up, each"
                        + " package with its two versions, and installs nothing."))
                .command(CommandGuidePages.id(Platform.OPENBSD, "pkg_add"))
                .command(CommandGuidePages.id(Platform.OPENBSD, "pkg_info"))
                .command(CommandGuidePages.id(Platform.OPENBSD, "pkg_delete"))
                .subheading("The ports tree")
                .paragraph("The ports tree is the recipe of every program, to build on your own computer. On OpenBSD"
                        + " you fetch it with cvs, and build a port with make, which needs the comp set.")
                .steps("Type cvs -q checkout -P ports to lay the tree out under /usr/ports.",
                        "Go to a port's folder, such as cd /usr/ports/sysutils/screenfetch.",
                        "Type make install clean to build it, install it and clean up.",
                        "Later, type cvs -q update -Pd in /usr/ports to bring the tree up to date.")
                .command(CommandGuidePages.id(Platform.OPENBSD, "cvs"))
                .seeAlso("jsc:openbsd_patches", "jsc:openbsd_overview")
                .register();
    }

    private static void patches(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_patches", section).titled("syspatch, the system's own patches")
                .paragraph("OpenBSD stays at version 7.5: what changes is its patches, which syspatch fetches for you"
                        + " from the network's Mirror and installs. The release has two, 001_ksh and 002_sshd.")
                .table("What syspatch does", "Command", "What it does")
                .row(GuideCell.code("syspatch -c"), GuideCell.words("Lists the patches not installed yet."))
                .row(GuideCell.code("syspatch"), GuideCell.words("Installs them, and links the kernel anew."))
                .row(GuideCell.code("syspatch -l"), GuideCell.words("Lists the patches installed."))
                .row(GuideCell.code("syspatch -r"), GuideCell.words("Takes the last patch installed back off."))
                .command(CommandGuidePages.id(Platform.OPENBSD, "syspatch"))
                .ifSomethingGoesWrong("syspatch says there is no route to the Mirror.",
                        "The computer is not on a network whose Mainframe runs the Mirror ([](jsc:openbsd_packages)).")
                .seeAlso("jsc:openbsd_packages")
                .register();
    }

    private static void defence(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_pf_doas", section).titled("pf and doas")
                .paragraph("pf is OpenBSD's firewall. It always runs, its rules are in /etc/pf.conf, and pfctl -si"
                        + " says how long it has run and what its state table holds. What its rules let through and"
                        + " what they stop comes with the network's security.")
                .listing("/etc/pf.conf", "# See pf.conf(5) and /etc/examples/pf.conf", "", "set skip on lo", "",
                        "block return    # block stateless traffic", "pass            # establish keep-state")
                .command(CommandGuidePages.id(Platform.OPENBSD, "pfctl"))
                .paragraph("doas runs a command as another user, root unless -u names one. Its one rule,"
                        + " permit persist :wheel in /etc/doas.conf, lets the wheel group do it, and root is in the"
                        + " wheel group. Until there are accounts you are root, so doas runs what you give it.")
                .paragraph("doas -C /etc/doas.conf and a command say whether the rules permit it, without running it.")
                .command(CommandGuidePages.id(Platform.OPENBSD, "doas"))
                .seeAlso("jsc:openbsd_daemons")
                .register();
    }

    private static void debugger(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("openbsd_ddb", section).titled("When the system panics: ddb")
                .paragraph("When OpenBSD's kernel panics, it stops in its debugger, ddb, and waits at the ddb>"
                        + " prompt. It never starts over by itself: you read what happened, and then you start it"
                        + " over.")
                .table("What ddb takes", "Command", "What it shows")
                .row(GuideCell.code("show panic"), GuideCell.words("What the kernel stopped on."))
                .row(GuideCell.code("trace"), GuideCell.words("Where it was when it stopped."))
                .row(GuideCell.code("ps"), GuideCell.words("The processes that were running."))
                .row(GuideCell.code("boot reboot"), GuideCell.words("Starts the computer over."))
                .seeAlso("jsc:openbsd_overview")
                .register();
    }
}
