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
 * The pages of the reboot chapter of the Linux by Hand book: what the live shell checks before it restarts the
 * machine into a hand-built system, the lines it refuses with, which typed files reach the installed system, and
 * what the machine checks when it starts that system.
 */
final class RebootRefusesPages {

    private RebootRefusesPages() {
    }

    /** Declares the sections and entries of the refusals of the reboot, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        beforeTheReboot(guide, chapter);
        afterTheReboot(guide, chapter);
    }

    private static void beforeTheReboot(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("before_the_reboot").titled("Before the reboot")
                .in(chapter).register();
        rebootChecklist(guide, section);
        willNotBootMessages(guide, section);
        whatTheHandFilesDo(guide, section);
    }

    private static void afterTheReboot(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("after_the_reboot").titled("After the reboot")
                .in(chapter).register();
        afterRebootChecks(guide, section);
    }

    private static void rebootChecklist(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("reboot_checklist", section).titled("What the new system needs before the reboot")
                .paragraph("When the new system is installed, you restart the machine into it with the reboot"
                        + " command. Before it restarts the machine, the shell checks a short list of what the new"
                        + " system needs to start. If something on the list is missing, the machine does not"
                        + " restart: the shell names the missing items, and your live session stays open.")
                .paragraph("The reboot step of each guide ([](jsc:arch_reboot), [](jsc:gentoo_reboot)) runs this"
                        + " check. The list has two parts. The always-asked part holds the steps that every"
                        + " installation needs. The optional part holds the rest of the handbook, and the shell checks"
                        + " it only when a setting asks for it.")
                .subheading("Checking before the reboot")
                .steps("Type reboot and press [[Enter]].",
                        "If the shell prints \"The new system will not boot:\", read the lines under it.",
                        "Do the step that each line names.",
                        "Type reboot again.")
                .subheading("The always-asked part")
                .paragraph("Each row names one missing item, the line the shell prints for it, and the step that"
                        + " clears it.")
                .table("Always asked for", "Missing item", "The shell prints", "The step that clears it")
                .row(GuideCell.words("Base system (Gentoo)"),
                        GuideCell.code("no base system installed (wget the stage 3, then tar)"),
                        GuideCell.words("Unpack the stage 3 archive onto the disk ([](jsc:stage_three))."))
                .row(GuideCell.words("Base system (Arch)"),
                        GuideCell.code("no base system installed (pacstrap)"),
                        GuideCell.words("Install the base system with pacstrap ([](jsc:arch_base_system))."))
                .row(GuideCell.words("Table of what to mount (Gentoo)"),
                        GuideCell.code("no root filesystem in /etc/fstab (blkid, then write the line)"),
                        GuideCell.words("Write the line for the root partition ([](jsc:fstab_by_hand))."))
                .row(GuideCell.words("Table of what to mount (Arch)"),
                        GuideCell.code("no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)"),
                        GuideCell.words("Write the table with genfstab ([](jsc:arch_fstab))."))
                .row(GuideCell.words("Kernel (Gentoo)"),
                        GuideCell.code("no kernel (genkernel all, or make in /usr/src/linux)"),
                        GuideCell.words("Build and install the kernel ([](jsc:kernel_gentoo))."))
                .row(GuideCell.words("Bootloader (both)"),
                        GuideCell.code("no bootloader (grub-install)"),
                        GuideCell.words("Install GRUB ([](jsc:arch_bootloader) on Arch, [](jsc:gentoo_bootloader)"
                                + " on Gentoo)."))
                .row(GuideCell.words("Bootloader's list (both)"),
                        GuideCell.code("nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)"),
                        GuideCell.words("Write the list with grub-mkconfig ([](jsc:arch_grub_config))."))
                .paragraph("Gentoo checks its table by the content of the file, not by running a tool. A line that"
                        + " does not start with a hash sign needs at least three fields, and its second field must be"
                        + " a slash. The hint names blkid, the tool that shows each partition's details. The stage 3"
                        + " writes a table with only comments in it, so this item stays on the list until you write"
                        + " the root line. On Arch the item goes as soon as genfstab has run.")
                .subheading("The optional part")
                .paragraph("The shell asks for the optional part only when the world's setting for that system is"
                        + " on. Both settings are off by default, so a world asks for the basics alone. The settings"
                        + " are install_by_hand.gentoo_every_step and install_by_hand.arch_every_step, in the server"
                        + " configuration.")
                .table("The optional part, when its setting is on", "Missing item", "The shell prints",
                        "The step that clears it")
                .row(GuideCell.words("Bind mounts (Gentoo)"),
                        GuideCell.code("/proc, /sys, /dev and /run were not bound into the new system (mount --rbind,"
                                + " --types proc)"),
                        GuideCell.words("Bind all four folders into the new system ([](jsc:mounting))."))
                .row(GuideCell.words("Profile (Gentoo)"),
                        GuideCell.code("no profile chosen (eselect profile list, then set)"),
                        GuideCell.words("Choose a profile ([](jsc:package_tree_and_world))."))
                .row(GuideCell.words("World update (Gentoo)"),
                        GuideCell.code("the system was not brought up to date (emerge --ask --verbose --update"
                                + " --deep --newuse @world)"),
                        GuideCell.words("Run the update the line names ([](jsc:package_tree_and_world))."))
                .row(GuideCell.words("Kernel link (Gentoo)"),
                        GuideCell.code("/usr/src/linux points nowhere (eselect kernel set 1)"),
                        GuideCell.words("Point /usr/src/linux at a kernel with eselect kernel set"
                                + " ([](jsc:kernel_gentoo))."))
                .row(GuideCell.words("Hardware clock (Arch)"),
                        GuideCell.code("the hardware clock was not set (hwclock --systohc)"),
                        GuideCell.words("Set the hardware clock with hwclock ([](jsc:arch_clock_and_name))."))
                .row(GuideCell.words("Machine name (both)"),
                        GuideCell.code("the machine has no name (echo <name> > /etc/hostname)"),
                        GuideCell.words("Give the machine a name, as the section below explains."))
                .subheading("The machine's name")
                .paragraph("A name has 1 to 15 characters. It uses letters, digits and hyphens, and it starts with a"
                        + " letter or a digit. You give it with the hostname command, or by writing it to"
                        + " /etc/hostname inside the new system. Either way, the checklist counts it.")
                .define("Reboot checklist", "The list of what the new system still needs, which the live shell"
                        + " builds before it restarts the machine.")
                .ifSomethingGoesWrong(
                        "hostname: the specified hostname is invalid",
                        "Use 1 to 15 letters, digits or hyphens, and start with a letter or a digit.",
                        "hostname: you cannot change the host name of the live medium",
                        "Enter the new system first ([](jsc:arch_chroot) on Arch, [](jsc:mounts_and_chroot) on"
                                + " Gentoo), then set the name there.")
                .register();
    }

    private static void willNotBootMessages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("will_not_boot_messages", section).titled("The refusals and their fixes")
                .paragraph("A refusal is the reboot command telling you that it did not restart the machine. Nothing"
                        + " is lost when it refuses: your live session stays open. Type the missing step, then run"
                        + " reboot again.")
                .paragraph("The lines appear in the live shell after you type reboot, each one exactly as shown in the"
                        + " table. The table gives what each line means and what you do about it.")
                .table("The lines the reboot command prints", "The line", "What it means", "The fix")
                .row(GuideCell.code("reboot: you are inside the new system. exit first."),
                        GuideCell.words("You are still inside the new system, entered with chroot."),
                        GuideCell.words("Type exit to leave the new system, then run reboot."))
                .row(GuideCell.code("The new system will not boot:"),
                        GuideCell.words("Items of the checklist are still missing, one for each line under it."),
                        GuideCell.words("Do the step that each line names ([](jsc:reboot_checklist)), then run reboot"
                                + " again."))
                .row(GuideCell.code("Rebooting into the new system ..."),
                        GuideCell.words("Nothing is missing. The installation is written to the disk, and the machine"
                                + " restarts."),
                        GuideCell.words("Nothing: the machine restarts."))
                .row(GuideCell.code("The installation could not be written to the disk (no space or no disk)."),
                        GuideCell.words("No disk has room for the system, or there is no disk to write to."),
                        GuideCell.words("Free space on the disk, or put a disk in the computer, then run reboot"
                                + " again."))
                .subheading("The reboot does not check the mounts")
                .paragraph("The reboot command does not check whether the new system's disk is still mounted. Only"
                        + " the checklist and the disk write decide. The Gentoo and Arch guides still tell you to"
                        + " unmount first with umount -R, so do that before you run reboot, as the guide says.")
                .paragraph("A chroot error about a root folder that does not exist comes from the chroot command."
                        + " It is not a refusal of the reboot.")
                .ifSomethingGoesWrong(
                        "reboot says you are inside the new system.",
                        "Type exit, which leaves the new system, then run reboot.",
                        "The disk message shows, with no space or no disk.",
                        "Free space on the disk, or put a disk in the computer. Then run reboot again.",
                        "The same list shows again after you did its steps.",
                        "Run that step again, read what it prints, then type reboot again.")
                .register();
    }

    private static void whatTheHandFilesDo(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("what_the_hand_files_do", section).titled("Which typed files reach the installed system")
                .paragraph("Much of a hand install happens in the live shell, in files you type or edit: the table,"
                        + " the name, the kernel, the bootloader's list. The installed system does not get all of"
                        + " them. The reboot checklist records the steps you ran, and on Gentoo it also reads the table"
                        + " you wrote.")
                .paragraph("While you work, the files you type go under the new system's root folder in the live"
                        + " session: /mnt on Arch, and /mnt/gentoo on Gentoo.")
                .subheading("What reaches the disk")
                .table("What the installation copies to the disk", "File", "Copied to the disk?")
                .row(GuideCell.words("The machine's name"), GuideCell.words("Yes: the name you chose."))
                .row(GuideCell.words("The packages you asked for"), GuideCell.words("Yes."))
                .row(GuideCell.code("/etc/fstab"), GuideCell.words("Yes: the table of what to mount."))
                .row(GuideCell.code("/etc/portage/make.conf"), GuideCell.words("Yes: the build options (Gentoo)."))
                .row(GuideCell.words("The kernel file you installed (/boot/vmlinuz-6.11.5-gentoo-x86_64 on Gentoo,"
                        + " /boot/vmlinuz-linux on Arch)"),
                        GuideCell.words("No: it is not copied."))
                .row(GuideCell.code("/boot/grub/grub.cfg"), GuideCell.words("No: the bootloader's list is not copied."))
                .paragraph("Then the installation writes its own boot files onto the disk: a folder named boot, and the"
                        + " kernel file boot/vmlinuz inside it. It writes them whatever you typed, so the machine can"
                        + " start even though the kernel file you installed is not copied.")
                .register();
    }

    private static void afterRebootChecks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("after_reboot_checks", section).titled("What the machine checks when it starts")
                .paragraph("When the new system starts, the machine runs the power-on self-test (POST) first. The"
                        + " firmware tests the hardware in it before it starts any system. At the end of the test,"
                        + " the machine checks the system disk once, and then it decides what to do next.")
                .paragraph("The check looks for two things on the disk: a folder named boot, and the kernel file"
                        + " boot/vmlinuz inside it. The kernel file is the one file that starts a Linux system. If"
                        + " both are there, the machine starts the system.")
                .table("How the machine decides", "What the check finds on the disk", "What you see", "What happens")
                .row(GuideCell.words("The folder boot and the kernel file are both there."),
                        GuideCell.words("The start goes on."),
                        GuideCell.words("The machine starts the system. With more than one system on the disk, it may"
                                + " show the Boot Menu first."))
                .row(GuideCell.words("There is no folder named boot, or no system on the disk at all, and no"
                        + " installation medium is in a drive."),
                        GuideCell.words("The firmware's no-device lines, below."),
                        GuideCell.words("The machine stops at the self-test and waits."))
                .row(GuideCell.words("The folder boot is there, but the kernel file is gone, and no installation"
                        + " medium is in a drive."),
                        GuideCell.code("kernel panic - not syncing: no init found"),
                        GuideCell.words("The machine stops at the self-test and waits, with the repair line below."))
                .subheading("The firmware's lines when nothing starts")
                .paragraph("The lines depend on the look of the machine's firmware, which follows its era. Linux"
                        + " systems start on Legacy machines and later.")
                .table("The firmware's look", "Firmware look", "What the screen shows")
                .row(GuideCell.words("Vintage"),
                        GuideCell.words("\"Non-system disk or disk error\", then \"Replace and press any key when"
                                + " ready\""))
                .row(GuideCell.words("Legacy and Transition"),
                        GuideCell.code("DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER"))
                .row(GuideCell.words("Standard, Advanced and later"),
                        GuideCell.words("\"No bootable device found\", then \"Press any key to enter Setup\""))
                .paragraph("When the machine stops with a complaint, the big header on the screen shows the complaint."
                        + " Without a complaint, the header reads \"No bootable device\". With a complaint, the screen"
                        + " also shows the line \"Put in an installation medium and install over it to repair.\"")
                .subheading("A machine that stops")
                .paragraph("A stopped machine keeps its message on the screen, and a monitor you open later still"
                        + " shows it. The machine checks the disk again once a second. It starts again by itself when"
                        + " the disk is whole, or when an installation medium is in a drive.")
                .paragraph("To repair a wrecked system, put an installation medium in a drive and install over the"
                        + " system ([](jsc:installing)). The install writes the folder boot and the kernel file back"
                        + " onto the disk, so the machine can start again.")
                .paragraph("If you take the installation medium out of the drive that the machine started from, the"
                        + " machine restarts and finds nothing to start from.")
                .define("Self-test (POST)", "The start-up test that the firmware runs first each time the computer"
                        + " starts, before any system starts.")
                .paragraph("A computer can also refuse to start for lack of power, which is a different refusal"
                        + " ([](jsc:power_supplies)).")
                .ifSomethingGoesWrong(
                        "The screen says \"kernel panic - not syncing: no init found\" and waits.",
                        "The kernel file boot/vmlinuz is gone from the disk. Install over the system from an"
                                + " installation medium ([](jsc:installing)).",
                        "The screen shows the firmware's no-device lines and waits.",
                        "The disk has no system, or its folder boot is gone. Install a system on the disk from an"
                                + " installation medium ([](jsc:installing)).")
                .seeAlso("jsc:firmware", "jsc:installing", "jsc:two_systems", "jsc:disks")
                .register();
    }
}
