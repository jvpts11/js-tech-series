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
import dev.jstech.core.guide.ModGuide;

import static dev.jstech.industrial.guide.IndustrialGuideTexts.FROM_THE_TAB;

/**
 * The drawings of what comes with J's Computers, in the mod's Control section: the Industrial Controller Computer,
 * its Fieldbus Cards and the industrial control cable.
 */
final class ControllerPages {

    private ControllerPages() {
    }

    static void declare(final ModGuide guide, final ModGuide.SectionRef section) {
        controller(guide, section);
        cards(guide, section);
        cable(guide, section);
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
                .seeAlso("jsindustrial:fieldbus_cards", "jsindustrial:industrial_control_cable")
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
}
