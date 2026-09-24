/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

import java.util.ArrayList;
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
 *
 * <p>Every word here is the installer speaking to the person installing, which is what real installers
 * translate, so it is read in that person's language; the names of systems and files stay as they are.
 */
@TextHolder
public enum InstallerStyle implements IStableName {

    /** Anything guided that has not been given a look of its own yet. */
    PLAIN("plain"),

    /** The setup of the first age: a welcome, a settings page, a box with a bar in it. */
    MC_DOS("mc_dos"),

    /**
     * The appliance's: five lines and it is ready.
     *
     * <p>It asks nothing, because there is nothing about this machine to ask. There is no tree to lay down
     * and no folder to choose, no name to type, and one disk to write to. Switching a network box on and
     * having it work was what it was sold for, so the install is over before a wizard would have finished
     * saying hello.
     */
    MC_NET("mc_net"),

    /** A text check of the machine, then a grey wizard with a picture down its side. */
    FRAMES_95("frames_95"),

    /** A blue text phase that copies, then a graphical phase that asks while it works. */
    FRAMES_XP("frames_xp"),

    /** Pale cards, a table of disks, one question to a page. */
    FRAMES_11("frames_11"),

    /** The server installer: a coloured header, a body of text, and Done at the foot. */
    UBUNTU("ubuntu"),

    /** The text installer: a grey window with its title in a tab, over blue. */
    DEBIAN("debian"),

    /** The text hub: every answer in a numbered list, and a letter to begin. */
    FEDORA("fedora");

    private static final TextKey TITLE_INSTALLER = TextKey.of("jsc.install.installer_style.title_installer",
            "%s installer");
    private static final TextKey TITLE_SETUP = TextKey.of("jsc.install.installer_style.title_setup", "%s Setup");

    private static final TextKey PLAIN_WELCOME = TextKey.of("jsc.install.installer_style.plain_welcome", "Install %s");
    private static final TextKey INSTALLING_SYSTEM =
            TextKey.of("jsc.install.installer_style.installing_system", "Installing %s");
    private static final TextKey INSTALLATION_COMPLETE =
            TextKey.of("jsc.install.installer_style.installation_complete", "Installation complete");
    private static final TextKey DOS_WELCOME = TextKey.of("jsc.install.installer_style.dos_welcome",
            "Welcome to Setup.");
    private static final TextKey DOS_SETTINGS = TextKey.of("jsc.install.installer_style.dos_settings",
            "Setup will use the following settings:");
    private static final TextKey DOS_COPY = TextKey.of("jsc.install.installer_style.dos_copy",
            "Setup is copying %s to the disk.");
    private static final TextKey DOS_DONE = TextKey.of("jsc.install.installer_style.dos_done",
            "%s Setup is complete.");
    private static final TextKey NET_WELCOME = TextKey.of("jsc.install.installer_style.net_welcome",
            "This will make this machine a %s server.");
    private static final TextKey NET_COPY = TextKey.of("jsc.install.installer_style.net_copy",
            "Setting up this server.");
    private static final TextKey NET_DONE = TextKey.of("jsc.install.installer_style.net_done",
            "This server is ready.");
    private static final TextKey FRAMES_95_WELCOME = TextKey.of("jsc.install.installer_style.frames_95_welcome",
            "Setup is checking this computer before it starts.");
    private static final TextKey FRAMES_95_DISK = TextKey.of("jsc.install.installer_style.frames_95_disk",
            "Choose a Disk");
    private static final TextKey COMPUTER_NAME = TextKey.of("jsc.install.installer_style.computer_name",
            "Computer Name");
    private static final TextKey FRAMES_95_COPY = TextKey.of("jsc.install.installer_style.frames_95_copy",
            "Copying Files");
    private static final TextKey SETUP_FINISHED = TextKey.of("jsc.install.installer_style.setup_finished",
            "Setup is finished.");
    private static final TextKey FRAMES_XP_DISK = TextKey.of("jsc.install.installer_style.frames_xp_disk",
            "The following list shows the disks on this computer.");
    private static final TextKey FRAMES_XP_COPY = TextKey.of("jsc.install.installer_style.frames_xp_copy",
            "Please wait while Setup copies files to the disk.");
    private static final TextKey FRAMES_11_DISK = TextKey.of("jsc.install.installer_style.frames_11_disk",
            "Where do you want to install %s?");
    private static final TextKey FRAMES_11_NAME = TextKey.of("jsc.install.installer_style.frames_11_name",
            "Name this PC");
    private static final TextKey UBUNTU_DISK = TextKey.of("jsc.install.installer_style.ubuntu_disk",
            "Guided storage configuration");
    private static final TextKey UBUNTU_NAME = TextKey.of("jsc.install.installer_style.ubuntu_name",
            "Profile setup");
    private static final TextKey UBUNTU_DESKTOP = TextKey.of("jsc.install.installer_style.ubuntu_desktop",
            "Desktop environment");
    private static final TextKey UBUNTU_COPY = TextKey.of("jsc.install.installer_style.ubuntu_copy",
            "Installing system");
    private static final TextKey DEBIAN_DISK = TextKey.of("jsc.install.installer_style.debian_disk",
            "Partition disks");
    private static final TextKey DEBIAN_NAME = TextKey.of("jsc.install.installer_style.debian_name",
            "Configure the network");
    private static final TextKey SOFTWARE_SELECTION = TextKey.of("jsc.install.installer_style.software_selection",
            "Software selection");
    private static final TextKey DEBIAN_COPY = TextKey.of("jsc.install.installer_style.debian_copy",
            "Installing the base system");
    private static final TextKey DEBIAN_DONE = TextKey.of("jsc.install.installer_style.debian_done",
            "Finish the installation");
    private static final TextKey FEDORA_HUB = TextKey.of("jsc.install.installer_style.fedora_hub", "Installation");
    private static final TextKey FEDORA_DISK = TextKey.of("jsc.install.installer_style.fedora_disk",
            "Installation Destination");
    private static final TextKey FEDORA_NAME = TextKey.of("jsc.install.installer_style.fedora_name", "Host name");
    private static final TextKey FEDORA_COPY = TextKey.of("jsc.install.installer_style.fedora_copy", "Progress");
    private static final TextKey FEDORA_DONE = TextKey.of("jsc.install.installer_style.fedora_done", "Complete!");

    private static final TextKey HINT_CONTINUE_EXIT = TextKey.of("jsc.install.installer_style.hint_continue_exit",
            "ENTER=Continue  F3=Exit");
    private static final TextKey HINT_RESTART = TextKey.of("jsc.install.installer_style.hint_restart",
            "ENTER=Restart");
    private static final TextKey HINT_XP_DISK = TextKey.of("jsc.install.installer_style.hint_xp_disk",
            "ENTER=Install  E=Erase disk  F3=Quit");
    private static final TextKey HINT_UBUNTU_REBOOT = TextKey.of("jsc.install.installer_style.hint_ubuntu_reboot",
            "[ Reboot Now ]");
    private static final TextKey HINT_UBUNTU_DONE_BACK =
            TextKey.of("jsc.install.installer_style.hint_ubuntu_done_back", "[ Done ]  [ Back ]");
    private static final TextKey HINT_DEBIAN = TextKey.of("jsc.install.installer_style.hint_debian",
            "<Tab> moves; <Space> selects; <Enter> activates buttons");
    private static final TextKey HINT_FEDORA_HUB = TextKey.of("jsc.install.installer_style.hint_fedora_hub",
            "Please make a selection from the above ['b' to begin installation, 'q' to quit, 'r' to refresh]:");
    private static final TextKey HINT_FEDORA_PAGE = TextKey.of("jsc.install.installer_style.hint_fedora_page",
            "Please make a selection from the above ['c' to continue, 'q' to quit, 'r' to refresh]:");

    private static final TextKey STEP_PLAIN_PREPARING =
            TextKey.of("jsc.install.installer_style.step_plain_preparing", "preparing the disk");
    private static final TextKey STEP_PLAIN_COPYING =
            TextKey.of("jsc.install.installer_style.step_plain_copying", "copying the system");
    private static final TextKey STEP_PLAIN_FOLDERS =
            TextKey.of("jsc.install.installer_style.step_plain_folders", "creating folders");
    private static final TextKey STEP_PLAIN_BOOT_ENTRY =
            TextKey.of("jsc.install.installer_style.step_plain_boot_entry", "registering the boot entry");
    private static final TextKey STEP_COPYING_FILES =
            TextKey.of("jsc.install.installer_style.step_copying_files", "Copying files");
    private static final TextKey STEP_NET_CHECKING =
            TextKey.of("jsc.install.installer_style.step_net_checking", "checking the disk");
    private static final TextKey STEP_NET_WRITING_SYSTEM =
            TextKey.of("jsc.install.installer_style.step_net_writing_system", "writing the system");
    private static final TextKey STEP_NET_WRITING_FILE =
            TextKey.of("jsc.install.installer_style.step_net_writing_file", "writing %s");
    private static final TextKey STEP_NET_INTERACTOR =
            TextKey.of("jsc.install.installer_style.step_net_interactor", "installing the interactor");
    private static final TextKey STEP_NET_NETWORK =
            TextKey.of("jsc.install.installer_style.step_net_network", "looking for a network");
    private static final TextKey STEP_COLLECTING =
            TextKey.of("jsc.install.installer_style.step_collecting", "Collecting information");
    private static final TextKey STEP_FINALIZING =
            TextKey.of("jsc.install.installer_style.step_finalizing", "Finalizing installation");
    private static final TextKey STEP_SYSTEM_FOLDERS =
            TextKey.of("jsc.install.installer_style.step_system_folders", "Creating the system folders");
    private static final TextKey STEP_BUILT_IN_PROGRAMS =
            TextKey.of("jsc.install.installer_style.step_built_in_programs", "Adding the built-in programs");
    private static final TextKey STEP_SETTING_UP_BOOT_ENTRY =
            TextKey.of("jsc.install.installer_style.step_setting_up_boot_entry", "Setting up the boot entry");
    private static final TextKey STEP_PREPARING_DISK =
            TextKey.of("jsc.install.installer_style.step_preparing_disk", "Preparing the disk");
    private static final TextKey STEP_COPYING_SYSTEM =
            TextKey.of("jsc.install.installer_style.step_copying_system", "Copying the system");
    private static final TextKey STEP_WRITING_BOOT_ENTRY =
            TextKey.of("jsc.install.installer_style.step_writing_boot_entry", "Writing the boot entry");
    private static final TextKey STEP_SETTING_HOST_NAME =
            TextKey.of("jsc.install.installer_style.step_setting_host_name", "Setting the host name");
    private static final TextKey STEP_UNPACKING =
            TextKey.of("jsc.install.installer_style.step_unpacking", "Unpacking the system");
    private static final TextKey STEP_INSTALLATION_ENVIRONMENT =
            TextKey.of("jsc.install.installer_style.step_installation_environment",
                    "Setting up the installation environment");

    private final String serializedName;

    InstallerStyle(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** The pages this installer shows, in order, with the steps of the copy spread among them. */
    public List<Stage> stages() {
        return switch (this) {
            case PLAIN -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(
                            STEP_PLAIN_PREPARING, STEP_PLAIN_COPYING, STEP_PLAIN_FOLDERS,
                            STEP_PLAIN_BOOT_ENTRY)),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case MC_DOS -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.SETTINGS, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(STEP_COPYING_FILES)),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            /*
             * One page that waits, and then the work. It asks nothing about the machine, which is the point,
             * but it does not write to somebody's disk before they have said go: that is the one thing every
             * installer here owes the player, and being quick is no reason to take it away.
             *
             * The steps are named for what this system actually lays down, the file that starts it and the
             * operating space among them, because those are the things a player later deletes or replaces.
             */
            case MC_NET -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(
                            STEP_NET_CHECKING, STEP_NET_WRITING_SYSTEM, STEP_NET_WRITING_FILE.with("netstart.sys"),
                            STEP_NET_INTERACTOR, STEP_NET_NETWORK)),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case FRAMES_95 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.WIZARD, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.WIZARD, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.WIZARD, steps(STEP_COPYING_FILES)),
                    new Stage(InstallerPage.DONE, InstallerChrome.WIZARD, List.of()));
            /*
             * The one installer that changes shape partway through. Its question sits on the graphical phase
             * with a step of its own, which is what the copy runs through while the player is answering it.
             */
            case FRAMES_XP -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(STEP_COPYING_FILES)),
                    new Stage(InstallerPage.NAME, InstallerChrome.SIDE_PANEL, steps(STEP_COLLECTING)),
                    // The family's name is its brand, which no language translates.
                    new Stage(InstallerPage.COPY, InstallerChrome.SIDE_PANEL, steps(
                            INSTALLING_SYSTEM.with("Frames"), STEP_FINALIZING)),
                    new Stage(InstallerPage.DONE, InstallerChrome.SIDE_PANEL, List.of()));
            /*
             * The newest one opens on a word before it asks anything. It used to start on the disk table, so
             * the first thing a player saw was a question about erasing something, with nothing having said
             * what was about to happen.
             */
            case FRAMES_11 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.CARD, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.CARD, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.CARD, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.CARD, steps(
                            STEP_COPYING_FILES, STEP_SYSTEM_FOLDERS, STEP_BUILT_IN_PROGRAMS,
                            STEP_SETTING_UP_BOOT_ENTRY)),
                    new Stage(InstallerPage.DONE, InstallerChrome.CARD, List.of()));
            case UBUNTU -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(
                            STEP_PREPARING_DISK, STEP_COPYING_SYSTEM, STEP_WRITING_BOOT_ENTRY,
                            STEP_SETTING_HOST_NAME)),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            case DEBIAN -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.BOXED_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.BOXED_TEXT, steps(
                            STEP_UNPACKING, STEP_WRITING_BOOT_ENTRY, STEP_SETTING_HOST_NAME)),
                    new Stage(InstallerPage.DONE, InstallerChrome.BOXED_TEXT, List.of()));
            case FEDORA -> List.of(
                    new Stage(InstallerPage.HUB, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(
                            STEP_INSTALLATION_ENVIRONMENT, STEP_PREPARING_DISK, STEP_COPYING_SYSTEM,
                            STEP_WRITING_BOOT_ENTRY, STEP_SETTING_HOST_NAME)),
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
    public Text title(final String systemName) {
        return switch (this) {
            case UBUNTU, DEBIAN, FEDORA -> TITLE_INSTALLER.with(systemName);
            default -> TITLE_SETUP.with(systemName);
        };
    }

    /** What the page says it is, in this installer's own words. */
    public Text heading(final InstallerPage page, final String systemName) {
        return switch (this) {
            case PLAIN -> switch (page) {
                case WELCOME -> PLAIN_WELCOME.with(systemName);
                case COPY -> INSTALLING_SYSTEM.with(systemName);
                default -> INSTALLATION_COMPLETE.text();
            };
            case MC_DOS -> switch (page) {
                case WELCOME -> DOS_WELCOME.text();
                case SETTINGS -> DOS_SETTINGS.text();
                case COPY -> DOS_COPY.with(systemName);
                default -> DOS_DONE.with(systemName);
            };
            /*
             * It calls the machine a server, because that is what this system makes of whatever it is put
             * on, and it never says "please wait": the whole of it is over in a breath.
             */
            case MC_NET -> switch (page) {
                case WELCOME -> NET_WELCOME.with(systemName);
                case COPY -> NET_COPY.text();
                default -> NET_DONE.text();
            };
            case FRAMES_95 -> switch (page) {
                case WELCOME -> FRAMES_95_WELCOME.text();
                case DISK -> FRAMES_95_DISK.text();
                case NAME -> COMPUTER_NAME.text();
                case COPY -> FRAMES_95_COPY.text();
                default -> SETUP_FINISHED.text();
            };
            case FRAMES_XP -> switch (page) {
                case DISK -> FRAMES_XP_DISK.text();
                case NAME -> COMPUTER_NAME.text();
                case COPY -> FRAMES_XP_COPY.text();
                default -> SETUP_FINISHED.text();
            };
            case FRAMES_11 -> switch (page) {
                case DISK -> FRAMES_11_DISK.with(systemName);
                case NAME -> FRAMES_11_NAME.text();
                case COPY -> INSTALLING_SYSTEM.with(systemName);
                default -> SETUP_FINISHED.text();
            };
            case UBUNTU -> switch (page) {
                case DISK -> UBUNTU_DISK.text();
                case NAME -> UBUNTU_NAME.text();
                case DESKTOP -> UBUNTU_DESKTOP.text();
                case COPY -> UBUNTU_COPY.text();
                default -> INSTALLATION_COMPLETE.text();
            };
            case DEBIAN -> switch (page) {
                case DISK -> DEBIAN_DISK.text();
                case NAME -> DEBIAN_NAME.text();
                case DESKTOP -> SOFTWARE_SELECTION.text();
                case COPY -> DEBIAN_COPY.text();
                default -> DEBIAN_DONE.text();
            };
            case FEDORA -> switch (page) {
                case HUB -> FEDORA_HUB.text();
                case DISK -> FEDORA_DISK.text();
                case DESKTOP -> SOFTWARE_SELECTION.text();
                case NAME -> FEDORA_NAME.text();
                case COPY -> FEDORA_COPY.text();
                default -> FEDORA_DONE.text();
            };
        };
    }

    /** The line of keys along the foot of the page, empty where the installer draws buttons instead. */
    public Text hint(final InstallerPage page) {
        return switch (this) {
            case MC_DOS -> switch (page) {
                case WELCOME, SETTINGS -> HINT_CONTINUE_EXIT.text();
                case DONE -> HINT_RESTART.text();
                default -> Text.EMPTY;
            };
            case FRAMES_95 -> page == InstallerPage.WELCOME ? HINT_CONTINUE_EXIT.text() : Text.EMPTY;
            case FRAMES_XP -> switch (page) {
                case DISK -> HINT_XP_DISK.text();
                case DONE -> HINT_RESTART.text();
                default -> Text.EMPTY;
            };
            case UBUNTU -> (page == InstallerPage.COPY || page == InstallerPage.DONE
                    ? HINT_UBUNTU_REBOOT : HINT_UBUNTU_DONE_BACK).text();
            case DEBIAN -> HINT_DEBIAN.text();
            case FEDORA -> (page == InstallerPage.HUB ? HINT_FEDORA_HUB : HINT_FEDORA_PAGE).text();
            default -> Text.EMPTY;
        };
    }

    /** The steps of a page's share of the copy, each a sentence or one already filled in. */
    private static List<Text> steps(final Object... steps) {
        final List<Text> out = new ArrayList<>(steps.length);
        for (final Object step : steps) {
            out.add(Text.of(step));
        }
        return out;
    }

    /**
     * One page of an installer, with the share of the copy that runs while it is up.
     *
     * @param page   what the page is for
     * @param chrome the shape it is drawn in
     * @param steps  the steps of the copy that belong to this page, in order; empty on a page that only asks
     */
    public record Stage(InstallerPage page, InstallerChrome chrome, List<Text> steps) {

        public Stage {
            steps = List.copyOf(steps);
        }

        /** Whether this page waits for the player, rather than moving on by itself when its steps are done. */
        public boolean asks() {
            return this.page != InstallerPage.COPY && this.page != InstallerPage.DONE;
        }
    }
}
