/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.jstechseries.network.StateTable;
import dev.jstech.core.jstechseries.network.StateTables;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * What the network reads of the Industrial Controller Computers, as the network's language reads any state table:
 * {@code QUERY machines WHERE heat > 90}, a row for each machine a controller reads, where the machine stands, from
 * every controller on the network; and {@code QUERY controllers}, a row for each controller, saying how many machines
 * its cards read and how many its cables reach past them.
 */
@TextHolder
public final class ControllerTables {

    /* The columns, as the network's language names them. */
    public static final String NAME_COLUMN = "name";
    public static final String KIND_COLUMN = "kind";
    public static final String STATE_COLUMN = "state";
    public static final String TEMPERATURE_COLUMN = "temperature";
    public static final String LIMIT_COLUMN = "temperature_limit";
    public static final String HEAT_COLUMN = "heat";
    public static final String POWER_COLUMN = "power";
    public static final String PORT_COLUMN = "port";
    public static final String PROGRESS_COLUMN = "progress";
    public static final String CONTROLLER_COLUMN = "icc";
    public static final String CARD_COLUMN = "card";
    public static final String CARDS_COLUMN = "cards";
    public static final String PORTS_COLUMN = "ports";
    public static final String READS_COLUMN = "reads";
    public static final String LEFT_OUT_COLUMN = "left_out";

    public static final TextKey MACHINES_TITLE = TextKey.of("jsindustrial.table.machines", "Machines");
    public static final TextKey NAME = TextKey.of("jsindustrial.table.machines.name", "Name");
    public static final TextKey KIND = TextKey.of("jsindustrial.table.machines.kind", "Kind");
    public static final TextKey STATE = TextKey.of("jsindustrial.table.machines.state", "State");
    public static final TextKey TEMPERATURE = TextKey.of("jsindustrial.table.machines.temperature", "Temperature");
    public static final TextKey TEMPERATURE_LIMIT = TextKey.of("jsindustrial.table.machines.temperature_limit",
            "Temperature limit");
    public static final TextKey HEAT = TextKey.of("jsindustrial.table.machines.heat", "Heat");
    public static final TextKey POWER = TextKey.of("jsindustrial.table.machines.power", "Power");
    public static final TextKey PORT = TextKey.of("jsindustrial.table.machines.port", "Port");
    public static final TextKey PROGRESS = TextKey.of("jsindustrial.table.machines.progress", "Progress");
    public static final TextKey CONTROLLER = TextKey.of("jsindustrial.table.machines.icc", "Controller");
    public static final TextKey CARD = TextKey.of("jsindustrial.table.machines.card", "Card");
    /**
     * Each machine a controller reads: its name (the block's and its number among the controller's machines of that
     * block, "Compressor #1"), what block it is, the state its lamp shows, its temperature and the most it stands in
     * degrees Celsius, how hot it is as a share of that most, what it draws and the class of its port, how far along
     * its work is, the controller and the card that read it, and the node it is on that card.
     */
    public static final StateTable MACHINES = StateTables.declare(StateTable.builder(id("machines"), MACHINES_TITLE)
            .text(NAME_COLUMN, NAME)
            .text(KIND_COLUMN, KIND)
            .text(STATE_COLUMN, STATE)
            .number(TEMPERATURE_COLUMN, TEMPERATURE, "°C")
            .number(LIMIT_COLUMN, TEMPERATURE_LIMIT, "°C")
            .number(HEAT_COLUMN, HEAT, "%")
            .number(POWER_COLUMN, POWER, "W")
            .text(PORT_COLUMN, PORT)
            .number(PROGRESS_COLUMN, PROGRESS, "%")
            .text(CONTROLLER_COLUMN, CONTROLLER)
            .text(CARD_COLUMN, CARD)
            .build());

    public static final TextKey CONTROLLERS_TITLE = TextKey.of("jsindustrial.table.controllers",
            "Industrial controllers");
    public static final TextKey CARDS = TextKey.of("jsindustrial.table.controllers.cards", "Fieldbus Cards");
    public static final TextKey PORTS = TextKey.of("jsindustrial.table.controllers.ports", "Machines it can read");
    public static final TextKey READS = TextKey.of("jsindustrial.table.controllers.reads", "Machines it reads");
    public static final TextKey LEFT_OUT = TextKey.of("jsindustrial.table.controllers.left_out",
            "Machines left out");
    /**
     * Each controller: its name, how many Fieldbus Cards it has and how many machines they can read, how many it
     * reads, and how many more its cables reach that no controller reads, left out for want of ports.
     */
    public static final StateTable CONTROLLERS = StateTables.declare(StateTable.builder(id("controllers"),
                    CONTROLLERS_TITLE)
            .text(NAME_COLUMN, NAME)
            .number(CARDS_COLUMN, CARDS, "")
            .number(PORTS_COLUMN, PORTS, "")
            .number(READS_COLUMN, READS, "")
            .number(LEFT_OUT_COLUMN, LEFT_OUT, "")
            .build());

    private ControllerTables() {
    }

    /** Declares the tables, while the mod is built. */
    static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
