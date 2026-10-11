/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import static dev.jstech.computers.guide.ComputersGuide.blocksOf;
import static dev.jstech.computers.guide.ComputersGuide.cablesOn;
import static dev.jstech.computers.guide.ComputersGuide.itemsOf;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.HubBlock;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.block.SubwooferBlock;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the peripherals chapter of the Guide to Operations: the monitors and the cables that join every
 * device to its computer, the drives and their media, the Dock Station, printers, speakers, Redstone Interfaces, the
 * KVM Switch, and how to disable a device. Each page explains the idea first, then the parts and their rules, and
 * ends with the problems a player meets and their fixes.
 */
final class PeripheralPages {

    private PeripheralPages() {
    }

    /** Declares the sections and entries of the peripherals chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("peripherals").titled("Peripherals")
                .icon(ComputingModule.MONITOR).in(chapter).register();
        monitors(guide, section);
        peripheralCables(guide, section);
        drives(guide, section);
        dockStation(guide, section);
        printers(guide, section);
        speakers(guide, section);
        redstoneInterfaces(guide, section);
        kvmSwitch(guide, section);
        disableDevice(guide, section);
    }

    private static void monitors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("monitors", section).titled("Monitors").icon(ComputingModule.MONITOR)
                .coversAll(blocksOf(MonitorBlock.class))
                .paragraph("The monitor is the screen of a computer. Everything you do with its programs, you do at"
                        + " the monitor: the computer is the case, and the monitor is where you sit. A monitor is a"
                        + " peripheral, and you join it to its computer with a peripheral cable, or you place it right"
                        + " against the computer.")
                .define("Peripheral", "A device, such as a monitor, a drive or a printer, that a computer reaches"
                        + " through its own cable.")
                .define("Peripheral Cable", "The cable that joins a computer to its devices. It carries no network:"
                        + " the network has its own data cables.")
                .paragraph("Eight monitors come in five eras. The Vintage has four: the Mono I with paper-white, the"
                        + " Mono II with green, the Amber, and the CGA with sixteen colours. The Legacy is the first"
                        + " full colour tube. The Transition, Standard and Advanced monitors are flat colour panels.")
                .table("The monitors", "Monitor", "Era", "Picture", "Joins others")
                .row(GuideCell.words("Mono I Monitor"), GuideCell.words("Vintage"),
                        GuideCell.words("Paper-white, every system in white"), GuideCell.words("No"))
                .row(GuideCell.words("Mono II Monitor"), GuideCell.words("Vintage"),
                        GuideCell.words("Green"), GuideCell.words("No"))
                .row(GuideCell.words("Amber Monitor"), GuideCell.words("Vintage"),
                        GuideCell.words("Amber"), GuideCell.words("No"))
                .row(GuideCell.words("CGA Monitor"), GuideCell.words("Vintage"),
                        GuideCell.words("Sixteen colours"), GuideCell.words("No"))
                .row(GuideCell.words("Legacy Monitor"), GuideCell.words("Legacy"),
                        GuideCell.words("Colour picture tube"), GuideCell.words("No"))
                .row(GuideCell.words("Transition Monitor"), GuideCell.words("Transition"),
                        GuideCell.words("Colour flat panel"), GuideCell.words("Yes"))
                .row(GuideCell.words("Monitor"), GuideCell.words("Standard"),
                        GuideCell.words("Colour flat panel"), GuideCell.words("Yes"))
                .row(GuideCell.words("Color Monitor"), GuideCell.words("Advanced"),
                        GuideCell.words("Colour flat panel"), GuideCell.words("Yes"))
                .define("Flat panel", "A monitor that can join others into one big screen: the Transition, Standard"
                        + " and Advanced monitors.")
                .paragraph("A flat panel joins its neighbours into one big screen. Put monitors of the same kind side"
                        + " by side, touching, in one wall and facing the same way, and they become one screen. A gap,"
                        + " or a step in the wall, leaves them as separate monitors. The game's tooltip reads \"Joins"
                        + " the same monitors beside it into one screen, up to 8 wide and 6 tall\", so one screen holds"
                        + " 48 monitors at most. Vintage and Legacy monitors do not join.")
                .scene("Three flat Monitors side by side in one wall, switched on. Only the corner monitor shows its"
                        + " power button. Drag the scene to see the back.", 110)
                .place(14, 0, 14, () -> ComputingModule.MONITOR.get(), "facing=north,button=false,lit=true", "")
                .place(15, 0, 14, () -> ComputingModule.MONITOR.get(), "facing=north,button=false,lit=true", "")
                .place(16, 0, 14, () -> ComputingModule.MONITOR.get(), "facing=north,button=true,lit=true", "")
                .balloon(1, GuideBlock.ScenePart.of(15, 0, 14), "A flat Monitor",
                        "Monitors of one kind, touching and facing the same way, make one screen.")
                .balloon(2, GuideBlock.ScenePart.of(16, 0, 14).box(13, 1, 15, 15, 3, 16), "Power button",
                        "Click it to switch the whole screen on or off.")
                .paragraph("Each monitor takes one video output of its computer. A video output is a connector on a"
                        + " graphics card, or on a board, and you can use it for one monitor only"
                        + " ([](jsc:graphics_cards)).")
                .define("Video output", "A connector on a graphics card, or on a board, that drives one monitor.")
                .paragraph("A lit monitor also takes video memory, one block for each monitor of its screen. A joined"
                        + " screen takes one block for each of its monitors. The bigger the era, the more memory a"
                        + " block takes:")
                .table("Video memory per block", "Monitor", "Memory per block")
                .row(GuideCell.words("Vintage, monochrome"), GuideCell.amount(64, "KB"))
                .row(GuideCell.words("Vintage, sixteen colours"), GuideCell.amount(256, "KB"))
                .row(GuideCell.words("Legacy"), GuideCell.amount(4, "MB"))
                .row(GuideCell.words("Transition"), GuideCell.amount(16, "MB"))
                .row(GuideCell.words("Standard"), GuideCell.amount(64, "MB"))
                .row(GuideCell.words("Advanced"), GuideCell.amount(256, "MB"))
                .paragraph("The computer gives its video memory first to its graphics windows, then to the monitors in"
                        + " the order you linked them. A monitor that does not fit stays dark.")
                .paragraph("Every monitor has a power button on its bezel. Click the button to switch that monitor on"
                        + " or off. It switches the monitor alone: the computer behind it keeps running, and you"
                        + " switch the computer at its case. On a big screen, the button in the corner switches the"
                        + " whole screen. Every monitor starts switched on.")
                .paragraph("From the Legacy on, a monitor that is switched on with no picture sleeps, and its lamp"
                        + " glows amber. A Vintage monitor keeps its lamp lit. A switched-off monitor opens nothing:"
                        + " a click on its face says \"The monitor is off\".")
                .paragraph("The monitor shows what its computer is doing at each stage: the self-test when the"
                        + " computer powers up, the Boot Menu, the Setup, the installer and the progress of an"
                        + " install. On a rack with a [KVM Switch](jsc:kvm_switch), it also shows the picker of"
                        + " the channels.")
                .paragraph("One player at a time sits at a monitor. If someone else is at it, you are told that"
                        + " another player is using the monitor.")
                .define("KVM", "Keyboard, video and mouse: one monitor and keyboard that reach several machines.")
                .paragraph("A picture tube, in the Vintage or the Legacy, makes a sound when it switches on or off. A"
                        + " flat panel goes dark without a sound.")
                .ifSomethingGoesWrong("No computer found in range over a Peripheral Cable",
                        "Lay the peripheral cable all the way, within the reach of its era ([](jsc:peripheral_cables)),"
                                + " or stand the monitor right against the computer.",
                        "The computer has no video output: install a graphics card",
                        "Put a graphics card in the computer, or use a processor with built-in graphics"
                                + " ([](jsc:graphics_cards)).",
                        "The computer's video outputs are all in use",
                        "Each graphics card has its own number of outputs. Add a card, or take a monitor away.",
                        "The monitor says \"Not enough video memory\" and names how much it needs.",
                        "Use a graphics card with more video memory, or fewer monitors, or smaller ones in a joined"
                                + " screen.",
                        "No signal - the computer is powered off",
                        "Switch the computer on at its case.",
                        "No signal - the computer has disabled this monitor",
                        "Enable the monitor again ([](jsc:disable_device)).",
                        "The monitor is off",
                        "Click the power button on its bezel.",
                        "This rack holds several computers - mount a KVM Switch to pick one",
                        "Mount a KVM Switch in the rack ([](jsc:kvm_switch)).",
                        "The rack holds no computer to show",
                        "Put a computer in the rack.",
                        "No computer linked",
                        "Join the monitor to a computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "The monitor says that another player is using it.",
                        "The other player keeps the screen until they leave it.")
                .seeAlso("jsc:peripheral_cables", "jsc:graphics_cards", "jsc:kvm_switch")
                .register();
    }

    private static void peripheralCables(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("peripheral_cables", section).titled("Peripheral cables and hubs")
                .icon(() -> ComputingModule.PERIPHERAL_CABLE)
                .coversAll(cablesOn(GridKind.PERIPHERAL))
                .coversAll(blocksOf(HubBlock.class))
                .paragraph("A peripheral cable joins a computer to its devices: monitors, speakers, drives,"
                        + " printers and Redstone Interfaces. It is not a data cable, and it carries no network. A"
                        + " device placed"
                        + " right against its computer needs no cable at all.")
                .scene("A Personal Computer with its devices on a Peripheral Cable: two speakers, a Hub, a Floppy"
                        + " Drive, a CD Drive and a Printer. Drag the scene to see the back.", 130)
                .place(9, 0, 11, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south", "")
                .place(10, 0, 11, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}")
                .place(11, 0, 11, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2104,0,0,0,0,0]}")
                .place(12, 0, 11, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,10296,0,0,0,0,0]}")
                .place(13, 0, 11, () -> ComputingModule.STANDARD_HUB.get(), "", "{LinkedOwner:2473901207552L}")
                .place(14, 0, 11, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,6200,0,0,0,0,0]}")
                .place(15, 0, 11, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}")
                .place(11, 0, 12, () -> ComputingModule.SPEAKER.get(), "facing=south,lit=true",
                        "{LinkedOwner:2473901207552L,Powered:1b}")
                .place(12, 0, 12, () -> ComputingModule.SPEAKER.get(), "facing=south,lit=true",
                        "{LinkedOwner:2473901207552L,Powered:1b}")
                .place(13, 0, 12, () -> ComputingModule.FLOPPY_DRIVE.get(), "facing=south,loaded=true",
                        "{LinkedOwner:2473901207552L,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:floppy_disk\","
                                + "count:1}]}}")
                .place(14, 0, 12, () -> ComputingModule.CD_DRIVE.get(), "facing=south,loaded=true",
                        "{LinkedOwner:2473901207552L,TrayOpen:1b,MediaSlot:{Size:1,Items:[{Slot:0,"
                                + "id:\"jsc:cd_rom\",count:1}]}}")
                .place(15, 0, 12, () -> ComputingModule.PRINTER.get(), "facing=south",
                        "{LinkedOwner:2473901207552L}")
                .view(-35, 25, 1.35F)
                .balloon(1, GuideBlock.ScenePart.of(9, 0, 11), "The computer")
                .balloon(2, GuideBlock.ScenePart.of(10, 0, 11), "The Peripheral Cable",
                        "It joins the computer to its devices and carries no network.")
                .balloon(3, GuideBlock.ScenePart.of(11, 0, 12), "A speaker",
                        "Join speakers to the computer's own cable, before the Hub: they do not pass through a Hub.")
                .balloon(4, GuideBlock.ScenePart.of(13, 0, 11), "The Hub",
                        "It takes one port of the computer and offers more ports of its own.")
                .balloon(5, GuideBlock.ScenePart.of(13, 0, 12), "A Floppy Drive")
                .balloon(6, GuideBlock.ScenePart.of(14, 0, 12), "A CD Drive",
                        "Press the eject button beside a drive to bring its tray out.")
                .balloon(7, GuideBlock.ScenePart.of(15, 0, 12), "The Printer")
                .paragraph("Each era has its own peripheral cable, and each one reaches a fixed number of cables from"
                        + " its computer. A device further away than that is not seen. A port takes the cable of its"
                        + " own era and of every earlier era, never a newer one.")
                .define("Port", "A socket on a computer, a hub or a device. Each socket has a kind: device, video or"
                        + " audio.")
                .table("How far each cable reaches", "Era", "Cable", "Reach")
                .row(GuideCell.words("Vintage"), GuideCell.words("Vintage Peripheral Cable"),
                        GuideCell.words("8 cables"))
                .row(GuideCell.words("Legacy"), GuideCell.words("Legacy Peripheral Cable"),
                        GuideCell.words("12 cables"))
                .row(GuideCell.words("Transition"), GuideCell.words("Transition Peripheral Cable"),
                        GuideCell.words("14 cables"))
                .row(GuideCell.words("Standard"), GuideCell.words("Peripheral Cable"),
                        GuideCell.words("16 cables"))
                .row(GuideCell.words("Advanced"), GuideCell.words("Advanced Peripheral Cable"),
                        GuideCell.words("20 cables"))
                .paragraph("Each kind of port takes its own kind of device. A drive, a printer, a Redstone Interface"
                        + " or a hub takes a device port. A monitor takes a video output of a graphics card. A speaker"
                        + " takes an audio output, and each audio output drives two speakers. The number of ports"
                        + " depends on the era of the board and of the card.")
                .table("Device ports by board era", "Board era", "Device ports")
                .row(GuideCell.words("Vintage"), GuideCell.words("2 ports"))
                .row(GuideCell.words("Legacy"), GuideCell.words("4 ports"))
                .row(GuideCell.words("Transition"), GuideCell.words("6 ports"))
                .row(GuideCell.words("Standard"), GuideCell.words("8 ports"))
                .row(GuideCell.words("Advanced"), GuideCell.words("10 ports"))
                .table("Video outputs by graphics card era", "Card era", "Video outputs")
                .row(GuideCell.words("Vintage"), GuideCell.words("1 output"))
                .row(GuideCell.words("Legacy"), GuideCell.words("2 outputs"))
                .row(GuideCell.words("Transition"), GuideCell.words("2 outputs"))
                .row(GuideCell.words("Standard"), GuideCell.words("4 outputs"))
                .row(GuideCell.words("Advanced"), GuideCell.words("4 outputs"))
                .paragraph("A server board adds one console output of its own. A processor with built-in graphics adds"
                        + " one output to the board. The audio outputs come from the sound card, and from the board"
                        + " itself from the Transition on ([](jsc:sound_cards)).")
                .paragraph("When the board runs out of device ports, a hub takes one port and offers ports of its own."
                        + " A hub is a device, and each device cabled to it takes one of the hub's ports.")
                .define("Hub", "A device that takes one port and offers more ports of its kind, so that more devices"
                        + " can join one computer.")
                .table("Hubs and the ports they offer", "Hub", "Ports it offers")
                .row(GuideCell.words("Vintage Hub"), GuideCell.words("2 ports"))
                .row(GuideCell.words("Legacy Hub"), GuideCell.words("4 ports"))
                .row(GuideCell.words("Transition Hub"), GuideCell.words("4 ports"))
                .row(GuideCell.words("Standard Hub"), GuideCell.words("7 ports"))
                .row(GuideCell.words("Advanced Hub"), GuideCell.words("7 ports"))
                .paragraph("A cable after a hub reaches as far again as the cable before it, and a hub can hang from"
                        + " another hub. Only device links pass through a hub. Screens and speakers do not, so join"
                        + " them to the computer. If you break a hub, the devices behind it lose their link.")
                .paragraph("The Device Manager, the Frames tool that lists the hardware, shows what is on every port"
                        + " ([](jsc:device_manager)).")
                .ifSomethingGoesWrong("The device is not seen.",
                        "Check that the cable is within the reach of its era, and that it is not newer than the port.",
                        "A monitor joined through a hub stays dark.",
                        "Screens and speakers do not pass through a hub. Join them to the computer.")
                .seeAlso("jsc:monitors", "jsc:drives", "jsc:device_manager")
                .register();
    }

    private static void drives(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("drives", section).titled("Drives and media").icon(ComputingModule.FLOPPY_DRIVE)
                .coversAll(blocksOf(MediaReaderBlock.class))
                .paragraph("A drive reads removable media: a disk or a disc that you put in, and take out again. You"
                        + " need drives to install systems and programs, and to carry files from one computer to"
                        + " another. Each drive is a peripheral, joined to its computer like any other device"
                        + " ([](jsc:peripheral_cables)).")
                .define("Linked drive", "A drive joined to the computer, into which you put a disc or a stick, such"
                        + " as a CD Drive or a Dock Station.")
                .paragraph("Media are named after their formats. CD stands for compact disc, DVD for digital versatile"
                        + " disc, and USB for Universal Serial Bus. A read-only memory (ROM) medium can only be read;"
                        + " a rewritable one (RW or RE) can also be written to.")
                .define("Format", "The physical kind of medium: floppy, CD, DVD, USB or Blu-ray. It decides which"
                        + " drives read the medium.")
                .paragraph("Each drive reads only some formats. The table shows what each drive reads.")
                .table("Drives and what they read", "Drive", "Reads")
                .row(GuideCell.words("Floppy Drive"), GuideCell.words("Floppy disks"))
                .row(GuideCell.words("CD Drive"), GuideCell.words("CD-ROMs and CD-RWs"))
                .row(GuideCell.words("DVD Drive"), GuideCell.words("DVD-ROMs and DVD-RWs, and CDs"))
                .row(GuideCell.words("Blu-ray Drive"), GuideCell.words("BD-ROMs and BD-REs, and DVDs and CDs"))
                .row(GuideCell.words("Dock Station"), GuideCell.words("USB flash drives, and storage disks"))
                .row(GuideCell.words("USB port of a case"), GuideCell.words("USB flash drives"))
                .paragraph("The Dock Station is a drive of its own for storage disks. The USB port on the front of a"
                        + " case takes a flash drive ([](jsc:case_front)).")
                .table("Media and the drives that read them", "Medium", "Format", "Writable", "Read by")
                .row(GuideCell.words("Floppy Disk"), GuideCell.words("Floppy"), GuideCell.words("Yes"),
                        GuideCell.words("Floppy Drive"))
                .row(GuideCell.words("CD-ROM"), GuideCell.words("CD"), GuideCell.words("No"),
                        GuideCell.words("CD Drive, DVD Drive, Blu-ray Drive"))
                .row(GuideCell.words("CD-RW"), GuideCell.words("CD"), GuideCell.words("Yes"),
                        GuideCell.words("CD Drive, DVD Drive, Blu-ray Drive"))
                .row(GuideCell.words("DVD-ROM"), GuideCell.words("DVD"), GuideCell.words("No"),
                        GuideCell.words("DVD Drive, Blu-ray Drive"))
                .row(GuideCell.words("DVD-RW"), GuideCell.words("DVD"), GuideCell.words("Yes"),
                        GuideCell.words("DVD Drive, Blu-ray Drive"))
                .row(GuideCell.words("BD-ROM"), GuideCell.words("Blu-ray"), GuideCell.words("No"),
                        GuideCell.words("Blu-ray Drive"))
                .row(GuideCell.words("BD-RE"), GuideCell.words("Blu-ray"), GuideCell.words("Yes"),
                        GuideCell.words("Blu-ray Drive"))
                .row(GuideCell.words("USB Flash Drive"), GuideCell.words("USB"), GuideCell.words("Yes"),
                        GuideCell.words("Dock Station, USB port of a case"))
                .paragraph("Writable means that the computer can write to the medium as well as read it.")
                .define("Tray", "The sliding part that a disc sits on. The eject button beside the drive moves it out"
                        + " and back in.")
                .paragraph("A floppy disk goes into the slot of its Floppy Drive, and a USB flash drive goes into its"
                        + " port with no tray in the way. A disc needs its tray open first.")
                .steps("Press the eject button beside the drive to bring the tray out.",
                        "Click the drive with the disc in your hand, so that it lies on the tray.",
                        "Press the eject button again, or push the tray in with an empty hand.")
                .paragraph("A computer sees a drive with its tray out as empty. The tray also stays shut while the"
                        + " computer reads the disc, that is, while it installs from the disc. To take a disc out,"
                        + " sneak-click the drive while its tray is open.")
                .ifSomethingGoesWrong("This CD drive cannot read that disc.",
                        "Each medium goes in a drive of its own kind: a CD goes in a CD Drive, and a DVD in a DVD"
                                + " Drive or a Blu-ray Drive.",
                        "The tray is closed - press the eject button to open it.",
                        "Press the eject button beside the drive.",
                        "The drive is reading - wait for it to finish.",
                        "The computer is installing from the disc. Wait until the installation has finished.",
                        "The drive already holds a disc - sneak-click to eject it.",
                        "Sneak-click the drive while its tray is open.")
                .seeAlso("jsc:dock_station", "jsc:installing", "jsc:case_front")
                .register();
    }

    private static void dockStation(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("dock_station", section).titled("The Dock Station").icon(ComputingModule.DOCK_STATION)
                .paragraph("The Dock Station reads storage disks and Universal Serial Bus (USB) flash drives for a"
                        + " computer. You join it to a computer like any device ([](jsc:peripheral_cables)), and it"
                        + " takes a cable of any era. It has three bays for disks, and a USB port for one flash"
                        + " drive.")
                .scene("A Dock Station joined to a Personal Computer, with a disk in each of its three bays and a USB"
                        + " flash drive in its port. Drag the scene to see the back.", 120)
                .place(12, 0, 14, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south", "")
                .place(13, 0, 14, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}")
                .place(14, 0, 14, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,48,0,0,0,0,0]}")
                .place(15, 0, 14, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}")
                .place(15, 0, 15, () -> ComputingModule.DOCK_STATION.get(), "facing=south,loaded=true",
                        "{LinkedOwner:3298534940672L,MediaSlot:{Size:1,Items:[{Slot:0,"
                                + "id:\"jsc:usb_flash_drive\",count:1}]},Bays:{Size:3,Items:["
                                + "{Slot:0,id:\"jsc:disk_hdd_500g\",count:1},"
                                + "{Slot:1,id:\"jsc:disk_ssd_500g\",count:1},"
                                + "{Slot:2,id:\"jsc:disk_nvme_500g\",count:1}]}}")
                .view(-35, 25, 1.35F)
                .balloon(1, GuideBlock.ScenePart.of(15, 0, 15).box(5, 10, 15, 14, 14, 16), "The 3.5\" bay",
                        "It takes a hard disk.")
                .balloon(2, GuideBlock.ScenePart.of(15, 0, 15).box(5, 6, 15, 14, 9, 16), "The 2.5\" bay",
                        "It takes a solid-state drive.")
                .balloon(3, GuideBlock.ScenePart.of(15, 0, 15).box(5, 2, 15, 14, 5, 16), "The M.2 tray",
                        "It takes a Non-Volatile Memory Express (NVMe) disk.")
                .balloon(4, GuideBlock.ScenePart.of(15, 0, 15).box(2, 7, 15, 4, 9, 16), "The USB port",
                        "It takes one USB flash drive.")
                .balloon(5, GuideBlock.ScenePart.of(15, 0, 14), "The Peripheral Cable")
                .balloon(6, GuideBlock.ScenePart.of(12, 0, 14), "The computer")
                .define("Bay", "A tray of the Dock Station that takes one disk of one size.")
                .table("The bays and the port", "Place", "Takes", "Shown on the Dock")
                .row(GuideCell.words("Bay"), GuideCell.words("A hard disk"), GuideCell.words("3.5\" BAY"))
                .row(GuideCell.words("Bay"), GuideCell.words("A solid-state drive (SSD)"),
                        GuideCell.words("2.5\" BAY"))
                .row(GuideCell.words("Bay"), GuideCell.words("A Non-Volatile Memory Express (NVMe) disk"),
                        GuideCell.words("M.2 TRAY"))
                .row(GuideCell.words("Port"), GuideCell.words("A USB flash drive"), GuideCell.words("USB"))
                .paragraph("A bay takes one disk, and only a disk of its own size. The Dock refuses anything"
                        + " that is not a disk or a flash drive.")
                .subheading("Putting things in and out")
                .steps("Click the Dock with a disk in your hand to put it in its bay.",
                        "Click the Dock with a USB flash drive in your hand to plug it into the port.",
                        "Sneak-click the Dock to take out the last thing you put in: the flash drive first, then the"
                                + " bays from the bottom.",
                        "Click the Dock with an empty hand to open its window.")
                .paragraph("A system's installer can be on a USB flash drive in the Dock, so that you install from the"
                        + " Dock ([](jsc:installing)).")
                .subheading("The window")
                .paragraph("The window shows each bay and the port, with what they hold. You can put disks in and"
                        + " take them out there too.")
                .table("Part of the window", "Part", "What it shows")
                .row(GuideCell.words("Link line"), GuideCell.words("\"Linked to a computer\" when a computer reaches"
                        + " the Dock, and \"No computer linked\" when none does."))
                .row(GuideCell.words("Bay rows"), GuideCell.words("Each bay, empty, or with its disk and how much of"
                        + " it is used."))
                .row(GuideCell.words("Port row"), GuideCell.words("The USB port, with the flash drive in it, or"
                        + " empty."))
                .row(GuideCell.words("Letters"), GuideCell.words("The drive letter the computer gives each disk and"
                        + " the flash drive."))
                .define("Drive letter", "The short name a computer gives a drive or a docked disk, so that programs"
                        + " can find it.")
                .paragraph("The computer sees each disk in the Dock as an external drive, and it can open and copy to"
                        + " it. The Dock works only while a computer is linked to it.")
                .ifSomethingGoesWrong("That tray already holds a disk - sneak-click or use the window to take it out.",
                        "Take the disk out in the window first.",
                        "Not docked to a computer",
                        "Join the Dock to a computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "No computer linked",
                        "Join the Dock to a computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "Nothing happens when you click the Dock.",
                        "Only disks and USB flash drives go in the Dock. Anything else is refused, and the Dock says"
                                + " nothing.")
                .seeAlso("jsc:drives", "jsc:disks", "jsc:installing")
                .register();
    }

    private static void printers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("printers", section).titled("Printers").icon(ComputingModule.PRINTER)
                .coversAll(blocksOf(PrinterBlock.class))
                .coversAll(itemsOf(PrintedPaperItem.class))
                .paragraph("A printer puts a document on paper. Each era has its own printer, and each one draws the"
                        + " picture on paper its own way, as the real printers of that time did. Join a printer to a"
                        + " computer, put plain paper in its tray, and print from a program.")
                .paragraph("A picture can come out on paper too. A sheet with a picture can hang in an item"
                        + " frame, where it fills the face of the frame. Right-click a Printed Paper sheet to read"
                        + " it.")
                .table("The printers", "Printer", "Era", "Ink", "Colour")
                .row(GuideCell.words("Epsilon FX-80"), GuideCell.words("Vintage"), GuideCell.words("Dot matrix"),
                        GuideCell.words("Black and white"))
                .row(GuideCell.words("Pakard DeskJot 940"), GuideCell.words("Legacy"),
                        GuideCell.words("Inkjet, in coarse dots"), GuideCell.words("Colour"))
                .row(GuideCell.words("Pakard FotoSmart C4280"), GuideCell.words("Transition"),
                        GuideCell.words("Inkjet, in fine dots"), GuideCell.words("Colour"))
                .row(GuideCell.words("Pakard LaserJot 1102"), GuideCell.words("Standard"),
                        GuideCell.words("Laser, with toner"), GuideCell.words("Black and white"))
                .row(GuideCell.words("Epsilon EcoTonk ET-2720"), GuideCell.words("Advanced"),
                        GuideCell.words("Ink tank, the picture as it is"), GuideCell.words("Colour"))
                .paragraph("The printer's tooltip, and the print dialog, say which printers print in black and white:"
                        + " \"Prints in black and white\", or \"Prints in color\". The dialog also shows the word Mono"
                        + " or Color beside the printer's name.")
                .paragraph("The FX-80 draws in black dots, one pass for every eight rows. The DeskJot leaves bands of"
                        + " colour, and the FotoSmart prints the same inks in finer dots. The LaserJot uses toner, and"
                        + " the EcoTonk prints the picture as it is.")
                .subheading("How long a page takes")
                .table("Ticks for one page", "Printer", "Ticks per page")
                .row(GuideCell.words("Epsilon FX-80"), GuideCell.words("144 ticks"))
                .row(GuideCell.words("Pakard DeskJot 940"), GuideCell.words("120 ticks"))
                .row(GuideCell.words("Pakard FotoSmart C4280"), GuideCell.words("100 ticks"))
                .row(GuideCell.words("Pakard LaserJot 1102"), GuideCell.words("70 ticks"))
                .row(GuideCell.words("Epsilon EcoTonk ET-2720"), GuideCell.words("110 ticks"))
                .paragraph("A tick is a twentieth of a second, so 20 ticks make a second. The fewer ticks a page takes,"
                        + " the faster the printer.")
                .subheading("Paper and the output")
                .paragraph("The tray takes plain paper only. Right-click the printer with paper to fill its tray. Each"
                        + " page takes one sheet from the tray. When the tray is empty, the job waits with the status"
                        + " \"no paper\".")
                .paragraph("The printer puts finished pages in its three output places. When they are full, the status"
                        + " says \"take the sheets\". Take the sheets from the printer's window: shift-click a sheet to"
                        + " move it into your inventory.")
                .subheading("The queue")
                .paragraph("The queue is the list of jobs waiting to print, and the first job is the one printing. The"
                        + " printer keeps 16 jobs, and a job can ask for 1 to 99 copies. A job sent to a full queue is"
                        + " refused. In the printer's window, PAUSE stops the printing, RESUME starts it again, and"
                        + " CANCEL JOB cancels the job.")
                .paragraph("The print dialog names the printer's state: \"Ready\", \"Printing\", \"Paused\", \"Out of"
                        + " paper\" or \"Output tray full\".")
                .subheading("The printer's window")
                .table("Part of the window", "Part", "What it shows")
                .row(GuideCell.words("QUEUE"), GuideCell.words("The jobs waiting to print."))
                .row(GuideCell.words("NOW"), GuideCell.words("The job that is printing."))
                .row(GuideCell.words("SHEET"), GuideCell.words("The progress of the job, sheet by sheet."))
                .row(GuideCell.words("OUT"), GuideCell.words("The output places, and the sheets in them."))
                .row(GuideCell.words("PAPER"), GuideCell.words("The sheets left in the tray, with the note"
                        + " \"sheets from the tray, one a page\"."))
                .row(GuideCell.words("PAUSE, RESUME, CANCEL JOB"), GuideCell.words("Buttons: stop the printing,"
                        + " start it again, or cancel the job."))
                .subheading("Printing from the command line")
                .paragraph("On the command line, a file goes to a printer with a command. The MC-DOS prompt uses PRINT,"
                        + " and the shells of Linux, FreeBSD and UNIX System V use lp. The commands are explained in"
                        + " full in the MC-DOS book ([](jsc:dos_printing)) and in the Linux books"
                        + " ([](jsc:printing)).")
                .table("The print commands", "Command", "System", "What it does")
                .row(GuideCell.code("PRINT"), GuideCell.words("MC-DOS"),
                        GuideCell.words("Prints text files in the background."))
                .row(GuideCell.code("lp"), GuideCell.words("Linux, FreeBSD and UNIX System V"),
                        GuideCell.words("Sends files to a printer. Its -d option names the printer, and -n the"
                                + " copies."))
                .row(GuideCell.code("lpstat"), GuideCell.words("Linux, FreeBSD and UNIX System V"),
                        GuideCell.words("Lists the print queue, and whether each printer is idle."))
                .paragraph("For lp, each printer has a name. Give it with -d. The name is also the start of the request"
                        + " id that lp answers with: the name, a dash and a number.")
                .table("Printer and its name for lp", "Printer", "Name for lp")
                .row(GuideCell.words("Epsilon FX-80"), GuideCell.code("epsilon"))
                .row(GuideCell.words("Pakard DeskJot 940"), GuideCell.code("deskjot"))
                .row(GuideCell.words("Pakard FotoSmart C4280"), GuideCell.code("fotosmart"))
                .row(GuideCell.words("Pakard LaserJot 1102"), GuideCell.code("laserjot"))
                .row(GuideCell.words("Epsilon EcoTonk ET-2720"), GuideCell.code("ecotonk"))
                .ifSomethingGoesWrong("Nothing comes out.",
                        "Read the status. \"no paper\" means the tray is empty, so add plain paper. \"take the sheets\""
                                + " means the output is full, so take the sheets out.",
                        "No printer is connected to this computer.",
                        "Join the printer to the computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "The printer says it cannot take more documents: its queue is full.",
                        "Wait for the queue to empty, or cancel the job with CANCEL JOB.",
                        "A picture in colour came out in black and white.",
                        "Print it on a printer that prints in colour: the Legacy, Transition or Advanced printer.",
                        "The lp command says the destination is \"non-existent\".",
                        "Use the name of a printer joined to the computer. lpstat shows them.")
                .seeAlso("jsc:peripheral_cables", "jsc:dos_printing", "jsc:printing")
                .register();
    }

    private static void speakers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("speakers", section).titled("Speakers and the subwoofer").icon(ComputingModule.SPEAKER)
                .coversAll(blocksOf(SpeakerBlock.class, SubwooferBlock.class))
                .paragraph("A speaker plays a computer's sound somewhere other than the monitor. The Vintage has no"
                        + " speaker; the Legacy and the later eras do. Join a speaker to the computer with a peripheral"
                        + " cable. It takes an audio output of the sound card, or of the board from the Transition on,"
                        + " and each audio output drives two speakers.")
                .define("Stereo", "A recording with two channels: a left side and a right side.")
                .paragraph("Two speakers beside a monitor play a stereo recording: the one on the left, as you face the"
                        + " monitor, plays the left side, and the one on the right plays the right side. A speaker on"
                        + " its own, or one that is not beside a monitor, plays both sides.")
                .paragraph("The speaker's window has a CHANNEL, with one of these settings:")
                .table("The channel of a speaker", "Channel", "What it plays")
                .row(GuideCell.words("Mono, alone"),
                        GuideCell.words("Both sides, as the only speaker of the computer."))
                .row(GuideCell.words("Mono"), GuideCell.words("Both sides, as one of several speakers."))
                .row(GuideCell.words("Left, by position"), GuideCell.words("The left side, from its place beside the"
                        + " monitor."))
                .row(GuideCell.words("Right, by position"), GuideCell.words("The right side, from its place beside the"
                        + " monitor."))
                .row(GuideCell.words("Off: monitor only"), GuideCell.words("No sound from this speaker: the"
                        + " computer's sound plays only out of the monitor."))
                .row(GuideCell.words("None"), GuideCell.words("No sound at all, for a speaker that is not linked"
                        + " to the computer."))
                .paragraph("The speaker's window also shows what it PLAYS. A Legacy speaker plays \"22 kHz, bass and"
                        + " treble cut\", so the bass and the treble are cut. A Transition satellite plays"
                        + " \"Bass cut: no subwoofer\". A Standard or Advanced speaker plays \"The whole range\".")
                .define("Satellite", "The small speaker of the Transition set, which plays everything but the bass.")
                .define("Subwoofer", "A box, with no cable of its own, that plays the bass the satellites leave out.")
                .paragraph("Stand a subwoofer against one of the computer's linked Transition speakers, on any of that"
                        + " speaker's six faces. Then the Transition speakers of the computer play the bass too. A"
                        + " computer needs only one subwoofer: a second one adds nothing.")
                .paragraph("Give each speaker a name of up to 32 characters. Programs find a speaker by its name, so no"
                        + " two speakers of one computer can share a name.")
                .ifSomethingGoesWrong("The music has no bass.",
                        "Stand a Transition subwoofer against one of the linked Transition speakers.",
                        "The speaker's window says \"Not linked\".",
                        "Join the speaker to the computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "Another speaker already has this name",
                        "Pick another name for this speaker.")
                .seeAlso("jsc:sound_cards", "jsc:peripheral_cables", "jsc:monitors")
                .register();
    }

    private static void redstoneInterfaces(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("redstone_interfaces", section).titled("Redstone Interfaces")
                .icon(ComputingModule.STANDARD_REDSTONE_INTERFACE)
                .coversAll(blocksOf(RedstoneInterfaceBlock.class))
                .paragraph("A Redstone Interface is a small block that lets a program read the redstone signal"
                        + " at one of its faces, or emit a signal from that face. Redstone is the wiring of Minecraft."
                        + " A computer"
                        + " reaches the interface through a peripheral cable, as it reaches its other devices"
                        + " ([](jsc:peripheral_cables)).")
                .scene("A Redstone Interface, its lens toward a redstone lamp that it lights, cabled to its computer."
                        + " Drag the scene to see the back.", 110)
                .place(14, 0, 10, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south", "")
                .place(13, 0, 10, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,8240,0,0,0,0,0]}")
                .place(12, 0, 10, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2088,0,0,0,0,0]}")
                .place(12, 0, 11, () -> ComputingModule.STANDARD_REDSTONE_INTERFACE.get(), "facing=south",
                        "{Emits:1b,Strength:15,LinkedOwner:3848290738176L}")
                .place(12, 0, 12, "minecraft:redstone_lamp", "lit=true", "")
                .balloon(1, GuideBlock.ScenePart.of(12, 0, 11).box(5, 1, 15, 11, 7, 16), "The lens",
                        "It reads or emits the redstone signal at the face it looks at.")
                .balloon(2, GuideBlock.ScenePart.of(12, 0, 11), "The Redstone Interface")
                .balloon(3, GuideBlock.ScenePart.of(12, 0, 12), "A redstone lamp",
                        "It lights while the interface emits a signal.")
                .balloon(4, GuideBlock.ScenePart.of(12, 0, 10), "The Peripheral Cable",
                        "It plugs into the back of the interface.")
                .balloon(5, GuideBlock.ScenePart.of(14, 0, 10), "The computer")
                .define("Lens", "The face of the block that reads or emits a signal. It is the face you look at when"
                        + " you place the block.")
                .paragraph("Place it the way you place an observer. Its lens points toward what you look at. Its back,"
                        + " where the cable plugs in, can face any way. Only the lens reads or emits.")
                .paragraph("The interface has two modes. Its signal strength runs from 0 to 15 in either mode.")
                .define("Signal strength", "How strong a redstone signal is, from 0 to 15.")
                .table("The two modes", "Mode", "What it does", "What the window shows")
                .row(GuideCell.words("IN"), GuideCell.words("Reads the signal that comes into its lens."),
                        GuideCell.words("Reads, with the strength"))
                .row(GuideCell.words("OUT"), GuideCell.words("Emits a signal from its lens."),
                        GuideCell.words("Emits, with the strength"))
                .paragraph("A program sets the mode and the strength. The window says which program set them last.")
                .paragraph("Name the interface in its window, with up to 32 characters. Programs find an interface by"
                        + " that name, and no two interfaces of one computer can share a name.")
                .paragraph("The interface has no power of its own. With no computer at the other end of its cable, its"
                        + " window says \"Off, no power\", and it reads and emits nothing. It keeps its mode and its"
                        + " strength until a computer is linked to it again.")
                .subheading("The window")
                .table("Part of the window", "Part", "What it shows")
                .row(GuideCell.words("NAME"), GuideCell.words("The name of the interface. Programs find it by"
                        + " this name."))
                .row(GuideCell.words("SIGNAL"), GuideCell.words("Whether the interface reads or emits, with the"
                        + " strength."))
                .row(GuideCell.words("STRENGTH"), GuideCell.words("The signal strength, from 0 to 15."))
                .row(GuideCell.words("MODE"), GuideCell.words("IN or OUT."))
                .row(GuideCell.words("SOFTWARE"), GuideCell.words("The program that last set the mode and the"
                        + " strength, after \"Set by\"."))
                .ifSomethingGoesWrong("It reads and emits nothing.",
                        "Its window says \"Off, no power\". Join the interface to a computer with a peripheral cable"
                                + " ([](jsc:peripheral_cables)).",
                        "The name is refused: \"Another interface already has this name\".",
                        "Pick another name for this interface, in its window.",
                        "A program cannot find the interface.",
                        "Check the name in its window. Programs find an interface only by its name.")
                .seeAlso("jsc:redstone_programs", "jsc:redstone_library")
                .register();
    }

    private static void kvmSwitch(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("kvm_switch", section).titled("The KVM Switch").icon(ComputingModule.KVM_SWITCH)
                .paragraph("A KVM Switch lets one monitor reach every computer of a rack. It is a rack unit that you"
                        + " mount in a Server Rack ([](jsc:server_racks)). Its tooltip reads \"Lets one monitor address"
                        + " every machine in the rack\".")
                .define("Rack unit", "One slot of height in a rack, counted in U. A KVM Switch takes one.")
                .paragraph("Each computer of the rack is a channel. A rack with one computer needs no KVM Switch: the"
                        + " monitor shows that computer. With two or more, the monitor first shows the channel bar, and"
                        + " you pick the machine you want.")
                .define("Channel", "One computer of a rack, chosen on the bar of a KVM Switch.")
                .table("The channel bar", "Part", "What it shows")
                .row(GuideCell.words("KVM SWITCH"), GuideCell.words("The title of the bar."))
                .row(GuideCell.words("Count"), GuideCell.words("How many machines the rack holds, for example \"2"
                        + " machines\"."))
                .row(GuideCell.words("F1 to F8"), GuideCell.words("The label of each channel, one row for each"
                        + " computer."))
                .row(GuideCell.words("Name"), GuideCell.words("The name of the machine. A machine with no name shows"
                        + " its place in the rack, such as \"bay 3U\"."))
                .row(GuideCell.words("Status"), GuideCell.words("ONLINE when the machine runs, and OFF when it does"
                        + " not."))
                .paragraph("Pick a channel with a click on its row, or with the number keys 1 to 8. The machine's own"
                        + " screen then opens on top. The bar holds 8 channels at most.")
                .ifSomethingGoesWrong("This rack holds several computers - mount a KVM Switch to pick one",
                        "Mount a KVM Switch in a free unit slot of the rack ([](jsc:server_racks)).",
                        "The rack holds no computer to show",
                        "Put a computer in the rack.",
                        "The channel says OFF.",
                        "That machine is not running: switch it on at its case.")
                .seeAlso("jsc:server_racks", "jsc:rack_equipment", "jsc:monitors")
                .register();
    }

    private static void disableDevice(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("disable_device", section).titled("Disabling a device")
                .paragraph("You can disable a device, so that its computer stops using it. The device keeps its port,"
                        + " and enabling it again brings it back. You disable a device when you want the computer to"
                        + " stop reading it, or writing to it.")
                .define("Disable", "To stop a computer from using a device. The device keeps its port until it is"
                        + " enabled again.")
                .subheading("In the Device Manager")
                .paragraph("On Frames, the Device Manager ([](jsc:device_manager)) lists the devices by type, by"
                        + " connection or by port.")
                .steps("Open the Device Manager.",
                        "Select the device.",
                        "Press Disable, or Enable if the device is already disabled.")
                .paragraph("The status line says that the device is disabled, and that the computer stops reading and"
                        + " writing it. If a device is missing from the list, use the \"Show hidden devices\""
                        + " option.")
                .subheading("On MC-DOS and MC-NET")
                .paragraph("MC-DOS and MC-NET have no Device Manager. Their Midsoft Diagnostics program, MSD"
                        + " ([](jsc:dos_msd)), takes the whole terminal. Press L for the LPT ports, or C for the COM"
                        + " ports. LPT and COM are the old names of the parallel and the serial ports.")
                .table("The LPT and COM Ports list", "Column", "What it shows")
                .row(GuideCell.words("Port"), GuideCell.words("The name of the port."))
                .row(GuideCell.words("Attached"), GuideCell.words("What is plugged into the port."))
                .row(GuideCell.words("Status"), GuideCell.words("On, or Off (disabled)."))
                .paragraph("In the list, press E to enable the device on the port you picked, or D to disable it. Enter"
                        + " puts the list away, and F3 exits MSD.")
                .subheading("On the Common Desktop Environment (CDE) and KDE")
                .paragraph("On CDE, the Workstation Info window has a Devices... button ([](jsc:cde_workstation_info))."
                        + " In its Devices dialog, select a device, then press Disable or Enable.")
                .paragraph("On KDE, the Info Center page Devices by port has a button for the selected device, which"
                        + " disables or enables it ([](jsc:kde_plasma)).")
                .paragraph("A disabled device goes quiet. A monitor shows \"No signal - the computer has disabled this"
                        + " monitor\", and a Redstone Interface reads and emits nothing. The computer also stops"
                        + " reading a drive, or the disks of the Dock Station.")
                .ifSomethingGoesWrong("A device is dark, or not seen, after you disabled it.",
                        "Enable it again: Enable in the Device Manager or in Workstation Info, or E in the MSD list.",
                        "The Device Manager does not list a device.",
                        "Use the \"Show hidden devices\" option.")
                .seeAlso("jsc:device_manager", "jsc:dos_msd", "jsc:cde_workstation_info", "jsc:kde_plasma")
                .register();
    }
}
