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
import net.minecraft.world.item.Items;

/**
 * The pages of the Guide to Operations' storage section: the servers that keep the network's disks, the disks of
 * each computer, where incoming items go, the External Storage Bus, fluids and chemicals, and the messages that
 * tell the player storage went wrong. Each page names the parts it explains and links to the pages that give the
 * rest, so no subject is written twice.
 */
final class StoragePages {

    private StoragePages() {
    }

    /** Declares the storage section and its entries, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("storage").titled("Storage")
                .icon(ComputingModule.STORAGE_SERVER).in(chapter).register();
        storageServers(guide, section);
        localStorage(guide, section);
        storagePriority(guide, section);
        externalStorageBus(guide, section);
        fluids(guide, section);
        storageTrouble(guide, section);
    }

    private static void storageServers(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("storage_servers", section).titled("Storage servers").icon(ComputingModule.STORAGE_SERVER)
                .paragraph("The network's room for items is on disks, and the disks sit in servers. A server lives in"
                        + " a Server Rack, a cabinet with slots called rack units (U), and each rack unit is one"
                        + " slot of height. A storage server is the server built for disks: it is 2 rack units"
                        + " tall and has more disk bays than any other server.")
                .define("Storage server", "A server built for disks, with 8 disk bays in 2 rack units (U). Its"
                        + " disks give the network room for items.")
                .define("Rack unit (U)", "One slot of height in a Server Rack. A server takes one or more of"
                        + " them.")
                .paragraph("Each disk goes into a drive bay on the front of the server. The bays are hot-swap:"
                        + " you can pull a disk out and push another in while the server runs.")
                .table("The Storage Server")
                .property("Height", "2 rack units (U)")
                .property("Disk bays", "8 bays, each takes one disk")
                .property("Gadget bays", "2 bays for a RAID Controller or a Cache Card")
                .property("Processor sockets", "1 socket")
                .property("Expansion slots", "1 slot")
                .property("Era", "Standard")
                .paragraph("RAID (redundant array of independent disks) joins the disks of one server into one"
                        + " disk for the network. You put a RAID Controller in a gadget bay, and you choose the"
                        + " mode on the STORAGE page of the server's Setup ([](jsc:setup_utility)). The modes and"
                        + " their numbers are in [](jsc:raid_arrays).")
                .paragraph("The network counts the room of every disk in every server of a rack. A server with no"
                        + " disks adds nothing to the network's room.")
                .ifSomethingGoesWrong(
                        "A slot says \"Blocked - the chassis does not cable this slot\".",
                        "That slot cannot hold a disk or a gadget. Use another slot.",
                        "A slot says \"Blocked - no unit in this row cables these slots\".",
                        "That slot cannot hold a disk or a gadget. Use another slot.")
                .seeAlso("jsc:server_racks", "jsc:raid_arrays", "jsc:local_storage")
                .register();
    }

    private static void localStorage(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("local_storage", section).titled("Local storage").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("A disk does not carry its contents inside the item. The network keeps what the disks"
                        + " hold in one store for the whole save, and each disk item carries only a number that"
                        + " finds its part of that store. So what a disk holds travels with the disk: pull it from"
                        + " a computer and carry it to another machine, a rack or another dimension.")
                .paragraph("What a disk holds is counted in the same units as everything on the network: items,"
                        + " fluids and chemicals ([](jsc:items_as_data)). Once a disk holds something, its tooltip"
                        + " shows a line that starts with \"Used\".")
                .paragraph("Each computer keeps its own disks as local storage, and that storage works without any"
                        + " network. The computer shows its room in storage slots, a display unit of 64 items.")
                .define("Storage slot", "A unit the game uses to show a computer's room, 64 items each. It is not a"
                        + " limit on what a disk holds.")
                .table("Storage slots by computer", "Computer", "Disk slots", "Storage slots shown")
                .row(GuideCell.words("Personal Computer"), GuideCell.words("2"), GuideCell.words("up to 18"))
                .row(GuideCell.words("Crafting Computer"), GuideCell.words("2"), GuideCell.words("up to 18"))
                .row(GuideCell.words("Cluster Management Computer"), GuideCell.words("2"),
                        GuideCell.words("up to 18"))
                .row(GuideCell.words("Mainframe"), GuideCell.words("4"), GuideCell.words("up to 27"))
                .warning("Formatting a disk erases everything on it: the items the network keeps there, and its"
                        + " files and installed systems. Take out what you need first.")
                .paragraph("Pulling a disk out of a computer does not erase it. The disk keeps its data for the"
                        + " next machine it goes into. In a server's bay, the RAID mode decides"
                        + " ([](jsc:raid_arrays)).")
                .subheading("Who may read a disk")
                .paragraph("The network does not read a computer's disks unless you let it. Each disk has a public"
                        + " share: the part of it that the network may read, counted in per-mille (parts of a"
                        + " thousand). A new disk is fully private, so its public share starts at 0 per-mille.")
                .define("Per-mille", "Parts of a thousand: 1,000 is the whole disk, 100 is a tenth of it.")
                .table("Public share of a disk", "Share, in per-mille", "What the network does")
                .row(GuideCell.code("0"), GuideCell.words("Reads nothing from this disk. This is the default."))
                .row(GuideCell.code("500"), GuideCell.words("Reads half the disk. The other half stays private."))
                .row(GuideCell.code("1000"), GuideCell.words("Reads the whole disk."))
                .paragraph("On a computer, you set the public share of its system disk in the Settings program,"
                        + " under Network, as \"System disk public share\" ([](jsc:this_pc_and_desktop_tools)). Each"
                        + " step moves it by 5%.")
                .paragraph("The network can take items out of a computer's public part, but it never puts items"
                        + " into it. The disks of a server are always fully public.")
                .paragraph("In the Network Manager ([](jsc:network_manager)), each computer's line shows its share"
                        + " as \"Private\". So \"Private 90%\" means a tenth of that disk is public.")
                .ifSomethingGoesWrong(
                        "The Network Manager says \"Private 100%\" for your computer.",
                        "Its system disk shares nothing. Raise the public share of the system disk in Settings,"
                                + " under Network.")
                .seeAlso("jsc:items_as_data", "jsc:personal_computers", "jsc:mainframes", "jsc:disks")
                .register();
    }

    private static void storagePriority(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("storage_priority", section).titled("Storage priority").icon(HardwareItems.DISK_TRENCH_20M)
                .paragraph("When the network stores items, it must choose a storage for them, and the storage"
                        + " priority decides the order. The storage with the highest priority fills first. This"
                        + " is not the priority of an Operation ([](jsc:states_and_queues)), which orders the jobs"
                        + " themselves.")
                .define("Storage priority", "The number that orders the storages when items go in. The highest"
                        + " fills first, and the network's own storage counts as priority 0.")
                .subheading("When items go in")
                .steps("Look at the free room of every storage, and skip any that refuse the item.",
                        "Give the items to the storage with the highest priority, up to its free room.",
                        "Between storages with the same priority, prefer the faster one, then the one with more"
                                + " room.",
                        "Give the rest to the next storage in that order, and write them.")
                .paragraph("Some items go back to the server they came from first, as far as that server has room."
                        + " This is how the results of a craft return ([](jsc:asking_for_a_craft)).")
                .subheading("When items come out")
                .paragraph("Taking items out uses another order: the fastest storage first, then the fuller one."
                        + " Speed here means read latency: the ticks a storage waits before it gives items out.")
                .table("Read latency of the network's disks", "Disk", "Read latency")
                .row(GuideCell.words("Hard disk (HDD)"), GuideCell.words("10 ticks"))
                .row(GuideCell.words("Solid-state drive (SSD)"), GuideCell.words("3 ticks"))
                .row(GuideCell.words("NVMe (non-volatile memory express)"), GuideCell.words("1 tick"))
                .define("Read latency", "The ticks a storage waits before it gives items out. Fewer ticks is"
                        + " faster.")
                .paragraph("These are the default values, and the mod's configuration can change them.")
                .subheading("Priority on buses")
                .paragraph("Only an External Storage Bus has a storage priority, and only from the Standard era on."
                        + " Servers, racks and computers always count as 0. You set a bus's priority from -99 to 99."
                        + " The same number also decides which bus claims an item first"
                        + " ([](jsc:bus_keep_max_and_priority)).")
                .paragraph("A bus with a negative priority fills after the network's own storage, and a bus with a"
                        + " positive one fills before it.")
                .subheading("Faster reads and writes")
                .paragraph("A Cache Card in a gadget bay cuts the read latency of that bay's disks by 25%. The"
                        + " Predictive Cache service cuts it by 15%. Their cuts add together"
                        + " ([](jsc:rack_equipment)).")
                .paragraph("Inside one server, the Load Balancer service changes the write order. New items go to"
                        + " the emptiest disk first, instead of filling the disks in turn"
                        + " ([](jsc:network_services)).")
                .ifSomethingGoesWrong(
                        "The bus has no priority setting.",
                        "Priority starts at the Standard era. Use a Standard or newer External Storage Bus.")
                .seeAlso("jsc:external_storage_bus", "jsc:states_and_queues", "jsc:network_services")
                .register();
    }

    private static void externalStorageBus(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("external_storage_bus", section).titled("The External Storage Bus")
                .icon(ComputingModule.EXTERNAL_STORAGE_BUS_ITEM)
                .paragraph("An External Storage Bus lets the network use a chest, a tank or a machine as storage,"
                        + " with no disk in it. You put the bus on a data cable beside the inventory, and the network"
                        + " counts what the inventory holds as its own storage.")
                .define("Inventory", "Any chest, tank or machine that holds items or fluid, and that a bus can"
                        + " face.")
                .paragraph("Its tooltip reads \"Right-click a data cable beside an inventory; the network uses the"
                        + " inventory as its storage\". A bus mounts on cables of its own era or an earlier one.")
                .paragraph("Its storage is much slower than the network's own disks. The speed below shows by how"
                        + " much.")
                .paragraph("The Vintage, Legacy, Transition and Advanced versions carry their era in their names,"
                        + " such as \"Vintage External Storage Bus\".")
                .subheading("What each era can do")
                .table("The External Storage Bus by era", "Era", "Filter and access", "Priority",
                        "Tags and looser matching")
                .row(GuideCell.words("Vintage"), GuideCell.words("none"), GuideCell.words("no"),
                        GuideCell.words("no"))
                .row(GuideCell.words("Legacy and Transition"), GuideCell.words("yes"), GuideCell.words("no"),
                        GuideCell.words("no"))
                .row(GuideCell.words("Standard"), GuideCell.words("yes"), GuideCell.words("yes"),
                        GuideCell.words("no"))
                .row(GuideCell.words("Advanced"), GuideCell.words("yes"),
                        GuideCell.words("yes"), GuideCell.words("yes"))
                .paragraph("The External Storage Bus has no conditions in any era. Conditions belong to the Import"
                        + " Bus and the Export Bus ([](jsc:bus_conditions)).")
                .subheading("Access and filter")
                .paragraph("Access sets what the network may do with the inventory. It starts at the Legacy era,"
                        + " and a Vintage bus always reads and writes.")
                .table("Access of the bus", "Setting", "What the network does")
                .row(GuideCell.words("READ AND WRITE"), GuideCell.words("Takes items out of the inventory and puts"
                        + " items into it. This is the default."))
                .row(GuideCell.words("READ ONLY"), GuideCell.words("Only takes items out. It never fills the"
                        + " inventory."))
                .row(GuideCell.words("WRITE ONLY"), GuideCell.words("Only puts items in. What is in the inventory is"
                        + " never taken, nor seen."))
                .paragraph("The filter decides which items the bus lets through. With no item listed, the bus lets"
                        + " everything through. With items listed, only those pass, or with \"ALL BUT THESE\""
                        + " everything except them.")
                .paragraph("The bus window shows what the inventory holds under \"HOLDS\", as slots and how many are"
                        + " used, with the note \"shown to the network as its storage\".")
                .subheading("Speed")
                .paragraph("The bus moves a tenth of what its cable carries each tick, and at least one item. Its"
                        + " read latency is 100 ticks, which is 5 seconds: ten times the 10 ticks of a hard disk"
                        + " (HDD). A solid-state drive (SSD) answers about 33 times faster than the bus, and an NVMe"
                        + " disk 100 times faster.")
                .paragraph("The bus window says \"10x slower than the network's own storage.\" That holds against a"
                        + " hard disk only. A server of solid-state or NVMe disks is faster still.")
                .paragraph("The network looks at the inventory again every 10 ticks. So a change in the inventory can"
                        + " reach the network's list of items up to 10 ticks late.")
                .subheading("Room and refusals")
                .paragraph("The room the bus offers is the inventory's free room. A READ ONLY bus offers none. An"
                        + " INSERT skips the bus when it is READ ONLY, or when its filter does not let the item"
                        + " through.")
                .ifSomethingGoesWrong(
                        "The window says \"no inventory before it\".",
                        "The bus faces nothing. Put it on a data cable beside an inventory.",
                        "The window says \"on no network\".",
                        "Join the bus's cable to a network ([](jsc:data_cables)).",
                        "The window says \"the chest is full\".",
                        "The inventory has no free room. Take items out of it.",
                        "The network never puts items into the bus.",
                        "The bus is READ ONLY. Set it to READ AND WRITE.",
                        "The network never takes items from the bus, and never sees them.",
                        "The bus is WRITE ONLY. Set it to READ AND WRITE or READ ONLY.",
                        "The bus has no READ ONLY setting.",
                        "Access starts at the Legacy era. Use a Legacy or newer External Storage Bus.")
                .seeAlso("jsc:buses", "jsc:storage_priority", "jsc:storage_trouble")
                .register();
    }

    private static void fluids(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("fluids", section).titled("Fluids and chemicals").icon(() -> Items.WATER_BUCKET)
                .paragraph("The network keeps fluids the way it keeps items: as data, counted in millibuckets (mB)."
                        + " A bucket holds 1,000 millibuckets, so a full bucket takes exactly as much room as one"
                        + " item ([](jsc:items_as_data)).")
                .define("Millibucket (mB)", "One thousandth of a bucket. Fluids and chemicals are counted in these,"
                        + " and each one takes one thousandth of an item's room.")
                .paragraph("An External Storage Bus can face a tank of any mod. The network then uses the tank's"
                        + " fluid as storage, as it does with items.")
                .paragraph("Fluid also comes in and goes out in containers. You can use a bucket or a tank for it, and"
                        + " each behaves its own way.")
                .table("Containers and the network", "Container", "What happens")
                .row(GuideCell.words("Bucket"), GuideCell.words("Gives up its whole 1,000 mB, or nothing. The empty"
                        + " bucket comes back."))
                .row(GuideCell.words("Tank item"), GuideCell.words("Gives up as much as fits, up to what the item"
                        + " allows at once. The emptied tank comes back."))
                .paragraph("Taking fluid out works the other way. A bucket is filled only with a full bucket's worth."
                        + " A refill puts back whatever the network could not take.")
                .subheading("Chemicals")
                .define("Chemical", "A substance that a mod such as Mekanism keeps in its own tanks.")
                .define("Bridge", "The piece of this mod that teaches the network how to read a chemical mod's"
                        + " tanks.")
                .paragraph("Chemicals are stored as data too, and a chemical takes room in millibuckets, like a"
                        + " fluid. They need Mekanism: install it, and a chemical bridge lets the network read its"
                        + " chemical tanks ([](jsc:mekanism)).")
                .ifSomethingGoesWrong(
                        "The fluid does not go in.",
                        "The network's storage has no room left. Add disks or servers"
                                + " ([](jsc:storage_trouble)).",
                        "No chemical shows in the network's storage.",
                        "Install Mekanism. The network reads chemicals only through it.")
                .seeAlso("jsc:items_as_data", "jsc:external_storage_bus", "jsc:mekanism")
                .register();
    }

    private static void storageTrouble(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("storage_trouble", section).titled("When storage goes wrong")
                .icon(ComputingModule.RAID_CONTROLLER)
                .paragraph("Storage goes wrong in a few known ways, and the game says which. An"
                        + " [Operation](jsc:what_operations_are) that does not finish its whole job shows its status"
                        + " in the Network Manager ([](jsc:network_manager)): \"partial\" when some of the job was"
                        + " done, \"failed\" when none was, and \"locked\" when it gave up waiting.")
                .paragraph("Each Operation reserves the items it will take, and another Operation that needs the same"
                        + " items waits for it. In the messages below, <item> stands for the name of"
                        + " the item.")
                .subheading("Operations on the network")
                .table("Messages of an Operation", "Message", "What it means, and the fix")
                .row(GuideCell.words("\"the network had no room left for the <item>\""),
                        GuideCell.words("Not all of the items fit. Add disks or servers to the network."))
                .row(GuideCell.words("\"the network was not holding enough <item>\""),
                        GuideCell.words("The network had less of the item than you asked for. Stock more of it, or"
                                + " ask for less."))
                .row(GuideCell.words("\"another Operation was holding the <item> it needed\""),
                        GuideCell.words("Another Operation reserved the items this one needs. Wait for it, or cancel"
                                + " it ([](jsc:cancelling_and_the_log))."))
                .subheading("Buses and inventories")
                .table("Messages at a bus and an inventory", "Message", "What it means, and the fix")
                .row(GuideCell.words("\"the chest is full\""),
                        GuideCell.words("The inventory has no free room. Take items out of it."))
                .row(GuideCell.words("\"on no network\""),
                        GuideCell.words("The bus's cable is not on a network. Join it to one"
                                + " ([](jsc:data_cables))."))
                .row(GuideCell.words("\"no inventory before it\""),
                        GuideCell.words("The bus faces nothing. Put it on a data cable beside an inventory."))
                .row(GuideCell.words("ON DEMAND, with \"Moves only while its cable has a redstone signal\""),
                        GuideCell.words("The bus moves only when its cable has a signal. Give the cable a redstone"
                                + " signal, or change the bus's mode."))
                .ifSomethingGoesWrong(
                        "The bus moves nothing.",
                        "Its filter lets nothing through, its mode is ON DEMAND with no redstone signal on its"
                                + " cable, or it is on no network. Check each of these.")
                .subheading("Disks and racks")
                .table("Messages about disks and RAID", "Message", "What it means, and the fix")
                .row(GuideCell.words("\"<mode> - needs <number>+ drives\""),
                        GuideCell.words("The bay has fewer disks than that RAID mode needs, so the change is refused."
                                + " Add disks, then set the mode again."))
                .row(GuideCell.words("\"<name> DEGRADED\""),
                        GuideCell.words("The array lost disks but still serves its items. Put a replacement disk in"
                                + " the bay ([](jsc:raid_arrays))."))
                .row(GuideCell.words("\"<name> FAILED\""),
                        GuideCell.words("More disks are gone than the mode can lose, so the array gives no room. Put"
                                + " the missing disks back."))
                .row(GuideCell.words("\"REBUILD <number>%\""),
                        GuideCell.words("A replacement disk is copying the array's items back. The array serves again"
                                + " when the copy is done."))
                .row(GuideCell.words("\"Blocked - the chassis does not cable this slot\""),
                        GuideCell.words("That slot cannot hold a disk or a gadget. Use another slot."))
                .warning("Pulling any disk out of a RAID 0 array erases the data on every disk of the array."
                        + " RAID 0 keeps no second copy.")
                .paragraph("Pulling a disk out of a RAID 1 or RAID 5 array moves what the other disks can take onto"
                        + " them. The array keeps working while it waits for a replacement, and it rebuilds when the"
                        + " replacement goes into the bay ([](jsc:raid_arrays)).")
                .subheading("Room on a disk")
                .ifSomethingGoesWrong(
                        "An Operation says \"the network had no room left for the <item>\".",
                        "Add disks or servers. A bigger disk holds more items. A newer era's disk holds more"
                                + " megabytes.",
                        "A file does not fit, and the computer says \"Not enough free space\".",
                        "Delete files you do not need, or use a bigger disk.",
                        "Installing a system says \"The target disk has no room for <system> (<number> MB needed).\"",
                        "Use a bigger disk. The number is in megabytes (MB), the room the system takes.")
                .seeAlso("jsc:local_storage", "jsc:external_storage_bus", "jsc:raid_arrays", "jsc:disks")
                .register();
    }
}
