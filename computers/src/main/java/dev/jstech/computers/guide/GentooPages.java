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
 * The pages of the Gentoo chapter of the Linux by Hand book: a system built from source and installed step by step
 * from the live medium, from the stage 3 archive to the first reboot, and then the building of programs from source.
 */
final class GentooPages {

    private GentooPages() {
    }

    /** Declares the sections and entries of the Gentoo chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        gentooInstall(guide, chapter);
        gentooBase(guide, chapter);
        gentooSystem(guide, chapter);
    }

    private static void gentooInstall(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("gentoo_install").titled("Gentoo from source")
                .in(chapter).register();
        guide.page("gentoo_overview", section).titled("How Gentoo is installed")
                .icon(ComputingModule.USB_FLASH_DRIVE)
                .paragraph("Gentoo has no installer that does the work for you. Its installation medium gives you a"
                        + " shell and a walkthrough, and you type every step yourself. Each tool you run is the real"
                        + " one. The general steps for every system are in [Installing a system](jsc:installing).")
                .paragraph("The system you end up with is built on your machine from source code. The kernel and"
                        + " every program you add are compiled from their source first, so the install takes a long"
                        + " time. You decide how many compiles run at once.")
                .define("compiling", "Turning source code, the text a programmer writes, into a program the"
                        + " computer can run.")
                .define("kernel", "The core program of the operating system. It drives the hardware and runs the"
                        + " other programs.")
                .paragraph("Gentoo is a Linux system of the Legacy era, and it takes 4,096 MB of disk once"
                        + " installed. Its package manager is Portage, which installs programs by compiling them.")
                .define("Portage", "Gentoo's package manager. It reads build recipes and compiles each program"
                        + " from its source.")
                .paragraph("The walkthrough is a file on the medium, /root/install.txt. Read it at any time: type"
                        + " less /root/install.txt and press [[Enter]]. The file is always in English.")
                .paragraph("Long steps keep the terminal busy until they finish. Press [[Ctrl]]+[[C]] to stop a step"
                        + " that runs too long ([](jsc:medium_shell)).")
                .paragraph("The walkthrough marks the steps a system cannot boot without with an asterisk. The"
                        + " setting install_by_hand.gentoo_every_step decides which steps you are asked, and it is"
                        + " off by default.")
                .table("Which steps you are asked", "Setting", "What you are asked")
                .row(GuideCell.code("false"), GuideCell.words("Only the steps marked with an asterisk (the"
                        + " default)."))
                .row(GuideCell.code("true"), GuideCell.words("Every step of the handbook, the optional ones too."))
                .paragraph("Every download and every package comes from the [Mirror](jsc:mirror). A"
                        + " [Mainframe](jsc:mainframes) on the same network must run the Mirror, or the fetch steps"
                        + " fail.")
                .paragraph("The new system is built in /mnt/gentoo, on your disk. The live medium runs in memory,"
                        + " and it forgets every file written outside /mnt/gentoo when it restarts.")
                .ifSomethingGoesWrong("A file you wrote is gone after a restart.",
                        "Only /mnt/gentoo lives on your disk. Write the new system there, not in /root.",
                        "The steps stop, and you cannot tell what to do next.",
                        "Each refusal names the step it is missing. Do that step, then try again.")
                .seeAlso("jsc:installing", "jsc:the_systems")
                .register();
    }

    private static void gentooBase(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("gentoo_base").titled("The base").in(chapter).register();
        stageThree(guide, section);
        makeConf(guide, section);
        mountsAndChroot(guide, section);
        packageTreeAndWorld(guide, section);
    }

    private static void stageThree(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("stage_three", section).titled("The stage 3 archive")
                .paragraph("A stage 3 is a ready-made archive of a base Gentoo system: the basic files, the tools and"
                        + " the libraries, before the kernel and before any program you chose. You download it and"
                        + " unpack it into the new system's disk, so you do not build the base from source yourself.")
                .define("stage 3", "A tar archive of a base Gentoo system, ready to unpack in place of building it.")
                .define("make.conf", "The file of build settings that Portage reads before it compiles a program.")
                .paragraph("Its name tells you what it holds. In stage3-vel64-openrc.tar.xz, vel64 is the processor"
                        + " architecture, and openrc is the init system, the program that starts the services at"
                        + " boot.")
                .paragraph("The disk must be mounted first ([](jsc:mounts_and_chroot)). The download saves the file"
                        + " in the folder you stand in, and the live medium starts you in /root, so you move to"
                        + " /mnt/gentoo before you download.")
                .subheading("Downloading and unpacking")
                .steps("Type cd /mnt/gentoo and press [[Enter]].",
                        "Type wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz and press [[Enter]].",
                        "Type tar xpvf stage3-*.tar.xz --xattrs-include='*.*' and press [[Enter]].",
                        "Wait for the prompt to come back. Unpacking takes longer on a slow disk.")
                .session("jsc:guide/terminal/bash", "Gentoo")
                .typed("livecd /mnt/gentoo # ", "wget mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz")
                .answer("--<date>--  mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz",
                        "Resolving mainframe... done.",
                        "Connecting to mainframe... connected.",
                        "Mirror request sent, awaiting response... 200 OK")
                .paragraph("The first line of the answer carries the date. A progress bar then fills while the archive"
                        + " downloads, and the screen says when the file is saved.")
                .paragraph("The letters after tar matter. x unpacks the archive, p keeps the file permissions, and f"
                        + " names the archive that follows. The v letter prints each path as it is unpacked. Without"
                        + " v, the screen stays quiet until the unpacking ends. --xattrs-include keeps the extra file"
                        + " information that some programs need.")
                .paragraph("When the unpacking ends, the new system has its folders, among them /etc, /boot, /usr and"
                        + " /var. It also has two starting files: /etc/portage/make.conf, and an /etc/fstab that"
                        + " holds only a header. Unpacking the archive again undoes every later step, so the"
                        + " checklist asks for them again.")
                .ifSomethingGoesWrong(
                        "The download says \"Name or service not known\".",
                        "No Mainframe running the Mirror is on this network, or the address does not begin with"
                                + " mirror://mainframe.",
                        "The download says \"ERROR 404: Not Found.\"",
                        "The address must end in /gentoo/stage3-vel64-openrc.tar.xz, spelled exactly like that.",
                        "tar says \"Cowardly refusing to create an empty archive\".",
                        "The command names no stage 3 file. Type the whole command, with stage3 in the file name.",
                        "tar says \"Cannot open: No such file or directory\".",
                        "The archive is in /root, the folder you started in. Type cd /mnt/gentoo, then download the"
                                + " stage 3 again there.",
                        "tar says \"refusing to unpack a whole system over the live medium\".",
                        "The disk is not mounted at /mnt/gentoo, or you are not in that folder. Mount it"
                                + " ([](jsc:mounts_and_chroot)), then type cd /mnt/gentoo.")
                .seeAlso("jsc:mounts_and_chroot", "jsc:make_conf", "jsc:installing")
                .register();
    }

    private static void makeConf(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("make_conf", section).titled("make.conf and MAKEOPTS")
                .paragraph("Portage reads a settings file before it builds any program: /etc/portage/make.conf, in"
                        + " the new system. The base system comes with one. It holds the compiler flags, the language"
                        + " of the build messages, and the setting that decides how many compiles run at once.")
                .paragraph("Your central processing unit (CPU) has several cores. A core is one of its processing"
                        + " units, and each one can run its own work at the same time.")
                .define("core", "One of the processing units inside a processor, each able to run its own work at"
                        + " the same time.")
                .table("Lines of make.conf in the base system", "Line", "What it does")
                .row(GuideCell.code("COMMON_FLAGS=\"-O2 -pipe\""), GuideCell.words("The compiler flags for every"
                        + " program. -O2 asks for faster programs, and -pipe makes the compiler itself faster."))
                .row(GuideCell.code("LC_MESSAGES=POSIX.utf8"), GuideCell.words("Writes the build messages in"
                        + " English."))
                .paragraph("The base file has no MAKEOPTS line. Without one, every build runs a single compile at a"
                        + " time, which is slow on a machine with several cores. Add the line yourself.")
                .define("MAKEOPTS", "The setting in make.conf that says how many compiles run at once.")
                .subheading("Adding the line")
                .steps("Type nano -w /mnt/gentoo/etc/portage/make.conf and press [[Enter]].",
                        "Add a line MAKEOPTS=\"-j4\" of your own, and leave the other lines as they are.",
                        "Save the file and close nano.")
                .paragraph("You can also add the line in one command, from inside the new system: type echo"
                        + " 'MAKEOPTS=\"-j4\"' >> /etc/portage/make.conf and press [[Enter]].")
                .paragraph("The number follows -j. The count comes from the MAKEOPTS line that is not commented out,"
                        + " and a line that begins with # sets nothing. More compiles at once than cores buys"
                        + " nothing, so the build uses the smaller of the two numbers.")
                .table("How many compiles run at once", "The line in make.conf", "Cores", "Compiles at once")
                .row(GuideCell.words("none"), GuideCell.words("any"), GuideCell.words("1"))
                .row(GuideCell.code("MAKEOPTS=\"-j4\""), GuideCell.words("8"), GuideCell.words("4"))
                .row(GuideCell.code("MAKEOPTS=\"-j16\""), GuideCell.words("4"), GuideCell.words("4"))
                .row(GuideCell.code("MAKEOPTS=\"-j\""), GuideCell.words("any"), GuideCell.words("1"))
                .row(GuideCell.code("MAKEOPTS=\"-j0\""), GuideCell.words("any"), GuideCell.words("1"))
                .paragraph("A -j with no number counts as no setting, and a count below 1 becomes 1 compile. If you"
                        + " write several MAKEOPTS lines, the last one counts.")
                .ifSomethingGoesWrong("Builds are slow, even on a strong machine.",
                        "The base file has no MAKEOPTS line, so every build runs one compile at a time. Add one, as"
                                + " shown above.",
                        "Your MAKEOPTS line does nothing.",
                        "The line starts with #, which makes it a comment. Remove the # at the start of the line.",
                        "nano says \"<name> is a directory\".",
                        "The name you gave is a folder. Type the full file name:"
                                + " /mnt/gentoo/etc/portage/make.conf.")
                .seeAlso("jsc:package_tree_and_world", "jsc:building_from_source")
                .register();
    }

    private static void mountsAndChroot(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mounts_and_chroot", section).titled("Mounts and chroot")
                .paragraph("The new system lives on your disk, not on the medium. To reach it, you mount the disk's"
                        + " partitions into folders of the live medium ([](jsc:mounting)). The root partition goes to"
                        + " /mnt/gentoo.")
                .paragraph("Then you step into the new system with chroot ([](jsc:arch_chroot)). Chroot makes"
                        + " /mnt/gentoo look like the root folder /, so every command you type runs in the new"
                        + " system.")
                .steps("Type mount /dev/sda2 /mnt/gentoo and press [[Enter]].",
                        "For UEFI (Unified Extensible Firmware Interface) only, type mount /dev/sda1 /mnt/gentoo/efi"
                                + " and press [[Enter]]. That partition is the EFI system partition (ESP).",
                        "Type mount --types proc /proc /mnt/gentoo/proc and press [[Enter]].",
                        "Bind the other three folders the same way: type mount --rbind /sys /mnt/gentoo/sys, then do"
                                + " the same for /dev and /run.",
                        "Type chroot /mnt/gentoo /bin/bash and press [[Enter]].",
                        "Type source /etc/profile and press [[Enter]]. The command prints nothing.")
                .paragraph("The folders /proc, /sys, /dev and /run are not files on your disk. The kernel fills them"
                        + " with live information about the machine, and the new system needs them to reach its"
                        + " devices. A bind shows one folder at a second place, so the new system sees them too.")
                .paragraph("Once you are inside, the prompt starts with (chroot), and the commands work on the new"
                        + " system. Type exit to leave it, and the live medium's own shell comes back.")
                .ifSomethingGoesWrong(
                        "mount says \"wrong fs type, bad option, bad superblock\" for the root.",
                        "The root partition is not formatted as ext4 yet. Format it first: mkfs.ext4 /dev/sda2"
                                + " ([](jsc:filesystems_and_mkfs)).",
                        "mount says \"mount point does not exist\" for /mnt/gentoo/efi.",
                        "Mount the root partition first. The EFI system partition goes inside it.",
                        "mount says \"wrong fs type\" for the EFI partition.",
                        "Format that partition first: mkfs.fat -F 32 /dev/sda1.",
                        "mount says \"mount point does not exist\" when you bind a folder.",
                        "Mount the root partition first, then bind the folders.",
                        "chroot says \"cannot change root directory\" and names the path.",
                        "Type the path /mnt/gentoo exactly, with the disk mounted there.",
                        "chroot says \"failed to run command '/bin/bash': No such file or directory\".",
                        "The stage 3 is not unpacked yet. Unpack it into /mnt/gentoo first"
                                + " ([](jsc:stage_three)).",
                        "bash says \"arch-chroot: command not found\".",
                        "Gentoo has no arch-chroot. Step in with chroot /mnt/gentoo /bin/bash.")
                .seeAlso("jsc:arch_chroot", "jsc:mounting", "jsc:stage_three", "jsc:gentoo_reboot")
                .register();
    }

    private static void packageTreeAndWorld(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("package_tree_and_world", section).titled("The package tree, the profile and @world")
                .paragraph("Portage installs programs from recipes. The package tree is the collection of those"
                        + " recipes, one for each program.")
                .define("ebuild", "The build recipe of one program: where to fetch its source, and how to build it.")
                .paragraph("The tree comes from a dated snapshot of the repository. Portage checks the snapshot"
                        + " against its checksum and its GNU Privacy Guard (GPG) signature. You get the tree with"
                        + " emerge-webrsync, which downloads the snapshot, or with emerge --sync, which copies it over"
                        + " rsync. Either one works with no tree yet.")
                .steps("Inside the new system, type emerge-webrsync and press [[Enter]].",
                        "Type eselect profile list and press [[Enter]] to see the profiles.",
                        "Type eselect profile set, then the number you want, and press [[Enter]].",
                        "Type emerge --ask --verbose --update --deep --newuse @world and press [[Enter]].")
                .define("profile", "A named set of default build choices. It says how programs are built, and it"
                        + " installs nothing.")
                .paragraph("The profile sets the defaults that every build follows. Choose one from this table.")
                .table("The profiles", "Number", "Profile")
                .row(GuideCell.words("1"), GuideCell.code("default/linux/vel64/23.0"))
                .row(GuideCell.words("2"), GuideCell.code("default/linux/vel64/23.0/systemd"))
                .row(GuideCell.words("3"), GuideCell.code("default/linux/vel64/23.0/desktop"))
                .row(GuideCell.words("4"), GuideCell.code("default/linux/vel64/23.0/desktop/gnome"))
                .row(GuideCell.words("5"), GuideCell.code("default/linux/vel64/23.0/desktop/plasma"))
                .row(GuideCell.words("6"), GuideCell.code("default/linux/vel64/23.0/no-multilib"))
                .row(GuideCell.words("7"), GuideCell.code("default/linux/vel64/23.0/hardened"))
                .paragraph("Choose a profile with eselect profile set, even when profile 1 suits you.")
                .define("@world", "The set of packages the system wants. Updating it brings each one up to date.")
                .paragraph("The update command compiles every package of the world that is out of date. Each package"
                        + " has USE flags, which are switches that turn its optional features on or off.")
                .define("USE flag", "A switch that turns an optional feature of a program on or off when it is built.")
                .table("The packages of the world", "Package and version", "Archive and size", "USE flags")
                .row(GuideCell.code("sys-libs/ncurses 6.5_p20250125"),
                        GuideCell.words("ncurses-6.5.tar.gz, 3.6 MB; replaces 6.4_p20240414"),
                        GuideCell.words("On: cxx, tinfo. Off: debug, doc."))
                .row(GuideCell.code("app-shells/bash 5.2_p37"),
                        GuideCell.words("bash-5.2.tar.gz, 10.7 MB; replaces 5.2_p26"),
                        GuideCell.words("On: net, nls, readline. Off: afs, plugins."))
                .row(GuideCell.code("app-editors/vim 9.1.0794"),
                        GuideCell.words("vim-9.1.0794.tar.gz, 16.8 MB; replaces nothing"),
                        GuideCell.words("On: acl, crypt, nls. Off: X, lua, perl."))
                .paragraph("With --ask, Portage stops and asks: Would you like to merge these packages? [Yes/No]. An"
                        + " answer that starts with n quits, and the screen says Quitting. Any other answer carries"
                        + " on, Enter included.")
                .paragraph("Each package then goes through its phases. It downloads its archive from the Mirror,"
                        + " unpacks the source, prepares it, configures it, compiles it, installs it and merges it"
                        + " into the system.")
                .ifSomethingGoesWrong(
                        "emerge says \"The portage tree is empty\".",
                        "Run emerge-webrsync, or emerge --sync, before you merge anything.",
                        "emerge says \"Could not resolve mirror://mainframe\".",
                        "No Mainframe running the [Mirror](jsc:mirror) is on this network. Connect to one, then try"
                                + " again.",
                        "emerge says \"command not found\" when run outside the new system, since the package manager"
                                + " is the new system's.",
                        "Step into the new system first: chroot /mnt/gentoo /bin/bash.",
                        "emerge says \"No packages given.\"",
                        "Type the name of at least one package after emerge.",
                        "emerge says there are no ebuilds to satisfy the name you typed.",
                        "Check the spelling. The name must be a program the Mirror has"
                                + " ([](jsc:building_from_source)).",
                        "eselect says that the target does not appear to be valid.",
                        "Type a number from the list that eselect profile list shows.")
                .seeAlso("jsc:make_conf", "jsc:kernel_gentoo", "jsc:building_from_source")
                .register();
    }

    private static void gentooSystem(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("gentoo_system").titled("The system").in(chapter)
                .register();
        kernelGentoo(guide, section);
        fstabByHand(guide, section);
        gentooBootloader(guide, section);
        gentooReboot(guide, section);
        buildingFromSource(guide, section);
    }

    private static void kernelGentoo(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("kernel_gentoo", section).titled("The kernel")
                .paragraph("The kernel is the part of the system that drives the hardware. Every program reaches the"
                        + " processor, the memory and the disks through it. Gentoo builds your kernel from its"
                        + " sources, so the install downloads the sources first.")
                .paragraph("This install uses kernel version 6.11.5. Its files carry the name 6.11.5-gentoo, and the"
                        + " compressed kernel goes into /boot.")
                .steps("Type emerge sys-kernel/gentoo-sources and press [[Enter]].",
                        "Type eselect kernel list and press [[Enter]] to see the kernels.",
                        "Type eselect kernel set 1 and press [[Enter]]. The command prints nothing.",
                        "Build and install the kernel by one of the two routes in the table below.")
                .paragraph("genkernel is a script that builds the whole kernel with one command. The by-hand route"
                        + " runs in /usr/src/linux, the folder that the kernel choice makes.")
                .define("genkernel", "A script that builds the kernel and its modules with one command.")
                .table("The two routes", "Route", "Command", "What it does")
                .row(GuideCell.words("Automatic"), GuideCell.code("genkernel all"),
                        GuideCell.words("Builds the kernel and its modules, and copies the kernel into /boot."))
                .row(GuideCell.words("By hand"), GuideCell.code("make -j4 && make modules_install && make install"),
                        GuideCell.words("Builds in /usr/src/linux, installs the modules, and copies the kernel into"
                                + " /boot."))
                .paragraph("The modules live under /lib/modules, in a folder named for the kernel version.")
                .define("module", "A piece of the kernel that loads when something needs it, kept as its own file.")
                .ifSomethingGoesWrong(
                        "genkernel says \"command not found\", because the command is on the new system and you are"
                                + " still in the live medium.",
                        "Step into the new system first: chroot /mnt/gentoo /bin/bash. make needs the same step.",
                        "genkernel says \"* ERROR: no kernel sources found in /usr/src\".",
                        "Download the sources first: emerge sys-kernel/gentoo-sources.",
                        "eselect says \"!!! Error: No kernel sources found in /usr/src\".",
                        "Download the sources first: emerge sys-kernel/gentoo-sources.",
                        "make says it has no targets and no makefile.",
                        "The kernel is built in its sources. Type cd /usr/src/linux first.",
                        "make says it has no rule for the target vmlinux.",
                        "The kernel is not built yet. Type make first, then the install.")
                .seeAlso("jsc:fstab_by_hand", "jsc:gentoo_bootloader", "jsc:package_tree_and_world")
                .register();
    }

    private static void fstabByHand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("fstab_by_hand", section).titled("The fstab by hand")
                .paragraph("The file system table, /etc/fstab, tells the system which partitions to mount at boot"
                        + " ([](jsc:arch_fstab)). The base starts with only a header in it, so you write the root line"
                        + " yourself.")
                .paragraph("The root line names the root partition by its universally unique identifier (UUID). The"
                        + " UUID belongs to one partition, and it stays the same when the disks are reordered"
                        + " ([](jsc:filesystems_and_mkfs)).")
                .steps("Type blkid >> /etc/fstab and press [[Enter]].",
                        "Type nano /etc/fstab and press [[Enter]].",
                        "Find the line of the root partition, and cut it down to UUID=<its uuid>  /  ext4  noatime  0"
                                + " 1.",
                        "Save the file and close nano.")
                .table("The fields of the root line", "Field", "Value", "What it means")
                .row(GuideCell.words("File system"), GuideCell.code("UUID=<its uuid>"),
                        GuideCell.words("The root partition, by its identifier."))
                .row(GuideCell.words("Mount point"), GuideCell.code("/"),
                        GuideCell.words("The root folder of the system."))
                .row(GuideCell.words("Type"), GuideCell.code("ext4"),
                        GuideCell.words("The file system the partition is formatted with."))
                .row(GuideCell.words("Options"), GuideCell.code("noatime"),
                        GuideCell.words("Do not record when each file was last read."))
                .row(GuideCell.words("Dump"), GuideCell.code("0"),
                        GuideCell.words("Not backed up by the dump tool."))
                .row(GuideCell.words("Pass"), GuideCell.code("1"),
                        GuideCell.words("Checked first at boot."))
                .paragraph("At reboot, the machine looks for a line whose mount point is /. It needs that line to find"
                        + " the root, so the reboot checks for it before it starts.")
                .ifSomethingGoesWrong(
                        "The reboot says \"no root filesystem in /etc/fstab\".",
                        "No line in the file mounts /. Write the root line as shown above, then reboot again"
                                + " ([](jsc:gentoo_reboot)).")
                .seeAlso("jsc:arch_fstab", "jsc:gentoo_reboot")
                .register();
    }

    private static void gentooBootloader(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gentoo_bootloader", section).titled("GRUB on Gentoo")
                .paragraph("The bootloader starts the kernel you built, and it shows the menu of the systems on"
                        + " the disk. On Gentoo the bootloader is GRUB ([](jsc:arch_bootloader)). GRUB is a package"
                        + " like any other, so you compile it from source too.")
                .paragraph("The firmware decides where GRUB goes. UEFI (Unified Extensible Firmware Interface) reads it"
                        + " from the EFI system partition. BIOS (basic input/output system) finds it on the disk"
                        + " itself.")
                .steps("Type emerge --ask sys-boot/grub and press [[Enter]]. Answer the question with Enter.",
                        "Type the install command for your firmware, from the table below, and press [[Enter]].",
                        "Type grub-mkconfig -o /boot/grub/grub.cfg and press [[Enter]].")
                .table("The install command for each firmware", "Firmware", "Command")
                .row(GuideCell.words("UEFI"), GuideCell.code("grub-install --efi-directory=/efi"))
                .row(GuideCell.words("BIOS (older firmware)"), GuideCell.code("grub-install /dev/sda"))
                .paragraph("The --efi-directory=/efi part must name the place where the EFI system partition is mounted"
                        + " in the new system. The mount step put it at /mnt/gentoo/efi, which is /efi inside the new"
                        + " system.")
                .paragraph("The menu that grub-mkconfig writes has one entry, named Gentoo Linux. The entry starts the"
                        + " kernel you built, and it gives the kernel your root partition by its UUID.")
                .ifSomethingGoesWrong(
                        "bash says \"grub-install: command not found\".",
                        "GRUB is not compiled yet. Type emerge --ask sys-boot/grub first.",
                        "grub-install says \"command not found\". The program belongs to the new system and is not"
                                + " built there yet.",
                        "Step into the new system first: chroot /mnt/gentoo /bin/bash, then run the command there.",
                        "grub-install says \"warning: no kernel image found in /boot.\"",
                        "Build the kernel first: genkernel all, or make in /usr/src/linux"
                                + " ([](jsc:kernel_gentoo)).",
                        "grub-install says \"error: /boot/efi doesn't look like an EFI partition.\"",
                        "The EFI system partition is not mounted where grub-install looks. Mount it, then name its"
                                + " place with --efi-directory=/efi, as the walkthrough does.",
                        "grub-install says \"error: install device isn't specified.\"",
                        "BIOS firmware needs the disk named. Type grub-install /dev/sda.",
                        "grub-install says \"error: cannot find a device for /dev/<dev>.\"",
                        "GRUB goes on the disk, not in a partition. Type grub-install /dev/sda.",
                        "grub-mkconfig says \"/boot/grub: No such file or directory\".",
                        "GRUB is not installed yet. Run grub-install first.")
                .seeAlso("jsc:arch_bootloader", "jsc:arch_grub_config", "jsc:gentoo_reboot")
                .register();
    }

    private static void gentooReboot(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gentoo_reboot", section).titled("Rebooting into Gentoo")
                .paragraph("The reboot is the last step. Before it starts the new system, the machine checks that the"
                        + " system has everything it needs to boot. If you missed something, it lists the missing"
                        + " items and refuses to start ([](jsc:reboot_checklist)).")
                .steps("Type exit and press [[Enter]].",
                        "Type cd and press [[Enter]].",
                        "Type umount -R /mnt/gentoo and press [[Enter]]. The command prints nothing.",
                        "Type reboot and press [[Enter]].")
                .paragraph("The machine always checks the items in the first table. Each missing item prints with its"
                        + " fix in brackets.")
                .table("Always checked", "Missing item", "What the machine says")
                .row(GuideCell.words("No base"), GuideCell.words("no base system installed (wget the stage 3,"
                        + " then tar)"))
                .row(GuideCell.words("No root in fstab"), GuideCell.words("no root filesystem in /etc/fstab (blkid,"
                        + " then write the line)"))
                .row(GuideCell.words("No kernel"), GuideCell.words("no kernel (genkernel all, or make in"
                        + " /usr/src/linux)"))
                .row(GuideCell.words("No bootloader"), GuideCell.words("no bootloader (grub-install)"))
                .row(GuideCell.words("No GRUB menu"), GuideCell.words("nothing for the bootloader to start"
                        + " (grub-mkconfig -o /boot/grub/grub.cfg)"))
                .paragraph("With install_by_hand.gentoo_every_step set to true, the machine also checks the items in"
                        + " the second table.")
                .table("Checked when every step is asked", "Missing item", "What the machine says")
                .row(GuideCell.words("Binds"), GuideCell.words("/proc, /sys, /dev and /run were not bound into the new"
                        + " system (mount --rbind, --types proc)"))
                .row(GuideCell.words("Profile"), GuideCell.words("no profile chosen (eselect profile list, then set)"))
                .row(GuideCell.words("World"), GuideCell.words("the system was not brought up to date (emerge --ask"
                        + " --verbose --update --deep --newuse @world)"))
                .row(GuideCell.words("Kernel link"), GuideCell.words("/usr/src/linux points nowhere (eselect kernel"
                        + " set 1)"))
                .row(GuideCell.words("Name"), GuideCell.words("the machine has no name (echo <name> >"
                        + " /etc/hostname)"))
                .paragraph("The name is set inside the new system, with echo <name> > /etc/hostname or with hostname"
                        + " <name>. A name starts with a letter or digit, holds up to 15 characters, and may contain"
                        + " hyphens.")
                .paragraph("When every item is in place, the reboot makes the disk you chose boot first. The machine"
                        + " answers \"Rebooting into the new system\".")
                .ifSomethingGoesWrong(
                        "reboot says \"you are inside the new system. exit first.\"",
                        "Type exit to leave the new system, then type reboot again.",
                        "The machine says \"The new system will not boot\" and lists items.",
                        "Each item names its fix in brackets. Do that step, then type reboot again.",
                        "hostname says \"the specified hostname is invalid\".",
                        "Use up to 15 letters, digits and hyphens, and start with a letter or digit.",
                        "hostname says \"you cannot change the host name of the live medium\".",
                        "Step into the new system first with chroot /mnt/gentoo /bin/bash, then set the name.")
                .seeAlso("jsc:reboot_checklist", "jsc:will_not_boot_messages", "jsc:after_reboot_checks")
                .register();
    }

    private static void buildingFromSource(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("building_from_source", section).titled("Building programs from source")
                .paragraph("Inside the new system, emerge installs programs the same way it did"
                        + " during the install. Portage compiles each program from its source on your machine, so"
                        + " every program takes time to build.")
                .paragraph("A compile is faster on a machine whose processor has a faster clock. The compiles at once"
                        + " are the smaller of two numbers: the count in MAKEOPTS ([](jsc:make_conf)), and the cores of"
                        + " your machine. There is always at least 1 compile at a time.")
                .paragraph("You name a program by its category and name, such as kde-plasma/plasma-meta, or by its"
                        + " name alone, in any letter case. The category follows the kind of program: applications go"
                        + " under app-misc, and desktops under x11-wm.")
                .paragraph("During the install, the Mirror installs by name only a program that runs on Linux, is not"
                        + " a background service, and fits the machine and its era.")
                .paragraph("To install a desktop, type emerge --ask kde-plasma/plasma-meta. It builds these packages"
                        + " in this order:")
                .table("What the KDE Plasma desktop brings", "Package", "Version", "Archive size")
                .row(GuideCell.code("dev-qt/qtbase"), GuideCell.words("6.7.3"), GuideCell.words("48.0 MB"))
                .row(GuideCell.code("kde-frameworks/kconfig"), GuideCell.words("6.6.0"), GuideCell.words("2.1 MB"))
                .row(GuideCell.code("kde-frameworks/kcoreaddons"), GuideCell.words("6.6.0"), GuideCell.words("2.4 MB"))
                .row(GuideCell.code("kde-plasma/kwin"), GuideCell.words("6.1.5"), GuideCell.words("8.6 MB"))
                .row(GuideCell.code("kde-plasma/plasma-workspace"), GuideCell.words("6.1.5"),
                        GuideCell.words("19.0 MB"))
                .row(GuideCell.code("kde-plasma/plasma-meta"), GuideCell.words("listed only"), GuideCell.words("none"))
                .paragraph("The GNOME, Cinnamon and CDE (Common Desktop Environment) desktops have their own"
                        + " chains. A package that is only listed, such as the desktop's meta package, takes no time"
                        + " to build.")
                .subheading("emerge on the system you installed")
                .paragraph("Once Gentoo starts from your own disk, emerge stays as its package manager, and it still"
                        + " compiles what it installs. Each of its words reaches the Mirror over the network.")
                .table("What emerge does on the installed system", "Words", "What they do")
                .row(GuideCell.code("emerge <package>"), GuideCell.words("Compiles and installs the package."))
                .row(GuideCell.code("emerge --ask <package>"),
                        GuideCell.words("Lists what it would build, and asks you before it starts."))
                .row(GuideCell.code("emerge --search [term]"),
                        GuideCell.words("Searches the Mirror for packages whose names hold the term."))
                .row(GuideCell.code("emerge --sync"), GuideCell.words("Reads the Mirror's lists again."))
                .row(GuideCell.code("emerge --update @world"),
                        GuideCell.words("Brings every installed package up to the version the Mirror serves."))
                .row(GuideCell.code("emerge --unmerge <package>"), GuideCell.words("Removes the package."))
                .command(CommandGuidePages.id(Platform.LINUX, "emerge"))
                .ifSomethingGoesWrong(
                        "emerge says there are no ebuilds to satisfy the name you typed.",
                        "Check the spelling, or use the category and name, such as kde-plasma/plasma-meta.",
                        "emerge says \"The portage tree is empty\".",
                        "Run emerge-webrsync first ([](jsc:package_tree_and_world)).",
                        "emerge says \"Could not resolve mirror://mainframe\".",
                        "No Mainframe running the Mirror is on this network. Connect to one, then try again.")
                .seeAlso("jsc:make_conf", "jsc:package_tree_and_world", "jsc:gentoo_reboot")
                .register();
    }
}
