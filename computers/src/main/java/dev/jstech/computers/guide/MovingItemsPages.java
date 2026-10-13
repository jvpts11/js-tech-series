/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the chapter on moving items: the buses that carry items between the network and the blocks it
 * touches, their eras and speeds, their settings and conditions, their windows and activity log, and how programs
 * set them.
 */
final class MovingItemsPages {

    /*
     * The cables of the buses scene. Lane 0 carries the access line; bit 5 is the east face and bit 4 the west
     * face. A bus sits on the up face (face 1), which its cable does not cross.
     */
    private static final String BUS_RUN =
            "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}";
    private static final String BUS_END_IMPORT = "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;32,0,0,0,0,0,0,0,0],"
            + "Parts:[{Face:1b,Type:\"jsc:import_bus\",Data:{}}]}";
    private static final String BUS_END_EXPORT = "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;16,0,0,0,0,0,0,0,0],"
            + "Parts:[{Face:1b,Type:\"jsc:export_bus\",Data:{}}]}";

    private MovingItemsPages() {
    }

    /** Declares the sections and entries of the moving items chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        busesAndEras(guide, chapter);
        busSettings(guide, chapter);
        busWindows(guide, chapter);
        busProgramming(guide, chapter);
    }

    private static void busesAndEras(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("buses_and_eras").titled("Buses")
                .icon(ComputingModule.IMPORT_BUS_ITEM).in(chapter).register();
        guide.page("buses", section).titled("Buses").icon(ComputingModule.IMPORT_BUS_ITEM)
                .coversAll(ComputersGuide.partsOf(kind -> ComputingParts.isImport(kind)
                        || ComputingParts.isExport(kind) || ComputingParts.isExternal(kind)))
                .paragraph("Buses are thin parts you put on the face of a data cable, against a chest or a"
                        + " machine, so the network can reach it without you.")
                .scene("An Import Bus on one cable face pulls items out of a chest, and an Export Bus on another"
                        + " face fills a furnace. The cable between them joins both to the network. Drag it to look"
                        + " at the buses from the side.", 120)
                .place(1, 0, 1, () -> CoreCables.BLOCK.get(), "", BUS_END_IMPORT)
                .place(2, 0, 1, () -> CoreCables.BLOCK.get(), "", BUS_RUN)
                .place(3, 0, 1, () -> CoreCables.BLOCK.get(), "", BUS_RUN)
                .place(4, 0, 1, () -> CoreCables.BLOCK.get(), "", BUS_RUN)
                .place(5, 0, 1, () -> CoreCables.BLOCK.get(), "", BUS_END_EXPORT)
                .place(1, 1, 1, "minecraft:chest", "facing=south", "")
                .place(5, 1, 1, "minecraft:furnace", "facing=south,lit=false", "")
                .view(-35, 30, 1.35F)
                .balloon(1, GuideBlock.ScenePart.of(1, 0, 1).box(2, 11, 2, 14, 16, 14), "The Import Bus",
                        "It takes items from the chest above it and puts them into the network.")
                .balloon(2, GuideBlock.ScenePart.of(5, 0, 1).box(2, 11, 2, 14, 16, 14), "The Export Bus",
                        "It takes items from the network and puts them into the furnace above it.")
                .balloon(3, GuideBlock.ScenePart.of(1, 1, 1), "The chest")
                .balloon(4, GuideBlock.ScenePart.of(5, 1, 1), "The furnace")
                .balloon(5, GuideBlock.ScenePart.of(3, 0, 1), "The data cable",
                        "It carries the buses' work to the network.")
                .define("Bus", "A thin part on one face of a data cable that moves items between the network and"
                        + " the block it touches.")
                .paragraph("Every move a bus makes is an Operation ([](jsc:what_operations_are)) that the network's"
                        + " Mainframe runs. So no bus moves while its network has no Mainframe"
                        + " ([](jsc:mainframes)).")
                .paragraph("There are three kinds of bus. Each one reaches one block, the chest, tank or machine it"
                        + " faces.")
                .table("The kinds of bus", "Kind", "What it does")
                .row(GuideCell.words("Import Bus"),
                        GuideCell.words("Takes items from the block it faces and puts them into the network."))
                .row(GuideCell.words("Export Bus"),
                        GuideCell.words("Takes items from the network and puts them into the block it faces."))
                .row(GuideCell.words("External Storage Bus"),
                        GuideCell.words("Moves nothing itself: the network uses the inventory it faces as storage"
                                + " of its own."))
                .define("Import Bus", "A bus that takes items from the block it faces and puts them into the"
                        + " network.")
                .define("Export Bus", "A bus that takes items from the network and puts them into the block it"
                        + " faces.")
                .paragraph("Buses move fluids too ([](jsc:fluids)). The External Storage Bus has its own page"
                        + " ([](jsc:external_storage_bus)).")
                .subheading("Putting a bus on a cable")
                .steps("Hold the bus in your hand.",
                        "Right-click the data cable on the face where the bus should sit.",
                        "Right-click the placed bus to open its window ([](jsc:bus_window)).")
                .paragraph("If you point at the chest or the machine instead, the bus looks for a cable behind that"
                        + " face first, then for any cable next to it.")
                .paragraph("A bus mounts only on an access or a backbone cable of its own era or of an earlier one"
                        + " ([](jsc:data_cables), [](jsc:eras)). On any other cable it does not fit. A face that"
                        + " already holds a part, or that a wire crosses, takes no bus, and nothing happens.")
                .subheading("When a bus moves")
                .paragraph("A bus moves only when every requirement in this table holds. Check them in the bus's"
                        + " window when a bus stays still.")
                .table("What a bus needs before it moves", "Requirement", "What it means")
                .row(GuideCell.words("The cable is on a network"),
                        GuideCell.words("A cable on no network moves nothing, and the window's lamp says OFFLINE."))
                .row(GuideCell.words("The network has a Mainframe"),
                        GuideCell.words("Without a Mainframe, no bus moves."))
                .row(GuideCell.words("The bus is ON"),
                        GuideCell.words("A bus set to OFF stays still until you turn it ON."))
                .row(GuideCell.words("Its mode allows it"),
                        GuideCell.words("A bus on ON DEMAND moves only while its cable's block has a redstone"
                                + " signal."))
                .row(GuideCell.words("Its conditions hold"),
                        GuideCell.words("A condition that does not hold makes the bus wait."))
                .define("Condition", "A rule a bus checks before it moves, such as a stock count or an hour of the"
                        + " day.")
                .paragraph("A bus that has moved something in the last 60 ticks, which is 3 seconds, shows a busy"
                        + " model on its cable, and that model blinks.")
                .ifSomethingGoesWrong(
                        "The message \"Storage buses mount on access and backbone cables of their era or an earlier"
                                + " one\" appears on the action bar.",
                        "Use an access or a backbone cable of the bus's era or of an earlier era"
                                + " ([](jsc:data_cables)).",
                        "Right-clicking the cable does nothing, and no message appears.",
                        "The face already holds a part, or a wire crosses it. Pick another face of the cable.",
                        "The bus moves nothing.",
                        "Check the requirements in the table above. The Activity tab shows the holds the bus had,"
                                + " with their reasons ([](jsc:bus_activity)).")
                .seeAlso("jsc:bus_filters_and_tags", "jsc:buses_by_era")
                .register();
        guide.page("buses_by_era", section).titled("Buses by era")
                .paragraph("Each era of hardware has its own buses, and the newer buses carry more. You get each bus"
                        + " from the creative tabs, as with everything in this mod. Exa and Singularity use the"
                        + " Advanced bus.")
                .table("Bus items by era", "Era", "Import Bus", "Export Bus", "External Storage Bus")
                .row(GuideCell.words("Vintage"), GuideCell.words("Vintage Import Bus"),
                        GuideCell.words("Vintage Export Bus"), GuideCell.words("Vintage External Storage Bus"))
                .row(GuideCell.words("Legacy"), GuideCell.words("Legacy Import Bus"),
                        GuideCell.words("Legacy Export Bus"), GuideCell.words("Legacy External Storage Bus"))
                .row(GuideCell.words("Transition"), GuideCell.words("Transition Import Bus"),
                        GuideCell.words("Transition Export Bus"),
                        GuideCell.words("Transition External Storage Bus"))
                .row(GuideCell.words("Standard"), GuideCell.words("Import Bus"),
                        GuideCell.words("Export Bus"), GuideCell.words("External Storage Bus"))
                .row(GuideCell.words("Advanced"), GuideCell.words("Advanced Import Bus"),
                        GuideCell.words("Advanced Export Bus"), GuideCell.words("Advanced External Storage Bus"))
                .paragraph("The newer the era, the more you can set on the bus, and the faster it moves. The table"
                        + " shows the speed of each era's bus and what its window can set.")
                .table("Speed and features by era", "Era", "Speed", "What the window can set")
                .row(GuideCell.words("Vintage"), GuideCell.amount(1, "it/t"),
                        GuideCell.words("No filter: an Export Bus sends the one item in its cell, and an Import Bus"
                                + " takes what it finds first."))
                .row(GuideCell.words("Legacy"), GuideCell.amount(8, "it/t"),
                        GuideCell.words("A filter of up to 5 items, ONLY THESE or ALL BUT THESE, and one keep and one"
                                + " max for the whole bus."))
                .row(GuideCell.words("Transition"), GuideCell.amount(16, "it/t"),
                        GuideCell.words("A filter of up to 5 items, each with its own keep and max."))
                .row(GuideCell.words("Standard"), GuideCell.amount(32, "it/t"),
                        GuideCell.words("A filter, a keep and a max for the whole bus, a priority and conditions."))
                .row(GuideCell.words("Advanced"), GuideCell.amount(64, "it/t"),
                        GuideCell.words("Everything Standard has, plus up to 4 tags and loose matching."))
                .define("Filter", "The list of up to five items, fluids or chemicals that a bus is told to move.")
                .define("Tag", "A group of items with a shared name, such as c:ores. A bus can list a tag in place"
                        + " of single items.")
                .define("Keep", "How many items a bus leaves in the block it faces (Import), or fills that block up"
                        + " to (Export).")
                .define("Max", "The most items one move takes. Zero means as many as the speed allows.")
                .paragraph("A bus's speed is the lower of two numbers: what its era allows, and what its cable"
                        + " carries. A Standard Import Bus on a Standard access cable moves 32 items a tick. On a"
                        + " Legacy access cable it moves 16 items a tick, because that cable carries 16 items a tick. A"
                        + " Vintage"
                        + " bus moves 1 item a tick on any cable.")
                .table("What a cable carries", "Era", "Access line", "Backbone line")
                .row(GuideCell.words("Vintage"), GuideCell.amount(4, "it/t"), GuideCell.amount(16, "it/t"))
                .row(GuideCell.words("Legacy"), GuideCell.amount(16, "it/t"), GuideCell.amount(64, "it/t"))
                .row(GuideCell.words("Transition"), GuideCell.amount(64, "it/t"), GuideCell.amount(256, "it/t"))
                .row(GuideCell.words("Standard"), GuideCell.amount(128, "it/t"), GuideCell.amount(512, "it/t"))
                .row(GuideCell.words("Advanced"), GuideCell.amount(256, "it/t"), GuideCell.amount(2048, "it/t"))
                .paragraph("Storage buses do not mount on long-distance or HPC lines. A cable of a newer era carries"
                        + " more, so a newer cable lifts the speed of a bus on it.")
                .paragraph("The External Storage Bus has no speed of its own. The network reaches its inventory at"
                        + " one tenth of what the cable carries, and at least 1 item a tick. It has no POWER and no"
                        + " MODE setting. Its own page explains it ([](jsc:external_storage_bus)).")
                .ifSomethingGoesWrong(
                        "The bus moves fewer items than the table says.",
                        "The cable is the limit. Use a cable of a newer era, or an access or backbone line"
                                + " ([](jsc:data_cables)).")
                .seeAlso("jsc:buses", "jsc:eras")
                .register();
    }

    private static void busSettings(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("bus_settings").titled("Setting a bus")
                .icon(ComputingModule.EXPORT_BUS_ITEM).in(chapter).register();
        guide.page("bus_filters_and_tags", section).titled("Filters, tags and matching")
                .icon(ComputingModule.EXPORT_BUS_ITEM)
                .paragraph("A filter is the list of things a bus is told to move. It has five slots, and each slot"
                        + " holds one item, one fluid or one chemical. A filled fluid container selects its fluid.")
                .paragraph("The two options for the list are ONLY THESE and ALL BUT THESE. ONLY THESE moves what the"
                        + " list names. ALL BUT THESE moves everything the list does not name.")
                .subheading("What moves")
                .table("What moves through a filter", "Filter", "What moves")
                .row(GuideCell.words("Empty, ONLY THESE, on an Import Bus"),
                        GuideCell.words("Everything, because an empty list takes everything."))
                .row(GuideCell.words("Empty, ONLY THESE, on an Export Bus"),
                        GuideCell.words("Nothing. An Export Bus sends only what it lists."))
                .row(GuideCell.words("Empty, ALL BUT THESE"),
                        GuideCell.words("Everything."))
                .row(GuideCell.words("Items listed, ONLY THESE"),
                        GuideCell.words("Only the listed items."))
                .row(GuideCell.words("Items listed, ALL BUT THESE"),
                        GuideCell.words("Everything except the listed items."))
                .subheading("Setting the filter in the window")
                .steps("Hold an item in your hand.",
                        "Click a filter cell with the item, and the cell lists it.",
                        "To clear a cell, empty your hand and click the cell.",
                        "Choose ONLY THESE or ALL BUT THESE.")
                .paragraph("On a Transition bus, the + item button lists the item in the first free cell.")
                .subheading("Tags")
                .paragraph("Advanced buses also match tags. A tag names a group of items, such as c:ores, and a tag"
                        + " on the bus matches every item that carries it. The window takes up to 4 tags. Click"
                        + " + tag and type the tag, with or without the # sign.")
                .paragraph("A tag counts as a listed entry for ONLY THESE and ALL BUT THESE, the same way an"
                        + " item does.")
                .subheading("Loose matching")
                .paragraph("A filter normally matches an item exactly. An Advanced bus can match loosely instead,"
                        + " which the window calls FUZZY.")
                .define("Fuzzy matching", "Matching an item as the plain item it is, whatever its damage and its"
                        + " components are.")
                .paragraph("With FUZZY, a damaged or enchanted tool matches the plain tool. A fluid matches its fluid,"
                        + " not its bucket. The window's note reads \"Fuzzy: an item matches whatever its damage and"
                        + " its components.\"")
                .ifSomethingGoesWrong(
                        "The Export Bus sends nothing.",
                        "Its filter is empty and set to ONLY THESE, so it lists no item. List the items, or choose"
                                + " ALL BUT THESE.",
                        "The Import Bus takes more than you wanted.",
                        "Its filter is empty, and an empty list takes everything. List the items you want, with ONLY"
                                + " THESE.",
                        "A damaged or enchanted tool does not move.",
                        "Choose FUZZY for MATCH on an Advanced bus, so a damaged or enchanted tool matches its plain"
                                + " item.")
                .seeAlso("jsc:bus_keep_max_and_priority")
                .register();
        guide.page("bus_keep_max_and_priority", section).titled("Keep, max, priority and mode")
                .paragraph("Five settings decide how much your bus moves and when. KEEP and MAX set the amounts."
                        + " PRIORITY decides who goes first when buses compete. MODE and POWER decide whether a bus"
                        + " moves at all.")
                .table("The settings of a bus", "Setting", "What it does")
                .row(GuideCell.words("KEEP"),
                        GuideCell.words("How many stay in the chest on an Import Bus, or how many the chest is filled"
                                + " to on an Export Bus, from 0 to 1,000,000."))
                .row(GuideCell.words("MAX"),
                        GuideCell.words("The most one move takes, where zero means as much as the speed allows in"
                                + " one second."))
                .row(GuideCell.words("PRIORITY"),
                        GuideCell.words("Who goes first when two buses want the same thing, from -99 to 99 in the"
                                + " window, on Standard and later buses only."))
                .row(GuideCell.words("MODE"),
                        GuideCell.words("CONTINUOUS moves at all times, and ON DEMAND moves only while the cable's"
                                + " block has a redstone signal."))
                .row(GuideCell.words("POWER"),
                        GuideCell.words("ON lets the bus move, and OFF stops it."))
                .subheading("KEEP and MAX")
                .paragraph("KEEP works in opposite ways on the two buses. An Import Bus leaves that many items in the"
                        + " chest it faces, and takes only what is above that number. An Export Bus fills the chest up"
                        + " to that number, and then stops.")
                .paragraph("MAX is the most one move takes, not a total. Zero means no cap: the bus moves as much as"
                        + " its speed allows in one second, which is 20 ticks. The window shows"
                        + " \"any\" with the note \"a time\" when MAX is zero.")
                .subheading("Priority")
                .paragraph("Each bus that wants something says so with its priority, every tick it wants it. A bus"
                        + " waits while a bus of a higher priority on the same network has wanted the same thing in"
                        + " the last 2 ticks. Equal priorities both go. Priority does not make a bus faster. It only"
                        + " decides who goes first ([](jsc:priorities)).")
                .paragraph("An Export Bus wants the item it sends. An Import Bus wants room in the network, and counts"
                        + " as wanting it while the network has had no room for its moves in the last 40 ticks.")
                .subheading("Mode and power")
                .paragraph("A bus on ON DEMAND checks its cable's block for a redstone signal. A bus on CONTINUOUS does"
                        + " not check. A bus set to OFF does not move, whatever its mode. MODE and POWER are on every"
                        + " Import and Export Bus, and not on the External Storage Bus"
                        + " ([](jsc:external_storage_bus)).")
                .subheading("Per-item keep and max")
                .paragraph("A Transition bus has no keep or max for the whole bus. Each item in its filter has its own"
                        + " keep and max, shown as a row in the window. An item with a keep or a max of zero uses the"
                        + " bus-wide value, and there is none on a Transition bus, so zero means no keep and no cap.")
                .ifSomethingGoesWrong(
                        "The bus does not move at all.",
                        "Check POWER: a bus set to OFF stops. Then check MODE: ON DEMAND needs a redstone signal on"
                                + " the cable's block.",
                        "A bus with a lower priority waits.",
                        "A bus with a higher priority on the same network wanted the same thing a moment ago. Raise"
                                + " the priority of this bus, or lower the other one.",
                        "The window has no PRIORITY row.",
                        "Only Standard and later buses have a priority. Vintage, Legacy and Transition buses do not.",
                        "The window shows a different number from the one you typed.",
                        "The window keeps KEEP and MAX between 0 and 1,000,000, and PRIORITY between -99 and 99.")
                .seeAlso("jsc:bus_conditions", "jsc:bus_filters_and_tags")
                .register();
        guide.page("bus_conditions", section).titled("Conditions")
                .paragraph("A condition is a rule that a bus checks before it moves. A Standard or later bus can"
                        + " have up to 4 conditions, and every one of them must hold for the bus to move. A bus whose"
                        + " condition does not hold waits, and its Activity tab says so.")
                .paragraph("The bus checks its conditions every 20 ticks, which is a second, and keeps the answer until"
                        + " the next check. You can also write conditions in IQL ([](jsc:iql)) with WHEN.")
                .table("The three kinds of condition", "Kind", "What the window says", "It holds when")
                .row(GuideCell.code("STOCK"),
                        GuideCell.words("only while the network holds under 64 minecraft:iron_ore"),
                        GuideCell.words("The network holds fewer than that number."))
                .row(GuideCell.code("HOURS"),
                        GuideCell.words("only between 18:00 and 06:00"),
                        GuideCell.words("The game's clock is between the two hours."))
                .row(GuideCell.code("AFTER"),
                        GuideCell.words("after the bus Coal in finishes"),
                        GuideCell.words("The named bus has made no move for 40 ticks."))
                .subheading("STOCK")
                .paragraph("STOCK counts what the whole network holds. The condition holds while that total is below"
                        + " the number you set. The number starts at 64 items and changes by 1 item; with Shift held,"
                        + " it changes by 64 items.")
                .define("Stock", "How many of an item the whole network holds, not the count in one chest.")
                .paragraph("Write a tag after a # sign, such as #c:ores, and STOCK counts every held item that carries"
                        + " the tag. In the window, click the item field with an item in your hand to write that"
                        + " item's id into it.")
                .subheading("HOURS")
                .paragraph("HOURS holds between two hours of the day. You set FROM and TO, and when TO comes before"
                        + " FROM, the window runs past midnight. The default is from 18:00 to 06:00, the night.")
                .paragraph("The hours follow the game's clock, not the real one. 06:00 is the start of each game day."
                        + " If FROM and TO are the same hour, the condition holds all day.")
                .paragraph("A bus holds only one hours window. Adding a second one is refused, and the action bar"
                        + " says \"A bus holds only one hours window. Remove it first to set another.\" From IQL, a new"
                        + " hours window replaces the old one.")
                .subheading("AFTER")
                .paragraph("AFTER waits for another bus on the same network. It holds once the named bus has made no"
                        + " move for 40 ticks, which is 2 seconds. Finished here means no move for that long, not that"
                        + " the job is done.")
                .paragraph("If no bus has that name, nothing holds this bus back, so the condition holds. Type the"
                        + " name in the window, up to 32 characters. The name is not case-sensitive.")
                .subheading("Adding and removing")
                .steps("Open the bus's window and go to CONFIGURE.",
                        "Click + condition, and choose STOCK, HOURS or AFTER.",
                        "Fill in the editor, with the - and + buttons for the numbers.",
                        "To take a condition off, click the right end of its row.")
                .ifSomethingGoesWrong(
                        "The bus waits and does not move.",
                        "Open its Activity tab. A line that says \"a condition holds it\" means a condition is not"
                                + " met, so the bus waits. Read the sentence of each condition in CONFIGURE"
                                + " ([](jsc:bus_activity)).",
                        "The window refuses a STOCK condition.",
                        "A count below 1, an unknown item id or a bad tag is refused. The window shows nothing when it"
                                + " refuses.",
                        "The bus does not take a fifth condition.",
                        "A bus holds up to 4 conditions. Take one off first, then add the new one.",
                        "The HOURS condition holds all day.",
                        "FROM and TO are the same hour. Set them to two different hours.")
                .seeAlso("jsc:bus_activity")
                .register();
    }

    private static void busWindows(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("bus_windows").titled("A bus at work")
                .icon(ComputingModule.IMPORT_BUS_ITEM).in(chapter).register();
        guide.page("bus_window", section).titled("A bus's window").icon(ComputingModule.IMPORT_BUS_ITEM)
                .paragraph("Right-click a placed bus to open its window. The title in capitals at the top says which"
                        + " bus it is, such as IMPORT BUS or EXTERNAL STORAGE BUS. A lamp at the top right shows the"
                        + " state of its cable, and your own inventory is on the same screen.")
                .table("The tabs of the window", "Tab", "What it shows")
                .row(GuideCell.words("CONFIGURE"), GuideCell.words("The name of the bus and its settings, row by row."))
                .row(GuideCell.words("ACTIVITY"),
                        GuideCell.words("The moves and holds of the bus, explained on the page What a bus did."))
                .row(GuideCell.words("SOFTWARE"),
                        GuideCell.words("The settings written as IQL and SIGMA lines, and which program set what."))
                .subheading("The lamp")
                .paragraph("The lamp is green when the bus's cable reaches a network. Hover it and it says LINKED."
                        + " It is red when the cable reaches no network, and then it says OFFLINE.")
                .subheading("CONFIGURE")
                .paragraph("The NAME field comes first. Type a name, up to 32 characters, and the change reaches the"
                        + " server at once. Software finds the bus by this name, so give each bus one"
                        + " ([](jsc:programming_buses)).")
                .paragraph("Below the name, the rows show the settings your bus's era has. Vintage shows the fewest,"
                        + " and Advanced shows the most. Each row has a label, its controls and sometimes a note.")
                .table("How you change a row", "Kind of row", "How you change it")
                .row(GuideCell.words("Filter cells"),
                        GuideCell.words("Click a cell with an item in your hand to list it. With an empty hand, click"
                                + " it to clear it."))
                .row(GuideCell.words("Option words, such as ONLY THESE or ON DEMAND"),
                        GuideCell.words("Click the option you want."))
                .row(GuideCell.words("Numbers: KEEP, MAX and PRIORITY"),
                        GuideCell.words("Click the - or + buttons: PRIORITY changes by 1, and KEEP and MAX change by 16"
                                + " when Shift is held."))
                .row(GuideCell.words("Conditions and tags"),
                        GuideCell.words("Click the + button to add one, and click a tag's chip or the right end of a"
                                + " condition's row to take it off."))
                .subheading("SOFTWARE")
                .paragraph("The SOFTWARE tab writes the bus's settings as IQL statements and as SIGMA calls, so you can"
                        + " copy them into a program. ADDRESS shows the name a program uses, such as bus://Ore in."
                        + " The section SET BY PROGRAMS lists which program set which setting, with lines such as"
                        + " \"Sigma: the keep\".")
                .define("Sigma", "A programming language of the mod. Its calls set buses the same way IQL statements"
                        + " do.")
                .paragraph("Hover a setting that a program set, and it says which program set it. Changing the setting"
                        + " in the window takes it back by hand.")
                .ifSomethingGoesWrong(
                        "A program cannot find the bus.",
                        "The bus has no name, or the program uses another one. Give the bus the name the program uses,"
                                + " in CONFIGURE. ADDRESS says \"Give the bus a name in CONFIGURE so software can find"
                                + " it.\" when the name is missing.",
                        "The lamp says OFFLINE.",
                        "The bus's cable is on no network. Join the cable to a network ([](jsc:first_network)).",
                        "A setting is not the one you set.",
                        "A program set it. Hover the setting to see which program, and change it by hand to take it"
                                + " back.")
                .seeAlso("jsc:bus_activity", "jsc:programming_buses")
                .register();
        guide.page("bus_activity", section).titled("What a bus did")
                .paragraph("The Activity tab is the log of one bus. Each line is a move the bus made, or a time it"
                        + " waited, with the reason. The bus keeps the latest 16 lines and shows 8 at a time, so you"
                        + " scroll to see the rest.")
                .paragraph("Each move is an Operation ([](jsc:what_operations_are)). The terminal and the Network"
                        + " Manager list the same log, so you can read these moves in the Network Manager"
                        + " ([](jsc:network_manager)).")
                .paragraph("A line shows the time, the item with its amount after an x, a status on the right, and the"
                        + " reason under the status.")
                .table("Status", "Status", "What it means")
                .row(GuideCell.code("COMPLETED"), GuideCell.words("The whole move went through."))
                .row(GuideCell.code("COMPLETED_PARTIAL"),
                        GuideCell.words("Part of the move went through. The rest stays in the bus and is sent again."))
                .row(GuideCell.code("WAITING"),
                        GuideCell.words("The bus held back, because the chest kept its KEEP amount or a condition did"
                                + " not hold."))
                .row(GuideCell.code("RESOURCE_LOCKED"),
                        GuideCell.words("No room: the network is full for an Import Bus, or the chest is full for an"
                                + " Export Bus."))
                .table("Reason", "Reason", "What it means")
                .row(GuideCell.words("into the network"),
                        GuideCell.words("An Import Bus moved items into the network."))
                .row(GuideCell.words("into the chest"), GuideCell.words("An Export Bus moved items into the chest."))
                .row(GuideCell.words("the chest keeps 16"),
                        GuideCell.words("The chest already holds its KEEP amount, so the Export Bus waits."))
                .row(GuideCell.words("the network is full"),
                        GuideCell.words("An Import Bus found no room in the network."))
                .row(GuideCell.words("the chest is full"), GuideCell.words("An Export Bus found the chest full."))
                .row(GuideCell.words("a condition holds it"),
                        GuideCell.words("A condition did not hold, so the bus waited."))
                .paragraph("A hold that repeats updates the time of the newest line, instead of adding a line. When the"
                        + " network takes only part of a move, the rest stays in the bus, and the bus sends it"
                        + " when there is room.")
                .ifSomethingGoesWrong(
                        "The log says \"Nothing moved yet.\"",
                        "The bus has not moved an item yet. Check what the bus needs before it moves"
                                + " ([](jsc:buses)), and what its filter lets through ([](jsc:bus_filters_and_tags)).",
                        "The status says RESOURCE_LOCKED.",
                        "The network or the chest has no room. Free space in the chest, or add storage to the network"
                                + " ([](jsc:storage)).",
                        "The reason says \"a condition holds it\".",
                        "A condition did not hold. Read the conditions of the bus in CONFIGURE"
                                + " ([](jsc:bus_conditions)).")
                .seeAlso("jsc:bus_window", "jsc:bus_conditions")
                .register();
    }

    private static void busProgramming(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("bus_programming").titled("Buses in programs")
                .icon(ComputingModule.EXPORT_BUS_ITEM).in(chapter).register();
        guide.page("programming_buses", section).titled("Programming buses").icon(ComputingModule.EXPORT_BUS_ITEM)
                .paragraph("Programs can set a bus, as well as the window can. A program finds a bus by its name, on"
                        + " the same network. Give the bus its name first, in its window ([](jsc:bus_window)).")
                .paragraph("Programs set a bus in IQL or in Sigma. IQL is the network's query language"
                        + " ([](jsc:iql)). Sigma is a programming language, and its sister language is Sigma#."
                        + " The Book of Sigma covers both ([](jsc:sigma_two_languages)).")
                .paragraph("A name matches regardless of capital letters, and the first bus with that name on the"
                        + " network is the one used. Put a name in quotes when it has spaces.")
                .paragraph("Vanilla items are written by their short name, such as iron_ore. Items from other mods"
                        + " are written in full, as namespace:path. Fluids are written with the word FLUID before"
                        + " them, as in FLUID water.")
                .subheading("IQL statements")
                .table("IQL statements for a bus", "Statement", "What it sets")
                .row(GuideCell.code("SET BUS 'Ore in' ON"), GuideCell.words("Turns the bus on."))
                .row(GuideCell.code("SET BUS 'Ore in' OFF"), GuideCell.words("Turns the bus off."))
                .row(GuideCell.code("SET BUS 'Ore in' MODE ON DEMAND"),
                        GuideCell.words("Moves only while there is a redstone signal."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER ONLY iron_ore, coal"),
                        GuideCell.words("Lists the items the bus moves."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER ALL BUT dirt"),
                        GuideCell.words("Moves everything except the listed items."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER NONE"), GuideCell.words("Clears the filter."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER TAG c:ores, c:logs"),
                        GuideCell.words("Lists tags, on an Advanced bus."))
                .row(GuideCell.code("SET BUS 'Ore in' MATCH FUZZY"),
                        GuideCell.words("Matches loosely, on an Advanced bus."))
                .row(GuideCell.code("SET BUS 'Ore in' KEEP 16 MAX 64"),
                        GuideCell.words("Sets KEEP and MAX for the whole bus."))
                .row(GuideCell.code("SET BUS 'Ore in' KEEP iron_ore 16 MAX iron_ore 64"),
                        GuideCell.words("Sets KEEP and MAX for one listed item, on a Transition bus."))
                .row(GuideCell.code("SET BUS 'Ore in' PRIORITY 5"),
                        GuideCell.words("Sets the priority, on a Standard or later bus."))
                .row(GuideCell.code("SET BUS 'Ore in' WHEN STOCK iron_ore < 512"),
                        GuideCell.words("Adds a STOCK condition."))
                .row(GuideCell.code("SET BUS 'Ore in' WHEN STOCK TAG c:ores < 4096"),
                        GuideCell.words("Adds a STOCK condition for a tag."))
                .row(GuideCell.code("SET BUS 'Ore in' WHEN TIME BETWEEN 18:00 AND 06:00"),
                        GuideCell.words("Sets the HOURS condition, and replaces the old one."))
                .row(GuideCell.code("SET BUS 'Ore in' AFTER BUS 'Coal in'"),
                        GuideCell.words("Adds an AFTER condition."))
                .row(GuideCell.code("SET BUS 'Chest wall' ACCESS READ ONLY"),
                        GuideCell.words("Sets what the network may do with an External Storage Bus."))
                .subheading("Sigma calls")
                .table("Sigma calls for a bus", "Call", "What it sets")
                .row(GuideCell.code("bus(\"Ore in\").Only(\"iron_ore, coal\");"),
                        GuideCell.words("Lists the items the bus moves."))
                .row(GuideCell.code("bus(\"Ore in\").AllBut(\"dirt\");"),
                        GuideCell.words("Moves everything except the listed items."))
                .row(GuideCell.code("bus(\"Ore in\").Tag(\"c:ores\");"), GuideCell.words("Lists a tag."))
                .row(GuideCell.code("bus(\"Ore in\").Fuzzy(true);"), GuideCell.words("Matches loosely."))
                .row(GuideCell.code("bus(\"Ore in\").Keep(16).Max(64);"),
                        GuideCell.words("Sets KEEP and MAX for the whole bus."))
                .row(GuideCell.code("bus(\"Ore in\").Item(\"iron_ore\").Keep(16);"),
                        GuideCell.words("Sets KEEP for one listed item."))
                .row(GuideCell.code("bus(\"Ore in\").Priority(5);"), GuideCell.words("Sets the priority."))
                .row(GuideCell.code("bus(\"Ore in\").WhenStock(\"iron_ore\", 512);"),
                        GuideCell.words("Adds a STOCK condition."))
                .row(GuideCell.code("bus(\"Ore in\").Between(18, 6);"),
                        GuideCell.words("Adds the HOURS condition, from 18:00 to 06:00."))
                .row(GuideCell.code("bus(\"Ore in\").After(bus(\"Coal in\"));"),
                        GuideCell.words("Adds an AFTER condition."))
                .row(GuideCell.code("bus(\"Ore in\").OnDemand();"),
                        GuideCell.words("Moves only with a redstone signal."))
                .paragraph("The SOFTWARE tab of a bus shows the IQL and SIGMA lines that would set it as it is now,"
                        + " so you can copy them ([](jsc:bus_window)). It writes an HOURS condition in job form, such"
                        + " as CREATE JOB bus_hours AS SET BUS 'Ore in' ON WHEN TIME BETWEEN 18:00 AND 06:00. The"
                        + " engine keeps that job as the bus's HOURS condition.")
                .subheading("When a statement is refused")
                .table("Messages from a bus statement", "Message", "What it means")
                .row(GuideCell.words("SET BUS needs the bus's name, in quotes when it has spaces"),
                        GuideCell.words("The statement has no bus name, or a name with spaces and no quotes."))
                .row(GuideCell.words("SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY,"
                        + " WHEN, AFTER BUS or ACCESS"),
                        GuideCell.words("The statement names no setting. Write one of the words listed."))
                .row(GuideCell.words("a bus has no setting, and then the word you typed"),
                        GuideCell.words("The word after the bus name is not a setting. Check its spelling."))
                .row(GuideCell.words("no bus named Ore in on this network"),
                        GuideCell.words("No bus has that name on the network the program runs on."))
                .row(GuideCell.words("bus Ore in set"), GuideCell.words("The statement worked, and the bus is set."))
                .row(GuideCell.words("the bus Ore in cannot be set so: its era or its kind has no such setting"),
                        GuideCell.words("The bus has no such setting, such as PRIORITY on a Legacy bus, or POWER on an"
                                + " External Storage Bus."))
                .row(GuideCell.words("no item or fluid named, and then the name you typed"),
                        GuideCell.words("The filter does not know that name, and items from other mods need their"
                                + " full name."))
                .row(GuideCell.words("Coal is not in the filter of the bus Ore in"),
                        GuideCell.words("A KEEP or MAX for one item needs that item in the filter first."))
                .row(GuideCell.words("25 is not an hour of the day"),
                        GuideCell.words("An hour for Between must be from 0 to 23."))
                .ifSomethingGoesWrong(
                        "A program's line says no bus has the name.",
                        "Check the bus's name in CONFIGURE, and check that the bus is on the network the program"
                                + " runs on.",
                        "The bus does not take a fifth condition.",
                        "A bus holds up to 4 conditions. Take one off first, then set the new one.",
                        "A setting does not change.",
                        "The bus's era does not have that setting. Check which settings each era has in Buses by era"
                                + " ([](jsc:buses_by_era)).",
                        "Setting an hour is refused.",
                        "Use hours from 0 to 23, such as 18 for six in the evening.")
                .seeAlso("jsc:iql", "jsc:what_iql_is", "jsc:buses_library")
                .register();
    }
}
