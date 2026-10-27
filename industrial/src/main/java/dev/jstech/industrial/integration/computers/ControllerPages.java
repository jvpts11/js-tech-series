/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.ModGuide;

import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_TAB;

/**
 * The drawings of what comes with J's Computers, in the mod's Control section: the Industrial Controller Computer,
 * its Fieldbus Cards and the industrial control cable, what the network reads of the plant and asks of it, the alerts
 * of the machines, and the Machine Monitor.
 */
final class ControllerPages {

    private ControllerPages() {
    }

    static void declare(final ModGuide guide, final ModGuide.SectionRef section) {
        controller(guide, section);
        cards(guide, section);
        cable(guide, section);
        reading(guide, section);
        alerts(guide, section);
        monitor(guide, section);
    }

    private static void controller(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("industrial_controller_computer", section).titled("Industrial Controller Computer")
                .icon(IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER)
                .coversAll(() -> IndustrialControllers.ALL.stream().map(BlockEntry::get).toList())
                .whatItIs("A computer built as a Personal Computer of its era is, whose Fieldbus Cards read the"
                        + " machines on its industrial control cables. There is one in each case J's Computers has,"
                        + " from the Vintage to the Advanced.")
                .whatItIsFor("Reading and commanding the machines of a plant from a computer on the data network.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Assemble it as a Personal Computer of its era: the same boards and parts, on the same"
                                + " screen.",
                        "Seat a Fieldbus Card in a slot of its board.",
                        "Lay Industrial Control Cable from it to the machines, no more than eight blocks of cable to"
                                + " each.",
                        "It links the machines its cables reach, the nearest first, as many as its cards read.")
                .whatCanGoWrong("A machine on its cable is not linked.",
                        "Its cards read no more machines. Seat another card, or give the machine a controller of its"
                                + " own.",
                        "A machine on two controllers' cables answers only one.",
                        "A machine is linked to one controller at a time, the first that reached it.")
                .seeAlso("jsindustrial:fieldbus_cards", "jsindustrial:industrial_control_cable",
                        "jsindustrial:reading_the_plant", "jsindustrial:machine_alerts")
                .register();
    }

    private static void cards(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("fieldbus_cards", section).titled("Fieldbus Cards").icon(FieldbusCards.PROFIBUS)
                .coversAll(() -> FieldbusCards.ALL.stream().map(ItemEntry::get).toList())
                .whatItIs("Expansion cards that let an Industrial Controller Computer read machines: one field bus"
                        + " of each era, each with the connector of its bus.")
                .whatItIsFor("Giving the controller its machines: it reads as many as its cards add up to.")
                .howToGetIt(FROM_THE_TAB)
                .subheading("The cards")
                .paragraph("RS-485, Vintage, on ISA: 2 machines. PROFIBUS, Legacy, on PCI: 4. PROFINET, Transition,"
                        + " on PCI: 6. EtherNet/IP, Standard, on PCIe: 8. EtherCAT, Advanced, on PCIe: 12.")
                .nextColumn()
                .howToUseIt("Seat the card in a free slot of an Industrial Controller Computer's board, a slot of"
                                + " the card's bus.",
                        "Lay Industrial Control Cable from the controller: at the card it ends in the card's"
                                + " connector.",
                        "Seat more cards for more machines, as many as the board has slots.")
                .whatCanGoWrong("It will not go in a Personal Computer.",
                        "Only an Industrial Controller Computer seats a Fieldbus Card.",
                        "It will not go in the controller either.",
                        "The board has no slot of the card's bus: an ISA card needs an ISA slot and a PCIe card a"
                                + " PCIe one; a PCI card goes in any board but an ISA one.")
                .seeAlso("jsindustrial:industrial_controller_computer")
                .register();
    }

    private static void cable(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("industrial_control_cable", section).titled("Industrial Control Cable")
                .icon(() -> IndustrialControlCable.CABLE).covers(() -> IndustrialControlCable.CABLE)
                .whatItIs("A violet cable from an Industrial Controller Computer to the machines it reads.")
                .whatItIsFor("Linking each machine to its controller, point to point, as a screen is linked to its"
                        + " computer.")
                .howToGetIt(FROM_THE_TAB)
                .nextColumn()
                .howToUseIt("Lay it from the controller to the machines, like a line of blocks. It runs in the"
                                + " shared cable block of the series, in a lane of its own, beside the energy cable.",
                        "At the controller it ends in the plug of its card: an RJ45, the violet D-sub of PROFIBUS,"
                                + " the grey D-sub of RS-485. At a machine it ends in an M12.",
                        "Every machine built on J's Core takes it, and so do tanks and valves.")
                .whatCanGoWrong("A machine at the end of a long run is not linked.",
                        "The cable reaches eight blocks of cable from the controller. Put a controller nearer the"
                                + " machine.",
                        "The cable does not join the computer.",
                        "It has no Fieldbus Card: a computer without one takes no control cable.")
                .seeAlso("jsindustrial:industrial_controller_computer")
                .register();
    }

    private static void reading(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("reading_the_plant", section).titled("Reading the plant from the network")
                .icon(IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER)
                .whatItIs("What the data network reads of the machines its Industrial Controller Computers read,"
                        + " as tables, and the requests it gives them to set those machines.")
                .whatItIsFor("Watching a plant and working it from any computer of the network.")
                .howToGetIt("It comes with an Industrial Controller Computer on the network: there is nothing to"
                        + " install.")
                .subheading("The tables")
                .paragraph("QUERY machines gives a row for each machine a controller reads: its name, such as"
                        + " Compressor #1, its kind, its state, its temperature and its limit, its heat, the power it"
                        + " draws and on what port, its progress, and the controller and card that read it.")
                .paragraph("QUERY controllers gives a row for each controller: its cards, its ports, how many"
                        + " machines it reads, and how many its cables reach that it has no port left for. QUERY"
                        + " alerts gives the alerts they keep.")
                .listingIn("iql", "plant.iql", "QUERY machines WHERE state = 'overheated'")
                .nextColumn()
                .subheading("The requests")
                .paragraph("RUN gives a controller a request for a machine it reads, named as the machines table"
                        + " names it, or by where it stands, such as 120, 64, -30.")
                .listingIn("iql", "work.iql",
                        "RUN jsindustrial:machine_set WITH machine = 'Compressor #1', parameter = 'on', value = 0",
                        "RUN jsindustrial:machine_set WITH machine = 'Compressor #2', redstone = 'low'",
                        "RUN jsindustrial:valve_set WITH valve = 'Valve #1', open = true",
                        "RUN jsindustrial:station_send WITH station = 'Capsule Station #1', to = 3")
                .paragraph("Set a value a machine offers to be set, or how it heeds redstone: ignore, high, low or"
                        + " pulse. Open or close a valve worked by hand. Send what waits in a capsule station to"
                        + " the station of that address on its tubes.")
                .howToUseIt("Write QUERY machines, QUERY controllers or QUERY alerts as any other statement of the"
                                + " network's language, on any computer of the network.",
                        "Write a RUN for the machine: ON and a controller's name sends it to that controller;"
                                + " without it, it goes to the controller that reads the machine.")
                .whatCanGoWrong("The request is refused: the controller reads no machine of that name.",
                        "Read the name in QUERY machines. A machine no controller reads takes no request.",
                        "The request is refused: the controller is switched off.",
                        "A controller switched off reads nothing and takes nothing. Switch it on.")
                .seeAlso("jsindustrial:industrial_controller_computer", "jsindustrial:machine_alerts",
                        "jsc:query_and_select", "jsc:iql_run")
                .register();
    }

    private static void alerts(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("machine_alerts", section).titled("Machine alerts")
                .icon(IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER)
                .whatItIs("What an Industrial Controller Computer says of the machines it reads when one falls into"
                        + " a fault, as a plant's alarm panel does: each alert with its grade, critical, warning or"
                        + " notice.")
                .whatItIsFor("Knowing at once that a machine is in trouble, wherever the plant is.")
                .howToGetIt("Every Industrial Controller Computer switched on raises them: there is nothing to"
                        + " install.")
                .table("The alerts", "Alert", "Grade", "When")
                .row(GuideCell.words("Overheating"), GuideCell.words("Critical"),
                        GuideCell.words("The machine is hotter than it can stand."))
                .row(GuideCell.words("Blown fuse"), GuideCell.words("Critical"),
                        GuideCell.words("The fuse of the machine, or of one of its ports, has blown."))
                .row(GuideCell.words("Undervoltage"), GuideCell.words("Warning"),
                        GuideCell.words("The machine is short of the voltage it works at."))
                .row(GuideCell.words("Gas leak"), GuideCell.words("Warning"),
                        GuideCell.words("A gas other than the air's own is at the machine past its limit: a poison"
                                + " past the share that is dangerous to breathe, a fuel past the share that can"
                                + " burn."))
                .row(GuideCell.words("Damaged"), GuideCell.words("Notice"),
                        GuideCell.words("The machine is damaged."))
                .nextColumn()
                .howToUseIt("The controller looks at its machines once a second. An alert is raised the moment a"
                                + " machine falls into a fault, and marked over once it is out of it: a fault that"
                                + " holds is one alert, and a fault that comes back is another.",
                        "Each alert goes into the network's event log as it is raised: a critical one as an error,"
                                + " a warning as a warning, a notice as information.",
                        "QUERY alerts lists them, the newest first. Each controller keeps its latest 64.",
                        "Acknowledge them in the Machine Monitor: an alert acknowledged is dimmed, never taken"
                                + " away.")
                .whatCanGoWrong("A machine in trouble raises no alert.",
                        "No controller reads it, or the one that does is switched off. See QUERY controllers.")
                .seeAlso("jsindustrial:machine_monitor", "jsindustrial:reading_the_plant", "jsc:network_manager")
                .register();
    }

    private static void monitor(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("machine_monitor", section).titled("Machine Monitor")
                .icon(IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER)
                .whatItIs("A program by Wonderwork Systems that shows the alerts of every Industrial Controller"
                        + " Computer on the network, on a computer of the Legacy or later.")
                .whatItIsFor("Watching a plant's alerts from a desk, and acknowledging them.")
                .howToGetIt("Install it on a computer of the network as any program is installed.")
                .subheading("The window")
                .paragraph("Along the top, Acknowledge, Acknowledge all and which alerts are shown: every one, or"
                        + " those of one kind. Under them, how many alerts of each kind it keeps, each with the"
                        + " lamp of its grade: red for critical, amber for a warning, grey for a notice.")
                .paragraph("The list gives each alert's time, its machine, where it stands, the alert and what the"
                        + " machine read. Choose one to see its machine in the panel: what it reads now, its"
                        + " temperature and its limit, its state, its power, and the controller and card that read"
                        + " it. The status line counts the alerts, the controllers and the machines they read.")
                .nextColumn()
                .paragraph("On Frames 95, XP and 7 it opens in its classic look, with the View and Alerts menus"
                        + " and the panel under the list. On the other desktops, from Frames 10 on, it opens in its"
                        + " modern look, with a box to search the machines by name and the panel beside the list.")
                .howToUseIt("Open it on a computer of the network: it reads the network on its own every couple of"
                                + " seconds.",
                        "Choose an alert and press Acknowledge, or press Acknowledge all.")
                .whatCanGoWrong("The list stays empty.",
                        "No alert is kept: every machine read is well. Or no controller is on this computer's"
                                + " network; the status line counts them.",
                        "It will not install.",
                        "The computer is older than the Legacy: the program needs a computer of the Legacy or"
                                + " later.")
                .seeAlso("jsindustrial:machine_alerts", "jsc:programs")
                .register();
    }
}
