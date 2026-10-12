/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;
import net.minecraft.world.level.block.Blocks;

/**
 * Declares the autocrafting pages of the Guide to Operations: the flow of a craft, the cards and the patterns,
 * the programs that write and manage them, the machine line that runs a machine, and what to do when a craft fails.
 */
final class AutocraftingPages {

    /*
     * The faces of a cable block as the game numbers them, which is how a part names the face it stands on.
     */
    private static final int NORTH_FACE = 2;
    private static final int SOUTH_FACE = 3;
    private static final int WEST_FACE = 4;
    private static final int EAST_FACE = 5;

    /*
     * What a crafting cable crosses, one bit per face it leaves by (down 1, up 2, north 4, south 8, west 16, east 32).
     * A cable that meets a device also carries that face shifted up eight bits, which makes its plug: 4096 is the
     * west one. A face that holds a part is left out, since a part closes its face to wires.
     */
    private static final int NORTH_ONLY = 4;
    private static final int SOUTH_ONLY = 8;
    private static final int WEST_ONLY = 16;
    private static final int NORTH_AND_EAST = 36;
    private static final int SOUTH_AND_EAST = 40;
    private static final int WEST_AND_EAST = 48;
    private static final int WEST_NORTH_AND_SOUTH = 28;
    private static final int PLUGGED_WEST_AND_EAST = 16 + 4096 + 32;

    /*
     * A standard Crafting Computer with the parts a valid build needs. Slot 0 is the board, 1 the processor, 2 to 5 the
     * memory, 6 to 9 the expansion cards, 10 the power supply and 11 and 12 the disks.
     */
    private static final String COMPUTER_DATA = "{Hardware:{Size:15,Items:["
            + "{Slot:0,id:\"jsc:motherboard_atx_standard_1155\",count:1},"
            + "{Slot:1,id:\"jsc:cpu_integra_celer_g530\",count:1},"
            + "{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},"
            + "{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},"
            + "{Slot:6,id:\"jsc:crafting_card_pcie3\",count:1},"
            + "{Slot:10,id:\"jsc:psu_850g\",count:1},"
            + "{Slot:11,id:\"jsc:disk_ssd_500g\",count:1}]}}";

    /* A standard encoder with a writable DVD in its bay and its tray held out. */
    private static final String ENCODER_DATA =
            "{Media:{Size:1,Items:[{Slot:0,id:\"jsc:dvd_rw\",count:1}]},TrayOpen:1b}";

    private AutocraftingPages() {
    }

    /** Declares the section and the entries of autocrafting, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("autocrafting").titled("Autocrafting")
                .icon(ComputingModule.CRAFTING_CARD_PCIE3).in(chapter).register();
        howAutocraftingWorks(guide, section);
        craftingCards(guide, section);
        patterns(guide, section);
        patternStudio(guide, section);
        patternEncoders(guide, section);
        craftingInterfaces(guide, section);
        inputRoutersAndReceivingBuses(guide, section);
        craftingManager(guide, section);
        craftPlanner(guide, section);
        askingForACraft(guide, section);
        workshop(guide, section);
        whenACraftGoesWrong(guide, section);
    }

    /**
     * The block entity data of a cable block holding one crafting wire: its links go in the crafting lane, which is the
     * sixth of the nine, and a part on a face is written beside them. A scene runs no server, so a cable never works
     * out its own links and must be told them.
     */
    private static String wire(final int links, final String parts) {
        return "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0," + links + ",0,0,0]"
                + (parts.isEmpty() ? "" : ",Parts:[" + parts + "]") + "}";
    }

    private static String part(final int face, final String type) {
        return "{Face:" + face + "b,Type:\"" + type + "\",Data:{}}";
    }

    private static void howAutocraftingWorks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("how_autocrafting_works", section).titled("How autocrafting works")
                .icon(() -> ComputingModule.CRAFTING_CABLE)
                .covers(() -> ComputingModule.CRAFTING_CABLE)
                .paragraph("You ask for 512 pistons. The network works out every step from what it has: how many"
                        + " planks, how much cobblestone, iron and redstone it needs, and what it must make first."
                        + " It also decides on which machines and in which order. Then it runs the crafting table"
                        + " recipes and the machines, and puts the pistons in storage. That is autocrafting.")
                .paragraph("To do it, the network must know the recipes. A recipe it knows is a pattern, and there"
                        + " are three kinds: a bench pattern, a 3x3 grid like a crafting table's with its result; a"
                        + " machine pattern, what goes into a machine and what comes back; and a multi-stage"
                        + " pattern, steps of both kinds in order.")
                .define("Autocrafting", "The network making what it lacks, by running the recipes it knows on what"
                        + " it holds.")
                .define("Pattern", "A recipe the network knows: a bench grid, a machine's inputs and outputs, or"
                        + " several of those in order.")
                .paragraph("The parts are a Crafting Computer that runs the crafts ([](jsc:crafting_computers)), the"
                        + " Crafting Cable that joins it to the machines, and on that cable the parts that feed each"
                        + " machine. A Crafting Card in the computer is what lets it craft at all"
                        + " ([](jsc:crafting_cards)). Two programs you install yourself write the patterns and load"
                        + " them: the Pattern Studio and the Crafting Manager ([](jsc:pattern_studio),"
                        + " [](jsc:crafting_manager)).")
                .scene("The whole line: a Crafting Computer, the Crafting Cable, and a furnace with the parts that feed"
                        + " it and take back what it makes. Drag it to see the back.", 130)
                .place(0, 0, 1, () -> ComputingModule.CRAFTING_COMPUTER.get(), "facing=south", COMPUTER_DATA)
                .place(1, 0, 1, () -> CoreCables.BLOCK.get(), "", wire(PLUGGED_WEST_AND_EAST, ""))
                .place(2, 0, 1, () -> CoreCables.BLOCK.get(), "", wire(WEST_AND_EAST, ""))
                .place(3, 0, 1, () -> CoreCables.BLOCK.get(), "",
                        wire(WEST_NORTH_AND_SOUTH, part(EAST_FACE, "jsc:crafting_interface")))
                .place(3, 0, 0, () -> CoreCables.BLOCK.get(), "", wire(SOUTH_AND_EAST, ""))
                .place(3, 0, 2, () -> CoreCables.BLOCK.get(), "", wire(NORTH_AND_EAST, ""))
                .place(4, 0, 0, () -> CoreCables.BLOCK.get(), "",
                        wire(WEST_ONLY, part(SOUTH_FACE, "jsc:crafting_input_router")))
                .place(4, 0, 2, () -> CoreCables.BLOCK.get(), "",
                        wire(WEST_ONLY, part(NORTH_FACE, "jsc:receiving_bus")))
                .place(4, 0, 1, () -> Blocks.FURNACE, "facing=south", "")
                .view(-35, 25, 1.35F)
                .balloon(1, GuideBlock.ScenePart.of(0, 0, 1), "The Crafting Computer",
                        "It runs the crafts and holds the Crafting Card.")
                .balloon(2, GuideBlock.ScenePart.of(2, 0, 1), "The Crafting Cable")
                .balloon(3, GuideBlock.ScenePart.of(3, 0, 1).box(6, 1, 1, 16, 15, 15), "The Crafting Interface",
                        "It holds the machine's patterns and feeds the machine.")
                .balloon(4, GuideBlock.ScenePart.of(4, 0, 0).box(1, 1, 6, 15, 15, 16), "The Crafting Input Router",
                        "Put one against each input face of a machine that has several.")
                .balloon(5, GuideBlock.ScenePart.of(4, 0, 2).box(1, 1, 0, 15, 15, 10), "The Crafting Receiving Bus",
                        "Put it against the machine's output.")
                .balloon(6, GuideBlock.ScenePart.of(4, 0, 1), "The machine")
                .define("Crafting Cable","The cable that joins a Crafting Computer to the machines it feeds, and"
                        + " carries the crafting parts on it.")
                .define("Crafting Card", "A card that goes in an expansion slot of a Crafting Computer, and sets how"
                        + " many interfaces it drives and how many recipes it keeps.")
                .define("Crafting Interface", "A cable part that sits against a machine, holds the machine's"
                        + " patterns and feeds the machine ([](jsc:crafting_interfaces)).")
                .define("Crafting Receiving Bus", "A cable part that sits against a machine's output and takes"
                        + " back what the machine makes for a craft.")
                .paragraph("Patterns are written in the Pattern Studio and carried to the Crafting Computer on media"
                        + " ([](jsc:pattern_encoders)). A craft then goes through the steps below.")
                .table("The steps of a craft", "Step", "What happens")
                .row(GuideCell.words("The request"), GuideCell.words("The Mainframe starts a craft only while it is"
                        + " running and has an operating system."))
                .row(GuideCell.words("The plan"), GuideCell.words("The network works out the plan, and the craft"
                        + " waits until the plan is ready."))
                .row(GuideCell.words("The computer"), GuideCell.words("The plan goes to a Crafting Computer that is"
                        + " switched on and holds a Crafting Card."))
                .row(GuideCell.words("One at a time"), GuideCell.words("A Crafting Computer runs one craft at a time,"
                        + " unless the network has a Supercomputer."))
                .row(GuideCell.words("The steps"), GuideCell.words("Bench steps run on the Crafting Computer, from the"
                        + " recipes in its cards."))
                .row(GuideCell.words("The machines"), GuideCell.words("Machine steps run on the Crafting Interfaces"
                        + " that the computer drives."))
                .row(GuideCell.words("The output"), GuideCell.words("What a machine makes goes back to the network"
                        + " through the Crafting Receiving Bus tied to its interface."))
                .paragraph("The network needs two things before any of it works: a running Mainframe with an"
                        + " operating system, and the recipes held in a card or in an interface. A machine works"
                        + " with the network when it offers item, fluid and chemical access, whatever mod it comes"
                        + " from.")
                .ifSomethingGoesWrong("The plan says \"nothing on the network knows how to make\", and then the"
                        + " item's name.",
                        "No pattern for that item is on the network. Add one, and load it into a card or an"
                                + " interface ([](jsc:patterns)).")
                .seeAlso("jsc:crafting_computers", "jsc:crafting_interfaces", "jsc:pattern_encoders",
                        "jsc:asking_for_a_craft")
                .register();
    }

    private static void craftingCards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("crafting_cards", section).titled("Crafting Cards").icon(ComputingModule.CRAFTING_CARD_PCIE3)
                .coversAll(ComputersGuide.itemsOf(CraftingCardItem.class))
                .paragraph("The Crafting Card is the part that lets a Crafting Computer craft. You put it in an"
                        + " expansion slot of the computer. Without a card the computer does not craft at all"
                        + " ([](jsc:crafting_computers)).")
                .paragraph("A card sets how much its computer can do. It drives a number of Crafting Interfaces, and"
                        + " it keeps a number of bench recipes in its own memory. The recipes stay on the card, so"
                        + " they go with it to another computer.")
                .define("ROM", "The memory of a Crafting Card where its bench recipes are kept. It is part of the"
                        + " card, so the recipes move with it.")
                .table("What one card gives, by era", "Era", "Interfaces it drives", "Recipes its ROM keeps")
                .row(GuideCell.words("Vintage"), GuideCell.words("2"), GuideCell.words("2"))
                .row(GuideCell.words("Legacy"), GuideCell.words("4"), GuideCell.words("4"))
                .row(GuideCell.words("Transition"), GuideCell.words("5"), GuideCell.words("5"))
                .row(GuideCell.words("Standard"), GuideCell.words("6"), GuideCell.words("6"))
                .row(GuideCell.words("Advanced"), GuideCell.words("8"), GuideCell.words("8"))
                .paragraph("The two numbers are separate limits. Today they match in every era, but they measure"
                        + " different things: the interfaces a card drives, and the recipes it keeps.")
                .paragraph("A computer with several cards adds them up: the interfaces it drives and the recipes it"
                        + " keeps are the totals over all its cards. A computer that is off drives no interfaces.")
                .paragraph("A card also sets the speed. Each card adds its throughput, and the craft speed of the"
                        + " computer is its processor (CPU) capacity times the sum of those throughputs.")
                .paragraph("Each card also adds its threads. The threads of a computer are the sum of the threads of"
                        + " its cards: the number of stages it runs in parallel for one craft.")
                .define("stage", "One step of a pipeline: a bench pattern or a machine pattern.")
                .table("The Crafting Cards", "Card", "Era", "Throughput", "Threads")
                .row(GuideCell.words("Forge Logic Crafting Card ISA"), GuideCell.words("Vintage"),
                        GuideCell.words("0.02x CPU"), GuideCell.words("1"))
                .row(GuideCell.words("Forge Logic Crafting Card PCI"), GuideCell.words("Legacy"),
                        GuideCell.words("0.03x CPU"), GuideCell.words("2"))
                .row(GuideCell.words("Forge Logic Crafting Card"), GuideCell.words("Transition"),
                        GuideCell.words("0.05x CPU"), GuideCell.words("2"))
                .row(GuideCell.words("Forge Logic Crafting Card T3"), GuideCell.words("Transition"),
                        GuideCell.words("0.10x CPU"), GuideCell.words("4"))
                .row(GuideCell.words("Forge Logic Crafting Card PCI-e 3.0"), GuideCell.words("Standard"),
                        GuideCell.words("0.15x CPU"), GuideCell.words("6"))
                .row(GuideCell.words("Forge Logic Crafting Card T4"), GuideCell.words("Advanced"),
                        GuideCell.words("0.20x CPU"), GuideCell.words("8"))
                .paragraph("Hover over a card to read its tooltip. Each line of the tooltip tells you one thing.")
                .table("The tooltip of a card", "Line of the tooltip", "What it tells you")
                .row(GuideCell.words("Crafting accelerator (FPGA)"), GuideCell.words("The kind of card: a crafting"
                        + " accelerator, built on a field-programmable gate array (FPGA)."))
                .row(GuideCell.words("Threads  -  N threads"),
                        GuideCell.words("How many stages the card runs at once."))
                .row(GuideCell.words("Throughput  -  Nx CPU"), GuideCell.words("The card's speed as a multiple of the"
                        + " computer's processor (CPU) capacity."))
                .row(GuideCell.words("Drives N Crafting Interfaces"), GuideCell.words("How many interfaces the card"
                        + " drives."))
                .row(GuideCell.words("ROM  -  N of M bench recipes"), GuideCell.words("How many bench recipes the"
                        + " card keeps, out of how many it can keep."))
                .row(GuideCell.words("Recipe lines, then \"and N more\""), GuideCell.words("The names of up to 8 bench"
                        + " recipes, then how many are left out."))
                .paragraph("You load a bench recipe with the Load button of the Pattern Studio or in the Crafting"
                        + " Manager ([](jsc:crafting_manager)). It goes into the first card with room, counting from"
                        + " the first slot, and a card that already keeps the same recipe is not loaded twice.")
                .ifSomethingGoesWrong("The Crafting Computer crafts nothing.",
                        "It has no Crafting Card, or it is off. Give it a card and switch it on.",
                        "A bench recipe is not on the computer any more.",
                        "The card that kept it went to another computer with the recipe. Take the card back, or"
                                + " load the recipe again.",
                        "The screen says \"Only a Crafting Computer loads recipes\".",
                        "Load the recipe into a Crafting Computer.",
                        "The screen says \"A Crafting Card is required\".",
                        "Put a card in the Crafting Computer first.",
                        "The screen says \"Not loaded: a card keeps it already, or every card's ROM is full\".",
                        "A card keeps the recipe already, so nothing is needed. If every ROM is full, add a card, or"
                                + " remove a recipe in the Crafting Manager.")
                .seeAlso("jsc:crafting_computers", "jsc:crafting_manager", "jsc:crafting_interfaces")
                .register();
    }

    private static void patterns(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("patterns", section).titled("Patterns")
                .paragraph("A pattern is a recipe the network can use, and there are three kinds. Each kind fits a"
                        + " different job, and you write each one in its own tab of the Pattern Studio"
                        + " ([](jsc:pattern_studio)).")
                .subheading("Bench patterns")
                .paragraph("A bench pattern is a 3x3 grid of nine cells and a result. A cell that the recipe leaves"
                        + " empty stays empty. You can give the pattern a name of up to 64 characters and a note of up"
                        + " to 256 characters, or leave both out: an unnamed pattern goes by the name of its result.")
                .paragraph("A cell can accept an item tag, such as any kind of planks, instead of one exact item. When"
                        + " a craft is planned, the network uses whatever of that tag it holds in stock. The hint"
                        + " \"Right-click a cell: the tag it accepts\" tells you what a cell takes.")
                .paragraph("A bench pattern holds items only. You put fluids and chemicals into machine patterns.")
                .subheading("Machine patterns")
                .paragraph("A machine pattern says what goes into a machine and what comes out. You write its inputs"
                        + " and its outputs, and each one has an amount per run: an input of 1 means one of that item"
                        + " each time the machine runs.")
                .table("Parts of a machine pattern", "Part", "What it holds")
                .row(GuideCell.words("Input"), GuideCell.words("An item, a fluid or a chemical, with the amount it"
                        + " takes per run (at least 1)."))
                .row(GuideCell.words("Output"), GuideCell.words("An item, a fluid or a chemical, with the amount it"
                        + " gives per run and a chance in percent."))
                .row(GuideCell.words("Timeout"), GuideCell.words("How long the craft waits while nothing moves on the"
                        + " interface, in ticks (the default is 200, which is 10 seconds)."))
                .paragraph("The chance of an output is between 1 and 100 percent. An output of 100 always comes out."
                        + " A smaller chance makes a byproduct that may or may not come out. The network counts a"
                        + " byproduct at its expected amount when it plans the craft, but a byproduct never blocks a"
                        + " craft: the real amount is counted when the machine runs.")
                .paragraph("Anything else that comes out of the machine is an unexpected output. It goes to the"
                        + " network, and it never fails a craft.")
                .paragraph("An input can be marked as estimated when its amount came from a meter that measures per"
                        + " tick, and not from the author of the pattern. The Pattern Studio then shows \"per run"
                        + " (estimated)\" after the amount, and the recipe still runs.")
                .paragraph("A machine pattern works only from a Crafting Interface that holds it. The interface is"
                        + " described in [](jsc:crafting_interfaces).")
                .subheading("Multi-stage patterns")
                .paragraph("A multi-stage pattern is a pipeline: an ordered list of stages, and each stage is exactly"
                        + " one bench pattern or one machine pattern. Stage 1 takes its inputs from the network. Each"
                        + " later stage takes the output of the stage before it, and it starts only when that stage"
                        + " has made its result.")
                .define("pipeline", "A multi-stage pattern: an ordered list of stages that the network runs in turn.")
                .paragraph("Fluids work only inside machine stages. A pipeline cannot hold another pipeline, and it"
                        + " holds up to 16 stages.")
                .paragraph("The network works out how much each stage must make, starting from the last stage and"
                        + " going back. For example, a stage that makes one iron block needs nine ingots from the"
                        + " stage before it, and a stage that makes one ingot needs nine nuggets from the stage"
                        + " before it.")
                .ifSomethingGoesWrong("The pipeline has no stages.",
                        "Add a bench or a machine pattern as a stage.",
                        "The screen says \"A pipeline cannot hold another pipeline\".",
                        "Put the stages of the inner pipeline in directly, one bench or machine pattern each.",
                        "A pipeline with 16 stages refuses another.",
                        "Remove a stage before you add one: a pipeline holds 16 stages at most.")
                .seeAlso("jsc:pattern_studio", "jsc:pattern_encoders", "jsc:crafting_interfaces", "jsc:fluids",
                        "jsc:when_a_craft_goes_wrong")
                .register();
    }

    private static void patternStudio(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("pattern_studio", section).titled("The Pattern Studio")
                .paragraph("The Pattern Studio is the desktop program in which you write patterns. It keeps three"
                        + " drafts on the computer, one for each tab: \"Bench\", \"Machine\" and \"Multi-stage\". A"
                        + " draft is not a recipe until it is complete, so you can leave one half written and come"
                        + " back to it.")
                .paragraph("The Studio does not come with any system. Install it from its CD, or on a Linux"
                        + " desktop from the Mirror, on a system with windows: Frames XP or newer, or a Linux,"
                        + " FreeBSD or UNIX desktop ([](jsc:programs)).")
                .paragraph("Nothing is used up in the Studio. A click on a cell of the editor records a copy of what"
                        + " your cursor holds, and your inventory sits in a band under the editor.")
                .paragraph("The Load button works only on a Crafting Computer, because only that computer loads"
                        + " recipes ([](jsc:crafting_computers)).")
                .table("The tabs of the Studio", "Tab", "What it writes")
                .row(GuideCell.words("Bench"), GuideCell.words("A bench pattern: a 3x3 grid and its result."))
                .row(GuideCell.words("Machine"), GuideCell.words("A machine pattern: its inputs, its outputs and its"
                        + " timeout."))
                .row(GuideCell.words("Multi-stage"), GuideCell.words("A pipeline of stages, from your drafts or from"
                        + " files on a medium."))
                .paragraph("On the right, the rail lists the files on the drives of the computer and the linked"
                        + " Pattern Encoder, under the tabs \"Files\" and \"Encoder\". The hints of the rail read"
                        + " \"Click adds a stage\" and \"Click opens the file\".")
                .subheading("The buttons")
                .table("The buttons of the Studio", "Button", "What it does")
                .row(GuideCell.words("+ Bench"), GuideCell.words("Adds the bench draft as a stage of the"
                        + " multi-stage tab. The Studio answers \"Bench stage added\"."))
                .row(GuideCell.words("+ Machine"), GuideCell.words("Adds the machine draft as a stage. The Studio"
                        + " answers \"Machine stage added\"."))
                .row(GuideCell.words("Burn"), GuideCell.words("Writes the draft onto the medium of the linked Pattern"
                        + " Encoder ([](jsc:pattern_encoders))."))
                .row(GuideCell.words("Save to disk"), GuideCell.words("Writes the draft to the system disk of the"
                        + " computer. The Studio answers \"Saved\" and the file's name."))
                .row(GuideCell.words("Load"), GuideCell.words("Loads the draft into the ROM of this Crafting Computer,"
                        + " where bench recipes are kept ([](jsc:crafting_cards))."))
                .subheading("Writing a bench pattern")
                .steps("Open the Bench tab.",
                        "Hold an item on your cursor, and click a cell of the grid to record a copy of it.",
                        "Fill the grid until it makes a result. The preview shows what the network would use.",
                        "Type a name in the Name field.",
                        "Press Load, or Save to disk, to keep the draft.")
                .paragraph("The Name and Note fields name the pattern, up to 64 and 256 characters. The Timeout field"
                        + " sets how many ticks a machine pattern waits, and it is at least 1 tick.")
                .paragraph("The preview shows what the network would use for the draft. Its lines read \"Network"
                        + " would use:\" and \"In stock:\". A cell that takes a tag shows \"Any #\" and the tag.")
                .paragraph("Until you add a stage, the multi-stage tab reads \"No stages yet\". Each stage then shows"
                        + " its number, its kind in brackets, \"[bench]\" or \"[machine]\", and its name.")
                .paragraph("If Just Enough Items (JEI), the recipe viewer, is installed, the transfer button of"
                        + " a recipe sends it into the open Studio. A bench recipe goes to the bench draft, and any"
                        + " recipe with inputs and outputs goes to the machine draft.")
                .paragraph("The Studio answers \"Recipe placed on the bench\" or \"Recipe placed in the machine"
                        + " draft\". If the Studio is not open, the message is \"Open the Pattern Studio to transfer"
                        + " recipes\".")
                .ifSomethingGoesWrong("The screen says \"The bench draft is not a recipe\".",
                        "The grid makes no result. Fill it so that one exists.",
                        "The screen says \"The machine draft needs an input and an output\".",
                        "Add at least one input and one output.",
                        "The Studio says \"Not a machine recipe\" or \"Not a pipeline\".",
                        "The file is of the wrong kind for this tab. Open a file of the kind the tab takes.")
                .seeAlso("jsc:patterns", "jsc:pattern_encoders", "jsc:crafting_manager", "jsc:crafting_cards")
                .register();
    }

    private static void patternEncoders(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("pattern_encoders", section).titled("The Pattern Encoder")
                .icon(ComputingModule.PATTERN_ENCODER)
                .coversAll(ComputersGuide.blocksOf(PatternEncoderBlock.class))
                .paragraph("A pattern on one computer is no use to another, so a pattern must travel. The Pattern"
                        + " Encoder is the device that burns patterns onto media, such as floppy disks, discs and USB"
                        + " sticks, and you carry the medium from one computer to another. The encoder joins to a"
                        + " computer with a Peripheral Cable ([](jsc:peripheral_cables)).")
                .steps("Write the pattern in the Pattern Studio ([](jsc:pattern_studio)).",
                        "Put a blank medium in a Pattern Encoder that is joined to the computer, and burn the pattern"
                                + " onto it.",
                        "On the Crafting Computer, open the Crafting Manager and load the pattern from the medium"
                                + " ([](jsc:crafting_manager)).")
                .paragraph("A bench pattern goes into a card, and a machine pattern goes into the interface of its"
                        + " machine. The plain Pattern Encoder is the Standard one, and each era has its own encoder.")
                .table("Media each encoder writes", "Encoder era", "Media it writes")
                .row(GuideCell.words("Vintage"), GuideCell.words("Floppy Disk"))
                .row(GuideCell.words("Legacy"), GuideCell.words("CD-RW"))
                .row(GuideCell.words("Transition"), GuideCell.words("DVD-RW and CD-RW"))
                .row(GuideCell.words("Standard"), GuideCell.words("DVD-RW, CD-RW and USB Flash Drive"))
                .row(GuideCell.words("Advanced"), GuideCell.words("BD-RE and USB Flash Drive"))
                .table("The media of patterns", "Medium", "Era", "Can the encoder write it?")
                .row(GuideCell.words("Floppy Disk"), GuideCell.words("Vintage"), GuideCell.words("yes"))
                .row(GuideCell.words("CD-RW"), GuideCell.words("Legacy"), GuideCell.words("yes"))
                .row(GuideCell.words("CD-ROM"), GuideCell.words("Legacy"), GuideCell.words("no"))
                .row(GuideCell.words("DVD-RW"), GuideCell.words("Transition"), GuideCell.words("yes"))
                .row(GuideCell.words("DVD-ROM"), GuideCell.words("Transition"), GuideCell.words("no"))
                .row(GuideCell.words("USB Flash Drive"), GuideCell.words("Standard"), GuideCell.words("yes"))
                .row(GuideCell.words("BD-RE"), GuideCell.words("Advanced"), GuideCell.words("yes"))
                .row(GuideCell.words("BD-ROM"), GuideCell.words("Advanced"), GuideCell.words("no"))
                .paragraph("A read-only medium, marked no, is refused, because a pattern needs a medium the encoder"
                        + " can write.")
                .paragraph("From the Legacy on, the encoder burns discs, and the discs lie on a tray. Press the eject"
                        + " button to bring it out, put the disc on it, and press the button again, or"
                        + " push the tray in with an empty hand. Nothing is burned while the tray is out, and the tray"
                        + " stays shut while a pattern is written. A floppy disk or a USB stick does not use the tray:"
                        + " it goes into its slot whatever the tray does.")
                .scene("A Pattern Encoder with its tray out and a DVD-RW lying on it. Drag it to see the other"
                        + " sides.", 110)
                .place(1, 0, 1, () -> ComputingModule.PATTERN_ENCODER.get(), "facing=south", ENCODER_DATA)
                .view(-25, 20, 1.2F)
                .balloon(1, GuideBlock.ScenePart.of(1, 0, 1), "The Pattern Encoder")
                .balloon(2, GuideBlock.ScenePart.of(1, 0, 1).box(2, 11, 12, 12, 13, 16), "The disc tray",
                        "Press the eject button to bring it out or to take it in.")
                .balloon(3, GuideBlock.ScenePart.of(1, 0, 1).box(12, 9, 15, 14, 11, 16), "The eject button")
                .paragraph("A burn runs in three phases: seeking, writing and verifying. The display shows"
                        + " \"Seeking\", then \"Writing\" and the file's name, then \"Verifying\" and the name,"
                        + " and last \"Done:\" and the name.")
                .table("How fast a pattern is burned", "Medium", "Writing speed", "Seek", "Verify")
                .row(GuideCell.words("Floppy Disk"), GuideCell.words("250 bytes a tick"), GuideCell.words("20 ticks"),
                        GuideCell.words("10 ticks"))
                .row(GuideCell.words("CD"), GuideCell.words("1,024 bytes a tick"), GuideCell.words("30 ticks"),
                        GuideCell.words("10 ticks"))
                .row(GuideCell.words("DVD"), GuideCell.words("4,096 bytes a tick"), GuideCell.words("15 ticks"),
                        GuideCell.words("5 ticks"))
                .row(GuideCell.words("Blu-ray"), GuideCell.words("16,384 bytes a tick"), GuideCell.words("12 ticks"),
                        GuideCell.words("4 ticks"))
                .row(GuideCell.words("USB Flash Drive"), GuideCell.words("no limit"), GuideCell.words("4 ticks"),
                        GuideCell.words("2 ticks"))
                .paragraph("Seek is the time to find the place on the medium, and verify is the read-back check. A tick"
                        + " is a twentieth of a second, so 20 ticks make one second.")
                .paragraph("The queue holds up to 8 burns. A burn is refused when the queue is full. The file is named"
                        + " after the pattern, with .craft at its end, and its name is made unique on the medium. When"
                        + " the write ends, the encoder reads the medium back and compares it. A mismatch shows"
                        + " \"Verify failed\" and the file's name.")
                .paragraph("The bay is locked while a burn runs, so you cannot take the disc out until the burn has"
                        + " finished. The panel reads \"Not linked\" when no computer is joined, and \"Linked to\""
                        + " with the computer's name when one is.")
                .table("The panel of the encoder", "Line on the panel", "What it means")
                .row(GuideCell.words("Ready"), GuideCell.words("The encoder waits for a job."))
                .row(GuideCell.words("Insert media"), GuideCell.words("No medium to write is in the bay."))
                .row(GuideCell.words("Tray open"), GuideCell.words("The tray is out and holds no medium to write."))
                .row(GuideCell.words("Seeking"), GuideCell.words("The encoder is finding the place on the medium."))
                .row(GuideCell.words("Writing, then the file's name"), GuideCell.words("The pattern file is being"
                        + " written."))
                .row(GuideCell.words("Verifying, then the file's name"), GuideCell.words("The written file is being"
                        + " read back and compared."))
                .row(GuideCell.words("Done, then the file's name"), GuideCell.words("The file is on the medium."))
                .row(GuideCell.words("Cancelled"), GuideCell.words("The burn was stopped before it finished."))
                .row(GuideCell.words("Write failed, with the reason"), GuideCell.words("The write did not finish: the"
                        + " medium is full, or there is no medium."))
                .table("Messages of the encoder", "Message", "What it means")
                .row(GuideCell.words("The tray is closed - press the eject button to open it."),
                        GuideCell.words("The tray does not open or take a disc until you press the eject button."))
                .row(GuideCell.words("The bay already holds a disc - sneak-click to eject it."),
                        GuideCell.words("The bay holds one disc, so take it out first."))
                .row(GuideCell.words("The encoder is writing - wait for it to finish."),
                        GuideCell.words("A burn is running, so the bay and the eject button are locked."))
                .ifSomethingGoesWrong("The Pattern Studio sees no encoder.",
                        "It is not joined to that computer. Join it with a Peripheral Cable.",
                        "The disc is not read, and the burn does not start.",
                        "The disc lies on an open tray. Close the tray with the eject button, or push it in with an"
                                + " empty hand.",
                        "The encoder says \"A Vintage encoder writes floppy disks only.\"",
                        "Put in a medium that the table above lists for this encoder.",
                        "The encoder says \"A Legacy encoder writes CDs only.\"",
                        "Put in a medium that the table above lists for this encoder.",
                        "The encoder says \"A Transition encoder writes DVDs and CDs only.\"",
                        "Put in a medium that the table above lists for this encoder.",
                        "The encoder says \"This encoder writes DVDs, CDs and USB sticks, not that.\"",
                        "Put in a medium that the table above lists for this encoder.",
                        "The encoder says \"An Advanced encoder writes Blu-ray discs and USB sticks only.\"",
                        "Put in a medium that the table above lists for this encoder.",
                        "The encoder says \"That disc is read-only and cannot be written.\"",
                        "Use a disc that can be written, as the table above shows.")
                .seeAlso("jsc:peripheral_cables", "jsc:drives", "jsc:pattern_studio", "jsc:crafting_manager")
                .register();
    }

    private static void craftingInterfaces(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("crafting_interfaces", section).titled("Crafting Interfaces and the machine line")
                .icon(ComputingModule.CRAFTING_INTERFACE_ITEM)
                .coversAll(ComputersGuide.partsOf(ComputingParts::isCrafting))
                .paragraph("A machine line is how the network runs a machine. You build it on the Crafting Cable,"
                        + " and it has three parts: the Crafting Interface, the Crafting Input Routers and the"
                        + " Crafting Receiving Bus.")
                .define("Crafting Input Router", "A cable part that sits on an interface's cable against one input"
                        + " face of a machine, and carries the inputs that face takes.")
                .paragraph("The Crafting Interface sits on the cable against the machine. It holds that machine's"
                        + " patterns, and it feeds the machine directly when it sits against it. It can also feed the"
                        + " machine through a cable of its own, coloured apart from the main one.")
                .table("Pattern files one interface holds", "Era", "Pattern files it holds")
                .row(GuideCell.words("Vintage"), GuideCell.words("3"))
                .row(GuideCell.words("Legacy"), GuideCell.words("6"))
                .row(GuideCell.words("Transition"), GuideCell.words("8"))
                .row(GuideCell.words("Standard"), GuideCell.words("9"))
                .row(GuideCell.words("Advanced"), GuideCell.words("12"))
                .paragraph("For a machine that takes inputs on more than one face, a Crafting Input Router goes against"
                        + " each input face, on the interface's own cable ([](jsc:input_routers_and_receiving_buses))."
                        + " The Crafting Receiving Bus goes against the machine's output: it takes back what comes out,"
                        + " and it credits it to the craft that fed the machine.")
                .steps("Lay Crafting Cable from the Crafting Computer to the machine.",
                        "Put a Crafting Interface on the cable against the machine.",
                        "For a machine with several input faces, put a Crafting Input Router against each one.",
                        "Put a Crafting Receiving Bus against the machine's output.",
                        "Load the machine's pattern into the interface, from the Crafting Manager"
                                + " ([](jsc:crafting_manager)).")
                .paragraph("An interface works one recipe at a time, or several jobs at once. Its mode decides which.")
                .table("The modes of an interface", "Mode", "What it does")
                .row(GuideCell.words("EXCLUSIVE"), GuideCell.words("One recipe at a time on this machine, as its"
                        + " routers need; jobs of the same pattern run together."))
                .row(GuideCell.words("NOT EXCLUSIVE"), GuideCell.words("Several jobs at once, different recipes too,"
                        + " as far as the machine takes them."))
                .paragraph("MAX JOBS sets how many jobs the interface runs at once, from 0 to 64 jobs. Auto (0)"
                        + " lets the interface take as many jobs as its machine accepts. A paused interface takes no"
                        + " new job. You name the interface too, with a name of up to 32 characters.")
                .paragraph("An interface is driven when a Crafting Computer drives the cable it sits on. A card drives"
                        + " the interfaces of its cable in cable order.")
                .paragraph("Interfaces beyond what the cards can drive wait, driven by no computer, until another"
                        + " card goes in. The screen reads"
                        + " \"Driven by a Crafting Computer\" or \"Driven by no Crafting Computer\".")
                .table("The jobs log of an interface", "Line in the log", "What it means")
                .row(GuideCell.words("No jobs yet."), GuideCell.words("The interface has run no job."))
                .row(GuideCell.words("3 of 8 made"), GuideCell.words("How many pieces of a job's output the machine has"
                        + " made, out of how many it must make; the numbers shown are an example."))
                .row(GuideCell.words("the machine gave none of the expected outputs"), GuideCell.words("The job ended"
                        + " with none of the outputs its pattern lists."))
                .row(GuideCell.words("unexpected outputs, sent to the network"), GuideCell.words("The machine made"
                        + " outputs that its pattern does not list, and they went to the network."))
                .row(GuideCell.words("draining: the recipe changes"), GuideCell.words("The interface waits for the"
                        + " outputs still owed before a new recipe goes in."))
                .ifSomethingGoesWrong("A machine is never fed.",
                        "Put the interface on a cable that a Crafting Computer drives, switch the computer on, or give"
                                + " the computer another card.",
                        "The output never comes back.",
                        "Put a Crafting Receiving Bus against the machine's output.")
                .seeAlso("jsc:input_routers_and_receiving_buses", "jsc:crafting_manager", "jsc:patterns",
                        "jsc:data_cables", "jsc:crafting_cards")
                .register();
    }

    private static void inputRoutersAndReceivingBuses(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("input_routers_and_receiving_buses", section).titled("Input routers and Receiving Buses")
                .icon(ComputingModule.CRAFTING_ROUTER_ITEM)
                .paragraph("A Crafting Input Router sits on the cable that leaves a Crafting Interface, against one"
                        + " input face of a machine that has several. Its tooltip reads \"On the cable that leaves a"
                        + " Crafting Interface, against one input face of a machine with several\".")
                .paragraph("The interface sends each input of a pattern through the router chosen for it. A router"
                        + " moves nothing on its own: the interface feeds through it, and the interface measures and"
                        + " accounts for every item.")
                .paragraph("Each router has a filter, set the same way as the filters of the buses ([](jsc:buses)). The"
                        + " filter takes an item, or a tag, and the screen asks for \"item, or #tag\". A tag looks like"
                        + " \"c:ores\".")
                .paragraph("The default router for an input is the one whose filter takes it. The screen says"
                        + " \"Chosen in the interface\", so you can choose a different router in the interface.")
                .table("What the interface says about a router", "What the screen says", "What it means")
                .row(GuideCell.words("accepts here"), GuideCell.words("The router takes this input for the machine on"
                        + " this face."))
                .row(GuideCell.words("takes nothing here"), GuideCell.words("The router takes no input for the face."))
                .row(GuideCell.words("no machine on that face"), GuideCell.words("Nothing is in front of the router."))
                .row(GuideCell.words("is also taken by another router"), GuideCell.words("Two routers take the same"
                        + " input, and the screen names the one that the interface uses."))
                .row(GuideCell.words("Copper Ingot x3 for Bronze Ingot"), GuideCell.words("A route: three Copper Ingots"
                        + " for the Bronze Ingot pattern."))
                .paragraph("A Crafting Receiving Bus sits on the crafting cable against a machine's output. Its tooltip"
                        + " reads \"On the crafting cable, against a machine's output; takes back what its interface's"
                        + " jobs made\". It takes back what comes out of the machine and credits it to the craft that"
                        + " fed the machine.")
                .scene("An Input Router and a Receiving Bus on the crafting cable, each against a face of a"
                        + " furnace. Drag it to see them from behind.", 110)
                .place(1, 0, 1, () -> Blocks.FURNACE, "facing=south", "")
                .place(0, 0, 1, () -> CoreCables.BLOCK.get(), "",
                        wire(SOUTH_ONLY, part(EAST_FACE, "jsc:crafting_input_router")))
                .place(0, 0, 2, () -> CoreCables.BLOCK.get(), "", wire(NORTH_ONLY, ""))
                .place(2, 0, 1, () -> CoreCables.BLOCK.get(), "",
                        wire(SOUTH_ONLY, part(WEST_FACE, "jsc:receiving_bus")))
                .place(2, 0, 2, () -> CoreCables.BLOCK.get(), "", wire(NORTH_ONLY, ""))
                .view(-35, 25, 1.2F)
                .balloon(1, GuideBlock.ScenePart.of(0, 0, 1).box(6, 2, 2, 16, 14, 14), "The Crafting Input Router",
                        "It stands against one input face of the machine.")
                .balloon(2, GuideBlock.ScenePart.of(2, 0, 1).box(0, 2, 2, 10, 14, 14), "The Crafting Receiving Bus",
                        "It stands against the machine's output.")
                .balloon(3, GuideBlock.ScenePart.of(1, 0, 1), "The machine")
                .balloon(4, GuideBlock.ScenePart.of(0, 0, 2), "The Crafting Cable")
                .paragraph("The bus ties itself to the Crafting Interface that feeds its machine, either against it or"
                        + " through the routers of its own cable. You can also tie it by hand to one interface, or to"
                        + " several. Tying it by hand is how an output that goes into a chest beside the machine is"
                        + " handled, and by hand the bus takes up to 8 ties.")
                .table("The tie texts of a bus", "The text", "What it means")
                .row(GuideCell.words("by itself: the interface that feeds this machine"),
                        GuideCell.words("The bus found the interface on its own."))
                .row(GuideCell.words("by hand"), GuideCell.words("You tied the bus to the interface yourself."))
                .row(GuideCell.words("TIE TO INTERFACE"), GuideCell.words("The button that ties the bus to an"
                        + " interface by hand."))
                .paragraph("While a job of its interfaces runs, what comes out of the machine is credited to that job,"
                        + " up to what the job fed in. What was in the machine before the job began stays there. A"
                        + " fluid or a chemical that no pattern lists stays in the machine as its buffer.")
                .table("The statuses on a bus", "Status", "What it means")
                .row(GuideCell.words("CREDITED"), GuideCell.words("The output counts for the craft that fed the"
                        + " machine."))
                .row(GuideCell.words("UNEXPECTED"), GuideCell.words("The output is not on the pattern, so it goes to"
                        + " the network."))
                .row(GuideCell.words("LATE"), GuideCell.words("The job had already ended, so the output goes where"
                        + " the outputs of that job go."))
                .paragraph("The routers and the buses are cable parts, so they mount on the crafting cable. An"
                        + " interface's own cable is coloured apart from the main crafting cable, which keeps the two"
                        + " runs apart ([](jsc:data_cables)).")
                .ifSomethingGoesWrong("The interface says \"no router on its cable takes it: this pattern cannot "
                        + "run\".",
                        "Put a Crafting Input Router on the interface's cable, against the input's face, with a filter"
                                + " that takes that input.",
                        "The screen says \"Not on the cable of any interface: nothing uses it.\"",
                        "Put the router on the cable that leaves an interface.",
                        "The screen says that an input is routed here, but the filter does not take it.",
                        "Change the filter so that it takes that input, or pick another router.",
                        "The bus says \"on no network\".",
                        "Put the bus on the crafting cable of a network, next to an interface.",
                        "The bus says \"Not tied: no interface feeds this machine. Tie it to one by hand.\"",
                        "Tie it to one interface by hand, with the TIE TO INTERFACE button.")
                .seeAlso("jsc:crafting_interfaces", "jsc:buses", "jsc:data_cables", "jsc:when_a_craft_goes_wrong")
                .register();
    }

    private static void craftingManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("crafting_manager", section).titled("The Crafting Manager")
                .paragraph("The Crafting Manager is the program that loads recipes into the cards and the interfaces of"
                        + " a Crafting Computer, and that shows what its crafting parts are doing. It runs only on a"
                        + " Crafting Computer, and it does not come with any system. Install it from its CD, or on a"
                        + " Linux desktop from the Mirror, on a system with windows: Frames XP or newer, or a Linux,"
                        + " FreeBSD or UNIX desktop ([](jsc:programs)).")
                .paragraph("You need a Crafting Card to manage recipes, and only a Crafting Computer loads them"
                        + " ([](jsc:crafting_computers)).")
                .paragraph("The Crafting Manager has two tabs, \"Recipes\" and \"Interfaces\".")
                .subheading("The Recipes tab")
                .paragraph("The left pane lists the .craft files on a medium in a linked drive. With no medium in a"
                        + " linked drive, the pane reads \"Removable media: none\" and asks you to insert a disc into a"
                        + " linked drive. A medium with no pattern files reads \"No .craft files\".")
                .paragraph("The right pane lists the places where the computer keeps recipes: each card's ROM, and each"
                        + " interface with its machine recipes. Bench recipes go into a card's ROM, and machine and"
                        + " pipeline recipes go into an interface.")
                .table("The buttons of the Recipes tab", "Button", "What it does")
                .row(GuideCell.words("Load"), GuideCell.words("Puts the selected recipe into the place chosen in"
                        + " \"into\", or into the first place with room."))
                .row(GuideCell.words("Load all"), GuideCell.words("Loads every recipe on the medium."))
                .row(GuideCell.words("Download"), GuideCell.words("Writes the recipes back onto the medium."))
                .row(GuideCell.words("Remove"), GuideCell.words("Removes the selected recipe."))
                .row(GuideCell.words("Move to"), GuideCell.words("Moves the selected recipe to another place."))
                .subheading("Loading a recipe from a disc")
                .steps("Insert the disc with the .craft files into a linked drive.",
                        "Select the recipe in the left pane.",
                        "Choose the place in \"into\", or leave it on the first place with room.",
                        "Press Load.")
                .subheading("The Interfaces tab")
                .paragraph("The Interfaces tab has one row for each Crafting Interface on the cable. The summary line"
                        + " above the rows gives the interfaces that the cards drive, out of the interfaces on the"
                        + " cable, and the number of Crafting Cards.")
                .table("The columns of the Interfaces tab", "Column", "What it shows")
                .row(GuideCell.words("INTERFACE"), GuideCell.words("The name of the interface."))
                .row(GuideCell.words("MACHINE"), GuideCell.words("The machine in front of the interface, or \"Touches"
                        + " no machine\" when nothing is in front."))
                .row(GuideCell.words("PATTERNS"), GuideCell.words("The recipes it holds, as two numbers: the recipes"
                        + " held, and the capacity."))
                .row(GuideCell.words("MODE"), GuideCell.words("\"Exclusive\" or \"Not exclusive\", as its modes"
                        + " ([](jsc:crafting_interfaces))."))
                .row(GuideCell.words("STATE"), GuideCell.words("\"Idle\" or \"Paused\"."))
                .row(GuideCell.words("JOBS"), GuideCell.words("\"Auto\" when the limit is 0, otherwise the limit."))
                .paragraph("Click the mode to switch it, and click the jobs to raise them by one. The jobs go up to 16"
                        + " jobs, then wrap back to Auto, and a right-click lowers them. The interface's own screen"
                        + " allows up to 64 jobs. The Pause and Resume buttons pause and restart an interface.")
                .paragraph("Each card is listed with the interfaces it drives and how full its ROM is. A line reads"
                        + " \"N more on the crafting cable wait, driven by none, until another card goes in\" when some"
                        + " interfaces have no card to drive them.")
                .ifSomethingGoesWrong("The screen says \"A Crafting Card is required to manage recipes.\"",
                        "Put a Crafting Card in the Crafting Computer.",
                        "The screen says \"No Crafting Interface on this computer's crafting cable.\"",
                        "Put a Crafting Interface on the crafting cable of this computer.",
                        "The screen says \"No cards and no interfaces\".",
                        "Give the computer a Crafting Card, and put interfaces on its cable.",
                        "The screen says \"Removable media: none\".",
                        "Insert a disc into a linked drive.",
                        "The screen says \"No .craft files\".",
                        "The medium holds no pattern. Burn the pattern again, or insert the right medium.")
                .seeAlso("jsc:crafting_computers", "jsc:crafting_interfaces", "jsc:crafting_cards", "jsc:drives",
                        "jsc:pattern_encoders", "jsc:input_routers_and_receiving_buses")
                .register();
    }

    private static void craftPlanner(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("craft_planner", section).titled("The Craft Planner")
                .paragraph("The Craft Planner is a program that shows you a craft before you make it. You pick an item"
                        + " from the network's craft catalogue. It then shows the whole plan: each step, each"
                        + " ingredient and the number of stages.")
                .paragraph("The network works out the catalogue and the plan, so they show what the network holds"
                        + " now. The Craft button sends the same request as the terminal and the Network Interactor"
                        + " ([](jsc:asking_for_a_craft)).")
                .subheading("Planning a craft")
                .steps("Type part of the item's name in the search box.",
                        "Pick the item from the list.",
                        "Read the plan: the steps, the stages and the ingredients, with what the network has.",
                        "Press Craft to start the craft.")
                .table("The texts of the Craft Planner", "The text", "What it means")
                .row(GuideCell.words("search item"), GuideCell.words("The box where you type to find an item in the"
                        + " catalogue."))
                .row(GuideCell.words("no match"), GuideCell.words("The search finds no item of that name."))
                .row(GuideCell.words("Pick an item to plan."), GuideCell.words("Nothing is chosen yet, so there is no"
                        + " plan."))
                .row(GuideCell.words("planning"), GuideCell.words("The plan is being worked out."))
                .row(GuideCell.words("No pattern on the network makes this."), GuideCell.words("No recipe is known for"
                        + " the item, so there is no plan."))
                .row(GuideCell.words("Craftable"), GuideCell.words("The network can make the item from what it holds"
                        + " now."))
                .row(GuideCell.words("max, an amount, and a stage count"), GuideCell.words("The largest amount the"
                        + " network can make now, and the number of stages it takes."))
                .row(GuideCell.words("Steps and Tree"), GuideCell.words("Two views of the plan: the steps in order, and"
                        + " the craft tree."))
                .row(GuideCell.words("INGREDIENTS"), GuideCell.words("The raw ingredients, each with the amount needed"
                        + " and what the network has."))
                .row(GuideCell.words("have 12"), GuideCell.words("The network holds 12 of that ingredient."))
                .row(GuideCell.words("short 3, in red"), GuideCell.words("The network is 3 short of that ingredient."))
                .row(GuideCell.words("raw"), GuideCell.words("The ingredient is a raw material, with no recipe of its"
                        + " own."))
                .row(GuideCell.words("machine x3, bench x2"), GuideCell.words("How many times a step runs on a machine,"
                        + " and how many times it runs on a bench."))
                .row(GuideCell.words("Craft, then the item's name"),
                        GuideCell.words("The button that starts the craft."))
                .paragraph("The planner works from what the network holds now, so \"max\" is the most the network can"
                        + " make at this moment. It looks only so deep: a recipe nested more than 16 levels down is not"
                        + " planned. A recipe that depends on itself is cut, so the plan does not loop.")
                .ifSomethingGoesWrong("The screen says \"No pattern on the network makes this.\"",
                        "Add a pattern for the item, as a bench, machine or multi-stage pattern"
                                + " ([](jsc:patterns)).",
                        "An ingredient is shown as short, in red.",
                        "Stock more of that ingredient, and plan the craft again.")
                .seeAlso("jsc:asking_for_a_craft", "jsc:patterns", "jsc:when_a_craft_goes_wrong",
                        "jsc:crafting_manager")
                .register();
    }

    private static void askingForACraft(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("asking_for_a_craft", section).titled("Asking for a craft")
                .icon(ComputingModule.CRAFTING_COMPUTER)
                .paragraph("You ask for a craft the way you ask for anything else: in the Network Interactor, or in"
                        + " IQL, the network's query language, with CRAFT 512 piston ([](jsc:iql)).")
                .paragraph("The network plans the craft from the bottom up: what it has, what it must make first, in"
                        + " which order and on which machines. Then it runs the steps. Bench steps run on the Crafting"
                        + " Computer, as many stages at once as its threads allow. Machine steps run on the Crafting"
                        + " Interfaces, each on its own machine ([](jsc:crafting_computers)).")
                .paragraph("Before you ask, the Craft Planner shows you the same plan, with what it needs and what is"
                        + " missing ([](jsc:craft_planner)). When you open a craft from the Network Interactor's"
                        + " catalogue, the popup asks for the plan at once.")
                .subheading("In the Network Interactor")
                .table("The keys of the craft popup", "Key", "What it does")
                .row(GuideCell.code("Enter"), GuideCell.words("Submits the full craft."))
                .row(GuideCell.code("Backspace"), GuideCell.words("Divides the quantity by 10."))
                .row(GuideCell.code("Escape"), GuideCell.words("Closes the popup."))
                .row(GuideCell.code("Left"), GuideCell.words("Chooses another recipe, when the plan has more than "
                        + "one."))
                .row(GuideCell.code("Right"), GuideCell.words("Chooses another recipe, when the plan has more than "
                        + "one."))
                .paragraph("The quantity, the number of items to craft, starts at 1 item and can go up to 99,999"
                        + " items.")
                .table("The texts of the craft popup", "The text", "What it tells you")
                .row(GuideCell.words("Craft, then the item's name"), GuideCell.words("The craft button, in the list."))
                .row(GuideCell.words("Request, then a number of items"), GuideCell.words("The button that requests that"
                        + " many items from storage."))
                .row(GuideCell.words("RECIPE"), GuideCell.words("How many patterns make the item, when it has more"
                        + " than one."))
                .row(GuideCell.words("PLAN - with, then a recipe"), GuideCell.words("The plan, with the recipe you"
                        + " chose."))
                .row(GuideCell.words("PLAN - raw ingredients"), GuideCell.words("The plan, when the item has no "
                        + "recipe to"
                        + " choose."))
                .row(GuideCell.words("planning"), GuideCell.words("The plan is being worked out."))
                .row(GuideCell.words("EST (estimated time), then seconds"),
                        GuideCell.words("The estimated time of the craft, in "
                        + "seconds."))
                .row(GuideCell.words("EST followed by two hyphens"), GuideCell.words("There is no estimate yet."))
                .row(GuideCell.words("Prio, then a level"),
                        GuideCell.words("The craft's priority, changed with the priority button: LOW, MED-, MED,"
                        + " MED+ or HIGH, with MED as the default."))
                .paragraph("The computer remembers your choice of recipe for each item, so you choose once.")
                .subheading("In the terminal")
                .paragraph("The craft popup of the terminal has the buttons CRAFT and PARTIAL. CRAFT makes the whole"
                        + " craft. PARTIAL makes what the network can make now, and the popup shows that largest amount"
                        + " as \"max now\". CLOSE closes the popup.")
                .paragraph("The Craft tab of the terminal lists what is RUNNING and what is JUST MADE. It reads"
                        + " \"nothing being made\" when the network is idle. A pattern must be loaded before the "
                                + "terminal"
                        + " can craft: without one, the terminal reads \"no patterns loaded\" and asks you to load one"
                        + " from a medium under Patterns.")
                .table("The words of a finished craft", "Word", "What it means")
                .row(GuideCell.words("done"), GuideCell.words("The craft made all that it asked for."))
                .row(GuideCell.words("partial"), GuideCell.words("The craft made part of what it asked for."))
                .row(GuideCell.words("locked"), GuideCell.words("Another operation held what the craft needed, so the"
                        + " craft timed out waiting."))
                .row(GuideCell.words("failed"), GuideCell.words("The craft could not be made."))
                .subheading("Cancelling a craft")
                .paragraph("You cancel a craft in the Network Manager. Open the details of the craft there, and press"
                        + " Cancel. A craft cancelled while its plan is still being worked out is discarded"
                        + " ([](jsc:network_manager)).")
                .ifSomethingGoesWrong("A many-step craft fails at one stage.",
                        "That stage ran out of time: its machine has no power, is full, or never gave its output to the"
                                + " Receiving Bus. The craft names the stage ([](jsc:when_a_craft_goes_wrong)).",
                        "The craft shows \"locked\".",
                        "Another operation held what it needed. Ask again once that operation has settled.")
                .seeAlso("jsc:iql", "jsc:craft_planner", "jsc:crafting_manager", "jsc:network_manager",
                        "jsc:when_a_craft_goes_wrong", "jsc:priorities", "jsc:operation_states")
                .register();
    }

    private static void workshop(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("workshop", section).titled("The Workshop and the personal-use cards")
                .icon(ComputingModule.CRAFTING_TABLE_CARD)
                .paragraph("A Personal Computer can do more than run programs. Its Workshop program works with four"
                        + " personal-use cards. You put a card in the computer, and the program uses it for one kind"
                        + " of work: the Crafting Table Card, the Furnace Card, the Enchanting Card or the Anvil Card"
                        + " ([](jsc:expansion_cards)).")
                .paragraph("The Workshop has four tabs: Crafting, Furnace, Enchanting and Anvil. A tab whose card is"
                        + " not in the computer is dim, and it reads \"no card\".")
                .subheading("Using a card")
                .steps("Put the card in the Personal Computer.",
                        "Open the Workshop, and choose the tab of that card.")
                .table("The personal-use cards", "Card", "In the Workshop", "UPDATE action")
                .row(GuideCell.words("Crafting Table Card"), GuideCell.words("Crafts a 3x3 grid with the game's "
                        + "crafting"
                        + " recipes."), GuideCell.words("None: a grid filled from the network is what a craft is for."))
                .row(GuideCell.words("Furnace Card"), GuideCell.words("Smelts with the game's smelting recipes, and no"
                        + " fuel."), GuideCell.words("SMELT: smelts an item held on the network."))
                .row(GuideCell.words("Enchanting Card"), GuideCell.words("Gives the three offers of an enchanting"
                        + " table, counting up to 15 bookshelves around it."), GuideCell.words("ENCHANT: enchants an"
                        + " item."))
                .row(GuideCell.words("Anvil Card"), GuideCell.words("Repairs, combines and renames with the game's "
                        + "anvil"
                        + " logic."), GuideCell.words("REPAIR, COMBINE and NAME."))
                .subheading("How the cards work")
                .table("The Furnace Card by era", "Era", "Speed")
                .row(GuideCell.words("Vintage"), GuideCell.words("does not smelt"))
                .row(GuideCell.words("Legacy"), GuideCell.words("2"))
                .row(GuideCell.words("Transition"), GuideCell.words("3"))
                .row(GuideCell.words("Standard"), GuideCell.words("4"))
                .row(GuideCell.words("Advanced"), GuideCell.words("6"))
                .paragraph("The speed grows with the era of the computer. Every number here is a first estimate, and it"
                        + " may change.")
                .paragraph("The Enchanting Card asks 1, 1 or 2 levels for its three offers, and it asks no lapis. The"
                        + " Anvil Card asks two thirds of the levels an anvil would ask, rounded up. An offer shows its"
                        + " level and what it costs, and an offer that cannot be made reads \"no offer\".")
                .paragraph("Repair takes a material from storage. Each piece of material mends a quarter of the item's"
                        + " durability, and up to 4 pieces are used.")
                .subheading("UPDATE through the cards")
                .paragraph("An UPDATE changes an item that the network stores. It runs in three steps. First, the item"
                        + " leaves the storage servers for the card's drawer, as a SELECT takes it out. Then the card"
                        + " does its work. Last, the result goes back into the item's own server, and what has"
                        + " no room there stays at the card's output.")
                .paragraph("A smelt of more than one stack runs the three steps again, one stack at a time. A step that"
                        + " cannot be done sends the item back as it was.")
                .paragraph("An UPDATE belongs to the Personal Computer that asked for it. If that computer stops, or"
                        + " loses its card, the UPDATE is discarded, and what the card held is left at its output. The"
                        + " drawer does not survive the world being closed.")
                .ifSomethingGoesWrong("The screen says \"only a Personal Computer's own cards change items for the"
                        + " network\".",
                        "Ask from a Personal Computer that has the card.",
                        "The screen says \"this computer has no\", then the card's name.",
                        "Put that card in the Personal Computer.",
                        "The screen says \"somebody has to ask for it: the card is paid for with their experience\".",
                        "Ask through a player, who pays with their own experience.",
                        "The screen names the item, then says \"does not smelt\".",
                        "The Furnace Card does not smelt in a Vintage computer. Use a Personal Computer from the"
                                + " Legacy era on.",
                        "The screen names the item, then says \"takes no enchantment\".",
                        "Enchant an item that takes enchantments.",
                        "The screen names the item, then says \"is not worn\".",
                        "Repair only an item that has lost durability.",
                        "The screen says \"the network holds nothing that mends\", then the item's name.",
                        "Put a material that mends the item into storage on the network.",
                        "The screen says \"the card could not do it to\", then the item's name, then the reason:"
                                + " nothing to change, too expensive or too few levels.",
                        "Check the item, and give the paying player enough levels.",
                        "The screen says \"the network was not holding\", then the item's name.",
                        "Put the item into storage on the network, and ask again.",
                        "The screen says \"whoever asked for it was not here to pay\".",
                        "Ask again while the player who asked is in the world.",
                        "The grid makes nothing.",
                        "Fill the grid with a recipe that the game knows.",
                        "The screen says \"Not enough levels for that offer\".",
                        "Give the paying player more levels, or choose a cheaper offer.")
                .seeAlso("jsc:expansion_cards", "jsc:operations_catalogue", "jsc:personal_computers",
                        "jsc:when_a_craft_goes_wrong")
                .register();
    }

    private static void whenACraftGoesWrong(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("when_a_craft_goes_wrong", section).titled("When a craft goes wrong")
                .paragraph("A craft can fail before it starts, or while it runs. The network names the step that went"
                        + " wrong, and the messages below tell you what each one means and how to fix it.")
                .table("Before a craft starts", "What you see", "What it means", "The fix")
                .row(GuideCell.words("No craft starts."), GuideCell.words("A craft starts only while the Mainframe is"
                        + " running and has an operating system."), GuideCell.words("Switch the Mainframe on, and "
                                + "install"
                        + " its operating system."))
                .row(GuideCell.words("The text \"nothing on the network knows how to make\", then the item."),
                        GuideCell.words("No pattern on the network makes the item."), GuideCell.words("Add a bench,"
                        + " machine or multi-stage pattern for it ([](jsc:patterns))."))
                .row(GuideCell.words("The text \"no crafting computer was free to make\", then the item."),
                        GuideCell.words("No Crafting Computer is switched on with a Crafting Card, or all of them are"
                        + " busy."), GuideCell.words("Switch a Crafting Computer on, give it a Crafting Card, or wait"
                        + " until one is free."))
                .row(GuideCell.words("The recipe does not appear in the plan."), GuideCell.words("The recipe is in the"
                        + " ROM of a Crafting Computer that is not running."), GuideCell.words("Switch that Crafting"
                        + " Computer on."))
                .subheading("While a craft runs")
                .table("While a craft runs", "What you see", "What it means", "The fix")
                .row(GuideCell.words("The text \"the machine making\", then the item, and \"stopped taking"
                        + " anything\"."), GuideCell.words("A machine job gave up after its timeout, while nothing "
                                + "moved"
                        + " on its interface."), GuideCell.words("Check the machine's power and its output. A Crafting"
                        + " Receiving Bus must face the output."))
                .row(GuideCell.words("The text \"no Crafting Interface holds the recipe for\", then the item."),
                        GuideCell.words("No interface holds the machine's pattern, and it was not placed in time."),
                        GuideCell.words("Put the machine's pattern into a Crafting Interface that a Crafting Computer"
                        + " drives."))
                .row(GuideCell.words("The text \"the ingredients for\", then the item, and \"ran out\"."),
                        GuideCell.words("The bench craft ran out of ingredients in storage."),
                        GuideCell.words("Stock more of that ingredient."))
                .row(GuideCell.words("The text \"the pattern has no stages in it\"."), GuideCell.words("A multi-stage"
                        + " pattern holds no stages."), GuideCell.words("Add stages to the pipeline"
                        + " ([](jsc:patterns))."))
                .row(GuideCell.words("The text \"stage\", then a number, \"of\", a count, and \"could not be"
                        + " started\"."), GuideCell.words("The machine or the interface of that stage is not ready."),
                        GuideCell.words("Check that stage's machine and its interface, as for a machine that stopped."))
                .row(GuideCell.words("The text \"stage\", then a number, \"of\", a count, and \"failed\"."),
                        GuideCell.words("The stage timed out."), GuideCell.words("Check the stage's machine for "
                                + "power and"
                        + " room, and that its output faces a Crafting Receiving Bus."))
                .subheading("How a craft ends")
                .paragraph("A craft that makes part of what it asked for is partial, and the part is kept. A craft that"
                        + " makes nothing has failed. If one stage fails, the whole craft fails: bench stages give back"
                        + " the items they took, and machine stages keep what they fed into their machines.")
                .paragraph("The eight states of an operation are listed in [](jsc:operation_states).")
                .table("How a craft ends", "The result", "What it means", "The fix")
                .row(GuideCell.words("The text \"delivered\", with two amounts."), GuideCell.words("A partial "
                        + "craft: some"
                        + " of the amount was made."), GuideCell.words("Ask for the rest once the missing items are in"
                        + " storage."))
                .row(GuideCell.words("The text \"timed out waiting on a locked resource\"."), GuideCell.words("Another"
                        + " operation held what this craft needed."), GuideCell.words("Ask again once that other "
                                + "operation"
                        + " has settled."))
                .row(GuideCell.words("The text \"failed\"."), GuideCell.words("The craft made nothing."),
                        GuideCell.words("Read the step that failed above, and fix it first."))
                .subheading("Timeouts")
                .paragraph("A machine job waits for its outputs, and its clock counts only while nothing moves on its"
                        + " interface. When the timeout passes, the job gives up. A machine pattern's timeout is "
                                + "200 ticks"
                        + " by default, which is 10 seconds ([](jsc:patterns)).")
                .paragraph("Outputs that come late are still credited for a short while after a job ends. After that,"
                        + " they go to the network as unexpected.")
                .paragraph("An interface waits until its owed outputs are in before a new recipe goes in. Its log shows"
                        + " draining until then ([](jsc:crafting_interfaces)).")
                .subheading("The interfaces and the cables")
                .table("Interfaces and cables", "What you see", "What it means", "The fix")
                .row(GuideCell.words("The screen says \"Driven by no Crafting Computer\"."), GuideCell.words("The"
                        + " interface is not on a cable that a Crafting Computer drives, or the cards drive fewer"
                        + " interfaces than the cable reaches."), GuideCell.words("Put it on a cable that a Crafting"
                        + " Computer drives, switch the computer on, or give the computer another card."))
                .row(GuideCell.words("The screen says \"no router on its cable takes it: this pattern cannot run\"."),
                        GuideCell.words("No router on the interface's cable takes one of the inputs."),
                        GuideCell.words("Put a Crafting Input Router on that cable, with a filter that takes the input"
                        + " ([](jsc:input_routers_and_receiving_buses))."))
                .paragraph("The other messages of the routers and the buses are listed in"
                        + " [](jsc:input_routers_and_receiving_buses).")
                .subheading("Planner limits")
                .paragraph("The planner stops at 16 levels of recipes, and it cuts a recipe that depends on itself"
                        + " ([](jsc:craft_planner)).")
                .ifSomethingGoesWrong("A craft does not start at all.",
                        "Check that the Mainframe is running with an operating system, and that a Crafting Computer is"
                                + " switched on with a card.",
                        "A craft makes less than you asked.",
                        "The craft settled as partial. Ask for the rest once the missing items are in storage.")
                .seeAlso("jsc:asking_for_a_craft", "jsc:crafting_interfaces", "jsc:input_routers_and_receiving_buses",
                        "jsc:craft_planner", "jsc:operation_states", "jsc:network_manager")
                .register();
    }
}
