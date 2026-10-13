/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the first chapter of Linux by Hand: what installing by hand means, what the machine needs before it
 * starts, the firmware of each era, the walkthrough on the install medium, and the medium's commands and Ctrl+C.
 * Each page links to the pages that go on from it, so no subject is written twice.
 */
final class BeforeYouBeginPages {

    private BeforeYouBeginPages() {
    }

    /** Declares the install-by-hand section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("by_hand_basics").titled("Installing by hand")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        installingByHand(guide, section);
        whatYouNeed(guide, section);
        firmwareForInstall(guide, section);
        theWalkthrough(guide, section);
        mediumShell(guide, section);
    }

    private static void installingByHand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("installing_by_hand", section).titled("Installing by hand").icon(ComputingModule.CD_ROM)
                .paragraph("Most systems install themselves. You put their medium in a drive, boot from it, and the"
                        + " installer does the work while it asks you a few questions. Arch Linux and Gentoo do not"
                        + " work that way. You type each step of their installation yourself, in order, and the"
                        + " system answers the way the real tool would.")
                .paragraph("The install medium is the floppy disk, compact disc (CD), digital versatile disc (DVD) or"
                        + " universal serial bus (USB) stick that a system is installed from. For these two systems"
                        + " it is a CD. When you boot it, the CD runs its own shell, a line where you type commands,"
                        + " and that running CD is the live medium.")
                .define("Install medium", "The floppy disk, compact disc (CD), digital versatile disc (DVD) or"
                        + " universal serial bus (USB) stick a system is installed from.")
                .define("Live medium", "An install medium that is running with its own shell. The shell comes from"
                        + " the medium, not from your disk, and nothing is installed until you type the steps.")
                .paragraph("The shell answers with the real tools' words, and it refuses what a real machine would"
                        + " refuse at that step. A mistake looks like a mistake on a real computer, so you learn the"
                        + " same reasons the real systems give.")
                .table("The two systems installed by hand", "System", "Shell on the medium", "How it is built")
                .row(GuideCell.words("Arch Linux"), GuideCell.code("zsh"),
                        GuideCell.words("The base system is installed from the network's package repository."))
                .row(GuideCell.words("Gentoo"), GuideCell.code("bash"),
                        GuideCell.words("The base system and the kernel are compiled from source first, which takes"
                                + " time."))
                .define("Arch Linux", "A Linux system you install by hand from its live medium, taking its base"
                        + " system from the network's package repository.")
                .define("Gentoo", "A Linux system you install by hand from its live medium, compiling its base"
                        + " system and its kernel from source.")
                .paragraph("The install ends when you type reboot. The shell refuses the reboot until the new system"
                        + " has everything it needs to start, and it lists what is missing. How long the steps take"
                        + " depends on your machine: a slow machine takes longer to compile (Gentoo) and to unpack"
                        + " (Arch Linux).")
                .paragraph("Before you boot the medium, check what you need ([](jsc:what_you_need)).")
                .ifSomethingGoesWrong("You expect an installer to start on its own, and a shell opens instead.",
                        "That shell is the live medium. Nothing is installed yet: read the walkthrough and type its"
                                + " steps, starting with help ([](jsc:the_walkthrough)).")
                .seeAlso("jsc:installing", "jsc:the_systems")
                .register();
    }

    private static void whatYouNeed(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("what_you_need", section).titled("What you need")
                .paragraph("Four things must be ready before you start. Each one has a refusal of its own, so the"
                        + " machine tells you which one is missing, and the table shows where each one comes from.")
                .define("Firmware", "The program on the motherboard that starts the computer, before any system"
                        + " runs.")
                .define("Era", "A generation of hardware, such as Vintage, Legacy or Standard. A computer has the era"
                        + " of its motherboard.")
                .define("Mainframe", "The computer that runs a network, which is every computer and device joined to it"
                        + " by data cable. It hosts the network's services, such as the Mirror.")
                .paragraph("First, the computer must be of Legacy era or newer. The era is the era of the motherboard"
                        + " in the computer, not of the parts you put on it. A Vintage computer cannot install either"
                        + " system.")
                .table("What you need", "Thing", "Where it comes from", "If it is missing")
                .row(GuideCell.words("A Legacy-era or newer motherboard"),
                        GuideCell.words("The motherboard in the computer."),
                        GuideCell.words("The firmware refuses: \"Arch Linux needs Legacy era hardware or newer; this"
                                + " machine is Vintage era.\""))
                .row(GuideCell.words("A disk in a disk slot"),
                        GuideCell.words("The computer's disk slots, named sda, sdb and so on in slot order."),
                        GuideCell.code("fdisk: cannot open /dev/sdb: No such file or directory"))
                .row(GuideCell.words("The Arch Linux or Gentoo CD in a CD Drive joined to the computer"),
                        GuideCell.words("The CD is in the Legacy creative tab, J's Computers - Legacy."),
                        GuideCell.words("The firmware says: \"No installation medium is in a drive linked to this"
                                + " machine.\""))
                .row(GuideCell.words("The Mirror, running on the network's Mainframe"),
                        GuideCell.words("Installed on the network with the command mirror install."),
                        GuideCell.words("The package steps fail, because the Mirror does not answer."))
                .define("Mirror", "The network's package repository, a service on the Mainframe. Every package"
                        + " manager of the network installs from it.")
                .paragraph("The package steps fetch from the Mirror, so they need it. It answers only while the"
                        + " Mainframe is running ([](jsc:mainframes)). Type mirror on its own to see how it stands:")
                .table("How the Mirror stands", "What it says", "What it means")
                .row(GuideCell.code("Mirror: not installed"),
                        GuideCell.words("Install it on the network with the command mirror install."))
                .row(GuideCell.code("Mirror: installed (Mainframe off)"),
                        GuideCell.words("It is installed, but the Mainframe is not running, so nothing answers."))
                .row(GuideCell.code("Mirror: serving"),
                        GuideCell.words("The Mirror answers, and the package steps can fetch."))
                .paragraph("Last, the machine must start from the medium. In the firmware's list, choose the CD. The"
                        + " machine restarts, runs its self-test again, and then the medium's shell comes up"
                        + " ([](jsc:firmware_for_install)).")
                .ifSomethingGoesWrong("The firmware's list shows \"Legacy era or newer\" beside the medium.",
                        "This machine is older than the medium needs. Put a Legacy-era or newer motherboard in the"
                                + " computer.",
                        "The firmware says there is no installation medium in a linked drive.",
                        "Put the CD in a CD Drive and join that drive to the computer ([](jsc:drives)).",
                        "The shell says the disk does not exist.",
                        "Put a disk in a disk slot. Then type lsblk to list the disks the shell can see"
                                + " ([](jsc:disks)).",
                        "The package steps fail, and the Mirror does not answer.",
                        "Type mirror to see how it stands. Install it with mirror install if it is not installed, and"
                                + " keep the Mainframe running.")
                .seeAlso("jsc:disks", "jsc:drives", "jsc:mainframes")
                .register();
    }

    private static void firmwareForInstall(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("firmware_for_install", section).titled("BIOS or UEFI")
                .icon(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150)
                .paragraph("The firmware comes in two kinds, and the era of your motherboard decides which one you"
                        + " have. The older kind is the basic input/output system (BIOS). The newer kind is the"
                        + " Unified Extensible Firmware Interface (UEFI). The install needs the difference, because"
                        + " each kind starts a disk in its own way.")
                .define("BIOS", "The older kind of firmware, in text or with a blue panel. It starts a disk from the"
                        + " very first part of it.")
                .define("Partition", "A part of a disk that the system treats as a disk of its own. One disk can hold"
                        + " several of these parts.")
                .define("UEFI", "The newer kind of firmware. It reads a small partition of the disk, formatted as FAT"
                        + " (File Allocation Table), to find the program that starts the system.")
                .table("The firmware of each era", "Era", "Firmware", "What the install needs")
                .row(GuideCell.words("Vintage"), GuideCell.words("Text BIOS"),
                        GuideCell.words("The whole disk: the start program goes on the disk itself."))
                .row(GuideCell.words("Legacy and Transition"), GuideCell.words("Blue BIOS"),
                        GuideCell.words("The whole disk: the start program goes on the disk itself."))
                .row(GuideCell.words("Standard, Advanced and later"), GuideCell.words("UEFI"),
                        GuideCell.words("A small FAT partition for the start program, made in the disk steps."))
                .paragraph("On a Vintage, Legacy or Transition computer the walkthrough is shorter. Its opening lines"
                        + " say so: \"On older firmware the root is the whole disk, /dev/sda, and the steps that say so"
                        + " below are left out.\"")
                .paragraph("On a Standard or later computer, make the partition first. The walkthrough marks the"
                        + " steps for modern firmware, and you type them in order: a new table, a 512 MB partition,"
                        + " its type set to EFI System (type 1), and then the partition formatted as FAT. The chapter"
                        + " [The disks](jsc:preparing_disks) shows each step.")
                .paragraph("Both kinds take the same keys while the computer starts. [[DEL]] enters the Setup, the"
                        + " firmware's settings, and [[F12]] opens the Boot Menu ([](jsc:firmware)).")
                .ifSomethingGoesWrong("The bootloader step refuses on a Standard or later computer.",
                        "The start program needs the FAT partition, mounted where that step says. Make the partition"
                                + " first, as the walkthrough's modern steps show ([](jsc:the_walkthrough)).")
                .seeAlso("jsc:firmware", "jsc:the_systems")
                .register();
    }

    private static void theWalkthrough(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("the_walkthrough", section).titled("The walkthrough on the medium")
                .paragraph("The medium carries its own walkthrough: the steps of the distribution's guide, written out"
                        + " in order, as a plain file. You do not need a guide from outside the game to follow it.")
                .define("Tool", "A command that takes time and holds the terminal until it ends, such as pacstrap, or"
                        + " emerge on Gentoo.")
                .paragraph("The walkthrough is kept in /root/install.txt, on the medium and not on your disk, so it is"
                        + " there whatever the disk holds. To find it and read it:")
                .steps("Type help and press [[Enter]].",
                        "Type less /root/install.txt and press [[Enter]].",
                        "Read the step you are on, then type it at the prompt.",
                        "Check what the shell answered, and only then type the next step.")
                .paragraph("The walkthrough lists the commands in order. Some steps carry a mark, and the table shows"
                        + " what each mark means.")
                .table("Marks in the walkthrough", "Mark", "What it means")
                .row(GuideCell.code("*"), GuideCell.words("A step the system cannot boot without."))
                .row(GuideCell.code("modern firmware only"),
                        GuideCell.words("A note under a step: the step is for a Standard or later computer. On older"
                                + " firmware it is left out."))
                .paragraph("The medium greets you when it starts: \"Type 'help' for the installation walkthrough.\""
                        + " It logs you in as root on its own, so you type no name and no password.")
                .subheading("What help prints")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("root@archiso ~ # ", "help")
                .answer("This is the Arch Linux installation medium.",
                        "Nothing here installs itself: the steps are typed by hand, in",
                        "order, and every tool is the real one.",
                        "",
                        "The whole walkthrough is in /root/install.txt",
                        "    less /root/install.txt",
                        "A tool that is running can be stopped with CTRL+C.")
                .paragraph("On Gentoo, the same help names Gentoo, and the prompt reads livecd instead of"
                        + " root@archiso.")
                .subheading("The every-step settings")
                .paragraph("The server can ask for more before a reboot. Each distribution has one setting, off by"
                        + " default, kept in jscomputers-server.toml beside the world. The table lists what each one"
                        + " adds.")
                .table("The every-step settings", "Setting", "Default", "When it is on, reboot also asks for")
                .row(GuideCell.words("Every step of Arch"), GuideCell.words("Off"),
                        GuideCell.words("The hardware clock set, and a name for the machine."))
                .row(GuideCell.words("Every step of Gentoo"), GuideCell.words("Off"),
                        GuideCell.words("The bind mounts, a profile, the @world update, the kernel link and a name for"
                                + " the machine."))
                .paragraph("These settings change only what reboot refuses. Every command still answers the way the"
                        + " real tool does, so you type the same commands either way. The basic list that reboot always"
                        + " checks is in [When the reboot refuses](jsc:when_reboot_refuses).")
                .ifSomethingGoesWrong("reboot answers: \"The new system will not boot:\", with a list under it.",
                        "Each line names what is missing and the step that provides it, in brackets. Do that step,"
                                + " then type reboot again.",
                        "reboot answers: \"reboot: you are inside the new system. exit first.\"",
                        "Type exit to leave the new system, then type reboot.")
                .seeAlso("jsc:installing", "jsc:the_systems")
                .register();
    }

    private static void mediumShell(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("medium_shell", section).titled("The medium's commands and Ctrl+C")
                .paragraph("The shell of the medium answers a fixed list of commands. Most are the real tools of the"
                        + " installation, with their real names. A few, such as help, exit and reboot, are the medium's"
                        + " own.")
                .paragraph("A word the shell does not know gets the refusal of the system the shell belongs to: bash"
                        + " on Gentoo, zsh on Arch Linux. Each system has its own commands, so a command of the other"
                        + " system is refused.")
                .table("Commands on both systems", "Group", "Commands")
                .row(GuideCell.words("Files and folders"),
                        GuideCell.code("ls, cat, less, more, cd, echo, nano, mkdir"))
                .row(GuideCell.words("Disks"),
                        GuideCell.code("lsblk, blkid, fdisk, mkfs, mkfs.ext4, mkfs.fat, mkfs.vfat, mount, umount"))
                .row(GuideCell.words("Entering the new system"), GuideCell.code("chroot"))
                .row(GuideCell.words("Settings"), GuideCell.code("source, export, env-update, hwclock, hostname"))
                .row(GuideCell.words("Start program"), GuideCell.code("grub-install, grub-mkconfig"))
                .row(GuideCell.words("The medium's own"), GuideCell.code("help, exit, reboot"))
                .table("Commands of one system", "System", "Its own commands")
                .row(GuideCell.words("Arch Linux"),
                        GuideCell.code("pacstrap, genfstab, pacman, mkinitcpio, arch-chroot"))
                .row(GuideCell.words("Gentoo"),
                        GuideCell.code("wget, tar, emerge-webrsync, emerge, eselect, genkernel, make"))
                .paragraph("The prompt tells you where you are. Outside the new system it shows the medium's name, and"
                        + " inside the new system it changes as the table shows. The ~ is the root's own folder,"
                        + " and any other folder shows its own path.")
                .table("The prompt", "System", "Outside the new system", "Inside the new system")
                .row(GuideCell.words("Arch Linux"), GuideCell.code("root@archiso ~ #"),
                        GuideCell.code("[root@archiso ~]#"))
                .row(GuideCell.words("Gentoo"), GuideCell.code("livecd ~ #"), GuideCell.code("(chroot) livecd ~ #"))
                .subheading("Entering and leaving the new system")
                .paragraph("You step into the new system with chroot on either system, or with arch-chroot on Arch"
                        + " Linux only. Once inside, the commands and the files you use are the new system's. Type exit"
                        + " to step back out. Outside the new system, exit prints logout.")
                .subheading("The partition editor")
                .paragraph("fdisk opens a partition editor that takes over the terminal. Each command is a letter, with"
                        + " an optional value after it. You type it on its own line and press [[Enter]]. The chapter"
                        + " [The disks](jsc:preparing_disks) takes you through a whole session in this editor.")
                .table("Commands of the partition editor", "Command", "What it does")
                .row(GuideCell.code("m"), GuideCell.words("Shows the editor's help, the same as typing help."))
                .row(GuideCell.code("g"),
                        GuideCell.words("Makes a new, empty partition table (GUID Partition Table, GPT)."))
                .row(GuideCell.code("p"), GuideCell.words("Prints the table as it is now."))
                .row(GuideCell.code("n"),
                        GuideCell.words("Makes a new partition, and n 512M gives it 512 MB on the same line."))
                .row(GuideCell.code("t"), GuideCell.words("Sets the type of a partition: t 1 marks it for UEFI."))
                .row(GuideCell.code("d"), GuideCell.words("Deletes a partition."))
                .row(GuideCell.code("w"),
                        GuideCell.words("Writes the table to the disk, the only command that changes the disk."))
                .row(GuideCell.code("q"),
                        GuideCell.words("Quits without writing, and throws away everything typed since the editor"
                                + " opened."))
                .subheading("Ctrl+C")
                .paragraph("Press [[Ctrl]]+[[C]] to stop the tool that is running. The prompt comes back, and the part"
                        + " of the work the tool had not finished stays not done.")
                .paragraph("With no tool running, [[Ctrl]]+[[C]] does nothing. The tools that take time are pacstrap,"
                        + " pacman, emerge, emerge-webrsync, genkernel, make, wget, tar, grub-install, grub-mkconfig,"
                        + " fdisk and the mkfs tools. Their output appears as it runs.")
                .warning("In fdisk, [[Ctrl]]+[[C]] throws away everything typed since the editor opened, the same as"
                        + " q. Only w writes the disk.")
                .ifSomethingGoesWrong("bash: pacstrap: command not found (Gentoo), or zsh: command not found: wget"
                                + " (Arch Linux).",
                        "The word is not a command of your system. Check the list of commands above: pacstrap belongs"
                                + " to Arch Linux, and wget to Gentoo.",
                        "emerge: command not found, and the message says the package manager is the new system's.",
                        "Step into the new system first, with chroot /mnt/gentoo /bin/bash, then type emerge there"
                                + " ([the chapter Gentoo](jsc:gentoo)).",
                        "mkinitcpio: command not found, and the message says it is the new system's.",
                        "Step into the new system first, with arch-chroot /mnt, then type mkinitcpio"
                                + " ([the chapter Arch](jsc:arch)).",
                        "grub-install: command not found, and the message says it is the new system's.",
                        "Step into the new system first, then type the command again.",
                        "chroot: cannot change root directory to '/mnt': No such file or directory",
                        "The folder is not the root of the new system. Mount the root partition at /mnt on Arch Linux,"
                                + " or at /mnt/gentoo on Gentoo, first ([the chapter The disks](jsc:preparing_disks)).",
                        "chroot: failed to run command '/bin/bash': No such file or directory",
                        "The base system is not installed yet. Install it first, with pacstrap on Arch Linux or with"
                                + " wget and tar on Gentoo.",
                        "fdisk: cannot open /dev/sdc: No such file or directory",
                        "There is no such disk. Type lsblk to list the disks the shell can see.",
                        "nano: /mnt/gentoo is a directory",
                        "nano opens files, not folders. Name a file after nano.")
                .seeAlso("jsc:installing", "jsc:firmware_for_install")
                .register();
    }
}
