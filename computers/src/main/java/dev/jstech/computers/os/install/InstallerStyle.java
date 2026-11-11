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

    /**
     * A glass window over a ground of night blue: Install now, the table of disks, a checklist of five steps over
     * the two phases of the whole install, and Set Up Frames for the machine's name.
     */
    FRAMES_7("frames_7"),

    /**
     * A white window with a thin blue border over the dark: Install now, the table of disks and the status of five
     * steps; then the machine's name asked full-screen in blue, the way the first set-up asked it.
     */
    FRAMES_10("frames_10"),

    /** Pale cards, a table of disks, one question to a page. */
    FRAMES_11("frames_11"),

    /** The server installer: a coloured header, a body of text, and Done at the foot. */
    UBUNTU("ubuntu"),

    /** The text installer: a grey window with its title in a tab, over blue. */
    DEBIAN("debian"),

    /** The text hub: every answer in a numbered list, and a letter to begin. */
    FEDORA("fedora"),

    /** FreeBSD's own: a navy ground and grey dialog boxes, their title flush in the top of their own border. */
    BSD_INSTALL("bsd_install"),

    /** UNIX System V's: one banner in reverse video and a settings page that asks the disk and the name together. */
    SYSTEM_V("system_v"),

    /**
     * The Quince System's: an Easy Install dialog on the desktop, and the system on three floppy disks, the installer
     * putting each out of the slot when it has read it and asking for the next (computers/docs/design/quince.md).
     */
    QUINCE_SYSTEM("quince_system"),

    /** Quince OS 9's, from its CD: an assistant of five steps listed down its left side, in Platinum. */
    QUINCE_OS_9("quince_os_9"),

    /** Quince OS X's, and that of every Quince system after it: an assistant of seven steps, a dot beside each. */
    QUINCE_OS_X("quince_os_x"),

    /**
     * OpenBSD's: questions and answers at the terminal, in the real one's order (computers/docs/design/
     * operating-systems.md): the machine's name, its services, the root disk, where the sets come from and which
     * of them go, then the sets one by one with their bars, and the way out.
     */
    OPENBSD("openbsd"),

    /**
     * Linux Mint's: a window on the live desktop its medium started, in five screens, the installation type, the
     * divider beside another system, who you are while the files are copied, the copy and the end.
     */
    MINT("mint");

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
            "The following list shows the existing partitions and unpartitioned space on this computer.");
    private static final TextKey FRAMES_XP_FORMAT = TextKey.of("jsc.install.installer_style.frames_xp_format",
            "Setup will install %s on partition");
    private static final TextKey FRAMES_XP_COPY = TextKey.of("jsc.install.installer_style.frames_xp_copy",
            "Please wait while Setup copies files to the disk.");
    private static final TextKey FRAMES_11_DISK = TextKey.of("jsc.install.installer_style.frames_11_disk",
            "Where do you want to install %s?");
    private static final TextKey FRAMES_11_NAME = TextKey.of("jsc.install.installer_style.frames_11_name",
            "Name this PC");
    private static final TextKey TITLE_INSTALL = TextKey.of("jsc.install.installer_style.title_install",
            "Install %s");
    private static final TextKey FRAMES_7_INSTALLING = TextKey.of("jsc.install.installer_style.frames_7_installing",
            "Installing %s...");
    private static final TextKey FRAMES_7_NAME = TextKey.of("jsc.install.installer_style.frames_7_name",
            "Choose a name for your computer to distinguish it on the network.");
    private static final TextKey FRAMES_10_NAME = TextKey.of("jsc.install.installer_style.frames_10_name",
            "Who's going to use this PC?");
    private static final TextKey STEP_COPYING_SYSTEM_FILES =
            TextKey.of("jsc.install.installer_style.step_copying_system_files", "Copying %s files");
    private static final TextKey STEP_EXPANDING_SYSTEM_FILES =
            TextKey.of("jsc.install.installer_style.step_expanding_system_files", "Expanding %s files");
    private static final TextKey STEP_GETTING_FILES_READY =
            TextKey.of("jsc.install.installer_style.step_getting_files_ready", "Getting files ready for installation");
    private static final TextKey STEP_INSTALLING_FEATURES =
            TextKey.of("jsc.install.installer_style.step_installing_features", "Installing features");
    private static final TextKey STEP_INSTALLING_UPDATES =
            TextKey.of("jsc.install.installer_style.step_installing_updates", "Installing updates");
    private static final TextKey STEP_COMPLETING_INSTALLATION =
            TextKey.of("jsc.install.installer_style.step_completing_installation", "Completing installation");
    private static final TextKey STEP_FINISHING_UP =
            TextKey.of("jsc.install.installer_style.step_finishing_up", "Finishing up");
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
    private static final TextKey TITLE_BSD = TextKey.of("jsc.install.installer_style.title_bsd", "%s Installer");
    private static final TextKey BSD_WELCOME = TextKey.of("jsc.install.installer_style.bsd_welcome", "Welcome");
    private static final TextKey BSD_NAME = TextKey.of("jsc.install.installer_style.bsd_name", "Set Hostname");
    private static final TextKey BSD_COMPONENTS =
            TextKey.of("jsc.install.installer_style.bsd_components", "Distribution Select");
    private static final TextKey BSD_DISK = TextKey.of("jsc.install.installer_style.bsd_disk", "Partitioning");
    private static final TextKey BSD_POOL = TextKey.of("jsc.install.installer_style.bsd_pool", "ZFS Configuration");
    private static final TextKey BSD_MIRROR = TextKey.of("jsc.install.installer_style.bsd_mirror",
            "Mirror Selection");
    private static final TextKey BSD_COPY = TextKey.of("jsc.install.installer_style.bsd_copy", "Archive Extraction");
    private static final TextKey BSD_SERVICES = TextKey.of("jsc.install.installer_style.bsd_services",
            "System Configuration");
    private static final TextKey BSD_DESKTOP = TextKey.of("jsc.install.installer_style.bsd_desktop", "Desktop");
    private static final TextKey BSD_DONE = TextKey.of("jsc.install.installer_style.bsd_done", "Complete");
    /* What bsdinstall's archive extraction names the base system and its kernel, always copied. */
    private static final TextKey STEP_BSD_BASE = TextKey.of("jsc.install.installer_style.step_bsd_base", "base.txz");
    private static final TextKey STEP_BSD_KERNEL =
            TextKey.of("jsc.install.installer_style.step_bsd_kernel", "kernel.txz");
    /* Package-visible: InstallerFlow compares a running step's label against this one to leave it out of the copy
     * when the ports component was not ticked, since the stages a style declares do not change at runtime. */
    static final TextKey STEP_BSD_PORTS = TextKey.of("jsc.install.installer_style.step_bsd_ports", "ports.txz");

    private static final TextKey TITLE_SYSTEM_V = TextKey.of("jsc.install.installer_style.title_system_v",
            "%s Release 3.2 Installation - Bellwether Labs");
    private static final TextKey SYSV_HINT_ASKING = TextKey.of("jsc.install.installer_style.sysv_hint_asking",
            "Up and Down choose, Enter goes on, F3 leaves the installation.");
    private static final TextKey SYSV_HINT_DONE = TextKey.of("jsc.install.installer_style.sysv_hint_done",
            "Remove the medium and press Enter to reboot.");
    /* What the one copy UNIX makes is named by, part by part, since there is one medium and so one copy to name. */
    private static final TextKey STEP_SYSV_BASE = TextKey.of("jsc.install.installer_style.step_sysv_base",
            "Base system");
    private static final TextKey STEP_SYSV_UTILITIES = TextKey.of("jsc.install.installer_style.step_sysv_utilities",
            "Utilities");
    private static final TextKey STEP_SYSV_HELP = TextKey.of("jsc.install.installer_style.step_sysv_help",
            "On-line help");
    private static final TextKey STEP_SYSV_MANUAL = TextKey.of("jsc.install.installer_style.step_sysv_manual",
            "Manual pages");

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
            "ENTER=Install  D=Delete Partition  F3=Quit");
    private static final TextKey HINT_XP_FORMAT = TextKey.of("jsc.install.installer_style.hint_xp_format",
            "ENTER=Continue  ESC=Cancel");
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

    private static final TextKey TITLE_QUINCE_SYSTEM =
            TextKey.of("jsc.install.installer_style.title_quince_system", "Installer");
    private static final TextKey QUINCE_EASY_INSTALL =
            TextKey.of("jsc.install.installer_style.quince_easy_install", "Easy Install");
    private static final TextKey QUINCE_INSERT =
            TextKey.of("jsc.install.installer_style.quince_insert", "Please insert the disk:");
    private static final TextKey QUINCE_SUCCESS =
            TextKey.of("jsc.install.installer_style.quince_success", "Installation was successful.");
    private static final TextKey QUINCE_WELCOME = TextKey.of("jsc.install.installer_style.quince_welcome", "Welcome");
    private static final TextKey QUINCE_INFO =
            TextKey.of("jsc.install.installer_style.quince_info", "Important Information");
    private static final TextKey QUINCE_LICENSE =
            TextKey.of("jsc.install.installer_style.quince_license", "Software License Agreement");
    private static final TextKey QUINCE_DESTINATION =
            TextKey.of("jsc.install.installer_style.quince_destination", "Select Destination");
    private static final TextKey QUINCE_A_DESTINATION =
            TextKey.of("jsc.install.installer_style.quince_a_destination", "Select a Destination");
    private static final TextKey QUINCE_INSTALL_SOFTWARE =
            TextKey.of("jsc.install.installer_style.quince_install_software", "Install Software");
    private static final TextKey QUINCE_X_WELCOME =
            TextKey.of("jsc.install.installer_style.quince_x_welcome", "Welcome to the %s Installer");
    private static final TextKey QUINCE_TYPE =
            TextKey.of("jsc.install.installer_style.quince_type", "Standard Install");
    private static final TextKey QUINCE_X_DONE =
            TextKey.of("jsc.install.installer_style.quince_x_done", "The software was successfully installed");
    /* The steps an assistant lists down its left side, in its own words. */
    private static final TextKey SIDE_INFO = TextKey.of("jsc.install.installer_style.side_info", "Important Info...");
    private static final TextKey SIDE_LICENSE =
            TextKey.of("jsc.install.installer_style.side_license", "Software License");
    private static final TextKey SIDE_INTRODUCTION =
            TextKey.of("jsc.install.installer_style.side_introduction", "Introduction");
    private static final TextKey SIDE_READ_ME = TextKey.of("jsc.install.installer_style.side_read_me", "Read Me");
    private static final TextKey SIDE_LICENSE_X = TextKey.of("jsc.install.installer_style.side_license_x", "License");
    private static final TextKey SIDE_TYPE =
            TextKey.of("jsc.install.installer_style.side_type", "Installation Type");
    private static final TextKey SIDE_INSTALL = TextKey.of("jsc.install.installer_style.side_install", "Install");
    private static final TextKey SIDE_FINISH = TextKey.of("jsc.install.installer_style.side_finish", "Finish Up");
    /* The Quince System's three disks, by the names on their labels, and what its copy writes off each. */
    private static final TextKey DISK_INSTALL_1 = TextKey.of("jsc.install.installer_style.disk_install_1",
            "Install 1");
    private static final TextKey DISK_SYSTEM_TOOLS =
            TextKey.of("jsc.install.installer_style.disk_system_tools", "System Tools");
    private static final TextKey DISK_UTILITIES = TextKey.of("jsc.install.installer_style.disk_utilities",
            "Utilities");
    private static final TextKey STEP_WRITING = TextKey.of("jsc.install.installer_style.step_writing",
            "Writing \"%s\"...");
    private static final TextKey STEP_QUINCE_UPDATING =
            TextKey.of("jsc.install.installer_style.step_quince_updating", "Updating the disk");
    private static final TextKey STEP_QUINCE_PREPARING =
            TextKey.of("jsc.install.installer_style.step_quince_preparing", "Preparing to install");
    private static final TextKey STEP_QUINCE_SYSTEM_FOLDER =
            TextKey.of("jsc.install.installer_style.step_quince_system_folder", "Installing the System Folder");
    private static final TextKey STEP_QUINCE_APPLICATIONS =
            TextKey.of("jsc.install.installer_style.step_quince_applications", "Installing the applications");
    private static final TextKey STEP_QUINCE_OPTIMIZING =
            TextKey.of("jsc.install.installer_style.step_quince_optimizing", "Optimizing system performance");

    /*
     * OpenBSD's sets, by the names its installer gives them, which are file names in every language; the three a
     * player may leave out are package-visible, so the flow can drop a set left out from the copy.
     */
    private static final Text SET_BSD = Text.literal("bsd");
    static final Text SET_BSD_RD = Text.literal("bsd.rd");
    private static final Text SET_BASE = Text.literal("base75.tgz");
    static final Text SET_COMP = Text.literal("comp75.tgz");
    static final Text SET_MAN = Text.literal("man75.tgz");
    private static final TextKey STEP_RELINKING = TextKey.of("jsc.install.installer_style.step_relinking",
            "Relinking to create unique kernel");
    private static final TextKey TITLE_OPENBSD = TextKey.of("jsc.install.installer_style.title_openbsd",
            "%s installation program");

    private static final TextKey MINT_TITLE = TextKey.of("jsc.install.installer_style.mint_title", "Install");
    private static final TextKey MINT_TYPE = TextKey.of("jsc.install.installer_style.mint_type",
            "Installation type");
    private static final TextKey MINT_ALONGSIDE = TextKey.of("jsc.install.installer_style.mint_alongside",
            "Install %s alongside %s");
    private static final TextKey MINT_WHO = TextKey.of("jsc.install.installer_style.mint_who", "Who are you?");
    private static final TextKey MINT_WELCOME = TextKey.of("jsc.install.installer_style.mint_welcome",
            "Welcome to %s");
    private static final TextKey MINT_COMPLETE = TextKey.of("jsc.install.installer_style.mint_complete",
            "Installation Complete");
    private static final TextKey STEP_MINT_COPYING = TextKey.of("jsc.install.installer_style.step_mint_copying",
            "Copying files...");
    private static final TextKey STEP_MINT_SYSTEM = TextKey.of("jsc.install.installer_style.step_mint_system",
            "Installing system");
    private static final TextKey STEP_MINT_HARDWARE = TextKey.of("jsc.install.installer_style.step_mint_hardware",
            "Configuring hardware...");
    private static final TextKey STEP_MINT_BOOT_LOADER =
            TextKey.of("jsc.install.installer_style.step_mint_boot_loader", "Installing the boot loader...");

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
                    new Stage(InstallerPage.FORMAT, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT, steps(STEP_COPYING_FILES)),
                    new Stage(InstallerPage.NAME, InstallerChrome.SIDE_PANEL, steps(STEP_COLLECTING)),
                    // The family's name is its brand, which no language translates.
                    new Stage(InstallerPage.COPY, InstallerChrome.SIDE_PANEL, steps(
                            INSTALLING_SYSTEM.with("Frames"), STEP_FINALIZING)),
                    new Stage(InstallerPage.DONE, InstallerChrome.SIDE_PANEL, List.of()));
            /*
             * The five steps of the copy, and the machine's name asked after them, as Set Up Frames asked it
             * once the copy had restarted the machine. The family's name in a step is its brand.
             */
            case FRAMES_7 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.GLASS, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.GLASS, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.GLASS, steps(
                            STEP_COPYING_SYSTEM_FILES.with("Frames"), STEP_EXPANDING_SYSTEM_FILES.with("Frames"),
                            STEP_INSTALLING_FEATURES, STEP_INSTALLING_UPDATES, STEP_COMPLETING_INSTALLATION)),
                    new Stage(InstallerPage.NAME, InstallerChrome.GLASS, List.of()),
                    new Stage(InstallerPage.DONE, InstallerChrome.GLASS, List.of()));
            /*
             * The setup window for the copy, then the first set-up in blue across the whole screen for the one
             * question left, which is who uses the machine; here that is the name it answers to.
             */
            case FRAMES_10 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.METRO, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.METRO, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.METRO, steps(
                            STEP_COPYING_SYSTEM_FILES.with("Frames"), STEP_GETTING_FILES_READY,
                            STEP_INSTALLING_FEATURES, STEP_INSTALLING_UPDATES, STEP_FINISHING_UP)),
                    new Stage(InstallerPage.NAME, InstallerChrome.FIRST_SETUP, List.of()),
                    new Stage(InstallerPage.DONE, InstallerChrome.FIRST_SETUP, List.of()));
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
            /*
             * The real order: welcome, hostname, components, disk, Mirror, the extraction (base and kernel always,
             * ports when ticked), services, a desktop only when the Mirror answered, and the end. A second COPY
             * stage sits after DESKTOP with no steps of its own: a desktop chosen from the Mirror adds its fetch
             * there rather than onto the extraction, which is already over by the time that page is reached.
             */
            case BSD_INSTALL -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.COMPONENTS, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.POOL, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.MIRROR, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.DIALOG_BOX,
                            steps(STEP_BSD_BASE, STEP_BSD_KERNEL, STEP_BSD_PORTS)),
                    new Stage(InstallerPage.SERVICES, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.DESKTOP, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.DIALOG_BOX, List.of()),
                    new Stage(InstallerPage.DONE, InstallerChrome.DIALOG_BOX, List.of()));
            /* The disk and the name asked together, the way the installers of the first age asked them, then the
             * one copy, then the end. */
            case SYSTEM_V -> List.of(
                    new Stage(InstallerPage.SETTINGS, InstallerChrome.FULL_TEXT, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.FULL_TEXT,
                            steps(STEP_SYSV_BASE, STEP_SYSV_UTILITIES, STEP_SYSV_HELP, STEP_SYSV_MANUAL)),
                    new Stage(InstallerPage.DONE, InstallerChrome.FULL_TEXT, List.of()));
            /*
             * Easy Install, then a copy off each of the three disks in turn, the installer asking for the next one
             * between them; what each copy writes is named as the real one named it, file by file.
             */
            case QUINCE_SYSTEM -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.QUINCE_DESKTOP, List.of(
                            STEP_WRITING.with("System"), STEP_WRITING.with("Seeker"))),
                    new Stage(InstallerPage.INSERT, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.QUINCE_DESKTOP, List.of(
                            STEP_WRITING.with("Control Panels"), STEP_WRITING.with("TextPad"))),
                    new Stage(InstallerPage.INSERT, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.QUINCE_DESKTOP, List.of(
                            STEP_WRITING.with("Calculator"), STEP_QUINCE_UPDATING.text())),
                    new Stage(InstallerPage.DONE, InstallerChrome.QUINCE_DESKTOP, List.of()));
            case QUINCE_OS_9 -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.READ_ME, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.LICENSE, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.QUINCE_DESKTOP, steps(
                            STEP_QUINCE_PREPARING, STEP_QUINCE_SYSTEM_FOLDER, STEP_QUINCE_APPLICATIONS,
                            STEP_QUINCE_UPDATING)),
                    new Stage(InstallerPage.DONE, InstallerChrome.QUINCE_DESKTOP, List.of()));
            case QUINCE_OS_X -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.READ_ME, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.LICENSE, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COMPONENTS, InstallerChrome.QUINCE_DESKTOP, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.QUINCE_DESKTOP, steps(
                            STEP_QUINCE_PREPARING, STEP_QUINCE_SYSTEM_FOLDER, STEP_QUINCE_APPLICATIONS,
                            STEP_QUINCE_OPTIMIZING)),
                    new Stage(InstallerPage.DONE, InstallerChrome.QUINCE_DESKTOP, List.of()));
            /*
             * The real order of questions; each set is a step of its own with its own bar, and the kernel is relinked
             * once they are all in.
             */
            case OPENBSD -> List.of(
                    new Stage(InstallerPage.WELCOME, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.SERVICES, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.DISK, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.MIRROR, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.COMPONENTS, InstallerChrome.TERMINAL, List.of()),
                    new Stage(InstallerPage.COPY, InstallerChrome.TERMINAL,
                            steps(SET_BSD, SET_BSD_RD, SET_BASE, SET_COMP, SET_MAN, STEP_RELINKING)),
                    new Stage(InstallerPage.DONE, InstallerChrome.TERMINAL, List.of()));
            /*
             * The copy starts as soon as the changes are written and runs under "Who are you?", as the real one
             * does, so its first step belongs to that page.
             */
            case MINT -> List.of(
                    new Stage(InstallerPage.DISK, InstallerChrome.LIVE_DESKTOP, List.of()),
                    new Stage(InstallerPage.ALONGSIDE, InstallerChrome.LIVE_DESKTOP, List.of()),
                    new Stage(InstallerPage.NAME, InstallerChrome.LIVE_DESKTOP, steps(STEP_MINT_COPYING)),
                    new Stage(InstallerPage.COPY, InstallerChrome.LIVE_DESKTOP,
                            steps(STEP_MINT_SYSTEM, STEP_MINT_HARDWARE, STEP_MINT_BOOT_LOADER)),
                    new Stage(InstallerPage.DONE, InstallerChrome.LIVE_DESKTOP, List.of()));
        };
    }

    /**
     * The names on the labels of the disks this system comes on, in the order the installer reads them: one name for
     * each disk of a system on several, and none for one that comes on a single medium.
     */
    public List<Text> disks() {
        return this == QUINCE_SYSTEM
                ? List.of(DISK_INSTALL_1.text(), DISK_SYSTEM_TOOLS.text(), DISK_UTILITIES.text())
                : List.of();
    }

    /**
     * What an assistant's list down its left side calls that page, or nothing for a page it does not list (the
     * Quince System's installer is a dialog and lists nothing).
     */
    public Text sideLabel(final InstallerPage page) {
        if (this == QUINCE_OS_9) {
            return switch (page) {
                case WELCOME -> QUINCE_WELCOME.text();
                case READ_ME -> SIDE_INFO.text();
                case LICENSE -> SIDE_LICENSE.text();
                case DISK -> QUINCE_DESTINATION.text();
                case COPY, DONE -> QUINCE_INSTALL_SOFTWARE.text();
                default -> Text.EMPTY;
            };
        }
        if (this == QUINCE_OS_X) {
            return switch (page) {
                case WELCOME -> SIDE_INTRODUCTION.text();
                case READ_ME -> SIDE_READ_ME.text();
                case LICENSE -> SIDE_LICENSE_X.text();
                case DISK -> QUINCE_DESTINATION.text();
                case COMPONENTS -> SIDE_TYPE.text();
                case COPY -> SIDE_INSTALL.text();
                case DONE -> SIDE_FINISH.text();
                default -> Text.EMPTY;
            };
        }
        return Text.EMPTY;
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

    /**
     * Whether this installer has anything to ask of a Mirror: a desktop to offer with the system, or the sets of
     * the system itself to fetch from it (OpenBSD's).
     */
    public boolean asksMirror() {
        for (final Stage stage : this.stages()) {
            if (stage.page() == InstallerPage.MIRROR || stage.page() == InstallerPage.DESKTOP) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether its medium starts a live desktop the installer runs on as a window, rather than starting the
     * installer itself (Linux Mint's).
     */
    public boolean bootsLive() {
        return this == MINT;
    }

    /**
     * Whether, left to itself on a disk another system fills, it shrinks that system's partition and goes in the room
     * freed, as Ubuntu and Linux Mint install alongside another system.
     */
    public boolean shrinksBeside() {
        return this == UBUNTU || this == MINT;
    }

    /**
     * Whether its disk step lists every partition and free space to choose from rather than whole disks: Frames XP's
     * text phase and the table of Frames 7 to 11.
     */
    public boolean listsPartitions() {
        return this == FRAMES_XP || this == FRAMES_7 || this == FRAMES_10 || this == FRAMES_11;
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
            case BSD_INSTALL -> TITLE_BSD.with(systemName);
            case SYSTEM_V -> TITLE_SYSTEM_V.with(systemName);
            // The family's name, not the edition's: that is what the window across the top said.
            case FRAMES_7 -> TITLE_INSTALL.with("Frames");
            case FRAMES_10 -> TITLE_SETUP.with("Frames");
            case QUINCE_SYSTEM -> TITLE_QUINCE_SYSTEM.text();
            case QUINCE_OS_9, QUINCE_OS_X -> TITLE_INSTALL.with(systemName);
            case OPENBSD -> TITLE_OPENBSD.with(systemName);
            case MINT -> MINT_TITLE.text();
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
                case FORMAT -> FRAMES_XP_FORMAT.with(systemName);
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
            case FRAMES_7 -> switch (page) {
                case WELCOME -> TITLE_INSTALL.with(systemName);
                case DISK -> FRAMES_11_DISK.with("Frames");
                case COPY -> FRAMES_7_INSTALLING.with("Frames");
                case NAME -> FRAMES_7_NAME.text();
                default -> SETUP_FINISHED.text();
            };
            case FRAMES_10 -> switch (page) {
                case WELCOME -> TITLE_INSTALL.with(systemName);
                case DISK -> FRAMES_11_DISK.with("Frames");
                case COPY -> INSTALLING_SYSTEM.with("Frames");
                case NAME -> FRAMES_10_NAME.text();
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
            case BSD_INSTALL -> switch (page) {
                case WELCOME -> BSD_WELCOME.text();
                case NAME -> BSD_NAME.text();
                case COMPONENTS -> BSD_COMPONENTS.text();
                case DISK -> BSD_DISK.text();
                case POOL -> BSD_POOL.text();
                case MIRROR -> BSD_MIRROR.text();
                case COPY -> BSD_COPY.text();
                case SERVICES -> BSD_SERVICES.text();
                case DESKTOP -> BSD_DESKTOP.text();
                default -> BSD_DONE.text();
            };
            /* One banner in reverse video the whole way through: the page says what it is by its own content. */
            case SYSTEM_V -> Text.EMPTY;
            case QUINCE_SYSTEM -> switch (page) {
                case WELCOME -> QUINCE_EASY_INSTALL.text();
                case INSERT -> QUINCE_INSERT.text();
                case COPY -> INSTALLING_SYSTEM.with(systemName);
                default -> QUINCE_SUCCESS.text();
            };
            case QUINCE_OS_9 -> switch (page) {
                case WELCOME -> QUINCE_WELCOME.text();
                case READ_ME -> QUINCE_INFO.text();
                case LICENSE -> QUINCE_LICENSE.text();
                case DISK -> QUINCE_DESTINATION.text();
                case COPY -> INSTALLING_SYSTEM.with(systemName);
                default -> QUINCE_SUCCESS.text();
            };
            case QUINCE_OS_X -> switch (page) {
                case WELCOME -> QUINCE_X_WELCOME.with(systemName);
                case READ_ME -> QUINCE_INFO.text();
                case LICENSE -> QUINCE_LICENSE.text();
                case DISK -> QUINCE_A_DESTINATION.text();
                case COMPONENTS -> QUINCE_TYPE.text();
                case COPY -> INSTALLING_SYSTEM.with(systemName);
                default -> QUINCE_X_DONE.text();
            };
            /* A terminal has no headings: each question says what it is about. */
            case OPENBSD -> Text.EMPTY;
            /* The page beside another system names that system, which the screen knows and puts in. */
            case MINT -> switch (page) {
                case DISK -> MINT_TYPE.text();
                case NAME -> MINT_WHO.text();
                case COPY -> MINT_WELCOME.with(systemName);
                case DONE -> MINT_COMPLETE.text();
                default -> Text.EMPTY;
            };
        };
    }

    /** The heading of Linux Mint's page beside another system, which names both. */
    public Text alongside(final String systemName, final String other) {
        return MINT_ALONGSIDE.with(systemName, other);
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
                case FORMAT -> HINT_XP_FORMAT.text();
                case DONE -> HINT_RESTART.text();
                default -> Text.EMPTY;
            };
            case UBUNTU -> (page == InstallerPage.COPY || page == InstallerPage.DONE
                    ? HINT_UBUNTU_REBOOT : HINT_UBUNTU_DONE_BACK).text();
            case DEBIAN -> HINT_DEBIAN.text();
            case FEDORA -> (page == InstallerPage.HUB ? HINT_FEDORA_HUB : HINT_FEDORA_PAGE).text();
            /* Nothing is chosen and F3 no longer leaves once the copy has begun, so the page names neither. */
            case SYSTEM_V -> page == InstallerPage.DONE ? SYSV_HINT_DONE.text()
                    : page == InstallerPage.COPY ? Text.EMPTY : SYSV_HINT_ASKING.text();
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
