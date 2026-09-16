/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import java.util.List;

/**
 * One system's installer, as data: the pages it shows, in its order and its words.
 *
 * <p>Every installer here asks the same three or four things, because those are the things the game has to ask:
 * which disk, what the computer is called, and, where a Mirror answers, which desktop comes with it. What tells
 * them apart is how they ask. The oldest puts everything on one settings page and waits for a key; one lists
 * its questions and lets the player pick which to answer first; another starts in text and finishes behind its
 * own graphical phase, asking its one question while the copy carries on underneath.
 *
 * <p>The steps of the copy belong to the pages, so an installer that asks something halfway through does not
 * have to stop the work to do it: the copy runs to the end of the page it is on, and waits there for an answer.
 *
 * <p>Pure data with no drawing in it. What colour any of this is belongs to the screen.
 */
public enum InstallerStyle {

    /** Anything guided that has not been given a look of its own yet. */
    PLAIN,

    /** The setup of the first age: a welcome, a settings page, a box with a bar in it. */
    MC_DOS,

    /** A text check of the machine, then a grey wizard with a picture down its side. */
    FRAMES_95,

    /** A blue text phase that copies, then a graphical phase that asks while it works. */
    FRAMES_XP,

    /** Pale cards, a table of disks, one question to a page. */
    FRAMES_11,

    /** The server installer: a coloured header, a body of text, and Done at the foot. */
    UBUNTU,

    /** The text installer: a grey window with its title in a tab, over blue. */
    DEBIAN,

    /** The text hub: every answer in a numbered list, and a letter to begin. */
    FEDORA;

    /** The pages this installer shows, in order, with the steps of the copy spread among them. */
    public List<Stage> stages() {
        return switch (this) {
            case PLAIN -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, List.of(
                            "preparing the disk", "copying the system", "creating folders",
                            "registering the boot entry")),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case MC_DOS -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.SETTINGS, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, List.of("Copying files")),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case FRAMES_95 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.WIZARD, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.WIZARD, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.WIZARD, List.of("Copying files")),
                    new Stage(InstallerPage.DONE, InstallerChrome.WIZARD, List.of()));
            /*
             * The one installer that changes shape partway through. Its question sits on the graphical phase
             * with a step of its own, which is what the copy runs through while the player is answering it.
             */
            case FRAMES_XP -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, List.of("Copying files")),
                    new Stage(InstallerPage.NAME, InstallerChrome.SIDE_PANEL, List.of("Collecting information")),
                    new Stage(InstallerPage.COPY, InstallerChrome.SIDE_PANEL, List.of(
                            "Installing Frames", "Finalizing installation")),
                    new Stage(InstallerPage.DONE, InstallerChrome.SIDE_PANEL, List.of()));
            case FRAMES_11 -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.CARD, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.CARD, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.CARD, List.of(
                            "Copying files", "Creating the system folders", "Adding the built-in programs",
                            "Setting up the boot entry")),
                    new Stage(InstallerPage.DONE, InstallerChrome.CARD, List.of()));
            case UBUNTU -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, List.of(
                            "Preparing the disk", "Copying the system", "Writing the boot entry",
                            "Setting the host name")),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case DEBIAN -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.BOXED_TEXT, List.of(
                            "Unpacking the system", "Writing the boot entry", "Setting the host name")),
                    new Stage(InstallerPage.DONE, InstallerChrome.BOXED_TEXT, List.of()));
            case FEDORA -> List.of(
                    new Stage(InstallerPage.HUB, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, List.of(
                            "Setting up the installation environment", "Preparing the disk", "Copying the system",
                            "Writing the boot entry", "Setting the host name")),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
        };
    }

    /**
     * Whether the questions are gathered in a list the player picks from rather than walked one after another.
     *
     * <p>An installer of that kind goes back to its list after every answer, and only begins when nothing on
     * the list is still wanted.
     */
    public boolean gathersQuestions() {
        return this == FEDORA;
    }

    /** Whether this installer offers a desktop with the system when a Mirror answers. */
    public boolean offersDesktop() {
        for (final Stage stage : this.stages()) {
            if (stage.page() == InstallerPage.DESKTOP) {
                return true;
            }
        }
        return false;
    }

    /** The name across the top of the installer. */
    public String title(final String systemName) {
        return switch (this) {
            case UBUNTU, DEBIAN, FEDORA -> systemName + " installer";
            default -> systemName + " Setup";
        };
    }

    /** What the page says it is, in this installer's own words. */
    public String heading(final InstallerPage page, final String systemName) {
        return switch (this) {
            case PLAIN -> switch (page) {
                case WELCOME -> "Install " + systemName;
                case COPY -> "Installing " + systemName;
                default -> "Installation complete";
            };
            case MC_DOS -> switch (page) {
                case WELCOME -> "Welcome to Setup.";
                case SETTINGS -> "Setup will use the following settings:";
                case COPY -> "Setup is copying " + systemName + " to the disk.";
                default -> systemName + " Setup is complete.";
            };
            case FRAMES_95 -> switch (page) {
                case WELCOME -> "Setup is checking this computer before it starts.";
                case DISK -> "Choose a Disk";
                case NAME -> "Computer Name";
                case COPY -> "Copying Files";
                default -> "Setup is finished.";
            };
            case FRAMES_XP -> switch (page) {
                case DISK -> "The following list shows the disks on this computer.";
                case NAME -> "Computer Name";
                case COPY -> "Please wait while Setup copies files to the disk.";
                default -> "Setup is finished.";
            };
            case FRAMES_11 -> switch (page) {
                case DISK -> "Where do you want to install " + systemName + "?";
                case NAME -> "Name this PC";
                case COPY -> "Installing " + systemName;
                default -> "Setup is finished.";
            };
            case UBUNTU -> switch (page) {
                case DISK -> "Guided storage configuration";
                case NAME -> "Profile setup";
                case DESKTOP -> "Desktop environment";
                case COPY -> "Installing system";
                default -> "Installation complete";
            };
            case DEBIAN -> switch (page) {
                case DISK -> "Partition disks";
                case NAME -> "Configure the network";
                case DESKTOP -> "Software selection";
                case COPY -> "Installing the base system";
                default -> "Finish the installation";
            };
            case FEDORA -> switch (page) {
                case HUB -> "Installation";
                case DISK -> "Installation Destination";
                case DESKTOP -> "Software selection";
                case NAME -> "Host name";
                case COPY -> "Progress";
                default -> "Complete!";
            };
        };
    }

    /** The line of keys along the foot of the page, empty where the installer draws buttons instead. */
    public String hint(final InstallerPage page) {
        return switch (this) {
            case MC_DOS -> switch (page) {
                case WELCOME, SETTINGS -> "ENTER=Continue  F3=Exit";
                case DONE -> "ENTER=Restart";
                default -> "";
            };
            case FRAMES_95 -> page == InstallerPage.WELCOME ? "ENTER=Continue  ESC=Exit" : "";
            case FRAMES_XP -> switch (page) {
                case DISK -> "ENTER=Install  E=Erase disk  F3=Quit";
                case DONE -> "ENTER=Restart";
                default -> "";
            };
            case UBUNTU -> page == InstallerPage.COPY || page == InstallerPage.DONE
                    ? "[ Reboot Now ]" : "[ Done ]  [ Back ]";
            case DEBIAN -> "<Tab> moves; <Space> selects; <Enter> activates buttons";
            case FEDORA -> page == InstallerPage.HUB
                    ? "Please make a selection from the above ['b' to begin installation, 'q' to quit, "
                            + "'r' to refresh]:"
                    : "Please make a selection from the above ['c' to continue, 'q' to quit, 'r' to refresh]:";
            default -> "";
        };
    }

    /**
     * One page of an installer, with the share of the copy that runs while it is up.
     *
     * @param page   what the page is for
     * @param chrome the shape it is drawn in
     * @param steps  the steps of the copy that belong to this page, in order; empty on a page that only asks
     */
    public record Stage(InstallerPage page, InstallerChrome chrome, List<String> steps) {

        public Stage {
            steps = List.copyOf(steps);
        }

        /** Whether this page waits for the player, rather than moving on by itself when its steps are done. */
        public boolean asks() {
            return this.page != InstallerPage.COPY && this.page != InstallerPage.DONE;
        }
    }
}
