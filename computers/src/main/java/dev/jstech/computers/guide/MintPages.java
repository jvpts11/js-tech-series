/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The Linux Mint chapter of The Linux Desk Reference: the distribution that comes up at its desktop, its live session
 * and its guided installer, and its two programs of its own, the Software Manager and the Update Manager.
 */
final class MintPages {

    private MintPages() {
    }

    /** Declares the sections and entries of Linux Mint, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef system = guide.section("mint_system").titled("The system").in(chapter).register();
        overview(guide, system);
        install(guide, system);
        final ModGuide.SectionRef programs = guide.section("mint_programs").titled("Its programs").in(chapter)
                .register();
        softwareManager(guide, programs);
        updateManager(guide, programs);
    }

    private static void overview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mint_overview", section).titled("Linux Mint on this computer")
                .paragraph("Linux Mint is a distribution made by Spearmint Linux, and the one that comes up at its"
                        + " desktop: Cinnamon is part of it. It starts behind its own mark with five dots under it,"
                        + " and the Menu button on its panel wears the same mark.")
                .table("Linux Mint at a glance")
                .property("Minimum hardware era", "Standard")
                .amount("Disk space of the installed system", 12288, "MB")
                .amount("Memory while it runs", 64, "MB")
                .property("Kernel", "linux")
                .property("Desktop", "Cinnamon")
                .property("Package manager", "apt")
                .paragraph("Besides apt at the Terminal, you have two programs of Mint's own in its menu: the Software"
                        + " Manager, which installs programs, and the Update Manager, which keeps them up to date"
                        + " ([](jsc:mint_software_manager), [](jsc:mint_update_manager)).")
                .seeAlso("jsc:the_systems", "jsc:mint_install", "jsc:desktops")
                .register();
    }

    private static void install(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mint_install", section).titled("The live session and the installer")
                .paragraph("Linux Mint's medium starts a live session: the whole desktop, running off a disk in the"
                        + " computer's memory. You can try every program in it, and nothing you do is kept on the disk"
                        + " when the computer starts over.")
                .define("Live session", "The system running from its medium and a disk in memory, which writes"
                        + " nothing to the computer's disks.")
                .paragraph("Open Install Linux Mint on the live desktop to start the installer. It has five screens,"
                        + " and it asks no language, keyboard or time zone: the game already knows them.")
                .define("Install Linux Mint", "The live desktop's icon that opens the installer over the session.")
                .table("The installer's screens", "Screen", "What it asks")
                .row(GuideCell.words("Installation type"), GuideCell.words("To install beside the system on the"
                        + " disk, or to erase the disk and install Linux Mint alone."))
                .row(GuideCell.words("Beside another system"), GuideCell.words("Shown only beside one: how the disk"
                        + " is divided between the two."))
                .row(GuideCell.words("Who are you?"), GuideCell.words("The computer's name. The password waits for"
                        + " accounts."))
                .row(GuideCell.words("The copy"), GuideCell.words("Nothing: it shows what Linux Mint has while it"
                        + " copies."))
                .row(GuideCell.words("Installation Complete"), GuideCell.words("Continue Testing, or Restart Now."))
                .paragraph("Linux Mint goes on ext4, with a swap as big as the computer's memory beside it.")
                .ifSomethingGoesWrong("Install Linux Mint says the computer cannot take the system.",
                        "The computer is older than the Standard era, or no disk has room for 12,288 MB.")
                .seeAlso("jsc:installing", "jsc:mint_overview")
                .register();
    }

    private static void softwareManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mint_software_manager", section).titled("The Software Manager")
                .paragraph("The Software Manager is Linux Mint's store, on the network's Mirror. Its front page puts"
                        + " one program forward, a different one each day, lists the categories, and shows the newest"
                        + " programs on the Mirror. It has no ratings.")
                .table("The categories", "Category", "What is in it")
                .row(GuideCell.words("Accessories, Graphics, Internet, Office"), GuideCell.words("Mint's own"
                        + " categories, as their names say."))
                .row(GuideCell.words("Programming, Sound and video, System tools, Games"), GuideCell.words("The"
                        + " same."))
                .row(GuideCell.words("Network"), GuideCell.words("The programs that work the network, and its"
                        + " services."))
                .row(GuideCell.words("Desktops"), GuideCell.words("The desktops this computer can take."))
                .steps("Open the Software Manager from the menu.", "Choose a category, or type in Search and press"
                                + " Enter.",
                        "Open a program to see its version, its size and what it takes in memory.",
                        "Press Install. It installs with apt, as the Terminal would.")
                .paragraph("The three lines at the top right list what you have installed.")
                .seeAlso("jsc:mint_update_manager", "jsc:mint_overview")
                .register();
    }

    private static void updateManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mint_update_manager", section).titled("The Update Manager")
                .paragraph("The Update Manager lists every installed program the Mirror has a newer version of, each"
                        + " one ticked. While there is one, its shield is on the panel; press it to open the Update"
                        + " Manager.")
                .steps("Untick what you do not want, or press Clear and tick what you do.",
                        "Press Install Updates.")
                .paragraph("A program is installed at once. The desktop is installed the next time the desktop"
                        + " starts. Refresh asks the Mirror again.")
                .ifSomethingGoesWrong("The Update Manager says no Mirror answers.",
                        "The computer is not on a network whose Mainframe runs the Mirror.")
                .seeAlso("jsc:mint_software_manager")
                .register();
    }
}
