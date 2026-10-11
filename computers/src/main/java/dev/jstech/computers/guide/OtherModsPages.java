/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The pages of the other mods that add features to J's Computers: ComputerCraft through the Network Gateway, the
 * recipe viewers JEI and EMI, Mekanism and Jade. Each feature exists only while its mod is installed.
 */
final class OtherModsPages {

    private OtherModsPages() {
    }

    /** Declares the sections and entries of the other mods' pages, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        modIntegrations(guide, chapter);
        gatewayPages(guide, chapter);
    }

    private static void modIntegrations(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("mod_integrations").titled("Working with other mods")
                .in(chapter).register();
        otherModsIntro(guide, section);
        jeiAndEmi(guide, section);
        mekanism(guide, section);
        jade(guide, section);
    }

    private static void gatewayPages(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("gateway").titled("ComputerCraft and the Gateway")
                .in(chapter).register();
        networkGateway(guide, section);
        computercraftSide(guide, section);
        gatewayManager(guide, section);
        gatewayPermissions(guide, section);
        gatewayCommand(guide, section);
        computercraftPeripheral(guide, section);
    }

    private static void otherModsIntro(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("other_mods_intro", section).titled("Other mods")
                .paragraph("Some of the mods you play with can add to J's Computers. Five of them each add a"
                        + " feature when they are installed: ComputerCraft, JEI, EMI, Mekanism and Jade.")
                .paragraph("Each of them is optional. Without one of these mods, its feature is missing,"
                        + " and the rest of J's Computers works as before.")
                .paragraph("ComputerCraft computers run programs written in Lua. The Network Gateway is the bridge"
                        + " between those computers and yours ([](jsc:network_gateway)). The section after this one"
                        + " explains the Gateway in detail.")
                .define("Network Gateway", "A block that links one of your computers to ComputerCraft"
                        + " computers.")
                .define("ComputerCraft", "The mod CC: Tweaked, which adds programmable computers to Minecraft.")
                .define("Lua", "The programming language in which the programs of ComputerCraft computers are"
                        + " written.")
                .register();
    }

    private static void jeiAndEmi(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("jei_and_emi", section).titled("Recipe viewers: JEI and EMI")
                .paragraph("A recipe viewer is a screen that lists how things are made. JEI and EMI are two of"
                        + " them, and each one is optional. With JEI installed, you can send a recipe from the"
                        + " viewer to the Pattern Studio, which writes the patterns for autocrafting"
                        + " ([](jsc:pattern_studio)).")
                .paragraph("In JEI, each recipe you can send has a transfer button. Pressing it moves the recipe's"
                        + " ingredients into the Pattern Studio. The Studio has two tabs for this, and the recipe"
                        + " goes to the one it fits.")
                .subheading("Where the recipe lands")
                .table("Where a recipe lands", "Recipe", "Lands on", "What you see")
                .row(GuideCell.words("A crafting recipe"), GuideCell.words("The Bench tab"),
                        GuideCell.words("Its ingredients fill the grid in the viewer's order."))
                .row(GuideCell.words("Any other recipe"), GuideCell.words("The Machine tab"),
                        GuideCell.words("One data cell for each input and each output."))
                .paragraph("The Bench tab has a grid of nine cells, three by three. The ingredients fill it row by"
                        + " row, in the order the viewer shows them. Any ingredient beyond the ninth is left out.")
                .paragraph("The Machine tab takes the other recipes, such as the ones of J's Industrial machines."
                        + " Each input and output becomes a data cell there: an item with its count, or a fluid in"
                        + " millibuckets (mB).")
                .define("Recipe viewer", "A screen in a mod that lists recipes: which ingredients make which"
                        + " result.")
                .define("Transfer button", "The button of a recipe viewer that sends a recipe's ingredients to the"
                        + " Pattern Studio.")
                .define("Ingredient list", "The side panel of a recipe viewer that lists the items you can use as"
                        + " ingredients.")
                .define("Millibucket (mB)", "The unit of volume for fluids on the network: one thousandth of a"
                        + " bucket.")
                .subheading("Before a transfer works")
                .paragraph("The Pattern Studio must be the front window of its desktop, or the transfer"
                        + " button refuses and the viewer shows an error.")
                .paragraph("You can also drag an item from the ingredient list onto a cell of the Studio. The cell"
                        + " takes that item, as if you had clicked it with the item on your cursor.")
                .subheading("EMI and Industrial")
                .paragraph("EMI lists the same recipes and can show one of its categories. A recipe does not move"
                        + " from EMI into the Pattern Studio, so use JEI when you want a transfer.")
                .paragraph("When J's Industrial is installed, its macerating and compressing recipes also appear in"
                        + " JEI and EMI, each in its own category.")
                .ifSomethingGoesWrong(
                        "The transfer button shows \"Open the Pattern Studio to transfer recipes\".",
                        "Open the Studio on the desktop, bring it to the front, and press the button again.",
                        "The recipe lands on the wrong cells.",
                        "The Bench tab fills the grid in the viewer's order. Rearrange the cells in the Bench tab.",
                        "The Machine tab says \"The machine draft needs an input and an output.\"",
                        "A recipe with no items or fluids that could be mapped leaves the draft incomplete. Add the"
                                + " missing input or output in the Machine tab.",
                        "The Studio says \"Not a machine recipe: <name>\".",
                        "Only recipes that machines make fit the Machine tab. Choose a recipe that a machine makes.")
                .seeAlso("jsc:pattern_studio", "jsc:pattern_encoders")
                .register();
    }

    private static void mekanism(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("mekanism", section).titled("Mekanism")
                .paragraph("Mekanism is a mod with its own machines and its own chemicals: gases and other"
                        + " substances that its machines handle. When Mekanism is installed, J's Computers reads"
                        + " those chemicals and handles them as data, the way it handles fluids.")
                .paragraph("A chemical moves through the network and sits on disks, counted in millibuckets (mB),"
                        + " the same unit as a fluid. So you store a chemical the way you store a fluid, and the"
                        + " disk's usage line shows it as a number of mB of chemical.")
                .paragraph("The network names a chemical by its registry id, with the prefix chemical/. The oxygen"
                        + " of Mekanism, for example, is named chemical/mekanism:oxygen.")
                .table("Chemical names", "Name", "What it names")
                .row(GuideCell.code("chemical/mekanism:oxygen"), GuideCell.words("The oxygen chemical of Mekanism."))
                .row(GuideCell.code("chemical/<registry id>"),
                        GuideCell.words("Any other chemical, by its registry id, from the mod that adds it."))
                .define("Chemical (Mekanism)", "A gas or similar substance that Mekanism handles like a fluid.")
                .paragraph("Computers adds no Mekanism recipe and no Mekanism machine of its own. Autocrafting works"
                        + " with any machine that offers the usual item, fluid, energy and chemical access, from any"
                        + " mod ([](jsc:how_autocrafting_works)).")
                .paragraph("The buffer of a Network Gateway takes items only, so a chemical pulled through a Gateway"
                        + " moves nothing into its buffer ([](jsc:computercraft_peripheral)).")
                .ifSomethingGoesWrong(
                        "The network says \"unknown chemical:\" followed by a name.",
                        "The name is not a chemical the game knows. Check that Mekanism is installed, and that the"
                                + " name is chemical/ followed by the chemical's registry id.",
                        "A chemical does not move.",
                        "Check that Mekanism is installed. Without it, chemicals are kept by their id, but every"
                                + " query answers that the chemical is unknown.")
                .seeAlso("jsc:items_as_data", "jsc:how_autocrafting_works")
                .register();
    }

    private static void jade(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("jade", section).titled("Jade")
                .paragraph("Jade is a mod that tells you what you are looking at. Point at a block, and a small box"
                        + " shows its name and a few lines about it. Jade is optional.")
                .paragraph("When Jade is installed, the plugin of this series for it switches on. A machine that"
                        + " describes its state to Jade adds a line to the box, such as \"Working: 40%\" or"
                        + " \"Idle\", as the processing machines of J's Industrial do.")
                .paragraph("The blocks of J's Computers do not describe themselves to Jade. For them, the box"
                        + " shows the block's name and nothing more.")
                .ifSomethingGoesWrong(
                        "Jade shows only a name on a Computers block.",
                        "That is expected for now. Read the block's own page in this guide, which explains its"
                                + " window and its parts.")
                .register();
    }

    private static void networkGateway(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("network_gateway", section).titled("The Network Gateway").icon(ComputingModule.NETWORK_GATEWAY)
                .coversAll(ComputersGuide.blocksOf(NetworkGatewayBlock.class))
                .paragraph("Do you play with ComputerCraft too? The Network Gateway is a bridge between its computers"
                        + " and yours. You place it like any block, and it joins two sides: a ComputerCraft wired"
                        + " network on its front, and one of your computers on its back.")
                .paragraph("The Gateway is placed facing you. Its front joins the ComputerCraft side, and its back"
                        + " has the socket for a [Peripheral Cable](jsc:peripheral_cables). Every computer takes that"
                        + " cable, and a cable of a later era does not fit this socket.")
                .scene("A Network Gateway cabled to a Personal Computer, with a ComputerCraft computer against its"
                        + " front. The ComputerCraft computer shows only when ComputerCraft is installed. Drag the"
                        + " scene to see the back.", 120)
                .place(13, 0, 13, () -> ComputingModule.PERSONAL_COMPUTER.get(), "facing=south", "")
                .place(14, 0, 13, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}")
                .place(15, 0, 13, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,24,0,0,0,0,0]}")
                .place(15, 0, 14, () -> CoreCables.BLOCK.get(), "",
                        "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,2060,0,0,0,0,0]}")
                .place(15, 0, 15, () -> ComputingModule.NETWORK_GATEWAY.get(), "facing=south,lit=true",
                        "{LinkedOwner:3573412843520L,LinkLength:3}")
                .place(15, 0, 16, "computercraft:computer_normal", "facing=north", "")
                .view(-75, 25, 1.3F)
                .balloon(1, GuideBlock.ScenePart.of(15, 0, 15), "The Network Gateway",
                        "Its front joins ComputerCraft and its back takes the cable to your computer.")
                .balloon(2, GuideBlock.ScenePart.of(15, 0, 16), "A ComputerCraft computer")
                .balloon(3, GuideBlock.ScenePart.of(15, 0, 14), "The Peripheral Cable")
                .balloon(4, GuideBlock.ScenePart.of(13, 0, 13), "The host")
                .define("Wired network", "The cables and wired modems that ComputerCraft computers use to reach"
                        + " each other and the devices around them.")
                .subheading("Linking to a host")
                .paragraph("The computer a Gateway is linked to is its host. The two link in one of two ways, and the"
                        + " Gateway's screen says which one it found.")
                .table("How a Gateway links to its host", "Link", "What it means")
                .row(GuideCell.words("Peripheral Cable"),
                        GuideCell.words("A cable runs from the Gateway's back to the host, up to 16 blocks long."))
                .row(GuideCell.words("adjacent"),
                        GuideCell.words("The Gateway stands directly against the host, with no cable."))
                .paragraph("When the Gateway is linked, its screen shows its name, the host and the link, such as"
                        + " \"cable, 5 blocks\" or \"adjacent\". When it has no host, the screen says it is not"
                        + " linked and asks you to plug the cable in the back.")
                .define("Host", "The computer that a Network Gateway is linked to through its back socket.")
                .subheading("The buffer")
                .paragraph("A right-click opens the Gateway's buffer. It has nine item slots under the label \"ITEM"
                        + " BUFFER\", and the hint \"pull lands here, push leaves\" tells you what it is for.")
                .paragraph("Items that a ComputerCraft program pulls from the network arrive in the buffer, and items"
                        + " it pushes leave from there. The buffer's items drop on the ground when you break the"
                        + " block, so empty it first.")
                .paragraph("Everything else about the Gateway, such as its permissions and its other"
                        + " controls, is set on"
                        + " its host computer.")
                .subheading("Name and light")
                .paragraph("The first time it links, the Gateway takes the name gateway-1, then gateway-2, and so on."
                        + " You rename it in the Gateway Manager or with the gateway command"
                        + " ([](jsc:gateway_manager)).")
                .paragraph("A name is cleaned up as you type it: lower case, letters, digits, underscores and dashes,"
                        + " and any other run of characters becomes one dash. A name holds 24 characters at most, and"
                        + " an empty name goes back to the default.")
                .paragraph("The status light on the block is lit while the Gateway is linked. Identify makes it blink"
                        + " for 3 seconds, so you can find the block among others.")
                .ifSomethingGoesWrong(
                        "The Gateway does nothing.",
                        "It is not linked to a host, or the host's chunk is not loaded, or the host has disabled it."
                                + " Check the link, keep the host's chunk loaded, and enable the Gateway on the host.",
                        "The ComputerCraft side does nothing.",
                        "CC: Tweaked is not installed. Without it the block still links to its host and holds items;"
                                + " only the ComputerCraft side never comes up.",
                        "The screen says it is not linked and to plug the cable in the back.",
                        "Put a Peripheral Cable on the back socket and run it to the host, or stand the Gateway"
                                + " directly against the host.",
                        "Renaming is refused with \"another gateway on the wire is already called <name>\".",
                        "Choose a different name. Two gateways on one wire cannot share a name.")
                .seeAlso("jsc:peripheral_cables", "jsc:mainframes")
                .register();
    }

    private static void computercraftSide(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("computercraft_side", section).titled("The Gateway and ComputerCraft")
                .paragraph("On its front, the Gateway joins a ComputerCraft wired network. A ComputerCraft computer"
                        + " sees it as a peripheral of the type jsc_gateway. Its name is jsc_gateway_ followed by the"
                        + " Gateway's name, with each dash turned into an underscore: gateway-1 becomes"
                        + " jsc_gateway_gateway_1.")
                .paragraph("A ComputerCraft computer finds the Gateway only when the Gateway is on its wire. Put a"
                        + " networking cable or a wired modem against the Gateway's front. Without CC: Tweaked there"
                        + " is no ComputerCraft side at all.")
                .define("Budget", "The share of each game tick that the Gateway's calls take from the programs of its"
                        + " host, in percent.")
                .subheading("What the ComputerCraft side can do")
                .paragraph("A ComputerCraft program calls the Gateway's peripheral. The Gateway answers each call with"
                        + " the same service your own programs use, under the permissions you set on the host"
                        + " ([](jsc:gateway_permissions)). The cost of a call comes out of the host's budget.")
                .paragraph("The Gateway shows the state of this side in its screen and with the gateway command. It"
                        + " says \"reachable\" when a ComputerCraft computer is attached or on its wire, and"
                        + " \"nothing attached\" when none is.")
                .subheading("Events the Gateway sends")
                .paragraph("The Gateway sends events to the ComputerCraft computers, so a program can wait for them"
                        + " instead of asking again and again.")
                .table("Events the Gateway sends", "Event", "Sent when")
                .row(GuideCell.code("jsc_operation(id, status)"),
                        GuideCell.words("An operation the computer started settles. Only that computer hears it."))
                .row(GuideCell.code("jsc_stock(name, total, previous)"),
                        GuideCell.words("The total of a watched name moves. The Gateway checks each watch every 20"
                                + " ticks."))
                .row(GuideCell.code("jsc_test(name)"),
                        GuideCell.words("You press Test event in the Gateway Manager, or send the test from the"
                                + " gateway command."))
                .paragraph("The status of an operation is one of done, partial, failed, running, waiting, locked,"
                        + " pending, discarded or unknown. The operation types are pull, push, delete, move, craft,"
                        + " analyze, reindex, vacuum, drop, update and operation.")
                .subheading("Sending a line back")
                .paragraph("A ComputerCraft program can call send to say a line to the host. Only a program of J's"
                        + " Computers that is listening hears the line, and each line is written to the Gateway's"
                        + " log as said. With nobody listening, the line reaches no one.")
                .paragraph("The advancement Talking to the Neighbours is earned when a program hears a ComputerCraft"
                        + " computer through a Network Gateway.")
                .ifSomethingGoesWrong(
                        "A ComputerCraft program gets an error back.",
                        "Read its text: it is the Gateway's refusal, such as \"denied: operations are off\". Each"
                                + " refusal and its fix are in [](jsc:gateway_permissions).",
                        "The error is the message of a CC: Tweaked method itself.",
                        "The text is that method's own message. When the method gives none, the text is the"
                                + " method's name followed by failed.",
                        "The program gets the text \"there is no <name> on this Gateway's wire\".",
                        "The named peripheral is not on the Gateway's wire. Check its name, and connect it to the"
                                + " wire.",
                        "The program gets the text \"no such method <name> on <peripheral>\".",
                        "The method name is not one of the peripheral's methods. Check the spelling against"
                                + " [](jsc:computercraft_peripheral).",
                        "A ComputerCraft computer cannot see the Gateway.",
                        "The Gateway is not on its wire. Put a networking cable or a wired modem against its front.")
                .seeAlso("jsc:gateway_manager", "jsc:computercraft_peripheral")
                .register();
    }

    private static void gatewayManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gateway_manager", section).titled("The Gateway Manager")
                .paragraph("The Gateway Manager is a desktop program. It lists the Network Gateways on this computer's"
                        + " ports, and it is where you set what ComputerCraft may do through each one. It is a"
                        + " Legacy-era program ([](jsc:eras)).")
                .define("Ceiling", "The highest priority that any request from a ComputerCraft program may carry.")
                .define("Call cap", "The number of calls per game tick that the Gateway answers for a ComputerCraft"
                        + " program.")
                .paragraph("Select a Gateway in the list on the left, then use the tabs and the buttons on the right."
                        + " Until a Gateway is linked to one of this computer's ports, the list says none linked yet.")
                .table("The four tabs", "Tab", "What it shows or sets")
                .row(GuideCell.words("Status"),
                        GuideCell.words("The link, the Mainframe, the budget, the calls served and the latest"
                                + " log lines."))
                .row(GuideCell.words("Permissions"),
                        GuideCell.words("The four settings: reading, operations, the priority ceiling and the calls a"
                                + " tick."))
                .row(GuideCell.words("Computers"),
                        GuideCell.words("The ComputerCraft computers attached to the Gateway, with ID, LABEL, STATE,"
                                + " AGENT and LAST SEEN."))
                .row(GuideCell.words("Log"),
                        GuideCell.words("What the Gateway did, with WHEN, WHO and RESULT columns."))
                .paragraph("The Status tab has two cards. THIS SIDE says \"Linked to\" and the host's name, or \"Not"
                        + " linked\", and it says whether the Mainframe is online or offline. COMPUTERCRAFT SIDE says"
                        + " CC: Tweaked with its version, or that CC: Tweaked is not installed.")
                .paragraph("The budget line shows the share of each tick that the Gateway's calls take from the host's"
                        + " programs, such as \"Budget: 12% of a tick\".")
                .subheading("The buttons")
                .table("The buttons", "Button", "What it does")
                .row(GuideCell.words("Rename"),
                        GuideCell.words("Opens the RENAME GATEWAY dialog, with APPLY and CANCEL."))
                .row(GuideCell.words("Identify"), GuideCell.words("Blinks the Gateway's lamps for 3 seconds."))
                .row(GuideCell.words("Clear buffer to network"),
                        GuideCell.words("Sends the items in the buffer to the network."))
                .row(GuideCell.words("Open the log"), GuideCell.words("Opens the Gateway's log."))
                .paragraph("On the Computers tab, Turn on, Reboot, Shutdown and Test event act on the ComputerCraft"
                        + " computers attached to the Gateway.")
                .steps("Open the Gateway Manager on the computer that has the Gateway on its port.",
                        "Select the Gateway in the list on the left.",
                        "Open the Permissions tab, and turn on the switches you need.",
                        "Choose the priority ceiling and the calls a tick.",
                        "Open the Status tab to check the link and the Mainframe.")
                .ifSomethingGoesWrong(
                        "The message \"select a gateway first\" appears.",
                        "No Gateway is selected. Click one in the list on the left, then press the button again.",
                        "The message \"CC: Tweaked is not installed\" appears.",
                        "The ComputerCraft side is missing. Install CC: Tweaked to use it.",
                        "The message says an action needs the agent on the ComputerCraft computer.",
                        "The action needs a program on that ComputerCraft computer, and none is there. Try an action"
                                + " that does not need one.",
                        "The message \"no Mainframe on this network\" appears.",
                        "Operations need a running Mainframe. Put one on the network ([](jsc:mainframes)).",
                        "The message \"the buffer is empty\" appears after Clear buffer to network.",
                        "Nothing is in the buffer to send. Put items in it yourself, or have a ComputerCraft program"
                                + " pull them in.")
                .seeAlso("jsc:gateway_permissions", "jsc:gateway_command")
                .register();
    }

    private static void gatewayPermissions(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gateway_permissions", section).titled("What ComputerCraft may do")
                .paragraph("A ComputerCraft program can ask the Gateway for two kinds of things: reading what the"
                        + " network holds, and starting operations that move or make items. You decide which kinds"
                        + " it may ask for, with four settings on the host.")
                .table("The four settings", "Setting", "What it allows", "Default")
                .row(GuideCell.words("Read the network"),
                        GuideCell.words("The reading calls: capacity, used, types, total, find, servers, computers,"
                                + " operation, operations and watch."),
                        GuideCell.words("On"))
                .row(GuideCell.words("Operations"),
                        GuideCell.words("The calls pull, push, craft, cancel and run."),
                        GuideCell.words("On"))
                .row(GuideCell.words("Priority ceiling"),
                        GuideCell.words("The highest priority a request may carry. A higher request is lowered to"
                                + " it."),
                        GuideCell.words("MEDIUM"))
                .row(GuideCell.words("Calls a tick"),
                        GuideCell.words("How many calls the Gateway answers in one game tick, which is a twentieth"
                                + " of a second."),
                        GuideCell.words("8"))
                .paragraph("The switches do not cover unwatch, log, send, online, name or host. Unwatch and log still"
                        + " count against the call cap, and send does not.")
                .paragraph("A request names its priority with one word: low, medium or high. A request with no word,"
                        + " or with a word the Gateway does not know, runs at medium. A ceiling lower than that"
                        + " priority then lowers it. [Priorities](jsc:priorities) explain the order in which the"
                        + " network serves them.")
                .paragraph("A quantity of 0 or less means all: pull all of a name, or push everything in the buffer of"
                        + " that name. The Gateway Manager shows it as pull all. What a push cannot move goes back into"
                        + " the buffer.")
                .paragraph("The run call starts a compiled program on another computer of the network. You give it"
                        + " the computer's name, the program's name and any arguments, and it returns a process"
                        + " number. It takes no priority.")
                .subheading("Refusals")
                .paragraph("A refused request goes back to the program as text, and the Gateway's log writes it as"
                        + " DENIED.")
                .table("Refusals and what they mean", "Refusal", "What it means")
                .row(GuideCell.code("denied: reading the network is off"), GuideCell.words("The read switch is off."))
                .row(GuideCell.code("denied: operations are off"), GuideCell.words("The operations switch is off."))
                .row(GuideCell.code("busy: 8 calls a tick"),
                        GuideCell.words("The program called more often than the call cap in one tick. The number is"
                                + " the cap."))
                .row(GuideCell.code("which item?"), GuideCell.words("The request named no item."))
                .row(GuideCell.code("unknown item: <name>"),
                        GuideCell.words("The game knows no item by that name, and fluids and chemicals say unknown"
                                + " fluid or unknown chemical."))
                .row(GuideCell.code("the computer is not on a network"),
                        GuideCell.words("The host has no network to serve the request from."))
                .row(GuideCell.code("the network has no running Mainframe"),
                        GuideCell.words("A running Mainframe must be on the network."))
                .row(GuideCell.code("could not start the <type>"),
                        GuideCell.words("The network refused to start the operation, such as a SELECT or an INSERT."))
                .row(GuideCell.code("the buffer holds no <name>"),
                        GuideCell.words("A push asked for an item that the buffer does not hold."))
                .row(GuideCell.code("no pattern crafts <name>"),
                        GuideCell.words("No pattern makes that item, so no craft can start."))
                .row(GuideCell.code("more than one operation starts with <id>; give more of the id"),
                        GuideCell.words("The id you gave matches two operations."))
                .row(GuideCell.code("<name>: no such computer on this network"),
                        GuideCell.words("The computer you named is not on this network."))
                .row(GuideCell.code("<name> is powered off"),
                        GuideCell.words("The computer you asked to run a program is off."))
                .row(GuideCell.code("<name> does not take programs from other computers"),
                        GuideCell.words("The computer refuses programs that come from another computer."))
                .row(GuideCell.code("<name>: nothing installed runs a program of this kind"),
                        GuideCell.words("The computer has nothing that can run that program."))
                .row(GuideCell.code("<name>: <n> MB will not fit in <m> MB of free memory"),
                        GuideCell.words("The computer has too little free memory for the program."))
                .ifSomethingGoesWrong(
                        "The message \"denied: reading the network is off\" appears.",
                        "Turn the read switch on in the Permissions tab, or type gateway <name> set read on in the"
                                + " command line.",
                        "The message \"denied: operations are off\" appears.",
                        "Turn the operations switch on in the Permissions tab, or type gateway <name> set operations"
                                + " on in the command line.",
                        "The message \"busy: 8 calls a tick\" appears.",
                        "The program called more than the cap in one tick. Raise the cap to 16, or slow the program"
                                + " down.",
                        "A program asks for a priority and gets a lower one.",
                        "The priority ceiling is lower than the request, so the request is lowered to it. Raise the"
                                + " ceiling in the Permissions tab.")
                .seeAlso("jsc:gateway_manager", "jsc:states_and_queues")
                .register();
    }

    private static void gatewayCommand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("gateway_command", section).titled("The gateway command")
                .paragraph("The gateway command manages the Gateways from the command line of a computer. It works on"
                        + " any computer with peripheral ports, even one with no Gateway, so it can tell you what is"
                        + " missing.")
                .steps("Type gateway list to see the Gateways on this computer's ports.",
                        "Type the Gateway's name, then status, perms, computers or log, to see its details.")
                .table("The forms of the command", "You type", "What it does")
                .row(GuideCell.code("gateway list"),
                        GuideCell.words("Lists the Gateways on this computer's ports, with NAME, LINK, CC and WHERE."))
                .row(GuideCell.code("<name> status"),
                        GuideCell.words("Shows the link, the network, the budget, the buffer and the latest"
                                + " log lines."))
                .row(GuideCell.code("<name> perms"),
                        GuideCell.words("Shows the read and operations switches, the priority ceiling and the calls a"
                                + " tick."))
                .row(GuideCell.code("<name> computers"),
                        GuideCell.words("Lists the ComputerCraft computers attached, with ID, LABEL, STATE, AGENT and"
                                + " LAST SEEN."))
                .row(GuideCell.code("<name> log"),
                        GuideCell.words("Shows what the Gateway did, oldest first at the bottom."))
                .row(GuideCell.code("<name> rename <new>"),
                        GuideCell.words("Gives the Gateway a new name, under the same rules as the Gateway Manager."))
                .row(GuideCell.code("<name> identify"), GuideCell.words("Blinks the Gateway's lamps."))
                .row(GuideCell.code("<name> clear"), GuideCell.words("Sends the items in the buffer to the network."))
                .row(GuideCell.code("<name> test"),
                        GuideCell.words("Sends the jsc_test event to the computers that listen for it."))
                .row(GuideCell.code("<name> set read on|off"), GuideCell.words("Turns the read switch on or off."))
                .row(GuideCell.code("<name> set operations on|off"),
                        GuideCell.words("Turns the operations switch on or off."))
                .row(GuideCell.code("<name> set priority low|medium|high"),
                        GuideCell.words("Sets the priority ceiling."))
                .row(GuideCell.code("<name> set cap 4|8|16"), GuideCell.words("Sets the calls a tick."))
                .paragraph("Names are matched without regard to capitals, so Gateway-1 and gateway-1 name the same"
                        + " Gateway.")
                .ifSomethingGoesWrong(
                        "The command says no gateways on this computer's ports.",
                        "No Gateway is linked to a port of this computer. Plug the cable in the back of the Gateway"
                                + " ([](jsc:network_gateway)).",
                        "The command says CC: Tweaked is not installed.",
                        "Install CC: Tweaked to use the ComputerCraft side. The rest of the Gateway still works.",
                        "The command says no gateway named <name> on this computer.",
                        "Check the name with gateway list. Names are not case-sensitive, but the spelling must match.",
                        "The command says this terminal is not on a computer.",
                        "Run the command from the command line of a computer.")
                .register();
    }

    private static void computercraftPeripheral(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("computercraft_peripheral", section).titled("The jsc_gateway peripheral")
                .paragraph("A ComputerCraft program reaches the Gateway through its peripheral, which has the type"
                        + " jsc_gateway. The program wraps it with peripheral.find(\"jsc_gateway\") and calls its"
                        + " methods. Each call counts against the call cap ([](jsc:gateway_permissions)).")
                .paragraph("Names in the calls are the names the game uses. An item is an id such as iron_ingot, and"
                        + " the minecraft: namespace may be left out for vanilla items. A fluid is written fluid/ and"
                        + " its id, and a chemical chemical/ and its id.")
                .subheading("Reading methods")
                .table("Reading methods, which need the read switch", "Method", "What it does")
                .row(GuideCell.code("capacity()"), GuideCell.words("Returns a number: the capacity of the network."))
                .row(GuideCell.code("used()"), GuideCell.words("Returns a number: how much of the capacity is in use."))
                .row(GuideCell.code("types()"), GuideCell.words("Returns a table of each name and its total."))
                .row(GuideCell.code("total(name)"),
                        GuideCell.words("Returns a number: how much of that name the network holds."))
                .row(GuideCell.code("find(name)"),
                        GuideCell.words("Returns a list of entries, each with a server and the quantity it holds."))
                .row(GuideCell.code("servers()"),
                        GuideCell.words("Returns a list of servers, each with its name, used and capacity, and whether"
                                + " it is online."))
                .row(GuideCell.code("computers()"),
                        GuideCell.words("Returns a list of the computers on the network, each with its name, label,"
                                + " operating system, type and online state."))
                .row(GuideCell.code("operation(id)"),
                        GuideCell.words("Returns the table of one operation, or nil when there is none."))
                .row(GuideCell.code("operations()"),
                        GuideCell.words("Returns a list of the operations, the running ones and those settled in the"
                                + " past hour."))
                .row(GuideCell.code("watch(name)"),
                        GuideCell.words("Returns the total now, and watches the name, so that a jsc_stock event comes"
                                + " when it moves."))
                .row(GuideCell.code("unwatch(name)"),
                        GuideCell.words("Returns true or false: true when the watch ends."))
                .subheading("Operation methods")
                .table("Operation methods, which need the operations switch", "Method", "What it does")
                .row(GuideCell.code("pull(name, quantity, priority)"),
                        GuideCell.words("Starts a pull that brings items into the buffer, and returns the id of the"
                                + " operation."))
                .row(GuideCell.code("push(name, quantity, priority)"),
                        GuideCell.words("Starts a push of items from the buffer to the network, and returns the id."))
                .row(GuideCell.code("craft(name, quantity, priority)"),
                        GuideCell.words("Starts a craft of the item, and returns the id."))
                .row(GuideCell.code("cancel(id)"), GuideCell.words("Stops an operation, and returns true or false."))
                .row(GuideCell.code("run(computer, program, args...)"),
                        GuideCell.words("Starts a program on another computer, and returns its process number."))
                .paragraph("The priority argument is optional. The quantity 0 means all ([](jsc:gateway_permissions)).")
                .paragraph("An operation has a long id. You may give only its first letters, as long as no other"
                        + " operation starts with the same letters.")
                .subheading("Status and log methods")
                .table("Status and log methods", "Method", "What it does")
                .row(GuideCell.code("online()"), GuideCell.words("Returns true when the Gateway is linked to a host."))
                .row(GuideCell.code("name()"), GuideCell.words("Returns the name of the Gateway."))
                .row(GuideCell.code("host()"),
                        GuideCell.words("Returns the name of the host computer, or an empty text while not linked."))
                .row(GuideCell.code("send(text)"),
                        GuideCell.words("Says a line to the host, and returns true or false."))
                .row(GuideCell.code("log(level, text)"),
                        GuideCell.words("Writes a line to the Gateway's log. The level is info, warn or error."))
                .paragraph("A line written to the log is cut at 96 characters, and the log keeps 40 entries.")
                .ifSomethingGoesWrong(
                        "A pulled fluid or chemical starts an operation, but nothing arrives in the buffer.",
                        "The buffer takes items only. Pull an item instead.",
                        "A push names a fluid or a chemical, and the text says \"the buffer holds no fluid/<id>\""
                                + " or \"the buffer holds no chemical/<id>\".",
                        "A push takes items from the buffer only. Push an item name.",
                        "The text says \"no such method <name> on <peripheral>\".",
                        "The method name is not one of those in the tables on this page. Check the spelling.",
                        "The text says \"more than one operation starts with\" the letters you gave.",
                        "Give more of the id, until only one operation starts with them.")
                .seeAlso("jsc:operations_catalogue")
                .register();
    }
}
