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
 * The pages of the achievements, one page per tab with a table of what each one asks and how the game checks it,
 * and the pages of the library: each of the six books, what it covers, and the system whose help opens it first.
 */
final class LibraryPages {

    private LibraryPages() {
    }

    /** Declares the sections and entries of the achievements and of the library, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        theAchievements(guide, chapter);
        theLibrary(guide, chapter);
    }

    private static void theAchievements(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("the_achievements").titled("Achievements")
                .icon(ComputingModule.PERSONAL_COMPUTER).in(chapter).register();
        achievements(guide, section);
        achievementsHardware(guide, section);
        achievementsSystems(guide, section);
        achievementsNetworks(guide, section);
        achievementsSigma(guide, section);
    }

    private static void achievements(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("achievements", section).titled("Achievements").icon(ComputingModule.PERSONAL_COMPUTER)
                .paragraph("An achievement is a step the game records for you the first time you do something worth"
                        + " noting, such as building a working computer or booting your first system. You earn each"
                        + " one once, and the game lists them in four tabs: Hardware, Operating Systems, Networks &"
                        + " Operations, and Sigma.")
                .table("The four tabs", "Tab", "What it rewards")
                .row(GuideCell.words("Hardware"),
                        GuideCell.words("Building computers, racks, datacenters and clusters."))
                .row(GuideCell.words("Operating Systems"),
                        GuideCell.words("Installing and booting systems, and the programs you run on them."))
                .row(GuideCell.words("Networks & Operations"),
                        GuideCell.words("Bringing a network to life, and running its Operations, the query language"
                                + " IQL, and autocrafting."))
                .row(GuideCell.words("Sigma"),
                        GuideCell.words("Writing, compiling and running Sigma and Sigma# programs."))
                .paragraph("Each tab starts with a root, its first achievement, and the others hang from it. Every"
                        + " achievement has a kind. A task is a plain step, a goal is a larger milestone, a challenge"
                        + " asks for several separate things, and a secret stays hidden in its tab until you earn it."
                        + " Once you earn a secret, its description shows too.")
                .paragraph("The game credits you for what you do yourself: when you do the thing, the achievement is"
                        + " yours. A machine that works on its own, such as a computer that finishes its power-on"
                        + " self-test, or a job that runs at a set time, credits its operator instead.")
                .paragraph("If you were offline when it happened, the achievement waits for you and appears at your"
                        + " next login. The game keeps up to 128 waiting achievements for each player.")
                .ifSomethingGoesWrong("An achievement you earned did not appear.",
                        "Check who did the action. Your own actions earn it for you, and a machine's achievement"
                                + " goes to its operator. If you were offline, it appears at your next login.",
                        "A machine's achievement went to another player.",
                        "The operator is the last player who placed or used that machine. Use the machine yourself,"
                                + " and its later achievements are yours.")
                .seeAlso("jsc:achievements_hardware", "jsc:achievements_systems", "jsc:achievements_networks",
                        "jsc:achievements_sigma")
                .define("Achievement", "A step the game records once for each player, when that player does"
                        + " something worth noting.")
                .define("Operator", "The player a machine's own achievements are credited to: the last player who"
                        + " placed or used the machine.")
                .register();
    }

    private static void achievementsHardware(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("achievements_hardware", section).titled("Hardware achievements")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("The Hardware tab rewards building. Its root is the first step, and the others ask you to"
                        + " build working computers, fill racks, link monitors and run whole clusters. The table says"
                        + " what each one asks, and how the game checks it.")
                .table("The Hardware achievements", "Achievement", "What it asks", "How it is earned")
                .row(GuideCell.words("Hardware (root)"),
                        GuideCell.words("Get your hands on a computer."),
                        GuideCell.words("You hold any computer item in your inventory: a computer case of any era,"
                                + " or a Mainframe."))
                .row(GuideCell.words("PC Master Race (task)"),
                        GuideCell.words("Watch a computer pass its power-on self-test."),
                        GuideCell.words("A computer finishes its power-on self-test (POST)."))
                .row(GuideCell.words("Have You Tried Turning It Off and On Again? (task)"),
                        GuideCell.words("Restart a running computer from its power menu."),
                        GuideCell.words("You choose restart in the power menu of the desktop. Shutting down does"
                                + " not count."))
                .row(GuideCell.words("Download More RAM (task)"),
                        GuideCell.words("Fill every RAM slot on a motherboard."),
                        GuideCell.words("The random-access memory (RAM) installed fills every RAM slot of the"
                                + " motherboard, checked whenever the parts change."))
                .row(GuideCell.words("The Save Icon (task)"),
                        GuideCell.words("Save a file to a floppy disk."),
                        GuideCell.words("A file is written to a floppy disk in a media slot."))
                .row(GuideCell.words("Battlestation (goal)"),
                        GuideCell.words("Link four monitors to one computer."),
                        GuideCell.words("Four monitors are linked to the same computer; monitors merely placed near it"
                                + " do not count."))
                .row(GuideCell.words("640K Ought to Be Enough (task)"),
                        GuideCell.words("Build a working Vintage computer."),
                        GuideCell.words("A computer of the Vintage era is built and its build is valid."))
                .row(GuideCell.words("Party Like It's 1999 (task)"),
                        GuideCell.words("Build a working Legacy computer."),
                        GuideCell.words("A computer of the Legacy era is built and its build is valid."))
                .row(GuideCell.words("Moore's Law Isn't Dead (task)"),
                        GuideCell.words("Build a working Standard computer."),
                        GuideCell.words("A computer of the Standard era is built and its build is valid."))
                .row(GuideCell.words("Time Traveller (challenge)"),
                        GuideCell.words("Build a working computer of each of the five eras: Vintage, Legacy,"
                                + " Transition, Standard and Advanced."),
                        GuideCell.words("Each of those five eras is built once, with a valid build."))
                .row(GuideCell.words("Rack 'Em Up (goal)"),
                        GuideCell.words("Fill every unit of a Server Rack."),
                        GuideCell.words("The rack's 8 units are all used, and a player filled the last one."))
                .row(GuideCell.words("It's Just Someone Else's Computer (goal)"),
                        GuideCell.words("Form a datacenter section behind a Server Router."),
                        GuideCell.words("A Server Router goes from no datacenter section to at least one."))
                .row(GuideCell.words("Herding Cats (goal)"),
                        GuideCell.words("Run a whole cluster from a Cluster Management Computer."),
                        GuideCell.words("You power on at least one node of the cluster, or you send an install to"
                                + " every node and it takes effect."))
                .row(GuideCell.words("TOP500 (challenge)"),
                        GuideCell.words("Bring a supercomputer online: Supercomputer Racks on High Compute Cable."),
                        GuideCell.words("The HBW Interface is on a network and has parallel crafts (more than"
                                + " zero)."))
                .paragraph("Time Traveller asks for the five eras that have a build achievement: Vintage, Legacy,"
                        + " Transition, Standard and Advanced. The eras Exa and Singularity have no build achievement.")
                .paragraph("Each build hangs from the one before it: 640K Ought to Be Enough comes first, then Party"
                        + " Like It's 1999, then Moore's Law Isn't Dead, and Time Traveller hangs from the last.")
                .ifSomethingGoesWrong("Rack 'Em Up does not appear after you fill a rack.",
                        "The last unit must be placed by a player. A rack filled by other means does not earn it.",
                        "Battlestation does not appear with four monitors near the computer.",
                        "Only monitors linked to the computer count. Link four monitors to it.",
                        "Time Traveller does not appear.",
                        "It hangs from Moore's Law Isn't Dead, so earn that first.")
                .seeAlso("jsc:first_computer", "jsc:eras", "jsc:server_racks", "jsc:cluster_management_computers")
                .register();
    }

    private static void achievementsSystems(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("achievements_systems", section).titled("System achievements")
                .icon(ComputingModule.CD_ROM)
                .paragraph("The Operating Systems tab rewards the systems you install and how you use them. Its root is"
                        + " the first boot of any system. Each system has a step of its own, and a few are hard ones,"
                        + " such as installing by hand or building from source.")
                .paragraph("Most install steps count the first boot: the first time an installed system starts through"
                        + " a monitor. The game marks the disk when it welcomes the system, so the mark stays with the"
                        + " disk. A live installer session does not count.")
                .paragraph("The Frames install steps hang from one another in the order of the editions, from Frames"
                        + " 95 to Frames 11, and the FreeBSD step hangs from the UNIX one.")
                .table("The system achievements", "Achievement", "What it asks", "How it is earned")
                .row(GuideCell.words("Operating Systems (root)"),
                        GuideCell.words("Boot a computer into an operating system."),
                        GuideCell.words("The first boot of any system."))
                .row(GuideCell.words("Abort, Retry, Fail? (task)"),
                        GuideCell.words("Install MC-DOS and boot it."),
                        GuideCell.words("The first boot of MC-DOS."))
                .row(GuideCell.words("Feels like the old days (task)"),
                        GuideCell.words("Install MC-NET and boot it."),
                        GuideCell.words("The first boot of MC-NET."))
                .row(GuideCell.words("Start Me Up (task)"),
                        GuideCell.words("Install Frames 95 and boot it."),
                        GuideCell.words("The first boot of Frames 95."))
                .row(GuideCell.words("The goat (task)"),
                        GuideCell.words("Install Frames XP and boot it."),
                        GuideCell.words("The first boot of Frames XP."))
                .row(GuideCell.words("Never Upgrading (task)"),
                        GuideCell.words("Install Frames 7 and boot it."),
                        GuideCell.words("The first boot of Frames 7."))
                .row(GuideCell.words("What Happened to Frames 9? (task)"),
                        GuideCell.words("Install Frames 10 and boot it."),
                        GuideCell.words("The first boot of Frames 10."))
                .row(GuideCell.words("More like, Bloat 11 (task)"),
                        GuideCell.words("Install Frames 11 and boot it."),
                        GuideCell.words("The first boot of Frames 11."))
                .row(GuideCell.words("Where it all started long ago... (task)"),
                        GuideCell.words("Install UNIX and boot it."),
                        GuideCell.words("The first boot of UNIX."))
                .row(GuideCell.words("It's Not Linux, Stop Asking (task)"),
                        GuideCell.words("Install FreeBSD and boot it."),
                        GuideCell.words("The first boot of FreeBSD."))
                .row(GuideCell.words("Bloat everywhere, but linux (task)"),
                        GuideCell.words("Install Ubuntu and boot it."),
                        GuideCell.words("The first boot of Ubuntu."))
                .row(GuideCell.words("The Universal Operating System (task)"),
                        GuideCell.words("Install Debian and boot it."),
                        GuideCell.words("The first boot of Debian."))
                .row(GuideCell.words("Freedom, Friends, Features, First (task)"),
                        GuideCell.words("Install Fedora and boot it."),
                        GuideCell.words("The first boot of Fedora."))
                .row(GuideCell.words("I Use Arch BTW (challenge)"),
                        GuideCell.words("Install Arch Linux by hand from the live medium and boot it."),
                        GuideCell.words("The first boot of Arch Linux, installed by hand from the live medium."))
                .row(GuideCell.words("Didn't Like It? Recompile! (challenge)"),
                        GuideCell.words("Build Gentoo from source and boot it."),
                        GuideCell.words("The first boot of Gentoo, built from source."))
                .row(GuideCell.words("Distro Hopper (challenge)"),
                        GuideCell.words("Install and boot all five distributions."),
                        GuideCell.words("One each of Ubuntu, Debian, Fedora, Arch Linux and Gentoo."))
                .row(GuideCell.words("Professional Larper (challenge)"),
                        GuideCell.words("Run screenfetch on every system it installs on."),
                        GuideCell.words("Run screenfetch on each of eleven systems: Ubuntu, Debian, Fedora, Arch Linux,"
                                + " Gentoo, FreeBSD, Frames 95, Frames XP, Frames 7, Frames 10 and Frames 11."))
                .row(GuideCell.words("Built from Ports (challenge)"),
                        GuideCell.words("Build a program from FreeBSD's ports tree and install it."),
                        GuideCell.words("A program built from the ports tree is installed."))
                .row(GuideCell.words("Clockwork (challenge)"),
                        GuideCell.words("Have cron run a job on UNIX on Vintage hardware."),
                        GuideCell.words("A scheduled cron job runs on a Vintage machine with UNIX installed; a job set"
                                + " to run once does not count."))
                .row(GuideCell.words("Press DEL to Enter Setup (task)"),
                        GuideCell.words("Open a computer's firmware setup."),
                        GuideCell.words("You open the Setup from a monitor."))
                .row(GuideCell.words("The Best of Both Worlds (goal)"),
                        GuideCell.words("Boot one of two systems sharing a disk from the boot menu."),
                        GuideCell.words("In the Boot Menu you pick one of two or more systems on the same disk."))
                .row(GuideCell.words("Format C: (secret)"),
                        GuideCell.words("Erase a disk that held an installed system."),
                        GuideCell.words("You erase a disk that held a system."))
                .row(GuideCell.words("RTFM (task)"),
                        GuideCell.words("Open a manual page."),
                        GuideCell.words("You open the manual page of a command: man on UNIX, FreeBSD and the Linux"
                                + " distributions."))
                .row(GuideCell.words("sudo make me a sandwich (task)"),
                        GuideCell.words("Install a package from the Mirror."),
                        GuideCell.words("A package is installed from the Mirror."))
                .row(GuideCell.words("Not Responding (task)"),
                        GuideCell.words("End a program from the Task Manager."),
                        GuideCell.words("You end a program from the Task Manager."))
                .row(GuideCell.words("Professional Procrastinator (challenge)"),
                        GuideCell.words("Win Minesweeper on the Expert board."),
                        GuideCell.words("You win Minesweeper on the Expert board."))
                .row(GuideCell.words("I'm In (task)"),
                        GuideCell.words("Take control of another computer through Remote Control."),
                        GuideCell.words("Remote Control opens a session on another machine."))
                .ifSomethingGoesWrong("A boot achievement did not appear after a reinstall.",
                        "The disk keeps the mark of its first welcome. Erase the disk, then install again, and the"
                                + " next boot counts. If the disk was not erased, the system was already welcomed.",
                        "Screenfetch on UNIX earns nothing.",
                        "UNIX has no screenfetch package, and it is not one of the systems Professional Larper counts.")
                .seeAlso("jsc:installing", "jsc:the_systems", "jsc:firmware", "jsc:help_on_a_computer")
                .register();
    }

    private static void achievementsNetworks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("achievements_networks", section).titled("Network and Operations achievements")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("The Networks & Operations tab follows the work of a network: a Mainframe on a network,"
                        + " computers joining it, Operations and IQL (the network's query language), autocrafting, and"
                        + " the trouble a network can get into. The table says what each one asks, and how the game"
                        + " checks it.")
                .table("The network and Operations achievements", "Achievement", "What it asks", "How it is earned")
                .row(GuideCell.words("Networks & Operations (root)"),
                        GuideCell.words("Bring a Mainframe onto a network."),
                        GuideCell.words("A Mainframe joins a network for the first time."))
                .row(GuideCell.words("Series of Tubes (task)"),
                        GuideCell.words("Join a computer to a Mainframe's network."),
                        GuideCell.words("A computer joins a network that has a Mainframe."))
                .row(GuideCell.words("SELECT * FROM chest (task)"),
                        GuideCell.words("Take items out of network storage with an Operation."),
                        GuideCell.words("A SELECT Operation completes."))
                .row(GuideCell.words("DROP DATABASE production_database (task)"),
                        GuideCell.words("Run an IQL statement."),
                        GuideCell.words("An IQL statement you type completes without error."))
                .row(GuideCell.words("Set It and Forget It (goal)"),
                        GuideCell.words("Have an IQL JOB run on its own."),
                        GuideCell.words("An IQL job runs on its own, and the Mainframe's operator is credited."))
                .row(GuideCell.words("Two Out of Three Ain't Bad (secret)"),
                        GuideCell.words("Have an Operation finish only partly done."),
                        GuideCell.words("An Operation ends PARTIAL, as the terminal shows it."))
                .row(GuideCell.words("Deadlock (secret)"),
                        GuideCell.words("Have an Operation give up waiting on a locked resource."),
                        GuideCell.words("An Operation ends RESOURCE LOCKED, as the terminal shows it."))
                .row(GuideCell.words("Recipe for Success (task)"),
                        GuideCell.words("Encode a crafting pattern."),
                        GuideCell.words("A Pattern Encoder encodes a pattern."))
                .row(GuideCell.words("The Factory Must Grow (task)"),
                        GuideCell.words("Finish an autocraft."),
                        GuideCell.words("An autocraft finishes."))
                .row(GuideCell.words("Machine Learning (goal)"),
                        GuideCell.words("Finish an autocraft through a machine."),
                        GuideCell.words("A machine finishes a step of an autocraft, or an autocraft runs one of its"
                                + " steps in a machine."))
                .row(GuideCell.words("Supply Chain Issues (challenge)"),
                        GuideCell.words("Finish an autocraft of ten stages or more."),
                        GuideCell.words("An autocraft with ten or more stages finishes."))
                .row(GuideCell.words("Just-in-Time (task)"),
                        GuideCell.words("Put an Import Bus and an Export Bus on one network."),
                        GuideCell.words("A bus cable part is placed on a network that already has the other kind of"
                                + " bus."))
                .row(GuideCell.words("Who Unplugged the Server? (secret)"),
                        GuideCell.words("Cut the last cable between a running computer and its Mainframe."),
                        GuideCell.words("A running computer loses its last route to a Mainframe."))
                .row(GuideCell.words("There Can Be Only One (secret)"),
                        GuideCell.words("Put two Mainframes on one network."),
                        GuideCell.words("A network conflict starts. The chat announces it too."))
                .row(GuideCell.words("Talking to the Neighbours (goal, with ComputerCraft only)"),
                        GuideCell.words("Hear a ComputerCraft computer through a Network Gateway."),
                        GuideCell.words("A Network Gateway receives a message from a ComputerCraft computer."))
                .paragraph("An Operation you asked for credits you. A scheduled Operation credits the operator of the"
                        + " Mainframe ([](jsc:mainframes)), and so does an IQL job that runs on its own.")
                .paragraph("Two Out of Three Ain't Bad and Deadlock name two of the ways an Operation can end:"
                        + " [the eight states of an Operation](jsc:operation_states) explains all of them.")
                .paragraph("Talking to the Neighbours appears only when the ComputerCraft mod is installed with J's"
                        + " Computers, and the achievement needs a [Network Gateway](jsc:network_gateway).")
                .ifSomethingGoesWrong("Machine Learning does not appear although autocrafts finish.",
                        "It needs a machine to run a step of the craft. The Factory Must Grow counts any autocraft.",
                        "Just-in-Time does not appear with one bus.",
                        "Place the other kind of bus on the same network: an Import Bus with an Export Bus.",
                        "Talking to the Neighbours is missing.",
                        "It needs ComputerCraft and a Network Gateway that hears one of its computers.")
                .seeAlso("jsc:what_a_network_is", "jsc:what_operations_are", "jsc:how_autocrafting_works")
                .register();
    }

    private static void achievementsSigma(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("achievements_sigma", section).titled("Sigma achievements").icon(ComputingModule.CD_ROM)
                .paragraph("Sigma and Sigma# are the two programming languages of the computers, and the Sigma tab"
                        + " rewards you for writing them, compiling them and running them. The Book of Sigma explains"
                        + " both languages from zero ([](jsc:sigma_two_languages)).")
                .paragraph("Most Sigma achievements of a program credit the operator of the machine it runs on. The"
                        + " compile and run commands credit the player who typed them.")
                .table("The Sigma achievements", "Achievement", "What it asks", "How it is earned")
                .row(GuideCell.words("Sigma (root)"),
                        GuideCell.words("Install the Σ# Compiler."),
                        GuideCell.words("The sgsc package is installed."))
                .row(GuideCell.words("Erm, What the Sigma? (task)"),
                        GuideCell.words("Run a Sigma program."),
                        GuideCell.words("A program starts at a terminal."))
                .row(GuideCell.words("Expected ';' but Found '}' (secret)"),
                        GuideCell.words("Fail to compile a program."),
                        GuideCell.words("A compile fails with errors, so the compiler produces nothing."))
                .row(GuideCell.words("Back in My Day (goal)"),
                        GuideCell.words("Compile a Σ program on MC-DOS."),
                        GuideCell.words("A Sigma program, not Sigma#, compiles on MC-DOS."))
                .row(GuideCell.words("Stack Overflow (secret)"),
                        GuideCell.words("Have a program halt 1,024 calls deep."),
                        GuideCell.words("A program's chain of calls goes more than 1,024 deep."))
                .row(GuideCell.words("Undefined Behaviour (secret)"),
                        GuideCell.words("Have a program halt dividing by zero."),
                        GuideCell.words("A program stops with a divide-by-zero or remainder-by-zero error."))
                .row(GuideCell.words("Now You Have Two Problems (task)"),
                        GuideCell.words("Start a second thread in a program."),
                        GuideCell.words("A program starts a thread."))
                .row(GuideCell.words("Look Ma, No Console (task)"),
                        GuideCell.words("Open a window from a program."),
                        GuideCell.words("A program opens a window."))
                .row(GuideCell.words("Stonks (task)"),
                        GuideCell.words("Have a program woken by a watch on network stock."),
                        GuideCell.words("A watch fires and wakes a program."))
                .row(GuideCell.words("Works on My Machine (task)"),
                        GuideCell.words("Run the same program on two computers."),
                        GuideCell.words("A program starts on a different machine from the one where you first ran"
                                + " it."))
                .row(GuideCell.words("Ship It (goal)"),
                        GuideCell.words("Pack a program with sgpack and publish it to the Mirror."),
                        GuideCell.words("A package is published to the Mirror."))
                .row(GuideCell.words("Automate the Boring Stuff (goal)"),
                        GuideCell.words("Have a program set an Operation going on the network."),
                        GuideCell.words("A program sets an Operation going."))
                .row(GuideCell.words("Five Nines (challenge)"),
                        GuideCell.words("Keep a program running for seven days without stopping."),
                        GuideCell.words("A program runs without stopping for 168,000 ticks."))
                .paragraph("Five Nines counts ticks, not seconds: 24,000 ticks make one in-game day, so seven days make"
                        + " 168,000 ticks. A tick is the game's basic unit of time, one twentieth of a second"
                        + " ([](jsc:units_and_ticks)).")
                .paragraph("Works on My Machine needs the game to remember where you ran a program: it keeps up to 64"
                        + " program names for each player.")
                .ifSomethingGoesWrong("The Expected ';' achievement appeared for an error you did not expect.",
                        "Any failed compile earns it, whichever error it is. Read the error line to find the real"
                                + " problem ([](jsc:sigma_reading_an_error)).",
                        "A program stops with a message about calls.",
                        "Calls went more than 1,024 deep: a method may be calling itself without end. Look for a method"
                                + " that calls itself ([](jsc:sigma_runtime_stops)).",
                        "A program stops with \"divided by zero\".",
                        "Check the divisor before you divide. A program that halts this way earns Undefined Behaviour.",
                        "Five Nines does not appear.",
                        "The program must run without a stop for 168,000 ticks. The game checks once every 1,200 ticks,"
                                + " so it can appear up to a minute later.")
                .seeAlso("jsc:sigma_compile", "jsc:sigma_run", "jsc:sigma_limits", "jsc:book_of_sigma")
                .register();
    }

    private static void theLibrary(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("the_library").titled("The library")
                .icon(ComputersBooks.FRAMES_RESOURCE_KIT).in(chapter).register();
        library(guide, section);
        framesResourceKit(guide, section);
        linuxDeskReference(guide, section);
        linuxByHand(guide, section);
        daemonHandbook(guide, section);
        learningIql(guide, section);
        bookOfSigma(guide, section);
    }

    private static void library(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("library", section).titled("The library").icon(ComputersGuide.MANUAL)
                .paragraph("A book is an item you can hold. The Guide to Operations is the first, and six more sit"
                        + " beside it: the Frames Resource Kit, The Linux Desk Reference, Linux by Hand, The Daemon"
                        + " Handbook, Learning IQL and The Book of Sigma. Each book opens its own manual when you use"
                        + " it, and each is written for one family of systems or one language.")
                .paragraph("A manual is one of these seven books, read as pages in the game. You turn its pages and"
                        + " find its chapters the same way you read this Guide.")
                .paragraph("You get the books from the creative tab J's Computers, among the programs, beside this"
                        + " Guide. J's Computers has no crafting recipes of its own, so the creative tab is where you"
                        + " take one, unless your modpack gave the books a recipe; the recipe viewer shows it if so.")
                .paragraph("When you open a computer's help, the system opens its own book first, and the other books"
                        + " follow in their order. The help of MC-NET opens the Guide to Operations.")
                .table("The first book of each system", "System", "Book it opens first", "Installed by")
                .row(GuideCell.words("MC-DOS"), GuideCell.words("The Frames Resource Kit"),
                        GuideCell.words("Any install"))
                .row(GuideCell.words("Frames 95, XP, 7, 10 and 11"), GuideCell.words("The Frames Resource Kit"),
                        GuideCell.words("Any install"))
                .row(GuideCell.words("Ubuntu, Debian and Fedora"), GuideCell.words("The Linux Desk Reference"),
                        GuideCell.words("A guided install"))
                .row(GuideCell.words("Arch Linux"), GuideCell.words("Linux by Hand"),
                        GuideCell.words("Installing by hand from the live medium"))
                .row(GuideCell.words("Gentoo"), GuideCell.words("Linux by Hand"),
                        GuideCell.words("Building from source"))
                .row(GuideCell.words("FreeBSD and UNIX"), GuideCell.words("The Daemon Handbook"),
                        GuideCell.words("Any install"))
                .row(GuideCell.words("MC-NET"), GuideCell.words("The Guide to Operations"),
                        GuideCell.words("Any install"))
                .paragraph("Every Linux install that is not guided opens Linux by Hand, which means Arch Linux and"
                        + " Gentoo.")
                .table("The six books", "Book", "What it covers")
                .row(GuideCell.words("Frames Resource Kit"),
                        GuideCell.words("MC-DOS and the Frames editions, from Frames 95 to Frames 11."))
                .row(GuideCell.words("The Linux Desk Reference"),
                        GuideCell.words("Fedora, Debian and Ubuntu, and the shell, desktops, editors and help they"
                                + " share with the other UNIX books."))
                .row(GuideCell.words("Linux by Hand"),
                        GuideCell.words("Installing Arch Linux and Gentoo by hand, one step at a time."))
                .row(GuideCell.words("The Daemon Handbook"),
                        GuideCell.words("FreeBSD and UNIX System V."))
                .row(GuideCell.words("Learning IQL"),
                        GuideCell.words("The network's query language, its engines and its tools."))
                .row(GuideCell.words("The Book of Sigma"),
                        GuideCell.words("Sigma and Sigma#, their library and their tools."))
                .ifSomethingGoesWrong("The help opened a book you did not expect.",
                        "The system and its install choose the first book, and you cannot change that. The other books"
                                + " still follow in their order.",
                        "You cannot find a book.",
                        "Open the creative tab J's Computers and look among the programs, beside the Guide.")
                .seeAlso("jsc:book_frames_resource_kit", "jsc:book_linux_desk_reference", "jsc:book_linux_by_hand",
                        "jsc:book_daemon_handbook", "jsc:book_learning_iql", "jsc:book_of_sigma",
                        "jsc:help_on_a_computer")
                .define("Manual", "One of the seven books of J's Computers, read as pages in the game.")
                .register();
    }

    private static void framesResourceKit(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_frames_resource_kit", section).titled("Frames Resource Kit")
                .icon(ComputersBooks.FRAMES_RESOURCE_KIT).covers(ComputersBooks.FRAMES_RESOURCE_KIT)
                .paragraph("The Frames Resource Kit is the reference for MC-DOS and the Frames editions, from Frames 95"
                        + " to Frames 11. It is written the way a 1990s technical kit was: thorough and exact, with"
                        + " tables of settings and commands. You keep it open while you work on a Midsoft system.")
                .paragraph("Its chapters are The Midsoft systems, MC-DOS, The desktop, The programs of Frames, Each"
                        + " edition, and The Command Prompt. If you have not chosen a system yet, start with"
                        + " [Which Frames goes on which machine](jsc:frames_which_machine).")
                .paragraph("The help of MC-DOS and of every Frames opens this book first.")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:frames_which_machine", "jsc:library")
                .register();
    }

    private static void linuxDeskReference(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_linux_desk_reference", section).titled("The Linux Desk Reference")
                .icon(ComputersBooks.LINUX_DESK_REFERENCE).covers(ComputersBooks.LINUX_DESK_REFERENCE)
                .paragraph("The Linux Desk Reference lies open beside the machine. It is written for the command line"
                        + " first: you read what to type, what the system answers, and what each step does. It covers"
                        + " Fedora, Debian and Ubuntu, the three distributions you install with a guide.")
                .paragraph("Its chapters are Fedora, Debian and Ubuntu, with the installs, the first boot, apt and dnf,"
                        + " and the four UNIX chapters it shares with the other books: The shell, The desktops,"
                        + " Editors at the terminal and Finding help.")
                .paragraph("Its help opens first for Ubuntu, Debian and Fedora, when you installed them with a guide."
                        + " Start with [Distributions](jsc:distributions).")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:distributions", "jsc:install_ubuntu", "jsc:library")
                .register();
    }

    private static void linuxByHand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_linux_by_hand", section).titled("Linux by Hand")
                .icon(ComputersBooks.LINUX_BY_HAND).covers(ComputersBooks.LINUX_BY_HAND)
                .paragraph("Linux by Hand is a lab notebook. It takes you through one step at a time: what to type,"
                        + " what the machine answers, and what each step does and why. It covers the two systems you"
                        + " install by hand, Arch Linux from the live medium and Gentoo from source.")
                .paragraph("Its chapters are Before you begin, The disks, Arch, Gentoo, When the reboot refuses,"
                        + " and the four UNIX chapters it shares with the other books.")
                .paragraph("Its help opens first for Arch Linux and for Gentoo. Start with"
                        + " [Installing by hand](jsc:installing_by_hand).")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:installing_by_hand", "jsc:library")
                .register();
    }

    private static void daemonHandbook(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_daemon_handbook", section).titled("The Daemon Handbook")
                .icon(ComputersBooks.DAEMON_HANDBOOK).covers(ComputersBooks.DAEMON_HANDBOOK)
                .paragraph("The Daemon Handbook is a hardback for FreeBSD and UNIX System V: calm, complete and"
                        + " precise. Read a step before you take it, and you know what it will do.")
                .paragraph("Its chapters are FreeBSD, with the install, the services at boot, the packages and the"
                        + " ports tree; UNIX System V, with its install and the packages on its medium; and the four"
                        + " UNIX chapters it shares with the other books.")
                .paragraph("Its help opens first on FreeBSD and on UNIX. Start with"
                        + " [FreeBSD on this computer](jsc:freebsd_overview).")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:freebsd_overview", "jsc:system_v_overview", "jsc:library")
                .register();
    }

    private static void learningIql(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_learning_iql", section).titled("Learning IQL")
                .icon(ComputersBooks.LEARNING_IQL).covers(ComputersBooks.LEARNING_IQL)
                .paragraph("Learning IQL teaches the network's query language by example: a query, its answer, and why"
                        + " it works. You ask the network questions in IQL, and the network answers them by running"
                        + " Operations. Start with [What IQL and Operations are](jsc:what_iql_is) if you are new.")
                .paragraph("Its chapters are Getting started, The language (every verb and clause, and every error),"
                        + " The engines (the three engines and how to switch them), The tools, and Recipes, which are"
                        + " ready queries.")
                .paragraph("No system's help opens this book first. You reach it from [The library](jsc:library),"
                        + " or you take it from the creative tab.")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:what_iql_is", "jsc:library")
                .register();
    }

    private static void bookOfSigma(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("book_of_sigma", section).titled("The Book of Sigma")
                .icon(ComputersBooks.BOOK_OF_SIGMA).covers(ComputersBooks.BOOK_OF_SIGMA)
                .paragraph("The Book of Sigma is the classic language book: a definition, then a program that shows"
                        + " it, and the programs are the star. It teaches Sigma and Sigma#, the two languages the"
                        + " computers are programmed in, with their library and their tools.")
                .paragraph("Its chapters are Getting started, The language, The library (every class and every"
                        + " member), Programs running, Errors, Sharing and Examples.")
                .paragraph("No system's help opens this book first. You reach it from [The library](jsc:library),"
                        + " or you take it from the creative tab.")
                .ifSomethingGoesWrong("You do not have this book.",
                        "Take it from the creative tab J's Computers, among the programs, beside the Guide.")
                .seeAlso("jsc:sigma_two_languages", "jsc:library")
                .register();
    }
}
