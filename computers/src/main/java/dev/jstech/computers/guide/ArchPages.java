/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.man.CommandGuidePages;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the Arch chapter of Linux by Hand: the base system, the file system table, stepping into the new
 * system, the clock and the name, the boot images, the boot loader and its boot list, the reboot, and the package
 * manager inside the new system.
 */
final class ArchPages {

    private ArchPages() {
    }

    /** Declares the sections and entries of the Arch install, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        baseSystem(guide, chapter);
        insideTheNewSystem(guide, chapter);
        booting(guide, chapter);
        finishing(guide, chapter);
    }

    private static void baseSystem(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("arch_base").titled("The base system")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        installingTheBaseSystem(guide, section);
        writingTheFileSystemTable(guide, section);
    }

    private static void installingTheBaseSystem(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_base_system", section).titled("Installing the base system (pacstrap)")
                .icon(ComputingModule.CD_ROM)
                .paragraph("The base system is the core of Arch Linux: the kernel ([](jsc:kernel_gentoo)), the core"
                        + " tools and the package manager, pacman. The command that installs it is pacstrap. It puts"
                        + " the packages on the disk, under /mnt, the folder where the new system is built.")
                .paragraph("Pacstrap is the first step that writes the new system, so the disk must be ready before it"
                        + " runs. The root partition has to be mounted on /mnt ([](jsc:mounting)).")
                .paragraph("On a real Arch install, pacstrap downloads the packages from the internet. Here the"
                        + " packages come from the Mirror, the package server of your network. A Mainframe must run"
                        + " it, and this computer must be on that network. Without a Mirror, pacstrap cannot start.")
                .define("pacman", "The package manager of Arch Linux: it finds, downloads and installs packages.")
                .define("Mirror", "The package server of a network. A Mainframe that runs it serves packages to every"
                        + " computer of the network.")
                .steps("Make sure the root partition is mounted on /mnt.",
                        "Type pacstrap -K /mnt base linux linux-firmware and press [[Enter]].",
                        "Wait until the prompt comes back. The step takes as long as your connection and your disk"
                                + " need.")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("root@archiso ~ # ", "pacstrap -K /mnt base linux linux-firmware")
                .answer("==> Creating install root at /mnt", "==> Installing packages to /mnt")
                .paragraph("Then come the download bars, the checks of the packages, one line for each package it"
                        + " installs, and the last steps of the install. Pacstrap asks you nothing: it does not stop to"
                        + " ask you to confirm, and it installs straight away.")
                .table("What the base system brings")
                .property("Packages installed", "48")
                .property("Kernel release", "6.11.5-arch1-1")
                .paragraph("The 48 packages include base, linux, linux-firmware, pacman, systemd, bash, coreutils and"
                        + " mkinitcpio. The last steps of the install also build both boot images, so you do not need"
                        + " to build them again.")
                .paragraph("When pacstrap finishes, the new system has its folders for /etc, /boot, /root, /usr, /var,"
                        + " /proc, /sys, /dev and /run. Its kernel and its two boot images"
                        + " ([](jsc:arch_mkinitcpio)) are in /boot. The step is recorded as done only at that moment.")
                .paragraph("Running pacstrap again starts over. The steps you did before, such as the file system"
                        + " table and the boot steps, are forgotten, and you do them again. The clock setting is kept.")
                .paragraph("You can stop pacstrap with [[Ctrl]]+[[C]]. A stopped run is not recorded as done, so"
                        + " you run it again from the start.")
                .ifSomethingGoesWrong(
                        "The screen says \"==> ERROR: /mnt is not a mountpoint!\".",
                        "Nothing is mounted on /mnt. Mount the root partition there, after its file system is made"
                                + " ([](jsc:mounting)): mount /dev/sda2 /mnt.",
                        "The screen says \"error: failed retrieving file 'core.db' from mirror://mainframe : Could not"
                                + " resolve host: mainframe\", and then \"error: failed to synchronize all databases"
                                + " (unexpected error)\".",
                        "This computer cannot reach a Mirror. Put it on a network whose Mainframe runs the Mirror"
                                + " ([](jsc:mirror)).",
                        "You stopped pacstrap with [[Ctrl]]+[[C]] before it finished.",
                        "The step is not recorded as done. Run pacstrap again from the start.")
                .seeAlso("jsc:arch_fstab")
                .register();
    }

    private static void writingTheFileSystemTable(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_fstab", section).titled("Writing the file system table (genfstab)")
                .icon(ComputingModule.CD_ROM)
                .paragraph("A system must know which disk to mount, and where, every time it starts. That list lives in"
                        + " one file, /etc/fstab, the file system table. The tool genfstab writes that file for you,"
                        + " from the disks you mounted.")
                .paragraph("Each line names a partition by its universally unique identifier (UUID), which the file"
                        + " system carries on its disk. The UUID stays the same when the disk gets another letter, so"
                        + " a renamed disk still boots. Here the same disk always gives the same UUID.")
                .steps("Make sure the base system is installed.",
                        "Type genfstab -U /mnt and press [[Enter]]. The table is printed, and nothing is written.",
                        "Type genfstab -U /mnt >> /mnt/etc/fstab and press [[Enter]]. The table is written into the"
                                + " file.")
                .paragraph("Print the table first if you want to check it. The printed table is the same text the"
                        + " redirection writes, so what you see is what the file gets.")
                .table("The lines of the table", "Line", "What it says")
                .row(GuideCell.code("# /dev/sda2"), GuideCell.words("A comment: the partition the line is about."))
                .row(GuideCell.code("UUID="), GuideCell.words("The partition's UUID, then the folder it mounts on."))
                .row(GuideCell.code("/"),
                        GuideCell.words("The folder the partition mounts on: the top of the system."))
                .row(GuideCell.code("ext4"), GuideCell.words("The file system type of the partition."))
                .row(GuideCell.code("rw,relatime"), GuideCell.words("The options: the disk is read and written, and"
                        + " access times are recorded in a lighter way."))
                .paragraph("If you mounted the boot partition, a second line follows after a blank line. It names that"
                        + " partition by its serial number, the identifier of the File Allocation Table (FAT) file"
                        + " system, and the folder it is mounted on, such as /boot.")
                .define("fstab", "The file in /etc that lists which partitions to mount where, and how, when the system"
                        + " starts.")
                .define("UUID", "A long identifier that a file system carries on its disk, so the system finds the"
                        + " partition whatever its device name is.")
                .ifSomethingGoesWrong(
                        "The screen says \"usage: genfstab [options] root\".",
                        "The command does not name /mnt. Type genfstab -U /mnt >> /mnt/etc/fstab.",
                        "The screen says \"==> ERROR: /mnt/etc does not exist\".",
                        "Pacstrap has not run yet. Install the base system first ([](jsc:arch_base_system)).")
                .seeAlso("jsc:arch_chroot")
                .register();
    }

    private static void insideTheNewSystem(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("arch_system").titled("Inside the new system")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        steppingIntoTheNewSystem(guide, section);
        clockAndName(guide, section);
        bootImages(guide, section);
    }

    private static void steppingIntoTheNewSystem(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_chroot", section).titled("Stepping into the new system (arch-chroot)")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Until now you have typed on the live medium, and your commands reached the disk from the"
                        + " outside. To finish the new system, you step inside it. From then on, the folder /mnt is the"
                        + " top of the system, and every path you type means a path inside the new system.")
                .paragraph("The Arch tool for this is arch-chroot. Running a program with a different folder as the top"
                        + " of the disk is called a chroot, and arch-chroot makes one for the new system.")
                .define("chroot", "Running a program with a different folder as the top of the disk, so that the"
                        + " program sees that folder as its whole system.")
                .paragraph("Outside the new system, the prompt reads root@archiso ~ #. Inside, it reads"
                        + " [root@archiso /]#, with the folder you are in between the brackets.")
                .steps("Type arch-chroot /mnt and press [[Enter]].",
                        "Check the prompt. It must read [root@archiso /]#.",
                        "Do the steps of this chapter inside. Type exit when you are done.")
                .paragraph("If you name a folder other than /mnt, the tool refuses, and you stay outside:")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("root@archiso ~ # ", "arch-chroot /mnt/x")
                .answer("chroot: cannot change root directory to '/mnt/x': No such file or directory")
                .paragraph("Leaving is done with exit. Nothing is lost when you leave: the work you did stays, and"
                        + " arch-chroot /mnt takes you back in.")
                .table("Commands that work only inside the new system", "Command", "On the live medium")
                .row(GuideCell.code("pacman"), GuideCell.words("Refused: you are not root in the new system yet."))
                .row(GuideCell.code("mkinitcpio"), GuideCell.words("Not found: step in first."))
                .row(GuideCell.code("grub-install"), GuideCell.words("Not found: step in first."))
                .row(GuideCell.code("grub-mkconfig"), GuideCell.words("Not found: step in first."))
                .row(GuideCell.code("hostname"), GuideCell.words("Refused: you cannot change the name of the live"
                        + " medium."))
                .ifSomethingGoesWrong(
                        "The screen says \"chroot: cannot change root directory to '/mnt/x': No such file or"
                                + " directory\".",
                        "The folder is not /mnt. Type arch-chroot /mnt.",
                        "The screen says \"chroot: failed to run command '/bin/bash': No such file or directory\".",
                        "The base system is not on the disk yet, so there is no shell to start. Install it first"
                                + " ([](jsc:arch_base_system)).",
                        "The screen says \"error: you cannot perform this operation unless you are root in the new\""
                                + " and then \"system (arch-chroot /mnt first)\".",
                        "You typed pacman on the live medium. Step in first with arch-chroot /mnt.",
                        "The screen says \"mkinitcpio: command not found (it is the new system's: arch-chroot /mnt"
                                + " first)\".",
                        "You are on the live medium. Step in first with arch-chroot /mnt.")
                .seeAlso("jsc:arch_clock_and_name")
                .register();
    }

    private static void clockAndName(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_clock_and_name", section).titled("The clock and the machine's name")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Two settings make the new system yours: the clock and the machine's name. The installer"
                        + " asks for both before the reboot only when the server setting Every step of Arch is on"
                        + " ([](jsc:installing)). You can always set them with the commands below.")
                .paragraph("The command hwclock --systohc copies the system's clock into the hardware clock,"
                        + " so the time is kept while the computer is off.")
                .define("hardware clock", "The clock on the motherboard, which keeps the time while the computer is"
                        + " off.")
                .steps("Type hwclock --systohc and press [[Enter]].",
                        "Type echo jarch > /etc/hostname, with your own name in place of jarch, and press [[Enter]].",
                        "Type hostname and press [[Enter]]. It prints the name you chose.")
                .table("The rules for the machine's name", "Rule", "What it says")
                .row(GuideCell.words("Length"), GuideCell.words("At most 15 characters."))
                .row(GuideCell.words("First character"), GuideCell.words("A letter or a digit."))
                .row(GuideCell.words("Other characters"), GuideCell.words("Letters, digits and hyphens."))
                .row(GuideCell.words("Not allowed"), GuideCell.words("Dots, underscores and spaces."))
                .paragraph("The name goes with the new system when the install finishes. The installed machine"
                        + " keeps it, and the network shows it.")
                .ifSomethingGoesWrong(
                        "The screen says \"hostname: you cannot change the host name of the live medium\".",
                        "You are on the live medium, not in the new system. Step in with arch-chroot /mnt first"
                                + " ([](jsc:arch_chroot)).",
                        "The screen says \"hostname: the specified hostname is invalid\".",
                        "The name breaks a rule in the table, or it has more than one word. Use letters, digits and"
                                + " hyphens, at most 15 characters, starting with a letter or a digit.")
                .seeAlso("jsc:arch_mkinitcpio")
                .register();
    }

    private static void bootImages(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_mkinitcpio", section).titled("Rebuilding the boot images (mkinitcpio)")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Before the real system starts, the kernel needs a small temporary system that loads the"
                        + " drivers for the disks. That system is the initramfs. Arch builds two of them, called boot"
                        + " images: a normal one, and a fallback for when the normal one does not start.")
                .define("initramfs", "A small temporary system that starts before the real one, to load the drivers"
                        + " for the disks.")
                .paragraph("The tool that builds them is mkinitcpio. Type mkinitcpio -P to build both images again. The"
                        + " tool works on the new system, so you must be inside it.")
                .steps("Make sure you are inside the new system.",
                        "Type mkinitcpio -P and press [[Enter]].",
                        "Wait until the prompt comes back.")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("[root@archiso /]# ", "mkinitcpio -P")
                .answer("==> Building image from preset: /etc/mkinitcpio.d/linux.preset: 'default'",
                        "==> Using default configuration file: '/etc/mkinitcpio.conf'",
                        "  -> -k /boot/vmlinuz-linux -g /boot/initramfs-linux.img",
                        "==> Starting build: '6.11.5-arch1-1'")
                .paragraph("Each image is built in steps called hooks. There are twelve: base, udev, autodetect,"
                        + " microcode, modconf, kms, keyboard, keymap, consolefont, block, filesystems and fsck.")
                .define("hook", "One step of building a boot image.")
                .paragraph("The fallback image skips the step autodetect. Pacstrap has already built both images in its"
                        + " last steps, so this command is for when you want them built again. On a slow processor it"
                        + " takes longer.")
                .ifSomethingGoesWrong(
                        "The screen says \"mkinitcpio: command not found (it is the new system's: arch-chroot /mnt"
                                + " first)\".",
                        "You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).")
                .register();
    }

    private static void booting(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("arch_boot").titled("Booting it")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        installingTheBootLoader(guide, section);
        writingTheBootList(guide, section);
    }

    private static void installingTheBootLoader(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_bootloader", section).titled("Installing the boot loader")
                .icon(ComputingModule.CD_ROM)
                .paragraph("When the firmware starts a disk, it starts the first program on it: the boot"
                        + " loader. The boot loader then starts the kernel with the boot images. Arch uses GRUB"
                        + " for that job. GRUB shows a menu, or starts its only entry, and loads the kernel.")
                .define("firmware", "The program on the motherboard that starts the computer, before any system runs.")
                .define("boot loader", "The program the firmware starts first, which then starts the kernel.")
                .define("GRUB", "The boot loader that Arch uses.")
                .paragraph("The boot loader comes as two packages, and you install both with pacman. The efibootmgr"
                        + " package is the tool that tells the firmware where the boot loader is. The grub-install"
                        + " command refuses until the grub package is installed.")
                .table("The packages for the boot loader", "Package", "Size")
                .row(GuideCell.code("grub"), GuideCell.words("7.8 MiB"))
                .row(GuideCell.code("efibootmgr"), GuideCell.words("0.1 MiB"))
                .steps("Make sure you are inside the new system.",
                        "Type pacman -S grub efibootmgr and press [[Enter]].",
                        "Press [[Enter]] or type Y at the question, and wait until the install finishes.",
                        "Type the grub-install command for your firmware, from the table below, and press [[Enter]].")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("[root@archiso /]# ", "pacman -S grub efibootmgr")
                .answer(":: Proceed with installation? [Y/n]")
                .paragraph("There are two kinds of firmware: the older Basic Input/Output System (BIOS) and the newer"
                        + " Unified Extensible Firmware Interface (UEFI). The table shows the install for each.")
                .table("The install for each firmware", "Firmware", "Command", "Platform it reports")
                .row(GuideCell.words("UEFI, in the Standard era and later"),
                        GuideCell.code("grub-install --efi-directory=/boot"), GuideCell.code("x86_64-efi"))
                .row(GuideCell.words("BIOS, in the Legacy and Transition eras"),
                        GuideCell.code("grub-install /dev/sda"), GuideCell.code("i386-pc"))
                .paragraph("Which row is yours depends on your computer's firmware"
                        + " ([](jsc:firmware_for_install)). On older firmware, the boot loader goes on the whole"
                        + " disk, not in a partition. On UEFI, mount the EFI system partition at /mnt/boot before"
                        + " you step in ([](jsc:mounting)), so that /boot inside the new system is that partition.")
                .paragraph("On UEFI the screen says \"Installing for x86_64-efi platform.\", and on BIOS it says"
                        + " \"Installing for i386-pc platform.\". Both end with \"Installation finished. No error"
                        + " reported.\" The install does not write the boot list. That is the next step"
                        + " ([](jsc:arch_grub_config)).")
                .ifSomethingGoesWrong(
                        "The screen says \"zsh: command not found: grub-install\", with a line that says the boot"
                                + " loader is a package.",
                        "The GRUB package is not installed yet. Install it first: pacman -S grub efibootmgr.",
                        "The screen says \"grub-install: command not found (it is the new system's: step into it"
                                + " first)\".",
                        "You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).",
                        "The screen says \"grub-install: error: /boot/efi doesn't look like an EFI partition.\"",
                        "The EFI system partition is not mounted where the command looks. Mount it at /mnt/boot before"
                                + " you step in ([](jsc:mounting)), then run grub-install --efi-directory=/boot.",
                        "The screen says \"grub-install: error: install device isn't specified.\"",
                        "On older firmware, name the disk: grub-install /dev/sda.",
                        "The screen says \"grub-install: error: cannot find a device for\" and the name of a partition,"
                                + " such as /dev/sda2.",
                        "The boot loader goes on the disk, not in a partition. Name the disk: grub-install /dev/sda.")
                .seeAlso("jsc:arch_grub_config")
                .register();
    }

    private static void writingTheBootList(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_grub_config", section).titled("Writing the boot list (grub-mkconfig)")
                .icon(ComputingModule.CD_ROM)
                .paragraph("The boot loader needs a list of what to start. That list is a file, and the tool"
                        + " grub-mkconfig writes it. It finds the kernel and the boot images in /boot, and writes one"
                        + " menu entry for them.")
                .define("grub.cfg", "The file the boot loader reads when it starts. It says which kernel to start, and"
                        + " how.")
                .paragraph("The file is always written to /boot/grub/grub.cfg, whatever you type after -o.")
                .steps("Make sure you are inside the new system.",
                        "Type grub-mkconfig -o /boot/grub/grub.cfg and press [[Enter]].",
                        "Wait for done at the end of the output.")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("[root@archiso /]# ", "grub-mkconfig -o /boot/grub/grub.cfg")
                .answer("Generating grub configuration file ...",
                        "Found linux image: /boot/vmlinuz-linux", "Found initrd image: /boot/initramfs-linux.img",
                        "Warning: os-prober will not be executed to detect other bootable",
                        "partitions. Systems on them will not be added to the GRUB boot",
                        "configuration. Check GRUB_DISABLE_OS_PROBER documentation entry.", "done")
                .paragraph("The first line of the output says the configuration file is being generated. The warning is"
                        + " expected. os-prober is a tool that looks for other systems on other disks, and here it"
                        + " does not run, so no other system gets an entry. On UEFI, the output also says it adds a"
                        + " boot menu entry for the firmware settings.")
                .paragraph("The file has one menu entry, named Arch Linux. It loads the kernel and the boot"
                        + " images, and it tells the kernel which partition is the root, by its UUID.")
                .ifSomethingGoesWrong(
                        "The screen says \"/usr/bin/grub-mkconfig: /boot/grub: No such file or directory\".",
                        "The boot loader is not installed yet, so its folder does not exist. Install it first"
                                + " ([](jsc:arch_bootloader)).",
                        "The screen says \"Usage: grub-mkconfig -o /boot/grub/grub.cfg\".",
                        "The command has no -o. Type grub-mkconfig -o /boot/grub/grub.cfg.",
                        "The screen says \"grub-mkconfig: command not found (it is the new system's: step into it"
                                + " first)\".",
                        "You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).")
                .seeAlso("jsc:arch_reboot")
                .register();
    }

    private static void finishing(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("arch_finish").titled("Finishing")
                .icon(ComputingModule.CD_ROM).in(chapter).register();
        checkingAndRebooting(guide, section);
        installingPackagesWithPacman(guide, section);
    }

    private static void checkingAndRebooting(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_reboot", section).titled("Checking and rebooting")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Before it reboots, the installer checks that the new system can start. If a part"
                        + " is missing, the machine lists what is missing, and it refuses to reboot until you add"
                        + " the part.")
                .paragraph("When the check passes, the installer copies the new system onto the disk that holds /mnt."
                        + " Then the machine reboots into it. The copy also takes the file system table you wrote, the"
                        + " name you chose, and the programs you asked for with pacman -S.")
                .steps("Type exit and press [[Enter]]. The prompt shows the live medium again.",
                        "Type reboot and press [[Enter]].")
                .table("What the check looks for", "Missing part", "The message it prints")
                .row(GuideCell.words("The base system"), GuideCell.code("no base system installed (pacstrap)"))
                .row(GuideCell.words("The file system table"),
                        GuideCell.code("no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)"))
                .row(GuideCell.words("The boot loader"), GuideCell.code("no bootloader (grub-install)"))
                .row(GuideCell.words("The boot list"),
                        GuideCell.code("nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)"))
                .paragraph("The boot images are not on this list. If the server setting Every step of Arch is on, the"
                        + " list also checks the hardware clock and the machine's name ([](jsc:arch_clock_and_name)).")
                .paragraph("If a part is missing, the reboot stops and lists what is missing:")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("root@archiso ~ # ", "reboot")
                .answer("The new system will not boot:", "  - no bootloader (grub-install)",
                        "  - nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)")
                .paragraph("After a refusal, do each listed step, in this chapter, and then reboot again. The firmware"
                        + " starts the installed disk first, and the boot list shows the entry Arch Linux.")
                .ifSomethingGoesWrong(
                        "The screen says \"reboot: you are inside the new system. exit first.\"",
                        "You are still inside the new system. Type exit, then reboot.",
                        "The screen says \"The new system will not boot:\" and lists the missing parts.",
                        "Do each listed step, then reboot again.",
                        "The screen says \"The installation could not be written to the disk (no space or no disk).\"",
                        "The disk that holds /mnt has no room for the system, or it is not there. Check the disk, then"
                                + " install again.")
                .seeAlso("jsc:when_reboot_refuses")
                .register();
    }

    private static void installingPackagesWithPacman(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("arch_pacman_in_system", section).titled("Installing packages with pacman")
                .icon(ComputingModule.CD_ROM)
                .paragraph("Inside the new system, pacman -S installs programs from the Mirror, so that they are on the"
                        + " new system when it starts. A program you install this way is a package, with its files and"
                        + " the names of what it needs.")
                .paragraph("Type pacman -S, a space, and then the names of the programs you want. Each name is"
                        + " the command name of a program that the Mirror has for Linux.")
                .steps("Make sure you are inside the new system.",
                        "Type pacman -S and the names, for example pacman -S grub efibootmgr.",
                        "Answer the question with Y or press [[Enter]] to install, or type n to stop.")
                .paragraph("Pacman asks \"Proceed with installation? [Y/n]\" before it changes anything. An answer that"
                        + " starts with n stops it, and nothing is installed. Any other answer, even an empty one, goes"
                        + " on.")
                .session("jsc:guide/terminal/zsh", "Arch Linux")
                .typed("[root@archiso /]# ", "pacman -S efibootmgr")
                .answer(":: Proceed with installation? [Y/n]")
                .paragraph("Each program you name is remembered, and it is installed on the new system when the install"
                        + " finishes. The package grub is also what the boot loader needs ([](jsc:arch_bootloader)).")
                .subheading("pacman on the system you installed")
                .paragraph("Once Arch Linux starts from your own disk, pacman stays as its package manager. Then pacman"
                        + " -S installs a package at once, and pacman has words to search, list, update and remove"
                        + " your packages too. Each of them reaches the Mirror over the network.")
                .table("What pacman does on the installed system", "Words", "What they do")
                .row(GuideCell.code("pacman -S <package>"), GuideCell.words("Installs the package from the Mirror."))
                .row(GuideCell.code("pacman -Ss [term]"),
                        GuideCell.words("Searches the Mirror for packages whose names hold the term."))
                .row(GuideCell.code("pacman -Q"), GuideCell.words("Lists the packages installed on this computer."))
                .row(GuideCell.code("pacman -Sy"), GuideCell.words("Reads the Mirror's lists again."))
                .row(GuideCell.code("pacman -Syu"),
                        GuideCell.words("Brings every installed package up to the version the Mirror serves."))
                .row(GuideCell.code("pacman -R <package>"), GuideCell.words("Removes the package."))
                .command(CommandGuidePages.id(Platform.LINUX, "pacman"))
                .ifSomethingGoesWrong(
                        "The screen says \"error: target not found:\" and then the name you typed.",
                        "The Mirror has no program by that name for Linux, or this machine cannot run it. Check the"
                                + " spelling, and the programs on the Mirror ([](jsc:programs)).",
                        "The screen says \"error: no targets specified (use -h for help)\".",
                        "You typed only flags, and no name. Add the name of a program after -S.",
                        "The screen says \"error: no operation specified (use -h for help)\".",
                        "The command must start with -S and name a program. Type pacman -S, a space, then the name.",
                        "The screen says \"error: you cannot perform this operation unless you are root in the new\""
                                + " and then \"system (arch-chroot /mnt first)\".",
                        "You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).",
                        "The screen says \"error: failed retrieving file 'core.db' from mirror://mainframe : Could not"
                                + " resolve host: mainframe\".",
                        "This computer cannot reach a Mirror. Put it on a network whose Mainframe runs the Mirror"
                                + " ([](jsc:mirror)).")
                .seeAlso("jsc:programs")
                .register();
    }
}
