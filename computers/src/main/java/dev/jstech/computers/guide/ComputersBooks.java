/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideStyle.CoverPart;
import dev.jstech.core.guide.GuideStyle.PartKind;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import java.util.ArrayList;
import java.util.List;

/**
 * The books of the systems and the languages, beside the Guide to Operations: the Frames Resource Kit, the Linux Desk
 * Reference, Linux by Hand, the Daemon Handbook, Learning IQL and the Book of Sigma. Each is dressed as the manuals of
 * its systems' time were, bound, papered and covered its own way, and each is an item a player can hold. The three
 * books of the UNIX-like systems share their chapters on the shell, the desktops, the editors and finding help.
 */
public final class ComputersBooks {

    /** A closed book's board, in the screen's pixels: as tall as the open book, its spine at the left edge. */
    private static final int BOARD_WIDTH = 180;
    private static final int BOARD_HEIGHT = 218;
    private static final String WHITE = "#FFFFFFFF";
    private static final String GOLD = "#FFD8B35A";

    public static final ItemEntry<ManualItem> FRAMES_RESOURCE_KIT = book("frames_resource_kit",
            ComputersGuide.FRAMES_RESOURCE_KIT).named("Frames Resource Kit").register();
    public static final ItemEntry<ManualItem> LINUX_DESK_REFERENCE = book("linux_desk_reference",
            ComputersGuide.LINUX_DESK_REFERENCE).named("The Linux Desk Reference").register();
    public static final ItemEntry<ManualItem> LINUX_BY_HAND = book("linux_by_hand", ComputersGuide.LINUX_BY_HAND)
            .named("Linux by Hand").register();
    public static final ItemEntry<ManualItem> DAEMON_HANDBOOK = book("daemon_handbook",
            ComputersGuide.DAEMON_HANDBOOK).named("The Daemon Handbook").register();
    public static final ItemEntry<ManualItem> LEARNING_IQL = book("learning_iql", ComputersGuide.LEARNING_IQL)
            .named("Learning IQL").register();
    public static final ItemEntry<ManualItem> BOOK_OF_SIGMA = book("book_of_sigma", ComputersGuide.BOOK_OF_SIGMA)
            .named("The Book of Sigma").register();

    private ComputersBooks() {
    }

    /** Declares the six books, their styles and their chapters. */
    static void declare(final ModGuide guide) {
        final List<GuideChapter> unix = unixChapters(guide);
        framesResourceKit(guide);
        linuxDeskReference(guide, unix);
        linuxByHand(guide, unix);
        daemonHandbook(guide, unix);
        learningIql(guide);
        bookOfSigma(guide);
    }

    /** A book's item, which opens that manual, among the programs of J's Computers' own tab beside the Guide. */
    private static ItemBuilder<ManualItem> book(final String path, final String manual) {
        return ComputingContent.CONTENT.item(path, properties -> new ManualItem(properties, manual))
                .tab(ComputingContent.shelf(ComputingContent.Shelf.PROGRAMS, null));
    }

    /** The chapters the books of FreeBSD, System V and the Linux distributions all hold. */
    private static List<GuideChapter> unixChapters(final ModGuide guide) {
        return List.of(
                guide.chapter("shell").titled("The shell").ownBooksOnly()
                        .about("The prompt and the tree of folders, the commands that work with files, text, the"
                                + " machine, its processes and its devices, and how commands are joined together.")
                        .register(),
                guide.chapter("unix_desktops").titled("The desktops").ownBooksOnly()
                        .about("Installing a desktop, and what is on each of them: KDE Plasma, GNOME, Cinnamon and"
                                + " CDE.")
                        .register(),
                guide.chapter("terminal_editors").titled("Editors at the terminal").ownBooksOnly()
                        .about("Writing a file without leaving the prompt: vim, emacs, vi and ee, and the keys of"
                                + " each.")
                        .register(),
                guide.chapter("finding_help").titled("Finding help").ownBooksOnly()
                        .about("Where to look when you forget how a command goes: help, man, whatis, apropos, info"
                                + " and listcmd.")
                        .register());
    }

    private static void framesResourceKit(final ModGuide guide) {
        final List<CoverPart> parts = board("#FFF4F4EF", WHITE, "#FFC9C9C0");
        parts.add(CoverPart.fill(0, 0, 12, BOARD_HEIGHT, GuideStyle.COVER));
        parts.add(CoverPart.fill(11, 0, 1, BOARD_HEIGHT, GuideStyle.SPINE));
        final String[] marks = {"frames_95", "frames_xp", "frames_7", "frames_10", "frames_11"};
        for (int i = 0; i < marks.length; i++) {
            parts.add(CoverPart.image(26 + i * 30, 26, 20, "jsc:gui/splash/" + marks[i] + "_mark"));
        }
        parts.add(CoverPart.fill(22, 52, 150, 1, GuideStyle.RULE));
        parts.add(CoverPart.words(PartKind.BOLD, 22, 64, 154, 1, 2.5F, GuideStyle.HEADING));
        parts.add(CoverPart.words(PartKind.BOLD, 22, 92, 154, 2, 1.5F, GuideStyle.INK));
        parts.add(CoverPart.fill(22, 112, 150, 1, GuideStyle.RULE));
        parts.add(CoverPart.words(PartKind.SMALL, 22, 118, 154, 3, 1, GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.SMALL, 22, 126, 154, 4, 1, GuideStyle.FAINT));
        // The band of a technical book of the 1990s, low across the cover.
        parts.add(CoverPart.fill(12, 160, 168, 26, GuideStyle.COVER));
        parts.add(CoverPart.fill(12, 160, 168, 2, GuideStyle.BAND));
        parts.add(CoverPart.words(PartKind.SMALL, 22, 168, 154, 5, 1, WHITE));
        parts.add(CoverPart.words(PartKind.SMALL, 22, 176, 154, 6, 1, WHITE));
        parts.add(CoverPart.image(22, 196, 9, "jsc:gui/emblem/frames_xp"));
        parts.add(CoverPart.words(PartKind.SMALL_BOLD, 34, 197, 142, 7, 1, GuideStyle.INK));
        guide.style("frames_resource_kit", style("jsc:guide/frames_resource_kit", thumbs(GuideStyle.Binding.GLUED,
                GuideStyle.Board.PLAIN, false), parts));
        guide.manual("frames_resource_kit").titled("Frames Resource Kit")
                .cover("Frames", "Resource Kit", "Frames 95, XP, 7, 10 and 11,", "and MC-DOS",
                        "The complete reference for every", "Midsoft system", "Midsoft Press")
                .style(ComputersGuide.FRAMES_RESOURCE_KIT).priority(50)
                .chapters(guide.chapter("midsoft_systems").titled("The Midsoft systems").ownBooksOnly()
                                .about("Which Frames goes on which machine, how each one is installed, and what"
                                        + " happens when two share a disk.")
                                .register(),
                        guide.chapter("mc_dos").titled("MC-DOS").ownBooksOnly()
                                .about("The first of Midsoft's systems: its prompt, its drives and paths, installing"
                                        + " it, and the programs that came with it.")
                                .register(),
                        guide.chapter("frames_desktop").titled("The desktop").ownBooksOnly()
                                .about("The Start menu, the taskbar and the windows of every edition, the settings"
                                        + " of each, and how to shut it down.")
                                .register(),
                        guide.chapter("frames_programs").titled("The programs of Frames").ownBooksOnly()
                                .about("The programs, one by one: This PC, the File Explorer, the Editor, the Device"
                                        + " Manager, the Task Manager, Help, the System Monitor and Virtual Studio.")
                                .register(),
                        guide.chapter("frames_editions").titled("Each edition").ownBooksOnly()
                                .about("What sets each edition apart, from Frames 95 to Frames 11.")
                                .register(),
                        guide.chapter("command_prompt").titled("The Command Prompt").ownBooksOnly()
                                .about("The prompt of MC-DOS and of every Frames: how a command is typed, and a page"
                                        + " for each command.")
                                .register())
                .about("This kit is the reference for Midsoft's systems: MC-DOS and every edition of Frames, from 95"
                                + " to 11. It tells you which one goes on which machine, how to install it, and"
                                + " everything on its desktop and at its prompt.",
                        "Each command has a page of its own, the same page HELP and /? show at the prompt.",
                        "The magnifier on the top edge finds anything by name, and the blocks down the edge of the"
                                + " right page each open a chapter.")
                .register();
    }

    private static void linuxDeskReference(final ModGuide guide, final List<GuideChapter> unix) {
        final List<CoverPart> parts = board("#FF2B2F36", "#FF55595E", "#FF16181C");
        // The wire coil down the spine, each loop through a hole in the board.
        parts.add(CoverPart.grain(PartKind.WIRE, -3, 0, 8, BOARD_HEIGHT, "#FF9AA0AA"));
        parts.add(CoverPart.grain(PartKind.HOLES, 6, 0, 2, BOARD_HEIGHT, "#FF121418"));
        bevel(parts, 28, 30, 132, 70, "#FF101317", "#FF3D434D", "#FF000000");
        parts.add(CoverPart.words(PartKind.CODE, 42, 46, 110, 1, 4, "#FF5FE07A"));
        parts.add(CoverPart.words(PartKind.BOLD, 28, 116, 148, 2, 1.5F, WHITE));
        parts.add(CoverPart.words(PartKind.BOLD, 28, 134, 148, 3, 1.5F, WHITE));
        parts.add(CoverPart.fill(28, 156, 132, 1, "#FF5A616B"));
        parts.add(CoverPart.words(PartKind.SMALL, 28, 162, 148, 4, 1, "#FFCDD6E2"));
        parts.add(CoverPart.words(PartKind.SMALL, 28, 196, 148, 5, 1, "#FF8A929C"));
        guide.style("linux_desk_reference", style("jsc:guide/linux_desk_reference", thumbs(GuideStyle.Binding.WIRE,
                GuideStyle.Board.PLAIN, false), parts));
        final List<GuideChapter> chapters = new ArrayList<>();
        chapters.add(guide.chapter("fedora_debian_ubuntu").titled("Fedora, Debian and Ubuntu").ownBooksOnly()
                .about("What a distribution is and which to choose, how each one installs, its first boot, and the"
                        + " package tools that fetch its programs: apt and dnf.")
                .register());
        chapters.addAll(unix);
        guide.manual("linux_desk_reference").titled("The Linux Desk Reference")
                .cover("$ _", "The Linux", "Desk Reference", "Fedora  -  Debian  -  Ubuntu",
                        "Lies flat beside the machine")
                .style(ComputersGuide.LINUX_DESK_REFERENCE).priority(50)
                .chapters(chapters.toArray(GuideChapter[]::new))
                .about("This reference lies open beside the machine for anyone running Fedora, Debian or Ubuntu:"
                                + " how to install them, how to work in the shell, the desktops and the editors, and"
                                + " where to look up a command.",
                        "Each command has a page of its own, the same page man and help show at the prompt.",
                        "The magnifier on the top edge finds anything by name, and the blocks down the edge of the"
                                + " right page each open a chapter.")
                .register();
    }

    private static void linuxByHand(final ModGuide guide, final List<GuideChapter> unix) {
        final List<CoverPart> parts = board("#FFB48A5A", "#FFC9A272", "#FF7D5A34");
        parts.add(CoverPart.grain(PartKind.KRAFT, 1, 1, BOARD_WIDTH - 2, BOARD_HEIGHT - 2, "#FFB48A5A"));
        parts.add(CoverPart.grain(PartKind.WIRE, -5, 0, 10, BOARD_HEIGHT, "#FF2A2A2A"));
        // The label pasted on the kraft, its lines left for the owner to fill in.
        bevel(parts, 32, 40, 124, 84, "#FFFBF8EE", WHITE, "#FFCFC7B2");
        parts.add(CoverPart.fill(32, 40, 124, 6, "#FFC94A4A"));
        parts.add(CoverPart.words(PartKind.BOLD, 40, 54, 112, 1, 1.5F, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.BOLD, 40, 70, 112, 2, 1.5F, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.SMALL, 40, 90, 112, 3, 1, GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.SMALL, 40, 99, 112, 4, 1, GuideStyle.FAINT));
        parts.add(CoverPart.fill(40, 106, 108, 1, "#FFB8C6D8"));
        parts.add(CoverPart.words(PartKind.SMALL, 40, 109, 112, 5, 1, GuideStyle.FAINT));
        parts.add(CoverPart.fill(40, 116, 108, 1, "#FFB8C6D8"));
        parts.add(CoverPart.stamp(79, 146, 92, 20, 6, -5, "#FFB3261E"));
        // A lab notebook: squared paper, and a paper flag of another colour stuck on for each chapter.
        final GuideStyle.Make make = new GuideStyle.Make(GuideStyle.Binding.WIRE, GuideStyle.Board.KRAFT,
                GuideStyle.Paper.GRAPH, GuideStyle.Marks.FLAGS,
                List.of("#FFF6E27A", "#FFF2A6C0", "#FF9FDCAE", "#FF9CC8F0", "#FFF6C27A"), false);
        guide.style("linux_by_hand", style("jsc:guide/linux_by_hand", make, parts));
        final List<GuideChapter> chapters = new ArrayList<>();
        chapters.add(guide.chapter("before_you_begin").titled("Before you begin").ownBooksOnly()
                .about("What installing by hand means, what you need first, and how the install medium helps you"
                        + " along the way.")
                .register());
        chapters.add(guide.chapter("preparing_disks").titled("The disks").ownBooksOnly()
                .about("Finding the disk, dividing it into partitions, giving each one a file system, and mounting"
                        + " it.")
                .register());
        chapters.add(guide.chapter("arch").titled("Arch").ownBooksOnly()
                .about("From a mounted disk to an Arch Linux that boots, one command at a time, and what each one"
                        + " does.")
                .register());
        chapters.add(guide.chapter("gentoo").titled("Gentoo").ownBooksOnly()
                .about("From a mounted disk to a Gentoo that boots: the stage, the settings, the kernel and the boot"
                        + " loader, and then building programs from their code.")
                .register());
        chapters.add(guide.chapter("when_reboot_refuses").titled("When the reboot refuses").ownBooksOnly()
                .about("Each reason a new system can refuse to start, and how to put it right.")
                .register());
        chapters.addAll(unix);
        guide.manual("linux_by_hand").titled("Linux by Hand")
                .cover("Linux", "by Hand", "Gentoo  -  Arch", "Machine:", "Started:", "INSTALL LOG")
                .style(ComputersGuide.LINUX_BY_HAND).priority(50)
                .chapters(chapters.toArray(GuideChapter[]::new))
                .about("This notebook takes you through installing Gentoo and Arch Linux by hand, the way they are"
                                + " really installed: you type every step, and the book tells you what each one"
                                + " does.",
                        "Its last chapters are the shell, the desktops, the editors and finding help, the same as in"
                                + " the other books of these systems.",
                        "A paper flag at the edge of the pages opens each chapter.")
                .register();
    }

    private static void daemonHandbook(final ModGuide guide, final List<GuideChapter> unix) {
        final List<CoverPart> parts = board("#FF7A1818", "#FF954646", "#FF4A0C0C");
        parts.add(CoverPart.grain(PartKind.CLOTH, 1, 0, BOARD_WIDTH - 2, BOARD_HEIGHT, GuideStyle.COVER));
        parts.add(CoverPart.frame(14, 14, 152, 190, GOLD));
        parts.add(CoverPart.frame(17, 17, 146, 184, "#FFA96639"));
        parts.add(CoverPart.image(72, 38, 36, "jsc:gui/splash/freebsd_orb"));
        parts.add(CoverPart.words(PartKind.CENTRED, 20, 90, 140, 1, 1.5F, GOLD));
        parts.add(CoverPart.words(PartKind.CENTRED, 20, 108, 140, 2, 1.5F, GOLD));
        parts.add(CoverPart.fill(48, 128, 84, 1, GOLD));
        parts.add(CoverPart.words(PartKind.CENTRED_SMALL, 20, 134, 140, 3, 1, "#FFF0D48A"));
        parts.add(CoverPart.words(PartKind.CENTRED_SMALL, 20, 184, 140, 4, 1, GOLD));
        // The ribbon's tail, out of the bottom edge.
        parts.add(CoverPart.fill(142, BOARD_HEIGHT - 6, 5, 6, GuideStyle.RIBBON));
        guide.style("daemon_handbook", style("jsc:guide/daemon_handbook", thumbs(GuideStyle.Binding.SEWN,
                GuideStyle.Board.CLOTH, true), parts));
        final List<GuideChapter> chapters = new ArrayList<>();
        chapters.add(guide.chapter("freebsd").titled("FreeBSD").ownBooksOnly()
                .about("Installing FreeBSD and starting it, the switches of its services, and its two ways to a"
                        + " program: a package built for you, or the ports tree that builds it on your machine.")
                .register());
        chapters.add(guide.chapter("system_v").titled("UNIX System V").ownBooksOnly()
                .about("The UNIX of the first era: installing it, adding its packages from the medium, and working"
                        + " in it.")
                .register());
        chapters.addAll(unix);
        guide.manual("daemon_handbook").titled("The Daemon Handbook")
                .cover("The Daemon", "Handbook", "FreeBSD and UNIX System V", "Daemon Foundation")
                .style(ComputersGuide.DAEMON_HANDBOOK).priority(50)
                .chapters(chapters.toArray(GuideChapter[]::new))
                .about("This handbook covers the two UNIX systems, FreeBSD and UNIX System V: how each one is"
                                + " installed and started, how programs are added, and how to work at its prompt.",
                        "The ribbon keeps the pages you were reading when you closed the book.",
                        "The magnifier on the top edge finds anything by name, and the blocks down the edge of the"
                                + " right page each open a chapter.")
                .register();
    }

    private static void learningIql(final ModGuide guide) {
        final List<CoverPart> parts = board(WHITE, WHITE, "#FFCFC8DA");
        parts.add(CoverPart.fill(0, 0, BOARD_WIDTH, 140, GuideStyle.COVER));
        // A result set, one of its rows picked.
        final int[] lefts = {20, 80, 116};
        final int[] widths = {60, 36, 36};
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < lefts.length; column++) {
                parts.add(CoverPart.frame(lefts[column], 16 + row * 12, widths[column], 13, "#FF82719C"));
                if (row == 3) {
                    parts.add(CoverPart.fill(lefts[column] + 1, 17 + row * 12, widths[column] - 2, 11,
                            "#FFF0B23A"));
                }
            }
        }
        parts.add(CoverPart.words(PartKind.BOLD, 20, 96, 156, 1, 1.5F, WHITE));
        parts.add(CoverPart.words(PartKind.BOLD, 20, 114, 156, 2, 2, WHITE));
        parts.add(CoverPart.words(PartKind.SMALL, 20, 148, 156, 3, 1, GuideStyle.INK));
        parts.add(CoverPart.words(PartKind.SMALL, 20, 156, 156, 4, 1, GuideStyle.INK));
        final String[] engines = {"iqlengine", "nextgreiql", "youriql"};
        for (int i = 0; i < engines.length; i++) {
            parts.add(CoverPart.image(20, 168 + i * 14, 12, "jsc:gui/program/" + engines[i] + "/frames_xp"));
            parts.add(CoverPart.words(PartKind.SMALL, 36, 171 + i * 14, 140, 5 + i, 1, GuideStyle.FAINT));
        }
        guide.style("learning_iql", style("jsc:guide/learning_iql", thumbs(GuideStyle.Binding.GLUED,
                GuideStyle.Board.PLAIN, false), parts));
        guide.manual("learning_iql").titled("Learning IQL")
                .cover("Learning", "IQL", "Queries, engines and the tools", "that ask the network",
                        "Midsoft IQL Server", "NextgreIQL", "Prophet YourIQL")
                .style(ComputersGuide.LEARNING_IQL).priority(50)
                .chapters(guide.chapter("iql_getting_started").titled("Getting started").ownBooksOnly()
                                .about("What IQL and Operations are, where a query is typed, and your first queries,"
                                        + " step by step.")
                                .register(),
                        guide.chapter("iql_language").titled("The language").ownBooksOnly()
                                .about("Every verb and clause of the language, what each one does, and every error"
                                        + " it can give.")
                                .register(),
                        guide.chapter("iql_engines").titled("The engines").ownBooksOnly()
                                .about("The three engines that answer queries, how to start and switch them, and what"
                                        + " each one adds to the language.")
                                .register(),
                        guide.chapter("iql_tools").titled("The tools").ownBooksOnly()
                                .about("The programs for writing and watching queries: ISMS, the Profiler, Nextgre"
                                        + " Planner Studio and the Prophet Reactive Console.")
                                .register(),
                        guide.chapter("iql_recipes").titled("Recipes").ownBooksOnly()
                                .about("Ready queries for what a network is asked to do most.")
                                .register())
                .about("IQL is the language you ask a network questions in and tell it what to do. This book teaches"
                                + " it from the first query to the last clause, with the engines that run it and the"
                                + " tools that write it.",
                        "The magnifier on the top edge finds anything by name, and the blocks down the edge of the"
                                + " right page each open a chapter.")
                .register();
    }

    private static void bookOfSigma(final ModGuide guide) {
        final List<CoverPart> parts = board("#FFFBFBF8", WHITE, "#FFD3D3CC");
        parts.add(CoverPart.words(PartKind.SMALL_BOLD, 20, 22, 156, 1, 1, GuideStyle.FAINT));
        parts.add(CoverPart.image(20, 40, 46, 58, "jsc:gui/guide/sigma"));
        parts.add(CoverPart.words(PartKind.BOLD, 76, 82, 100, 2, 2, GuideStyle.INK));
        parts.add(CoverPart.fill(20, 112, 140, 1, GuideStyle.RULE));
        parts.add(CoverPart.words(PartKind.SMALL, 20, 118, 156, 3, 1, GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.SMALL, 20, 126, 156, 4, 1, GuideStyle.FAINT));
        parts.add(CoverPart.words(PartKind.SMALL, 20, 170, 156, 5, 1, GuideStyle.FAINT));
        parts.add(CoverPart.image(20, 186, 12, "jsc:gui/program/sgsc/frames_xp"));
        parts.add(CoverPart.words(PartKind.SMALL_BOLD, 36, 189, 140, 6, 1, GuideStyle.INK));
        guide.style("book_of_sigma", style("jsc:guide/book_of_sigma", thumbs(GuideStyle.Binding.GLUED,
                GuideStyle.Board.PLAIN, false), parts));
        guide.manual("book_of_sigma").titled("The Book of Sigma")
                .cover("THE BOOK OF", "Sigma", "Sigma and Sigma#: the languages,", "their library and their tools",
                        "Second Edition, for Sigma# 2", "Sigma Foundation")
                .style(ComputersGuide.BOOK_OF_SIGMA).priority(50)
                .chapters(guide.chapter("sigma_getting_started").titled("Getting started").ownBooksOnly()
                                .about("The two languages and which to choose, installing their tools, and your first"
                                        + " program, from typing it to running it.")
                                .register(),
                        guide.chapter("sigma_language").titled("The language").ownBooksOnly()
                                .about("Everything a program is written with, from comments and types to classes,"
                                        + " interfaces and lambdas.")
                                .register(),
                        guide.chapter("sigma_library").titled("The library").ownBooksOnly()
                                .about("Every class the library gives a program, member by member: the console, files,"
                                        + " the machine, the network, windows and the rest.")
                                .register(),
                        guide.chapter("sigma_running").titled("Programs running").ownBooksOnly()
                                .about("How a program runs on a machine: its processes and threads, what each call"
                                        + " costs, its limits, and how it is saved with the world.")
                                .register(),
                        guide.chapter("sigma_errors").titled("Errors").ownBooksOnly()
                                .about("How to read an error, and every error a program can meet while it is built or"
                                        + " while it runs.")
                                .register(),
                        guide.chapter("sigma_sharing").titled("Sharing").ownBooksOnly()
                                .about("Packing a program for others, and keeping its versions with Knot.")
                                .register(),
                        guide.chapter("sigma_examples").titled("Examples").ownBooksOnly()
                                .about("A working program for each part of the library, ready to build and run.")
                                .register())
                .about("Sigma and Sigma# are the languages the computers here are programmed in. This book teaches"
                                + " both, from the first program to the whole library, with the tools that build"
                                + " them.",
                        "The magnifier on the top edge finds anything by name, and the blocks down the edge of the"
                                + " right page each open a chapter.")
                .register();
    }

    /** A book's style: its pages laid out as a binder's, and the bar the Guide to Operations fills while it opens. */
    private static GuideStyle style(final String palette, final GuideStyle.Make make, final List<CoverPart> parts) {
        // Held over an item, the manual key lights blue blocks one after another, as Frames XP did while it loaded.
        return CoreGuide.book(palette, make, parts).withHoldBar(GuideStyle.HoldBar.BLOCKS);
    }

    /** How a book with a printed thumb index is made, its blocks in its heading's colour and its band's by turns. */
    private static GuideStyle.Make thumbs(final GuideStyle.Binding binding, final GuideStyle.Board board,
                                          final boolean ribbon) {
        return new GuideStyle.Make(binding, board, GuideStyle.Paper.PLAIN, GuideStyle.Marks.THUMB,
                List.of(GuideStyle.HEADING, GuideStyle.BAND), ribbon);
    }

    /** The closed book's board: its colour, lit along its top and left edges and shaded along the others. */
    private static List<CoverPart> board(final String fill, final String light, final String dark) {
        final List<CoverPart> parts = new ArrayList<>();
        bevel(parts, 0, 0, BOARD_WIDTH, BOARD_HEIGHT, fill, light, dark);
        return parts;
    }

    /** A raised plate: a rectangle of a colour, lit along its top and left edges and shaded along the others. */
    private static void bevel(final List<CoverPart> parts, final int x, final int y, final int width,
                              final int height, final String fill, final String light, final String dark) {
        parts.add(CoverPart.fill(x, y, width, height, fill));
        parts.add(CoverPart.fill(x, y, width, 1, light));
        parts.add(CoverPart.fill(x, y, 1, height, light));
        parts.add(CoverPart.fill(x, y + height - 1, width, 1, dark));
        parts.add(CoverPart.fill(x + width - 1, y, 1, height, dark));
    }
}
